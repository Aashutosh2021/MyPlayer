package com.example.myplayer.aria.providers

import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.lyrics.LyricsRepository
import com.example.myplayer.playback.MusicController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsProvider @Inject constructor(
    private val musicController: MusicController,
    private val lyricsRepository: LyricsRepository,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun getLyrics(commandOrdinal: Int, transactionId: String?): LyricsResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val currentLocal = musicController.currentSong.value
        val currentOnline = musicController.currentOnlineSong.value

        if (currentLocal == null && currentOnline == null) {
            return@withContext LyricsResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId, "", null)
        }

        val songId: String
        val title: String
        val artist: String
        val durationSec: Int

        if (currentLocal != null) {
            songId = currentLocal.id
            title = currentLocal.title
            artist = currentLocal.artist
            durationSec = (currentLocal.duration / 1000).toInt()
        } else {
            songId = currentOnline!!.videoId
            title = currentOnline.title
            artist = currentOnline.artist
            durationSec = (currentOnline.durationMs / 1000).toInt()
        }

        try {
            // fetchLyrics handles local room check and then fallback to API
            val result = lyricsRepository.fetchLyrics(
                songId = songId,
                trackName = title,
                artistName = artist,
                durationSeconds = durationSec
            )

            performanceMonitor.recordLatency("lyrics_fetch", System.currentTimeMillis() - startTime)

            if (result != null) {
                val lyricsText = result.syncedLyrics ?: result.plainLyrics
                if (!lyricsText.isNullOrBlank()) {
                    return@withContext LyricsResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, songId, lyricsText)
                }
            }

            return@withContext LyricsResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId, songId, null)
        } catch (e: Exception) {
            return@withContext LyricsResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, songId, null)
        }
    }
}
