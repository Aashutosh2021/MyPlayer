package com.example.myplayer.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncSession(
    val sessionId: String,
    val sessionName: String,
    val masterDeviceId: String,
    val masterDeviceName: String,
    val hostAddress: String? = null,
    val port: Int = 48950,
    val devices: List<SyncDevice> = emptyList()
)
