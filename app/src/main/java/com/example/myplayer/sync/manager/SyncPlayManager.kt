package com.example.myplayer.sync.manager

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.playback.MusicController
import com.example.myplayer.sync.clock.SyncClock
import com.example.myplayer.sync.coordinator.SyncMasterCoordinator
import com.example.myplayer.sync.coordinator.SyncSlaveCoordinator
import com.example.myplayer.sync.discovery.SyncDiscovery
import com.example.myplayer.sync.model.*
import com.example.myplayer.sync.transport.SocketSyncServer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPlayManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val discovery: SyncDiscovery,
    private val socketServer: SocketSyncServer,
    private val masterCoordinator: SyncMasterCoordinator,
    private val slaveCoordinator: SyncSlaveCoordinator,
    private val syncClock: SyncClock,
    private val musicController: MusicController,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val TAG = "SyncPlayManager"
        private const val PREFS_NAME = "sync_play_prefs"
        private const val KEY_DEVICE_ID = "sync_device_id"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString(KEY_DEVICE_ID, null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        newId
    }

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        updateNetworkInfo()

        // Collect discovery sessions
        scope.launch {
            discovery.discoveredSessions.collect { sessions ->
                _uiState.update { it.copy(discoveredSessions = sessions) }
            }
        }

        // Collect master state
        scope.launch {
            combine(
                masterCoordinator.sessionState,
                masterCoordinator.devicesFlow,
                musicController.currentSong,
                musicController.isPlaying
            ) { session, devices, song, isPlaying ->
                if (_uiState.value.role == SyncRole.MASTER) {
                    val syncTrack = song?.let {
                        SyncTrack(
                            title = it.title,
                            artist = it.artist,
                            album = it.album,
                            durationMs = it.duration,
                            videoId = it.videoId
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            session = session,
                            connectedDevices = devices,
                            currentTrack = syncTrack,
                            isPlaying = isPlaying,
                            connectionStatus = if (session != null) ConnectionStatus.CONNECTED else ConnectionStatus.IDLE
                        )
                    }
                }
            }.collect()
        }

        // Collect slave state
        scope.launch {
            combine(
                slaveCoordinator.sessionState,
                slaveCoordinator.devicesFlow,
                slaveCoordinator.currentTrack,
                slaveCoordinator.isReady,
                slaveCoordinator.isTrackAvailable,
                slaveCoordinator.unavailableReason,
                slaveCoordinator.driftMs,
                syncClock.clockState,
                musicController.isPlaying
            ) { args: Array<Any?> ->
                val session = args[0] as? SyncSession
                @Suppress("UNCHECKED_CAST")
                val devices = args[1] as List<SyncDevice>
                val track = args[2] as? SyncTrack
                val isReady = args[3] as Boolean
                val isAvailable = args[4] as Boolean
                val reason = args[5] as? String
                val drift = args[6] as Long
                val clock = args[7] as SyncClockState
                val isPlaying = args[8] as Boolean

                if (_uiState.value.role == SyncRole.SLAVE) {
                    _uiState.update { state ->
                        state.copy(
                            session = session,
                            connectedDevices = devices,
                            currentTrack = track,
                            isSlaveReady = isReady,
                            isTrackAvailable = isAvailable,
                            unavailableReason = reason,
                            driftMs = drift,
                            latencyMs = clock.roundTripTimeMs,
                            isPlaying = isPlaying,
                            connectionStatus = if (session != null) ConnectionStatus.CONNECTED else state.connectionStatus
                        )
                    }
                }
            }.collect()
        }

        // Collect slave session termination reason
        scope.launch {
            slaveCoordinator.sessionEndedReason.collect { reason ->
                if (reason != null && _uiState.value.role == SyncRole.SLAVE) {
                    _uiState.update { it.copy(
                        role = SyncRole.NONE,
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        session = null,
                        errorMessage = reason
                    ) }
                    slaveCoordinator.clearSessionEndedReason()
                }
            }
        }
    }

    suspend fun getDeviceName(): String {
        val userName = settingsDataStore.userName.firstOrNull()
        return if (!userName.isNullOrBlank()) {
            userName
        } else {
            "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        }
    }

    fun startDiscovery() {
        updateNetworkInfo()
        if (!discovery.isWifiConnected()) {
            _uiState.update { it.copy(isWifiConnected = false, errorMessage = "Please connect to a Wi-Fi network to use Sync Play") }
            return
        }
        _uiState.update { it.copy(isWifiConnected = true, connectionStatus = ConnectionStatus.DISCOVERING, errorMessage = null) }
        discovery.startDiscovery()
    }

    fun stopDiscovery() {
        discovery.stopDiscovery()
        if (_uiState.value.connectionStatus == ConnectionStatus.DISCOVERING) {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.IDLE) }
        }
    }

    fun updateNetworkInfo() {
        val ip = discovery.getLocalIpAddress()
        val isEmu = isEmulatorDevice() || ip?.startsWith("10.0.2.") == true
        _uiState.update { it.copy(localIp = ip, isEmulator = isEmu) }
    }

    private fun isEmulatorDevice(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("vbox86p"))
    }

    suspend fun createMasterSession(customRoomName: String? = null) {
        updateNetworkInfo()
        if (!discovery.isWifiConnected()) {
            _uiState.update { it.copy(isWifiConnected = false, errorMessage = "Wi-Fi connection is required to host a Sync Room") }
            return
        }

        stopDiscovery()
        leaveSession()

        val myName = getDeviceName()
        val sessionId = "MYPLAYER-${(1000..9999).random()}"
        val roomName = if (!customRoomName.isNullOrBlank()) customRoomName else "$myName's Room"

        // 1. Start TCP socket server
        val boundPort = socketServer.start()

        // 2. Start NSD advertising
        discovery.startAdvertising(
            sessionId = sessionId,
            sessionName = roomName,
            masterName = myName,
            port = boundPort
        )

        // 3. Start master coordinator
        masterCoordinator.startSession(
            sessionId = sessionId,
            sessionName = roomName,
            masterDeviceId = deviceId,
            masterDeviceName = myName,
            port = boundPort
        )

        _uiState.update { it.copy(
            role = SyncRole.MASTER,
            connectionStatus = ConnectionStatus.CONNECTED,
            errorMessage = null
        ) }

        Log.i(TAG, "Master session created successfully: $sessionId on port $boundPort")
    }

    suspend fun joinSessionByAddress(
        host: String,
        port: Int = SocketSyncServer.DEFAULT_PORT,
        sessionId: String = "MANUAL-${(1000..9999).random()}",
        sessionName: String = "Direct Room",
        masterName: String = "Master ($host)"
    ) {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").removePrefix("/")
        val session = DiscoveredSession(
            sessionId = sessionId,
            sessionName = sessionName,
            masterName = masterName,
            hostAddress = cleanHost,
            port = port
        )
        joinSlaveSession(session)
    }

    suspend fun joinSlaveSession(session: DiscoveredSession) {
        if (!discovery.isWifiConnected()) {
            _uiState.update { it.copy(isWifiConnected = false, errorMessage = "Wi-Fi connection is required to join a Sync Room") }
            return
        }

        stopDiscovery()
        leaveSession()

        _uiState.update { it.copy(
            role = SyncRole.SLAVE,
            connectionStatus = ConnectionStatus.CONNECTING,
            errorMessage = null
        ) }

        val myName = getDeviceName()
        try {
            slaveCoordinator.joinSession(
                host = session.hostAddress,
                port = session.port,
                sessionId = session.sessionId,
                deviceId = deviceId,
                deviceName = myName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Master: ${e.message}", e)
            val friendlyMsg = if (session.hostAddress.startsWith("10.0.2.")) {
                "Cannot connect to Android Emulator IP (${session.hostAddress}) from a physical device. Host the room on your physical phone instead, or use 'Join by IP' with your PC's Wi-Fi IP."
            } else {
                "Failed to connect to room at ${session.hostAddress}:${session.port}: ${e.message ?: "Connection timed out"}"
            }
            _uiState.update { it.copy(
                role = SyncRole.NONE,
                connectionStatus = ConnectionStatus.ERROR,
                errorMessage = friendlyMsg
            ) }
        }
    }

    fun leaveSession() {
        val currentRole = _uiState.value.role
        if (currentRole == SyncRole.MASTER) {
            discovery.stopAdvertising()
            masterCoordinator.stopSession()
        } else if (currentRole == SyncRole.SLAVE) {
            slaveCoordinator.disconnect()
        }

        _uiState.update { it.copy(
            role = SyncRole.NONE,
            connectionStatus = ConnectionStatus.IDLE,
            session = null,
            connectedDevices = emptyList(),
            currentTrack = null,
            isSlaveReady = false,
            isTrackAvailable = true,
            unavailableReason = null,
            driftMs = 0L,
            latencyMs = 0L
        ) }
    }

    fun broadcastMasterSeek(positionMs: Long) {
        if (_uiState.value.role == SyncRole.MASTER) {
            masterCoordinator.broadcastSeek(positionMs)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isSyncActive(): Boolean = _uiState.value.role != SyncRole.NONE
    fun isMaster(): Boolean = _uiState.value.role == SyncRole.MASTER
    fun isSlave(): Boolean = _uiState.value.role == SyncRole.SLAVE
}
