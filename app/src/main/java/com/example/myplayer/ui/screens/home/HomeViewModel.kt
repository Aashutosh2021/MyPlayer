package com.example.myplayer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    val musicController: MusicController
) : ViewModel() {

    val recentSongs: StateFlow<List<SongEntity>> = repository.getRecentlyAddedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<SongEntity>> = repository.getMostPlayedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.getFavoriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSongs: StateFlow<List<SongEntity>> = repository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSong = musicController.currentSong
    val isPlaying = musicController.isPlaying

    fun playSong(songs: List<SongEntity>, startIndex: Int) {
        musicController.playSongs(songs, startIndex)
        val song = songs.getOrNull(startIndex) ?: return
        viewModelScope.launch { repository.incrementPlayCount(song.id) }
        viewModelScope.launch { repository.addRecentHistory(song.id) }
    }

    fun toggleFavorite(song: SongEntity, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(com.example.myplayer.data.repository.PlayableSong.Local(song), isCurrentlyFavorite) }
    }
}
