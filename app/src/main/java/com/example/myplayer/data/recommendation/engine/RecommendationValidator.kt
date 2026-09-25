package com.example.myplayer.data.recommendation.engine

import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.logging.RecommendationMetrics
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.utils.RecommendationConstants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationValidator @Inject constructor(
    private val logger: RecommendationLogger,
    private val metrics: RecommendationMetrics
) {
    fun filterAndValidate(
        seed: RecommendationSeed,
        candidates: List<RecommendationSong>
    ): List<RecommendationSong> {
        val seenIds = mutableSetOf<String>()
        val validSongs = mutableListOf<RecommendationSong>()

        for (candidate in candidates) {
            // Reject: Null/Blank IDs
            if (candidate.videoId.isBlank()) {
                logger.log("Validation Failed: Blank videoId found")
                continue
            }

            // Reject: Current playing song
            if (seed.songId.isNotBlank() && candidate.videoId == seed.songId) {
                // This is expected, no need to log as failure unless verbose
                continue
            }

            // Reject: Duplicate songs
            if (!seenIds.add(candidate.videoId)) {
                logger.log("Validation Failed: Duplicate song ${candidate.videoId}")
                continue
            }

            // Reject: Null/Blank title or artist
            if (candidate.title.isBlank() || candidate.artist.isBlank()) {
                logger.log("Validation Failed: Blank title or artist for ${candidate.videoId}")
                continue
            }

            validSongs.add(candidate)
        }
        
        logger.log("Validation Passed for ${validSongs.size} out of ${candidates.size} candidates.")
        return validSongs
    }
}
