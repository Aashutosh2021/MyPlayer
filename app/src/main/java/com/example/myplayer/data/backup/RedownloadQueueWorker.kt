package com.example.myplayer.data.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One downloaded song that needs its audio re-fetched after a restore. */
@Serializable
data class RedownloadItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val localPath: String
)

/**
 * Sequential redownload queue for restored downloads. Downloads ONE song per run
 * (resolve stream URL via NewPipe -> DownloadWorker -> await completion), then
 * re-enqueues itself for the rest. Pending items live in a queue file so the
 * queue survives app restarts and device reboots; [resumeIfPending] on app start
 * revives an interrupted queue.
 */
@HiltWorker
class RedownloadQueueWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository,
    private val innertubeApi: InnertubeApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pending = readQueue().ifEmpty { return Result.success() }

        // Skip songs whose file already exists (same-device restore).
        val todo = pending.filter { item ->
            val candidate = BackupRestoreManager.resolveLocalPath(
                appContext, item.localPath, "${item.title}.m4a"
            )
            !BackupRestoreManager.checkFileExists(appContext, candidate)
        }
        if (todo.isEmpty()) {
            writeQueue(emptyList())
            Log.i(TAG, "Redownload queue complete — nothing pending")
            return Result.success()
        }

        val current = todo.first()
        val remaining = todo.drop(1)
        writeQueue(remaining)

        try {
            val streamUrl = innertubeApi.getStreamUrl(current.videoId)
            if (streamUrl.isNullOrBlank()) {
                Log.w(TAG, "Could not resolve stream URL for '${current.title}' — skipping")
            } else {
                val started = downloadRepository.startDownload(
                    OnlineSong(
                        videoId = current.videoId,
                        title = current.title,
                        artist = current.artist,
                        thumbnailUrl = current.thumbnailUrl,
                        durationMs = current.durationMs,
                        streamUrl = streamUrl
                    )
                )
                if (started) awaitDownloadFinished(current.videoId)
                Log.i(TAG, "Redownloaded '${current.title}' — ${remaining.size} left in queue")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Redownload failed for '${current.title}' — continuing with next", e)
        }

        if (remaining.isNotEmpty()) scheduleSelf()
        return Result.success()
    }

    /** Waits (max 8 min) for the per-song DownloadWorker to reach a terminal state. */
    private suspend fun awaitDownloadFinished(videoId: String) {
        try {
            withTimeoutOrNull(TimeUnit.MINUTES.toMillis(8)) {
                WorkManager.getInstance(appContext)
                    .getWorkInfosForUniqueWorkFlow("download_$videoId")
                    .map { infos -> infos.firstOrNull()?.state?.isFinished == true }
                    .filter { finished -> finished }
                    .first()
            }
        } catch (_: TimeoutCancellationException) {
            Log.w(TAG, "Timed out waiting for $videoId — moving on (download may still finish on its own)")
        }
    }

    private fun readQueue(): List<RedownloadItem> = runCatching {
        val file = queueFile()
        if (!file.exists()) return emptyList()
        Json.decodeFromString<List<RedownloadItem>>(file.readText())
    }.getOrDefault(emptyList())

    private fun writeQueue(items: List<RedownloadItem>) = runCatching {
        val file = queueFile()
        if (items.isEmpty()) file.delete() else file.writeText(Json.encodeToString(items))
    }

    private fun queueFile() = File(appContext.filesDir, "redownload_queue.json")

    private fun scheduleSelf() {
        val request = OneTimeWorkRequestBuilder<RedownloadQueueWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            WORK_NAME, ExistingWorkPolicy.REPLACE, request
        )
    }

    companion object {
        private const val TAG = "RedownloadQueueWorker"
        private const val WORK_NAME = "com.example.myplayer.REDOWNLOAD_QUEUE"

        /** Starts a fresh sequential redownload queue (replaces any running one). */
        suspend fun enqueue(context: Context, items: List<RedownloadItem>) = withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext
            val file = File(context.filesDir, "redownload_queue.json")
            file.writeText(Json.encodeToString(items))
            Log.i(TAG, "Queued ${items.size} songs for sequential redownload")
            val request = OneTimeWorkRequestBuilder<RedownloadQueueWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }

        /** Revives an interrupted queue on app start (KEEP — never restarts a live queue). */
        fun resumeIfPending(context: Context) {
            runCatching {
                val file = File(context.filesDir, "redownload_queue.json")
                if (!file.exists() || file.length() == 0L) return
                val request = OneTimeWorkRequestBuilder<RedownloadQueueWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME, ExistingWorkPolicy.KEEP, request
                )
            }
        }
    }
}
