package com.example.myplayer.aria.providers

import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationProvider @Inject constructor(
    private val musicRepository: MusicRepository,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun getRecommendations(commandOrdinal: Int, transactionId: String?): RecommendationResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val mostPlayed = musicRepository.getMostPlayedSongs().first()
            val trending = musicRepository.getTrendingSongs().first()
            val combined = (mostPlayed.take(10) + trending.take(10)).distinctBy { it.id }

            val results = combined.map { song ->
                val isOnline = song.path.startsWith("http") || song.path.startsWith("online://")
                SongInfo(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.duration,
                    path = song.path,
                    albumArtUrl = song.albumArt,
                    isOnline = isOnline,
                    playCount = song.playCount
                )
            }

            performanceMonitor.recordLatency("database_read", System.currentTimeMillis() - startTime)
            return@withContext RecommendationResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, results)
        } catch (e: Exception) {
            return@withContext RecommendationResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, emptyList())
        }
    }
}
