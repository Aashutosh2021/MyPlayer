package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.PlaylistDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.local.entity.PlaylistSongCrossReference
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.model.toSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository owning Playlist creation, deletions, item additions, reordering, and removals.
 * Standardizes Dispatchers.IO for SQLite transactions and structured error handling.
 * Consolidates mapping functions by invoking mapped extension helpers.
 *
 * Introduced in Phase R8 — Data Layer Consolidation.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val downloadedSongDao: DownloadedSongDao
) {
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> = playlistDao.getSongsInPlaylist(playlistId)

    fun getSongsInPlaylistAsPlayable(playlistId: Long): Flow<List<PlayableSong>> {
        return combine(
            playlistDao.getSongsInPlaylist(playlistId),
            downloadedSongDao.getAllDownloads()
        ) { playlistSongs, downloadedSongs ->
            val downloadedMap = downloadedSongs.associateBy { it.id }
            playlistSongs.map { entity ->
                val downloaded = downloadedMap[entity.id] ?: downloadedMap[entity.videoId ?: ""]
                if (downloaded != null) {
                    PlayableSong.Downloaded(downloaded)
                } else if (entity.path.startsWith("online://")) {
                    val vid = entity.videoId?.takeIf { it.isNotBlank() } ?: entity.path.removePrefix("online://")
                    PlayableSong.Online(
                        id = vid,
                        title = entity.title,
                        artist = entity.artist,
                        durationMs = entity.duration,
                        thumbnailUrl = entity.albumArt
                    )
                } else if (!entity.videoId.isNullOrBlank()) {
                    PlayableSong.Online(
                        id = entity.videoId,
                        title = entity.title,
                        artist = entity.artist,
                        durationMs = entity.duration,
                        thumbnailUrl = entity.albumArt
                    )
                } else {
                    PlayableSong.Local(entity)
                }
            }
        }
    }


    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        try {
            playlistDao.insertPlaylist(PlaylistEntity(name = name))
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to create playlist $name", e)
            -1L
        }
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        try {
            playlistDao.renamePlaylist(playlistId, newName)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to rename playlist $playlistId to $newName", e)
        }
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        try {
            playlistDao.deletePlaylist(playlist)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to delete playlist ${playlist.id}", e)
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: PlayableSong, position: Int) = withContext(Dispatchers.IO) {
        try {
            when (song) {
                is PlayableSong.Online -> {
                    val entity = song.toSongEntity()
                    songDao.insertSongs(listOf(entity))
                }
                is PlayableSong.Downloaded -> {
                    val entity = song.toSongEntity()
                    songDao.insertSongs(listOf(entity))
                }
                is PlayableSong.Local -> {
                    // Local songs are already in the DB, no insertion needed.
                }
            }
            playlistDao.insertSongToPlaylist(PlaylistSongCrossReference(playlistId, song.id, position))
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to add song ${song.id} to playlist $playlistId", e)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        try {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to remove song $songId from playlist $playlistId", e)
        }
    }

    suspend fun reorderSongsInPlaylist(playlistId: Long, orderedSongIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            orderedSongIds.forEachIndexed { index, songId ->
                playlistDao.updateSongPosition(playlistId, songId, index)
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to reorder playlist $playlistId", e)
        }
    }
}
