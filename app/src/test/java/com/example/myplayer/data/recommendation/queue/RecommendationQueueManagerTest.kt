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

        val recentPlaybackWindow = RecentPlaybackWindow()
        val health = RecommendationQueueHealth(logger)
        
        queueManager = RecommendationQueueManager(
            queue, policy, history, RecommendationFilter(history, recentPlaybackWindow, queueMetrics), repository, validator, strategy, queueMetrics, recentPlaybackWindow, health, logger
        )
    }

    @Test
    fun testConcurrentEnqueueAndDequeue() = runBlocking(Dispatchers.Default) {
        val seed = RecommendationSeed("seed1", "artist1")
        queueManager.updateSession(seed)

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

    @Test
    fun testAutoplaySessionContinuationPreservesQueue() = runBlocking(Dispatchers.Default) {
        val seed1 = RecommendationSeed("seed_initial", "Artist Initial")
        queueManager.updateSession(seed1)

        val songs = (1..5).map { i ->
            RecommendationSong(
                videoId = "rec_song_$i",
                title = "Title $i",
                artist = "Artist $i",
                source = "Test",
                recommendationScore = 100 - i,
                popularityScore = 50,
                durationMs = 180000L,
                thumbnailUrl = ""
            )
        }
        queueManager.enqueue(songs)
        assertEquals(5, queueManager.size())

        // 1. Next song is dequeued for autoplay
        val dequeued = queueManager.dequeue()
        assertEquals("rec_song_1", dequeued?.videoId)
        assertEquals(4, queueManager.size())

        // 2. Playback starts for the dequeued song: updateSession is called with dequeued song as seed
        val nextSeed = RecommendationSeed("rec_song_1", "Artist 1")
        queueManager.updateSession(nextSeed)

        // Queue must NOT be wiped! It must preserve the remaining 4 songs
        assertEquals("Autoplay session continuation must preserve queued recommendations", 4, queueManager.size())
        val nextInQueue = queueManager.peekNext()
        assertEquals("rec_song_2", nextInQueue?.videoId)
    }

    @Test
    fun testQueueDeduplication() = runBlocking(Dispatchers.Default) {
        val song1 = RecommendationSong(videoId = "duplicate_id", title = "Title 1", artist = "Artist", durationMs = 0L, thumbnailUrl = "")
        val song2 = RecommendationSong(videoId = "duplicate_id", title = "Title 1 Dupe", artist = "Artist", durationMs = 0L, thumbnailUrl = "")
        val song3 = RecommendationSong(videoId = "unique_id", title = "Title 2", artist = "Artist", durationMs = 0L, thumbnailUrl = "")

        queueManager.enqueue(listOf(song1, song2, song3))

        assertEquals("Duplicate videoId entries must be filtered out", 2, queueManager.size())
    }
}
