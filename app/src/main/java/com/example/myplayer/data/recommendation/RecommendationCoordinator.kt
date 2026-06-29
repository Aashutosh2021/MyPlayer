package com.example.myplayer.data.recommendation

import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.queue.RecommendationQueueManager
import com.example.myplayer.data.recommendation.playback.RecommendationPlaybackRepository
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
    private val playbackRepository: RecommendationPlaybackRepository,
    private val logger: RecommendationLogger
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Called whenever an online playback starts.
     * Starts or updates the continuous session and fetches recommendations.
     */
    fun onPlaybackStarted(song: OnlineSong) {
        val seed = RecommendationSeed(
            songId = song.videoId,
            artist = song.artist,
            title = song.title,
            album = "", // OnlineSong doesn't map album directly here
            durationMs = song.durationMs,
            source = "youtube"
        )
        startRecommendationSession(seed)
    }

    /**
     * Called whenever a local or downloaded song starts playing.
     * Builds a seed from the local metadata so recommendations can still be
     * generated (sourced from YouTube via artist/title matching).
     */
    fun onPlaybackStarted(song: SongEntity) {
        // A local song id is not a YouTube videoId, so we leave songId distinct
        // from the search query (which is built from artist/title).
        val seed = RecommendationSeed(
            songId = song.id,
            artist = song.artist,
            title = song.title,
            album = song.album,
            durationMs = song.duration,
            source = "local"
        )
        startRecommendationSession(seed)
    }

    private fun startRecommendationSession(seed: RecommendationSeed) {
        scope.launch {
            try {
                logger.logEvent("PlaybackStarted", mapOf(
                    "songId" to seed.songId,
                    "artist" to seed.artist,
                    "source" to seed.source
                ))

                // 1. Explicitly fetch raw data into repo cache
                recommendationManager.preloadRecommendations(seed)

                // 2. Trigger QueueManager to validate TTL and generate queue if needed
                queueManager.updateSession(seed)

            } catch (e: Exception) {
                logger.error("RecommendationCoordinator failed to handle playback started", e)
                logger.logEvent("PlaybackFailed", mapOf("reason" to (e.message ?: "Unknown")))
            }
        }
    }

    /**
     * Resolves the next recommendation from the queue and fetches its stream URL.
     * Keeps dequeuing if stream URLs fail to resolve.
     */
    suspend fun getNextAutoplaySong(): OnlineSong? {
        while (!queueManager.isEmpty()) {
            val rec = queueManager.dequeue() ?: break

            val song = playbackRepository.resolveStreamUrl(rec)
            if (song != null) {
                logger.logEvent("AutoplayRecommendationSelected", mapOf(
                    "videoId" to rec.videoId,
                    "score" to rec.recommendationScore
                ))
                return song
            }
        }
        
        logger.logEvent("QueueEmpty", mapOf("action" to "Attempting recovery"))
        
        // Queue empty, attempt one synchronous recovery
        val recovered = queueManager.recoverQueueSynchronously()
        if (recovered) {
            while (!queueManager.isEmpty()) {
                val rec = queueManager.dequeue() ?: break
                val song = playbackRepository.resolveStreamUrl(rec)
                if (song != null) {
                    logger.logEvent("AutoplayRecommendationSelected", mapOf(
                        "videoId" to rec.videoId,
                        "score" to rec.recommendationScore,
                        "context" to "Recovered"
                    ))
                    return song
                }
            }
        }

        return null
    }

    val queueState = queueManager.queueState

    /**
     * Terminate the session entirely (e.g., app close or service death).
     */
    fun endSession() {
        logger.logEvent("SessionEnded", emptyMap())
        queueManager.clearQueue()
        recommendationManager.reset()
    }

    /**
     * Plays a specific recommendation from the UI.
     * Resolves stream URL and returns an OnlineSong for playback.
     */
    suspend fun playRecommendation(song: com.example.myplayer.data.recommendation.model.RecommendationSong): OnlineSong? {
        val resolved = playbackRepository.resolveStreamUrl(song)
        if (resolved != null) {
            queueManager.acceptSong(song.videoId)
        }
        return resolved
    }

    /**
     * Rejects a recommendation from the queue.
     */
    fun rejectRecommendation(videoId: String) {
        queueManager.rejectSong(videoId)
    }

    /**
     * Refreshes the recommendation queue.
     */
    fun refreshQueue() {
        queueManager.refreshQueue()
    }
}
