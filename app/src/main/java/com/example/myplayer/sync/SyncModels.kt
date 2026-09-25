package com.example.myplayer.sync

enum class SyncRole { NONE, MASTER, SLAVE }

enum class SyncConnectionState {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

data class SyncDevice(
    val deviceId: String,
    val displayName: String,
    val isMaster: Boolean = false
)

/** Cross-device track identity sent in TRACK_PREPARE — see SyncTrackMatcher for matching rules. */
data class SyncTrackRef(
    val videoId: String?,
    val title: String,
    val artist: String,
    val durationMs: Long
)

data class SyncUiState(
    val role: SyncRole = SyncRole.NONE,
    val connectionState: SyncConnectionState = SyncConnectionState.IDLE,
    val boundPort: Int = 45200,
    val localIpAddress: String? = null,
    val isEmulator: Boolean = false,
    val roomHostAddress: String? = null,
    val devices: List<SyncDevice> = emptyList(),
    val errorMessage: String? = null
)
