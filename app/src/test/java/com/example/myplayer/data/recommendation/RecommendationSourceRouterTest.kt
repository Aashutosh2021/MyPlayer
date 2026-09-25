package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.api.LastFmRecommendationSource
import com.example.myplayer.data.recommendation.api.RecommendationSourceRouter
import com.example.myplayer.data.recommendation.api.YouTubeRecommendationSource
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.queue.RecommendationHistory
import com.example.myplayer.data.recommendation.state.RecommendationUserState
import com.example.myplayer.data.recommendation.state.RecommendationUserStateDetector
import com.example.myplayer.data.repository.RecentHistoryRepository
import com.example.myplayer.playback.PlaybackStateManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationSourceRouterTest {

    private lateinit var fakeDetector: FakeUserStateDetector
    private lateinit var fakeLastFmSource: FakeLastFmSource
    private lateinit var fakeYouTubeSource: FakeYouTubeSource
    private lateinit var router: RecommendationSourceRouter

    @Before
    fun setup() {
        fakeDetector = FakeUserStateDetector()
        fakeLastFmSource = FakeLastFmSource()
        fakeYouTubeSource = FakeYouTubeSource()

        router = RecommendationSourceRouter(
            userStateDetector = fakeDetector.asDetector(),
            lastFmSource = fakeLastFmSource.asSource(),
            youTubeRadioSource = fakeYouTubeSource.asSource()
        )
    }

    @Test
    fun testColdStartUser_RoutesToLastFmSource() = runBlocking {
        fakeDetector.stubbedState = RecommendationUserState(
            hasPlaybackHistory = false,
            hasOnlinePlaybackHistory = false,
            hasLocalPlaybackHistory = false
        )

        val coldSeed = RecommendationSeed.ColdStartSeed
        val expectedSongs = listOf(
            OnlineSong(videoId = "cold1", title = "Hit Song", artist = "Pop Star", thumbnailUrl = "", durationMs = 180000L)
        )
        fakeLastFmSource.stubbedSongs = expectedSongs

        val result = router.fetchRawRecommendations(coldSeed)

        assertTrue("Last.fm source must be called for cold start", fakeLastFmSource.called)
        assertFalse("YouTube source must not be called when Last.fm succeeds", fakeYouTubeSource.called)
        assertEquals(expectedSongs, result)
    }

    @Test
    fun testReturningUser_WithOnlineSeed_RoutesToYouTubeRadioSource() = runBlocking {
        fakeDetector.stubbedState = RecommendationUserState(
            hasPlaybackHistory = true,
            hasOnlinePlaybackHistory = true,
            hasLocalPlaybackHistory = false
        )

        val onlineSeed = RecommendationSeed(songId = "kJQP7kiw5Fk", artist = "Luis Fonsi", title = "Despacito", source = "youtube")
        val expectedRadioSongs = listOf(
            OnlineSong(videoId = "radio1", title = "Con Calma", artist = "Daddy Yankee", thumbnailUrl = "", durationMs = 210000L)
        )
        fakeYouTubeSource.stubbedSongs = expectedRadioSongs

        val result = router.fetchRawRecommendations(onlineSeed)

        assertTrue("YouTube radio source must be called for returning user", fakeYouTubeSource.called)
        assertFalse("Last.fm source must not be called when YouTube radio succeeds", fakeLastFmSource.called)
        assertEquals(expectedRadioSongs, result)
    }

    @Test
    fun testYouTubeRadioEmpty_FallsBackToLastFmOrTrending() = runBlocking {
        fakeDetector.stubbedState = RecommendationUserState(
            hasPlaybackHistory = true,
            hasOnlinePlaybackHistory = true,
            hasLocalPlaybackHistory = false
        )

        val onlineSeed = RecommendationSeed(songId = "unknownId", artist = "Indie", title = "Rare Song", source = "youtube")
        fakeYouTubeSource.stubbedSongs = emptyList()

        val fallbackSongs = listOf(
            OnlineSong(videoId = "fb1", title = "Fallback Hit", artist = "Chart Artist", thumbnailUrl = "", durationMs = 200000L)
        )
        fakeLastFmSource.stubbedSongs = fallbackSongs

        val result = router.fetchRawRecommendations(onlineSeed)

        assertTrue("YouTube radio source was attempted", fakeYouTubeSource.called)
        assertTrue("Last.fm fallback source was invoked when YouTube returned empty", fakeLastFmSource.called)
        assertEquals(fallbackSongs, result)
    }

    private class FakeUserStateDetector {
        var stubbedState = RecommendationUserState(
            hasPlaybackHistory = false,
            hasOnlinePlaybackHistory = false,
            hasLocalPlaybackHistory = false
        )

        fun asDetector(): RecommendationUserStateDetector {
            return object : RecommendationUserStateDetector(
                recentHistoryRepository = mockRepository(),
                recommendationHistory = RecommendationHistory(),
                playbackStateManager = PlaybackStateManager()
            ) {
                override suspend fun detectUserState(): RecommendationUserState = stubbedState
            }
        }

        private fun mockRepository(): RecentHistoryRepository {
            val fakeDao = object : com.example.myplayer.data.local.dao.RecentHistoryDao {
                override fun getRecentHistory() = kotlinx.coroutines.flow.emptyFlow<List<com.example.myplayer.data.local.entity.SongEntity>>()
                override fun addRecent(history: com.example.myplayer.data.local.entity.RecentHistoryEntity) {}
                override fun getAllRecentHistorySync() = emptyList<com.example.myplayer.data.local.entity.RecentHistoryEntity>()
                override suspend fun getHistoryCount() = 0
                override suspend fun getRecentHistoryEntries(limit: Int) = emptyList<com.example.myplayer.data.local.entity.RecentHistoryEntity>()
                override fun insertRecentHistory(recent: List<com.example.myplayer.data.local.entity.RecentHistoryEntity>) {}
            }
            return RecentHistoryRepository(fakeDao)
        }
    }

    private class FakeLastFmSource {
        var called = false
        var stubbedSongs = emptyList<OnlineSong>()

        fun asSource(): LastFmRecommendationSource {
            val okHttp = okhttp3.OkHttpClient()
            val enc = com.example.myplayer.security.StringEncryptionManager()
            val cfg = com.example.myplayer.data.recommendation.lastfm.LastFmConfig(enc)
            val cache = com.example.myplayer.data.recommendation.lastfm.LastFmCache()
            val chartSource = com.example.myplayer.data.recommendation.lastfm.LastFmChartSource(okHttp, cfg, cache)
            val matcher = com.example.myplayer.data.recommendation.lastfm.LastFmTrackMatcher(com.example.myplayer.data.online.InnertubeApi(okHttp, enc))

            return object : LastFmRecommendationSource(chartSource, matcher, com.example.myplayer.data.online.InnertubeApi(okHttp, enc)) {
                override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
                    called = true
                    return stubbedSongs
                }
            }
        }
    }

    private class FakeYouTubeSource {
        var called = false
        var stubbedSongs = emptyList<OnlineSong>()

        fun asSource(): YouTubeRecommendationSource {
            val okHttp = okhttp3.OkHttpClient()
            val enc = com.example.myplayer.security.StringEncryptionManager()
            val api = com.example.myplayer.data.online.InnertubeApi(okHttp, enc)

            return object : YouTubeRecommendationSource(api) {
                override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
                    called = true
                    return stubbedSongs
                }
            }
        }
    }
}
