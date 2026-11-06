package com.myplayer.app.data.artwork

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.myplayer.app.data.artwork.cache.ArtworkCache
import com.myplayer.app.data.artwork.matching.ArtworkMatcher
import com.myplayer.app.data.artwork.model.ArtworkResult
import com.myplayer.app.data.artwork.provider.DeezerArtworkProvider
import com.myplayer.app.data.artwork.provider.ITunesArtworkProvider
import com.myplayer.app.data.artwork.provider.YouTubeMusicArtworkProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cache: ArtworkCache,
    private val matcher: ArtworkMatcher,
    private val deezerProvider: DeezerArtworkProvider,
    private val iTunesProvider: ITunesArtworkProvider,
    private val youTubeMusicProvider: YouTubeMusicArtworkProvider
) {
    companion object {
        private const val TAG = "ArtworkRepository"
    }

    private val providers = listOf(
        deezerProvider,
        iTunesProvider,
        youTubeMusicProvider
    ).sortedBy { it.priority }

    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ArtworkResult?>>()
    private val prefetchSemaphore = Semaphore(4)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun getArtwork(
        title: String?,
        artist: String?,
        album: String? = null,
        localArtworkUri: String? = null
    ): ArtworkResult? {
        val safeTitle = title.orEmpty().trim()
        val safeArtist = artist.orEmpty().trim()
        val cleanArtist = matcher.cleanArtist(safeArtist)
        val cleanTitle = matcher.cleanTitle(safeTitle, cleanArtist)

        // 0. If localArtworkUri is a local file/content URI, return it immediately (works 100% offline!)
        if (!localArtworkUri.isNullOrBlank()) {
            if (localArtworkUri.startsWith("file://") ||
                localArtworkUri.startsWith("content://") ||
                localArtworkUri.startsWith("/")
            ) {
                val normalized = if (localArtworkUri.startsWith("/")) {
                    Uri.fromFile(File(localArtworkUri)).toString()
                } else {
                    localArtworkUri
                }
                return ArtworkResult(url = normalized, provider = "LocalFile", width = 0, height = 0)
            }

            // Check if a downloaded local poster exists for this YouTube thumbnail URL
            extractYoutubeIdFromThumbnail(localArtworkUri)?.let { ytId ->
                val localArt = File(File(context.filesDir, "artwork"), "$ytId.jpg")
                if (localArt.exists() && localArt.length() > 512L) {
                    return ArtworkResult(
                        url = Uri.fromFile(localArt).toString(),
                        provider = "DownloadedArt",
                        width = 0,
                        height = 0
                    )
                }
            }

            // If online or already in Coil cache, return direct URL without changing cache key when offline
            val finalUrl = if (isOnline()) upgradeThumbnailIfNeeded(localArtworkUri) else localArtworkUri
            return ArtworkResult(
                url = finalUrl,
                provider = "Direct",
                width = 0,
                height = 0
            )
        }

        if (cleanTitle.isBlank() && cleanArtist.isBlank()) {
            return null
        }

        val cacheKey = matcher.createStableCacheKey(safeArtist, safeTitle)

        // 1. Check positive cache (<1ms response)
        cache.get(cacheKey)?.let { return it }

        // 2. If offline, do not spam network providers or write negative cache entries
        if (!isOnline()) {
            return null
        }

        // 3. Check negative cache
        if (cache.isNegativeCached(cacheKey)) {
            return null
        }

        // 4. Deduplicate in-flight requests and query external providers
        val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
            scope.async {
                fetchFromProviders(cleanTitle, cleanArtist, album, cacheKey)
            }
        }

        return try {
            deferred.await()
        } finally {
            inFlightRequests.remove(cacheKey)
        }
    }

    private fun extractYoutubeIdFromThumbnail(url: String): String? {
        val match = Regex("""/vi/([a-zA-Z0-9_-]{11})/""").find(url)
        return match?.groupValues?.getOrNull(1)
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun upgradeThumbnailIfNeeded(url: String): String {
        return when {
            url.contains("googleusercontent.com") -> {
                val googleDimRegex = Regex("""=w\d+-h\d+[^=]*$""")
                if (googleDimRegex.containsMatchIn(url)) {
                    url.replace(googleDimRegex, "=w800-h800-l90-rj")
                } else if (url.contains("=")) {
                    url.substringBeforeLast("=") + "=w800-h800-l90-rj"
                } else {
                    "$url=w800-h800-l90-rj"
                }
            }
            url.contains("i.ytimg.com") -> {
                url.replace(Regex("""/hqdefault\.jpg$"""), "/hq720.jpg")
            }
            else -> url.replace(Regex("""w\d+-h\d+"""), "w800-h800")
        }
    }

    private suspend fun fetchFromProviders(
        title: String,
        artist: String,
        album: String?,
        cacheKey: String
    ): ArtworkResult? {
        for (provider in providers) {
            try {
                val result = provider.fetchArtwork(title, artist, album)
                if (result != null && !result.url.isNullOrBlank()) {
                    cache.put(cacheKey, result, title, artist)
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Provider ${provider.name} failed for $artist - $title: ${e.message}")
            }
        }
        if (isOnline()) {
            cache.putNotFound(cacheKey, title, artist)
        }
        return null
    }

    suspend fun prefetchBatch(items: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext emptyList()
        items.map { (title, artist) ->
            async {
                prefetchSemaphore.withPermit {
                    getArtwork(title, artist)
                }
            }
        }.awaitAll()
    }
}
