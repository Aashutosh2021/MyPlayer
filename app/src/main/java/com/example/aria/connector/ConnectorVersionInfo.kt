package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Version metadata for a connected app. Used for handshake validation and backward compatibility.
 */
@Parcelize
data class ConnectorVersionInfo(
    val sdkVersion: Int,
    val appVersionCode: Int,
    val appVersionName: String,
    val supportedFeatures: List<String>,
    val optionalFeatures: List<String> = emptyList(),
    val experimentalFeatures: List<String> = emptyList(),
) : Parcelable
