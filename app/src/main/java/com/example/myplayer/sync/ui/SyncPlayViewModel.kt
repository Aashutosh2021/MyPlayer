package com.example.myplayer.sync.ui

import androidx.lifecycle.ViewModel
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import com.example.myplayer.sync.SyncPlayManager
import com.example.myplayer.sync.SyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SyncPlayViewModel @Inject constructor(
    private val syncPlayManager: SyncPlayManager,
    private val musicController: MusicController
) : ViewModel() {

    val uiState: StateFlow<SyncUiState> = syncPlayManager.uiState
    val currentSong: StateFlow<SongEntity?> = musicController.currentSong

    fun createRoom(displayName: String) = syncPlayManager.createRoom(displayName)
    fun joinNearbyRoom() = syncPlayManager.startDiscovery()
    fun joinByIp(ip: String, port: Int = 45200) = syncPlayManager.joinByIp(ip, port)
    fun startSyncPlayback() = syncPlayManager.startSyncPlayback()
    fun pause() = syncPlayManager.pause()
    fun resume() = syncPlayManager.resume()
    fun seek(positionMs: Long) = syncPlayManager.seek(positionMs)
    fun leaveRoom() = syncPlayManager.leave()

    override fun onCleared() {
        super.onCleared()
        syncPlayManager.stop()
    }
}
