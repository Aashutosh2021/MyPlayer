package com.example.myplayer.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.online.InnertubeApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSourceResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedSongDao: DownloadedSongDao,
    private val songDao: SongDao,
    private val innertubeApi: InnertubeApi,
    private val losslessStreamResolver: LosslessStreamResolver
) {
    private val _currentAudioQuality = MutableStateFlow(AudioQualityInfo())
    val currentAudioQuality: StateFlow<AudioQualityInfo> = _currentAudioQuality.asStateFlow()

    suspend fun resolve(request: PlayRequest, setCustomError: (String) -> Unit): String? {
        val path = request.localUri

        // If it starts with http:// or https://, it is already a resolved streaming URL
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            _currentAudioQuality.value = losslessStreamResolver.inspectQuality(path, isLocalFile = false)
            return path
        }

        // Rule 1: Check if the song has been recorded in the downloaded_songs database
        val downloaded = withContext(Dispatchers.IO) {
            downloadedSongDao.getById(request.songId)
        }

        if (downloaded != null) {
            val localPath = downloaded.localPath
            val fileExists = checkLocalFileExists(localPath)

            if (fileExists) {
                Log.d("PlaybackSourceResolver", "Playing downloaded song locally from: $localPath")
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(localPath, isLocalFile = true)
                return localPath
            } else {
                Log.w("PlaybackSourceResolver", "Downloaded file missing at: $localPath for ${request.songId}. Falling back to online streaming.")
                // Clean up stale download record so user can manually re-download
                withContext(Dispatchers.IO) {
                    try {
                        downloadedSongDao.deleteById(request.songId)
                    } catch (e: Exception) {
                        Log.e("PlaybackSourceResolver", "Failed to clean stale download record", e)
                    }
                }
                // Fall back directly to streaming using the download ID (YouTube videoId)
                return resolveOnlineStream(downloaded.id, setCustomError)
            }
        }

        // Rule 2: If path is a local file / content URI (not online://)
        if (path != null && !path.startsWith("online://")) {
            val fileExists = checkLocalFileExists(path)

            if (fileExists) {
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(path, isLocalFile = true)
                return path
            } else {
                Log.w("PlaybackSourceResolver", "Local file missing: $path for song ${request.songId}")
                // Check if this song can be streamed (e.g. from restored backup or YouTube origin)
                val videoId = withContext(Dispatchers.IO) {
                    songDao.getSongById(request.songId)?.videoId
                } ?: request.metadata["videoId"]
                  ?: if (request.songId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) request.songId else null

                if (!videoId.isNullOrBlank()) {
                    Log.i("PlaybackSourceResolver", "Local file missing, falling back to streaming for videoId: $videoId")
                    return resolveOnlineStream(videoId, setCustomError)
                } else {
                    setCustomError("Local file not found. Playback stopped.")
                    return null
                }
            }
        }

        // Rule 3: Online songs (path starts with online:// or path is null)
        val videoId = if (path != null && path.startsWith("online://")) {
            path.removePrefix("online://")
        } else if (request.songId.startsWith("online://")) {
            request.songId.removePrefix("online://")
        } else {
            request.songId
        }

        return resolveOnlineStream(videoId, setCustomError)
    }

    private suspend fun checkLocalFileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                val uri = Uri.parse(filePath)
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length > 0
                } ?: false
            } else {
                val file = File(filePath)
                file.exists() && file.length() > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun resolveOnlineStream(videoId: String, setCustomError: (String) -> Unit): String? {
        if (!isNetworkAvailable()) {
            Log.e("PlaybackSourceResolver", "Offline, cannot play online song: $videoId")
            setCustomError("No internet connection. Cannot stream song.")
            return null
        }

        Log.d("PlaybackSourceResolver", "Resolving stream URL for videoId: $videoId")
        val streamUrl = withContext(Dispatchers.IO) {
            try {
                innertubeApi.getStreamUrl(videoId)
            } catch (e: Exception) {
                Log.e("PlaybackSourceResolver", "Failed to resolve stream URL for $videoId", e)
                null
            }
        }

        if (!streamUrl.isNullOrBlank()) {
            Log.d("PlaybackSourceResolver", "Successfully resolved $videoId to stream URL: $streamUrl")
            _currentAudioQuality.value = losslessStreamResolver.inspectQuality(streamUrl, isLocalFile = false)
            return streamUrl
        } else {
            Log.e("PlaybackSourceResolver", "Failed to resolve stream URL for $videoId")
            setCustomError("Failed to load audio stream. Please check connection.")
            return null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
