package com.example.myplayer.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long,
    val videoId: String? = null
)
