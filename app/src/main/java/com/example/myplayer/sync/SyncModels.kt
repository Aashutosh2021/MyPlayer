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

/** State of an individual device during track preparation & playback. */
enum class DeviceSyncStatus {
    IDLE,
    PREPARING,
    DOWNLOADING,
    READY,
    FAILED,
    DISCONNECTED
}

/** Explicit Sync playback state machine across the room. */
enum class SyncPlaybackState {
    IDLE,
    PREPARING,
    WAITING_FOR_READY,
    SCHEDULED,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

data class SyncDevice(
    val deviceId: String,
    val displayName: String,
    val isMaster: Boolean = false,
    val status: DeviceSyncStatus = DeviceSyncStatus.IDLE,
    val downloadProgress: Int = 0,
    val errorMessage: String? = null
)

/** Cross-device track identity sent in TRACK_PREPARE — see SyncTrackMatcher for matching rules. */
data class SyncTrackRef(
    val videoId: String?,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val streamUrl: String? = null,
    val albumArt: String? = null
)

data class SyncUiState(
    val role: SyncRole = SyncRole.NONE,
    val connectionState: SyncConnectionState = SyncConnectionState.IDLE,
    val playbackState: SyncPlaybackState = SyncPlaybackState.IDLE,
    val boundPort: Int = 45200,
    val localIpAddress: String? = null,
    val isEmulator: Boolean = false,
    val roomHostAddress: String? = null,
    val devices: List<SyncDevice> = emptyList(),
    val currentTrack: SyncTrackRef? = null,
    val errorMessage: String? = null
)
