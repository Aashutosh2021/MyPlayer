package com.example.myplayer.ui.screens.main

import androidx.lifecycle.ViewModel
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.myplayer.data.online.InnertubeApi
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

    val currentSong = musicController.currentSong
    val currentOnlineSong = musicController.currentOnlineSong
    val isPlaying = musicController.isPlaying
    val currentPosition = musicController.currentPosition
    val currentDuration = musicController.currentDuration
    val sleepTimerRemainingSeconds = musicController.sleepTimerRemainingSeconds
    val isShuffleOn = musicController.isShuffleOn
    val repeatMode = musicController.repeatMode

    fun playSong(song: com.example.myplayer.data.repository.PlayableSong) = musicController.playSong(song)
    fun playPlaylist(songs: List<com.example.myplayer.data.repository.PlayableSong>, startIndex: Int) = musicController.playPlaylist(songs, startIndex)
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
    private val downloadedIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { downloads -> downloads.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // videoId currently being resolved/queued for download
    private val _resolvingDownloadId = MutableStateFlow<String?>(null)

    /** True if the currently playing song is an online (downloadable) song. */
    val isCurrentSongOnline: StateFlow<Boolean> = currentOnlineSong
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True if the currently playing online song is already downloaded. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentSongDownloaded: StateFlow<Boolean> =
        combine(currentOnlineSong, downloadedIds) { online, ids ->
            online != null && ids.contains(online.videoId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True while the current online song's download is being resolved or is in progress. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentSongDownloading: StateFlow<Boolean> =
        combine(currentOnlineSong, downloadRepository.downloadProgress, _resolvingDownloadId) { online, progress, resolving ->
            online != null && (resolving == online.videoId || progress.containsKey(online.videoId))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Resolves the stream URL for the currently playing online song and queues a download.
     */
    fun downloadCurrentSong() {
        val song = currentOnlineSong.value ?: return
        viewModelScope.launch {
            if (downloadRepository.isDownloaded(song.videoId)) return@launch
            _resolvingDownloadId.value = song.videoId
            try {
                val url = innertubeApi.getStreamUrl(song.videoId)
                if (url != null) {
                    downloadRepository.startDownload(song.copy(streamUrl = url))
                } else {
                    Log.e("MainViewModel", "Could not resolve download URL for '${song.title}'")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Download failed for '${song.title}'", e)
            } finally {
                _resolvingDownloadId.value = null
            }
        }
    }
}
