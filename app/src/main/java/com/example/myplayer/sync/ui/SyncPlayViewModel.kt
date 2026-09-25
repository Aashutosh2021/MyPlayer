package com.example.myplayer.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.playback.MusicController
import com.example.myplayer.sync.manager.SyncPlayManager
import com.example.myplayer.sync.model.DiscoveredSession
import com.example.myplayer.sync.model.SyncRole
import com.example.myplayer.sync.model.SyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncPlayViewModel @Inject constructor(
    private val syncPlayManager: SyncPlayManager,
    private val musicController: MusicController
) : ViewModel() {

    val uiState: StateFlow<SyncUiState> = syncPlayManager.uiState
    val currentPosition: StateFlow<Long> = musicController.currentPosition
    val currentDuration: StateFlow<Long> = musicController.currentDuration

    fun startScanning() {
        syncPlayManager.startDiscovery()
    }

    fun stopScanning() {
        syncPlayManager.stopDiscovery()
    }

    fun createRoom(customName: String? = null) {
        viewModelScope.launch {
            syncPlayManager.createMasterSession(customName)
        }
    }

    fun joinRoom(session: DiscoveredSession) {
        viewModelScope.launch {
            syncPlayManager.joinSlaveSession(session)
        }
    }

    fun leaveRoom() {
        syncPlayManager.leaveSession()
    }

    fun clearError() {
        syncPlayManager.clearError()
    }

    fun onMasterPlayPause() {
        if (uiState.value.role == SyncRole.MASTER) {
            musicController.playPause()
        }
    }

    fun onMasterSeek(positionMs: Long) {
        if (uiState.value.role == SyncRole.MASTER) {
            syncPlayManager.broadcastMasterSeek(positionMs)
        }
    }

    fun onMasterNext() {
        if (uiState.value.role == SyncRole.MASTER) {
            musicController.skipToNext()
        }
    }

    fun onMasterPrevious() {
        if (uiState.value.role == SyncRole.MASTER) {
            musicController.skipToPrevious()
        }
    }
}
