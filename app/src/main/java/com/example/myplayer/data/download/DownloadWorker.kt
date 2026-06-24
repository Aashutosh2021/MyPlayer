package com.example.myplayer.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.prefs.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val downloadedSongDao: DownloadedSongDao,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG_DOWNLOAD = "download"
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_THUMBNAIL_URL = "thumbnail_url"
        const val KEY_DURATION_MS = "duration_ms"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIF_ID = 1001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Ensure the channel exists before posting the notification (Android 8+)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Music download progress"
                }
            )
        }
        return ForegroundInfo(
            NOTIF_ID,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading Song")
                .setContentText("Fetching audio stream...")
                .setOngoing(true)
                .setSilent(true)
                .build(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Failed to set foreground", e)
        }
        
        Log.i("DOWNLOAD_DEBUG", "=== WORKER STARTED === runAttemptCount=$runAttemptCount id=$id")
        val videoId = inputData.getString(KEY_VIDEO_ID)
        if (videoId == null) {
            Log.e("DOWNLOAD_DEBUG", "WORKER FAILED: videoId is null")
            return Result.failure()
        }
        val title = inputData.getString(KEY_TITLE) ?: "Unknown"
        val artist = inputData.getString(KEY_ARTIST) ?: "Unknown"
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""
        val durationMs = inputData.getLong(KEY_DURATION_MS, 0L)
        val streamUrl = inputData.getString(KEY_STREAM_URL)
        if (streamUrl == null) {
            Log.e("DOWNLOAD_DEBUG", "WORKER FAILED: streamUrl is null")
            return Result.failure(workDataOf(KEY_ERROR to "No stream URL provided"))
        }
        Log.i("DOWNLOAD_DEBUG", "WORKER INPUT: title=$title, videoId=$videoId, url=${streamUrl.take(80)}...")
        Log.i("DownloadWorker", "Starting download: $title ($videoId)")
        setProgress(workDataOf(KEY_PROGRESS to 0))

        return try {
            val customFolderUriString = preferencesManager.downloadFolderUri.firstOrNull()
            val hasCustomFolder = !customFolderUriString.isNullOrEmpty()

            val safeTitle = title
                .replace(Regex("[^a-zA-Z0-9 _\\-]"), "")
                .trim()
                .take(60)
                .ifBlank { videoId }

            val finalFileName = "$safeTitle.m4a"
            val tmpFileName = "$safeTitle.tmp"

            // Skip if already downloaded
            if (downloadedSongDao.existsById(videoId)) {
                return Result.success(workDataOf(KEY_PROGRESS to 100))
            }

            if (isStopped) return Result.failure(workDataOf(KEY_ERROR to "Stopped before start"))

            var savedPath = ""
            var fileSize = 0L

            // Bypass YouTube throttling using chunked requests and Android User-Agent
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            val CHUNK_SIZE = 10L * 1024 * 1024 // 10MB chunks
            
            var totalSize = -1L
            try {
                val headReq = Request.Builder()
                    .url(streamUrl)
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Range", "bytes=0-0")
                    .build()
                okHttpClient.newCall(headReq).execute().use { response ->
                    val cr = response.header("Content-Range")
                    if (cr != null) {
                        totalSize = cr.substringAfterLast("/").toLongOrNull() ?: -1L
                    } else if (response.isSuccessful) {
                        totalSize = response.body?.contentLength() ?: -1L
                    }
                }
            } catch(e: Exception) {
                Log.e("DOWNLOAD_DEBUG", "Failed to fetch file size", e)
            }

            suspend fun downloadToStream(out: java.io.OutputStream, startOffset: Long): Long {
                var downloaded = startOffset
                while (true) {
                    if (isStopped) throw IOException("Stopped by user")
                    val endOffset = if (totalSize > 0) minOf(downloaded + CHUNK_SIZE - 1, totalSize - 1) else downloaded + CHUNK_SIZE - 1
                    val req = Request.Builder()
                        .url(streamUrl)
                        .addHeader("User-Agent", userAgent)
                        .addHeader("Range", "bytes=$downloaded-$endOffset")
                        .build()

                    val bytesReadThisChunk = withContext(Dispatchers.IO) {
                        okHttpClient.newCall(req).execute().use { response ->
                            if (response.code == 416) return@withContext -1L // Reached end of stream
                            if (!response.isSuccessful && response.code != 206) throw IOException("HTTP ${response.code}")
                            val body = response.body ?: throw IOException("Empty body")
                            val stream = body.byteStream()
                            val buf = ByteArray(8192)
                            var readThisChunk = 0L
                            while (true) {
                                val r = stream.read(buf)
                                if (r == -1) break
                                out.write(buf, 0, r)
                                readThisChunk += r
                            }
                            readThisChunk
                        }
                    }

                    if (bytesReadThisChunk <= 0L) break
                    downloaded += bytesReadThisChunk
                    if (totalSize > 0) {
                        val prog = ((downloaded.toFloat() / totalSize) * 100).toInt()
                        setProgress(workDataOf(KEY_PROGRESS to prog.coerceIn(0, 99)))
                    } else {
                        setProgress(workDataOf(KEY_PROGRESS to 50))
                    }
                    
                    if (totalSize > 0 && downloaded >= totalSize) break
                    if (bytesReadThisChunk < CHUNK_SIZE && totalSize == -1L) break
                }
                return downloaded
            }

            if (hasCustomFolder) {
                val treeUri = Uri.parse(customFolderUriString!!)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    ?: return Result.failure(workDataOf(KEY_ERROR to "Could not access custom folder"))

                rootDoc.findFile(tmpFileName)?.delete()
                val tmpDoc = rootDoc.createFile("application/octet-stream", tmpFileName)
                    ?: return Result.failure(workDataOf(KEY_ERROR to "Could not create temp file"))

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(tmpDoc.uri)?.use { out -> 
                        downloadToStream(out, 0L) 
                    }
                }

                rootDoc.findFile(finalFileName)?.delete()
                tmpDoc.renameTo(finalFileName)
                savedPath = tmpDoc.uri.toString()
                fileSize = tmpDoc.length()
            } else {
                val musicDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: context.filesDir
                musicDir.mkdirs()
                val destFile = File(musicDir, finalFileName)
                val tempFile = File(musicDir, tmpFileName)

                var alreadyDownloaded = if (tempFile.exists()) tempFile.length() else 0L
                if (alreadyDownloaded > 0 && totalSize > 0 && alreadyDownloaded >= totalSize) {
                    alreadyDownloaded = 0L
                    tempFile.delete()
                }

                withContext(Dispatchers.IO) {
                    java.io.FileOutputStream(tempFile, alreadyDownloaded > 0L).use { out ->
                        downloadToStream(out, alreadyDownloaded)
                    }
                }

                if (!tempFile.renameTo(destFile)) {
                    tempFile.copyTo(destFile, overwrite = true)
                    tempFile.delete()
                }
                savedPath = destFile.absolutePath
                fileSize = destFile.length()
            }

            val entity = DownloadedSongEntity(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
                localPath = savedPath,
                fileSizeBytes = fileSize,
                downloadedAt = System.currentTimeMillis()
            )
            downloadedSongDao.insert(entity)
            setProgress(workDataOf(KEY_PROGRESS to 100))
            Result.success(workDataOf(KEY_PROGRESS to 100))

        } catch (e: Exception) {
            Log.e("DownloadWorker", "Download failed for $videoId", e)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }
}
