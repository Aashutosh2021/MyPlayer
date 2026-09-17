package com.example.myplayer.aria.cache

import android.util.Log
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AriaMemoryCache @Inject constructor(
    private val songDao: SongDao,
    private val downloadedSongDao: DownloadedSongDao
) {
    companion object {
        private const val TAG = "AriaMemoryCache"
    }

    private val localSongs = ConcurrentHashMap<String, SongEntity>()
    private val downloadedSongs = ConcurrentHashMap<String, DownloadedSongEntity>()
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        startSyncing()
    }

    private fun startSyncing() {
        // Sync local songs flow into memory cache
        cacheScope.launch {
            try {
                songDao.getAllSongs().collectLatest { list ->
                    val startTime = System.currentTimeMillis()
                    localSongs.clear()
                    for (song in list) {
                        localSongs[song.id] = song
                    }
                    Log.d(TAG, "Synced ${list.size} local songs to memory cache in ${System.currentTimeMillis() - startTime}ms")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing local songs to cache", e)
            }
        }

        // Sync downloaded songs flow into memory cache
        cacheScope.launch {
            try {
                downloadedSongDao.getAllDownloads().collectLatest { list ->
                    val startTime = System.currentTimeMillis()
                    downloadedSongs.clear()
                    for (download in list) {
                        downloadedSongs[download.id] = download
                    }
                    Log.d(TAG, "Synced ${list.size} downloaded songs to memory cache in ${System.currentTimeMillis() - startTime}ms")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing downloaded songs to cache", e)
            }
        }
    }

    fun getLocalSong(id: String): SongEntity? {
        return localSongs[id]
    }

    fun getDownloadedSong(id: String): DownloadedSongEntity? {
        return downloadedSongs[id]
    }

    fun getAllLocalSongs(): List<SongEntity> {
        return localSongs.values.toList()
    }

    fun getAllDownloadedSongs(): List<DownloadedSongEntity> {
        return downloadedSongs.values.toList()
    }
}
