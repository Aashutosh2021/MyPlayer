package com.example.myplayer.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncDevice(
    val deviceId: String,
    val deviceName: String,
    val role: String, // "MASTER", "SLAVE"
    val isReady: Boolean = false,
    val currentTrackTitle: String? = null,
    val isTrackAvailable: Boolean = true,
    val unavailableReason: String? = null,
    val latencyMs: Long = 0L,
    val driftMs: Long = 0L
)
