package com.example.myplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.data.recommendation.RecommendationCoordinator
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
    private val recommendationCoordinator: RecommendationCoordinator
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Keep a local copy of the queue so we can map MediaItem → SongEntity
    private val _songQueue = mutableListOf<SongEntity>()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _currentOnlineSong = MutableStateFlow<OnlineSong?>(null)
    val currentOnlineSong: StateFlow<OnlineSong?> = _currentOnlineSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()

    // --- Sleep Timer ---
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long>(-1L)
    val sleepTimerRemainingSeconds: StateFlow<Long> = _sleepTimerRemainingSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var positionJob: Job? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            setupController()
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateCurrentSong()
                _currentPosition.value = 0L
                _currentDuration.value = 0L
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                _isPlaying.value = isPlaying
                if (isPlaying) startPositionUpdater() else stopPositionUpdater()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _currentDuration.value = mediaController?.duration ?: 0L
                }
            }
        })
    }

    private fun startPositionUpdater() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _currentPosition.value = mediaController?.currentPosition ?: 0L
                _currentDuration.value = mediaController?.duration?.takeIf { it > 0 } ?: _currentDuration.value
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdater() {
        positionJob?.cancel()
        positionJob = null
        _currentPosition.value = mediaController?.currentPosition ?: _currentPosition.value
    }

    private fun updateCurrentSong() {
        val mediaId = mediaController?.currentMediaItem?.mediaId ?: return
        val localSong = _songQueue.firstOrNull { it.id == mediaId }
        if (localSong != null) {
            _currentSong.value = localSong
            _currentOnlineSong.value = null
        }
        // Online songs update _currentOnlineSong directly in playOnlineSong()
    }

    // ── Local Playback ────────────────────────────────────────────────────────

    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0) {
        val controller = mediaController ?: return
        _songQueue.clear()
        _songQueue.addAll(songs)
        _currentOnlineSong.value = null

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.path))   // Uri.parse handles both file:// and content:// URIs
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
        }
        controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        controller.prepare()
        controller.play()

        _currentSong.value = songs.getOrNull(startIndex)
        _currentPosition.value = 0L
        _isPlaying.value = true
    }

    /**
     * Unified play dispatcher: plays any PlayableSong regardless of source.
     */
    fun playSong(song: PlayableSong) {
        when (song) {
            is PlayableSong.Local -> playSongs(listOf(song.entity), 0)
            is PlayableSong.Downloaded -> playDownloadedSong(song.entity)
            is PlayableSong.Online -> {
                Log.w("MusicController", "playSong called with Online song — no stream URL available. Use OnlineSearchViewModel.streamSong() instead.")
            }
        }
    }

    /**
     * Queue an entire playlist and start playback from [startIndex].
     * Supports Local and Downloaded songs. Converts all to SongEntity for ExoPlayer queue.
     */
    fun playPlaylist(songs: List<PlayableSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val entities = songs.mapNotNull { playable ->
            when (playable) {
                is PlayableSong.Local -> playable.entity
                is PlayableSong.Downloaded -> SongEntity(
                    id = playable.entity.id,
                    title = playable.entity.title,
                    artist = playable.entity.artist,
                    album = "",
                    duration = playable.entity.durationMs,
                    path = playable.entity.localPath,
                    albumArt = playable.entity.thumbnailUrl,
                    dateAdded = playable.entity.downloadedAt
                )
                is PlayableSong.Online -> null // Online songs need stream resolution, can't queue directly
            }
        }
        if (entities.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, entities.size - 1)
        playSongs(entities, clampedIndex)
    }

    // ── Online Streaming ──────────────────────────────────────────────────────

    fun playOnlineSong(song: OnlineSong) {
        val controller = mediaController ?: return
        val streamUrl = song.streamUrl ?: run {
            Log.e("MusicController", "No stream URL for ${song.videoId}")
            return
        }

        _songQueue.clear()
        _currentSong.value = null
        _currentOnlineSong.value = song

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.videoId)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .apply {
                        if (song.thumbnailUrl.isNotBlank()) {
                            setArtworkUri(Uri.parse(song.thumbnailUrl))
                        }
                    }
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        _currentPosition.value = 0L
        _isPlaying.value = true

        // Phase 4.5: Recommendation Validation & Stability Sprint
        // Coordinator handles all background dispatch and queue management
        recommendationCoordinator.onPlaybackStarted(song)
    }

    fun playDownloadedSong(song: DownloadedSongEntity) {
        val localSong = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = "",
            duration = song.durationMs,
            path = song.localPath,
            albumArt = song.thumbnailUrl,
            dateAdded = song.downloadedAt
        )
        playSongs(listOf(localSong))
    }

    // ── Controls ──────────────────────────────────────────────────────────────

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        
        // Try to seek to next in the current queue
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        } else {
            // Fallback: Play a random song from the local library
            scope.launch {
                musicRepository.getAllSongs().firstOrNull()?.let { songs ->
                    if (songs.isNotEmpty()) {
                        val randomSong = songs.random()
                        playSongs(listOf(randomSong), 0)
                    }
                }
            }
        }
    }

    fun skipToPrevious() { mediaController?.seekToPrevious() }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L

    // ── Sleep Timer ───────────────────────────────────────────────────────────

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive) {
                mediaController?.pause()
                _sleepTimerRemainingSeconds.value = -1L
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = -1L
    }
}
