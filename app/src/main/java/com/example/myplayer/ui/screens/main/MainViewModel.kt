package com.example.myplayer.ui.screens.main

import androidx.lifecycle.ViewModel
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong

@HiltViewModel
class MainViewModel @Inject constructor(
    val musicController: MusicController,
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository,
    private val innertubeApi: InnertubeApi
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                downloadRepository.performIntegrityCheck()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Integrity check failed on startup", e)
            }
        }
    }

    val currentSong = musicController.currentSong
    val currentOnlineSong = musicController.currentOnlineSong
    val isPlaying = musicController.isPlaying
    val currentPosition = musicController.currentPosition
    val currentDuration = musicController.currentDuration
    val sleepTimerRemainingSeconds = musicController.sleepTimerRemainingSeconds
    val isShuffleOn = musicController.isShuffleOn
    val repeatMode = musicController.repeatMode
    val currentAudioQuality = musicController.currentAudioQuality
    val playbackQueue = musicController.playbackQueue
    val playbackError = musicController.playbackError
    fun clearPlaybackError() = musicController.clearPlaybackError()

    fun playSong(song: com.example.myplayer.data.repository.PlayableSong) = musicController.playSong(song)
    fun playPlaylist(songs: List<com.example.myplayer.data.repository.PlayableSong>, startIndex: Int) = musicController.playPlaylist(songs, startIndex)
    fun playQueueIndex(index: Int) = musicController.playQueueIndex(index)
    fun playPause() = musicController.playPause()
    fun skipToNext() = musicController.skipToNext()
    fun skipToPrevious() = musicController.skipToPrevious()
    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)
    fun startSleepTimer(minutes: Int) = musicController.startSleepTimer(minutes)
    fun cancelSleepTimer() = musicController.cancelSleepTimer()
    fun toggleShuffle() = musicController.setShuffleEnabled(!musicController.isShuffleOn.value)
    fun cycleRepeatMode() = musicController.cycleRepeatMode()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> = combine(currentSong, currentOnlineSong) { local, online ->
        local?.id ?: online?.videoId
    }.flatMapLatest { songId ->
        if (songId != null) {
            musicRepository.isFavorite(songId)
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleFavorite() {
        val local = currentSong.value
        val online = currentOnlineSong.value
        val playable = local?.let { PlayableSong.Local(it) }
            ?: online?.let { PlayableSong.Online(it.videoId, it.title, it.artist, it.durationMs, it.thumbnailUrl) }
            ?: return
            
        val currentlyFavorite = isFavorite.value
        viewModelScope.launch {
            musicRepository.toggleFavorite(playable, currentlyFavorite)
        }
    }

    // ── Downloads ───────────────────────────────────────────────────────────
    val downloadedIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { downloads -> downloads.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // videoId currently being resolved/queued for download
    private val _resolvingDownloadId = MutableStateFlow<String?>(null)

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()
    fun clearDownloadError() { _downloadError.value = null }

    /**
     * Resolves the downloadable metadata for the currently playing track regardless
     * of whether it was initiated as a direct OnlineSong or mapped as a SongEntity.
     */
    fun getDownloadableTrack(
        online: OnlineSong?,
        local: SongEntity?,
        downloadedSet: Set<String>
    ): OnlineSong? {
        if (online != null && online.videoId.isNotBlank()) {
            return online
        }
        if (local != null) {
            val videoId = local.videoId?.takeIf { it.isNotBlank() }
                ?: if (local.path.startsWith("online://")) {
                    local.path.removePrefix("online://").takeIf { it.isNotBlank() }
                } else null
                ?: if (downloadedSet.contains(local.id)) {
                    local.id
                } else null
                ?: if (local.id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                    local.id
                } else null

            if (videoId != null) {
                val streamUrl = if (local.path.startsWith("http://") || local.path.startsWith("https://")) {
                    local.path
                } else null
                return OnlineSong(
                    videoId = videoId,
                    title = local.title,
                    artist = local.artist,
                    thumbnailUrl = local.albumArt ?: "",
                    durationMs = local.duration,
                    streamUrl = streamUrl
                )
            }
        }
        return null
    }

    /** True if any song is currently loaded (canDownload flag for UI). */
    val isCurrentSongOnline: StateFlow<Boolean> = combine(currentOnlineSong, currentSong, downloadedIds) { online, local, _ ->
        online != null || local != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True if the currently playing online song is already downloaded. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentSongDownloaded: StateFlow<Boolean> =
        combine(currentOnlineSong, currentSong, downloadedIds) { online, local, ids ->
            val track = getDownloadableTrack(online, local, ids)
            track != null && ids.contains(track.videoId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True while the current online song's download is being resolved or is in progress. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentSongDownloading: StateFlow<Boolean> =
        combine(currentOnlineSong, currentSong, downloadRepository.downloadProgress, _resolvingDownloadId) { online, local, progress, resolving ->
            val track = getDownloadableTrack(online, local, downloadedIds.value)
            val id = track?.videoId ?: ""
            id.isNotEmpty() && (resolving == id || progress.containsKey(id))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Resolves the stream URL for the currently playing online song and queues a download.
     */
    fun downloadCurrentSong() {
        val currentLocal = currentSong.value
        val currentOnline = currentOnlineSong.value
        val song = getDownloadableTrack(currentOnline, currentLocal, downloadedIds.value)

        if (song == null) {
            // Track is a pure local file already stored on device storage
            if (currentLocal != null) {
                Log.i("MainViewModel", "[DOWNLOAD] Song '${currentLocal.title}' is pure local storage file")
                _downloadError.value = "This song is already available on your device storage"
            } else {
                Log.w("MainViewModel", "[DOWNLOAD] No downloadable track in Now Playing")
            }
            return
        }

        val videoId = song.videoId
        Log.i("MainViewModel", "[DOWNLOAD] Click received for '${song.title}' ($videoId)")

        // Case 3 — Already downloaded
        if (downloadedIds.value.contains(videoId)) {
            Log.i("MainViewModel", "[DOWNLOAD] Song '$videoId' already downloaded, skipping")
            _downloadError.value = "Already downloaded and available offline"
            return
        }

        // Case 2 — Already downloading (prevent duplicate jobs)
        if (_resolvingDownloadId.value == videoId || downloadRepository.isDownloading(videoId)) {
            Log.i("MainViewModel", "[DOWNLOAD] Song '$videoId' is already downloading, skipping duplicate")
            return
        }

        viewModelScope.launch {
            if (downloadRepository.isDownloaded(videoId)) return@launch

            _resolvingDownloadId.value = videoId
            Log.i("MainViewModel", "[DOWNLOAD] Starting download resolution for: ${song.title} ($videoId)")

            try {
                var streamUrl = song.streamUrl?.takeIf { it.isNotBlank() }
                if (streamUrl == null) {
                    streamUrl = innertubeApi.getCachedStreamUrl(videoId)
                }
                if (streamUrl == null) {
                    Log.i("MainViewModel", "[DOWNLOAD] Resolving stream URL via InnertubeApi for: $videoId")
                    streamUrl = innertubeApi.getStreamUrl(videoId)
                } else {
                    Log.i("MainViewModel", "[DOWNLOAD] Reusing active session stream URL for: $videoId")
                }

                if (streamUrl.isNullOrBlank()) {
                    // Case 4 — Missing URL
                    val msg = "Download failed: no valid media URL"
                    Log.e("MainViewModel", "[DOWNLOAD] $msg for '${song.title}' ($videoId)")
                    _downloadError.value = msg
                    return@launch
                }

                Log.i("MainViewModel", "[DOWNLOAD] Enqueuing download in DownloadRepository for '${song.title}'")
                val enqueued = downloadRepository.startDownload(song.copy(streamUrl = streamUrl))
                if (!enqueued) {
                    val msg = "Download failed: could not schedule download"
                    Log.e("MainViewModel", "[DOWNLOAD] $msg for '${song.title}' ($videoId)")
                    _downloadError.value = msg
                } else {
                    Log.i("MainViewModel", "[DOWNLOAD] Successfully enqueued download for '${song.title}'")
                }
            } catch (e: com.example.myplayer.data.online.YouTubeStreamBlockedException) {
                // Case 5 — Download failure: do not remain stuck in downloading state
                val msg = e.message ?: "Download failed: YouTube stream blocked"
                Log.e("MainViewModel", "[DOWNLOAD] $msg for '${song.title}' ($videoId)", e)
                _downloadError.value = msg
            } catch (e: Exception) {
                val msg = "Download failed: ${e.message ?: "Unknown error"}"
                Log.e("MainViewModel", "[DOWNLOAD] $msg for '${song.title}' ($videoId)", e)
                _downloadError.value = msg
            } finally {
                _resolvingDownloadId.value = null
            }
        }
    }

    fun removeCurrentSongDownload() {
        val song = getDownloadableTrack(currentOnlineSong.value, currentSong.value, downloadedIds.value) ?: return
        Log.i("MainViewModel", "[DOWNLOAD] Removing download for '${song.title}' (${song.videoId})")
        viewModelScope.launch {
            downloadRepository.deleteDownloadById(song.videoId)
        }
    }
}
