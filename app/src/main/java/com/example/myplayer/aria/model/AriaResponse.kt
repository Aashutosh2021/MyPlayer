package com.example.myplayer.aria.model

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

data class AriaResponse(
    val status: AriaStatus,
    val commandOrdinal: Int,
    val errorMessage: String? = null,
    val payload: Bundle? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        AriaStatus.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readString(),
        parcel.readBundle(AriaResponse::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.ordinal)
        parcel.writeInt(commandOrdinal)
        parcel.writeString(errorMessage)
        parcel.writeBundle(payload)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AriaResponse> {
        override fun createFromParcel(parcel: Parcel): AriaResponse = AriaResponse(parcel)
        override fun newArray(size: Int): Array<AriaResponse?> = arrayOfNulls(size)
    }
}
