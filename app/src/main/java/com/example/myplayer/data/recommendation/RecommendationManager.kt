package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.strategy.RecommendationStrategy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator for the Recommendation Engine.
 * Provides a clean interface for the future playback layer (e.g., autoplay integration).
 * Operates purely on data, containing no actual playback logic.
 */
@Singleton
class RecommendationManager @Inject constructor(
    private val repository: RecommendationRepository
) {
    /**
     * Retrieves the next recommended song based on the currently playing song.
     * Returns null if no recommendations are found.
     */
    suspend fun getNextRecommendation(currentSong: OnlineSong): RecommendationSong? {
        val recommendations = repository.getRecommendations(currentSong)
        // Future logic can be added here to filter out already played songs
        return recommendations.firstOrNull()
    }
    
    /**
     * Explicitly preloads recommendations for a given song.
     */
    suspend fun preloadRecommendations(song: OnlineSong) {
        repository.preloadRecommendations(song)
    }

    /**
     * Clears all cached recommendations (e.g., when the user clears queue).
     */
    fun reset() {
        repository.clearCache()
    }
    
    /**
     * Set a new active ranking strategy.
     */
    fun setStrategy(strategy: RecommendationStrategy) {
        repository.setStrategy(strategy)
    }
}
