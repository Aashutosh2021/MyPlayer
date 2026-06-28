package com.example.myplayer.data.recommendation.queue

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentPlaybackWindow @Inject constructor() {
    private val MAX_SIZE = 20
    private val recentVideoIds = mutableListOf<String>()

    @Synchronized
    fun add(videoId: String) {
        if (recentVideoIds.contains(videoId)) {
            recentVideoIds.remove(videoId)
        }
        recentVideoIds.add(videoId)
        if (recentVideoIds.size > MAX_SIZE) {
            recentVideoIds.removeAt(0)
        }
    }

    @Synchronized
    fun contains(videoId: String): Boolean {
        return recentVideoIds.contains(videoId)
    }

    @Synchronized
    fun clear() {
        recentVideoIds.clear()
    }
}
