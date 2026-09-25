package com.example.myplayer.sync.coordinator

import android.os.SystemClock
import android.util.Log
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import com.example.myplayer.sync.clock.SyncClock
import com.example.myplayer.sync.model.*
import com.example.myplayer.sync.transport.SocketSyncServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMasterCoordinator @Inject constructor(
    private val socketServer: SocketSyncServer,
    private val syncClock: SyncClock,
    private val musicController: MusicController
) {
    companion object {
        private const val TAG = "SyncMasterCoordinator"
        private const val PREPARE_LEAD_TIME_MS = 1000L
        private const val PLAY_RESUME_LEAD_TIME_MS = 500L
        private const val SEEK_LEAD_TIME_MS = 600L
        private const val HEARTBEAT_INTERVAL_MS = 1000L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val seq = AtomicLong(1)

    private var activeSession: SyncSession? = null
    private val connectedDevices = ConcurrentHashMap<String, SyncDevice>()

    private val _sessionState = MutableStateFlow<SyncSession?>(null)
    val sessionState: StateFlow<SyncSession?> = _sessionState.asStateFlow()

    private val _devicesFlow = MutableStateFlow<List<SyncDevice>>(emptyList())
    val devicesFlow: StateFlow<List<SyncDevice>> = _devicesFlow.asStateFlow()

    private var activePlayRequestId: String = ""
    private var isLocallyDrivenAction = false

    private var heartbeatJob: Job? = null
    private var messageCollectionJob: Job? = null
    private var disconnectCollectionJob: Job? = null
    private var musicControllerObserverJob: Job? = null

    fun startSession(sessionId: String, sessionName: String, masterDeviceId: String, masterDeviceName: String, port: Int) {
        stopSession()

        val masterDevice = SyncDevice(
            deviceId = masterDeviceId,
            deviceName = masterDeviceName,
            role = "MASTER",
            isReady = true
        )
        connectedDevices[masterDeviceId] = masterDevice

        val session = SyncSession(
            sessionId = sessionId,
            sessionName = sessionName,
            masterDeviceId = masterDeviceId,
            masterDeviceName = masterDeviceName,
            port = port,
            devices = listOf(masterDevice)
        )
        activeSession = session
        _sessionState.value = session
        _devicesFlow.value = listOf(masterDevice)

        listenToClients()
        startHeartbeat()
        observeMusicController()
        Log.i(TAG, "Master session created: $sessionId ($sessionName)")
    }

    private fun listenToClients() {
        messageCollectionJob?.cancel()
        messageCollectionJob = scope.launch {
            socketServer.incomingMessages.collect { (deviceId, message) ->
                handleIncomingMessage(deviceId, message)
            }
        }

        disconnectCollectionJob?.cancel()
        disconnectCollectionJob = scope.launch {
            socketServer.clientDisconnects.collect { deviceId ->
                handleClientDisconnected(deviceId)
            }
        }
    }

    private fun handleIncomingMessage(deviceId: String, message: SyncMessage) {
        when (message) {
            is SyncMessage.JoinRequest -> {
                Log.i(TAG, "JoinRequest from ${message.deviceName} (${message.senderDeviceId})")
                val session = activeSession ?: return
                if (message.sessionId != session.sessionId) {
                    socketServer.sendTo(deviceId, SyncMessage.JoinRejected(
                        sequence = seq.incrementAndGet(),
                        senderDeviceId = session.masterDeviceId,
                        timestamp = syncClock.localMonotonicNow(),
                        reason = "Invalid session ID"
                    ))
                    return
                }

                val newDevice = SyncDevice(
                    deviceId = message.senderDeviceId,
                    deviceName = message.deviceName,
                    role = "SLAVE",
                    isReady = false
                )
                connectedDevices[message.senderDeviceId] = newDevice
                updateDevices()

                socketServer.sendTo(deviceId, SyncMessage.JoinAccepted(
                    sequence = seq.incrementAndGet(),
                    senderDeviceId = session.masterDeviceId,
                    timestamp = syncClock.localMonotonicNow(),
                    session = session.copy(devices = connectedDevices.values.toList())
                ))

                // Notify all devices of updated roster
                broadcastDeviceList()

                // If Master is currently playing a song, send prepare to the new Slave immediately
                val currentSong = musicController.currentSong.value
                if (currentSong != null) {
                    val track = toSyncTrack(currentSong)
                    val playReqId = UUID.randomUUID().toString()
                    activePlayRequestId = playReqId
                    socketServer.sendTo(deviceId, SyncMessage.TrackPrepare(
                        sequence = seq.incrementAndGet(),
                        senderDeviceId = session.masterDeviceId,
                        timestamp = syncClock.localMonotonicNow(),
                        track = track,
                        playRequestId = playReqId,
                        positionMs = musicController.getCurrentPosition()
                    ))
                }
            }

            is SyncMessage.Ping -> {
                val now = syncClock.localMonotonicNow()
                socketServer.sendTo(deviceId, SyncMessage.Pong(
                    sequence = seq.incrementAndGet(),
                    senderDeviceId = activeSession?.masterDeviceId ?: "",
                    timestamp = now,
                    t1 = message.t1,
                    t2 = now,
                    t3 = now
                ))
            }

            is SyncMessage.TrackReady -> {
                Log.d(TAG, "TrackReady from ${message.senderDeviceId}: available=${message.isAvailable}, reason=${message.reason}")
                connectedDevices[message.senderDeviceId]?.let { dev ->
                    connectedDevices[message.senderDeviceId] = dev.copy(
                        isReady = message.isAvailable,
                        isTrackAvailable = message.isAvailable,
                        unavailableReason = message.reason,
                        currentTrackTitle = message.matchedTitle
                    )
                    updateDevices()
                }

                checkAllSlavesReadyAndStart(message.playRequestId)
            }

            is SyncMessage.Leave -> {
                handleClientDisconnected(message.senderDeviceId)
            }

            else -> {}
        }
    }

    private fun handleClientDisconnected(deviceId: String) {
        if (connectedDevices.remove(deviceId) != null) {
            Log.i(TAG, "Removed device $deviceId, remaining=${connectedDevices.size}")
            updateDevices()
            broadcastDeviceList()
        }
    }

    private fun updateDevices() {
        val list = connectedDevices.values.toList()
        _devicesFlow.value = list
        activeSession = activeSession?.copy(devices = list)
        _sessionState.value = activeSession
    }

    private fun broadcastDeviceList() {
        val session = activeSession ?: return
        socketServer.broadcast(SyncMessage.DeviceListUpdate(
            sequence = seq.incrementAndGet(),
            senderDeviceId = session.masterDeviceId,
            timestamp = syncClock.localMonotonicNow(),
            devices = connectedDevices.values.toList()
        ))
    }

    private fun checkAllSlavesReadyAndStart(playRequestId: String) {
        if (playRequestId != activePlayRequestId) return
        val slaves = connectedDevices.values.filter { it.role == "SLAVE" }
        if (slaves.isEmpty()) return

        val allReady = slaves.all { it.isReady || !it.isTrackAvailable }
        if (allReady) {
            executeSynchronizedPlayAt(activePlayRequestId, musicController.getCurrentPosition(), PREPARE_LEAD_TIME_MS)
        }
    }

    private fun executeSynchronizedPlayAt(playRequestId: String, positionMs: Long, leadTimeMs: Long) {
        val session = activeSession ?: return
        val targetMasterTime = syncClock.localMonotonicNow() + leadTimeMs

        socketServer.broadcast(SyncMessage.PlayAt(
            sequence = seq.incrementAndGet(),
            senderDeviceId = session.masterDeviceId,
            timestamp = syncClock.localMonotonicNow(),
            playRequestId = playRequestId,
            positionMs = positionMs,
            masterTargetTimeMs = targetMasterTime
        ))

        scope.launch {
            val remainingDelay = targetMasterTime - syncClock.localMonotonicNow()
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }
            isLocallyDrivenAction = true
            musicController.play()
            isLocallyDrivenAction = false
        }
    }

    private fun observeMusicController() {
        musicControllerObserverJob?.cancel()
        musicControllerObserverJob = scope.launch {
            var previousSongId: String? = null

            launch {
                musicController.currentSong.collect { song ->
                    if (song != null && song.id != previousSongId) {
                        previousSongId = song.id
                        onMasterSongChanged(song)
                    }
                }
            }

            launch {
                musicController.isPlaying.collect { isPlaying ->
                    if (!isLocallyDrivenAction) {
                        onMasterIsPlayingChanged(isPlaying)
                    }
                }
            }
        }
    }

    private fun onMasterSongChanged(song: SongEntity) {
        val session = activeSession ?: return
        val track = toSyncTrack(song)
        val playReqId = UUID.randomUUID().toString()
        activePlayRequestId = playReqId

        // Mark all slaves as not ready until they prepare
        connectedDevices.values.filter { it.role == "SLAVE" }.forEach { dev ->
            connectedDevices[dev.deviceId] = dev.copy(isReady = false, unavailableReason = null)
        }
        updateDevices()

        // Temporarily pause master so it waits for prepare
        isLocallyDrivenAction = true
        musicController.pause()
        isLocallyDrivenAction = false

        Log.d(TAG, "Broadcasting TrackPrepare for '${song.title}' (playRequestId=$playReqId)")
        socketServer.broadcast(SyncMessage.TrackPrepare(
            sequence = seq.incrementAndGet(),
            senderDeviceId = session.masterDeviceId,
            timestamp = syncClock.localMonotonicNow(),
            track = track,
            playRequestId = playReqId,
            positionMs = 0L
        ))

        // Failsafe timeout: if slaves take > 1500ms, start playback anyway
        scope.launch {
            delay(1500L)
            if (activePlayRequestId == playReqId) {
                executeSynchronizedPlayAt(playReqId, 0L, 500L)
            }
        }
    }

    private fun onMasterIsPlayingChanged(isPlaying: Boolean) {
        val session = activeSession ?: return
        val now = syncClock.localMonotonicNow()
        val currentPos = musicController.getCurrentPosition()

        if (!isPlaying) {
            socketServer.broadcast(SyncMessage.Pause(
                sequence = seq.incrementAndGet(),
                senderDeviceId = session.masterDeviceId,
                timestamp = now,
                positionMs = currentPos
            ))
        } else {
            // User hit Play on Master
            val targetTime = now + PLAY_RESUME_LEAD_TIME_MS
            socketServer.broadcast(SyncMessage.PlayAt(
                sequence = seq.incrementAndGet(),
                senderDeviceId = session.masterDeviceId,
                timestamp = now,
                playRequestId = activePlayRequestId,
                positionMs = currentPos,
                masterTargetTimeMs = targetTime
            ))
        }
    }

    fun broadcastSeek(positionMs: Long) {
        val session = activeSession ?: return
        val now = syncClock.localMonotonicNow()
        val targetTime = now + SEEK_LEAD_TIME_MS

        socketServer.broadcast(SyncMessage.Seek(
            sequence = seq.incrementAndGet(),
            senderDeviceId = session.masterDeviceId,
            timestamp = now,
            positionMs = positionMs,
            masterTargetTimeMs = targetTime
        ))

        scope.launch {
            val waitMs = targetTime - syncClock.localMonotonicNow()
            if (waitMs > 0) delay(waitMs)
            isLocallyDrivenAction = true
            musicController.seekTo(positionMs)
            musicController.play()
            isLocallyDrivenAction = false
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                val session = activeSession ?: break
                val currentSong = musicController.currentSong.value
                val syncTrack = currentSong?.let { toSyncTrack(it) }

                socketServer.broadcast(SyncMessage.SyncState(
                    sequence = seq.incrementAndGet(),
                    senderDeviceId = session.masterDeviceId,
                    timestamp = syncClock.localMonotonicNow(),
                    track = syncTrack,
                    positionMs = musicController.getCurrentPosition(),
                    isPlaying = musicController.isPlaying.value,
                    playbackSpeed = musicController.getPlaybackSpeed(),
                    masterTimestamp = syncClock.localMonotonicNow()
                ))
            }
        }
    }

    private fun toSyncTrack(song: SongEntity): SyncTrack {
        return SyncTrack(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.duration,
            videoId = song.videoId
        )
    }

    fun stopSession() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        messageCollectionJob?.cancel()
        messageCollectionJob = null
        disconnectCollectionJob?.cancel()
        disconnectCollectionJob = null
        musicControllerObserverJob?.cancel()
        musicControllerObserverJob = null

        activeSession?.let { session ->
            socketServer.broadcast(SyncMessage.Leave(
                sequence = seq.incrementAndGet(),
                senderDeviceId = session.masterDeviceId,
                timestamp = syncClock.localMonotonicNow(),
                reason = "Master session ended"
            ))
        }

        socketServer.stop()
        connectedDevices.clear()
        activeSession = null
        _sessionState.value = null
        _devicesFlow.value = emptyList()
        Log.i(TAG, "Master session stopped")
    }
}
