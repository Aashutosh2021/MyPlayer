package com.example.myplayer.data.recommendation.state

import com.example.myplayer.data.recommendation.queue.RecommendationHistory
import com.example.myplayer.data.repository.RecentHistoryRepository
import com.example.myplayer.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accurately determines if the user is in Cold Start vs Returning User.
 * CRITICAL RULE: A user is NOT defined as a new user based on local library emptiness.
 * Online playback history counts equally towards establishing playback history.
 */
@Singleton
open class RecommendationUserStateDetector @Inject constructor(
    private val recentHistoryRepository: RecentHistoryRepository,
    private val recommendationHistory: RecommendationHistory,
    private val playbackStateManager: PlaybackStateManager
) {
    open suspend fun detectUserState(): RecommendationUserState = withContext(Dispatchers.IO) {
        val count = recentHistoryRepository.getHistoryCount()
        val entries = recentHistoryRepository.getRecentHistoryEntries(50)

        val currentSong = playbackStateManager.currentSong.value
        val currentOnlineSong = playbackStateManager.currentOnlineSong.value
        val hasActiveSong = currentSong != null || currentOnlineSong != null

        val isYouTubeId = { id: String ->
            id.startsWith("online://") || id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))
        }

        val hasOnline = currentOnlineSong != null || entries.any { isYouTubeId(it.songId) }
        val hasLocal = currentSong != null || entries.any { !isYouTubeId(it.songId) }
        val hasPlaybackHistory = count > 0 || entries.isNotEmpty() || hasActiveSong
        val hasRecoHistory = recommendationHistory.hasAnyHistory()

        RecommendationUserState(
            hasPlaybackHistory = hasPlaybackHistory,
            hasOnlinePlaybackHistory = hasOnline,
            hasLocalPlaybackHistory = hasLocal,
            hasRecommendationHistory = hasRecoHistory,
            totalPlaysCount = count
        )
    }
}
