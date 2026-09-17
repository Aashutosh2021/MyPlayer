package com.example.myplayer.aria.model

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

/**
 * Common interface for all strongly-typed ARIA IPC responses.
 */
interface AriaBaseResponse : Parcelable {
    val status: AriaStatus
    val commandOrdinal: Int
    val transactionId: String?
}

/**
 * Response for track-specific operations (e.g. PLAY, GET_CURRENT_SONG, progress stream).
 */
data class TrackResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val song: SongInfo?,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readParcelable(SongInfo::class.java.classLoader),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeParcelable(song, flags)
        parcel.writeLong(positionMs)
        parcel.writeLong(durationMs)
        parcel.writeByte(if (isPlaying) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrackResponse> {
        override fun createFromParcel(parcel: Parcel): TrackResponse = TrackResponse(parcel)
        override fun newArray(size: Int): Array<TrackResponse?> = arrayOfNulls(size)
    }
}

/**
 * Response for playlist-related operations (e.g. PLAY_PLAYLIST, CREATE_PLAYLIST).
 */
data class PlaylistResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val playlistId: Long = -1L,
    val playlistName: String = "",
    val songs: List<SongInfo> = emptyList()
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.createTypedArrayList(SongInfo.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeLong(playlistId)
        parcel.writeString(playlistName)
        parcel.writeTypedList(songs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PlaylistResponse> {
        override fun createFromParcel(parcel: Parcel): PlaylistResponse = PlaylistResponse(parcel)
        override fun newArray(size: Int): Array<PlaylistResponse?> = arrayOfNulls(size)
    }
}

/**
 * Response for queue status queries.
 */
data class QueueResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val queue: QueueInfo?
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readParcelable(QueueInfo::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeParcelable(queue, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<QueueResponse> {
        override fun createFromParcel(parcel: Parcel): QueueResponse = QueueResponse(parcel)
        override fun newArray(size: Int): Array<QueueResponse?> = arrayOfNulls(size)
    }
}

/**
 * Response for listening history or search requests.
 */
data class HistoryResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val songs: List<SongInfo> = emptyList()
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.createTypedArrayList(SongInfo.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeTypedList(songs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<HistoryResponse> {
        override fun createFromParcel(parcel: Parcel): HistoryResponse = HistoryResponse(parcel)
        override fun newArray(size: Int): Array<HistoryResponse?> = arrayOfNulls(size)
    }
}

/**
 * Response for lyrics retrieval.
 */
data class LyricsResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val songId: String,
    val lyrics: String?
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeString(songId)
        parcel.writeString(lyrics)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LyricsResponse> {
        override fun createFromParcel(parcel: Parcel): LyricsResponse = LyricsResponse(parcel)
        override fun newArray(size: Int): Array<LyricsResponse?> = arrayOfNulls(size)
    }
}

/**
 * Response for song recommendations.
 */
data class RecommendationResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val songs: List<SongInfo> = emptyList()
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.createTypedArrayList(SongInfo.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeTypedList(songs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RecommendationResponse> {
        override fun createFromParcel(parcel: Parcel): RecommendationResponse = RecommendationResponse(parcel)
        override fun newArray(size: Int): Array<RecommendationResponse?> = arrayOfNulls(size)
    }
}

/**
 * Standard generic response for operations returning no special data (e.g. pause, setShuffle).
 */
data class AriaGeneralResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val message: String? = null
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeString(message)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AriaGeneralResponse> {
        override fun createFromParcel(parcel: Parcel): AriaGeneralResponse = AriaGeneralResponse(parcel)
        override fun newArray(size: Int): Array<AriaGeneralResponse?> = arrayOfNulls(size)
    }
}

/**
 * System and service health evaluation response.
 */
data class HealthResponse(
    override val status: AriaStatus,
    override val commandOrdinal: Int,
    override val transactionId: String?,
    val version: String,
    val state: String,
    val playbackState: String,
    val queueSize: Int,
    val memoryUsageBytes: Long,
    val capabilities: List<String>
) : AriaBaseResponse {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readLong(),
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(transactionId)
        parcel.writeString(version)
        parcel.writeString(state)
        parcel.writeString(playbackState)
        parcel.writeInt(queueSize)
        parcel.writeLong(memoryUsageBytes)
        parcel.writeStringList(capabilities)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<HealthResponse> {
        override fun createFromParcel(parcel: Parcel): HealthResponse = HealthResponse(parcel)
        override fun newArray(size: Int): Array<HealthResponse?> = arrayOfNulls(size)
    }
}
