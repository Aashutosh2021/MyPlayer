package com.example.myplayer.sync

import org.json.JSONObject

/**
 * V1 protocol subset. Deliberately excludes CLOCK_PING/PONG and SYNC_STATE/drift
 * correction — see SyncPlayManager's doc comment for why. Every message still
 * carries sessionId/senderId/sequence/timestamp so stale or cross-session
 * messages can be rejected safely (SyncPlayManager.handleIncoming does this).
 */
sealed class SyncMessage {
    abstract val sessionId: String
    abstract val senderId: String
    abstract val sequence: Long
    abstract val timestamp: Long

    data class JoinRequest(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val displayName: String
    ) : SyncMessage()

    data class JoinAccepted(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long
    ) : SyncMessage()

    data class TrackPrepare(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val videoId: String?,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val startPositionMs: Long
    ) : SyncMessage()

    data class Ready(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val trackAvailable: Boolean
    ) : SyncMessage()

    /** startDelayMs is relative ("start this many ms after you receive this"), not an
     *  absolute timestamp — this is what lets V1 skip clock synchronization entirely. */
    data class PlayAt(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val startDelayMs: Long,
        val positionMs: Long
    ) : SyncMessage()

    data class Pause(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val positionMs: Long
    ) : SyncMessage()

    data class Seek(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val positionMs: Long
    ) : SyncMessage()

    data class Leave(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long
    ) : SyncMessage()

    data class Error(
        override val sessionId: String,
        override val senderId: String,
        override val sequence: Long,
        override val timestamp: Long,
        val reason: String
    ) : SyncMessage()
}

/** Newline-delimited JSON framing (see SyncTransport) — encode() must never emit a
 *  raw newline inside the JSON, so keep string fields free of control characters. */
object SyncMessageCodec {

    fun encode(message: SyncMessage): String {
        val json = JSONObject()
        json.put("sessionId", message.sessionId)
        json.put("senderId", message.senderId)
        json.put("sequence", message.sequence)
        json.put("timestamp", message.timestamp)

        when (message) {
            is SyncMessage.JoinRequest -> {
                json.put("type", "JOIN_REQUEST")
                json.put("displayName", message.displayName)
            }
            is SyncMessage.JoinAccepted -> json.put("type", "JOIN_ACCEPTED")
            is SyncMessage.TrackPrepare -> {
                json.put("type", "TRACK_PREPARE")
                json.put("videoId", message.videoId ?: "")
                json.put("title", message.title)
                json.put("artist", message.artist)
                json.put("durationMs", message.durationMs)
                json.put("startPositionMs", message.startPositionMs)
            }
            is SyncMessage.Ready -> {
                json.put("type", "READY")
                json.put("trackAvailable", message.trackAvailable)
            }
            is SyncMessage.PlayAt -> {
                json.put("type", "PLAY_AT")
                json.put("startDelayMs", message.startDelayMs)
                json.put("positionMs", message.positionMs)
            }
            is SyncMessage.Pause -> {
                json.put("type", "PAUSE")
                json.put("positionMs", message.positionMs)
            }
            is SyncMessage.Seek -> {
                json.put("type", "SEEK")
                json.put("positionMs", message.positionMs)
            }
            is SyncMessage.Leave -> json.put("type", "LEAVE")
            is SyncMessage.Error -> {
                json.put("type", "ERROR")
                json.put("reason", message.reason)
            }
        }
        return json.toString()
    }

    /** Returns null on any malformed input — callers must drop it silently, never throw. */
    fun decode(raw: String): SyncMessage? {
        return try {
            val json = JSONObject(raw)
            val sessionId = json.getString("sessionId")
            val senderId = json.getString("senderId")
            val sequence = json.getLong("sequence")
            val timestamp = json.getLong("timestamp")

            when (json.getString("type")) {
                "JOIN_REQUEST" -> SyncMessage.JoinRequest(
                    sessionId, senderId, sequence, timestamp,
                    displayName = json.optString("displayName", "Unknown")
                )
                "JOIN_ACCEPTED" -> SyncMessage.JoinAccepted(sessionId, senderId, sequence, timestamp)
                "TRACK_PREPARE" -> SyncMessage.TrackPrepare(
                    sessionId, senderId, sequence, timestamp,
                    videoId = json.optString("videoId").takeIf { it.isNotBlank() },
                    title = json.optString("title", ""),
                    artist = json.optString("artist", ""),
                    durationMs = json.optLong("durationMs", 0L),
                    startPositionMs = json.optLong("startPositionMs", 0L)
                )
                "READY" -> SyncMessage.Ready(
                    sessionId, senderId, sequence, timestamp,
                    trackAvailable = json.optBoolean("trackAvailable", true)
                )
                "PLAY_AT" -> SyncMessage.PlayAt(
                    sessionId, senderId, sequence, timestamp,
                    startDelayMs = json.optLong("startDelayMs", 0L),
                    positionMs = json.optLong("positionMs", 0L)
                )
                "PAUSE" -> SyncMessage.Pause(
                    sessionId, senderId, sequence, timestamp,
                    positionMs = json.optLong("positionMs", 0L)
                )
                "SEEK" -> SyncMessage.Seek(
                    sessionId, senderId, sequence, timestamp,
                    positionMs = json.optLong("positionMs", 0L)
                )
                "LEAVE" -> SyncMessage.Leave(sessionId, senderId, sequence, timestamp)
                "ERROR" -> SyncMessage.Error(
                    sessionId, senderId, sequence, timestamp,
                    reason = json.optString("reason", "unknown")
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
