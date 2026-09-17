package com.example.myplayer.data.artwork.cache

import android.content.Context
import android.util.Log
import com.example.myplayer.data.artwork.model.ArtworkResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class CachedArtworkEntry(
    val url: String?,
    val provider: String?,
    val trackName: String,
    val artistName: String,
    val cachedAt: Long,
    val width: Int = 0,
    val height: Int = 0,
    val notFound: Boolean = false
)

@Singleton
class ArtworkCache @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ArtworkCache"
        private const val MAX_MEMORY_ENTRIES = 500
        private const val POSITIVE_CACHE_TTL_MS = 14L * 24 * 60 * 60 * 1000 // 14 days
        private const val NEGATIVE_CACHE_TTL_MS = 24L * 60 * 60 * 1000      // 24 hours
        private const val CACHE_SUBDIR = "artwork_cache"
        private const val MAX_DISK_FILES = 2000
    }

    private val memoryCache = object : LinkedHashMap<String, CachedArtworkEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedArtworkEntry>?): Boolean {
            return size > MAX_MEMORY_ENTRIES
        }
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_SUBDIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Attempts to retrieve artwork from in-memory LruCache, falling back to disk cache.
     * Returns null if cache miss or expired.
     */
    suspend fun get(cacheKey: String): ArtworkResult? = withContext(Dispatchers.IO) {
        // 1. Memory lookup
        synchronized(memoryCache) {
            val memEntry = memoryCache.get(cacheKey)
            if (memEntry != null) {
                val ttl = if (memEntry.notFound) NEGATIVE_CACHE_TTL_MS else POSITIVE_CACHE_TTL_MS
                if (System.currentTimeMillis() - memEntry.cachedAt <= ttl) {
                    if (memEntry.notFound || memEntry.url.isNullOrBlank()) {
                        return@withContext null // Negative cache hit
                    }
                    return@withContext ArtworkResult(
                        url = memEntry.url,
                        provider = memEntry.provider ?: "Cache",
                        width = memEntry.width,
                        height = memEntry.height
                    )
                } else {
                    memoryCache.remove(cacheKey)
                }
            }
        }

        // 2. Disk lookup
        val file = File(cacheDir, "${cacheKey}.json")
        if (!file.exists()) return@withContext null

        try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            val cachedAt = json.optLong("cachedAt", 0L)
            val notFound = json.optBoolean("notFound", false)
            val ttl = if (notFound) NEGATIVE_CACHE_TTL_MS else POSITIVE_CACHE_TTL_MS

            if (System.currentTimeMillis() - cachedAt > ttl) {
                file.delete()
                return@withContext null
            }

            val url = json.optString("url", "").takeIf { it.isNotBlank() }
            val provider = json.optString("provider", "Cache")
            val trackName = json.optString("trackName", "")
            val artistName = json.optString("artistName", "")
            val width = json.optInt("width", 0)
            val height = json.optInt("height", 0)

            val entry = CachedArtworkEntry(
                url = url,
                provider = provider,
                trackName = trackName,
                artistName = artistName,
                cachedAt = cachedAt,
                width = width,
                height = height,
                notFound = notFound
            )

            synchronized(memoryCache) {
                memoryCache.put(cacheKey, entry)
            }

            if (notFound || url == null) {
                return@withContext null
            }

            return@withContext ArtworkResult(
                url = url,
                provider = provider,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading artwork cache for key $cacheKey: ${e.message}")
            file.delete()
            return@withContext null
        }
    }

    /**
     * Checks if this key is currently recorded as a negative cache hit (i.e. not found recently).
     */
    suspend fun isNegativeCached(cacheKey: String): Boolean = withContext(Dispatchers.IO) {
        synchronized(memoryCache) {
            val mem = memoryCache.get(cacheKey)
            if (mem != null && mem.notFound) {
                if (System.currentTimeMillis() - mem.cachedAt <= NEGATIVE_CACHE_TTL_MS) {
                    return@withContext true
                }
            }
        }
        val file = File(cacheDir, "${cacheKey}.json")
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                if (json.optBoolean("notFound", false)) {
                    val cachedAt = json.optLong("cachedAt", 0L)
                    if (System.currentTimeMillis() - cachedAt <= NEGATIVE_CACHE_TTL_MS) {
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return@withContext false
    }

    /**
     * Saves a successfully resolved artwork to both memory and persistent disk cache.
     */
    suspend fun put(
        cacheKey: String,
        result: ArtworkResult,
        trackName: String,
        artistName: String
    ) = withContext(Dispatchers.IO) {
        val entry = CachedArtworkEntry(
            url = result.url,
            provider = result.provider,
            trackName = trackName,
            artistName = artistName,
            cachedAt = System.currentTimeMillis(),
            width = result.width,
            height = result.height,
            notFound = false
        )

        synchronized(memoryCache) {
            memoryCache.put(cacheKey, entry)
        }

        writeDiskEntry(cacheKey, entry)
    }

    /**
     * Records a negative cache entry so providers are not repeatedly spammed for unresolvable tracks.
     */
    suspend fun putNotFound(
        cacheKey: String,
        trackName: String,
        artistName: String
    ) = withContext(Dispatchers.IO) {
        val entry = CachedArtworkEntry(
            url = null,
            provider = null,
            trackName = trackName,
            artistName = artistName,
            cachedAt = System.currentTimeMillis(),
            notFound = true
        )

        synchronized(memoryCache) {
            memoryCache.put(cacheKey, entry)
        }

        writeDiskEntry(cacheKey, entry)
    }

    private fun writeDiskEntry(cacheKey: String, entry: CachedArtworkEntry) {
        try {
            val json = JSONObject().apply {
                put("url", entry.url ?: "")
                put("provider", entry.provider ?: "")
                put("trackName", entry.trackName)
                put("artistName", entry.artistName)
                put("cachedAt", entry.cachedAt)
                put("width", entry.width)
                put("height", entry.height)
                put("notFound", entry.notFound)
            }

            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, "${cacheKey}.json")
            file.writeText(json.toString(), Charsets.UTF_8)

            pruneDiskIfNecessary()
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing artwork cache to disk for $cacheKey: ${e.message}")
        }
    }

    private fun pruneDiskIfNecessary() {
        try {
            val files = cacheDir.listFiles() ?: return
            if (files.size > MAX_DISK_FILES) {
                // Sort by lastModified and delete oldest 25%
                files.sortedBy { it.lastModified() }
                    .take(files.size - (MAX_DISK_FILES * 3 / 4))
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            // Prune error is non-fatal
        }
    }

    /**
     * Clears in-memory and disk caches.
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(memoryCache) {
            memoryCache.clear()
        }
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing disk cache: ${e.message}")
        }
    }
}
