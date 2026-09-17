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
class HistoryProvider @Inject constructor(
    private val musicRepository: MusicRepository,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun getListeningHistory(limit: Int, commandOrdinal: Int, transactionId: String?): HistoryResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val list = musicRepository.getRecentHistory().first()
            val limitedList = if (limit > 0) list.take(limit) else list

            val results = limitedList.map { song ->
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
            return@withContext HistoryResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, results)
        } catch (e: Exception) {
            return@withContext HistoryResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, emptyList())
        }
    }
}
