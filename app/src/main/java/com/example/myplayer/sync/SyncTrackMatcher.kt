package com.example.myplayer.sync

import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Matches an incoming TRACK_PREPARE against this device's local library.
 * Priority order:
 * 1. videoId (stable shared identifier across devices)
 * 2. metadata match: title + artist (case-insensitive) and duration within tolerance
 */
@Singleton
class SyncTrackMatcher @Inject constructor(
    private val songDao: SongDao
) {
    companion object {
        private const val DURATION_TOLERANCE_MS = 2000L
    }

    suspend fun findLocalMatch(ref: SyncTrackRef): SongEntity? {
        val allSongs = songDao.getAllSongs().first()

        if (!ref.videoId.isNullOrBlank()) {
            allSongs.firstOrNull { it.videoId == ref.videoId }?.let { return it }
        }

        val localMatch = allSongs.firstOrNull { song ->
            song.title.equals(ref.title, ignoreCase = true) &&
                song.artist.equals(ref.artist, ignoreCase = true) &&
                abs(song.duration - ref.durationMs) <= DURATION_TOLERANCE_MS
        }
        if (localMatch != null) return localMatch

        // If the track is an online track with a videoId, synthesize a streamable SongEntity
        if (!ref.videoId.isNullOrBlank()) {
            return SongEntity(
                id = ref.videoId,
                title = ref.title,
                artist = ref.artist,
                album = "Sync Play Online",
                duration = ref.durationMs,
                path = "online://${ref.videoId}",
                albumArt = null,
                dateAdded = System.currentTimeMillis(),
                videoId = ref.videoId
            )
        }

        return null
    }
}
