package com.example.myplayer.data.recommendation

import com.example.myplayer.data.recommendation.model.RecommendationResult
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator for the Recommendation Engine data layer.
 * Operates purely on data, containing no actual playback or queue logic.
 */
@Singleton
class RecommendationManager @Inject constructor(
    private val repository: RecommendationRepository
) {
    /**
     * Generates unranked recommendations for a given seed.
     */
    suspend fun generateRecommendations(seed: RecommendationSeed): List<RecommendationSong> {
        return repository.getRecommendations(seed)
    }
    
    /**
     * Explicitly preloads recommendations into the backend repository cache.
     */
    suspend fun preloadRecommendations(seed: RecommendationSeed) {
        repository.preloadRecommendations(seed)
    }

    /**
     * Clears all cached recommendations.
     */
    fun reset() {
        repository.clearCache()
    }
}
