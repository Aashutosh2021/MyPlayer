package com.example.myplayer.data.recommendation

import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.playback.RecommendationPlaybackRepository
import com.example.myplayer.data.recommendation.queue.*
import com.example.myplayer.playback.PlaybackCompletionGuard
import com.example.myplayer.playback.PlaybackEvent
import com.example.myplayer.playback.PlaybackEventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RecommendationAutoplayTest {

    private lateinit var playbackEventBus: PlaybackEventBus
    private lateinit var fakeSettingsDataStore: FakeSettingsDataStore
    private lateinit var queueManager: RecommendationQueueManager
    private lateinit var playbackRepository: RecommendationPlaybackRepository
    private lateinit var logger: RecommendationLogger

    @Before
    fun setup() {
        playbackEventBus = PlaybackEventBus()
        fakeSettingsDataStore = FakeSettingsDataStore()
        playbackRepository = RecommendationPlaybackRepository()
        logger = RecommendationLogger()

        // Real QueueManager with internal components
        val queue = RecommendationQueue()
        val policy = DefaultQueuePolicy()
        val history = RecommendationHistory()
        val metrics = QueueMetrics()
        val recentPlaybackWindow = RecentPlaybackWindow()
        val recoMetrics = com.example.myplayer.data.recommendation.logging.RecommendationMetrics()
        val cache = com.example.myplayer.data.recommendation.cache.RecommendationCache(logger)
        val engine = com.example.myplayer.data.recommendation.engine.RecommendationEngine(
            object : com.example.myplayer.data.recommendation.api.RecommendationSource {
                override val sourceName = "Test"
                override suspend fun fetchRawRecommendations(seed: com.example.myplayer.data.recommendation.model.RecommendationSeed) = emptyList<com.example.myplayer.data.online.model.OnlineSong>()
            },
            com.example.myplayer.data.recommendation.mapper.RecommendationMapper(),
            logger,
            recoMetrics
        )
        val repo = RecommendationRepository(engine, cache)
        val validator = com.example.myplayer.data.recommendation.engine.RecommendationValidator(logger, recoMetrics)
        val strat = com.example.myplayer.data.recommendation.strategy.DefaultRankingStrategy()
        val health = RecommendationQueueHealth(logger)

        queueManager = RecommendationQueueManager(
            queue, policy, history,
            RecommendationFilter(history, recentPlaybackWindow, metrics),
            repo, validator, strat, metrics, recentPlaybackWindow, health, logger
        )
    }

    @Test
    fun testPlaybackCompletionGuard_RejectsDuplicateStateEnded() {
        val guard = PlaybackCompletionGuard()

        val firstCheck = guard.isAlreadyHandled("song_123")
        assertFalse("First completion should be processed", firstCheck)

        val duplicateCheck = guard.isAlreadyHandled("song_123")
        assertTrue("Duplicate completion for same mediaId must be rejected", duplicateCheck)

        guard.reset()
        val afterResetCheck = guard.isAlreadyHandled("song_123")
        assertFalse("After reset, new completion should be allowed", afterResetCheck)
    }

    @Test
    fun testAutoplay_WhenEnabled_DequeuesAndEmitsPlayRequest() = runBlocking {
        fakeSettingsDataStore.autoplayEnabled = true

        val candidate = RecommendationSong(
            videoId = "next_rec_vid",
            title = "Recommended Title",
            artist = "Recommended Artist",
            durationMs = 210000L,
            thumbnailUrl = "https://thumb.url"
        )
        // Enqueue directly into real queue
        queueManager.enqueue(listOf(candidate))

        val coordinator = RecommendationCoordinator(
            playbackEventBus = playbackEventBus,
            settingsDataStore = fakeSettingsDataStore,
            recommendationManager = mockManager(),
            queueManager = queueManager,
            playbackRepository = playbackRepository,
            logger = logger
        ).apply {
            networkAvailabilityOverride = true
        }

        val emittedEvents = mutableListOf<PlaybackEvent>()
        val job = launch {
            playbackEventBus.events.collect { event ->
                if (event is PlaybackEvent.PlayRequestReady) {
                    emittedEvents.add(event)
                }
            }
        }

        playbackEventBus.emit(PlaybackEvent.SongCompleted("current_song_id"))

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertEquals(1, emittedEvents.size)
        val playRequestReady = emittedEvents[0] as PlaybackEvent.PlayRequestReady
        assertEquals("next_rec_vid", playRequestReady.requests[0].songId)
        assertEquals("Recommended Title", playRequestReady.requests[0].title)
    }

    @Test
    fun testAutoplay_WhenDisabled_DoesNotEmitPlayRequest() = runBlocking {
        fakeSettingsDataStore.autoplayEnabled = false

        val candidate = RecommendationSong(
            videoId = "candidate_should_not_play",
            title = "Candidate",
            artist = "Artist",
            durationMs = 200000L,
            thumbnailUrl = ""
        )
        queueManager.enqueue(listOf(candidate))

        val coordinator = RecommendationCoordinator(
            playbackEventBus = playbackEventBus,
            settingsDataStore = fakeSettingsDataStore,
            recommendationManager = mockManager(),
            queueManager = queueManager,
            playbackRepository = playbackRepository,
            logger = logger
        ).apply {
            networkAvailabilityOverride = true
        }

        val emittedEvents = mutableListOf<PlaybackEvent>()
        val job = launch {
            playbackEventBus.events.collect { event ->
                if (event is PlaybackEvent.PlayRequestReady) {
                    emittedEvents.add(event)
                }
            }
        }

        playbackEventBus.emit(PlaybackEvent.SongCompleted("current_song_id"))

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertTrue("When autoplay is disabled, no PlayRequestReady must be emitted", emittedEvents.isEmpty())
        assertEquals("Queue item should remain when autoplay is disabled", 1, queueManager.size())
    }

    @Test
    fun testAutoplay_WhenAutoplayRequestedEmitted_PlaysNextRecommendation() = runBlocking {
        fakeSettingsDataStore.autoplayEnabled = true

        val song1 = RecommendationSong(
            videoId = "failed_song_vid",
            title = "Failed Song",
            artist = "Artist",
            durationMs = 200000L,
            thumbnailUrl = ""
        )
        val song2 = RecommendationSong(
            videoId = "next_working_song_vid",
            title = "Next Working Song",
            artist = "Artist",
            durationMs = 200000L,
            thumbnailUrl = ""
        )
        queueManager.enqueue(listOf(song1, song2))

        val coordinator = RecommendationCoordinator(
            playbackEventBus = playbackEventBus,
            settingsDataStore = fakeSettingsDataStore,
            recommendationManager = mockManager(),
            queueManager = queueManager,
            playbackRepository = playbackRepository,
            logger = logger
        ).apply {
            networkAvailabilityOverride = true
        }

        val emittedEvents = mutableListOf<PlaybackEvent>()
        val job = launch {
            playbackEventBus.events.collect { event ->
                if (event is PlaybackEvent.PlayRequestReady) {
                    emittedEvents.add(event)
                }
            }
        }

        // Simulate terminal recovery failure emitting AutoplayRequested
        playbackEventBus.emit(PlaybackEvent.AutoplayRequested)

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertEquals("Next recommendation should be dequeued and requested", 1, emittedEvents.size)
        val ready = emittedEvents[0] as PlaybackEvent.PlayRequestReady
        assertEquals("failed_song_vid", ready.requests[0].songId) // first item dequeued
    }

    @Test
    fun testAutoplay_ContinuousSequence_Plays10SongsContinuously() = runBlocking {
        fakeSettingsDataStore.autoplayEnabled = true

        // Enqueue 10 recommendation songs
        val songs = (1..10).map { i ->
            RecommendationSong(
                videoId = "rec_song_$i",
                title = "Title $i",
                artist = "Artist $i",
                durationMs = 180000L,
                thumbnailUrl = ""
            )
        }
        queueManager.enqueue(songs)
        assertEquals("Queue should have 10 songs", 10, queueManager.size())

        val coordinator = RecommendationCoordinator(
            playbackEventBus = playbackEventBus,
            settingsDataStore = fakeSettingsDataStore,
            recommendationManager = mockManager(),
            queueManager = queueManager,
            playbackRepository = playbackRepository,
            logger = logger
        ).apply {
            networkAvailabilityOverride = true
        }

        val playedSongIds = mutableListOf<String>()
        val job = launch {
            playbackEventBus.events.collect { event ->
                if (event is PlaybackEvent.PlayRequestReady) {
                    playedSongIds.add(event.requests[0].songId)
                }
            }
        }

        // Simulate each song completing in sequence
        for (i in 1..10) {
            playbackEventBus.emit(PlaybackEvent.SongCompleted("rec_song_$i"))
            kotlinx.coroutines.delay(50)
        }

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertEquals("All 10 songs should have been emitted for continuous autoplay", 10, playedSongIds.size)
        assertEquals("All 10 songs must be represented in played history", (1..10).map { "rec_song_$it" }.toSet(), playedSongIds.toSet())
        assertEquals("Queue should be empty after 10 songs", 0, queueManager.size())
    }

    private fun mockManager(): RecommendationManager {
        val cache = com.example.myplayer.data.recommendation.cache.RecommendationCache(logger)
        val engine = com.example.myplayer.data.recommendation.engine.RecommendationEngine(
            object : com.example.myplayer.data.recommendation.api.RecommendationSource {
                override val sourceName = "Mock"
                override suspend fun fetchRawRecommendations(seed: com.example.myplayer.data.recommendation.model.RecommendationSeed) = emptyList<com.example.myplayer.data.online.model.OnlineSong>()
            },
            com.example.myplayer.data.recommendation.mapper.RecommendationMapper(),
            logger,
            com.example.myplayer.data.recommendation.logging.RecommendationMetrics()
        )
        return RecommendationManager(RecommendationRepository(engine, cache))
    }

    private class FakeSettingsDataStore : SettingsDataStore() {
        var autoplayEnabled = true
        override val isAutoplayEnabled: Flow<Boolean>
            get() = flowOf(autoplayEnabled)
    }
}
