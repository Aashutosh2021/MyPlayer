package com.example.myplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.playback.PlaybackCompletionGuard
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong
import kotlinx.coroutines.flow.firstOrNull
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SyncPlaybackInterceptor {
    fun onPlayRequested(song: SongEntity, startIndex: Int): Boolean
    fun onPauseRequested(): Boolean = false
    fun onResumeRequested(): Boolean = false
    fun onSeekRequested(positionMs: Long): Boolean = false
    fun onTrackSkipRequested(isNext: Boolean): Boolean = false
    fun onPlayerIsPlayingChanged(isPlaying: Boolean, positionMs: Long) {}
}

@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val settingsDataStore: SettingsDataStore,
    private val playbackCompletionGuard: PlaybackCompletionGuard,
    private val sleepTimerManager: SleepTimerManager,
    private val playbackErrorHandler: PlaybackErrorHandler,
    private val playbackStateManager: PlaybackStateManager,
    private val playbackRouter: PlaybackRouter,
    private val playbackEventBus: PlaybackEventBus,
    private val downloadedSongDao: DownloadedSongDao,
    private val playbackSourceResolver: PlaybackSourceResolver,
    private val mediaItemFactory: MediaItemFactory
) : PlaybackRouterDelegate {
    // IMPORTANT: MediaController.verifyApplicationThread() enforces that ALL MediaController
    // API calls (currentPosition, duration, seekTo, play, pause, setMediaItem, etc.) must
    // be made from the application (Main) thread. This scope must stay on Dispatchers.Main.
    // Heavy background work (DB, network) inside coroutines uses withContext(Dispatchers.IO).
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Keep a local copy of the queue so we can map MediaItem → SongEntity
    private val _songQueue = mutableListOf<SongEntity>()
    private val _playbackQueue = MutableStateFlow<List<SongEntity>>(emptyList())
    val playbackQueue: StateFlow<List<SongEntity>> = _playbackQueue.asStateFlow()

    @Volatile var syncPlaybackInterceptor: SyncPlaybackInterceptor? = null

    // Playback history stack (Phase R9/R10)
    private val playbackHistory = java.util.Stack<Int>()
    private var isNavigatingHistory = false
    private var lastIndex = C.INDEX_UNSET

    // Online playback recovery & bounded retry state
    private val onlineRetryCount = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private var activeRecoveryJob: Job? = null
    private var currentRecoveryToken: Long = 0L
    private var isCurrentTrackAutoplayRecommendation: Boolean = false

    val currentSong: StateFlow<SongEntity?> = playbackStateManager.currentSong
    val currentOnlineSong: StateFlow<OnlineSong?> = playbackStateManager.currentOnlineSong
    val isPlaying: StateFlow<Boolean> = playbackStateManager.isPlaying
    val playbackError: StateFlow<String?> = playbackErrorHandler.playbackError
    val currentPosition: StateFlow<Long> = playbackStateManager.currentPosition
    val currentDuration: StateFlow<Long> = playbackStateManager.currentDuration
    val isShuffleOn: StateFlow<Boolean> = playbackStateManager.isShuffleOn
    val repeatMode: StateFlow<Int> = playbackStateManager.repeatMode
    val sleepTimerRemainingSeconds: StateFlow<Long> = sleepTimerManager.sleepTimerRemainingSeconds
    val currentAudioQuality: StateFlow<AudioQualityInfo> = playbackRouter.currentAudioQuality

    private var positionJob: Job? = null

    private val playbackPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPersistedRepeatMode(): Int =
        playbackPrefs.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)

    private fun saveRepeatMode(mode: Int) {
        playbackPrefs.edit().putInt(KEY_REPEAT_MODE, mode).apply()
    }

    private fun getPersistedShuffleMode(): Boolean =
        playbackPrefs.getBoolean(KEY_SHUFFLE_MODE, false)

    private fun saveShuffleMode(enabled: Boolean) {
        playbackPrefs.edit().putBoolean(KEY_SHUFFLE_MODE, enabled).apply()
    }

    init {
        // Restore persisted repeat and shuffle states immediately for UI/state flows
        val savedRepeat = getPersistedRepeatMode()
        val savedShuffle = getPersistedShuffleMode()
        playbackStateManager.updateRepeatMode(savedRepeat)
        playbackStateManager.updateShuffle(savedShuffle)

        initializeMediaController()

        scope.launch {
            playbackEventBus.events.collect { event ->
                if (event is PlaybackEvent.PlayRequestReady) {
                    cancelActiveRecovery("Received PlayRequestReady")
                    Log.d("MusicController", "Received PlayRequestReady event: playing ${event.requests.size} items")
                    val targetReq = event.requests.getOrNull(event.startIndex)
                    if (targetReq != null) {
                        isCurrentTrackAutoplayRecommendation = (targetReq.playbackSource == PlaybackSourceType.RECOMMENDATION)
                        val artUrl = targetReq.albumArt?.ifBlank { "https://img.youtube.com/vi/${targetReq.songId}/hqdefault.jpg" }
                            ?: "https://img.youtube.com/vi/${targetReq.songId}/hqdefault.jpg"
                        val newEntity = SongEntity(
                            id = targetReq.songId,
                            title = targetReq.title,
                            artist = targetReq.artist,
                            album = if (targetReq.playbackSource == PlaybackSourceType.RECOMMENDATION) "Recommended" else "Online",
                            duration = 0L,
                            path = targetReq.streamUrl ?: targetReq.localUri ?: "online://${targetReq.songId}",
                            albumArt = artUrl,
                            dateAdded = System.currentTimeMillis(),
                            videoId = targetReq.songId
                        )
                        val existingIndex = _songQueue.indexOfFirst { it.id == newEntity.id || it.videoId == newEntity.videoId }
                        val playIndex = if (existingIndex != -1) {
                            _songQueue[existingIndex] = newEntity
                            existingIndex
                        } else {
                            _songQueue.add(newEntity)
                            _songQueue.size - 1
                        }
                        _playbackQueue.value = _songQueue.toList()
                        playOnlineQueueIndex(playIndex)
                    }
                }
            }
        }
    }

    private fun initializeMediaController() {
        try {
            context.startService(android.content.Intent(context, MusicService::class.java))
        } catch (_: Exception) {}

        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    Log.w("MusicController", "MediaController disconnected from MusicService session. Rebuilding connection...")
                    mediaController = null
                    scope.launch {
                        delay(500)
                        initializeMediaController()
                    }
                }
            })
            .buildAsync()

        mediaControllerFuture = future
        future.addListener({
            try {
                mediaController = future.get()
                setupController()
                Log.d("MusicController", "MediaController successfully connected to MusicService")
            } catch (e: Exception) {
                Log.e("MusicController", "Failed to connect MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        val controller = mediaController
        if (controller != null) {
            val savedRepeat = getPersistedRepeatMode()
            val savedShuffle = getPersistedShuffleMode()
            val effectiveSavedRepeat = if (controller.mediaItemCount <= 1 && savedRepeat == Player.REPEAT_MODE_ALL) {
                Player.REPEAT_MODE_OFF
            } else {
                savedRepeat
            }
            controller.repeatMode = effectiveSavedRepeat
            controller.shuffleModeEnabled = savedShuffle
            playbackStateManager.updateRepeatMode(savedRepeat)
            playbackStateManager.updateShuffle(savedShuffle)
            updateCurrentSong()
        }

        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val controller = mediaController
                if (controller != null) {
                    val currentIndex = controller.currentMediaItemIndex
                    Log.d("MusicController", "[TRANSITION] reason=$reason index=$currentIndex mediaId=${mediaItem?.mediaId}")
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                        if (controller.mediaItemCount <= 1 && playbackStateManager.repeatMode.value != Player.REPEAT_MODE_ONE) {
                            Log.d("MusicController", "Single track repeat intercepted under REPEAT_MODE_ALL - triggering completion for autoplay")
                            handlePlaybackEnded()
                            return
                        }
                    }
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                        playbackHistory.clear()
                        lastIndex = currentIndex
                        isNavigatingHistory = false
                    } else {
                        if (lastIndex != C.INDEX_UNSET && lastIndex != currentIndex) {
                            if (!isNavigatingHistory) {
                                playbackHistory.push(lastIndex)
                            }
                        }
                        lastIndex = currentIndex
                        isNavigatingHistory = false
                    }
                    playbackRouter.onTrackTransition(scope, this@MusicController, currentIndex)
                }

                updateCurrentSong()
                playbackStateManager.updatePosition(0L)
                playbackStateManager.updateDuration(0L)
                playbackCompletionGuard.reset()

                mediaItem?.let { item ->
                    val vId = extractVideoId(item.mediaId, item.localConfiguration?.uri?.toString())
                    if (vId != null) {
                        onlineRetryCount.remove(vId)
                    }
                    val isOnline = isOnlineTrack(item.mediaId, item.localConfiguration?.uri?.toString())
                    playbackEventBus.emit(
                        PlaybackEvent.SongStarted(
                            songId = item.mediaId,
                            title = item.mediaMetadata.title?.toString() ?: "",
                            artist = item.mediaMetadata.artist?.toString() ?: "",
                            isOnline = isOnline
                        )
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                playbackStateManager.updatePlayingState(isPlaying)
                if (isPlaying) {
                    startPositionUpdater()
                    val mediaId = mediaController?.currentMediaItem?.mediaId
                    if (mediaId != null) {
                        onlineRetryCount.remove(mediaId)
                        extractVideoId(mediaId, mediaController?.currentMediaItem?.localConfiguration?.uri?.toString())?.let {
                            onlineRetryCount.remove(it)
                        }
                    }
                } else {
                    stopPositionUpdater()
                }
                val pos = mediaController?.currentPosition ?: 0L
                syncPlaybackInterceptor?.onPlayerIsPlayingChanged(isPlaying, pos)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackStateManager.updateDuration(mediaController?.duration ?: 0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    if (activeRecoveryJob?.isActive == true) {
                        Log.d("MusicController", "Ignoring STATE_ENDED while online recovery is active")
                        return
                    }
                    val mediaId = mediaController?.currentMediaItem?.mediaId
                    if (!playbackCompletionGuard.isAlreadyHandled(mediaId)) {
                        handlePlaybackEnded()
                    } else {
                        Log.d("MusicController", "PlaybackCompletionGuard rejected duplicate completion for $mediaId")
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val controller = mediaController ?: return
                val currentItem = controller.currentMediaItem
                val mediaId = currentItem?.mediaId ?: ""
                val uriStr = currentItem?.localConfiguration?.uri?.toString()
                val isOnline = isOnlineTrack(mediaId, uriStr)
                val videoId = if (isOnline) extractVideoId(mediaId, uriStr) else null
                val isRetryable = isOnline && !videoId.isNullOrBlank() && isRetryableOnlineError(error)

                if (isOnline && videoId != null) {
                    Log.w("MusicController", "[ONLINE_PLAYBACK_ERROR] videoId=$videoId errorCode=${error.errorCode} errorCodeName=${error.errorCodeName}")
                } else {
                    Log.w("MusicController", "Player error for $mediaId: ${error.errorCodeName} (isOnline=$isOnline)")
                }

                if (!isRetryable || videoId.isNullOrBlank()) {
                    val hasNext = controller.hasNextMediaItem()
                    playbackEventBus.emit(PlaybackEvent.SongError(mediaId, error.message ?: "Unknown error"))
                    playbackErrorHandler.handleError(error, hasNext, isOnline = isOnline)
                    if (hasNext) {
                        controller.seekToNext()
                        controller.prepare()
                        controller.play()
                    } else {
                        playbackStateManager.updatePlayingState(false)
                    }
                    return
                }

                val currentAttempt = onlineRetryCount.getOrDefault(videoId, 0)
                if (currentAttempt < MAX_ONLINE_RETRIES) {
                    val nextAttempt = currentAttempt + 1
                    onlineRetryCount[videoId] = nextAttempt
                    val delayMs = RETRY_DELAYS_MS.getOrElse(currentAttempt) { 1500L }

                    Log.i("MusicController", "[ONLINE_RECOVERY_START] videoId=$videoId attempt=$nextAttempt")

                    val token = ++currentRecoveryToken
                    activeRecoveryJob?.cancel()
                    activeRecoveryJob = scope.launch {
                        if (delayMs > 0L) {
                            delay(delayMs)
                        }
                        if (token != currentRecoveryToken || !isActive) {
                            Log.d("MusicController", "Recovery cancelled before resolution for $videoId")
                            return@launch
                        }

                        Log.i("MusicController", "[ONLINE_RECOVERY_RESOLVE] videoId=$videoId")
                        val freshUrl = withContext(Dispatchers.IO) {
                            try {
                                playbackSourceResolver.resolveFreshStreamUrl(videoId)
                            } catch (e: Exception) {
                                Log.e("MusicController", "Exception while resolving fresh stream URL for $videoId", e)
                                null
                            }
                        }

                        if (token != currentRecoveryToken || !isActive) {
                            Log.d("MusicController", "Recovery cancelled after resolution for $videoId")
                            return@launch
                        }

                        if (!freshUrl.isNullOrBlank()) {
                            Log.i("MusicController", "[ONLINE_RECOVERY_SUCCESS] videoId=$videoId attempt=$nextAttempt")
                            withContext(Dispatchers.Main) {
                                if (token != currentRecoveryToken || !isActive) return@withContext
                                val ctrl = mediaController ?: return@withContext
                                val activeMediaId = ctrl.currentMediaItem?.mediaId
                                if (activeMediaId != null && activeMediaId != mediaId && activeMediaId != videoId) {
                                    Log.w("MusicController", "Active track changed during recovery from $mediaId to $activeMediaId")
                                    return@withContext
                                }

                                val qIndex = _songQueue.indexOfFirst { it.id == mediaId || it.videoId == videoId }
                                val queueEntity = if (qIndex != -1) _songQueue[qIndex] else null
                                if (qIndex != -1 && queueEntity != null) {
                                    _songQueue[qIndex] = queueEntity.copy(path = freshUrl)
                                    _playbackQueue.value = _songQueue.toList()
                                }
                                val onlineSong = playbackStateManager.currentOnlineSong.value
                                if (onlineSong != null && (onlineSong.videoId == videoId || onlineSong.videoId == mediaId)) {
                                    playbackStateManager.updateCurrentOnlineSong(onlineSong.copy(streamUrl = freshUrl))
                                }

                                val currentItemMeta = ctrl.currentMediaItem?.mediaMetadata
                                val title = currentItemMeta?.title?.toString() ?: queueEntity?.title ?: "Online Song"
                                val artist = currentItemMeta?.artist?.toString() ?: queueEntity?.artist ?: ""
                                val artworkUri = currentItemMeta?.artworkUri?.toString()
                                    ?: queueEntity?.albumArt
                                    ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                                val freshMediaItem = mediaItemFactory.createMediaItem(
                                    songId = mediaId.ifBlank { videoId },
                                    path = freshUrl,
                                    title = title,
                                    artist = artist,
                                    albumArt = artworkUri
                                )

                                val resumePos = ctrl.currentPosition.coerceAtLeast(0L)

                                if (ctrl.mediaItemCount <= 1) {
                                    ctrl.setMediaItem(freshMediaItem, resumePos)
                                } else {
                                    val idx = ctrl.currentMediaItemIndex
                                    if (idx in 0 until ctrl.mediaItemCount) {
                                        ctrl.replaceMediaItem(idx, freshMediaItem)
                                        ctrl.seekTo(idx, resumePos)
                                    } else {
                                        ctrl.setMediaItem(freshMediaItem, resumePos)
                                    }
                                }
                                ctrl.prepare()
                                ctrl.play()
                                playbackStateManager.updatePlayingState(true)
                                playbackErrorHandler.clearPlaybackError()
                            }
                        } else {
                            Log.e("MusicController", "[ONLINE_RECOVERY_FAILED] videoId=$videoId attempt=$nextAttempt reason=Fresh URL resolution returned null")
                            withContext(Dispatchers.Main) {
                                if (token == currentRecoveryToken && isActive) {
                                    onPlayerError(error)
                                }
                            }
                        }
                    }
                } else {
                    Log.e("MusicController", "[ONLINE_RECOVERY_FAILED] videoId=$videoId attempt=$currentAttempt reason=Exhausted $MAX_ONLINE_RETRIES retries")
                    onlineRetryCount.remove(videoId)

                    val isAutoplayTrack = isCurrentTrackAutoplayRecommendation ||
                        _songQueue.firstOrNull { it.id == mediaId || it.videoId == videoId }?.album == "Recommended"

                    if (isAutoplayTrack) {
                        Log.w("MusicController", "[AUTOPLAY_SKIP_FAILED_TRACK] videoId=$videoId")
                        Log.i("MusicController", "[AUTOPLAY_NEXT_AFTER_FAILURE] videoId=$videoId")

                        playbackEventBus.emit(PlaybackEvent.SongError(mediaId.ifBlank { videoId }, "Exhausted $MAX_ONLINE_RETRIES retries"))
                        playbackEventBus.emit(PlaybackEvent.AutoplayRequested)
                    } else {
                        val hasNext = controller.hasNextMediaItem()
                        playbackEventBus.emit(PlaybackEvent.SongError(mediaId, error.message ?: "Unknown error"))
                        playbackErrorHandler.handleError(error, hasNext, isOnline = true)
                        if (hasNext) {
                            controller.seekToNext()
                            controller.prepare()
                            controller.play()
                        } else {
                            playbackStateManager.updatePlayingState(false)
                        }
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                playbackStateManager.updateShuffle(shuffleModeEnabled)
                saveShuffleMode(shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                if (!(repeatMode == Player.REPEAT_MODE_OFF && playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ALL && (mediaController?.mediaItemCount ?: 0) <= 1)) {
                    playbackStateManager.updateRepeatMode(repeatMode)
                    saveRepeatMode(repeatMode)
                }
            }
        })
    }

    private fun handlePlaybackEnded() {
        val controller = mediaController
        if (controller?.hasNextMediaItem() == true) {
            Log.d("MusicController", "ExoPlayer has remaining queue items, manual queue retains priority")
            return
        }
        val mediaId = controller?.currentMediaItem?.mediaId ?: ""

        val currentIndex = _songQueue.indexOfFirst { (!mediaId.isNullOrBlank() && (it.id == mediaId || it.videoId == mediaId)) }
        if (playbackStateManager.isShuffleOn.value && _songQueue.size > 1) {
            val validIndices = _songQueue.indices.filter { it != currentIndex }
            val nextIndex = validIndices.randomOrNull() ?: 0
            Log.d("MusicController", "Shuffle selected next online queue song at index $nextIndex")
            playOnlineQueueIndex(nextIndex)
            return
        }
        if (currentIndex != -1 && currentIndex + 1 < _songQueue.size) {
            Log.d("MusicController", "Advancing to next online queue song at index ${currentIndex + 1} (${_songQueue[currentIndex + 1].title})")
            playOnlineQueueIndex(currentIndex + 1)
            return
        }

        if (playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ALL && _songQueue.size > 1) {
            Log.d("MusicController", "Queue loop under REPEAT_MODE_ALL, restarting from index 0")
            playOnlineQueueIndex(0)
            return
        }

        Log.d("MusicController", "Queue ended, emitting SongCompleted for autoplay recommendation")
        playbackEventBus.emit(PlaybackEvent.SongCompleted(mediaId))
    }

    private fun startPositionUpdater() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                playbackStateManager.updatePosition(mediaController?.currentPosition ?: 0L)
                val dur = mediaController?.duration ?: 0L
                if (dur > 0L) {
                    playbackStateManager.updateDuration(dur)
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdater() {
        positionJob?.cancel()
        positionJob = null
        playbackStateManager.updatePosition(mediaController?.currentPosition ?: currentPosition.value)
    }

    private fun updateCurrentSong() {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem
        val mediaId = currentItem?.mediaId
        val uriStr = currentItem?.localConfiguration?.uri?.toString()

        val song = if (currentItem != null) {
            _songQueue.firstOrNull {
                (!mediaId.isNullOrBlank() && (it.id == mediaId || it.videoId == mediaId)) ||
                (!uriStr.isNullOrBlank() && it.path == uriStr)
            } ?: if (controller.mediaItemCount == _songQueue.size && controller.currentMediaItemIndex in _songQueue.indices) {
                val candidate = _songQueue[controller.currentMediaItemIndex]
                if (mediaId.isNullOrBlank() || candidate.id == mediaId || candidate.videoId == mediaId) {
                    candidate
                } else null
            } else null
        } else null

        if (song != null) {
            val resolvedArt = song.albumArt ?: song.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
            val songWithArt = if (song.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
                song.copy(albumArt = resolvedArt)
            } else {
                song
            }
            playbackStateManager.updateCurrentSong(songWithArt)
        } else if (currentItem != null) {
            val metadata = currentItem.mediaMetadata
            val resolvedArt = metadata.artworkUri?.toString()
                ?: mediaId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
            val fallbackSong = SongEntity(
                id = mediaId ?: java.util.UUID.randomUUID().toString(),
                title = metadata.title?.toString() ?: "Unknown Track",
                artist = metadata.artist?.toString() ?: "Unknown Artist",
                album = metadata.albumTitle?.toString() ?: "Music",
                duration = controller.duration.coerceAtLeast(0L),
                path = uriStr ?: "",
                albumArt = resolvedArt,
                dateAdded = System.currentTimeMillis(),
                videoId = mediaId
            )
            playbackStateManager.updateCurrentSong(fallbackSong)
        }

        if (currentItem != null) {
            val isOnline = uriStr?.startsWith("http") == true || uriStr?.startsWith("online://") == true || (mediaId != null && mediaId.length == 11)
            if (isOnline && !mediaId.isNullOrBlank()) {
                val metadata = currentItem.mediaMetadata
                val art = song?.albumArt ?: metadata.artworkUri?.toString() ?: "https://img.youtube.com/vi/$mediaId/hqdefault.jpg"
                val onlineSong = OnlineSong(
                    videoId = mediaId,
                    title = song?.title ?: metadata.title?.toString() ?: "Unknown Track",
                    artist = song?.artist ?: metadata.artist?.toString() ?: "Unknown Artist",
                    thumbnailUrl = art,
                    durationMs = controller.duration.coerceAtLeast(0L),
                    durationText = "",
                    streamUrl = uriStr ?: "online://$mediaId"
                )
                playbackStateManager.updateCurrentOnlineSong(onlineSong)
            }
        }
    }

    // ── Online Playback Recovery Helpers ──────────────────────────────────────

    private fun isOnlineTrack(mediaId: String?, uriStr: String?): Boolean {
        if (mediaId.isNullOrBlank() && uriStr.isNullOrBlank()) return false
        if (uriStr?.startsWith("http://") == true ||
            uriStr?.startsWith("https://") == true ||
            uriStr?.startsWith("online://") == true) {
            return true
        }
        val queueItem = _songQueue.firstOrNull { it.id == mediaId || it.videoId == mediaId }
        if (queueItem != null) {
            val path = queueItem.path
            if (path.startsWith("http") || path.startsWith("online://")) return true
            if (path.startsWith("/") || path.startsWith("content://") || path.startsWith("file://")) {
                if (java.io.File(path).exists()) return false
            }
            if (!queueItem.videoId.isNullOrBlank()) return true
        }
        val onlineSong = playbackStateManager.currentOnlineSong.value
        if (onlineSong != null && (onlineSong.videoId == mediaId || onlineSong.streamUrl == uriStr)) {
            return true
        }
        if (mediaId != null && mediaId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return true
        }
        return false
    }

    private fun extractVideoId(mediaId: String?, uriStr: String?): String? {
        val queueItem = _songQueue.firstOrNull { it.id == mediaId || it.videoId == mediaId }
        queueItem?.videoId?.let { if (it.isNotBlank()) return it }
        val onlineSong = playbackStateManager.currentOnlineSong.value
        if (onlineSong != null && onlineSong.videoId.isNotBlank()) {
            if (onlineSong.videoId == mediaId || onlineSong.streamUrl == uriStr) {
                return onlineSong.videoId
            }
        }
        if (mediaId != null) {
            if (mediaId.startsWith("online://")) return mediaId.removePrefix("online://")
            if (mediaId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return mediaId
        }
        if (uriStr != null && uriStr.startsWith("online://")) {
            return uriStr.removePrefix("online://")
        }
        return queueItem?.id?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
    }

    private fun isRetryableOnlineError(error: PlaybackException): Boolean {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> true
            else -> {
                val cause = error.cause
                cause is java.io.IOException ||
                cause is java.net.SocketTimeoutException ||
                cause is java.net.UnknownHostException ||
                cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException
            }
        }
    }

    private fun cancelActiveRecovery(reason: String) {
        if (activeRecoveryJob != null) {
            Log.d("MusicController", "Cancelling active recovery: $reason")
            activeRecoveryJob?.cancel()
            activeRecoveryJob = null
        }
        currentRecoveryToken++
        onlineRetryCount.clear()
    }

    // ── Local Playback ────────────────────────────────────────────────────────

    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        cancelActiveRecovery("playSongs")
        isCurrentTrackAutoplayRecommendation = false
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        val selectedSong = songs[clampedIndex]

        if (syncPlaybackInterceptor?.onPlayRequested(selectedSong, clampedIndex) == true) {
            Log.d("MusicController", "[SYNC PLAY INTERCEPT] Intercepted playback request for: ${selectedSong.title}")
            val resolvedArt = selectedSong.albumArt ?: selectedSong.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
            val songWithArt = if (selectedSong.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
                selectedSong.copy(albumArt = resolvedArt)
            } else {
                selectedSong
            }
            val updatedSongs = songs.toMutableList()
            updatedSongs[clampedIndex] = songWithArt
            _songQueue.clear()
            _songQueue.addAll(updatedSongs)
            _playbackQueue.value = _songQueue.toList()
            playbackStateManager.updateCurrentSong(songWithArt)
            return
        }

        Log.d("MusicController", "[QUEUE_CREATED] size=${songs.size} startIndex=$clampedIndex firstId=${songs.firstOrNull()?.id}")
        _songQueue.clear()
        _songQueue.addAll(songs)
        _playbackQueue.value = _songQueue.toList()
        playbackStateManager.updateCurrentOnlineSong(null)

        val requests = songs.map { song ->
            PlayRequest(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                playbackSource = PlaybackSourceType.LOCAL,
                localUri = song.path,
                albumArt = song.albumArt
            )
        }

        playbackRouter.play(scope, this, requests, clampedIndex)
    }

    /**
     * Unified play dispatcher: plays any PlayableSong regardless of source.
     */
    fun playSong(song: PlayableSong) {
        when (song) {
            is PlayableSong.Local -> playSongs(listOf(song.entity), 0)
            is PlayableSong.Downloaded -> playDownloadedSong(song.entity)
            is PlayableSong.Online -> {
                val onlineSong = OnlineSong(
                    videoId = song.id,
                    title = song.title,
                    artist = song.artist,
                    thumbnailUrl = song.thumbnailUrl ?: "",
                    durationMs = song.durationMs
                )
                playOnlineSong(onlineSong)
            }
        }
    }

    /**
     * Queue an entire playlist and start playback from [startIndex].
     * Supports Local, Downloaded, and Online songs. Converts all to SongEntity for ExoPlayer queue.
     */
    fun playPlaylist(songs: List<PlayableSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val entities = songs.map { playable ->
            when (playable) {
                is PlayableSong.Local -> playable.entity
                is PlayableSong.Downloaded -> SongEntity(
                    id = playable.entity.id,
                    title = playable.entity.title,
                    artist = playable.entity.artist,
                    album = playable.entity.album.ifBlank { "Downloads" },
                    duration = playable.entity.durationMs,
                    path = playable.entity.localPath,
                    albumArt = playable.entity.thumbnailUrl,
                    dateAdded = playable.entity.downloadedAt,
                    videoId = playable.entity.id
                )
                is PlayableSong.Online -> SongEntity(
                    id = playable.id,
                    title = playable.title,
                    artist = playable.artist,
                    album = "YouTube Music",
                    duration = playable.durationMs,
                    path = "online://${playable.id}",
                    albumArt = playable.thumbnailUrl,
                    dateAdded = System.currentTimeMillis(),
                    videoId = playable.id
                )
            }
        }
        if (entities.isEmpty()) return
        cancelActiveRecovery("playDownloadedSongs")
        isCurrentTrackAutoplayRecommendation = false
        val clampedIndex = startIndex.coerceIn(0, entities.size - 1)
        playSongs(entities, clampedIndex)
    }

    // ── Online Streaming ──────────────────────────────────────────────────────

    fun playOnlineSongs(songs: List<OnlineSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        cancelActiveRecovery("playOnlineSongs")
        isCurrentTrackAutoplayRecommendation = false
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        val selectedSong = songs[clampedIndex]

        val syntheticSongs = songs.map { s ->
            val artUrl = s.thumbnailUrl.ifBlank { "https://img.youtube.com/vi/${s.videoId}/hqdefault.jpg" }
            SongEntity(
                id = s.videoId,
                title = s.title,
                artist = s.artist,
                album = "Online",
                duration = s.durationMs,
                path = s.streamUrl ?: "online://${s.videoId}",
                albumArt = artUrl,
                dateAdded = System.currentTimeMillis(),
                videoId = s.videoId
            )
        }
        val selectedSynthetic = syntheticSongs[clampedIndex]

        if (syncPlaybackInterceptor?.onPlayRequested(selectedSynthetic, clampedIndex) == true) {
            Log.d("MusicController", "[SYNC PLAY INTERCEPT] Intercepted online playback request for: ${selectedSynthetic.title}")
            _songQueue.clear()
            _songQueue.addAll(syntheticSongs)
            _playbackQueue.value = _songQueue.toList()
            playbackStateManager.updateCurrentSong(selectedSynthetic)
            playbackStateManager.updateCurrentOnlineSong(selectedSong)
            return
        }

        _songQueue.clear()
        _songQueue.addAll(syntheticSongs)
        _playbackQueue.value = _songQueue.toList()

        playOnlineQueueIndex(clampedIndex)
    }

    fun playOnlineQueueIndex(index: Int) {
        if (index !in _songQueue.indices) return
        cancelActiveRecovery("playOnlineQueueIndex")
        val targetEntity = _songQueue[index]
        val vId = targetEntity.videoId ?: targetEntity.id
        val artUrl = targetEntity.albumArt ?: "https://img.youtube.com/vi/$vId/hqdefault.jpg"
        val onlineSong = OnlineSong(
            videoId = vId,
            title = targetEntity.title,
            artist = targetEntity.artist,
            thumbnailUrl = artUrl,
            durationMs = targetEntity.duration,
            durationText = "",
            streamUrl = if (targetEntity.path.startsWith("http")) targetEntity.path else null
        )

        playbackStateManager.updateCurrentSong(targetEntity)
        playbackStateManager.updateCurrentOnlineSong(onlineSong)
        playbackStateManager.updatePosition(0L)
        playbackStateManager.updateDuration(targetEntity.duration)

        val request = PlayRequest(
            songId = vId,
            title = targetEntity.title,
            artist = targetEntity.artist,
            playbackSource = PlaybackSourceType.ONLINE,
            localUri = targetEntity.path,
            streamUrl = onlineSong.streamUrl,
            albumArt = artUrl
        )
        playbackRouter.play(scope, this, listOf(request), 0)
    }

    fun playOnlineSong(song: OnlineSong) {
        playOnlineSongs(listOf(song), 0)
    }

    fun playDownloadedSongs(songs: List<DownloadedSongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val entities = songs.map { d ->
            SongEntity(
                id = d.id,
                title = d.title,
                artist = d.artist,
                album = d.album.ifBlank { "Downloads" },
                duration = d.durationMs,
                path = d.localPath,
                albumArt = d.thumbnailUrl,
                dateAdded = d.downloadedAt,
                videoId = d.id
            )
        }
        val clampedIndex = startIndex.coerceIn(0, entities.size - 1)
        playSongs(entities, clampedIndex)
    }

    fun playDownloadedSong(song: DownloadedSongEntity) {
        scope.launch {
            val downloads = withContext(Dispatchers.IO) {
                downloadedSongDao.getAllDownloadsSync()
            }
            if (downloads.isEmpty()) {
                val localSong = SongEntity(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album.ifBlank { "Downloads" },
                    duration = song.durationMs,
                    path = song.localPath,
                    albumArt = song.thumbnailUrl,
                    dateAdded = song.downloadedAt,
                    videoId = song.id
                )
                playSongs(listOf(localSong), 0)
                return@launch
            }

            val startIndex = downloads.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playDownloadedSongs(downloads, startIndex)
        }
    }

    // ── Controls ──────────────────────────────────────────────────────────────

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (syncPlaybackInterceptor?.onResumeRequested() == true) {
            return
        }
        try {
            context.startService(android.content.Intent(context, MusicService::class.java))
        } catch (_: Exception) {}
        mediaController?.play()
    }

    fun pause() {
        if (syncPlaybackInterceptor?.onPauseRequested() == true) {
            return
        }
        mediaController?.pause()
    }

    fun skipToNext() {
        cancelActiveRecovery("skipToNext")
        if (syncPlaybackInterceptor?.onTrackSkipRequested(isNext = true) == true) {
            return
        }
        val controller = mediaController ?: return
        val currentMediaId = controller.currentMediaItem?.mediaId ?: ""
        if (currentMediaId.isNotEmpty()) {
            playbackEventBus.emit(PlaybackEvent.SongSkipped(currentMediaId))
        }
        Log.d("MusicController", "[SKIP_NEXT] hasNext=${controller.hasNextMediaItem()} currentIndex=${controller.currentMediaItemIndex}")
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        } else {
            val currentIndex = _songQueue.indexOfFirst {
                (!currentMediaId.isNullOrBlank() && (it.id == currentMediaId || it.videoId == currentMediaId))
            }
            if (playbackStateManager.isShuffleOn.value && _songQueue.size > 1) {
                val nextIndex = _songQueue.indices.filter { it != currentIndex }.randomOrNull() ?: 0
                Log.d("MusicController", "[SKIP_NEXT] Shuffle playing queue item at $nextIndex")
                playOnlineQueueIndex(nextIndex)
            } else if (currentIndex != -1 && currentIndex + 1 < _songQueue.size) {
                Log.d("MusicController", "[SKIP_NEXT] Advancing to online queue song at ${currentIndex + 1}")
                playOnlineQueueIndex(currentIndex + 1)
            } else if (playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ALL && _songQueue.isNotEmpty()) {
                Log.d("MusicController", "[SKIP_NEXT] Looping online queue under REPEAT_MODE_ALL")
                playOnlineQueueIndex(0)
            } else {
                Log.d("MusicController", "[SKIP_NEXT] End of queue reached, requesting autoplay")
                playbackEventBus.emit(PlaybackEvent.SongCompleted(currentMediaId))
            }
        }
    }

    fun skipToPrevious() {
        cancelActiveRecovery("skipToPrevious")
        if (syncPlaybackInterceptor?.onTrackSkipRequested(isNext = false) == true) {
            return
        }
        val controller = mediaController ?: return
        Log.d("MusicController", "[SKIP_PREV] pos=${controller.currentPosition} hasPrev=${controller.hasPreviousMediaItem()} historySize=${playbackHistory.size}")
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
            return
        }
        if (!playbackHistory.isEmpty()) {
            val prevIndex = playbackHistory.pop()
            if (prevIndex in 0 until controller.mediaItemCount) {
                isNavigatingHistory = true
                controller.seekTo(prevIndex, C.TIME_UNSET)
                controller.play()
                return
            }
        }
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPrevious()
            return
        }
        val currentMediaId = controller.currentMediaItem?.mediaId ?: ""
        val currentIndex = _songQueue.indexOfFirst {
            (!currentMediaId.isNullOrBlank() && (it.id == currentMediaId || it.videoId == currentMediaId))
        }
        if (currentIndex > 0) {
            playOnlineQueueIndex(currentIndex - 1)
            return
        }
        controller.seekTo(0L)
    }

    fun seekTo(positionMs: Long) {
        if (syncPlaybackInterceptor?.onSeekRequested(positionMs) == true) {
            return
        }
        mediaController?.seekTo(positionMs)
        playbackStateManager.updatePosition(positionMs)
    }

    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L

    /** Clears the one-shot playback error after the UI has shown it. */
    fun clearPlaybackError() { playbackErrorHandler.clearPlaybackError() }

    // ── Sleep Timer ───────────────────────────────────────────────────────────

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startSleepTimer(scope, minutes) {
            mediaController?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelSleepTimer()
    }

    // ── Shuffle & Repeat ──────────────────────────────────────────────────────

    fun setShuffleEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
        playbackStateManager.updateShuffle(enabled)
        saveShuffleMode(enabled)
    }

    fun setRepeatMode(mode: Int) {
        val effectiveExoRepeat = if ((mediaController?.mediaItemCount ?: 0) <= 1 && mode == Player.REPEAT_MODE_ALL) {
            Player.REPEAT_MODE_OFF
        } else {
            mode
        }
        mediaController?.repeatMode = effectiveExoRepeat
        playbackStateManager.updateRepeatMode(mode)
        saveRepeatMode(mode)
    }

    fun cycleRepeatMode() {
        val current = mediaController?.repeatMode ?: playbackStateManager.repeatMode.value
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        setRepeatMode(next)
    }

    // ── PlaybackRouterDelegate Implementation ──────────────────────────────────

    override fun playMediaItems(mediaItems: List<MediaItem>, startIndex: Int) {
        val controller = mediaController ?: return
        if (startIndex >= 0 && startIndex < mediaItems.size) {
            val userRepeat = playbackStateManager.repeatMode.value
            val effectiveExoRepeat = if (mediaItems.size <= 1 && userRepeat == Player.REPEAT_MODE_ALL) {
                Player.REPEAT_MODE_OFF
            } else {
                userRepeat
            }
            controller.repeatMode = effectiveExoRepeat
            controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            controller.prepare()
            controller.play()

            val targetItem = mediaItems.getOrNull(startIndex)
            val mId = targetItem?.mediaId
            val uriStr = targetItem?.localConfiguration?.uri?.toString()
            val song = if (targetItem != null) {
                _songQueue.firstOrNull {
                    (!mId.isNullOrBlank() && (it.id == mId || it.videoId == mId)) ||
                    (!uriStr.isNullOrBlank() && it.path == uriStr)
                } ?: if (mediaItems.size == _songQueue.size && startIndex in _songQueue.indices) {
                    val candidate = _songQueue[startIndex]
                    if (mId.isNullOrBlank() || candidate.id == mId || candidate.videoId == mId) {
                        candidate
                    } else null
                } else null
            } else null

            val effectiveSong = song ?: if (targetItem != null) {
                val meta = targetItem.mediaMetadata
                val art = meta.artworkUri?.toString() ?: mId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
                SongEntity(
                    id = mId ?: java.util.UUID.randomUUID().toString(),
                    title = meta.title?.toString() ?: "Unknown Track",
                    artist = meta.artist?.toString() ?: "Unknown Artist",
                    album = meta.albumTitle?.toString() ?: "Online",
                    duration = controller.duration.coerceAtLeast(0L),
                    path = uriStr ?: "",
                    albumArt = art,
                    dateAdded = System.currentTimeMillis(),
                    videoId = mId
                )
            } else null

            if (effectiveSong != null) {
                val resolvedArt = effectiveSong.albumArt ?: effectiveSong.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
                val songWithArt = if (effectiveSong.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
                    effectiveSong.copy(albumArt = resolvedArt)
                } else {
                    effectiveSong
                }
                playbackStateManager.updateCurrentSong(songWithArt)
                playbackStateManager.updatePosition(0L)
                playbackStateManager.updatePlayingState(true)
            }
        }
    }

    override fun prepareMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
        onPrepared: () -> Unit
    ) {
        val controller = mediaController ?: return
        if (startIndex >= 0 && startIndex < mediaItems.size) {
            controller.setMediaItems(mediaItems, startIndex, startPositionMs)
            controller.playWhenReady = false
            controller.prepare()

            val targetItem = mediaItems.getOrNull(startIndex)
            val mId = targetItem?.mediaId
            val uriStr = targetItem?.localConfiguration?.uri?.toString()
            val song = if (targetItem != null) {
                _songQueue.firstOrNull {
                    (!mId.isNullOrBlank() && (it.id == mId || it.videoId == mId)) ||
                    (!uriStr.isNullOrBlank() && it.path == uriStr)
                }
            } else null ?: if (mediaItems.size == _songQueue.size && startIndex in _songQueue.indices) {
                _songQueue[startIndex]
            } else null

            if (song != null) {
                val resolvedArt = song.albumArt ?: song.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
                val songWithArt = if (song.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
                    song.copy(albumArt = resolvedArt)
                } else {
                    song
                }
                playbackStateManager.updateCurrentSong(songWithArt)
                playbackStateManager.updatePosition(startPositionMs)
                playbackStateManager.updatePlayingState(false)
            }

            if (controller.playbackState == Player.STATE_READY) {
                onPrepared()
            } else {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                            controller.removeListener(this)
                            onPrepared()
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        controller.removeListener(this)
                        Log.e("MusicController", "prepareMediaItems player error", error)
                    }
                }
                controller.addListener(listener)
            }
        }
    }

    fun prepareForSync(song: SongEntity, startPositionMs: Long = 0L, onPrepared: () -> Unit = {}) {
        val resolvedArt = song.albumArt ?: song.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
        val songWithArt = if (song.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
            song.copy(albumArt = resolvedArt)
        } else {
            song
        }

        if (!_songQueue.any { it.id == songWithArt.id || (it.videoId != null && it.videoId == songWithArt.videoId) }) {
            _songQueue.clear()
            _songQueue.add(songWithArt)
            _playbackQueue.value = _songQueue.toList()
        }
        playbackStateManager.updateCurrentSong(songWithArt)

        val request = PlayRequest(
            songId = songWithArt.id,
            title = songWithArt.title,
            artist = songWithArt.artist,
            playbackSource = if (songWithArt.path.startsWith("/") || songWithArt.path.startsWith("content://") || java.io.File(songWithArt.path).exists()) PlaybackSourceType.LOCAL else PlaybackSourceType.ONLINE,
            localUri = songWithArt.path,
            streamUrl = if (songWithArt.path.startsWith("http")) songWithArt.path else null,
            albumArt = songWithArt.albumArt
        )
        playbackRouter.prepare(scope, this, request, startPositionMs, onPrepared)
    }

    fun startPreparedSyncPlayback(positionMs: Long = 0L) {
        val controller = mediaController ?: return
        if (positionMs > 0L && Math.abs(controller.currentPosition - positionMs) > 1000L) {
            controller.seekTo(positionMs)
        }
        controller.playWhenReady = true
        controller.play()
        playbackStateManager.updatePlayingState(true)
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        val controller = mediaController ?: return
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            if (controller.mediaItemCount == _songQueue.size && index in 0 until controller.mediaItemCount) {
                controller.replaceMediaItem(index, mediaItem)
            }
        } else {
            scope.launch(Dispatchers.Main) {
                if (controller.mediaItemCount == _songQueue.size && index in 0 until controller.mediaItemCount) {
                    controller.replaceMediaItem(index, mediaItem)
                }
            }
        }
    }

    override fun stopPlayback() {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            mediaController?.stop()
            playbackStateManager.updatePlayingState(false)
            playbackStateManager.updateCurrentSong(null)
            playbackStateManager.updateCurrentOnlineSong(null)
        } else {
            scope.launch(Dispatchers.Main) {
                mediaController?.stop()
                playbackStateManager.updatePlayingState(false)
                playbackStateManager.updateCurrentSong(null)
                playbackStateManager.updateCurrentOnlineSong(null)
            }
        }
    }

    override fun getMediaItemAt(index: Int): MediaItem? {
        val controller = mediaController ?: return null
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            return null
        }
        if (index >= 0 && index < controller.mediaItemCount) {
            return controller.getMediaItemAt(index)
        }
        return null
    }

    override fun setCustomError(message: String) {
        playbackErrorHandler.setCustomError(message)
    }

    override fun getMediaItemCount(): Int {
        val controller = mediaController ?: return 0
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            return _songQueue.size
        }
        return controller.mediaItemCount
    }

    override fun getNextPlayRequest(): PlayRequest? {
        val currentMediaId = mediaController?.currentMediaItem?.mediaId
        val currentIndex = _songQueue.indexOfFirst { it.id == currentMediaId || it.videoId == currentMediaId }
        if (currentIndex != -1 && currentIndex + 1 < _songQueue.size) {
            val nextEntity = _songQueue[currentIndex + 1]
            val vId = nextEntity.videoId ?: nextEntity.id
            return PlayRequest(
                songId = vId,
                title = nextEntity.title,
                artist = nextEntity.artist,
                playbackSource = if (nextEntity.path.startsWith("http") || nextEntity.path.startsWith("online://")) PlaybackSourceType.ONLINE else PlaybackSourceType.LOCAL,
                localUri = nextEntity.path,
                albumArt = nextEntity.albumArt
            )
        }
        return null
    }

    // ── ARIA SDK Helpers ──────────────────────────────────────────────────────

    fun getMediaController(): MediaController? = mediaController

    fun getSongQueue(): List<SongEntity> = _songQueue

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
    }

    fun getPlaybackSpeed(): Float {
        return mediaController?.playbackParameters?.speed ?: 1.0f
    }

    fun setPlayerVolume(volume: Float) {
        mediaController?.volume = volume
    }

    fun getPlayerVolume(): Float {
        return mediaController?.volume ?: 1.0f
    }

    fun addSongToQueue(song: SongEntity) {
        val controller = mediaController ?: return
        _songQueue.add(song)
        _playbackQueue.value = _songQueue.toList()
        val isOnline = song.path.startsWith("online://") || song.path.startsWith("http") || (song.videoId != null && song.videoId.length == 11)
        if (!isOnline && song.path.isNotBlank()) {
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.path))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .apply {
                            if (!song.albumArt.isNullOrBlank()) {
                                setArtworkUri(Uri.parse(song.albumArt))
                            }
                        }
                        .build()
                )
                .build()
            controller.addMediaItem(mediaItem)
        }
    }

    fun playQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in _songQueue.indices) {
            val target = _songQueue[index]
            val isOnline = target.path.startsWith("http") ||
                           target.path.startsWith("online://") ||
                           (target.videoId != null && target.videoId.length == 11)
            if (isOnline || controller.mediaItemCount <= 1) {
                playOnlineQueueIndex(index)
                return
            }
        }
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, C.TIME_UNSET)
            controller.play()
        }
    }

    companion object {
        const val PREFS_NAME = "playback_preferences"
        const val KEY_REPEAT_MODE = "playback_repeat_mode"
        const val KEY_SHUFFLE_MODE = "playback_shuffle_mode"
        const val MAX_ONLINE_RETRIES = 3
        val RETRY_DELAYS_MS = longArrayOf(0L, 750L, 1750L)
    }
}


