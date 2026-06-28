package com.example.myplayer.data.recommendation.queue

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.RecommendationRepository
import com.example.myplayer.data.recommendation.api.RecommendationSource
import com.example.myplayer.data.recommendation.cache.RecommendationCache
import com.example.myplayer.data.recommendation.engine.RecommendationEngine
import com.example.myplayer.data.recommendation.engine.RecommendationValidator
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.logging.RecommendationMetrics
import com.example.myplayer.data.recommendation.mapper.RecommendationMapper
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.strategy.DefaultRankingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecommendationQueueManagerTest {

    private lateinit var queueManager: RecommendationQueueManager
    private lateinit var queue: RecommendationQueue
    private lateinit var history: RecommendationHistory

    @Before
    fun setup() {
        queue = RecommendationQueue()
        history = RecommendationHistory()
        val policy = DefaultQueuePolicy()
        val queueMetrics = QueueMetrics()

        val logger = RecommendationLogger()
        val metrics = RecommendationMetrics()
        val cache = RecommendationCache(logger)

        val mapper = RecommendationMapper()
        val source = object : RecommendationSource {
            override val sourceName = "Test"
            override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> = emptyList()
        }
        val engine = RecommendationEngine(source, mapper, logger, metrics)
        val repository = RecommendationRepository(engine, cache)

        val strategy = DefaultRankingStrategy()
        val validator = RecommendationValidator(logger, metrics)

        queueManager = RecommendationQueueManager(
            queue, policy, history, RecommendationFilter(history, queueMetrics), repository, validator, strategy, queueMetrics, logger
        )
    }

    @Test
    fun testConcurrentEnqueueAndDequeue() = runBlocking(Dispatchers.Default) {
        val seed = RecommendationSeed("seed1", "artist1")
        queueManager.startSession(seed)

        val enqueueCount = 1000
        val dequeueCount = 500
        val songs = (1..enqueueCount).map { i ->
            RecommendationSong(
                videoId = "song_$i",
                title = "Title $i",
                artist = "Artist",
                source = "Test",
                recommendationScore = i,
                popularityScore = i,
                durationMs = 0L,
                thumbnailUrl = ""
            )
        }

        val enqueueJob = async {
            songs.chunked(10).forEach { chunk ->
                queueManager.enqueue(chunk)
            }
        }

        val dequeueJob = async {
            var dequeued = 0
            while (dequeued < dequeueCount) {
                if (queueManager.dequeue() != null) {
                    dequeued++
                }
            }
        }

        awaitAll(enqueueJob, dequeueJob)

        assertEquals(enqueueCount - dequeueCount, queueManager.size())
    }
}
