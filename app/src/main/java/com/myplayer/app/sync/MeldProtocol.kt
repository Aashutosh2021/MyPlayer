package com.myplayer.app.sync

import android.os.SystemClock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.max

object MeldMessageTypes {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val BUFFER_READY = "buffer_ready"
    const val KICK_USER = "kick_user"
    const val TRANSFER_HOST = "transfer_host"
    const val PING = "ping"
    const val SUGGEST_TRACK = "suggest_track"
    const val APPROVE_SUGGESTION = "approve_suggestion"
    const val REJECT_SUGGESTION = "reject_suggestion"
    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val BUFFER_WAIT = "buffer_wait"
    const val BUFFER_COMPLETE = "buffer_complete"
    const val ERROR = "error"
    const val PONG = "pong"
    const val HOST_CHANGED = "host_changed"
    const val KICKED = "kicked"
    const val SYNC_STATE = "sync_state"
    const val SUGGESTION_RECEIVED = "suggestion_received"
}

object MeldPlaybackActions {
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val SEEK = "seek"
    const val CHANGE_TRACK = "change_track"
}

/**
 * Lightweight, zero-dependency protobuf binary wire-format writer.
 */
class ProtoWriter {
    val stream = ByteArrayOutputStream()

    fun writeVarint(value: Long): ProtoWriter {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            stream.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        stream.write((v and 0x7F).toInt())
        return this
    }

    fun writeTag(fieldNumber: Int, wireType: Int): ProtoWriter =
        writeVarint(((fieldNumber shl 3) or wireType).toLong())

    fun writeString(fieldNumber: Int, value: String?): ProtoWriter {
        if (!value.isNullOrEmpty()) {
            val b = value.toByteArray(Charsets.UTF_8)
            writeTag(fieldNumber, 2)
            writeVarint(b.size.toLong())
            stream.write(b)
        }
        return this
    }

    fun writeBytes(fieldNumber: Int, bytes: ByteArray?): ProtoWriter {
        if (bytes != null && bytes.isNotEmpty()) {
            writeTag(fieldNumber, 2)
            writeVarint(bytes.size.toLong())
            stream.write(bytes)
        }
        return this
    }

    fun writeInt64(fieldNumber: Int, value: Long): ProtoWriter {
        if (value != 0L) {
            writeTag(fieldNumber, 0)
            writeVarint(value)
        }
        return this
    }

    fun writeBool(fieldNumber: Int, value: Boolean): ProtoWriter {
        if (value) {
            writeTag(fieldNumber, 0)
            writeVarint(1L)
        }
        return this
    }

    fun toByteArray(): ByteArray = stream.toByteArray()
}

/**
 * Lightweight, zero-dependency protobuf binary wire-format reader.
 */
class ProtoReader(private val bytes: ByteArray) {
    private var pos = 0

    fun hasMore(): Boolean = pos < bytes.size

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (pos < bytes.size) {
            val b = bytes[pos++].toLong()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80L) == 0L) break
            shift += 7
        }
        return result
    }

    fun readTag(): Pair<Int, Int>? {
        if (!hasMore()) return null
        val tag = readVarint().toInt()
        return (tag ushr 3) to (tag and 0x07)
    }

    fun readString(length: Int): String {
        val len = length.coerceAtMost(bytes.size - pos)
        val s = String(bytes, pos, len, Charsets.UTF_8)
        pos += len
        return s
    }

    fun readBytes(length: Int): ByteArray {
        val len = length.coerceAtMost(bytes.size - pos)
        val b = bytes.copyOfRange(pos, pos + len)
        pos += len
        return b
    }

    fun skip(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            1 -> pos = (pos + 8).coerceAtMost(bytes.size)
            2 -> {
                val len = readVarint().toInt()
                pos = (pos + len).coerceAtMost(bytes.size)
            }
            5 -> pos = (pos + 4).coerceAtMost(bytes.size)
            else -> pos = bytes.size
        }
    }
}

data class MeldJoinApprovedData(
    val roomCode: String,
    val userId: String,
    val sessionToken: String,
    val currentTrack: LtTrack?,
    val isPlaying: Boolean,
    val position: Long,
    val users: List<LtUser>,
    val revision: Long
)

data class MeldPlaybackActionData(
    val action: String,
    val trackId: String,
    val position: Long,
    val track: LtTrack?,
    val serverTime: Long,
    val revision: Long
)

data class MeldPongData(
    val clientTime: Long,
    val serverReceiveTime: Long,
    val serverSendTime: Long,
    val sequence: Long
)

data class MeldSuggestionData(
    val suggestionId: String,
    val fromUserId: String,
    val fromUsername: String,
    val track: LtTrack
)

object MeldMessageCodec {

    fun encodeEnvelope(type: String, payload: ByteArray): ByteArray {
        return ProtoWriter()
            .writeString(1, type)
            .writeBytes(2, payload)
            .writeBool(3, false)
            .toByteArray()
    }

    fun decodeEnvelope(data: ByteArray): Pair<String, ByteArray> {
        val reader = ProtoReader(data)
        var type = ""
        var payload = byteArrayOf()
        var compressed = false
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) type = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) payload = reader.readBytes(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 0) compressed = reader.readVarint() != 0L else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        if (compressed && payload.isNotEmpty()) {
            payload = try {
                GZIPInputStream(ByteArrayInputStream(payload)).use { it.readBytes() }
            } catch (_: Exception) {
                payload
            }
        }
        return type to payload
    }

    // ── Encoders ─────────────────────────────────────────────────────────────

    fun encodeCreateRoom(username: String): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.CREATE_ROOM,
            ProtoWriter().writeString(1, username).toByteArray()
        )

    fun encodeJoinRoom(roomCode: String, username: String): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.JOIN_ROOM,
            ProtoWriter().writeString(1, roomCode.uppercase()).writeString(2, username).toByteArray()
        )

    fun encodeLeaveRoom(): ByteArray =
        encodeEnvelope(MeldMessageTypes.LEAVE_ROOM, byteArrayOf())

    fun encodeApproveJoin(userId: String): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.APPROVE_JOIN,
            ProtoWriter().writeString(1, userId).toByteArray()
        )

    fun encodeRejectJoin(userId: String, reason: String = "Rejected by host"): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.REJECT_JOIN,
            ProtoWriter().writeString(1, userId).writeString(2, reason).toByteArray()
        )

    fun encodePing(clientTime: Long, sequence: Long): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.PING,
            ProtoWriter().writeInt64(1, clientTime).writeInt64(2, sequence).toByteArray()
        )

    fun encodeBufferReady(trackId: String): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.BUFFER_READY,
            ProtoWriter().writeString(1, trackId).toByteArray()
        )

    fun encodeKickUser(userId: String, reason: String = "Removed by host"): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.KICK_USER,
            ProtoWriter().writeString(1, userId).writeString(2, reason).toByteArray()
        )

    fun encodeTransferHost(newHostId: String): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.TRANSFER_HOST,
            ProtoWriter().writeString(1, newHostId).toByteArray()
        )

    fun encodePlaybackAction(
        action: String,
        track: LtTrack?,
        positionMs: Long,
        serverTime: Long,
        revision: Long
    ): ByteArray {
        val trackBytes = if (track != null) encodeTrackInfo(track) else byteArrayOf()
        val payload = ProtoWriter()
            .writeString(1, action)
            .writeString(2, track?.id ?: "")
            .writeInt64(3, positionMs)
            .writeBytes(4, trackBytes)
            .writeInt64(9, serverTime)
            .writeInt64(10, revision)
            .toByteArray()
        return encodeEnvelope(MeldMessageTypes.PLAYBACK_ACTION, payload)
    }

    fun encodeSuggestTrack(track: LtTrack): ByteArray {
        val trackBytes = encodeTrackInfo(track)
        val payload = ProtoWriter().writeBytes(1, trackBytes).toByteArray()
        return encodeEnvelope(MeldMessageTypes.SUGGEST_TRACK, payload)
    }

    fun encodeApproveSuggestion(suggestionId: String): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.APPROVE_SUGGESTION,
            ProtoWriter().writeString(1, suggestionId).toByteArray()
        )

    fun encodeRejectSuggestion(suggestionId: String, reason: String = "Rejected"): ByteArray =
        encodeEnvelope(
            MeldMessageTypes.REJECT_SUGGESTION,
            ProtoWriter().writeString(1, suggestionId).writeString(2, reason).toByteArray()
        )

    private fun encodeTrackInfo(t: LtTrack): ByteArray =
        ProtoWriter()
            .writeString(1, t.id)
            .writeString(2, t.title)
            .writeString(3, t.artist ?: "")
            .writeInt64(5, t.durationMs)
            .writeString(6, t.thumbnail ?: "")
            .toByteArray()

    // ── Decoders ─────────────────────────────────────────────────────────────

    fun decodeRoomCreated(payload: ByteArray): Triple<String, String, String> {
        val reader = ProtoReader(payload)
        var roomCode = ""
        var userId = ""
        var sessionToken = ""
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) roomCode = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) userId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 2) sessionToken = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return Triple(roomCode, userId, sessionToken)
    }

    fun decodeJoinRequest(payload: ByteArray): Pair<String, String> {
        val reader = ProtoReader(payload)
        var userId = ""
        var username = ""
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) userId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) username = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return userId to username
    }

    fun decodeJoinApproved(payload: ByteArray): MeldJoinApprovedData {
        val reader = ProtoReader(payload)
        var roomCode = ""
        var userId = ""
        var sessionToken = ""
        var stateBytes = byteArrayOf()
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) roomCode = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) userId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 2) sessionToken = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                4 -> if (wireType == 2) stateBytes = reader.readBytes(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        val (currentTrack, isPlaying, position, users, revision) = decodeRoomState(stateBytes)
        return MeldJoinApprovedData(roomCode, userId, sessionToken, currentTrack, isPlaying, position, users, revision)
    }

    fun decodeRoomState(payload: ByteArray): Tuple5<LtTrack?, Boolean, Long, List<LtUser>, Long> {
        val reader = ProtoReader(payload)
        var track: LtTrack? = null
        var isPlaying = false
        var position = 0L
        val users = mutableListOf<LtUser>()
        var revision = 0L
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                3 -> if (wireType == 2) {
                    val userBytes = reader.readBytes(reader.readVarint().toInt())
                    decodeUserInfo(userBytes)?.let { users.add(it) }
                } else reader.skip(wireType)
                4 -> if (wireType == 2) {
                    val trackBytes = reader.readBytes(reader.readVarint().toInt())
                    track = decodeTrackInfo(trackBytes)
                } else reader.skip(wireType)
                5 -> if (wireType == 0) isPlaying = reader.readVarint() != 0L else reader.skip(wireType)
                6 -> if (wireType == 0) position = reader.readVarint() else reader.skip(wireType)
                10 -> if (wireType == 0) revision = reader.readVarint() else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return Tuple5(track, isPlaying, position, users, revision)
    }

    fun decodeUserInfo(payload: ByteArray): LtUser? {
        val reader = ProtoReader(payload)
        var userId = ""
        var username = ""
        var isHost = false
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) userId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) username = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 0) isHost = reader.readVarint() != 0L else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return if (userId.isNotEmpty()) LtUser(userId, username, isHost) else null
    }

    fun decodeTrackInfo(payload: ByteArray): LtTrack? {
        val reader = ProtoReader(payload)
        var id = ""
        var title = ""
        var artist = ""
        var duration = 0L
        var thumbnail = ""
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) id = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) title = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 2) artist = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                5 -> if (wireType == 0) duration = reader.readVarint() else reader.skip(wireType)
                6 -> if (wireType == 2) thumbnail = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return if (id.isNotEmpty() || title.isNotEmpty()) {
            LtTrack(id, title, artist.ifBlank { null }, thumbnail.ifBlank { null }, duration)
        } else null
    }

    fun decodeUserJoined(payload: ByteArray): Pair<String, String> {
        val reader = ProtoReader(payload)
        var userId = ""
        var username = ""
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) userId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) username = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return userId to username
    }

    fun decodePlaybackAction(payload: ByteArray): MeldPlaybackActionData {
        val reader = ProtoReader(payload)
        var action = ""
        var trackId = ""
        var position = 0L
        var track: LtTrack? = null
        var serverTime = 0L
        var revision = 0L
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) action = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) trackId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 0) position = reader.readVarint() else reader.skip(wireType)
                4 -> if (wireType == 2) {
                    val trackBytes = reader.readBytes(reader.readVarint().toInt())
                    track = decodeTrackInfo(trackBytes)
                } else reader.skip(wireType)
                9 -> if (wireType == 0) serverTime = reader.readVarint() else reader.skip(wireType)
                10 -> if (wireType == 0) revision = reader.readVarint() else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return MeldPlaybackActionData(action, trackId, position, track, serverTime, revision)
    }

    fun decodePong(payload: ByteArray): MeldPongData {
        val reader = ProtoReader(payload)
        var clientTime = 0L
        var serverReceiveTime = 0L
        var serverSendTime = 0L
        var sequence = 0L
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 0) clientTime = reader.readVarint() else reader.skip(wireType)
                2 -> if (wireType == 0) serverReceiveTime = reader.readVarint() else reader.skip(wireType)
                3 -> if (wireType == 0) serverSendTime = reader.readVarint() else reader.skip(wireType)
                4 -> if (wireType == 0) sequence = reader.readVarint() else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return MeldPongData(clientTime, serverReceiveTime, serverSendTime, sequence)
    }

    fun decodeSuggestionReceived(payload: ByteArray): MeldSuggestionData? {
        val reader = ProtoReader(payload)
        var suggestionId = ""
        var fromUserId = ""
        var fromUsername = ""
        var track: LtTrack? = null
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) suggestionId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) fromUserId = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                3 -> if (wireType == 2) fromUsername = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                4 -> if (wireType == 2) {
                    val trackBytes = reader.readBytes(reader.readVarint().toInt())
                    track = decodeTrackInfo(trackBytes)
                } else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return if (suggestionId.isNotEmpty() && track != null) {
            MeldSuggestionData(suggestionId, fromUserId, fromUsername, track)
        } else null
    }

    fun decodeError(payload: ByteArray): Pair<String, String> {
        val reader = ProtoReader(payload)
        var code = ""
        var message = ""
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) code = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                2 -> if (wireType == 2) message = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return code to message
    }

    fun decodeReason(payload: ByteArray): String {
        val reader = ProtoReader(payload)
        var reason = ""
        while (reader.hasMore()) {
            val (field, wireType) = reader.readTag() ?: break
            when (field) {
                1 -> if (wireType == 2) reason = reader.readString(reader.readVarint().toInt()) else reader.skip(wireType)
                else -> reader.skip(wireType)
            }
        }
        return reason
    }
}

data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

/**
 * Clock offset estimator for NTP-style time sync via periodic PING/PONG samples.
 */
class MeldServerClock {
    private var serverOffsetMs: Double? = null
    private var bestRoundTripMs = Long.MAX_VALUE

    @Synchronized
    fun reset() {
        serverOffsetMs = null
        bestRoundTripMs = Long.MAX_VALUE
    }

    @Synchronized
    fun recordPong(clientTime: Long, serverReceiveTime: Long, serverSendTime: Long): Boolean {
        val receivedAt = SystemClock.elapsedRealtime()
        if (clientTime <= 0L || clientTime > receivedAt || (receivedAt - clientTime) > 60_000L) return false
        if (serverReceiveTime <= 0L || serverSendTime < serverReceiveTime) return false

        val roundTrip = receivedAt - clientTime
        val serverProcessing = serverSendTime - serverReceiveTime
        val networkRoundTrip = max(0L, roundTrip - serverProcessing)
        val sampleOffset = serverSendTime + networkRoundTrip / 2.0 - receivedAt
        val previousOffset = serverOffsetMs

        if (networkRoundTrip < bestRoundTripMs) bestRoundTripMs = networkRoundTrip
        val weight = if (networkRoundTrip <= bestRoundTripMs + 50L) 0.25 else 0.05
        serverOffsetMs = previousOffset?.let { it + weight * (sampleOffset - it) } ?: sampleOffset
        return previousOffset == null
    }

    @Synchronized
    fun now(): Long? = serverOffsetMs?.let { (SystemClock.elapsedRealtime() + it).toLong() }
}
