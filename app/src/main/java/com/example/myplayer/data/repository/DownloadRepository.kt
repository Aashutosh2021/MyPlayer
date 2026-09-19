package com.example.myplayer.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.myplayer.data.download.DownloadWorker
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.dao.CachedLyricsDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.online.model.OnlineSong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadedSongDao: DownloadedSongDao,
    private val songDao: SongDao,
    private val cachedLyricsDao: CachedLyricsDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _manualProgress = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val workManagerFlow: Flow<List<WorkInfo>> = flow {
        try {
            emitAll(WorkManager.getInstance(context).getWorkInfosByTagFlow("download"))
        } catch (e: Exception) {
            Log.w("DownloadRepository", "Could not observe WorkManager download tag flow", e)
            emit(emptyList())
        }
    }

    val downloadProgress: StateFlow<Map<String, Int>> = combine(
        workManagerFlow,
        _manualProgress
    ) { workInfos, manual ->
        val terminalIds = workInfos
            .filter { it.state.isFinished }
            .mapNotNull { info ->
                info.tags.firstOrNull { it.startsWith("videoId_") }?.removePrefix("videoId_")
            }
            .toSet()

        if (terminalIds.isNotEmpty()) {
            _manualProgress.update { current -> current - terminalIds }
        }

        val activeMap = workInfos
            .filter { info ->
                info.state == WorkInfo.State.ENQUEUED ||
                info.state == WorkInfo.State.RUNNING ||
                info.state == WorkInfo.State.BLOCKED
            }
            .mapNotNull { info ->
                val videoId = info.tags.firstOrNull { it.startsWith("videoId_") }
                    ?.removePrefix("videoId_")
                    ?: return@mapNotNull null
                val prog = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                videoId to prog
            }
            .toMap()
        (manual - terminalIds) + activeMap
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun setDownloadProgress(videoId: String, progress: Int) {
        _manualProgress.update { it + (videoId to progress) }
    }

    fun removeDownload(videoId: String) {
        _manualProgress.update { it - videoId }
    }

    fun isDownloading(videoId: String): Boolean {
        return downloadProgress.value.containsKey(videoId)
    }

    fun getAllDownloads(): Flow<List<DownloadedSongEntity>> = downloadedSongDao.getAllDownloads()

    suspend fun isDownloaded(videoId: String): Boolean = withContext(Dispatchers.IO) {
        downloadedSongDao.existsById(videoId)
    }

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
            Log.e("DownloadRepository", "[DOWNLOAD] No stream URL for ${song.videoId}")
            return false
        }

        Log.i("DownloadRepository", "[DOWNLOAD] Scheduling expedited download for: ${song.title} (${song.videoId})")
        setDownloadProgress(song.videoId, 0)

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
            .addTag("download")
            .addTag("videoId_${song.videoId}")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        return try {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_${song.videoId}",
                ExistingWorkPolicy.KEEP,   // Don't cancel a running download if same song is enqueued again
                request
            )
            Log.i("DownloadRepository", "[DOWNLOAD] Successfully enqueued WorkManager job for: ${song.videoId}")
            true
        } catch (e: Exception) {
            Log.e("DownloadRepository", "[DOWNLOAD] Failed to enqueue WorkManager job for ${song.videoId}", e)
            removeDownload(song.videoId)
            false
        }
    }

    suspend fun deleteDownload(entity: DownloadedSongEntity) {
        try {
            if (entity.localPath.startsWith("content://")) {
                val uri = android.net.Uri.parse(entity.localPath)
                val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                doc?.delete()
            } else {
                val file = File(entity.localPath)
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Could not delete file", e)
        }

        // Delete cached lyrics
        try {
            cachedLyricsDao.deleteLyricsForSong(entity.id)
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Could not delete cached lyrics for ${entity.id}", e)
        }

        downloadedSongDao.deleteById(entity.id)
        removeDownload(entity.id)
        Log.i("DownloadRepository", "Deleted download record for ${entity.id}. PlaybackSourceResolver will route online on next play.")
    }

    suspend fun deleteDownloadById(videoId: String) = withContext(Dispatchers.IO) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("download_$videoId")
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Could not cancel WorkManager download task for $videoId", e)
        }

        removeDownload(videoId)

        val entity = downloadedSongDao.getById(videoId)
        if (entity != null) {
            deleteDownload(entity)
        } else {
            downloadedSongDao.deleteById(videoId)
            try {
                cachedLyricsDao.deleteLyricsForSong(videoId)
            } catch (e: Exception) {
                Log.e("DownloadRepository", "Could not delete cached lyrics for $videoId", e)
            }
        }
    }

    suspend fun insertDownload(song: DownloadedSongEntity) {
        downloadedSongDao.insert(song)
    }

    suspend fun performIntegrityCheck() {
        val downloads = withContext(Dispatchers.IO) {
            downloadedSongDao.getAllDownloadsSync()
        }

        withContext(Dispatchers.IO) {
            val toDelete = mutableListOf<String>()
            val seenFilePaths = mutableSetOf<String>()

            for (download in downloads) {
                val path = download.localPath
                // 1. Check duplicate file path entries
                if (path in seenFilePaths) {
                    Log.w("DownloadRepository", "Duplicate path entry detected for ${download.title}. Removing database record.")
                    toDelete.add(download.id)
                    continue
                }
                seenFilePaths.add(path)

                // 2. Exclude songs that are currently enqueued or actively downloading in WorkManager
                val workInfos: List<WorkInfo> = try {
                    WorkManager.getInstance(context).getWorkInfosForUniqueWork("download_${download.id}").get()
                } catch (e: Exception) {
                    emptyList()
                }
                
                var isDownloading = false
                for (info in workInfos) {
                    if (info.state == WorkInfo.State.ENQUEUED || 
                        info.state == WorkInfo.State.RUNNING || 
                        info.state == WorkInfo.State.BLOCKED) {
                        isDownloading = true
                        break
                    }
                }

                if (isDownloading) {
                    continue
                }

                // 3. Verify file exists and is not empty (corrupted/missing)
                val fileExists = try {
                    if (path.startsWith("content://") || path.startsWith("file://")) {
                        val uri = android.net.Uri.parse(path)
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                            it.length > 0
                        } ?: false
                    } else {
                        val file = File(path)
                        file.exists() && file.length() > 0
                    }
                } catch (e: Exception) {
                    false
                }

                if (!fileExists) {
                    Log.w("DownloadRepository", "Integrity check failed: file missing or empty for ${download.title} at $path. Removing database record.")
                    toDelete.add(download.id)
                }
            }

            if (toDelete.isNotEmpty()) {
                for (id in toDelete) {
                    downloadedSongDao.deleteById(id)
                    // NOTE: We intentionally do NOT reset SongEntity.path to "online://" here.
                    // PlaybackSourceResolver handles routing correctly when downloaded_songs record is absent.
                }
                Log.i("DownloadRepository", "Integrity check completed. Cleaned up ${toDelete.size} stale/invalid download records.")
            } else {
                Log.i("DownloadRepository", "Integrity check completed successfully. All records valid.")
            }
        }
    }

    fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>> =
        downloadedSongDao.searchDownloads(query)
}
