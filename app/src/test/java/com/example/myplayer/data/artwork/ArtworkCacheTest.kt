package com.example.myplayer.data.artwork

import android.content.Context
import com.example.myplayer.data.artwork.cache.ArtworkCache
import com.example.myplayer.data.artwork.model.ArtworkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArtworkCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cacheDir: File
    private lateinit var mockContext: FakeContext
    private lateinit var artworkCache: ArtworkCache

    @Before
    fun setup() {
        cacheDir = tempFolder.newFolder("artwork_test_cache")
        mockContext = FakeContext(cacheDir)
        artworkCache = ArtworkCache(mockContext)
    }

    @Test
    fun putAndGet_storesAndRetrievesFromCache() = runBlocking {
        val result = ArtworkResult(
            url = "https://is1-ssl.mzstatic.com/image/thumb/1000x1000bb.jpg",
            provider = "iTunes",
            width = 1000,
            height = 1000
        )

        artworkCache.put("coldplay_yellow", result, "Yellow", "Coldplay")

        val retrieved = artworkCache.get("coldplay_yellow")
        assertNotNull(retrieved)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/1000x1000bb.jpg", retrieved?.url)
        assertEquals("iTunes", retrieved?.provider)
        assertEquals(1000, retrieved?.width)
    }

    @Test
    fun putNotFound_recordsNegativeCacheHit() = runBlocking {
        assertFalse(artworkCache.isNegativeCached("obscure_song"))

        artworkCache.putNotFound("obscure_song", "Obscure Track", "Unknown Artist")

        assertTrue(artworkCache.isNegativeCached("obscure_song"))
        assertNull(artworkCache.get("obscure_song"))
    }

    @Test
    fun clear_removesAllEntries() = runBlocking {
        artworkCache.put("track1", ArtworkResult("url1", "Deezer"), "T1", "A1")
        artworkCache.put("track2", ArtworkResult("url2", "iTunes"), "T2", "A2")

        assertNotNull(artworkCache.get("track1"))
        assertNotNull(artworkCache.get("track2"))

        artworkCache.clear()

        assertNull(artworkCache.get("track1"))
        assertNull(artworkCache.get("track2"))
    }

    private class FakeContext(private val baseDir: File) : android.content.ContextWrapper(null) {
        override fun getCacheDir(): File = baseDir
        override fun getApplicationContext(): Context = this
    }
}
