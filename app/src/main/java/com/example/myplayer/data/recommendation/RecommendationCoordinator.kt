package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.queue.RecommendationQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrator for the entire Recommendation subsystem.
 * Handles lifecycle events from the playback engine and coordinates the Manager and QueueManager.
 */
@Singleton
class RecommendationCoordinator @Inject constructor(
    private val recommendationManager: RecommendationManager,
    private val queueManager: RecommendationQueueManager,
    private val logger: RecommendationLogger
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Called whenever an online playback starts.
     * Starts or updates the continuous session and fetches recommendations.
     */
    fun onPlaybackStarted(song: OnlineSong) {
        scope.launch {
            try {
                val seed = RecommendationSeed(
                    songId = song.videoId,
                    artist = song.artist,
                    album = "", // OnlineSong doesn't map album directly here
                    durationMs = song.durationText.toLongOrNull() ?: 0L,
                    source = "youtube"
                )

                logger.logEvent("PlaybackStarted", mapOf(
                    "songId" to song.videoId,
                    "artist" to song.artist
                ))

                // 1. Explicitly fetch raw data into repo cache
                recommendationManager.preloadRecommendations(seed)

                // 2. Trigger QueueManager to validate TTL and generate queue if needed
                queueManager.updateSession(seed)

            } catch (e: Exception) {
                logger.error("RecommendationCoordinator failed to handle playback started", e)
            }
        }
    }

    /**
     * Terminate the session entirely (e.g., app close or service death).
     */
    fun endSession() {
        logger.logEvent("SessionEnded", emptyMap())
        queueManager.clearQueue()
        recommendationManager.reset()
    }
}
