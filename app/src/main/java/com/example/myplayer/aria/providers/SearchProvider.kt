package com.example.myplayer.aria.providers

import android.util.Log
import com.example.myplayer.aria.cache.AriaMemoryCache
import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.online.InnertubeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchProvider @Inject constructor(
    private val memoryCache: AriaMemoryCache,
    private val innertubeApi: InnertubeApi,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun search(query: String, source: String?, commandOrdinal: Int, transactionId: String?): HistoryResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (query.isBlank()) {
            return@withContext HistoryResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, emptyList())
        }

        val results = mutableListOf<SongInfo>()
        val src = source?.lowercase() ?: "all"

        try {
            // 1. Search local memory cache instantly (Task 3: Avoid SQLite database lookup)
            if (src == "local" || src == "all") {
                val cacheStart = System.currentTimeMillis()
                val queryLower = query.lowercase().trim()
                val matched = memoryCache.getAllLocalSongs().filter {
                    it.title.lowercase().contains(queryLower) ||
                    it.artist.lowercase().contains(queryLower) ||
                    it.album.lowercase().contains(queryLower)
                }
                
                matched.forEach { song ->
                    results.add(
                        SongInfo(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            durationMs = song.duration,
                            path = song.path,
                            albumArtUrl = song.albumArt,
                            isOnline = false,
                            playCount = song.playCount
                        )
                    )
                }
                performanceMonitor.recordLatency("local_search", System.currentTimeMillis() - cacheStart)
            }

            // 2. Search online Remote API
            if (src == "online" || src == "all") {
                val apiStart = System.currentTimeMillis()
                val onlinePage = innertubeApi.search(query)
                onlinePage.songs.forEach { song ->
                    results.add(
                        SongInfo(
                            id = "online://${song.videoId}",
                            title = song.title,
                            artist = song.artist,
                            album = "YouTube Music",
                            durationMs = song.durationMs,
                            path = "online://${song.videoId}",
                            albumArtUrl = song.thumbnailUrl,
                            isOnline = true,
                            playCount = 0
                        )
                    )
                }
                performanceMonitor.recordLatency("online_search", System.currentTimeMillis() - apiStart)
            }

            performanceMonitor.recordLatency("search_query", System.currentTimeMillis() - startTime)
            return@withContext HistoryResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, results)

        } catch (e: Exception) {
            Log.e("SearchProvider", "Search failed", e)
            return@withContext HistoryResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, emptyList())
        }
    }
}
