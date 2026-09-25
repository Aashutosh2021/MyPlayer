package com.example.myplayer.sync

import android.os.Build
import android.util.Log
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncPlayManager"
private const val PLAY_AT_LEAD_MS = 2500L

@Singleton
class SyncPlayManager @Inject constructor(
    private val discovery: SyncDiscovery,
    private val trackMatcher: SyncTrackMatcher,
    private val musicController: MusicController
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val deviceId: String = UUID.randomUUID().toString()
    private var sessionId: String = ""
    private var sequenceCounter = 0L

    private var transport: SyncTransport? = null
    private var transportJob: Job? = null
    private var advertiseJob: Job? = null
    private var discoveryJob: Job? = null

    private var pendingTrack: SongEntity? = null
    private var pendingStartPositionMs: Long = 0L

    private val _uiState = MutableStateFlow(SyncUiState(isEmulator = isEmulator()))
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private fun nextSeq() = sequenceCounter++

    private fun updateState(transform: (SyncUiState) -> SyncUiState) {
        _uiState.value = transform(_uiState.value)
    }

    // ---------------------------------------------------------------
    // MASTER
    // ---------------------------------------------------------------

    fun createRoom(displayName: String) {
        stop()

        sessionId = UUID.randomUUID().toString()
        val localIp = getLocalIpAddress()
        val isEmu = isEmulator()
        updateState {
            SyncUiState(
                role = SyncRole.MASTER,
                connectionState = SyncConnectionState.ADVERTISING,
                localIpAddress = localIp,
                isEmulator = isEmu
            )
        }

        val server = LanServerTransport()
        transport = server

        transportJob = managerScope.launch {
            server.events().collect { event ->
                when (event) {
                    is TransportEvent.PortBound -> {
                        Log.d(TAG, "Master PortBound: ${event.port}")
                        updateState { it.copy(boundPort = event.port) }
                        advertiseJob = managerScope.launch {
                            discovery.advertise("MyPlayer-$displayName", event.port).collect { }
                        }
                    }
                    is TransportEvent.PeerConnected -> {
                        Log.d(TAG, "Master: PeerConnected received")
                        updateState {
                            it.copy(
                                connectionState = SyncConnectionState.CONNECTED,
                                devices = if (it.devices.isEmpty()) {
                                    listOf(SyncDevice("peer", "Client device", isMaster = false))
                                } else it.devices
                            )
                        }
                    }
                    is TransportEvent.Message -> handleIncoming(event.message)
                    is TransportEvent.Disconnected -> {
                        Log.d(TAG, "Master: Peer disconnected")
                        updateState {
                            it.copy(
                                connectionState = SyncConnectionState.ADVERTISING,
                                devices = emptyList()
                            )
                        }
                    }
                    is TransportEvent.Error -> {
                        Log.e(TAG, "Master transport error", event.throwable)
                        updateState { it.copy(connectionState = SyncConnectionState.ERROR, errorMessage = event.throwable.message) }
                    }
                }
            }
        }
    }

    fun startSyncPlayback() {
        val song = musicController.currentSong.value
        if (song == null) {
            updateState { it.copy(errorMessage = "Play a song locally first, then start Sync Play") }
            return
        }
        pendingStartPositionMs = musicController.getCurrentPosition()
        sendMessage(
            SyncMessage.TrackPrepare(
                sessionId = sessionId,
                senderId = deviceId,
                sequence = nextSeq(),
                timestamp = System.currentTimeMillis(),
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                durationMs = song.duration,
                startPositionMs = pendingStartPositionMs
            )
        )
    }

    // ---------------------------------------------------------------
    // SLAVE
    // ---------------------------------------------------------------

    fun startDiscovery() {
        stop()
        val isEmu = isEmulator()
        updateState {
            SyncUiState(
                role = SyncRole.SLAVE,
                connectionState = SyncConnectionState.DISCOVERING,
                isEmulator = isEmu
            )
        }

        discoveryJob = managerScope.launch {
            discovery.discover().collect { host ->
                joinHost(host)
            }
        }
    }

    private fun joinHost(host: DiscoveredHost) {
        discoveryJob?.cancel()
        discoveryJob = null
        discovery.stopDiscovery()

        val hostIp = host.host.hostAddress ?: ""

        // Check if discovered host is an emulator's private virtual IP (10.0.2.x) from a physical device
        if (hostIp.startsWith("10.0.2.") && !isEmulator()) {
            Log.w(TAG, "Discovered emulator IP $hostIp from physical device")
            updateState {
                it.copy(
                    connectionState = SyncConnectionState.ERROR,
                    errorMessage = "Host ($hostIp) is running in an Android Emulator. Emulators use private virtual networks that physical devices cannot route to.\n\nSolution: Make this physical device the Host (Master) instead!"
                )
            }
            return
        }

        connectToHost(host.host, host.port, hostIp)
    }

    fun joinByIp(ip: String, port: Int = 45200) {
        stop()
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) {
            updateState { it.copy(errorMessage = "Please enter a valid IP address") }
            return
        }
        val isEmu = isEmulator()
        updateState {
            SyncUiState(
                role = SyncRole.SLAVE,
                connectionState = SyncConnectionState.CONNECTING,
                roomHostAddress = cleanIp,
                isEmulator = isEmu
            )
        }

        managerScope.launch(Dispatchers.IO) {
            val inetAddress = try {
                InetAddress.getByName(cleanIp)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateState {
                        it.copy(
                            connectionState = SyncConnectionState.ERROR,
                            errorMessage = "Invalid IP address: $cleanIp"
                        )
                    }
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                connectToHost(inetAddress, port, cleanIp)
            }
        }
    }

    private fun connectToHost(hostAddress: InetAddress, port: Int, displayAddress: String) {
        updateState {
            it.copy(
                connectionState = SyncConnectionState.CONNECTING,
                roomHostAddress = displayAddress
            )
        }

        val client = LanClientTransport(hostAddress, port)
        transport = client

        transportJob = managerScope.launch {
            client.events().collect { event ->
                when (event) {
                    is TransportEvent.PortBound -> { }
                    is TransportEvent.PeerConnected -> {
                        Log.d(TAG, "Slave connected to Master, sending JoinRequest")
                        sendMessage(
                            SyncMessage.JoinRequest(
                                sessionId = "",
                                senderId = deviceId,
                                sequence = nextSeq(),
                                timestamp = System.currentTimeMillis(),
                                displayName = Build.MODEL
                            )
                        )
                        updateState { it.copy(connectionState = SyncConnectionState.CONNECTED) }
                    }
                    is TransportEvent.Message -> handleIncoming(event.message)
                    is TransportEvent.Disconnected -> {
                        Log.d(TAG, "Slave: Disconnected from Master")
                        updateState { it.copy(connectionState = SyncConnectionState.DISCONNECTED) }
                    }
                    is TransportEvent.Error -> {
                        Log.e(TAG, "Slave transport error", event.throwable)
                        val errorMsg = if (displayAddress.startsWith("10.0.2.") && !isEmulator()) {
                            "Cannot connect to $displayAddress: Host is an Android Emulator. Please make the physical device the Host (Master) instead."
                        } else {
                            event.throwable.message ?: "Failed to connect to $displayAddress:$port"
                        }
                        updateState {
                            it.copy(
                                connectionState = SyncConnectionState.ERROR,
                                errorMessage = errorMsg
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // SHARED MESSAGE HANDLING
    // ---------------------------------------------------------------

    private fun handleIncoming(message: SyncMessage) {
        val haveRealSession = sessionId.isNotBlank()
        if (haveRealSession && message.sessionId != sessionId && message !is SyncMessage.JoinRequest) {
            Log.w(TAG, "Dropping message for unknown/stale session: ${message.sessionId}")
            return
        }

        when (message) {
            is SyncMessage.JoinRequest -> {
                Log.d(TAG, "Master received JoinRequest from ${message.displayName} (${message.senderId})")
                sendMessage(
                    SyncMessage.JoinAccepted(
                        sessionId = sessionId,
                        senderId = deviceId,
                        sequence = nextSeq(),
                        timestamp = System.currentTimeMillis()
                    )
                )
                updateState {
                    it.copy(
                        connectionState = SyncConnectionState.CONNECTED,
                        devices = listOf(SyncDevice(message.senderId, message.displayName, isMaster = false))
                    )
                }
            }

            is SyncMessage.JoinAccepted -> {
                Log.d(TAG, "Slave received JoinAccepted for sessionId ${message.sessionId}")
                sessionId = message.sessionId
                updateState {
                    it.copy(connectionState = SyncConnectionState.CONNECTED)
                }
            }

            is SyncMessage.TrackPrepare -> {
                managerScope.launch {
                    val ref = SyncTrackRef(message.videoId, message.title, message.artist, message.durationMs)
                    val local = trackMatcher.findLocalMatch(ref)
                    if (local == null) {
                        sendMessage(SyncMessage.Ready(sessionId, deviceId, nextSeq(), System.currentTimeMillis(), trackAvailable = false))
                        updateState { it.copy(errorMessage = "Track not found locally: ${message.title}") }
                        return@launch
                    }
                    pendingTrack = local
                    pendingStartPositionMs = message.startPositionMs
                    sendMessage(SyncMessage.Ready(sessionId, deviceId, nextSeq(), System.currentTimeMillis(), trackAvailable = true))
                }
            }

            is SyncMessage.Ready -> {
                if (message.trackAvailable) {
                    scheduleSynchronizedStart()
                } else {
                    updateState { it.copy(errorMessage = "The other device is missing this track") }
                }
            }

            is SyncMessage.PlayAt -> {
                managerScope.launch {
                    delay(message.startDelayMs)
                    val song = pendingTrack ?: return@launch
                    musicController.playSongs(listOf(song), 0)
                    if (message.positionMs > 0) {
                        musicController.seekTo(message.positionMs)
                    }
                }
            }

            is SyncMessage.Pause -> musicController.getMediaController()?.pause()

            is SyncMessage.Seek -> musicController.seekTo(message.positionMs)

            is SyncMessage.Leave -> {
                updateState { it.copy(connectionState = SyncConnectionState.DISCONNECTED, devices = emptyList()) }
            }

            is SyncMessage.Error -> {
                updateState { it.copy(errorMessage = message.reason) }
            }
        }
    }

    private fun scheduleSynchronizedStart() {
        sendMessage(
            SyncMessage.PlayAt(
                sessionId = sessionId,
                senderId = deviceId,
                sequence = nextSeq(),
                timestamp = System.currentTimeMillis(),
                startDelayMs = PLAY_AT_LEAD_MS,
                positionMs = pendingStartPositionMs
            )
        )
        managerScope.launch {
            delay(PLAY_AT_LEAD_MS)
            val song = musicController.currentSong.value ?: return@launch
            musicController.playSongs(listOf(song), 0)
            if (pendingStartPositionMs > 0) {
                musicController.seekTo(pendingStartPositionMs)
            }
        }
    }

    private fun sendMessage(message: SyncMessage) {
        managerScope.launch { transport?.send(message) }
    }

    // ---------------------------------------------------------------
    // USER COMMANDS
    // ---------------------------------------------------------------

    fun pause() {
        musicController.getMediaController()?.pause()
        if (_uiState.value.role == SyncRole.MASTER) {
            sendMessage(SyncMessage.Pause(sessionId, deviceId, nextSeq(), System.currentTimeMillis(), musicController.getCurrentPosition()))
        }
    }

    fun resume() {
        musicController.getMediaController()?.play()
    }

    fun seek(positionMs: Long) {
        musicController.seekTo(positionMs)
        if (_uiState.value.role == SyncRole.MASTER) {
            sendMessage(SyncMessage.Seek(sessionId, deviceId, nextSeq(), System.currentTimeMillis(), positionMs))
        }
    }

    fun leave() {
        if (sessionId.isNotBlank()) {
            sendMessage(SyncMessage.Leave(sessionId, deviceId, nextSeq(), System.currentTimeMillis()))
        }
        stop()
    }

    fun stop() {
        discoveryJob?.cancel(); discoveryJob = null
        advertiseJob?.cancel(); advertiseJob = null
        transportJob?.cancel(); transportJob = null
        transport?.close(); transport = null

        discovery.stopAdvertising()
        discovery.stopDiscovery()

        pendingTrack = null
        pendingStartPositionMs = 0L
        sequenceCounter = 0L
        sessionId = ""
        val isEmu = isEmulator()
        updateState { SyncUiState(isEmulator = isEmu) }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val isWifiOrEth = intf.name.contains("wlan", ignoreCase = true) ||
                        intf.name.contains("eth", ignoreCase = true)
                if (isWifiOrEth) {
                    for (addr in Collections.list(intf.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            }
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("sdk_gphone")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }
}
