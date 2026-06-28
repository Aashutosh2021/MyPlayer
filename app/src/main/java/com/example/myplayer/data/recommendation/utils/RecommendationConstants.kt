package com.example.myplayer.data.recommendation.utils

object RecommendationConstants {
    // Scoring
    const val SCORE_SAME_ARTIST = 50
    const val SCORE_SAME_ALBUM = 30
    const val SCORE_SAME_GENRE = 20
    const val SCORE_POPULARITY = 10
    const val SCORE_RECENT = 5

    // Cache limits
    const val MAX_CACHE_SIZE = 100
    const val CACHE_EXPIRATION_MS = 15L * 60L * 1000L // 15 minutes
}
