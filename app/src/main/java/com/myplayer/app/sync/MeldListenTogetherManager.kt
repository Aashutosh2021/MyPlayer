package com.myplayer.app.sync

import android.os.SystemClock
import android.util.Log
import com.myplayer.app.data.local.entity.SongEntity
import com.myplayer.app.data.online.InnertubeApi
import com.myplayer.app.data.repository.PlayableSong
import com.myplayer.app.playback.MusicController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MeldListenTogether"
const val MELD_DEFAULT_SERVER_URL = "wss://metroserverx.meowery.eu/ws"
private const val PING_INTERVAL_MS = 25_000L

/**
 * Implementation of Listen Together using the official Meld WebSocket relay server
 * (default: wss://metroserverx.meowery.eu/ws — "The Meowery", Poland).
 *
 * Uses standard OkHttp WebSockets with protobuf binary envelopes, eliminating
 * broken MQTT libraries and public broker logging issues.
 */
@Singleton
class MeldListenTogetherManager @Inject constructor(
    private val musicController: MusicController,
    private val trackMatcher: SyncTrackMatcher,
    private val innertubeApi: InnertubeApi,
    private val okHttpClient: OkHttpClient
) {
    private val managerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unhandled LT coroutine error", e)
        }
    )

    private val _uiState = MutableStateFlow(LtRoomUiState())
    val uiState: StateFlow<LtRoomUiState> = _uiState.asStateFlow()

    private var activeServerUrl = MELD_DEFAULT_SERVER_URL

    // ── Session plumbing ─────────────────────────────────────────────────────
    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private val pingSequence = AtomicLong(0L)
    private val serverClock = MeldServerClock()
    private var playbackRevision = 0L

    // Role bookkeeping
    private var deviceId = UUID.randomUUID().toString()
    private var isHost = false
    private var lastBroadcastSongId: String? = null
    private var isGuestSyncing = false

    init {
        // Host currentSong mirror
        managerScope.launch {
            musicController.currentSong.collect { song ->
                if (_uiState.value.role == LtRole.HOST) {
                    if (song != null && musicController.isPlaying.value && song.id != lastBroadcastSongId) {
                        Log.d(TAG, "Host started new song — broadcasting to room: ${song.title}")
                        broadcastTrack(song)
                    } else if (song != null) {
                        _uiState.update { it.copy(currentTrack = toLtTrack(song)) }
                    }
                }
            }
        }

        // Host isPlaying mirror (pause / resume)
        managerScope.launch {
            var wasPlaying = false
            musicController.isPlaying.collect { playing ->
                if (_uiState.value.role != LtRole.HOST || lastBroadcastSongId == null) {
                    wasPlaying = playing
                    return@collect
                }
                if (wasPlaying && !playing) {
                    Log.d(TAG, "Host paused — broadcasting pause")
                    broadcastPlaybackAction(MeldPlaybackActions.PAUSE)
                } else if (!wasPlaying && playing) {
                    Log.d(TAG, "Host resumed — broadcasting play")
                    broadcastPlaybackAction(MeldPlaybackActions.PLAY)
                }
                wasPlaying = playing
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════════

    fun createOnlineRoom(username: String) {
        val validUsername = username.ifBlank { "Host" }
        deviceId = UUID.randomUUID().toString()
        isHost = true
        resetBookkeeping()

        _uiState.update {
            it.copy(
                role = LtRole.HOST,
                connectionState = LtConnectionState.CONNECTING,
                roomCode = "",
                userId = deviceId,
                username = validUsername,
                waitingForApproval = false,
                errorMessage = null,
                currentTrack = musicController.currentSong.value?.let { s -> toLtTrack(s) },
                users = emptyList(),
                pendingJoinRequests = emptyList(),
                pendingSuggestions = emptyList()
            )
        }

        connectWebSocket { ws ->
            Log.d(TAG, "Connected to $activeServerUrl, sending create_room for $validUsername")
            val payload = MeldMessageCodec.encodeCreateRoom(validUsername)
            ws.send(ByteString.of(*payload))
        }
    }

    fun joinOnlineRoom(roomCode: String, username: String) {
        val trimmedCode = roomCode.trim().uppercase()
        if (trimmedCode.length < 4) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid room code") }
            return
        }
        val validUsername = username.ifBlank { "Guest" }
        deviceId = UUID.randomUUID().toString()
        isHost = false
        resetBookkeeping()

        _uiState.update {
            it.copy(
                role = LtRole.GUEST,
                connectionState = LtConnectionState.CONNECTING,
                roomCode = trimmedCode,
                userId = deviceId,
                username = validUsername,
                waitingForApproval = true,
                errorMessage = null,
                users = emptyList(),
                pendingJoinRequests = emptyList(),
                pendingSuggestions = emptyList()
            )
        }

        connectWebSocket { ws ->
            Log.d(TAG, "Connected to $activeServerUrl, sending join_room for $trimmedCode as $validUsername")
            val payload = MeldMessageCodec.encodeJoinRoom(trimmedCode, validUsername)
            ws.send(ByteString.of(*payload))
        }
    }

    fun leaveOnlineRoom() {
        sendBytes(MeldMessageCodec.encodeLeaveRoom())
        disconnect()
        _uiState.value = LtRoomUiState()
        Log.d(TAG, "Left online room")
    }

    fun setAutoApprove(autoApprove: Boolean) {
        _uiState.update { it.copy(autoApproveJoins = autoApprove) }
    }

    fun suggestCurrentSong() {
        val current = musicController.currentSong.value ?: return
        val track = toLtTrack(current)
        sendBytes(MeldMessageCodec.encodeSuggestTrack(track))
        Log.d(TAG, "Song suggestion sent: ${track.title}")
    }

    fun approveJoin(userId: String) {
        if (_uiState.value.role != LtRole.HOST) return
        val req = _uiState.value.pendingJoinRequests.firstOrNull { it.userId == userId }
        sendBytes(MeldMessageCodec.encodeApproveJoin(userId))
        _uiState.update { s ->
            s.copy(
                pendingJoinRequests = s.pendingJoinRequests.filter { it.userId != userId },
                users = if (req != null && s.users.none { u -> u.userId == userId }) {
                    s.users + LtUser(userId = req.userId, username = req.username, isHost = false)
                } else s.users
            )
        }
        Log.d(TAG, "Approved join for userId=$userId")
    }

    fun rejectJoin(userId: String) {
        if (_uiState.value.role != LtRole.HOST) return
        sendBytes(MeldMessageCodec.encodeRejectJoin(userId))
        _uiState.update { s ->
            s.copy(pendingJoinRequests = s.pendingJoinRequests.filter { it.userId != userId })
        }
    }

    fun approveSuggestion(suggestion: LtSongSuggestion) {
        if (_uiState.value.role != LtRole.HOST) return
        sendBytes(MeldMessageCodec.encodeApproveSuggestion(suggestion.suggestionId))
        _uiState.update { s ->
            s.copy(pendingSuggestions = s.pendingSuggestions.filter { it.suggestionId != suggestion.suggestionId })
        }
        managerScope.launch {
            try {
                val playable = PlayableSong.Online(
                    id = suggestion.track.id,
                    title = suggestion.track.title,
                    artist = suggestion.track.artist ?: "",
                    durationMs = suggestion.track.durationMs,
                    thumbnailUrl = suggestion.track.thumbnail
                )
                musicController.playPlaylist(listOf(playable))
                Log.d(TAG, "Approved suggestion — now playing: ${suggestion.track.title}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not play suggested song: ${e.message}")
            }
        }
    }

    fun rejectSuggestion(suggestionId: String) {
        sendBytes(MeldMessageCodec.encodeRejectSuggestion(suggestionId))
        _uiState.update { s ->
            s.copy(pendingSuggestions = s.pendingSuggestions.filter { it.suggestionId != suggestionId })
        }
    }

    fun transferHost(newHostUserId: String) {
        if (_uiState.value.role != LtRole.HOST) return
        sendBytes(MeldMessageCodec.encodeTransferHost(newHostUserId))
    }

    fun kickUser(userId: String) {
        if (_uiState.value.role != LtRole.HOST) return
        sendBytes(MeldMessageCodec.encodeKickUser(userId))
        _uiState.update { s ->
            s.copy(users = s.users.filter { it.userId != userId })
        }
    }

    fun setServerUrl(url: String) {
        activeServerUrl = url.trim().ifBlank { MELD_DEFAULT_SERVER_URL }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WEBSOCKET LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    private fun connectWebSocket(onOpenAction: (WebSocket) -> Unit) {
        disconnect()

        val request = Request.Builder()
            .url(activeServerUrl)
            .header("User-Agent", "com.myplayer.app")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to $activeServerUrl")
                managerScope.launch {
                    startPingLoop()
                    onOpenAction(webSocket)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleIncomingBytes(bytes.toByteArray())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                managerScope.launch {
                    _uiState.update {
                        it.copy(
                            connectionState = LtConnectionState.ERROR,
                            errorMessage = "Connection error: ${t.message ?: "Failed to reach server"}"
                        )
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed ($code): $reason")
                managerScope.launch {
                    if (_uiState.value.role != LtRole.NONE) {
                        _uiState.update { it.copy(connectionState = LtConnectionState.IDLE) }
                    }
                }
            }
        })
    }

    private fun sendBytes(data: ByteArray): Boolean {
        val ws = webSocket ?: return false
        return ws.send(ByteString.of(*data))
    }

    private fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        serverClock.reset()
        runCatching { webSocket?.close(1000, "Disconnecting") }
        webSocket = null
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = managerScope.launch {
            // Send initial ping immediately
            sendPing()
            while (isActive) {
                delay(PING_INTERVAL_MS)
                sendPing()
            }
        }
    }

    private fun sendPing() {
        val clientTime = SystemClock.elapsedRealtime()
        val seq = pingSequence.incrementAndGet()
        sendBytes(MeldMessageCodec.encodePing(clientTime, seq))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MESSAGE ROUTING
    // ══════════════════════════════════════════════════════════════════════════

    private fun handleIncomingBytes(data: ByteArray) {
        val (type, payload) = runCatching { MeldMessageCodec.decodeEnvelope(data) }.getOrNull() ?: return
        managerScope.launch {
            when (type) {
                MeldMessageTypes.ROOM_CREATED -> {
                    val (roomCode, userId, _) = MeldMessageCodec.decodeRoomCreated(payload)
                    deviceId = userId
                    _uiState.update {
                        it.copy(
                            roomCode = roomCode,
                            userId = userId,
                            connectionState = LtConnectionState.CONNECTED,
                            users = listOf(LtUser(userId, it.username, isHost = true))
                        )
                    }
                    Log.i(TAG, "Room created: $roomCode (userId=$userId)")
                    // If host is already playing a song, announce it
                    musicController.currentSong.value?.let { current ->
                        if (musicController.isPlaying.value) broadcastTrack(current)
                    }
                }

                MeldMessageTypes.JOIN_REQUEST -> {
                    val (reqUserId, reqUsername) = MeldMessageCodec.decodeJoinRequest(payload)
                    Log.i(TAG, "Join request: $reqUsername ($reqUserId)")
                    if (_uiState.value.autoApproveJoins) {
                        approveJoin(reqUserId)
                    } else {
                        _uiState.update { s ->
                            s.copy(
                                pendingJoinRequests = s.pendingJoinRequests + LtJoinRequest(reqUserId, reqUsername)
                            )
                        }
                    }
                }

                MeldMessageTypes.JOIN_APPROVED -> {
                    val approved = MeldMessageCodec.decodeJoinApproved(payload)
                    deviceId = approved.userId
                    _uiState.update {
                        it.copy(
                            roomCode = approved.roomCode,
                            userId = approved.userId,
                            connectionState = LtConnectionState.CONNECTED,
                            waitingForApproval = false,
                            currentTrack = approved.currentTrack,
                            users = approved.users
                        )
                    }
                    Log.i(TAG, "Joined room ${approved.roomCode} as guest")
                    if (approved.isPlaying && approved.currentTrack != null) {
                        applyGuestTrackSync(approved.currentTrack, approved.position, true)
                    }
                }

                MeldMessageTypes.JOIN_REJECTED -> {
                    val reason = MeldMessageCodec.decodeReason(payload).ifBlank { "Host rejected your request" }
                    disconnect()
                    _uiState.value = LtRoomUiState(errorMessage = reason)
                }

                MeldMessageTypes.USER_JOINED -> {
                    val (uId, uName) = MeldMessageCodec.decodeUserJoined(payload)
                    _uiState.update { s ->
                        if (s.users.none { it.userId == uId }) {
                            s.copy(users = s.users + LtUser(uId, uName, isHost = false))
                        } else s
                    }
                    Log.d(TAG, "User joined: $uName")
                }

                MeldMessageTypes.USER_LEFT -> {
                    val (uId, _) = MeldMessageCodec.decodeUserJoined(payload)
                    _uiState.update { s ->
                        s.copy(
                            users = s.users.filter { it.userId != uId },
                            pendingJoinRequests = s.pendingJoinRequests.filter { it.userId != uId }
                        )
                    }
                }

                MeldMessageTypes.SYNC_PLAYBACK -> {
                    val action = MeldMessageCodec.decodePlaybackAction(payload)
                    if (_uiState.value.role == LtRole.GUEST) {
                        handleGuestPlaybackAction(action)
                    }
                }

                MeldMessageTypes.PONG -> {
                    val pong = MeldMessageCodec.decodePong(payload)
                    serverClock.recordPong(pong.clientTime, pong.serverReceiveTime, pong.serverSendTime)
                }

                MeldMessageTypes.SUGGESTION_RECEIVED -> {
                    val suggestion = MeldMessageCodec.decodeSuggestionReceived(payload)
                    if (_uiState.value.role == LtRole.HOST && suggestion != null) {
                        _uiState.update { s ->
                            s.copy(
                                pendingSuggestions = s.pendingSuggestions + LtSongSuggestion(
                                    suggestionId = suggestion.suggestionId,
                                    track = suggestion.track,
                                    fromUsername = suggestion.fromUsername,
                                    fromUserId = suggestion.fromUserId
                                )
                            )
                        }
                    }
                }

                MeldMessageTypes.HOST_CHANGED -> {
                    val (newHostId, _) = MeldMessageCodec.decodeUserJoined(payload)
                    isHost = deviceId == newHostId
                    _uiState.update { s ->
                        s.copy(
                            role = if (isHost) LtRole.HOST else LtRole.GUEST,
                            users = s.users.map { u -> u.copy(isHost = u.userId == newHostId) }
                        )
                    }
                }

                MeldMessageTypes.KICKED -> {
                    val reason = MeldMessageCodec.decodeReason(payload).ifBlank { "You were removed from the room" }
                    disconnect()
                    _uiState.value = LtRoomUiState(errorMessage = reason)
                }

                MeldMessageTypes.ERROR -> {
                    val (code, message) = MeldMessageCodec.decodeError(payload)
                    Log.w(TAG, "Server error: $code - $message")
                    if (code == "room_not_found" || code == "session_not_found") {
                        disconnect()
                        _uiState.value = LtRoomUiState(errorMessage = message)
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLAYBACK SYNCHRONIZATION
    // ══════════════════════════════════════════════════════════════════════════

    private fun broadcastTrack(song: SongEntity) {
        lastBroadcastSongId = song.id
        val track = toLtTrack(song)
        _uiState.update { it.copy(currentTrack = track) }
        val pos = musicController.getCurrentPosition()
        val sTime = serverClock.now() ?: System.currentTimeMillis()
        sendBytes(
            MeldMessageCodec.encodePlaybackAction(
                action = MeldPlaybackActions.CHANGE_TRACK,
                track = track,
                positionMs = pos,
                serverTime = sTime,
                revision = ++playbackRevision
            )
        )
    }

    private fun broadcastPlaybackAction(action: String) {
        val current = _uiState.value.currentTrack ?: return
        val pos = musicController.getCurrentPosition()
        val sTime = serverClock.now() ?: System.currentTimeMillis()
        sendBytes(
            MeldMessageCodec.encodePlaybackAction(
                action = action,
                track = current,
                positionMs = pos,
                serverTime = sTime,
                revision = ++playbackRevision
            )
        )
    }

    private fun handleGuestPlaybackAction(action: MeldPlaybackActionData) {
        when (action.action) {
            MeldPlaybackActions.CHANGE_TRACK -> {
                if (action.track != null) {
                    _uiState.update { it.copy(currentTrack = action.track) }
                    applyGuestTrackSync(action.track, action.position, true)
                }
            }

            MeldPlaybackActions.PLAY -> {
                val elapsed = maxOf(0L, (serverClock.now() ?: System.currentTimeMillis()) - action.serverTime)
                val target = action.position + elapsed
                val currentPos = musicController.getCurrentPosition()
                if (kotlin.math.abs(currentPos - target) > 1000L) {
                    musicController.seekTo(target)
                }
                musicController.play()
            }

            MeldPlaybackActions.PAUSE -> {
                musicController.pause()
                musicController.seekTo(action.position)
            }

            MeldPlaybackActions.SEEK -> {
                musicController.seekTo(action.position)
            }
        }
    }

    private fun applyGuestTrackSync(track: LtTrack, positionMs: Long, shouldPlay: Boolean) {
        if (isGuestSyncing) return
        isGuestSyncing = true
        managerScope.launch {
            try {
                // 1. Try local match
                val ref = SyncTrackRef(
                    videoId = track.id,
                    title = track.title,
                    artist = track.artist ?: "",
                    durationMs = track.durationMs,
                    streamUrl = null,
                    albumArt = track.thumbnail
                )
                val local = trackMatcher.findLocalFileMatch(ref)
                val playable = if (local != null) {
                    PlayableSong.Local(local)
                } else {
                    PlayableSong.Online(
                        id = track.id,
                        title = track.title,
                        artist = track.artist ?: "Listen Together",
                        durationMs = track.durationMs,
                        thumbnailUrl = track.thumbnail
                    )
                }
                musicController.playPlaylist(listOf(playable))
                if (positionMs > 0) {
                    musicController.seekTo(positionMs)
                }
                if (!shouldPlay) {
                    musicController.pause()
                }
                sendBytes(MeldMessageCodec.encodeBufferReady(track.id))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync guest track: ${e.message}")
            } finally {
                isGuestSyncing = false
            }
        }
    }

    private fun resetBookkeeping() {
        lastBroadcastSongId = null
        playbackRevision = 0L
        isGuestSyncing = false
    }

    private fun toLtTrack(song: SongEntity) = LtTrack(
        id = song.videoId ?: song.id,
        title = song.title,
        artist = song.artist,
        thumbnail = song.albumArt,
        durationMs = song.duration
    )
}
