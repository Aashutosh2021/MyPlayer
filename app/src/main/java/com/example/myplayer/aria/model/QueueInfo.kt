package com.example.myplayer.aria.model

import android.os.Parcel
import android.os.Parcelable

data class QueueInfo(
    val songs: List<SongInfo>,
    val currentIndex: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createTypedArrayList(SongInfo.CREATOR) ?: emptyList(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(songs)
        parcel.writeInt(currentIndex)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<QueueInfo> {
        override fun createFromParcel(parcel: Parcel): QueueInfo = QueueInfo(parcel)
        override fun newArray(size: Int): Array<QueueInfo?> = arrayOfNulls(size)
    }
}
