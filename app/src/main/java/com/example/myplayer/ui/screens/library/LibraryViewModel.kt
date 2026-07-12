package com.example.myplayer.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.FolderEntity
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

    val folders: StateFlow<List<FolderEntity>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hybridLibrary: StateFlow<List<PlayableSong>> = hybridLibraryRepository.getHybridLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun addFolder(uri: String, name: String) {
        viewModelScope.launch {
            _isScanning.value = true
            try { repository.addFolder(uri, name) }
            finally { _isScanning.value = false }
        }
    }

    fun removeFolder(folder: FolderEntity) {
        viewModelScope.launch { repository.removeFolder(folder) }
    }

    fun rescanFolder(folder: FolderEntity) {
        viewModelScope.launch {
            _isScanning.value = true
            try { repository.rescanAllFolders() }
            finally { _isScanning.value = false }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun addSongToPlaylist(playlistId: Long, song: PlayableSong) {
        viewModelScope.launch { 
            // Put it at the end. For simplicity we just use 0 or fetch count. 
            // MusicRepository could handle max position but we'll just pass 0 for now
            // or let the DB order by insertion if position is 0
            repository.addSongToPlaylist(playlistId, song, 0)
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadById(videoId)
        }
    }
}
