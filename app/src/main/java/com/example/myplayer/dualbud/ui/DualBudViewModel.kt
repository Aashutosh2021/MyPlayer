package com.example.myplayer.dualbud.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.YouTubeStreamBlockedException
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.dualbud.DualChannelPlaybackManager
import com.example.myplayer.dualbud.model.DualBudModeState
import com.example.myplayer.dualbud.model.DualChannelId
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DualBudViewModel @Inject constructor(
    private val manager: DualChannelPlaybackManager,
    private val musicController: MusicController,
    private val innertubeApi: InnertubeApi,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val uiState: StateFlow<DualBudModeState> = manager.state

    // Downloaded songs from downloaded section only
    val downloadedSongs: StateFlow<List<DownloadedSongEntity>> = downloadRepository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Online search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<OnlineSong>>(emptyList())
    val searchResults: StateFlow<List<OnlineSong>> = _searchResults.asStateFlow()

    private val _loadingChannel = MutableStateFlow<DualChannelId?>(null)
    val loadingChannel: StateFlow<DualChannelId?> = _loadingChannel.asStateFlow()

    private val _resolvingSongId = MutableStateFlow<String?>(null)
    val resolvingSongId: StateFlow<String?> = _resolvingSongId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Auto-enable when navigating to Dual Bud screen if not already enabled
        manager.enableDualBudMode()
        pauseNormalPlaybackIfPlaying()
    }

    private fun pauseNormalPlaybackIfPlaying() {
        if (musicController.isPlaying.value) {
            musicController.playPause()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            try {
                val page = innertubeApi.search(query.trim())
                _searchResults.value = page.songs
            } catch (e: Exception) {
                Log.e("DualBudViewModel", "Search failed for '$query'", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun selectOnlineSong(channel: DualChannelId, song: OnlineSong, onSelected: () -> Unit = {}) {
        pauseNormalPlaybackIfPlaying()
        viewModelScope.launch {
            _loadingChannel.value = channel
            _resolvingSongId.value = song.videoId
            try {
                val streamUrl = innertubeApi.getStreamUrl(song.videoId)
                if (!streamUrl.isNullOrBlank()) {
                    val artUrl = song.thumbnailUrl.ifBlank {
                        "https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg"
                    }
                    val songEntity = SongEntity(
                        id = song.videoId,
                        title = song.title,
                        artist = song.artist,
                        album = "YouTube Music",
                        duration = song.durationMs,
                        path = streamUrl,
                        albumArt = artUrl,
                        dateAdded = System.currentTimeMillis(),
                        videoId = song.videoId
                    )
                    manager.loadSong(channel, songEntity, autoPlay = true)
                    onSelected()
                } else {
                    _errorMessage.value = "Could not resolve stream URL for '${song.title}'"
                }
            } catch (e: YouTubeStreamBlockedException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                Log.e("DualBudViewModel", "Streaming failed for '${song.title}'", e)
                _errorMessage.value = "Playback failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loadingChannel.value = null
                _resolvingSongId.value = null
            }
        }
    }

    fun selectDownloadedSong(channel: DualChannelId, downloaded: DownloadedSongEntity, onSelected: () -> Unit = {}) {
        pauseNormalPlaybackIfPlaying()
        val songEntity = SongEntity(
            id = downloaded.id,
            title = downloaded.title,
            artist = downloaded.artist,
            album = downloaded.album.ifBlank { "Downloaded" },
            duration = downloaded.durationMs,
            path = downloaded.localPath,
            albumArt = downloaded.thumbnailUrl,
            dateAdded = downloaded.downloadedAt,
            videoId = downloaded.id
        )
        manager.loadSong(channel, songEntity, autoPlay = true)
        onSelected()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun enableDualBudMode() {
        pauseNormalPlaybackIfPlaying()
        manager.enableDualBudMode()
    }

    fun disableDualBudMode() {
        manager.disableDualBudMode()
    }

    fun toggleDualBudMode() {
        if (uiState.value.isEnabled) {
            disableDualBudMode()
        } else {
            enableDualBudMode()
        }
    }

    fun togglePlayPause(channel: DualChannelId) {
        pauseNormalPlaybackIfPlaying()
        manager.togglePlayPause(channel)
    }

    fun seekTo(channel: DualChannelId, positionMs: Long) {
        manager.seekTo(channel, positionMs)
    }

    fun setVolume(channel: DualChannelId, volume: Float) {
        manager.setVolume(channel, volume)
    }

    fun swapChannels() {
        manager.swapChannels()
    }
}
