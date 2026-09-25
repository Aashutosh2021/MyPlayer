package com.example.myplayer.sync.track

import android.util.Log
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.sync.model.SyncTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Intelligent cross-device track identifier and matcher.
 * Matches Master's SyncTrack against the local device library and downloads
 * without relying on device-specific MediaStore IDs or local file paths.
 */
@Singleton
class SyncTrackMatcher @Inject constructor(
    private val songDao: SongDao,
    private val downloadedSongDao: DownloadedSongDao
) {
    companion object {
        private const val TAG = "SyncTrackMatcher"
        private const val DURATION_TOLERANCE_EXACT_MS = 4000L
        private const val DURATION_TOLERANCE_STRICT_MS = 2500L
    }

    suspend fun findMatchingSong(track: SyncTrack): PlayableSong? = withContext(Dispatchers.IO) {
        // Priority 1: Check stable videoId if present (e.g. YouTube / Downloaded ID)
        if (!track.videoId.isNullOrBlank()) {
            val localByVideo = songDao.getSongByVideoId(track.videoId)
            if (localByVideo != null) {
                Log.d(TAG, "Matched via videoId in local songs: ${localByVideo.title}")
                return@withContext PlayableSong.Local(localByVideo)
            }

            val downloaded = downloadedSongDao.getById(track.videoId)
            if (downloaded != null) {
                Log.d(TAG, "Matched via videoId in downloads: ${downloaded.title}")
                return@withContext PlayableSong.Downloaded(downloaded)
            }
        }

        // Priority 2: Exact normalized Title + Artist match within duration tolerance
        val normTargetTitle = normalize(track.title)
        val normTargetArtist = normalize(track.artist)
        if (normTargetTitle.isBlank()) return@withContext null

        val localSongs = songDao.getAllSongsSync()
        val exactMatchLocal = localSongs.firstOrNull { song ->
            val normTitle = normalize(song.title)
            val normArtist = normalize(song.artist)
            val durationDiff = abs(song.duration - track.durationMs)
            normTitle == normTargetTitle &&
                (normArtist == normTargetArtist || normTargetArtist.isBlank() || normArtist.isBlank()) &&
                (track.durationMs <= 0 || durationDiff <= DURATION_TOLERANCE_EXACT_MS)
        }
        if (exactMatchLocal != null) {
            Log.d(TAG, "Matched exact title+artist in local songs: ${exactMatchLocal.title}")
            return@withContext PlayableSong.Local(exactMatchLocal)
        }

        val downloadedSongs = downloadedSongDao.getAllDownloadsSync()
        val exactMatchDownload = downloadedSongs.firstOrNull { song ->
            val normTitle = normalize(song.title)
            val normArtist = normalize(song.artist)
            val durationDiff = abs(song.durationMs - track.durationMs)
            normTitle == normTargetTitle &&
                (normArtist == normTargetArtist || normTargetArtist.isBlank() || normArtist.isBlank()) &&
                (track.durationMs <= 0 || durationDiff <= DURATION_TOLERANCE_EXACT_MS)
        }
        if (exactMatchDownload != null) {
            Log.d(TAG, "Matched exact title+artist in downloads: ${exactMatchDownload.title}")
            return@withContext PlayableSong.Downloaded(exactMatchDownload)
        }

        // Priority 3: Title match with strict duration check (handles differing artist tags/remixes)
        if (track.durationMs > 0) {
            val strictDurationLocal = localSongs.firstOrNull { song ->
                val normTitle = normalize(song.title)
                val durationDiff = abs(song.duration - track.durationMs)
                normTitle == normTargetTitle && durationDiff <= DURATION_TOLERANCE_STRICT_MS
            }
            if (strictDurationLocal != null) {
                Log.d(TAG, "Matched title + strict duration in local songs: ${strictDurationLocal.title}")
                return@withContext PlayableSong.Local(strictDurationLocal)
            }

            val strictDurationDownload = downloadedSongs.firstOrNull { song ->
                val normTitle = normalize(song.title)
                val durationDiff = abs(song.durationMs - track.durationMs)
                normTitle == normTargetTitle && durationDiff <= DURATION_TOLERANCE_STRICT_MS
            }
            if (strictDurationDownload != null) {
                Log.d(TAG, "Matched title + strict duration in downloads: ${strictDurationDownload.title}")
                return@withContext PlayableSong.Downloaded(strictDurationDownload)
            }
        }

        Log.w(TAG, "No match found locally for track: '${track.title}' by '${track.artist}' (${track.durationMs}ms)")
        null
    }

    private fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("""\([^)]*\)"""), "") // Remove (feat. ...), (Official Video), etc.
            .replace(Regex("""\[[^\]]*]"""), "") // Remove [Remix], [Audio], etc.
            .replace(Regex("""[^a-z0-9\s]"""), "") // Strip punctuation
            .trim()
            .replace(Regex("""\s+"""), " ")
    }
}
