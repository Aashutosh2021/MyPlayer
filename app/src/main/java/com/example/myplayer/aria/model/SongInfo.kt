package com.example.myplayer.aria.model

import android.os.Parcel
import android.os.Parcelable

data class SongInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val albumArtUrl: String?,
    val isOnline: Boolean,
    val playCount: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(album)
        parcel.writeLong(durationMs)
        parcel.writeString(path)
        parcel.writeString(albumArtUrl)
        parcel.writeByte(if (isOnline) 1 else 0)
        parcel.writeInt(playCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SongInfo> {
        override fun createFromParcel(parcel: Parcel): SongInfo = SongInfo(parcel)
        override fun newArray(size: Int): Array<SongInfo?> = arrayOfNulls(size)
    }
}
