package com.example.myplayer.data.recommendation.model

/**
 * Immutable domain model representing a recommended song.
 * Kept strictly independent from the OnlineSong API model.
 */
data class RecommendationSong(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long
)
