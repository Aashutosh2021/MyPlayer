package com.myplayer.app.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.myplayer.app.data.local.dao.DownloadedSongDao
import com.myplayer.app.data.local.dao.SongDao
import com.myplayer.app.data.online.InnertubeApi
import com.myplayer.app.data.preferences.AudioSettingsRepository
import com.myplayer.app.data.preferences.StreamQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val losslessStreamResolver: LosslessStreamResolver,
    private val audioSettingsRepository: AudioSettingsRepository
) {
    private val _currentAudioQuality = MutableStateFlow(AudioQualityInfo())
    val currentAudioQuality: StateFlow<AudioQualityInfo> = _currentAudioQuality.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val streamUrlTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val STREAM_URL_TTL_MS = 5 * 60 * 60 * 1000L

    private val prefs by lazy { context.getSharedPreferences("spotify_yt_id_mapping", Context.MODE_PRIVATE) }
    private val spotifyToYoutubeIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activePrefetchJob: Job? = null

    suspend fun resolve(
        request: PlayRequest,
        setCustomError: ((String) -> Unit)? = null
    ): String? {
        val localUri = request.localUri
        val quickId = (localUri?.removePrefix("online://") ?: request.songId)
            .removePrefix("online://")
            .removePrefix("spotify:track:")
            .removePrefix("spotify:")
            .trim()
        val mappedQuickId = spotifyToYoutubeIdCache[quickId] ?: prefs.getString(quickId, null) ?: quickId

        // 1. ALWAYS check if song is downloaded locally FIRST!
        // Fixes the bug where an online cached URL was returned even when the user was offline and had the song downloaded.
        val downloaded = withContext(Dispatchers.IO) {
            try {
                downloadedSongDao.getById(request.songId)
                    ?: downloadedSongDao.getById(quickId)
                    ?: downloadedSongDao.getById(mappedQuickId)
            } catch (_: Exception) { null }
        }
        if (downloaded != null && checkLocalFileExists(downloaded.localPath)) {
            Log.d("PlaybackSourceResolver", "Playing from local downloaded file: ${downloaded.localPath}")
            _currentAudioQuality.value = losslessStreamResolver.inspectQuality(downloaded.localPath, isLocalFile = true)
            return downloaded.localPath
        }

        // 2. Pure local device audio file
        if (!localUri.isNullOrBlank() &&
            (localUri.startsWith("content://") || localUri.startsWith("file://") || localUri.startsWith("/"))
        ) {
            if (checkLocalFileExists(localUri)) {
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(localUri, isLocalFile = true)
                return localUri
            }
        }

        // 3. Fast in-memory stream URL cache (<1ms) when network is available
        if (isNetworkAvailable() && mappedQuickId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            val fastCachedUrl = innertubeApi.getCachedStreamUrl(mappedQuickId)
            if (!fastCachedUrl.isNullOrBlank()) {
                Log.d("PlaybackSourceResolver", "0ms instant cache hit for $mappedQuickId")
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(fastCachedUrl, isLocalFile = false)
                return fastCachedUrl
            }
        }

        // 4. Direct HTTP URL
        if (!request.streamUrl.isNullOrBlank() && request.streamUrl.startsWith("http")) {
            _currentAudioQuality.value = losslessStreamResolver.inspectQuality(request.streamUrl, isLocalFile = false)
            return request.streamUrl
        } else if (!localUri.isNullOrBlank() && (localUri.startsWith("http://") || localUri.startsWith("https://"))) {
            _currentAudioQuality.value = losslessStreamResolver.inspectQuality(localUri, isLocalFile = false)
            return localUri
        }

        var cleanId = mappedQuickId
        if (cleanId.isBlank()) {
            setCustomError?.invoke("Invalid track ID")
            return null
        }

        // 5. If cleanId is a 22-char Spotify ID, map to 11-char YouTube ID
        val isYouTubeId = cleanId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))
        if (!isYouTubeId) {
            val dbSong = withContext(Dispatchers.IO) {
                try { songDao.getSongById(request.songId) } catch (_: Exception) { null }
            }
            val existingYtId = dbSong?.videoId?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
            if (existingYtId != null) {
                spotifyToYoutubeIdCache[quickId] = existingYtId
                prefs.edit().putString(quickId, existingYtId).apply()
                cleanId = existingYtId

                // Also check if this mapped YouTube ID is downloaded locally!
                val mappedDownload = withContext(Dispatchers.IO) {
                    try { downloadedSongDao.getById(existingYtId) } catch (_: Exception) { null }
                }
                if (mappedDownload != null && checkLocalFileExists(mappedDownload.localPath)) {
                    _currentAudioQuality.value = losslessStreamResolver.inspectQuality(mappedDownload.localPath, isLocalFile = true)
                    return mappedDownload.localPath
                }
            } else {
                val qTitle = request.title.ifBlank { dbSong?.title ?: request.metadata["title"] ?: "" }
                val qArtist = request.artist.ifBlank { dbSong?.artist ?: request.metadata["artist"] ?: "" }
                if (qTitle.isNotBlank()) {
                    val matchedYtId = withContext(Dispatchers.IO) {
                        try {
                            innertubeApi.search("$qTitle $qArtist".trim(), filtered = true)
                                .songs.firstOrNull()?.videoId
                        } catch (e: Exception) {
                            Log.w("PlaybackSourceResolver", "Spotify->YouTube match failed for $cleanId", e)
                            null
                        }
                    }
                    if (!matchedYtId.isNullOrBlank()) {
                        spotifyToYoutubeIdCache[quickId] = matchedYtId
                        prefs.edit().putString(quickId, matchedYtId).apply()
                        cleanId = matchedYtId
                        prefetchScope.launch {
                            try {
                                songDao.updateSongVideoId(request.songId, matchedYtId)
                                songDao.updateSongPath(request.songId, "online://$matchedYtId")
                                if (dbSong != null) {
                                    songDao.insertSongs(
                                        listOf(dbSong.copy(videoId = matchedYtId, path = "online://$matchedYtId"))
                                    )
                                }
                            } catch (_: Exception) {}
                        }
                    } else {
                        setCustomError?.invoke("Could not find stream for '$qTitle'")
                        return null
                    }
                } else {
                    setCustomError?.invoke("Could not find stream for '${request.songId}'")
                    return null
                }
            }
        }

        return resolveOnlineStream(cleanId, setCustomError, forceRefresh = false)
    }

    private suspend fun checkLocalFileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                val uri = Uri.parse(filePath)
                if (uri.scheme == "file") {
                    val f = File(uri.path ?: return@withContext false)
                    return@withContext f.exists() && f.length() > 0L
                }
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length > 0
                } ?: false
            } else {
                val file = File(filePath)
                file.exists() && file.length() > 0L
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
        activePrefetchJob?.cancel()
        innertubeApi.cancelAllPrefetches()
    }

    suspend fun resolveOnlineStream(
        videoId: String,
        setCustomError: ((String) -> Unit)? = null,
        forceRefresh: Boolean = false
    ): String? {
        _isResolving.value = true
        try {
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

            val config = audioSettingsRepository.configFlow.firstOrNull()
            // Automatically step down quality on slow cellular connections so 9s chunks load in <0.5s
            val targetQuality = when {
                isSlowConnection() -> StreamQuality.LOW
                isWifiConnected() -> config?.wifiQuality ?: StreamQuality.HIGH
                else -> config?.mobileQuality ?: StreamQuality.MEDIUM
            }

            Log.d("PlaybackSourceResolver", "Resolving stream URL for videoId: $videoId (quality=${targetQuality.name}, forceRefresh=$forceRefresh)")
            val streamUrl = withContext(Dispatchers.IO) {
                try {
                    innertubeApi.getStreamUrl(videoId, targetQuality = targetQuality, forceRefresh = forceRefresh)
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

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Detects slow or congested networks (< 1500 kbps downstream bandwidth estimate)
     * so we can pick lighter Opus audio streams that buffer 9s chunks effortlessly.
     */
    fun isSlowConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        val downKbps = caps.linkDownstreamBandwidthKbps
        return downKbps in 1..1500
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun prefetchVisibleStreams(videoIds: List<String>) {
        innertubeApi.prefetchVisibleStreams(videoIds)
    }

    fun prefetchSongs(requests: List<PlayRequest>) {
        if (requests.isEmpty()) return
        activePrefetchJob?.cancel()
        activePrefetchJob = prefetchScope.launch {
            for (req in requests.take(3)) {
                if (!isActive) break
                try {
                    resolve(req)
                } catch (_: Exception) {}
            }
        }
    }
}
