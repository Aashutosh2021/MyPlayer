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

    private var positionJob: Job? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            setupController()
        }, MoreExecutors.directExecutor())

        scope.launch {
            playbackEventBus.events.collect { event ->
                if (event is PlaybackEvent.PlayRequestReady) {
                    Log.d("MusicController", "Received PlayRequestReady event: playing ${event.requests.size} items")
                    playbackRouter.play(scope, this@MusicController, event.requests, event.startIndex)
                }
            }
        }
    }

    private fun setupController() {
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val controller = mediaController
                if (controller != null) {
                    val currentIndex = controller.currentMediaItemIndex
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
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                playbackStateManager.updateRepeatMode(repeatMode)
            }
        })
    }

    private fun handlePlaybackEnded() {
        val mediaId = mediaController?.currentMediaItem?.mediaId ?: ""
        playbackEventBus.emit(PlaybackEvent.SongCompleted(mediaId))
        playbackEventBus.emit(PlaybackEvent.AutoplayRequested)
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
        val mediaId = mediaController?.currentMediaItem?.mediaId ?: return
        val localSong = _songQueue.firstOrNull { it.id == mediaId }
        if (localSong != null) {
            playbackStateManager.updateCurrentSong(localSong)
        }
    }

    // ── Local Playback ────────────────────────────────────────────────────────

    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _songQueue.clear()
        _songQueue.addAll(songs)
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

        playbackRouter.play(scope, this, requests, startIndex)
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
        val request = PlayRequest(
            songId = song.videoId,
            title = song.title,
            artist = song.artist,
            playbackSource = PlaybackSourceType.ONLINE,
            localUri = "online://${song.videoId}",
            streamUrl = song.streamUrl,
            albumArt = song.thumbnailUrl
        )

        _songQueue.clear()
        playbackStateManager.updateCurrentSong(null)
        playbackStateManager.updateCurrentOnlineSong(song)

        playbackRouter.play(scope, this, listOf(request), 0)
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

            val entities = downloads.map { d ->
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
            val startIndex = downloads.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playSongs(entities, startIndex)
        }
    }

    // ── Controls ──────────────────────────────────────────────────────────────

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        val currentMediaId = controller.currentMediaItem?.mediaId ?: ""
        if (currentMediaId.isNotEmpty()) {
            playbackEventBus.emit(PlaybackEvent.SongSkipped(currentMediaId))
        }
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        } else {
            scope.launch {
                val songs = withContext(Dispatchers.IO) {
                    musicRepository.getAllSongs().firstOrNull()
                }
                songs?.let {
                    if (it.isNotEmpty()) {
                        playSongs(listOf(it.random()), 0)
                    }
                }
            }
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (!playbackHistory.isEmpty()) {
            val prevIndex = playbackHistory.pop()
            if (prevIndex in 0 until controller.mediaItemCount) {
                isNavigatingHistory = true
                controller.seekTo(prevIndex, C.TIME_UNSET)
                controller.play()
                return
            }
        }
        controller.seekTo(0L)
    }

    fun seekTo(positionMs: Long) {
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
    }

    fun cycleRepeatMode() {
        val next = when (mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        mediaController?.repeatMode = next
        playbackStateManager.updateRepeatMode(next)
    }

    // ── PlaybackRouterDelegate Implementation ──────────────────────────────────

    override fun playMediaItems(mediaItems: List<MediaItem>, startIndex: Int) {
        val controller = mediaController ?: return
        if (startIndex >= 0) {
            controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            controller.prepare()
            controller.play()

            if (startIndex < _songQueue.size) {
                playbackStateManager.updateCurrentSong(_songQueue[startIndex])
                playbackStateManager.updatePosition(0L)
                playbackStateManager.updatePlayingState(true)
            }
        }
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
}
