package com.example.myplayer.sync

import android.content.Context
import android.net.Uri
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File
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
    private val songDao: SongDao,
    private val downloadedSongDao: DownloadedSongDao,
    @param:ApplicationContext private val context: Context? = null
) {
    /** Secondary constructor for unit testing without Hilt */
    constructor(songDao: SongDao) : this(
        songDao = songDao,
        downloadedSongDao = object : DownloadedSongDao {
            override fun getAllDownloads(): Flow<List<DownloadedSongEntity>> = flowOf(emptyList())
            override fun getAllDownloadsSync(): List<DownloadedSongEntity> = emptyList()
            override fun insertDownloads(downloads: List<DownloadedSongEntity>) {}
            override suspend fun getById(id: String): DownloadedSongEntity? = null
            override suspend fun existsById(id: String): Boolean = false
            override suspend fun insert(song: DownloadedSongEntity) {}
            override suspend fun deleteById(id: String) {}
            override fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>> = flowOf(emptyList())
        },
        context = null
    )

    companion object {
        private const val DURATION_TOLERANCE_MS = 2000L
    }

    /** Optional validator hook for testing or custom file validation */
    var fileValidator: ((String) -> Boolean)? = null

    /**
     * Checks whether an exact match exists on local disk with non-zero file size.
     * Returns a valid SongEntity pointing to the local file, or null if missing.
     */
    suspend fun findLocalFileMatch(ref: SyncTrackRef): SongEntity? = withContext(Dispatchers.IO) {
        // 1. Check DownloadedSongDao by videoId
        if (!ref.videoId.isNullOrBlank()) {
            val downloaded = downloadedSongDao.getById(ref.videoId)
            if (downloaded != null && verifyLocalFile(downloaded.localPath)) {
                return@withContext SongEntity(
                    id = downloaded.id,
                    title = downloaded.title,
                    artist = downloaded.artist,
                    album = downloaded.album.ifBlank { "Downloads" },
                    duration = downloaded.durationMs,
                    path = downloaded.localPath,
                    albumArt = downloaded.thumbnailUrl,
                    dateAdded = downloaded.downloadedAt,
                    videoId = downloaded.id
                )
            }
        }

        // 2. Check SongDao (local device media library)
        val allSongs = songDao.getAllSongs().firstOrNull() ?: emptyList()

        val defaultArt = ref.albumArt ?: ref.videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

        if (!ref.videoId.isNullOrBlank()) {
            val byVideoId = allSongs.firstOrNull { it.videoId == ref.videoId }
            if (byVideoId != null && verifyLocalFile(byVideoId.path)) {
                return@withContext if (byVideoId.albumArt.isNullOrBlank() && !defaultArt.isNullOrBlank()) {
                    byVideoId.copy(albumArt = defaultArt)
                } else byVideoId
            }
        }

        val metadataMatch = allSongs.firstOrNull { song ->
            song.title.equals(ref.title, ignoreCase = true) &&
            song.artist.equals(ref.artist, ignoreCase = true) &&
            abs(song.duration - ref.durationMs) <= DURATION_TOLERANCE_MS
        }
        if (metadataMatch != null && verifyLocalFile(metadataMatch.path)) {
            return@withContext if (metadataMatch.albumArt.isNullOrBlank() && !defaultArt.isNullOrBlank()) {
                metadataMatch.copy(albumArt = defaultArt)
            } else metadataMatch
        }

        // 3. Check DownloadedSongDao by metadata
        val allDownloads = downloadedSongDao.getAllDownloadsSync()
        val downloadMetadataMatch = allDownloads.firstOrNull { d ->
            d.title.equals(ref.title, ignoreCase = true) &&
            d.artist.equals(ref.artist, ignoreCase = true)
        }
        if (downloadMetadataMatch != null && verifyLocalFile(downloadMetadataMatch.localPath)) {
            val art = downloadMetadataMatch.thumbnailUrl.ifBlank { defaultArt }
            return@withContext SongEntity(
                id = downloadMetadataMatch.id,
                title = downloadMetadataMatch.title,
                artist = downloadMetadataMatch.artist,
                album = downloadMetadataMatch.album.ifBlank { "Downloads" },
                duration = downloadMetadataMatch.durationMs,
                path = downloadMetadataMatch.localPath,
                albumArt = art,
                dateAdded = downloadMetadataMatch.downloadedAt,
                videoId = downloadMetadataMatch.id
            )
        }

        null
    }

    fun verifyLocalFile(path: String): Boolean {
        fileValidator?.let { return it(path) }

        return try {
            if (path.startsWith("content://") || path.startsWith("file://")) {
                val ctx = context ?: return true // In unit test without Context, assume URI is valid
                val uri = Uri.parse(path)
                ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length > 0
                } ?: false
            } else {
                val file = File(path)
                file.exists() && file.isFile && file.length() > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Legacy matcher kept for backwards compatibility with tests.
     */
    suspend fun findLocalMatch(ref: SyncTrackRef): SongEntity? {
        val allSongs = songDao.getAllSongs().firstOrNull() ?: emptyList()

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
                albumArt = ref.albumArt ?: "https://img.youtube.com/vi/${ref.videoId}/hqdefault.jpg",
                dateAdded = System.currentTimeMillis(),
                videoId = ref.videoId
            )
        }

        return null
    }
}
