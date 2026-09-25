package com.example.myplayer.data.recommendation.queue

import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationFilter @Inject constructor(
    private val history: RecommendationHistory,
    private val recentPlaybackWindow: RecentPlaybackWindow,
    private val metrics: QueueMetrics
) {
    fun filter(
        candidates: List<RecommendationSong>,
        seed: RecommendationSeed?,
        currentlyQueuedIds: Set<String>
    ): List<RecommendationSong> {
        val filtered = mutableListOf<RecommendationSong>()
        val seenIds = mutableSetOf<String>()

        for (song in candidates) {
            // Duplicate within candidates
            if (!seenIds.add(song.videoId)) {
                metrics.recordDuplicateFiltered()
                continue
            }

            // Already queued
            if (currentlyQueuedIds.contains(song.videoId)) {
                metrics.recordDuplicateFiltered()
                continue
            }

            // Current seed song
            if (!seed?.songId.isNullOrBlank() && seed?.songId == song.videoId) {
                metrics.recordRejected()
                continue
            }

            // Recently played in this continuous session
            if (recentPlaybackWindow.contains(song.videoId)) {
                metrics.recordRejected()
                continue
            }

            // Previously served
            if (history.hasBeenServed(song.videoId)) {
                metrics.recordRejected()
                continue
            }

            // Previously rejected by user
            if (history.hasBeenRejected(song.videoId)) {
                metrics.recordRejected()
                continue
            }

            filtered.add(song)
        }

        return filtered
    }
}
