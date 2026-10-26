package com.example.myplayer.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.PlayedSongHistoryDao
import com.example.myplayer.data.local.entity.PlayedSongHistoryEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.OnlineSearchRepository
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val searchRepository: OnlineSearchRepository,
    private val downloadRepository: DownloadRepository,
    private val innertubeApi: InnertubeApi,
    private val musicController: MusicController,
    private val musicRepository: MusicRepository,
    private val playedSongHistoryDao: PlayedSongHistoryDao,
    private val downloadedSongDao: DownloadedSongDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val fifteenDaysCutoff = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000L

    val playedHistory: StateFlow<List<PlayedSongHistoryEntity>> = playedSongHistoryDao
        .getPlayedHistory(fifteenDaysCutoff)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingStream = MutableStateFlow<String?>(null) // videoId currently loading
    val isLoadingStream: StateFlow<String?> = _isLoadingStream.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()
 
    val downloadProgress: StateFlow<Map<String, Int>> = downloadRepository.downloadProgress

    val playlists = musicRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSongToPlaylist(playlistId: Long, song: OnlineSong) {
        viewModelScope.launch {
            val playable = PlayableSong.Online(
                id = song.videoId,
                title = song.title,
                artist = song.artist,
                durationMs = song.durationMs,
                thumbnailUrl = song.thumbnailUrl
            )
            musicRepository.addSongToPlaylist(playlistId, playable, 0)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<OnlineSong>) {
        viewModelScope.launch {
            songs.forEachIndexed { index, song ->
                val playable = PlayableSong.Online(
                    id = song.videoId,
                    title = song.title,
                    artist = song.artist,
                    durationMs = song.durationMs,
                    thumbnailUrl = song.thumbnailUrl
                )
                musicRepository.addSongToPlaylist(playlistId, playable, index)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<OnlineSong>> = _query
        .debounce(500)
        .filter { it.length >= 2 }
        .flatMapLatest { q -> searchRepository.search(q) }
        .cachedIn(viewModelScope)

    val recentSearches = searchRepository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Track which songs are already downloaded
        viewModelScope.launch {
            downloadRepository.getAllDownloads().collect { downloads ->
                _downloadedIds.value = downloads.map { it.id }.toSet()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                playedSongHistoryDao.pruneOldHistory(fifteenDaysCutoff)
                val downloads = downloadedSongDao.getAllDownloadsSync()
                downloads.filter { it.downloadedAt >= fifteenDaysCutoff }.forEach { dl ->
                    playedSongHistoryDao.insertOrUpdate(
                        PlayedSongHistoryEntity(
                            id = dl.id,
                            title = dl.title,
                            artist = dl.artist,
                            thumbnailUrl = dl.thumbnailUrl,
                            durationMs = dl.durationMs,
                            isOnline = false,
                            playedAt = dl.downloadedAt
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("OnlineSearchViewModel", "Failed to sync initial played history", e)
            }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }

    fun onSearch(q: String) {
        if (q.isBlank()) return
        _query.value = q
        viewModelScope.launch {
            searchRepository.saveSearch(q)
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.deleteSearch(query)
        }
    }

    fun deletePlayedHistoryItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playedSongHistoryDao.deleteById(id)
        }
    }

    fun clearPlayedHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            playedSongHistoryDao.clearHistory()
        }
    }

    fun playHistorySong(song: PlayedSongHistoryEntity) {
        viewModelScope.launch {
            val downloaded = withContext(Dispatchers.IO) { downloadedSongDao.getById(song.id) }
            if (downloaded != null && java.io.File(downloaded.localPath).exists()) {
                musicController.playDownloadedSong(downloaded)
                withContext(Dispatchers.IO) {
                    playedSongHistoryDao.insertOrUpdate(
                        song.copy(playedAt = System.currentTimeMillis(), isOnline = false)
                    )
                }
            } else {
                val onlineSong = OnlineSong(
                    videoId = song.id,
                    title = song.title,
                    artist = song.artist,
                    thumbnailUrl = song.thumbnailUrl ?: "https://img.youtube.com/vi/${song.id}/hqdefault.jpg",
                    durationMs = song.durationMs
                )
                playSongFromSearch(onlineSong)
                withContext(Dispatchers.IO) {
                    playedSongHistoryDao.insertOrUpdate(
                        song.copy(playedAt = System.currentTimeMillis(), isOnline = true)
                    )
                }
            }
        }
    }

    /**
     * Resolves the stream URL and plays the song immediately,
     * queueing remaining search results so playback advances continuously.
     */
    fun playSongFromSearch(song: OnlineSong, contextSongs: List<OnlineSong> = emptyList()) {
        viewModelScope.launch {
            _isLoadingStream.value = song.videoId
            try {
                // Record in played history
                withContext(Dispatchers.IO) {
                    playedSongHistoryDao.insertOrUpdate(
                        PlayedSongHistoryEntity(
                            id = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            durationMs = song.durationMs,
                            isOnline = true,
                            playedAt = System.currentTimeMillis()
                        )
                    )
                }

                val url = innertubeApi.getStreamUrl(song.videoId)
                if (url != null) {
                    val playableSong = song.copy(streamUrl = url)
                    if (contextSongs.isNotEmpty()) {
                        // Compose the queue in the selected song's context so next track
                        // continues the vibe, not the next same-title YouTube search result.
                        val (orderedList, index) = SearchQueueComposer.compose(song, contextSongs)
                        val playlist = orderedList.map { s ->
                            if (s.videoId == song.videoId) playableSong else s
                        }
                        musicController.playOnlineSongs(playlist, index)
                    } else {
                        musicController.playOnlineSong(playableSong)
                    }
                } else {
                    _errorMessage.value = "Could not resolve stream URL for '${song.title}'"
                }
            } catch (e: com.example.myplayer.data.online.YouTubeStreamBlockedException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                Log.e("OnlineSearchViewModel", "Stream failed", e)
                _errorMessage.value = "Streaming failed: ${e.message}"
            } finally {
                _isLoadingStream.value = null
            }
        }
    }

    fun streamSong(song: OnlineSong) {
        playSongFromSearch(song, emptyList())
    }

    /**
     * Resolves the stream URL and then queues a download.
     */
    fun downloadSong(song: OnlineSong) {
        viewModelScope.launch {
            Log.i("DOWNLOAD_DEBUG", "DOWNLOAD BUTTON CLICKED for: ${song.title} (${song.videoId})")
            if (downloadRepository.isDownloaded(song.videoId)) {
                Log.i("DOWNLOAD_DEBUG", "Already downloaded, skipping")
                _errorMessage.value = "Already available offline"
                return@launch
            }

            _isLoadingStream.value = song.videoId + "_dl"
            try {
                Log.i("DOWNLOAD_DEBUG", "Resolving stream URL...")
                val url = innertubeApi.getStreamUrl(song.videoId)
                if (url != null) {
                    Log.i("DOWNLOAD_DEBUG", "STREAM URL RECEIVED: ${url.take(80)}...")
                    val started = downloadRepository.startDownload(song.copy(streamUrl = url))
                    if (started) {
                        Log.i("DOWNLOAD_DEBUG", "Foreground download service started for ${song.title}")
                    } else {
                        Log.e("DOWNLOAD_DEBUG", "Failed to start download service for ${song.title}")
                        _errorMessage.value = "Could not start download for '${song.title}'"
                    }
                } else {
                    Log.e("DOWNLOAD_DEBUG", "Stream URL was NULL")
                    _errorMessage.value = "Could not resolve download URL for '${song.title}'"
                }
            } catch (e: com.example.myplayer.data.online.YouTubeStreamBlockedException) {
                Log.e("DOWNLOAD_DEBUG", "YouTube blocked: ${e.message}")
                _errorMessage.value = e.message
            } catch (e: Exception) {
                Log.e("DOWNLOAD_DEBUG", "Download resolution FAILED", e)
                _errorMessage.value = "Download failed: ${e.message}"
            } finally {
                _isLoadingStream.value = null
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadById(videoId)
        }
    }
}


