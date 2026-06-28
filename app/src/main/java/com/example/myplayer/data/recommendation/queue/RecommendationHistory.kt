package com.example.myplayer.data.recommendation.queue

import javax.inject.Inject
import javax.inject.Singleton

enum class RecommendationHistoryState {
    RECOMMENDED,
    PLAYED,
    ACCEPTED,
    SKIPPED,
    REJECTED,
    TIMED_OUT
}

@Singleton
class RecommendationHistory @Inject constructor() {
    private val historyMap = mutableMapOf<String, RecommendationHistoryState>()

    @Synchronized
    fun recordState(videoId: String, state: RecommendationHistoryState) {
        historyMap[videoId] = state
    }

    @Synchronized
    fun getState(videoId: String): RecommendationHistoryState? {
        return historyMap[videoId]
    }

    @Synchronized
    fun hasBeenServed(videoId: String): Boolean {
        val state = historyMap[videoId]
        return state == RecommendationHistoryState.RECOMMENDED || 
               state == RecommendationHistoryState.PLAYED ||
               state == RecommendationHistoryState.ACCEPTED
    }

    @Synchronized
    fun hasBeenRejected(videoId: String): Boolean {
        val state = historyMap[videoId]
        return state == RecommendationHistoryState.REJECTED || 
               state == RecommendationHistoryState.SKIPPED
    }

    @Synchronized
    fun clear() {
        historyMap.clear()
    }
}
