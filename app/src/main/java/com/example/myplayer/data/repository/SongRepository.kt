package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.FolderDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.FolderEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository owning scanned songs, directory folders, scanning, and metadata play counts.
 * Standardizes Dispatchers.IO for SQLite transactions and structured error handling.
 *
 * Introduced in Phase R8 — Data Layer Consolidation.
 */
@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val folderDao: FolderDao,
    private val mediaScanner: MediaScanner
) {
    // Queries
    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()
    fun getTrendingSongs(): Flow<List<SongEntity>> = songDao.getTrendingSongs()
    fun getRecentlyAddedSongs(): Flow<List<SongEntity>> = songDao.getRecentlyAddedSongs()
    fun getMostPlayedSongs(): Flow<List<SongEntity>> = songDao.getMostPlayedSongs()
    fun searchSongs(query: String): Flow<List<SongEntity>> = songDao.searchSongs(query)

    suspend fun incrementPlayCount(songId: String) = withContext(Dispatchers.IO) {
        try {
            songDao.incrementPlayCount(songId)
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Failed to increment play count for $songId", e)
        }
    }

    // Folder Management
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun addFolder(uri: String, name: String) = withContext(Dispatchers.IO) {
        try {
            val folder = FolderEntity(uri, name)
            folderDao.addFolder(folder)
            mediaScanner.scanFolder(folder)
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Failed to add folder $uri", e)
        }
    }

    suspend fun removeFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        try {
            folderDao.deleteFolder(folder)
            songDao.deleteSongsByFolder(folder.uri)
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Failed to remove folder ${folder.uri}", e)
        }
    }

    suspend fun rescanAllFolders() = withContext(Dispatchers.IO) {
        try {
            mediaScanner.scanAllFolders()
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Rescan failed", e)
        }
    }
}
