package com.example.myplayer.aria.providers

import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.playback.MusicController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataProvider @Inject constructor(
    private val musicController: MusicController,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    fun getCurrentSong(commandOrdinal: Int, transactionId: String?): TrackResponse {
        val startTime = System.currentTimeMillis()
        val currentLocal = musicController.currentSong.value
        val currentOnline = musicController.currentOnlineSong.value
        val controller = musicController.getMediaController()

        if (currentLocal == null && currentOnline == null) {
            return TrackResponse(AriaStatus.NOTHING_PLAYING, commandOrdinal, transactionId, null)
        }

        val position = controller?.currentPosition ?: 0L
        val duration = controller?.duration ?: 0L
        val isPlaying = controller?.isPlaying ?: false

        val songInfo = if (currentLocal != null) {
            val isOnline = currentLocal.path.startsWith("http") || currentLocal.path.startsWith("online://")
            SongInfo(
                id = currentLocal.id,
                title = currentLocal.title,
                artist = currentLocal.artist,
                album = currentLocal.album,
                durationMs = currentLocal.duration,
                path = currentLocal.path,
                albumArtUrl = currentLocal.albumArt,
                isOnline = isOnline,
                playCount = currentLocal.playCount
            )
        } else {
            SongInfo(
                id = "online://${currentOnline!!.videoId}",
                title = currentOnline.title,
                artist = currentOnline.artist,
                album = "YouTube Music",
                durationMs = currentOnline.durationMs,
                path = "online://${currentOnline.videoId}",
                albumArtUrl = currentOnline.thumbnailUrl,
                isOnline = true,
                playCount = 0
            )
        }

        performanceMonitor.recordLatency("commands", System.currentTimeMillis() - startTime)

        return TrackResponse(
            status = AriaStatus.SUCCESS,
            commandOrdinal = commandOrdinal,
            transactionId = transactionId,
            song = songInfo,
            positionMs = position,
            durationMs = duration,
            isPlaying = isPlaying
        )
    }
}
