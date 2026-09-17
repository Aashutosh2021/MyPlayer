package com.example.myplayer.data.download

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio tagging and media scanner helper.
 * Inspired by LastWave's download metadata tagging and MediaStore reconciliation.
 */
@Singleton
class AudioTagHelper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "AudioTagHelper"
    }

    /**
     * Downloads cover art bytes from the given URL.
     */
    suspend fun fetchArtworkBytes(thumbnailUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isBlank()) return@withContext null
        try {
            val request = Request.Builder()
                .url(thumbnailUrl)
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download cover art from $thumbnailUrl", e)
            null
        }
    }

    /**
     * Scans the downloaded file with Android's MediaScannerConnection
     * so it immediately appears in system audio pickers, MediaStore, and external players.
     */
    fun scanDownloadedFile(context: Context, localPath: String, title: String, artist: String) {
        try {
            if (localPath.startsWith("content://")) {
                // Scoped storage SAF URIs don't use file-path based MediaScanner
                return
            }
            val file = File(localPath)
            if (!file.exists() || file.length() <= 0L) return

            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf("audio/*")
            ) { path, uri ->
                Log.d(TAG, "MediaScanner indexed: path=$path uri=$uri title='$title' artist='$artist'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning downloaded file: $localPath", e)
        }
    }
}
