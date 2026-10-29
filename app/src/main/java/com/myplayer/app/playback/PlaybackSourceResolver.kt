package com.myplayer.app.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.myplayer.app.data.local.dao.DownloadedSongDao
import com.myplayer.app.data.local.dao.SongDao
import com.myplayer.app.data.online.InnertubeApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.myplayer.app.data.preferences.AudioSettingsRepository
import com.myplayer.app.data.preferences.StreamQuality
import kotlinx.coroutines.flow.firstOrNull
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

    // 5-hour TTL: YouTube InnerTube stream URLs expire after ~6 hours
    private val streamUrlTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val STREAM_URL_TTL_MS = 5 * 60 * 60 * 1000L

    // Spotify-imported Liked Songs / Playlist tracks carry 22-char Spotify IDs.
    // SharedPreferences + in-memory cache ensure a match is permanently remembered across app restarts.
    private val prefs by lazy { context.getSharedPreferences("spotify_yt_id_mapping", Context.MODE_PRIVATE) }
    private val spotifyToYoutubeIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activePrefetchJob: Job? = null

    suspend fun resolve(
        request: PlayRequest,
        setCustomError: ((String) -> Unit)? = null
    ): String? {
        val localUri = request.localUri

        // 0. ULTRA-FAST PATH (<1ms): Check in-memory stream URL cache FIRST!
        val quickId = (localUri?.removePrefix("online://") ?: request.songId)
            .removePrefix("online://")
            .removePrefix("spotify:track:")
            .removePrefix("spotify:")
            .trim()
        val mappedQuickId = spotifyToYoutubeIdCache[quickId] ?: prefs.getString(quickId, null) ?: quickId
        if (mappedQuickId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            val fastCachedUrl = innertubeApi.getCachedStreamUrl(mappedQuickId)
            if (!fastCachedUrl.isNullOrBlank()) {
                Log.d("PlaybackSourceResolver", "0ms instant cache hit for $mappedQuickId")
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(fastCachedUrl, isLocalFile = false)
                return fastCachedUrl
            }
        }

        // 1. Check if song is downloaded locally
        val downloaded = withContext(Dispatchers.IO) {
            try {
                downloadedSongDao.getById(request.songId) ?: downloadedSongDao.getById(mappedQuickId)
            } catch (_: Exception) { null }
        }
        if (downloaded != null) {
            val file = File(downloaded.localPath)
            if (file.exists() && file.length() > 0L) {
                _currentAudioQuality.value = losslessStreamResolver.inspectQuality(downloaded.localPath, isLocalFile = true)
                return downloaded.localPath
            }
        }

        // 2. Pure local device audio file
        if (request.playbackSource == PlaybackSourceType.LOCAL && !localUri.isNullOrBlank()) {
            if (localUri.startsWith("content://") || localUri.startsWith("file://") || localUri.startsWith("/")) {
                val fileExists = checkLocalFileExists(localUri)
                if (fileExists) {
                    _currentAudioQuality.value = losslessStreamResolver.inspectQuality(localUri, isLocalFile = true)
                    return localUri
                }
            }
        }

        // 3. Direct HTTP URL
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

        // 4. If cleanId is still a 22-char Spotify ID, match to 11-char YouTube ID AND save permanently to DB!
        val isYouTubeId = cleanId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))
        if (!isYouTubeId) {
            val dbSong = withContext(Dispatchers.IO) {
                try {
                    songDao.getSongById(request.songId)
                } catch (_: Exception) { null }
            }
            val existingYtId = dbSong?.videoId?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
            if (existingYtId != null) {
                spotifyToYoutubeIdCache[quickId] = existingYtId
                prefs.edit().putString(quickId, existingYtId).apply()
                cleanId = existingYtId
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
                        // Permanently save the 11-char YouTube ID into Room DB so it NEVER needs searching again!
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

            val config = audioSettingsRepository.configFlow.firstOrNull()
            val targetQuality = if (isWifiConnected()) {
                config?.wifiQuality ?: StreamQuality.HIGH
            } else {
                config?.mobileQuality ?: StreamQuality.MEDIUM
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

    /**
     * Prefetches upcoming songs in the queue (including 22-char Spotify Playlist songs!)
     * so pressing Next (>|) or tapping upcoming songs plays in 0ms!
     */
    fun prefetchSongs(requests: List<PlayRequest>) {
        if (requests.isEmpty()) return
        activePrefetchJob?.cancel()
        activePrefetchJob = prefetchScope.launch {
            for (req in requests.take(3)) {
                if (!isActive) break
                try {
                    // Calling resolve(req) converts 22-char Spotify IDs to 11-char YT IDs
                    // AND caches their googlevideo.com stream URLs in the background!
                    resolve(req)
                } catch (_: Exception) {}
            }
        }
    }
}
