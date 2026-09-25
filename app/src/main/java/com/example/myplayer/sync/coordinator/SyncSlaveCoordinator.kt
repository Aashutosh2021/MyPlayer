package com.example.myplayer.sync.coordinator

import android.os.SystemClock
import android.util.Log
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.playback.MusicController
import com.example.myplayer.sync.clock.SyncClock
import com.example.myplayer.sync.drift.SyncDriftCorrector
import com.example.myplayer.sync.model.*
import com.example.myplayer.sync.track.SyncTrackMatcher
import com.example.myplayer.sync.transport.SocketSyncClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSlaveCoordinator @Inject constructor(
    private val socketClient: SocketSyncClient,
    private val syncClock: SyncClock,
    private val syncTrackMatcher: SyncTrackMatcher,
    private val syncDriftCorrector: SyncDriftCorrector,
    private val musicController: MusicController
) {
    companion object {
        private const val TAG = "SyncSlaveCoordinator"
        private const val CLOCK_SYNC_INTERVAL_MS = 4000L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val seq = AtomicLong(1)

    private var activeSession: SyncSession? = null
    private var myDeviceId: String = ""
    private var myDeviceName: String = ""

    private val _sessionState = MutableStateFlow<SyncSession?>(null)
    val sessionState: StateFlow<SyncSession?> = _sessionState.asStateFlow()

    private val _devicesFlow = MutableStateFlow<List<SyncDevice>>(emptyList())
    val devicesFlow: StateFlow<List<SyncDevice>> = _devicesFlow.asStateFlow()

    private val _currentTrack = MutableStateFlow<SyncTrack?>(null)
    val currentTrack: StateFlow<SyncTrack?> = _currentTrack.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isTrackAvailable = MutableStateFlow(true)
    val isTrackAvailable: StateFlow<Boolean> = _isTrackAvailable.asStateFlow()

    private val _unavailableReason = MutableStateFlow<String?>(null)
    val unavailableReason: StateFlow<String?> = _unavailableReason.asStateFlow()

    private val _driftMs = MutableStateFlow(0L)
    val driftMs: StateFlow<Long> = _driftMs.asStateFlow()

    private val _sessionEndedReason = MutableStateFlow<String?>(null)
    val sessionEndedReason: StateFlow<String?> = _sessionEndedReason.asStateFlow()

    private var messageListenerJob: Job? = null
    private var clockPingerJob: Job? = null

    suspend fun joinSession(
        host: String,
        port: Int,
        sessionId: String,
        deviceId: String,
        deviceName: String
    ) = withContext(Dispatchers.IO) {
        disconnect()

        myDeviceId = deviceId
        myDeviceName = deviceName
        syncClock.reset()
        _sessionEndedReason.value = null

        socketClient.connect(host, port)

        // Launch reader on Main dispatcher to safely interact with MusicController
        messageListenerJob = scope.launch {
            socketClient.observeMessages().collect { message ->
                handleIncomingMessage(message)
            }
        }

        // Send JoinRequest
        socketClient.send(SyncMessage.JoinRequest(
            sequence = seq.incrementAndGet(),
            senderDeviceId = deviceId,
            timestamp = syncClock.localMonotonicNow(),
            deviceName = deviceName,
            sessionId = sessionId
        ))

        startClockSync()
    }

    private fun handleIncomingMessage(message: SyncMessage) {
        when (message) {
            is SyncMessage.JoinAccepted -> {
                Log.i(TAG, "JoinAccepted into session: ${message.session.sessionId}")
                activeSession = message.session
                _sessionState.value = message.session
                _devicesFlow.value = message.session.devices
            }

            is SyncMessage.JoinRejected -> {
                Log.w(TAG, "JoinRejected: ${message.reason}")
                _sessionEndedReason.value = "Join rejected by Master: ${message.reason}"
                disconnect()
            }

            is SyncMessage.Pong -> {
                val t4 = syncClock.localMonotonicNow()
                syncClock.recordSample(t1 = message.t1, t2 = message.t2, t3 = message.t3, t4 = t4)
            }

            is SyncMessage.DeviceListUpdate -> {
                _devicesFlow.value = message.devices
                activeSession = activeSession?.copy(devices = message.devices)
                _sessionState.value = activeSession
            }

            is SyncMessage.TrackPrepare -> {
                handleTrackPrepare(message)
            }

            is SyncMessage.PlayAt -> {
                handlePlayAt(message)
            }

            is SyncMessage.Pause -> {
                Log.d(TAG, "Master paused at ${message.positionMs} ms")
                musicController.pause()
            }

            is SyncMessage.Seek -> {
                handleSeek(message)
            }

            is SyncMessage.SyncState -> {
                handleSyncState(message)
            }

            is SyncMessage.Leave -> {
                Log.i(TAG, "Master or session closed: ${message.reason}")
                _sessionEndedReason.value = "Sync session ended — Master disconnected."
                disconnect()
            }

            else -> {}
        }
    }

    private fun handleTrackPrepare(msg: SyncMessage.TrackPrepare) {
        _currentTrack.value = msg.track
        _isReady.value = false
        _isTrackAvailable.value = true
        _unavailableReason.value = null

        scope.launch {
            val matchedSong = syncTrackMatcher.findMatchingSong(msg.track)
            if (matchedSong != null) {
                try {
                    // Load and pause track locally so it is buffered and prepared
                    when (matchedSong) {
                        is PlayableSong.Local -> musicController.playSongs(listOf(matchedSong.entity), 0)
                        is PlayableSong.Downloaded -> musicController.playDownloadedSong(matchedSong.entity)
                        is PlayableSong.Online -> musicController.playSong(matchedSong)
                    }
                    musicController.pause()
                    if (msg.positionMs > 0) {
                        musicController.seekTo(msg.positionMs)
                    }

                    _isReady.value = true
                    _isTrackAvailable.value = true
                    val title = when (matchedSong) {
                        is PlayableSong.Local -> matchedSong.entity.title
                        is PlayableSong.Downloaded -> matchedSong.entity.title
                        is PlayableSong.Online -> matchedSong.title
                    }
                    socketClient.send(SyncMessage.TrackReady(
                        sequence = seq.incrementAndGet(),
                        senderDeviceId = myDeviceId,
                        timestamp = syncClock.localMonotonicNow(),
                        playRequestId = msg.playRequestId,
                        isAvailable = true,
                        matchedTitle = title
                    ))
                    Log.i(TAG, "Track prepared successfully: $title")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to prepare matched track locally: ${e.message}", e)
                    reportTrackUnavailable(msg.playRequestId, "Error preparing track: ${e.message}")
                }
            } else {
                reportTrackUnavailable(msg.playRequestId, "Song not found in local library")
            }
        }
    }

    private suspend fun reportTrackUnavailable(playRequestId: String, reason: String) {
        _isReady.value = false
        _isTrackAvailable.value = false
        _unavailableReason.value = reason
        socketClient.send(SyncMessage.TrackReady(
            sequence = seq.incrementAndGet(),
            senderDeviceId = myDeviceId,
            timestamp = syncClock.localMonotonicNow(),
            playRequestId = playRequestId,
            isAvailable = false,
            reason = reason
        ))
    }

    private fun handlePlayAt(msg: SyncMessage.PlayAt) {
        if (!_isTrackAvailable.value) return

        val localTargetMonotonic = syncClock.toLocalMonotonic(msg.masterTargetTimeMs)
        val now = syncClock.localMonotonicNow()
        val waitDelayMs = localTargetMonotonic - now

        Log.d(TAG, "Scheduling PlayAt: masterTarget=${msg.masterTargetTimeMs}, localTarget=$localTargetMonotonic, wait=$waitDelayMs ms")

        scope.launch {
            if (waitDelayMs > 0) {
                delay(waitDelayMs)
            }
            if (msg.positionMs > 0) {
                musicController.seekTo(msg.positionMs)
            }
            musicController.play()
            _isReady.value = true
        }
    }

    private fun handleSeek(msg: SyncMessage.Seek) {
        if (msg.masterTargetTimeMs != null) {
            val localTarget = syncClock.toLocalMonotonic(msg.masterTargetTimeMs)
            val waitMs = localTarget - syncClock.localMonotonicNow()
            scope.launch {
                if (waitMs > 0) delay(waitMs)
                musicController.seekTo(msg.positionMs)
                musicController.play()
            }
        } else {
            musicController.seekTo(msg.positionMs)
        }
    }

    private fun handleSyncState(msg: SyncMessage.SyncState) {
        if (!_isTrackAvailable.value) return

        val now = syncClock.localMonotonicNow()
        val masterMonotonicAtState = msg.masterTimestamp
        val slaveLocalAtState = syncClock.toLocalMonotonic(masterMonotonicAtState)
        val elapsedSinceState = (now - slaveLocalAtState).coerceAtLeast(0L)

        val expectedMasterPos = if (msg.isPlaying) {
            msg.positionMs + (elapsedSinceState * msg.playbackSpeed).toLong()
        } else {
            msg.positionMs
        }

        val slaveCurrentPos = musicController.getCurrentPosition()
        val drift = syncDriftCorrector.correctDrift(slaveCurrentPos, expectedMasterPos, musicController)
        _driftMs.value = drift

        // Ensure playback playing/paused state matches Master
        if (msg.isPlaying != musicController.isPlaying.value) {
            if (msg.isPlaying) musicController.play() else musicController.pause()
        }
    }

    private fun startClockSync() {
        clockPingerJob?.cancel()
        clockPingerJob = scope.launch(Dispatchers.IO) {
            while (isActive && socketClient.isConnected()) {
                val t1 = syncClock.localMonotonicNow()
                socketClient.send(SyncMessage.Ping(
                    sequence = seq.incrementAndGet(),
                    senderDeviceId = myDeviceId,
                    timestamp = t1,
                    t1 = t1
                ))
                delay(CLOCK_SYNC_INTERVAL_MS)
            }
        }
    }

    fun disconnect() {
        messageListenerJob?.cancel()
        messageListenerJob = null
        clockPingerJob?.cancel()
        clockPingerJob = null

        scope.launch(Dispatchers.IO) {
            if (socketClient.isConnected()) {
                socketClient.send(SyncMessage.Leave(
                    sequence = seq.incrementAndGet(),
                    senderDeviceId = myDeviceId,
                    timestamp = syncClock.localMonotonicNow()
                ))
                socketClient.disconnect()
            }
        }

        syncDriftCorrector.reset(musicController)
        activeSession = null
        _sessionState.value = null
        _devicesFlow.value = emptyList()
        _currentTrack.value = null
        _isReady.value = false
        _isTrackAvailable.value = true
        _unavailableReason.value = null
        _driftMs.value = 0L
        Log.i(TAG, "Slave coordinator disconnected")
    }

    fun clearSessionEndedReason() {
        _sessionEndedReason.value = null
    }
}
