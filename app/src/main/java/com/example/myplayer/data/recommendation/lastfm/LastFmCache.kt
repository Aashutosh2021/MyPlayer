package com.example.myplayer.data.recommendation.lastfm

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmCache @Inject constructor() {

    private data class CachedChart(
        val tracks: List<LastFmTrackMetadata>,
        val timestampMs: Long
    )

    private val cache = ConcurrentHashMap<String, CachedChart>()
    // 60 minutes TTL
    private val TTL_MS = 60L * 60L * 1000L

    fun get(key: String): List<LastFmTrackMetadata>? {
        val entry = cache[key] ?: return null
        val now = System.currentTimeMillis()
        if (now - entry.timestampMs <= TTL_MS) {
            return entry.tracks
        }
        return null
    }

    /**
     * Retrieves cached tracks even if expired, used for graceful fallback
     * when the network is unavailable or the API is rate-limited.
     */
    fun getStale(key: String): List<LastFmTrackMetadata>? {
        return cache[key]?.tracks
    }

    fun put(key: String, tracks: List<LastFmTrackMetadata>) {
        if (tracks.isNotEmpty()) {
            cache[key] = CachedChart(tracks, System.currentTimeMillis())
        }
    }

    fun clear() {
        cache.clear()
    }
}
