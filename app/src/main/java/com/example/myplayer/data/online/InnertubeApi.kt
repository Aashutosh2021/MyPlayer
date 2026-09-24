package com.example.myplayer.data.online

import android.util.Log
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.security.StringEncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

class YouTubeStreamBlockedException(message: String) : Exception(message)

/**
 * Implements the YouTube Music Innertube API protocol.
 * This is the same API used by NewPipe, yt-dlp, and music.youtube.com.
 * No API key is required — uses the public web client credentials.
 */
@Singleton
class InnertubeApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val enc: StringEncryptionManager  // Phase 3: runtime key decryption
) {

    init {
        // Initialize NewPipe exactly once with the Hilt-injected OkHttpClient.
        // This MUST happen here (not in Application.onCreate) so that the same
        // singleton client is used by both InnertubeApi and NewPipe internals.
        if (newPipeInitialized.compareAndSet(false, true)) {
            NewPipe.init(NewPipeDownloader(okHttpClient))
            Log.d(TAG, "NewPipe initialized with injected OkHttpClient")
        }
    }
    companion object {
        private const val TAG = "InnertubeApi"
        private val newPipeInitialized = AtomicBoolean(false)
        // BASE_URL and API_KEY are no longer hardcoded constants.
        // They are retrieved at runtime from StringEncryptionManager (Phase 2/3).
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    }

    // Lazy-resolved via enc to avoid plaintext in DEX string table
    private val BASE_URL: String by lazy { enc.baseUrl }
    private val API_KEY: String by lazy { enc.ytmApiKey }

    // YouTube Music web client context
    private val CLIENT_CONTEXT: String by lazy {
        JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", enc.clientWebName)
                    put("clientVersion", enc.clientWebVersion)
                    put("hl", "en")
                    put("gl", "US")
                })
            })
        }.toString()
    }

    // YouTube VR client context to fetch direct, deciphered streaming URLs
    private val PLAY_CLIENT_CONTEXT: String by lazy {
        JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", enc.clientVrName)
                    put("clientVersion", "1.65.10")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
        }.toString()
    }

    data class SearchPage(
        val songs: List<OnlineSong>,
        val continuationToken: String?
    )

    /**
     * Search YouTube Music for songs matching [query].
     * @param continuationToken Pass the previous page's token to load more results.
     */
    suspend fun search(query: String, continuationToken: String? = null): SearchPage = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject(CLIENT_CONTEXT).apply {
                if (continuationToken != null) {
                    put("continuation", continuationToken)
                } else {
                    put("query", query)
                    put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D") // filter: songs only
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL/search?key=$API_KEY&prettyPrint=false")
                .post(bodyJson.toString().toRequestBody(MEDIA_TYPE_JSON))
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("Referer", "https://music.youtube.com/")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext SearchPage(emptyList(), null)

            parseSearchResponse(body)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for '$query'", e)
            SearchPage(emptyList(), null)
        }
    }

    private fun parseSearchResponse(json: String): SearchPage {
        val songs = mutableListOf<OnlineSong>()
        var continuationToken: String? = null

        try {
            val root = JSONObject(json)

            // Try to extract continuation token
            continuationToken = root
                .optJSONObject("continuationContents")
                ?.optJSONObject("musicShelfContinuation")
                ?.optJSONArray("continuations")
                ?.optJSONObject(0)
                ?.optJSONObject("nextContinuationData")
                ?.optString("continuation")
                ?: root
                    .optJSONObject("contents")
                    ?.optJSONObject("tabbedSearchResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("continuations")
                    ?.optJSONObject(0)
                    ?.optJSONObject("nextContinuationData")
                    ?.optString("continuation")

            // Find musicShelfRenderer contents
            val contents = findMusicShelfContents(root) ?: return SearchPage(songs, null)

            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue
                val renderer = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                val song = parseMusicItem(renderer) ?: continue
                songs.add(song)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed", e)
        }

        return SearchPage(songs, continuationToken?.takeIf { it.isNotBlank() })
    }

    private fun findMusicShelfContents(root: JSONObject): JSONArray? {
        // Normal search response path
        val tabs = root
            .optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")

        if (tabs != null) {
            for (i in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
                val sections = tab.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: continue

                for (j in 0 until sections.length()) {
                    val shelf = sections.optJSONObject(j)
                        ?.optJSONObject("musicShelfRenderer") ?: continue
                    val shelfContents = shelf.optJSONArray("contents")
                    if (shelfContents != null && shelfContents.length() > 0) {
                        return shelfContents
                    }
                }
            }
        }

        // Continuation response path
        return root
            .optJSONObject("continuationContents")
            ?.optJSONObject("musicShelfContinuation")
            ?.optJSONArray("contents")
    }

    private fun parseMusicItem(renderer: JSONObject): OnlineSong? {
        return try {
            // Extract video ID from navigation endpoint
            val videoId = renderer
                .optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?: renderer
                    .optJSONObject("flexColumns")
                    ?.let { null } // try from flex columns below
                ?: return null

            if (videoId.isBlank()) return null

            // Extract title and artist from flexColumns
            val flexColumns = renderer.optJSONArray("flexColumns") ?: return null

            val title = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: return null

            // Artist is usually in the second flex column
            val subtitleRuns = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            val artist = subtitleRuns?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
            val durationText = subtitleRuns?.let { runs ->
                // Duration is usually the last run
                var d = ""
                for (k in runs.length() - 1 downTo 0) {
                    val text = runs.optJSONObject(k)?.optString("text") ?: continue
                    if (text.contains(":")) { d = text; break }
                }
                d
            } ?: ""

            val durationMs = parseDurationToMs(durationText)

            // Get highest quality thumbnail
            val thumbnails = renderer
                .optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")

            val rawThumbnailUrl = if (thumbnails != null && thumbnails.length() > 0) {
                thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") ?: ""
            } else ""

            val highResThumbnailUrl = when {
                rawThumbnailUrl.contains("googleusercontent.com") -> {
                    val googleDimRegex = Regex("""=w\d+-h\d+[^=]*$""")
                    if (googleDimRegex.containsMatchIn(rawThumbnailUrl)) {
                        rawThumbnailUrl.replace(googleDimRegex, "=w800-h800-l90-rj")
                    } else if (rawThumbnailUrl.contains("=")) {
                        rawThumbnailUrl.substringBeforeLast("=") + "=w800-h800-l90-rj"
                    } else {
                        "$rawThumbnailUrl=w800-h800-l90-rj"
                    }
                }
                rawThumbnailUrl.isNotBlank() -> {
                    rawThumbnailUrl.replace(Regex("""w\d+-h\d+"""), "w800-h800")
                }
                else -> ""
            }

            OnlineSong(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = highResThumbnailUrl,
                durationMs = durationMs,
                durationText = durationText
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse music item", e)
            null
        }
    }

    data class CachedStream(
        val url: String,
        val expiryTimeMs: Long
    )

    private val streamUrlCache = java.util.concurrent.ConcurrentHashMap<String, CachedStream>()
    private val CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours

    /**
     * Returns the cached stream URL if available and not expired.
     */
    fun getCachedStreamUrl(videoId: String): String? {
        val cached = streamUrlCache[videoId] ?: return null
        if (System.currentTimeMillis() < cached.expiryTimeMs) {
            return cached.url
        }
        streamUrlCache.remove(videoId)
        return null
    }

    /**
     * Caches a stream URL for a given video ID with standard TTL.
     */
    fun cacheStreamUrl(videoId: String, url: String) {
        if (videoId.isNotBlank() && url.isNotBlank()) {
            streamUrlCache[videoId] = CachedStream(url, System.currentTimeMillis() + CACHE_TTL_MS)
        }
    }

    /**
     * Resolves the actual audio stream URL for a given YouTube Music video ID.
     * Uses NewPipeExtractor to bypass bot detection and poToken issues.
     */
    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        getCachedStreamUrl(videoId)?.let { cachedUrl ->
            Log.i(TAG, "getStreamUrl: Returning cached stream URL for $videoId (<1ms)")
            return@withContext cachedUrl
        }

        try {
            Log.i(TAG, "getStreamUrl: Starting resolution for $videoId")
            val url = "https://www.youtube.com/watch?v=$videoId"
            Log.i(TAG, "getStreamUrl: Creating YouTube stream extractor for $url")
            val extractor = org.schabi.newpipe.extractor.ServiceList.YouTube.getStreamExtractor(url)
            Log.i(TAG, "getStreamUrl: Calling extractor.fetchPage() (this might block/hang)...")
            extractor.fetchPage()
            Log.i(TAG, "getStreamUrl: extractor.fetchPage() completed successfully")

            val audioStreams = extractor.audioStreams
            Log.i(TAG, "getStreamUrl: Found ${audioStreams.size} audio streams")
            if (audioStreams.isEmpty()) {
                throw YouTubeStreamBlockedException("YouTube stream extraction failed: No audio streams found.")
            }

            // Prefer highest bitrate
            val bestStream = audioStreams.maxByOrNull { it.getBitrate() }
            val contentUrl = bestStream?.getContent()
            Log.i(TAG, "getStreamUrl: Selected stream URL: ${contentUrl?.take(80)}...")
            if (!contentUrl.isNullOrBlank()) {
                cacheStreamUrl(videoId, contentUrl)
            }
            contentUrl
        } catch (e: org.schabi.newpipe.extractor.exceptions.ExtractionException) {
            Log.e(TAG, "NewPipe Extraction failed", e)
            throw YouTubeStreamBlockedException("YouTube blocked this stream: ${e.message}")
        } catch (e: YouTubeStreamBlockedException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getStreamUrl failed for $videoId", e)
            null
        }
    }

    private fun parseDurationToMs(durationText: String): Long {
        if (durationText.isBlank()) return 0L
        return try {
            val parts = durationText.split(":").map { it.toLong() }
            when (parts.size) {
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                2 -> (parts[0] * 60 + parts[1]) * 1000
                1 -> parts[0] * 1000
                else -> 0L
            }
        } catch (e: Exception) { 0L }
    }
}
