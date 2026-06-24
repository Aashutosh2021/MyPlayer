package com.example.myplayer.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    val musicController: MusicController
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchResults: StateFlow<List<SongEntity>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else repository.searchSongs(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSong = musicController.currentSong

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun playSong(songs: List<SongEntity>, startIndex: Int) {
        musicController.playSongs(songs, startIndex)
        val song = songs.getOrNull(startIndex) ?: return
        viewModelScope.launch { repository.incrementPlayCount(song.id) }
        viewModelScope.launch { repository.addRecentHistory(song.id) }
    }
}
