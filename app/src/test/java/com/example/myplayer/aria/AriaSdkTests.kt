package com.example.myplayer.aria

import android.os.Bundle
import com.example.myplayer.aria.cache.AriaMemoryCache
import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class AriaSdkTests {

    // Manual stub implementations for Room DAOs
    private class StubSongDao : SongDao {
        override fun getAllSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override suspend fun getAllSongsSync(): List<SongEntity> = emptyList()
        override suspend fun getSongByVideoId(videoId: String): SongEntity? = null
        override fun getTrendingSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override fun getSongById(id: String): SongEntity? = null
        override fun insertSongs(songs: List<SongEntity>) {}
        override fun deleteSongsByFolder(folderUri: String) {}
        override fun getRecentlyAddedSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override fun getMostPlayedSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override fun incrementPlayCount(id: String) {}
        override fun updateSongPath(id: String, path: String) {}
        override fun searchSongs(query: String): Flow<List<SongEntity>> = flowOf(emptyList())
    }

    private class StubDownloadedSongDao : DownloadedSongDao {
        override fun getAllDownloads(): Flow<List<DownloadedSongEntity>> = flowOf(emptyList())
        override fun getAllDownloadsSync(): List<DownloadedSongEntity> = emptyList()
        override fun insertDownloads(downloads: List<DownloadedSongEntity>) {}
        override suspend fun getById(id: String): DownloadedSongEntity? = null
        override suspend fun existsById(id: String): Boolean = false
        override suspend fun insert(song: DownloadedSongEntity) {}
        override suspend fun deleteById(id: String) {}
        override fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>> = flowOf(emptyList())
    }

    @Test
    fun testMemoryCacheLookups() {
        val songDao = StubSongDao()
        val downloadedSongDao = StubDownloadedSongDao()
        
        val cache = AriaMemoryCache(songDao, downloadedSongDao)
        assertNull(cache.getLocalSong("nonexistent"))
        assertNull(cache.getDownloadedSong("nonexistent"))
    }

    @Test
    fun testPerformanceMonitor() {
        val monitor = AriaPerformanceMonitor()
        monitor.recordLatency("database", 10L)
        monitor.recordLatency("database", 20L)
        
        assertEquals(15.0, monitor.getAverageLatency("database"), 0.01)
        assertEquals(2L, monitor.getSampleCount("database"))
    }

    @Test
    fun testAriaStatusEnumConsistency() {
        assertEquals(AriaStatus.SUCCESS, AriaStatus.valueOf("SUCCESS"))
        assertEquals(AriaStatus.PERMISSION_DENIED, AriaStatus.valueOf("PERMISSION_DENIED"))
    }
}
