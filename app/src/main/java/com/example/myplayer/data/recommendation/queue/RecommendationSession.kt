package com.example.myplayer.data.recommendation.queue

import com.example.myplayer.data.recommendation.model.RecommendationSeed
import java.util.UUID

data class RecommendationSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val startedAt: Long = System.currentTimeMillis(),
    var endedAt: Long = 0,
    var seedSong: RecommendationSeed? = null,
    var recommendationsGenerated: Int = 0,
    var recommendationsQueued: Int = 0,
    var recommendationsPlayed: Int = 0,
    var recommendationsSkipped: Int = 0,
    var recommendationsRejected: Int = 0,
    var queueRefills: Int = 0,
    var totalRecommendationRequests: Int = 0,
    var totalRecommendationTimeMs: Long = 0
) {
    val averageRecommendationTime: Long
        get() = if (totalRecommendationRequests == 0) 0 else totalRecommendationTimeMs / totalRecommendationRequests

    val sessionDurationMs: Long
        get() = (if (endedAt == 0L) System.currentTimeMillis() else endedAt) - startedAt
}
