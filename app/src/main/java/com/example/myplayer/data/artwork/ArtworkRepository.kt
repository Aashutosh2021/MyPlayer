package com.example.myplayer.data.artwork

import android.util.Log
import com.example.myplayer.data.artwork.cache.ArtworkCache
import com.example.myplayer.data.artwork.matching.ArtworkMatcher
import com.example.myplayer.data.artwork.model.ArtworkResult
import com.example.myplayer.data.artwork.provider.DeezerArtworkProvider
import com.example.myplayer.data.artwork.provider.ITunesArtworkProvider
import com.example.myplayer.data.artwork.provider.YouTubeMusicArtworkProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkRepository @Inject constructor(
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

        // If both title and artist are missing, return local/fallback URI immediately
        if (cleanTitle.isBlank() && cleanArtist.isBlank()) {
            return localArtworkUri?.takeIf { it.isNotBlank() }?.let {
                ArtworkResult(url = upgradeThumbnailIfNeeded(it), provider = "Local", width = 0, height = 0)
            }
        }

        val cacheKey = matcher.createStableCacheKey(safeArtist, safeTitle)

        // 1. Check positive cache (<1ms response)
        cache.get(cacheKey)?.let { return it }

        // 2. Check negative cache
        if (cache.isNegativeCached(cacheKey)) {
            return localArtworkUri?.takeIf { it.isNotBlank() }?.let {
                ArtworkResult(url = upgradeThumbnailIfNeeded(it), provider = "Local", width = 0, height = 0)
            }
        }

        // 3. Deduplicate in-flight requests and query external providers (Deezer -> iTunes -> YouTube Music)
        val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
            scope.async {
                fetchFromProviders(cleanTitle, cleanArtist, album, cacheKey)
            }
        }

        val result = try {
            deferred.await()
        } finally {
            inFlightRequests.remove(cacheKey)
        }

        // 4. Return HD result or fall back to local/fallback URI
        return result ?: localArtworkUri?.takeIf { it.isNotBlank() }?.let {
            ArtworkResult(url = it, provider = "Local", width = 0, height = 0)
        }
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
        // Negative cache when all fail
        cache.putNotFound(cacheKey, title, artist)
        return null
    }

    suspend fun prefetchBatch(items: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        items.map { (title, artist) ->
            async {
                prefetchSemaphore.withPermit {
                    getArtwork(title, artist)
                }
            }
        }.awaitAll()
    }
}
