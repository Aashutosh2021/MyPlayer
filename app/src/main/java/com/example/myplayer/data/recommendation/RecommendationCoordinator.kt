package com.example.myplayer.data.recommendation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
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
open class RecommendationCoordinator internal constructor(
    context: Context?,
    private val playbackEventBus: PlaybackEventBus,
    private val settingsDataStore: SettingsDataStore,
    private val recommendationManager: RecommendationManager,
    private val queueManager: RecommendationQueueManager,
    private val playbackRepository: RecommendationPlaybackRepository,
    private val logger: RecommendationLogger,
    private val innertubeApi: com.example.myplayer.data.online.InnertubeApi? = null,
    @Suppress("UNUSED_PARAMETER") isTest: Boolean
) {
    private val context: Context? = context

    @Inject
    constructor(
        @ApplicationContext context: Context,
        playbackEventBus: PlaybackEventBus,
        settingsDataStore: SettingsDataStore,
        recommendationManager: RecommendationManager,
        queueManager: RecommendationQueueManager,
        playbackRepository: RecommendationPlaybackRepository,
        logger: RecommendationLogger,
        innertubeApi: com.example.myplayer.data.online.InnertubeApi
    ) : this(
        context,
        playbackEventBus,
        settingsDataStore,
        recommendationManager,
        queueManager,
        playbackRepository,
        logger,
        innertubeApi,
        false
    )

    constructor(
        playbackEventBus: PlaybackEventBus,
        settingsDataStore: SettingsDataStore,
        recommendationManager: RecommendationManager,
        queueManager: RecommendationQueueManager,
        playbackRepository: RecommendationPlaybackRepository,
        logger: RecommendationLogger
    ) : this(
        null,
        playbackEventBus,
        settingsDataStore,
        recommendationManager,
        queueManager,
        playbackRepository,
        logger,
        null,
        true
    )

    var networkAvailabilityOverride: Boolean? = null
    companion object {
        private const val TAG = "RecommendationCoord"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Collect playback events to seed recommendations and drive safe autoplay
        scope.launch {
            playbackEventBus.events.collect { event ->
                handlePlaybackEvent(event)
            }
        }
    }

    private suspend fun handlePlaybackEvent(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.SongStarted -> {
                Log.d(TAG, "PlaybackStarted event: ${event.songId} (${event.title})")
                // Mark accepted in queue history if it was a recommendation
                queueManager.acceptSong(event.songId)
                val seed = RecommendationSeed(
                    songId = event.songId,
                    title = event.title,
                    artist = event.artist,
                    source = if (event.isOnline) "youtube" else "local"
                )
                startRecommendationSession(seed)
                prefetchUpcomingRecommendationStream()
            }
            is PlaybackEvent.SongCompleted -> {
                Log.d(TAG, "SongCompleted event for: ${event.songId}. Evaluating autoplay...")
                triggerAutoplay()
            }
            is PlaybackEvent.SongSkipped -> {
                queueManager.skipSong(event.songId)
            }
            is PlaybackEvent.SongError -> {
                queueManager.rejectSong(event.songId)
            }
            is PlaybackEvent.QueueEmpty -> {
                triggerAutoplay()
            }
            is PlaybackEvent.AutoplayRequested -> {
                triggerAutoplay()
            }
            is PlaybackEvent.PlayRequestReady -> {
                // Handled by MusicController
            }
        }
    }

    fun startRecommendationSession(seed: RecommendationSeed) {
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

    fun prefetchRecommendations(seed: RecommendationSeed = RecommendationSeed.ColdStartSeed) {
        scope.launch {
            if (queueManager.isEmpty()) {
                Log.d(TAG, "Recommendations prefetch requested with seed: ${seed.songId.ifBlank { seed.title }}")
                startRecommendationSession(seed)
            }
        }
    }

    fun prefetchColdStartIfNeeded() {
        prefetchRecommendations(RecommendationSeed.ColdStartSeed)
    }

    private suspend fun triggerAutoplay() {
        val isAutoplayEnabled = settingsDataStore.isAutoplayEnabled.firstOrNull() ?: true
        if (!isAutoplayEnabled) {
            Log.d(TAG, "AUTOPLAY_SKIPPED: Autoplay is disabled by user settings")
            logger.logEvent("AUTOPLAY_SKIPPED", mapOf("reason" to "DisabledInSettings"))
            return
        }

        if (!isNetworkAvailable()) {
            Log.w(TAG, "AUTOPLAY_SKIPPED: Network unavailable for recommendation autoplay")
            logger.logEvent("AUTOPLAY_SKIPPED", mapOf("reason" to "NetworkUnavailable"))
            return
        }

        var nextSong = queueManager.dequeue()

        // If queue was empty, attempt synchronous recovery
        if (nextSong == null) {
            Log.d(TAG, "Queue empty during autoplay, attempting recovery...")
            val recovered = queueManager.recoverQueueSynchronously()
            if (recovered) {
                nextSong = queueManager.dequeue()
            }
        }

        if (nextSong != null) {
            Log.i(TAG, "AUTOPLAY_RECOMMENDATION_SELECTED: Next track -> ${nextSong.title} by ${nextSong.artist} (${nextSong.videoId})")
            logger.logEvent("AUTOPLAY_RECOMMENDATION_SELECTED", mapOf(
                "videoId" to nextSong.videoId,
                "title" to nextSong.title,
                "artist" to nextSong.artist,
                "source" to nextSong.source
            ))

            val playRequest = playbackRepository.preparePlayRequest(nextSong)
            playbackEventBus.emit(
                PlaybackEvent.PlayRequestReady(
                    requests = listOf(playRequest),
                    startIndex = 0
                )
            )
            prefetchUpcomingRecommendationStream()
        } else {
            Log.w(TAG, "AUTOPLAY_SKIPPED: No recommendation candidates available")
            logger.logEvent("AUTOPLAY_SKIPPED", mapOf("reason" to "NoCandidatesAvailable"))
        }
    }

    private var prefetchStreamJob: kotlinx.coroutines.Job? = null

    private fun prefetchUpcomingRecommendationStream() {
        val api = innertubeApi ?: return
        prefetchStreamJob?.cancel()
        prefetchStreamJob = scope.launch {
            kotlinx.coroutines.delay(2500)
            if (!isNetworkAvailable()) return@launch
            val nextSong = queueManager.peekNext() ?: return@launch
            val vId = nextSong.videoId
            if (vId.isNotBlank() && api.getCachedStreamUrl(vId) == null) {
                try {
                    Log.d(TAG, "Prefetching stream URL for next recommendation: $vId (${nextSong.title})")
                    api.getStreamUrl(vId)
                } catch (e: Exception) {
                    Log.d(TAG, "Prefetch failed for $vId (will resolve on demand): ${e.message}")
                }
            }
        }
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
        networkAvailabilityOverride?.let { return it }
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
