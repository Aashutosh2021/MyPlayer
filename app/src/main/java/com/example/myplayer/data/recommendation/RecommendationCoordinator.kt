package com.example.myplayer.data.recommendation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.queue.RecommendationQueueManager
import com.example.myplayer.data.recommendation.playback.RecommendationPlaybackRepository
import com.example.myplayer.playback.PlaybackEvent
import com.example.myplayer.playback.PlaybackEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackEventBus: PlaybackEventBus,
    private val settingsDataStore: SettingsDataStore,
    private val recommendationManager: RecommendationManager,
    private val queueManager: RecommendationQueueManager,
    private val playbackRepository: RecommendationPlaybackRepository,
    private val logger: RecommendationLogger
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Auto-recommendation background engine disabled to keep app lightweight, fast, and smooth
    }

    private suspend fun handlePlaybackEvent(event: PlaybackEvent) {
        // Auto-recommendation seeding and autoplay disabled
    }

    private fun startRecommendationSession(seed: RecommendationSeed) {
        scope.launch {
            try {
                logger.logEvent("PlaybackStarted", mapOf(
                    "songId" to seed.songId,
                    "artist" to seed.artist,
                    "source" to seed.source
                ))

                recommendationManager.preloadRecommendations(seed)
                queueManager.updateSession(seed)

            } catch (e: Exception) {
                logger.error("RecommendationCoordinator failed to handle playback started", e)
                logger.logEvent("PlaybackFailed", mapOf("reason" to (e.message ?: "Unknown")))
            }
        }
    }

    private suspend fun triggerAutoplay() {
        // Disabled: No autoplay recommendations
    }

    val queueState = queueManager.queueState

    fun endSession() {
        logger.logEvent("SessionEnded", emptyMap())
        queueManager.clearQueue()
        recommendationManager.reset()
    }

    suspend fun playRecommendation(song: com.example.myplayer.data.recommendation.model.RecommendationSong): OnlineSong? {
        queueManager.acceptSong(song.videoId)
        return OnlineSong(
            videoId = song.videoId,
            title = song.title,
            artist = song.artist,
            thumbnailUrl = song.thumbnailUrl,
            durationMs = song.durationMs,
            durationText = song.durationMs.toString(),
            streamUrl = "online://${song.videoId}"
        )
    }

    fun rejectRecommendation(videoId: String) {
        queueManager.rejectSong(videoId)
    }

    fun refreshQueue() {
        queueManager.refreshQueue()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
