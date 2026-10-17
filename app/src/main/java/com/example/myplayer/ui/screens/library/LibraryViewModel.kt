package com.example.myplayer.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.HybridLibraryRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val hybridLibraryRepository: HybridLibraryRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloaded songs strictly sorted in alphabetical order (case-insensitive)
    val downloadedSongs: StateFlow<List<PlayableSong>> = downloadRepository.getAllDownloads()
        .map { list ->
            list.sortedBy { it.title.lowercase() }.map { PlayableSong.Downloaded(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hybridLibrary: StateFlow<List<PlayableSong>> = hybridLibraryRepository.getHybridLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<PlayableSong>> = repository.getFavoriteSongs()
        .map { list -> list.map { PlayableSong.Local(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun addSongToPlaylist(playlistId: Long, song: PlayableSong) {
        viewModelScope.launch { 
            repository.addSongToPlaylist(playlistId, song, 0)
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadById(videoId)
        }
    }
}


