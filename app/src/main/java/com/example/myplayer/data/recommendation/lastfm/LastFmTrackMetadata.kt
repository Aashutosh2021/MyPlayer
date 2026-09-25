package com.example.myplayer.data.recommendation.lastfm

data class LastFmTrackMetadata(
    val name: String,
    val artist: String,
    val playcount: Long = 0,
    val listeners: Long = 0,
    val durationSeconds: Long = 0,
    val imageUrl: String = "",
    val mbid: String = ""
)
