package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.RecentHistoryDao
import com.example.myplayer.data.local.entity.RecentHistoryEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository owning User Listening History/Recent plays tracking.
 * Standardizes Dispatchers.IO for SQLite transactions and structured error handling.
 *
 * Introduced in Phase R8 — Data Layer Consolidation.
 */
@Singleton
class RecentHistoryRepository @Inject constructor(
    private val recentHistoryDao: RecentHistoryDao
) {
    fun getRecentHistory(): Flow<List<SongEntity>> = recentHistoryDao.getRecentHistory()

    suspend fun addRecentHistory(songId: String) = withContext(Dispatchers.IO) {
        try {
            recentHistoryDao.addRecent(RecentHistoryEntity(songId))
        } catch (e: Exception) {
            android.util.Log.e("RecentHistoryRepository", "Failed to add recent history for $songId", e)
        }
    }

    suspend fun getHistoryCount(): Int = withContext(Dispatchers.IO) {
        try {
            recentHistoryDao.getHistoryCount()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getRecentHistoryEntries(limit: Int = 50): List<RecentHistoryEntity> = withContext(Dispatchers.IO) {
        try {
            recentHistoryDao.getRecentHistoryEntries(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

