package com.example.myplayer.sync

import android.util.Log
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MeldListenTogether"

/**
 * Manages the Online Listen Together (Meld Room Code) session.
 *
 * ponytail: In-memory room manager for UI flow, connect to WebSocket relay server when backend protobuf endpoint is configured.
 */
@Singleton
class MeldListenTogetherManager @Inject constructor(
    private val musicController: MusicController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(LtRoomUiState())
    val uiState: StateFlow<LtRoomUiState> = _uiState.asStateFlow()

    init {
        // Automatically sync currently playing track when inside an active online room
        scope.launch {
            musicController.currentSong.collect { song ->
                if (_uiState.value.role != LtRole.NONE) {
                    _uiState.update { current ->
                        current.copy(
                            currentTrack = song?.let { s ->
                                LtTrack(
                                    id = s.id,
                                    title = s.title,
                                    artist = s.artist,
                                    thumbnail = s.albumArt,
                                    durationMs = s.duration
                                )
                            } ?: current.currentTrack
                        )
                    }
                }
            }
        }
    }

    fun createOnlineRoom(username: String) {
        val validUsername = username.ifBlank { "Host" }
        val generatedCode = generateRoomCode()
        val myUserId = UUID.randomUUID().toString()
        val hostUser = LtUser(userId = myUserId, username = validUsername, isHost = true)

        val activeSong = musicController.currentSong.value
        val track = activeSong?.let { s ->
            LtTrack(
                id = s.id,
                title = s.title,
                artist = s.artist,
                thumbnail = s.albumArt,
                durationMs = s.duration
            )
        }

        _uiState.update {
            it.copy(
                role = LtRole.HOST,
                connectionState = LtConnectionState.CONNECTED,
                roomCode = generatedCode,
                userId = myUserId,
                username = validUsername,
                waitingForApproval = false,
                errorMessage = null,
                currentTrack = track,
                users = listOf(hostUser),
                pendingJoinRequests = emptyList(),
                pendingSuggestions = emptyList()
            )
        }
        Log.d(TAG, "Created online room $generatedCode as host: $validUsername")
    }

    fun joinOnlineRoom(roomCode: String, username: String) {
        val trimmedCode = roomCode.trim().uppercase()
        if (trimmedCode.length < 4) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid room code (min 4 characters)") }
            return
        }

        val validUsername = username.ifBlank { "Guest" }
        val myUserId = UUID.randomUUID().toString()
        val guestUser = LtUser(userId = myUserId, username = validUsername, isHost = false)
        val dummyHost = LtUser(userId = "host-id", username = "Room Host", isHost = true)

        val activeSong = musicController.currentSong.value
        val track = activeSong?.let { s ->
            LtTrack(
                id = s.id,
                title = s.title,
                artist = s.artist,
                thumbnail = s.albumArt,
                durationMs = s.duration
            )
        }

        _uiState.update {
            it.copy(
                role = LtRole.GUEST,
                connectionState = LtConnectionState.CONNECTED,
                roomCode = trimmedCode,
                userId = myUserId,
                username = validUsername,
                waitingForApproval = false,
                errorMessage = null,
                currentTrack = track,
                users = listOf(dummyHost, guestUser),
                pendingJoinRequests = emptyList(),
                pendingSuggestions = emptyList()
            )
        }
        Log.d(TAG, "Joined online room $trimmedCode as guest: $validUsername")
    }

    fun leaveOnlineRoom() {
        _uiState.value = LtRoomUiState()
        Log.d(TAG, "Left online room")
    }

    fun setAutoApprove(autoApprove: Boolean) {
        _uiState.update { it.copy(autoApproveJoins = autoApprove) }
    }

    fun suggestCurrentSong() {
        val current = musicController.currentSong.value ?: return
        val currentState = _uiState.value
        if (currentState.role == LtRole.NONE) return

        val suggestion = LtSongSuggestion(
            suggestionId = UUID.randomUUID().toString(),
            track = LtTrack(
                id = current.id,
                title = current.title,
                artist = current.artist,
                thumbnail = current.albumArt,
                durationMs = current.duration
            ),
            fromUsername = currentState.username.ifBlank { "Guest" },
            fromUserId = currentState.userId
        )

        _uiState.update { state ->
            state.copy(pendingSuggestions = state.pendingSuggestions + suggestion)
        }
    }

    fun approveJoin(userId: String) {
        _uiState.update { state ->
            val req = state.pendingJoinRequests.firstOrNull { it.userId == userId }
            if (req != null) {
                state.copy(
                    pendingJoinRequests = state.pendingJoinRequests.filter { it.userId != userId },
                    users = state.users + LtUser(userId = req.userId, username = req.username, isHost = false)
                )
            } else state
        }
    }

    fun rejectJoin(userId: String) {
        _uiState.update { state ->
            state.copy(pendingJoinRequests = state.pendingJoinRequests.filter { it.userId != userId })
        }
    }

    fun approveSuggestion(suggestion: LtSongSuggestion) {
        _uiState.update { state ->
            state.copy(
                pendingSuggestions = state.pendingSuggestions.filter { it.suggestionId != suggestion.suggestionId },
                currentTrack = suggestion.track
            )
        }
        scope.launch {
            try {
                val playable = com.example.myplayer.data.repository.PlayableSong.Online(
                    id = suggestion.track.id,
                    title = suggestion.track.title,
                    artist = suggestion.track.artist ?: "",
                    durationMs = suggestion.track.durationMs,
                    thumbnailUrl = suggestion.track.thumbnail
                )
                musicController.playSong(playable)
            } catch (e: Exception) {
                Log.w(TAG, "Could not play suggested song: ${e.message}")
            }
        }
    }

    fun rejectSuggestion(suggestionId: String) {
        _uiState.update { state ->
            state.copy(pendingSuggestions = state.pendingSuggestions.filter { it.suggestionId != suggestionId })
        }
    }

    fun transferHost(newHostUserId: String) {
        _uiState.update { state ->
            val updatedUsers = state.users.map { user ->
                when (user.userId) {
                    newHostUserId -> user.copy(isHost = true)
                    state.userId -> user.copy(isHost = false)
                    else -> user
                }
            }
            val newRole = if (state.userId == newHostUserId) LtRole.HOST else LtRole.GUEST
            state.copy(role = newRole, users = updatedUsers)
        }
    }

    fun kickUser(userId: String) {
        _uiState.update { state ->
            state.copy(users = state.users.filter { it.userId != userId })
        }
    }

    private fun generateRoomCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
