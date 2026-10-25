package com.example.myplayer.sync.ui

import androidx.lifecycle.ViewModel
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import com.example.myplayer.sync.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SyncPlayViewModel @Inject constructor(
    private val syncPlayManager: SyncPlayManager,
    private val meldListenTogetherManager: MeldListenTogetherManager,
    private val musicController: MusicController
) : ViewModel() {

    // Local Wi-Fi Sync State
    val uiState: StateFlow<SyncUiState> = syncPlayManager.uiState

    // Online Listen Together State
    val ltState: StateFlow<LtRoomUiState> = meldListenTogetherManager.uiState

    val currentSong: StateFlow<SongEntity?> = musicController.currentSong

    // Local Wi-Fi Sync Play actions
    fun createRoom(displayName: String) = syncPlayManager.createRoom(displayName)
    fun joinNearbyRoom() = syncPlayManager.startDiscovery()
    fun joinByIp(ip: String, port: Int = 45200) = syncPlayManager.joinByIp(ip, port)
    fun startSyncPlayback() = syncPlayManager.startSyncPlayback()
    fun pause() = syncPlayManager.pause()
    fun resume() = syncPlayManager.resume()
    fun seek(positionMs: Long) = syncPlayManager.seek(positionMs)
    fun removeSlave(deviceId: String) = syncPlayManager.removeSlaveDevice(deviceId)
    fun leaveRoom() = syncPlayManager.leave()

    // Online Listen Together (Meld Room Code) actions
    fun createOnlineRoom(username: String) = meldListenTogetherManager.createOnlineRoom(username)
    fun joinOnlineRoom(roomCode: String, username: String) = meldListenTogetherManager.joinOnlineRoom(roomCode, username)
    fun leaveOnlineRoom() = meldListenTogetherManager.leaveOnlineRoom()
    fun setAutoApprove(autoApprove: Boolean) = meldListenTogetherManager.setAutoApprove(autoApprove)
    fun suggestCurrentSong() = meldListenTogetherManager.suggestCurrentSong()
    fun approveJoin(userId: String) = meldListenTogetherManager.approveJoin(userId)
    fun rejectJoin(userId: String) = meldListenTogetherManager.rejectJoin(userId)
    fun approveSuggestion(suggestion: LtSongSuggestion) = meldListenTogetherManager.approveSuggestion(suggestion)
    fun rejectSuggestion(suggestionId: String) = meldListenTogetherManager.rejectSuggestion(suggestionId)
    fun transferHost(userId: String) = meldListenTogetherManager.transferHost(userId)
    fun kickUser(userId: String) = meldListenTogetherManager.kickUser(userId)

    override fun onCleared() {
        super.onCleared()
        // DO NOT stop SyncPlayManager or MeldListenTogetherManager on ViewModel cleared.
        // Both are application-scoped singletons so navigating away does not kill active rooms.
    }
}
