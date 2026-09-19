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
        // 1. Local artwork first (as per design spec: Local artwork/cache -> Deezer -> iTunes -> YouTube Music)
        if (!localArtworkUri.isNullOrBlank()) {
            return ArtworkResult(url = localArtworkUri, provider = "Local", width = 0, height = 0)
        }

        val safeTitle = title.orEmpty()
        val safeArtist = artist.orEmpty()
        val cleanArtist = matcher.cleanArtist(safeArtist)
        val cleanTitle = matcher.cleanTitle(safeTitle, cleanArtist)
        if (cleanTitle.isBlank() && cleanArtist.isBlank()) {
            return null
        }

        val cacheKey = matcher.createStableCacheKey(safeArtist, safeTitle)

        // 2. Check positive cache
        cache.get(cacheKey)?.let { return it }

        // 3. Check negative cache
        if (cache.isNegativeCached(cacheKey)) {
            return null
        }

        // 4. Deduplicate in-flight requests
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

        return result
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
