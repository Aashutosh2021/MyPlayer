package com.example.myplayer.data.recommendation.model

/**
 * Immutable domain model for a recommended song.
 * Contains all necessary metadata for UI display and future autoplay playback.
 */
data class RecommendationSong(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long,
    val thumbnailUrl: String,
    val popularityScore: Int = 0,
    val recommendationScore: Int = 0,
    val source: String = "unknown",
    val reason: String = "",
    val metadata: Map<String, String> = emptyMap()
)
