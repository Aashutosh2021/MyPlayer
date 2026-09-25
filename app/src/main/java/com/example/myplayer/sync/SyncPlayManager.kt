package com.example.myplayer.sync

import android.os.Build
import android.util.Log
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.playback.MusicController
import com.example.myplayer.playback.SyncPlaybackInterceptor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncPlayManager"
private const val PLAY_AT_LEAD_MS = 1500L

@Singleton
class SyncPlayManager @Inject constructor(
    private val discovery: SyncDiscovery,
    private val trackMatcher: SyncTrackMatcher,
    private val musicController: MusicController,
    private val innertubeApi: InnertubeApi,
    private val downloadRepository: DownloadRepository,
    private val downloadedSongDao: DownloadedSongDao
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val deviceId: String = UUID.randomUUID().toString()
    private var sessionId: String = ""
    private var sequenceCounter = 0L

    private var transport: SyncTransport? = null
    private var transportJob: Job? = null
    private var advertiseJob: Job? = null
    private var discoveryJob: Job? = null
    private var slavePrepareJob: Job? = null

    private var pendingTrack: SongEntity? = null
    private var pendingStartPositionMs: Long = 0L
    private var isMasterPrepared = false
    private var isPlaybackScheduled = false

    // Multi-device tracking on Master (supporting N concurrent Slaves)
    private val connectedDevices = ConcurrentHashMap<String, SyncDevice>()

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
        connectedDevices.clear()

        updateState {
            SyncUiState(
                role = SyncRole.MASTER,
                connectionState = SyncConnectionState.ADVERTISING,
                playbackState = SyncPlaybackState.IDLE,
                localIpAddress = localIp,
                isEmulator = isEmu,
                devices = emptyList()
            )
        }

        // Install playback interceptor so normal MyPlayer UI controls (play/pause/seek/skip) sync to Slaves
        musicController.syncPlaybackInterceptor = object : SyncPlaybackInterceptor {
            override fun onPlayRequested(song: SongEntity, startIndex: Int): Boolean {
                if (_uiState.value.role == SyncRole.MASTER && connectedDevices.isNotEmpty()) {
                    Log.i(TAG, "SyncPlay intercepting playback on Master for: ${song.title}")
                    initiateMasterSyncTrack(song, 0L)
                    return true
                }
                return false
            }

            override fun onPauseRequested(): Boolean {
                if (_uiState.value.role == SyncRole.MASTER && connectedDevices.isNotEmpty()) {
                    Log.i(TAG, "SyncPlay intercepting PAUSE on Master")
                    pause()
                    return true
                }
                return false
            }

            override fun onResumeRequested(): Boolean {
                if (_uiState.value.role == SyncRole.MASTER && connectedDevices.isNotEmpty()) {
                    Log.i(TAG, "SyncPlay intercepting RESUME on Master")
                    resume()
                    return true
                }
                return false
            }

            override fun onSeekRequested(positionMs: Long): Boolean {
                if (_uiState.value.role == SyncRole.MASTER && connectedDevices.isNotEmpty()) {
                    Log.i(TAG, "SyncPlay intercepting SEEK on Master to ${positionMs}ms")
                    seek(positionMs)
                    return true
                }
                return false
            }

            override fun onTrackSkipRequested(isNext: Boolean): Boolean {
                if (_uiState.value.role == SyncRole.MASTER && connectedDevices.isNotEmpty()) {
                    val queue = musicController.getSongQueue()
                    val current = musicController.currentSong.value
                    val currentIndex = if (current != null) queue.indexOfFirst { it.id == current.id } else -1
                    val targetIndex = if (isNext) currentIndex + 1 else currentIndex - 1
                    if (targetIndex in queue.indices) {
                        val targetSong = queue[targetIndex]
                        Log.i(TAG, "SyncPlay intercepting SKIP (${if (isNext) "next" else "prev"}) to: ${targetSong.title}")
                        initiateMasterSyncTrack(targetSong, 0L)
                        return true
                    }
                }
                return false
            }

            override fun onPlayerIsPlayingChanged(isPlaying: Boolean, positionMs: Long) {
                if (_uiState.value.role == SyncRole.MASTER && connectedDevices.isNotEmpty()) {
                    if (!isPlaying && _uiState.value.playbackState == SyncPlaybackState.PLAYING) {
                        Log.i(TAG, "Master player paused via system/notification at ${positionMs}ms. Broadcasting PAUSE to slaves.")
                        sendMessage(
                            SyncMessage.Pause(
                                sessionId = sessionId,
                                senderId = deviceId,
                                sequence = nextSeq(),
                                timestamp = System.currentTimeMillis(),
                                positionMs = positionMs
                            )
                        )
                        updateState { it.copy(playbackState = SyncPlaybackState.PAUSED) }
                    } else if (isPlaying && _uiState.value.playbackState == SyncPlaybackState.PAUSED) {
                        Log.i(TAG, "Master player resumed via system/notification at ${positionMs}ms. Synchronizing slaves.")
                        resume()
                    }
                }
            }
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
                        Log.d(TAG, "Master: PeerConnected event received at transport layer")
                        updateState { it.copy(connectionState = SyncConnectionState.CONNECTED) }
                    }
                    is TransportEvent.Message -> handleIncoming(event.message)
                    is TransportEvent.Disconnected -> {
                        Log.d(TAG, "Master: All clients disconnected or transport closed")
                        if (connectedDevices.isEmpty()) {
                            updateState {
                                it.copy(
                                    connectionState = SyncConnectionState.ADVERTISING,
                                    playbackState = SyncPlaybackState.IDLE,
                                    devices = emptyList()
                                )
                            }
                        }
                    }
                    is TransportEvent.Error -> {
                        Log.e(TAG, "Master transport error", event.throwable)
                        updateState {
                            it.copy(
                                connectionState = SyncConnectionState.ERROR,
                                errorMessage = event.throwable.message
                            )
                        }
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
        val pos = musicController.getCurrentPosition()
        initiateMasterSyncTrack(song, pos)
    }

    private fun initiateMasterSyncTrack(song: SongEntity, startPositionMs: Long) {
        pendingTrack = song
        pendingStartPositionMs = startPositionMs
        isMasterPrepared = false
        isPlaybackScheduled = false

        val resolvedArt = song.albumArt ?: song.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
        val trackRef = SyncTrackRef(
            videoId = song.videoId,
            title = song.title,
            artist = song.artist,
            durationMs = song.duration,
            streamUrl = if (song.path.startsWith("http")) song.path else null,
            albumArt = resolvedArt
        )

        // Reset all connected slaves to PREPARING status
        for ((id, dev) in connectedDevices) {
            connectedDevices[id] = dev.copy(
                status = DeviceSyncStatus.PREPARING,
                downloadProgress = 0,
                errorMessage = null
            )
        }

        updateState {
            it.copy(
                playbackState = SyncPlaybackState.WAITING_FOR_READY,
                currentTrack = trackRef,
                devices = connectedDevices.values.toList(),
                errorMessage = null
            )
        }

        val trackSource = if (song.path.startsWith("http") || song.path.startsWith("online://")) "STREAM" else "LOCAL"
        Log.i(TAG, "SYNC PREPARE:\ntrackId=${song.videoId ?: song.id}\ndevice=all\nsource=$trackSource")

        val songWithArt = if (song.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
            song.copy(albumArt = resolvedArt)
        } else {
            song
        }

        // 1. Prepare Master player (buffering without immediate playback)
        musicController.prepareForSync(songWithArt, startPositionMs) {
            Log.d(TAG, "Master player buffer PREPARED and READY")
            isMasterPrepared = true
            checkMasterBarrierAndStart()
        }

        // 2. Broadcast TRACK_PREPARE to all Slaves
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
                startPositionMs = startPositionMs,
                streamUrl = trackRef.streamUrl,
                albumArt = trackRef.albumArt
            )
        )
    }

    private fun checkMasterBarrierAndStart() {
        if (_uiState.value.role != SyncRole.MASTER) return
        if (!isMasterPrepared) {
            Log.d(TAG, "Master barrier waiting: Master player buffer not yet ready.")
            return
        }

        val slaves = connectedDevices.values.filter { !it.isMaster }
        if (slaves.isEmpty()) {
            Log.i(TAG, "Master barrier satisfied: No slaves connected. Scheduling PLAY_AT.")
            scheduleSynchronizedStart()
            return
        }

        val allSlavesReady = slaves.all { it.status == DeviceSyncStatus.READY }
        if (allSlavesReady) {
            Log.i(TAG, "Master barrier satisfied: all ${slaves.size} slave(s) are READY. Scheduling PLAY_AT.")
            scheduleSynchronizedStart()
        } else {
            val readyCount = slaves.count { it.status == DeviceSyncStatus.READY }
            Log.d(TAG, "Master barrier waiting: $readyCount/${slaves.size} slave(s) ready.")
        }
    }

    private fun scheduleSynchronizedStart() {
        if (isPlaybackScheduled) return
        isPlaybackScheduled = true

        val leadMs = PLAY_AT_LEAD_MS
        val startPos = pendingStartPositionMs
        val targetTimestamp = System.currentTimeMillis() + leadMs

        Log.i(TAG, "SYNC PLAY_AT:\ntimestamp=$targetTimestamp\nposition=$startPos")
        updateState { it.copy(playbackState = SyncPlaybackState.SCHEDULED) }

        sendMessage(
            SyncMessage.PlayAt(
                sessionId = sessionId,
                senderId = deviceId,
                sequence = nextSeq(),
                timestamp = System.currentTimeMillis(),
                startDelayMs = leadMs,
                positionMs = startPos
            )
        )

        managerScope.launch {
            delay(leadMs)
            Log.i(TAG, "SYNC START:\ndevice=Master\nposition=$startPos")
            musicController.startPreparedSyncPlayback(startPos)
            updateState { it.copy(playbackState = SyncPlaybackState.PLAYING) }
        }
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
                playbackState = SyncPlaybackState.IDLE,
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
                playbackState = SyncPlaybackState.IDLE,
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
                        updateState {
                            it.copy(
                                connectionState = SyncConnectionState.DISCONNECTED,
                                playbackState = SyncPlaybackState.IDLE,
                                devices = emptyList()
                            )
                        }
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

                val newDevice = SyncDevice(
                    deviceId = message.senderId,
                    displayName = message.displayName,
                    isMaster = false,
                    status = DeviceSyncStatus.IDLE
                )
                connectedDevices[message.senderId] = newDevice
                updateState {
                    it.copy(
                        connectionState = SyncConnectionState.CONNECTED,
                        devices = connectedDevices.values.toList()
                    )
                }

                // If music is already playing when a new Slave joins, synchronize it without restarting existing devices
                if (_uiState.value.playbackState == SyncPlaybackState.PLAYING || musicController.isPlaying.value) {
                    val currentSong = musicController.currentSong.value
                    if (currentSong != null) {
                        val currentPos = musicController.getCurrentPosition()
                        Log.i(TAG, "Synchronizing late-joining device ${message.displayName} to current track at ${currentPos}ms")
                        sendMessage(
                            SyncMessage.TrackPrepare(
                                sessionId = sessionId,
                                senderId = deviceId,
                                sequence = nextSeq(),
                                timestamp = System.currentTimeMillis(),
                                videoId = currentSong.videoId,
                                title = currentSong.title,
                                artist = currentSong.artist,
                                durationMs = currentSong.duration,
                                startPositionMs = currentPos + 2500L,
                                streamUrl = if (currentSong.path.startsWith("http")) currentSong.path else null,
                                albumArt = currentSong.albumArt ?: currentSong.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
                            )
                        )
                    }
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
                handleSlaveTrackPrepare(message)
            }

            is SyncMessage.TrackDownloadProgress -> {
                val dev = connectedDevices[message.senderId]
                if (dev != null) {
                    connectedDevices[message.senderId] = dev.copy(
                        status = DeviceSyncStatus.DOWNLOADING,
                        downloadProgress = message.progress
                    )
                    updateState { it.copy(devices = connectedDevices.values.toList()) }
                }
                Log.i(TAG, "SYNC DOWNLOAD:\ndevice=${message.senderId}\nprogress=${message.progress}")
            }

            is SyncMessage.TrackPrepareFailed -> {
                val dev = connectedDevices[message.senderId]
                if (dev != null) {
                    connectedDevices[message.senderId] = dev.copy(
                        status = DeviceSyncStatus.FAILED,
                        errorMessage = message.reason
                    )
                    updateState { it.copy(devices = connectedDevices.values.toList()) }
                }
                Log.e(TAG, "SYNC FAILURE:\ndevice=${message.senderId}\nreason=${message.reason}")
            }

            is SyncMessage.Ready -> {
                val dev = connectedDevices[message.senderId]
                if (message.trackAvailable) {
                    if (dev != null) {
                        connectedDevices[message.senderId] = dev.copy(
                            status = DeviceSyncStatus.READY,
                            downloadProgress = 100
                        )
                        updateState { it.copy(devices = connectedDevices.values.toList()) }
                    }
                    Log.i(TAG, "SYNC READY:\ndevice=${message.senderId}")
                    checkMasterBarrierAndStart()
                } else {
                    if (dev != null) {
                        connectedDevices[message.senderId] = dev.copy(
                            status = DeviceSyncStatus.FAILED,
                            errorMessage = "Track unavailable"
                        )
                        updateState { it.copy(devices = connectedDevices.values.toList()) }
                    }
                    Log.w(TAG, "SYNC FAILURE:\ndevice=${message.senderId}\nreason=Track unavailable")
                }
            }

            is SyncMessage.PlayAt -> {
                updateState { it.copy(playbackState = SyncPlaybackState.SCHEDULED) }
                managerScope.launch {
                    delay(message.startDelayMs)
                    Log.i(TAG, "SYNC START:\ndevice=Slave\nposition=${message.positionMs}")
                    musicController.startPreparedSyncPlayback(message.positionMs)
                    updateState { it.copy(playbackState = SyncPlaybackState.PLAYING) }
                }
            }

            is SyncMessage.Pause -> {
                musicController.getMediaController()?.pause()
                updateState { it.copy(playbackState = SyncPlaybackState.PAUSED) }
            }

            is SyncMessage.Seek -> {
                musicController.seekTo(message.positionMs)
            }

            is SyncMessage.Leave -> {
                if (_uiState.value.role == SyncRole.MASTER) {
                    connectedDevices.remove(message.senderId)
                    updateState { it.copy(devices = connectedDevices.values.toList()) }
                    Log.i(TAG, "Slave left room: ${message.senderId}")
                    checkMasterBarrierAndStart()
                } else {
                    Log.i(TAG, "Master left room. Room closed.")
                    updateState {
                        it.copy(
                            connectionState = SyncConnectionState.DISCONNECTED,
                            playbackState = SyncPlaybackState.IDLE,
                            devices = emptyList()
                        )
                    }
                }
            }

            is SyncMessage.Kick -> {
                if (message.targetDeviceId == deviceId) {
                    Log.i(TAG, "Device was kicked from room by Master: $deviceId")
                    stop()
                    updateState {
                        it.copy(
                            connectionState = SyncConnectionState.DISCONNECTED,
                            playbackState = SyncPlaybackState.IDLE,
                            devices = emptyList(),
                            errorMessage = "You were removed from the room by the host"
                        )
                    }
                }
            }

            is SyncMessage.Error -> {
                updateState { it.copy(errorMessage = message.reason) }
            }
        }
    }

    // ---------------------------------------------------------------
    // SLAVE TRACK PREPARATION PIPELINE
    // ---------------------------------------------------------------

    private fun handleSlaveTrackPrepare(message: SyncMessage.TrackPrepare) {
        slavePrepareJob?.cancel()
        val resolvedArt = message.albumArt ?: message.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
        val ref = SyncTrackRef(
            videoId = message.videoId,
            title = message.title,
            artist = message.artist,
            durationMs = message.durationMs,
            streamUrl = message.streamUrl,
            albumArt = resolvedArt
        )

        updateState {
            it.copy(
                playbackState = SyncPlaybackState.PREPARING,
                currentTrack = ref,
                errorMessage = null
            )
        }

        slavePrepareJob = managerScope.launch(Dispatchers.IO) {
            // STEP 1: Check whether exact track already exists locally
            val localSong = trackMatcher.findLocalFileMatch(ref)
            if (localSong != null) {
                Log.i(TAG, "SYNC PREPARE:\ntrackId=${ref.videoId ?: ref.title}\ndevice=Slave\nsource=LOCAL")
                val localWithArt = if (localSong.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
                    localSong.copy(albumArt = resolvedArt)
                } else localSong
                withContext(Dispatchers.Main) {
                    musicController.prepareForSync(localWithArt, message.startPositionMs) {
                        Log.i(TAG, "SYNC READY:\ndevice=Slave")
                        sendMessage(
                            SyncMessage.Ready(
                                sessionId = sessionId,
                                senderId = deviceId,
                                sequence = nextSeq(),
                                timestamp = System.currentTimeMillis(),
                                trackAvailable = true
                            )
                        )
                        updateState { it.copy(playbackState = SyncPlaybackState.WAITING_FOR_READY) }
                    }
                }
                return@launch
            }

            // STEP 2: Local file does NOT exist -> Download pipeline
            Log.i(TAG, "SYNC PREPARE:\ntrackId=${ref.videoId ?: ref.title}\ndevice=Slave\nsource=DOWNLOAD")
            sendMessage(
                SyncMessage.TrackDownloadProgress(
                    sessionId = sessionId,
                    senderId = deviceId,
                    sequence = nextSeq(),
                    timestamp = System.currentTimeMillis(),
                    progress = 0
                )
            )

            val downloadedSong = downloadMissingTrack(ref)
            if (downloadedSong != null) {
                val dlWithArt = if (downloadedSong.albumArt.isNullOrBlank() && !resolvedArt.isNullOrBlank()) {
                    downloadedSong.copy(albumArt = resolvedArt)
                } else downloadedSong
                Log.i(TAG, "Downloaded file verified for ${dlWithArt.title}. Preparing Media3...")
                withContext(Dispatchers.Main) {
                    musicController.prepareForSync(dlWithArt, message.startPositionMs) {
                        Log.i(TAG, "SYNC READY:\ndevice=Slave")
                        sendMessage(
                            SyncMessage.Ready(
                                sessionId = sessionId,
                                senderId = deviceId,
                                sequence = nextSeq(),
                                timestamp = System.currentTimeMillis(),
                                trackAvailable = true
                            )
                        )
                        updateState { it.copy(playbackState = SyncPlaybackState.WAITING_FOR_READY) }
                    }
                }
                return@launch
            }

            // STEP 3: Download failed -> Fall back to online streaming safely
            Log.w(TAG, "SYNC FAILURE:\ndevice=Slave\nreason=Download failed, trying streaming fallback")
            val streamSong = prepareStreamingFallback(ref)
            if (streamSong != null) {
                withContext(Dispatchers.Main) {
                    musicController.prepareForSync(streamSong, message.startPositionMs) {
                        Log.i(TAG, "SYNC READY:\ndevice=Slave (streaming fallback)")
                        sendMessage(
                            SyncMessage.Ready(
                                sessionId = sessionId,
                                senderId = deviceId,
                                sequence = nextSeq(),
                                timestamp = System.currentTimeMillis(),
                                trackAvailable = true
                            )
                        )
                        updateState { it.copy(playbackState = SyncPlaybackState.WAITING_FOR_READY) }
                    }
                }
                return@launch
            }

            // STEP 4: Both download and streaming preparation failed
            Log.e(TAG, "SYNC FAILURE:\ndevice=Slave\nreason=Could not download or stream track")
            sendMessage(
                SyncMessage.TrackPrepareFailed(
                    sessionId = sessionId,
                    senderId = deviceId,
                    sequence = nextSeq(),
                    timestamp = System.currentTimeMillis(),
                    reason = "Could not prepare track: ${ref.title}"
                )
            )
            withContext(Dispatchers.Main) {
                updateState {
                    it.copy(
                        playbackState = SyncPlaybackState.ERROR,
                        errorMessage = "Failed to prepare track: ${ref.title}"
                    )
                }
            }
        }
    }

    private suspend fun downloadMissingTrack(ref: SyncTrackRef): SongEntity? = withContext(Dispatchers.IO) {
        try {
            // Resolve YouTube videoId
            val videoId = if (!ref.videoId.isNullOrBlank()) {
                ref.videoId
            } else {
                val searchResult = innertubeApi.search("${ref.title} ${ref.artist}")
                val firstMatch = searchResult.songs.firstOrNull()
                firstMatch?.videoId ?: return@withContext null
            }

            // Check if already in downloadedSongDao
            val existing = downloadedSongDao.getById(videoId)
            if (existing != null && File(existing.localPath).let { it.exists() && it.length() > 0 }) {
                return@withContext SongEntity(
                    id = existing.id,
                    title = existing.title,
                    artist = existing.artist,
                    album = existing.album.ifBlank { "Downloads" },
                    duration = existing.durationMs,
                    path = existing.localPath,
                    albumArt = existing.thumbnailUrl,
                    dateAdded = existing.downloadedAt,
                    videoId = existing.id
                )
            }

            // Resolve stream URL
            val streamUrl = ref.streamUrl ?: innertubeApi.getStreamUrl(videoId)
            if (streamUrl.isNullOrBlank()) {
                Log.w(TAG, "Could not resolve stream URL for download of $videoId")
                return@withContext null
            }

            val onlineSong = OnlineSong(
                videoId = videoId,
                title = ref.title,
                artist = ref.artist,
                thumbnailUrl = "",
                durationMs = ref.durationMs,
                streamUrl = streamUrl
            )

            val enqueued = downloadRepository.startDownload(onlineSong)
            if (!enqueued) return@withContext null

            // Monitor download progress and wait for verified local file
            var lastReported = -1
            val startTime = System.currentTimeMillis()
            val maxWaitMs = 60_000L

            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                // Check if file is written and persisted in DB
                val downloaded = downloadedSongDao.getById(videoId)
                if (downloaded != null) {
                    val file = File(downloaded.localPath)
                    if (file.exists() && file.isFile && file.length() > 0) {
                        Log.i(TAG, "Verified downloaded file at ${file.absolutePath} (${file.length()} bytes)")
                        val resolvedArt = downloaded.thumbnailUrl.ifBlank {
                            ref.albumArt ?: ref.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" } ?: ""
                        }
                        return@withContext SongEntity(
                            id = downloaded.id,
                            title = downloaded.title,
                            artist = downloaded.artist,
                            album = downloaded.album.ifBlank { "Downloads" },
                            duration = downloaded.durationMs,
                            path = downloaded.localPath,
                            albumArt = resolvedArt,
                            dateAdded = downloaded.downloadedAt,
                            videoId = downloaded.id
                        )
                    }
                }

                // Check and report progress to Master
                val progress = downloadRepository.downloadProgress.value[videoId] ?: 0
                if (progress != lastReported && progress in 0..100) {
                    lastReported = progress
                    Log.i(TAG, "SYNC DOWNLOAD:\ndevice=Slave\nprogress=$progress")
                    sendMessage(
                        SyncMessage.TrackDownloadProgress(
                            sessionId = sessionId,
                            senderId = deviceId,
                            sequence = nextSeq(),
                            timestamp = System.currentTimeMillis(),
                            progress = progress
                        )
                    )
                }

                delay(300)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadMissingTrack", e)
            null
        }
    }

    private suspend fun prepareStreamingFallback(ref: SyncTrackRef): SongEntity? = withContext(Dispatchers.IO) {
        val defaultArt = ref.albumArt ?: ref.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
        try {
            val url = ref.streamUrl ?: if (!ref.videoId.isNullOrBlank()) {
                innertubeApi.getStreamUrl(ref.videoId)
            } else null

            if (!url.isNullOrBlank()) {
                SongEntity(
                    id = ref.videoId ?: UUID.randomUUID().toString(),
                    title = ref.title,
                    artist = ref.artist,
                    album = "Sync Play Online",
                    duration = ref.durationMs,
                    path = url,
                    albumArt = defaultArt,
                    dateAdded = System.currentTimeMillis(),
                    videoId = ref.videoId
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing streaming fallback", e)
            null
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
            sendMessage(
                SyncMessage.Pause(
                    sessionId = sessionId,
                    senderId = deviceId,
                    sequence = nextSeq(),
                    timestamp = System.currentTimeMillis(),
                    positionMs = musicController.getCurrentPosition()
                )
            )
        }
        updateState { it.copy(playbackState = SyncPlaybackState.PAUSED) }
    }

    fun resume() {
        if (_uiState.value.role == SyncRole.MASTER) {
            pendingStartPositionMs = musicController.getCurrentPosition()
            isPlaybackScheduled = false
            scheduleSynchronizedStart()
        } else {
            musicController.getMediaController()?.play()
            updateState { it.copy(playbackState = SyncPlaybackState.PLAYING) }
        }
    }

    fun seek(positionMs: Long) {
        musicController.seekTo(positionMs)
        if (_uiState.value.role == SyncRole.MASTER) {
            sendMessage(
                SyncMessage.Seek(
                    sessionId = sessionId,
                    senderId = deviceId,
                    sequence = nextSeq(),
                    timestamp = System.currentTimeMillis(),
                    positionMs = positionMs
                )
            )
        }
    }

    /** Master removes/kicks an individual slave device from the room. */
    fun removeSlaveDevice(targetDeviceId: String) {
        if (_uiState.value.role != SyncRole.MASTER) return
        val dev = connectedDevices[targetDeviceId] ?: return
        Log.i(TAG, "Master removing slave device: ${dev.displayName} ($targetDeviceId)")

        sendMessage(
            SyncMessage.Kick(
                sessionId = sessionId,
                senderId = deviceId,
                sequence = nextSeq(),
                timestamp = System.currentTimeMillis(),
                targetDeviceId = targetDeviceId
            )
        )

        connectedDevices.remove(targetDeviceId)
        updateState { it.copy(devices = connectedDevices.values.toList()) }

        // Re-check barrier in case this slave was pending preparation and blocking start
        checkMasterBarrierAndStart()
    }

    /** Explicit Leave Room UI action — terminates the session cleanly. */
    fun leave() {
        if (sessionId.isNotBlank()) {
            sendMessage(
                SyncMessage.Leave(
                    sessionId = sessionId,
                    senderId = deviceId,
                    sequence = nextSeq(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        stop()
    }

    fun stop() {
        musicController.syncPlaybackInterceptor = null
        slavePrepareJob?.cancel(); slavePrepareJob = null
        discoveryJob?.cancel(); discoveryJob = null
        advertiseJob?.cancel(); advertiseJob = null
        transportJob?.cancel(); transportJob = null
        transport?.close(); transport = null

        discovery.stopAdvertising()
        discovery.stopDiscovery()

        connectedDevices.clear()
        pendingTrack = null
        pendingStartPositionMs = 0L
        isMasterPrepared = false
        isPlaybackScheduled = false
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
