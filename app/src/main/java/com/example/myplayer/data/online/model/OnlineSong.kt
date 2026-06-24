package com.example.myplayer.data.online.model

data class OnlineSong(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val durationText: String = "",
    var streamUrl: String? = null
)
