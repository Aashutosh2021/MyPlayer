package com.example.myplayer.aria.providers

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.media3.common.Player
import com.example.myplayer.aria.cache.AriaMemoryCache
import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicController: MusicController,
    private val memoryCache: AriaMemoryCache,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    companion object {
        private const val TAG = "PlaybackController"
    }

    suspend fun play(songId: String, source: String?, commandOrdinal: Int, transactionId: String?): TrackResponse = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        if (songId.isBlank()) {
            return@withContext TrackResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, null)
        }

        // Check online prefix
        if (songId.startsWith("online://") || source == "online") {
            val videoId = songId.removePrefix("online://")
            val onlineSong = com.example.myplayer.data.online.model.OnlineSong(
                videoId = videoId,
                title = "Online Song",
                artist = "YouTube Music",
                thumbnailUrl = "",
                durationMs = 0L
            )
            musicController.playOnlineSong(onlineSong)
            performanceMonitor.recordLatency("playback", System.currentTimeMillis() - startTime)
            val info = SongInfo(songId, "Online Song", "YouTube Music", "YouTube Music", 0L, songId, null, true, 0)
            return@withContext TrackResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, info)
        }

        // Serve read instantly from Memory Cache (Task 3: Memory Cache validation, 0ms latency)
        val cacheStart = System.currentTimeMillis()
        val downloadedSong = memoryCache.getDownloadedSong(songId)
        val localSong = memoryCache.getLocalSong(songId)
        performanceMonitor.recordLatency("cache_read", System.currentTimeMillis() - cacheStart)

        if (downloadedSong != null && source != "local") {
            musicController.playDownloadedSong(downloadedSong)
            performanceMonitor.recordLatency("playback", System.currentTimeMillis() - startTime)
            val info = SongInfo(
                id = downloadedSong.id,
                title = downloadedSong.title,
                artist = downloadedSong.artist,
                album = downloadedSong.album.ifBlank { "Downloads" },
                durationMs = downloadedSong.durationMs,
                path = downloadedSong.localPath,
                albumArtUrl = downloadedSong.thumbnailUrl,
                isOnline = false,
                playCount = 0
            )
            return@withContext TrackResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, info)
        }

        if (localSong != null) {
            musicController.playSongs(listOf(localSong), 0)
            performanceMonitor.recordLatency("playback", System.currentTimeMillis() - startTime)
            val info = SongInfo(
                id = localSong.id,
                title = localSong.title,
                artist = localSong.artist,
                album = localSong.album,
                durationMs = localSong.duration,
                path = localSong.path,
                albumArtUrl = localSong.albumArt,
                isOnline = false,
                playCount = localSong.playCount
            )
            return@withContext TrackResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId, info)
        }

        return@withContext TrackResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId, null)
    }

    fun pause(commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        
        if (controller.isPlaying) {
            controller.pause()
            performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
            return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        }
        return AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, "Playback is already paused.")
    }

    fun resume(commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        
        if (!controller.isPlaying) {
            controller.play()
            performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
            return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        }
        return AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId, "Playback is already playing.")
    }

    fun stop(commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        musicController.stopPlayback()
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun next(commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        
        musicController.skipToNext()
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun previous(commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        
        musicController.skipToPrevious()
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun seek(positionMs: Long, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        if (positionMs < 0) {
            return AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, "Position cannot be negative.")
        }
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        
        // Task 6: Request Validation
        val duration = controller.duration
        if (duration > 0 && positionMs > duration) {
            return AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, "Seek position $positionMs exceeds track duration $duration.")
        }

        musicController.seekTo(positionMs)
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun setShuffle(enabled: Boolean, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        musicController.setShuffleEnabled(enabled)
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun setRepeatMode(mode: Int, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        val media3Mode = when (mode) {
            0 -> Player.REPEAT_MODE_OFF
            1 -> Player.REPEAT_MODE_ONE
            2 -> Player.REPEAT_MODE_ALL
            else -> return AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, "Invalid repeat mode: $mode.")
        }
        musicController.setRepeatMode(media3Mode)
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun setSpeed(speed: Float, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        if (speed < 0.25f || speed > 2.0f) {
            return AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, "Playback speed must be between 0.25 and 2.0.")
        }
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        musicController.setPlaybackSpeed(speed)
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }

    fun setVolume(volume: Float, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        val startTime = System.currentTimeMillis()
        if (volume < 0.0f || volume > 1.0f) {
            return AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId, "Volume level must be between 0.0 and 1.0.")
        }
        val controller = musicController.getMediaController() ?: return AriaGeneralResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId)
        musicController.setPlayerVolume(volume)
        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)
        return AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
    }
}
