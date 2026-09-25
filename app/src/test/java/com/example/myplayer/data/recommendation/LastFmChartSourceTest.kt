package com.example.myplayer.data.recommendation

import com.example.myplayer.data.recommendation.lastfm.LastFmCache
import com.example.myplayer.data.recommendation.lastfm.LastFmChartSource
import com.example.myplayer.data.recommendation.lastfm.LastFmConfig
import com.example.myplayer.data.recommendation.lastfm.LastFmTrackMetadata
import com.example.myplayer.security.StringEncryptionManager
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LastFmChartSourceTest {

    private lateinit var cache: LastFmCache
    private lateinit var config: LastFmConfig

    @Before
    fun setup() {
        cache = LastFmCache()
        config = LastFmConfig(StringEncryptionManager())
    }

    @Test
    fun testCachePutAndGet() {
        val tracks = listOf(
            LastFmTrackMetadata(name = "Blinding Lights", artist = "The Weeknd", playcount = 1000L),
            LastFmTrackMetadata(name = "Shape of You", artist = "Ed Sheeran", playcount = 900L)
        )
        cache.put("global_top_tracks", tracks)

        val retrieved = cache.get("global_top_tracks")
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.size)
        assertEquals("Blinding Lights", retrieved?.get(0)?.name)
    }

    @Test
    fun testStaleCacheFallback_ReturnsPreviousData() {
        val tracks = listOf(
            LastFmTrackMetadata(name = "Stay", artist = "The Kid LAROI", playcount = 800L)
        )
        cache.put("global_top_tracks", tracks)

        // Stale retrieval returns cached data even when network is offline
        val stale = cache.getStale("global_top_tracks")
        assertNotNull(stale)
        assertEquals(1, stale?.size)
        assertEquals("Stay", stale?.get(0)?.name)
    }

    @Test
    fun testParseChartTracks_ValidJson() = runBlocking {
        val sampleJson = """
            {
              "tracks": {
                "track": [
                  {
                    "name": "Levitating",
                    "duration": "203",
                    "playcount": "54321",
                    "listeners": "12345",
                    "artist": {
                      "name": "Dua Lipa"
                    },
                    "image": [
                      {"#text": "https://example.com/small.jpg", "size": "small"},
                      {"#text": "https://example.com/large.jpg", "size": "extralarge"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val mockClient = createMockOkHttpClient(responseCode = 200, responseBody = sampleJson)
        val source = LastFmChartSource(mockClient, config, cache)

        val tracks = source.getTopTracks(limit = 10)
        assertEquals(1, tracks.size)
        assertEquals("Levitating", tracks[0].name)
        assertEquals("Dua Lipa", tracks[0].artist)
        assertEquals("https://example.com/large.jpg", tracks[0].imageUrl)
    }

    @Test
    fun testParseChartTracks_RateLimitError_FallsBackToStaleCache() = runBlocking {
        // Seed stale cache
        val initialTracks = listOf(
            LastFmTrackMetadata(name = "Cached Song", artist = "Cached Artist")
        )
        cache.put("global_top_tracks", initialTracks)

        val rateLimitJson = """
            {
              "message": "Rate Limit Exceeded",
              "error": 29
            }
        """.trimIndent()

        val mockClient = createMockOkHttpClient(responseCode = 200, responseBody = rateLimitJson)
        val source = LastFmChartSource(mockClient, config, cache)

        // When rate-limited, source must not throw; it returns stale cache
        val tracks = source.getTopTracks(limit = 10)
        assertEquals(1, tracks.size)
        assertEquals("Cached Song", tracks[0].name)
    }

    private fun createMockOkHttpClient(responseCode: Int, responseBody: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message(if (responseCode == 200) "OK" else "Error")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }
}
