package com.example.myplayer.dualbud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.dualbud.DualChannelPlaybackManager
import com.example.myplayer.dualbud.model.DualBudModeState
import com.example.myplayer.dualbud.model.DualChannelId
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DualBudViewModel @Inject constructor(
    private val manager: DualChannelPlaybackManager,
    private val songDao: SongDao,
    private val musicController: MusicController
) : ViewModel() {

    val uiState: StateFlow<DualBudModeState> = manager.state
    val librarySongs: Flow<List<SongEntity>> = songDao.getAllSongs()

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

    fun selectSong(channel: DualChannelId, song: SongEntity) {
        pauseNormalPlaybackIfPlaying()
        manager.loadSong(channel, song, autoPlay = true)
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

    override fun onCleared() {
        super.onCleared()
        // We preserve dual playback state unless user explicitly exits
    }
}
