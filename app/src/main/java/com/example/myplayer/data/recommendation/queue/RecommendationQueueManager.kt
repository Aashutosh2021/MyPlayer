package com.example.myplayer.data.recommendation.queue

import com.example.myplayer.data.recommendation.RecommendationRepository
import com.example.myplayer.data.recommendation.engine.RecommendationValidator
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.strategy.RecommendationStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class RecommendationQueueManager @Inject constructor(
    private val queue: RecommendationQueue,
    private val policy: RecommendationQueuePolicy,
    private val history: RecommendationHistory,
    private val filter: RecommendationFilter,
    private val repository: RecommendationRepository,
    private val validator: RecommendationValidator,
    private val strategy: RecommendationStrategy,
    private val metrics: QueueMetrics,
    private val recentPlaybackWindow: RecentPlaybackWindow,
    private val health: RecommendationQueueHealth,
    private val logger: RecommendationLogger
) {
    private var session: RecommendationSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isRefilling = false
    
    // 15 minutes TTL
    private val QUEUE_TTL_MS = 15L * 60L * 1000L

    fun updateSession(seed: RecommendationSeed) {
        val isNewSeed = session != null && session?.seedSong?.songId != seed.songId
        if (session == null) {
            logger.logEvent("SessionStarted", mapOf("seedSongId" to seed.songId))
            session = RecommendationSession(seedSong = seed)
            queue.clear()
            history.clear()
            recentPlaybackWindow.clear()
        } else {
            logger.logEvent("SessionUpdated", mapOf("newSeedSongId" to seed.songId))
            session?.seedSong = seed
            
            // Validate TTL or seed change
            if (isNewSeed || queue.isExpired(QUEUE_TTL_MS)) {
                if (isNewSeed) {
                    logger.log("New seed song detected (${seed.songId}), refreshing recommendation queue")
                } else {
                    logger.logEvent("QueueExpired", mapOf("ttlMs" to QUEUE_TTL_MS))
                }
                val sizeBeforeClear = queue.size()
                queue.clear()
                health.recordExpiredEntries(sizeBeforeClear)
            }
        }
        
        if (seed.songId.isNotBlank()) {
            recentPlaybackWindow.add(seed.songId)
            history.recordState(seed.songId, RecommendationHistoryState.PLAYED)
        }
        checkAndRefillQueue(seed)
    }

    @Synchronized
    fun enqueue(songs: List<RecommendationSong>) {
        queue.enqueue(songs)
    }

    suspend fun dequeue(): RecommendationSong? {
        val nextSong = queue.dequeue()

        if (nextSong != null) {
            history.recordState(nextSong.videoId, RecommendationHistoryState.RECOMMENDED)
            session?.let { it.recommendationsPlayed++ }
            health.recordQueueSize(queue.size())
            logger.log("Dequeued next recommendation: ${nextSong.videoId}. Queue size is now ${queue.size()}")
        } else {
            health.recordEmptyQueue()
        }
        checkAndRefillQueue(session?.seedSong)
        return nextSong
    }

    fun peekNext(): RecommendationSong? = queue.peek()
    
    fun peekNext(count: Int): List<RecommendationSong> = queue.peekNext(count)
    
    val queueState = queue.queueState
    fun getQueueSnapshot(): List<RecommendationSong> = queue.getSnapshot()

    fun rejectSong(videoId: String) {
        logger.log("Rejected song: $videoId")
        history.recordState(videoId, RecommendationHistoryState.REJECTED)
        session?.let { it.recommendationsRejected++ }
        if (queue.remove(videoId)) {
            checkAndRefillQueue(session?.seedSong)
        }
    }

    fun skipSong(videoId: String) {
        logger.log("Skipped song: $videoId")
        history.recordState(videoId, RecommendationHistoryState.SKIPPED)
        session?.let { it.recommendationsSkipped++ }
    }

    fun acceptSong(videoId: String) {
        history.recordState(videoId, RecommendationHistoryState.ACCEPTED)
        session?.let { it.recommendationsPlayed++ }
    }

    fun refreshQueue() {
        queue.clear()
        checkAndRefillQueue(session?.seedSong)
    }

    fun invalidateQueue() {
        queue.clear()
    }

    fun clearQueue() {
        queue.clear()
        history.clear()
        recentPlaybackWindow.clear()
        session?.endedAt = System.currentTimeMillis()
        session = null
    }

    fun size(): Int = queue.size()

    fun isEmpty(): Boolean = queue.isEmpty()

    /**
     * Performs a synchronous fetch to recover an empty queue.
     * Returns true if songs were successfully enqueued.
     */
    suspend fun recoverQueueSynchronously(): Boolean {
        val seed = session?.seedSong ?: return false
        logger.logEvent("RecoveryStarted", mapOf("seedId" to seed.songId))
        
        try {
            val recommendations = repository.getRecommendations(seed)
            val filtered = filter.filter(recommendations, seed, queue.getQueuedIds())
            val valid = validator.filterAndValidate(seed, filtered)
            val ranked = strategy.scoreAndRank(seed, valid)

            if (ranked.isNotEmpty()) {
                queue.enqueue(ranked)
                health.recordRefill()
                logger.logEvent("RecoverySucceeded", mapOf("recoveredSize" to ranked.size))
                return true
            } else {
                logger.logEvent("RecoveryFailed", mapOf("reason" to "No valid recommendations after filter/rank"))
                return false
            }
        } catch (e: Exception) {
            logger.logEvent("RecoveryFailed", mapOf("reason" to (e.message ?: "Unknown Exception")))
            return false
        }
    }

    private fun checkAndRefillQueue(seed: RecommendationSeed?) {
        if (seed == null) return
        if (isRefilling) return

        if (policy.shouldRefill(queue.size())) {
            val sizeBefore = queue.size()
            logger.logEvent("QueueRefillStarted", mapOf(
                "sizeBefore" to sizeBefore,
                "targetSize" to policy.targetSize
            ))
            isRefilling = true
            scope.launch {
                val fillTime = measureTimeMillis {
                    try {
                        session?.let { it.totalRecommendationRequests++ }
                        
                        // 1. Fetch raw/mapped from repository
                        val recommendations = repository.getRecommendations(seed)
                        
                        // 2. Filter (History, Duplicate, Current, Queued)
                        val filtered = filter.filter(recommendations, seed, queue.getQueuedIds())
                        
                        // 3. Validate
                        val valid = validator.filterAndValidate(seed, filtered)
                        
                        // 4. Rank
                        val ranked = strategy.scoreAndRank(seed, valid)

                        if (ranked.isNotEmpty()) {
                            queue.enqueue(ranked)
                            health.recordRefill()
                            session?.let { 
                                it.queueRefills++
                                it.recommendationsGenerated += recommendations.size
                                it.recommendationsQueued += ranked.size
                            }
                        }
                        metrics.recordQueueRefresh(0, ranked.size)
                        
                        logger.logEvent("QueueRefillCompleted", mapOf(
                            "generated" to recommendations.size,
                            "valid" to valid.size,
                            "ranked" to ranked.size,
                            "newQueueSize" to queue.size()
                        ))
                    } catch (e: Exception) {
                        logger.error("Error during queue refill", e)
                        logger.logEvent("QueueRefillFailed", mapOf("reason" to (e.message ?: "Unknown")))
                    } finally {
                        isRefilling = false
                    }
                }
                session?.let { it.totalRecommendationTimeMs += fillTime }
                metrics.recordQueueRefresh(fillTime, 0)
                logger.logEvent("QueueRefillMetrics", mapOf(
                    "fillTimeMs" to fillTime,
                    "sessionId" to (session?.sessionId ?: "none")
                ))
            }
        }
    }
}
