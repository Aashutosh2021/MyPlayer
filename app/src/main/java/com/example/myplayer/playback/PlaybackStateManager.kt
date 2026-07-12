package com.example.myplayer.playback

import androidx.media3.common.Player
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.model.OnlineSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateManager @Inject constructor() {
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _currentOnlineSong = MutableStateFlow<OnlineSong?>(null)
    val currentOnlineSong: StateFlow<OnlineSong?> = _currentOnlineSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()

    private val _isShuffleOn = MutableStateFlow(false)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    fun updateCurrentSong(song: SongEntity?) {
        _currentSong.value = song
        if (song != null) {
            _currentOnlineSong.value = null
        }
    }

    fun updateCurrentOnlineSong(song: OnlineSong?) {
        _currentOnlineSong.value = song
        if (song != null) {
            _currentSong.value = null
        }
    }

    fun updatePlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updatePosition(pos: Long) {
        _currentPosition.value = pos
    }

    fun updateDuration(duration: Long) {
        _currentDuration.value = duration
    }

    fun updateShuffle(enabled: Boolean) {
        _isShuffleOn.value = enabled
    }

    fun updateRepeatMode(mode: Int) {
        _repeatMode.value = mode
    }
}
