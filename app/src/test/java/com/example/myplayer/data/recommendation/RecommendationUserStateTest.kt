package com.example.myplayer.data.recommendation

import com.example.myplayer.data.local.dao.RecentHistoryDao
import com.example.myplayer.data.local.entity.RecentHistoryEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.recommendation.queue.RecommendationHistory
import com.example.myplayer.data.recommendation.state.RecommendationUserState
import com.example.myplayer.data.recommendation.state.RecommendationUserStateDetector
import com.example.myplayer.data.repository.RecentHistoryRepository
import com.example.myplayer.playback.PlaybackStateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationUserStateTest {

    private lateinit var recentHistoryDao: FakeRecentHistoryDao
    private lateinit var recentHistoryRepository: RecentHistoryRepository
    private lateinit var recommendationHistory: RecommendationHistory
    private lateinit var playbackStateManager: PlaybackStateManager
    private lateinit var detector: RecommendationUserStateDetector

    @Before
    fun setup() {
        recentHistoryDao = FakeRecentHistoryDao()
        recentHistoryRepository = RecentHistoryRepository(recentHistoryDao)
        recommendationHistory = RecommendationHistory()
        playbackStateManager = PlaybackStateManager()
        detector = RecommendationUserStateDetector(
            recentHistoryRepository,
            recommendationHistory,
            playbackStateManager
        )
    }

    @Test
    fun testFreshInstall_HasNoPlaybackHistory_IsColdStart() = runBlocking {
        val state = detector.detectUserState()
        assertTrue("Fresh install should be cold start", state.isColdStart)
        assertFalse("Should have no playback history", state.hasPlaybackHistory)
        assertFalse("Should have no online playback history", state.hasOnlinePlaybackHistory)
        assertFalse("Should have no local playback history", state.hasLocalPlaybackHistory)
    }

    @Test
    fun testUserWithNoLocalSongs_ButHasOnlinePlaybackHistory_IsNotColdStart() = runBlocking {
        // User searched and played a YouTube song (11-char videoId)
        recentHistoryDao.addRecent(RecentHistoryEntity(songId = "kJQP7kiw5Fk"))

        val state = detector.detectUserState()
        assertFalse("User with online playback history must NOT be treated as cold start", state.isColdStart)
        assertTrue("Should have playback history", state.hasPlaybackHistory)
        assertTrue("Should have online playback history", state.hasOnlinePlaybackHistory)
        assertFalse("Should have no local playback history", state.hasLocalPlaybackHistory)
    }

    @Test
    fun testActiveOnlineSong_IsNotColdStart() = runBlocking {
        // No persistent history yet, but online song is actively playing in session
        playbackStateManager.updateCurrentOnlineSong(
            com.example.myplayer.data.online.model.OnlineSong(
                videoId = "kJQP7kiw5Fk",
                title = "Despacito",
                artist = "Luis Fonsi",
                thumbnailUrl = "",
                durationMs = 200000L
            )
        )

        val state = detector.detectUserState()
        assertFalse("Actively playing online song must NOT be treated as cold start", state.isColdStart)
        assertTrue("Should have online playback history", state.hasOnlinePlaybackHistory)
    }

    @Test
    fun testUserWithLocalPlaybackHistory_IsNotColdStart() = runBlocking {
        recentHistoryDao.addRecent(RecentHistoryEntity(songId = "local_track_12345"))

        val state = detector.detectUserState()
        assertFalse("User with local history must NOT be treated as cold start", state.isColdStart)
        assertTrue("Should have local playback history", state.hasLocalPlaybackHistory)
        assertFalse("Should have no online playback history", state.hasOnlinePlaybackHistory)
    }

    private class FakeRecentHistoryDao : RecentHistoryDao {
        private val list = mutableListOf<RecentHistoryEntity>()

        override fun getRecentHistory(): Flow<List<SongEntity>> = emptyFlow()

        override fun addRecent(history: RecentHistoryEntity) {
            list.removeAll { it.songId == history.songId }
            list.add(0, history)
        }

        override fun getAllRecentHistorySync(): List<RecentHistoryEntity> = list.toList()

        override suspend fun getHistoryCount(): Int = list.size

        override suspend fun getRecentHistoryEntries(limit: Int): List<RecentHistoryEntity> = list.take(limit)

        override fun insertRecentHistory(recent: List<RecentHistoryEntity>) {
            list.addAll(recent)
        }
    }
}
