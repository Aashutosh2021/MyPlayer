package com.example.myplayer.sync.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Versioned protocol envelopes exchanged over the local Wi-Fi TCP transport.
 */
@Serializable
sealed class SyncMessage {
    abstract val protocolVersion: Int
    abstract val sequence: Long
    abstract val senderDeviceId: String
    abstract val timestamp: Long

    @Serializable
    @SerialName("HELLO")
    data class Hello(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val deviceName: String,
        val role: String,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("JOIN_REQUEST")
    data class JoinRequest(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val deviceName: String,
        val sessionId: String,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("JOIN_ACCEPTED")
    data class JoinAccepted(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val session: SyncSession,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("JOIN_REJECTED")
    data class JoinRejected(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val reason: String,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("PING")
    data class Ping(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val t1: Long,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("PONG")
    data class Pong(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val t1: Long,
        val t2: Long,
        val t3: Long,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("TRACK_PREPARE")
    data class TrackPrepare(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val track: SyncTrack,
        val playRequestId: String,
        val positionMs: Long = 0L,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("TRACK_READY")
    data class TrackReady(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val playRequestId: String,
        val isAvailable: Boolean,
        val reason: String? = null,
        val matchedTitle: String? = null,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("PLAY_AT")
    data class PlayAt(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val playRequestId: String,
        val positionMs: Long,
        val masterTargetTimeMs: Long,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("PAUSE")
    data class Pause(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val positionMs: Long,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("SEEK")
    data class Seek(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val positionMs: Long,
        val masterTargetTimeMs: Long? = null,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("STOP")
    data class Stop(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("SYNC_STATE")
    data class SyncState(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val track: SyncTrack?,
        val positionMs: Long,
        val isPlaying: Boolean,
        val playbackSpeed: Float,
        val masterTimestamp: Long,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("DEVICE_LIST_UPDATE")
    data class DeviceListUpdate(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val devices: List<SyncDevice>,
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("LEAVE")
    data class Leave(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val reason: String = "User left session",
        override val protocolVersion: Int = 1
    ) : SyncMessage()

    @Serializable
    @SerialName("ERROR")
    data class Error(
        override val sequence: Long,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val message: String,
        override val protocolVersion: Int = 1
    ) : SyncMessage()
}
