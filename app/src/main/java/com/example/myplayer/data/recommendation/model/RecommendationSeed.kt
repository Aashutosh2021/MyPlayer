package com.example.myplayer.data.recommendation.model

data class RecommendationSeed(
    val songId: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val language: String = "",
    val mood: String = "",
    val year: Int = 0,
    val durationMs: Long = 0,
    val source: String = "unknown"
)
