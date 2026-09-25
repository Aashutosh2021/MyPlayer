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
    private val downloadedSongDao: DownloadedSongDao
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
                    Log.d("MusicController", "Received PlayRequestReady event: playing ${event.requests.size} items")
                    playbackRouter.play(scope, this@MusicController, event.requests, event.startIndex)
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
            controller.repeatMode = savedRepeat
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
                }

                updateCurrentSong()
                playbackStateManager.updatePosition(0L)
                playbackStateManager.updateDuration(0L)
                playbackCompletionGuard.reset()

                mediaItem?.let { item ->
                    val isOnline = item.localConfiguration?.uri?.toString()?.startsWith("http") == true ||
                                   item.localConfiguration?.uri?.toString()?.startsWith("online://") == true
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
                if (isPlaying) startPositionUpdater() else stopPositionUpdater()
                val pos = mediaController?.currentPosition ?: 0L
                syncPlaybackInterceptor?.onPlayerIsPlayingChanged(isPlaying, pos)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackStateManager.updateDuration(mediaController?.duration ?: 0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    val mediaId = mediaController?.currentMediaItem?.mediaId
                    if (!playbackCompletionGuard.isAlreadyHandled(mediaId)) {
                        handlePlaybackEnded()
                    } else {
                        Log.d("MusicController", "PlaybackCompletionGuard rejected duplicate completion for $mediaId")
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("MusicController", "Player error for ${mediaController?.currentMediaItem?.mediaId}: ${error.errorCodeName}")
                val controller = mediaController ?: return
                val hasNext = controller.hasNextMediaItem()
                val mediaId = controller.currentMediaItem?.mediaId ?: ""
                playbackEventBus.emit(PlaybackEvent.SongError(mediaId, error.message ?: "Unknown error"))
                playbackErrorHandler.handleError(error, hasNext)
                if (hasNext) {
                    controller.seekToNext()
                    controller.prepare()
                    controller.play()
                } else {
                    playbackStateManager.updatePlayingState(false)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                playbackStateManager.updateShuffle(shuffleModeEnabled)
                saveShuffleMode(shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                playbackStateManager.updateRepeatMode(repeatMode)
                saveRepeatMode(repeatMode)
            }
        })
    }

    private fun handlePlaybackEnded() {
        val mediaId = mediaController?.currentMediaItem?.mediaId ?: ""
        playbackEventBus.emit(PlaybackEvent.SongCompleted(mediaId))
        // Auto-recommendation / autoplay disabled to keep app lightweight, fast, and user-directed
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
            }
        } else null ?: if (controller.mediaItemCount == _songQueue.size && controller.currentMediaItemIndex in _songQueue.indices) {
            _songQueue[controller.currentMediaItemIndex]
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
    }

    // ── Local Playback ────────────────────────────────────────────────────────

    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
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
        val clampedIndex = startIndex.coerceIn(0, entities.size - 1)
        playSongs(entities, clampedIndex)
    }

    // ── Online Streaming ──────────────────────────────────────────────────────

    fun playOnlineSong(song: OnlineSong) {
        val artUrl = song.thumbnailUrl.ifBlank { "https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg" }
        val syntheticSong = SongEntity(
            id = song.videoId,
            title = song.title,
            artist = song.artist,
            album = "Online",
            duration = song.durationMs,
            path = song.streamUrl ?: "online://${song.videoId}",
            albumArt = artUrl,
            dateAdded = System.currentTimeMillis(),
            videoId = song.videoId
        )

        if (syncPlaybackInterceptor?.onPlayRequested(syntheticSong, 0) == true) {
            Log.d("MusicController", "[SYNC PLAY INTERCEPT] Intercepted online playback request for: ${syntheticSong.title}")
            _songQueue.clear()
            _songQueue.add(syntheticSong)
            _playbackQueue.value = _songQueue.toList()
            playbackStateManager.updateCurrentSong(syntheticSong)
            return
        }

        val request = PlayRequest(
            songId = song.videoId,
            title = song.title,
            artist = song.artist,
            playbackSource = PlaybackSourceType.ONLINE,
            localUri = "online://${song.videoId}",
            streamUrl = song.streamUrl,
            albumArt = artUrl
        )

        _songQueue.clear()
        _songQueue.add(syntheticSong)
        _playbackQueue.value = _songQueue.toList()
        playbackStateManager.updateCurrentSong(null)
        playbackStateManager.updateCurrentOnlineSong(song)

        playbackRouter.play(scope, this, listOf(request), 0)
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
        }
    }

    fun skipToPrevious() {
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
        mediaController?.repeatMode = mode
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
        if (controller.mediaItemCount == _songQueue.size && index < controller.mediaItemCount) {
            controller.replaceMediaItem(index, mediaItem)
        }
    }

    override fun stopPlayback() {
        mediaController?.stop()
        playbackStateManager.updatePlayingState(false)
        playbackStateManager.updateCurrentSong(null)
        playbackStateManager.updateCurrentOnlineSong(null)
    }

    override fun getMediaItemAt(index: Int): MediaItem? {
        val controller = mediaController ?: return null
        if (index >= 0 && index < controller.mediaItemCount) {
            return controller.getMediaItemAt(index)
        }
        return null
    }

    override fun setCustomError(message: String) {
        playbackErrorHandler.setCustomError(message)
    }

    override fun getMediaItemCount(): Int {
        return mediaController?.mediaItemCount ?: 0
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

    fun playQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, C.TIME_UNSET)
            controller.play()
        }
    }

    companion object {
        const val PREFS_NAME = "playback_preferences"
        const val KEY_REPEAT_MODE = "playback_repeat_mode"
        const val KEY_SHUFFLE_MODE = "playback_shuffle_mode"
    }
}


