package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Concrete wrapper for [AppResponse] to allow passing it over AIDL.
 */
@Parcelize
data class AppResponseContainer(val response: AppResponse) : Parcelable
