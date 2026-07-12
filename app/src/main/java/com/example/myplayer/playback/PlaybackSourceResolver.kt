package com.example.myplayer.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.online.InnertubeApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSourceResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedSongDao: DownloadedSongDao,
    private val innertubeApi: InnertubeApi
) {
    suspend fun resolve(request: PlayRequest, setCustomError: (String) -> Unit): String? {
        val path = request.localUri

        // If it starts with http:// or https://, it is already a resolved streaming URL
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            return path
        }

        // Rule 1: Check if the song has been downloaded (is in the downloaded_songs database)
        val downloaded = withContext(Dispatchers.IO) {
            downloadedSongDao.getById(request.songId)
        }

        if (downloaded != null) {
            val localPath = downloaded.localPath
            val fileExists = withContext(Dispatchers.IO) {
                try {
                    if (localPath.startsWith("content://") || localPath.startsWith("file://")) {
                        val uri = Uri.parse(localPath)
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                            it.length > 0
                        } ?: false
                    } else {
                        val file = java.io.File(localPath)
                        file.exists() && file.length() > 0
                    }
                } catch (e: Exception) {
                    false
                }
            }

            if (fileExists) {
                Log.d("PlaybackSourceResolver", "Playing downloaded song locally from: $localPath")
                return localPath
            } else {
                Log.e("PlaybackSourceResolver", "Downloaded file missing at: $localPath")
                setCustomError("Downloaded file not found. Playback stopped.")
                return null
            }
        }

        // Rule 2: If the path is a placeholder online URI (online://<videoId>), resolve streaming URL
        val targetPath = path ?: "online://${request.songId}"
        val isYoutubeVideo = !request.songId.contains("/") && !request.songId.contains(":") && !request.songId.contains("content")
        if (targetPath.startsWith("online://") || isYoutubeVideo) {
            if (!isNetworkAvailable()) {
                Log.e("PlaybackSourceResolver", "Offline, cannot play online song: ${request.songId}")
                if (request.playbackSource == PlaybackSourceType.LOCAL) {
                    setCustomError("Song is no longer available offline. Connect to the internet to stream.")
                } else {
                    setCustomError("No internet connection. Cannot play online song.")
                }
                return null
            }

            Log.d("PlaybackSourceResolver", "Resolving stream URL for online song: ${request.songId}")
            val streamUrl = withContext(Dispatchers.IO) {
                try {
                    innertubeApi.getStreamUrl(request.songId)
                } catch (e: Exception) {
                    Log.e("PlaybackSourceResolver", "Failed to resolve stream URL for online song ${request.songId}", e)
                    null
                }
            }

            if (!streamUrl.isNullOrBlank()) {
                Log.d("PlaybackSourceResolver", "Successfully resolved online song to stream URL: $streamUrl")
                return streamUrl
            } else {
                Log.e("PlaybackSourceResolver", "Failed to resolve stream URL for ${request.songId}")
                setCustomError("Failed to load audio stream. Please check connection.")
                return null
            }
        }

        // Rule 3: Scanned local songs (not in downloaded database, e.g. from local folder scan)
        if (path != null) {
            val fileExists = withContext(Dispatchers.IO) {
                try {
                    if (path.startsWith("content://") || path.startsWith("file://")) {
                        val uri = Uri.parse(path)
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                            it.length > 0
                        } ?: false
                    } else {
                        val file = java.io.File(path)
                        file.exists() && file.length() > 0
                    }
                } catch (e: Exception) {
                    false
                }
            }

            if (fileExists) {
                return path
            } else {
                Log.e("PlaybackSourceResolver", "Local scanned file missing: $path")
                setCustomError("Local file not found. Playback stopped.")
                return null
            }
        }

        setCustomError("Local file not found. Playback stopped.")
        return null
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
