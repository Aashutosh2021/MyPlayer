package com.example.myplayer.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.myplayer.data.download.DownloadWorker
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.online.model.OnlineSong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedSongDao: DownloadedSongDao
) {
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    fun setDownloadProgress(videoId: String, progress: Int) {
        _downloadProgress.update { it + (videoId to progress) }
    }

    fun removeDownload(videoId: String) {
        _downloadProgress.update { it - videoId }
    }

    fun getAllDownloads(): Flow<List<DownloadedSongEntity>> = downloadedSongDao.getAllDownloads()

    suspend fun isDownloaded(videoId: String): Boolean = downloadedSongDao.existsById(videoId)

    fun getDownloadedById(id: String): Flow<List<DownloadedSongEntity>> =
        downloadedSongDao.searchDownloads("")

    /**
     * Schedules an expedited background download for the given [song].
     * Uses WorkManager's Expedited Jobs to ensure the download starts immediately,
     * bypassing background execution limits even if notifications are suppressed.
     */
    fun startDownload(song: OnlineSong): Boolean {
        val streamUrl = song.streamUrl
        if (streamUrl.isNullOrBlank()) {
            Log.e("DownloadRepository", "No stream URL for ${song.videoId}")
            return false
        }

        Log.i("DownloadRepository", "Scheduling expedited download for: ${song.title}")
        
        val data = workDataOf(
            DownloadWorker.KEY_VIDEO_ID to song.videoId,
            DownloadWorker.KEY_TITLE to song.title,
            DownloadWorker.KEY_ARTIST to song.artist,
            DownloadWorker.KEY_THUMBNAIL_URL to song.thumbnailUrl,
            DownloadWorker.KEY_DURATION_MS to song.durationMs,
            DownloadWorker.KEY_STREAM_URL to streamUrl
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_${song.videoId}",
            ExistingWorkPolicy.KEEP,   // Don't cancel a running download if same song is enqueued again
            request
        )
        
        return true
    }

    suspend fun deleteDownload(entity: DownloadedSongEntity) {
        try {
            java.io.File(entity.localPath).delete()
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Could not delete file", e)
        }
        downloadedSongDao.deleteById(entity.id)
    }

    suspend fun insertDownload(song: DownloadedSongEntity) {
        downloadedSongDao.insert(song)
    }

    fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>> =
        downloadedSongDao.searchDownloads(query)
}
