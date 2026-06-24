package com.example.myplayer.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val downloads: StateFlow<List<DownloadedSongEntity>> = downloadRepository
        .getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDownloads: StateFlow<List<DownloadedSongEntity>> = combine(
        downloads, _searchQuery
    ) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    fun playSong(song: DownloadedSongEntity) {
        musicController.playDownloadedSong(song)
    }

    fun deleteSong(song: DownloadedSongEntity) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(song)
        }
    }
}
