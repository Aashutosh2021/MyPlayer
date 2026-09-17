package com.example.myplayer.aria.providers

import com.example.myplayer.aria.cache.AriaMemoryCache
import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueProvider @Inject constructor(
    private val musicController: MusicController,
    private val memoryCache: AriaMemoryCache,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun queueSong(songId: String, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        if (songId.isBlank()) {
            return@withContext AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId)
        }

        // 1. Check online prefix
        if (songId.startsWith("online://")) {
            val videoId = songId.removePrefix("online://")
            val onlineSongEntity = SongEntity(
                id = songId,
                title = "Online Song",
                artist = "YouTube Music",
                album = "YouTube Music",
                duration = 0L,
                path = songId,
                albumArt = null,
                dateAdded = System.currentTimeMillis(),
                videoId = videoId
            )
            musicController.addSongToQueue(onlineSongEntity)
            performanceMonitor.recordLatency("queue_ops", System.currentTimeMillis() - startTime)
            return@withContext AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        }

        // 2. Query instant Memory Cache (Task 3: No DB hits for IPC validation)
        val cacheStart = System.currentTimeMillis()
        val downloadedSong = memoryCache.getDownloadedSong(songId)
        val localSong = memoryCache.getLocalSong(songId)
        performanceMonitor.recordLatency("cache_read", System.currentTimeMillis() - cacheStart)

        if (downloadedSong != null) {
            val songEntity = SongEntity(
                id = downloadedSong.id,
                title = downloadedSong.title,
                artist = downloadedSong.artist,
                album = downloadedSong.album.ifBlank { "Downloads" },
                duration = downloadedSong.durationMs,
                path = downloadedSong.localPath,
                albumArt = downloadedSong.thumbnailUrl,
                dateAdded = downloadedSong.downloadedAt,
                videoId = downloadedSong.id
            )
            musicController.addSongToQueue(songEntity)
            performanceMonitor.recordLatency("queue_ops", System.currentTimeMillis() - startTime)
            return@withContext AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        }

        if (localSong != null) {
            musicController.addSongToQueue(localSong)
            performanceMonitor.recordLatency("queue_ops", System.currentTimeMillis() - startTime)
            return@withContext AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        }

        return@withContext AriaGeneralResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId)
    }

    fun getQueue(commandOrdinal: Int, transactionId: String?): QueueResponse {
        val startTime = System.currentTimeMillis()
        val controller = musicController.getMediaController() ?: return QueueResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId, null)
        
        val currentQueue = musicController.getSongQueue()
        val currentIndex = controller.currentMediaItemIndex

        val songInfoList = currentQueue.map { song ->
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

        val queueInfo = QueueInfo(
            songs = songInfoList,
            currentIndex = currentIndex.coerceAtLeast(0)
        )

        performanceMonitor.recordLatency("queue_ops", System.currentTimeMillis() - startTime)

        return QueueResponse(
            status = AriaStatus.SUCCESS,
            commandOrdinal = commandOrdinal,
            transactionId = transactionId,
            queue = queueInfo
        )
    }
}
