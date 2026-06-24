package com.example.myplayer.ui.screens.main

import androidx.lifecycle.ViewModel
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong

@HiltViewModel
class MainViewModel @Inject constructor(
    val musicController: MusicController,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val currentSong = musicController.currentSong
    val currentOnlineSong = musicController.currentOnlineSong
    val isPlaying = musicController.isPlaying
    val currentPosition = musicController.currentPosition
    val currentDuration = musicController.currentDuration
    val sleepTimerRemainingSeconds = musicController.sleepTimerRemainingSeconds

    fun playSong(song: com.example.myplayer.data.repository.PlayableSong) = musicController.playSong(song)
    fun playPlaylist(songs: List<com.example.myplayer.data.repository.PlayableSong>, startIndex: Int) = musicController.playPlaylist(songs, startIndex)
    fun playPause() = musicController.playPause()
    fun skipToNext() = musicController.skipToNext()
    fun skipToPrevious() = musicController.skipToPrevious()
    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)
    fun startSleepTimer(minutes: Int) = musicController.startSleepTimer(minutes)
    fun cancelSleepTimer() = musicController.cancelSleepTimer()

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
}
