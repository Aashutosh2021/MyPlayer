package com.example.myplayer.sync.model

enum class ConnectionStatus {
    IDLE,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

data class DiscoveredSession(
    val sessionId: String,
    val sessionName: String,
    val masterName: String,
    val hostAddress: String,
    val port: Int
) {
    val isEmulatorHost: Boolean get() = hostAddress.startsWith("10.0.2.")
}

data class SyncUiState(
    val role: SyncRole = SyncRole.NONE,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val session: SyncSession? = null,
    val discoveredSessions: List<DiscoveredSession> = emptyList(),
    val connectedDevices: List<SyncDevice> = emptyList(),
    val currentTrack: SyncTrack? = null,
    val isPlaying: Boolean = false,
    val isSlaveReady: Boolean = false,
    val isTrackAvailable: Boolean = true,
    val unavailableReason: String? = null,
    val latencyMs: Long = 0L,
    val driftMs: Long = 0L,
    val errorMessage: String? = null,
    val isWifiConnected: Boolean = true,
    val localIp: String? = null,
    val isEmulator: Boolean = false
)
