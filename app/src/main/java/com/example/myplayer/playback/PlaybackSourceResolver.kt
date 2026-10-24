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

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    // 5-hour TTL: YouTube InnerTube stream URLs expire after ~6 hours
    private val streamUrlTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val STREAM_URL_TTL_MS = 5 * 60 * 60 * 1000L

    // Spotify-imported Liked Songs / Playlist tracks carry 22-char Spotify IDs.
    // Cache maps them to their matched 11-char YouTube videoIds so a match is
    // never searched twice during a session.
    private val spotifyToYoutubeIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun resolve(
        request: PlayRequest,
        setCustomError: ((String) -> Unit)? = null
    ): String? {
        val localUri = request.localUri

        // 1. Pure local file check: only true filesystem/content URIs
        if (request.playbackSource == PlaybackSourceType.LOCAL && !localUri.isNullOrBlank()) {
            if (localUri.startsWith("content://") || localUri.startsWith("file://") || localUri.startsWith("/")) {
                val fileExists = checkLocalFileExists(localUri)
                if (fileExists) {
                    _currentAudioQuality.value = losslessStreamResolver.inspectQuality(localUri, isLocalFile = true)
                    return localUri
                } else {
                    Log.w("PlaybackSourceResolver", "Local file missing: $localUri for song ${request.songId}")
                    val dbVideoId = withContext(Dispatchers.IO) {
                        songDao.getSongById(request.songId)?.videoId
                    } ?: request.metadata["videoId"]
                    if (!dbVideoId.isNullOrBlank()) {
                        return resolveOnlineStream(dbVideoId, setCustomError)
                    }
                    setCustomError?.invoke("Local file not found. Playback stopped.")
                    return null
                }
            }
        }

        // 2. Downloaded songs check
        val downloaded = withContext(Dispatchers.IO) {
            downloadedSongDao.getById(request.songId)
        }
        if (downloaded != null) {
            val dlPath = downloaded.localPath
            val fileExists = checkLocalFileExists(dlPath)
            if (fileExists) {
                Log.d("PlaybackSourceResolver", "Playing downloaded song locally from: $dlPath")
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(dlPath, isLocalFile = true)
                return dlPath
            } else {
                Log.w("PlaybackSourceResolver", "Downloaded file missing at: $dlPath for ${request.songId}. Falling back to online streaming.")
                withContext(Dispatchers.IO) {
                    try { downloadedSongDao.deleteById(request.songId) } catch (_: Exception) {}
                }
                return resolveOnlineStream(downloaded.id, setCustomError)
            }
        }

        // 3. Direct HTTP stream URL already resolved
        val directStreamUrl = if (!request.streamUrl.isNullOrBlank() && request.streamUrl.startsWith("http")) {
            request.streamUrl
        } else if (!localUri.isNullOrBlank() && (localUri.startsWith("http://") || localUri.startsWith("https://"))) {
            localUri
        } else {
            null
        }

        if (!directStreamUrl.isNullOrBlank()) {
            val rawVideoId = when {
                request.songId.matches(Regex("^[a-zA-Z0-9_-]{11}$")) -> request.songId
                localUri?.startsWith("online://") == true -> localUri.removePrefix("online://").takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
                else -> request.metadata["videoId"]
            }
            if (rawVideoId != null) {
                val cachedTimestamp = streamUrlTimestamps[rawVideoId]
                val isExpired = cachedTimestamp != null && (System.currentTimeMillis() - cachedTimestamp) >= STREAM_URL_TTL_MS
                if (isExpired) {
                    Log.w("PlaybackSourceResolver", "Stream URL for $rawVideoId is older than 5 hours. Refreshing fresh stream.")
                    return resolveOnlineStream(rawVideoId, setCustomError, forceRefresh = true)
                }
            }
            _currentAudioQuality.value = losslessStreamResolver.inspectQuality(directStreamUrl, isLocalFile = false)
            return directStreamUrl
        }

        // 4. Extract clean ID from localUri or songId
        val rawId = when {
            localUri?.startsWith("online://") == true -> localUri.removePrefix("online://")
            else -> request.songId
        }

        var cleanId = rawId
            .removePrefix("online://")
            .removePrefix("spotify:track:")
            .removePrefix("spotify:")
            .trim()

        if (cleanId.isBlank()) {
            setCustomError?.invoke("Invalid track ID")
            return null
        }

        // 5. If cleanId is a 22-char Spotify ID (not an 11-char YouTube videoId),
        // automatically match it to its 11-char YouTube videoId using title + artist!
        val isYoutubeId = cleanId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))
        if (!isYoutubeId) {
            val cachedYtId = spotifyToYoutubeIdCache[cleanId]
            if (!cachedYtId.isNullOrBlank()) {
                cleanId = cachedYtId
            } else {
                val dbSong = withContext(Dispatchers.IO) { songDao.getSongById(request.songId) }
                val existingYtId = dbSong?.videoId?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
                if (!existingYtId.isNullOrBlank()) {
                    spotifyToYoutubeIdCache[cleanId] = existingYtId
                    cleanId = existingYtId
                } else {
                    val qTitle = request.title.ifBlank { dbSong?.title ?: request.metadata["title"] ?: "" }
                    val qArtist = request.artist.ifBlank { dbSong?.artist ?: request.metadata["artist"] ?: "" }
                    val query = "$qTitle $qArtist".trim()
                    if (query.isNotBlank()) {
                        val matchedYtId = withContext(Dispatchers.IO) {
                            try {
                                innertubeApi.search(query, filtered = true).songs.firstOrNull()?.videoId
                            } catch (e: Exception) {
                                Log.w("PlaybackSourceResolver", "Spotify->YouTube match failed for $cleanId", e)
                                null
                            }
                        }
                        if (!matchedYtId.isNullOrBlank()) {
                            spotifyToYoutubeIdCache[cleanId] = matchedYtId
                            cleanId = matchedYtId
                            withContext(Dispatchers.IO) {
                                try { songDao.updateSongVideoId(request.songId, matchedYtId) } catch (_: Exception) {}
                            }
                        } else {
                            setCustomError?.invoke("Could not find matching stream for '${qTitle}'")
                            return null
                        }
                    } else {
                        setCustomError?.invoke("Invalid track ID: $cleanId")
                        return null
                    }
                }
            }
        }

        return resolveOnlineStream(cleanId, setCustomError, forceRefresh = false)
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

    suspend fun resolveFreshStreamUrl(
        videoId: String,
        setCustomError: ((String) -> Unit)? = null
    ): String? {
        streamUrlTimestamps.remove(videoId)
        return resolveOnlineStream(videoId, setCustomError = setCustomError, forceRefresh = true)
    }

    fun cancelPrefetches() {
        innertubeApi.cancelAllPrefetches()
    }

    suspend fun resolveOnlineStream(
        videoId: String,
        setCustomError: ((String) -> Unit)? = null,
        forceRefresh: Boolean = false
    ): String? {
        _isResolving.value = true
        try {
            // 1. Check InnertubeApi cache directly (which already enforces its own TTL)
            if (!forceRefresh) {
                val cached = innertubeApi.getCachedStreamUrl(videoId)
                if (!cached.isNullOrBlank()) {
                    val ts = streamUrlTimestamps[videoId]
                    val expiredByResolver = ts != null && (System.currentTimeMillis() - ts) >= STREAM_URL_TTL_MS
                    if (!expiredByResolver) {
                        if (ts == null) streamUrlTimestamps[videoId] = System.currentTimeMillis()
                        Log.d("PlaybackSourceResolver", "Instant cache hit for $videoId (<1ms)")
                        _currentAudioQuality.value = losslessStreamResolver.inspectQuality(cached, isLocalFile = false)
                        return cached
                    }
                }
            }

            if (!isNetworkAvailable()) {
                Log.e("PlaybackSourceResolver", "Offline, cannot play online song: $videoId")
                setCustomError?.invoke("No internet connection. Cannot stream song.")
                return null
            }

            Log.d("PlaybackSourceResolver", "Resolving stream URL for videoId: $videoId (forceRefresh=$forceRefresh)")
            val streamUrl = withContext(Dispatchers.IO) {
                try {
                    innertubeApi.getStreamUrl(videoId, forceRefresh = forceRefresh)
                } catch (e: Exception) {
                    Log.e("PlaybackSourceResolver", "Failed to resolve stream URL for $videoId", e)
                    null
                }
            }

            if (!streamUrl.isNullOrBlank()) {
                streamUrlTimestamps[videoId] = System.currentTimeMillis()
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(streamUrl, isLocalFile = false)
                return streamUrl
            } else {
                setCustomError?.invoke("Failed to load audio stream. Please check connection.")
                return null
            }
        } finally {
            _isResolving.value = false
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        // ponytail: activeNetwork returns null during Doze/screen-off even when
        // the foreground service still has internet. Assume online and let the
        // actual HTTP call fail if truly offline — ExoPlayer handles IO errors.
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun prefetchVisibleStreams(videoIds: List<String>) {
        innertubeApi.prefetchVisibleStreams(videoIds)
    }

    fun prefetchSongs(requests: List<PlayRequest>) {
        val onlineVideoIds = requests.mapNotNull { req ->
            val uri = req.localUri ?: req.songId
            if (req.playbackSource == PlaybackSourceType.ONLINE || uri.startsWith("online://")) {
                uri.removePrefix("online://")
            } else if (req.songId.matches(Regex("^[a-zA-Z0-9_-]{11}$")) && req.playbackSource != PlaybackSourceType.LOCAL) {
                req.songId
            } else null
        }
        if (onlineVideoIds.isNotEmpty()) {
            innertubeApi.prefetchVisibleStreams(onlineVideoIds)
        }
    }
}
