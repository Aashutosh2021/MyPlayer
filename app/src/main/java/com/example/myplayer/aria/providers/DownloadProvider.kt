package com.example.myplayer.aria.providers

import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProvider @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val innertubeApi: InnertubeApi,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun downloadSong(songId: String, title: String, artist: String, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (songId.isBlank() || title.isBlank() || artist.isBlank()) {
            return@withContext AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId)
        }

        val videoId = songId.removePrefix("online://")

        // Task 6: Request Validation (Download already running)
        val activeProgress = downloadRepository.downloadProgress.value
        if (activeProgress.containsKey(videoId)) {
            return@withContext AriaGeneralResponse(
                status = AriaStatus.BUSY,
                commandOrdinal = commandOrdinal,
                transactionId = transactionId,
                message = "Download for song $videoId is already in progress (${activeProgress[videoId]}%)."
            )
        }

        // Task 6: Request Validation (Song already downloaded)
        if (downloadRepository.isDownloaded(videoId)) {
            return@withContext AriaGeneralResponse(
                status = AriaStatus.ALREADY_EXISTS,
                commandOrdinal = commandOrdinal,
                transactionId = transactionId,
                message = "Song $videoId is already downloaded."
            )
        }

        try {
            // Resolve stream URL using NewPipe
            val streamUrl = innertubeApi.getStreamUrl(videoId)
            if (streamUrl.isNullOrBlank()) {
                return@withContext AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, "Could not resolve stream URL for $videoId.")
            }

            val onlineSong = OnlineSong(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = "",
                durationMs = 0L,
                streamUrl = streamUrl
            )

            val scheduled = downloadRepository.startDownload(onlineSong)
            performanceMonitor.recordLatency("download_ops", System.currentTimeMillis() - startTime)
            
            return@withContext if (scheduled) {
                AriaGeneralResponse(AriaStatus.QUEUED, commandOrdinal, transactionId)
            } else {
                AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, "Failed to schedule download worker.")
            }
        } catch (e: Exception) {
            return@withContext AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, "Error enqueuing download: ${e.message}")
        }
    }

    suspend fun deleteDownload(songId: String, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (songId.isBlank()) return@withContext AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId)

        val videoId = songId.removePrefix("online://")

        // Validate download exists
        if (!downloadRepository.isDownloaded(videoId)) {
            return@withContext AriaGeneralResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId, "Download record not found for $videoId.")
        }

        try {
            downloadRepository.deleteDownloadById(videoId)
            performanceMonitor.recordLatency("download_ops", System.currentTimeMillis() - startTime)
            return@withContext AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        } catch (e: Exception) {
            return@withContext AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, "Error deleting downloaded file: ${e.message}")
        }
    }
}
