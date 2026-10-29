package com.myplayer.app.data.artwork

import android.content.Context
import com.myplayer.app.data.artwork.cache.ArtworkCache
import com.myplayer.app.data.artwork.matching.ArtworkMatcher
import com.myplayer.app.data.artwork.model.ArtworkResult
import com.myplayer.app.data.artwork.provider.ArtworkProvider
import com.myplayer.app.data.artwork.provider.DeezerArtworkProvider
import com.myplayer.app.data.artwork.provider.ITunesArtworkProvider
import com.myplayer.app.data.artwork.provider.YouTubeMusicArtworkProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArtworkRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cacheDir: File
    private lateinit var mockContext: FakeContext
    private lateinit var artworkCache: ArtworkCache
    private lateinit var matcher: ArtworkMatcher

    @Before
    fun setup() {
        cacheDir = tempFolder.newFolder("repo_test_cache")
        mockContext = FakeContext(cacheDir)
        artworkCache = ArtworkCache(mockContext)
        matcher = ArtworkMatcher()
    }

    @Test
    fun getArtwork_returnsLocalUriWhenTitleAndArtistEmpty() = runBlocking {
        val repo = ArtworkRepository(
            cache = artworkCache,
            matcher = matcher,
            deezerProvider = createFailingDeezer(),
            iTunesProvider = createFailingItunes(),
            youTubeMusicProvider = createStubYt()
        )

        val result = repo.getArtwork(
            title = "",
            artist = "",
            localArtworkUri = "content://media/external/audio/albumart/1"
        )

        assertNotNull(result)
        assertEquals("content://media/external/audio/albumart/1", result?.url)
        assertEquals("Local", result?.provider)
    }

    @Test
    fun getArtwork_hitsCacheBeforeCallingProviders() = runBlocking {
        val cacheKey = matcher.createStableCacheKey("Coldplay", "Yellow")
        val cached = ArtworkResult("https://cdn.example.com/yellow.jpg", "Deezer", 1000, 1000)
        artworkCache.put(cacheKey, cached, "Yellow", "Coldplay")

        val repo = ArtworkRepository(
            cache = artworkCache,
            matcher = matcher,
            deezerProvider = createFailingDeezer(),
            iTunesProvider = createFailingItunes(),
            youTubeMusicProvider = createStubYt()
        )

        val result = repo.getArtwork("Yellow", "Coldplay")
        assertNotNull(result)
        assertEquals(cached.url, result?.url)
    }

    @Test
    fun getArtwork_returnsLocalUriImmediatelyWhenPresent() = runBlocking {
        val repo = ArtworkRepository(
            cache = artworkCache,
            matcher = matcher,
            deezerProvider = createFailingDeezer(),
            iTunesProvider = createFailingItunes(),
            youTubeMusicProvider = createStubYt()
        )

        val result = repo.getArtwork(
            title = "Track Title",
            artist = "Artist Name",
            localArtworkUri = "https://lh3.googleusercontent.com/thumbnail"
        )

        assertNotNull(result)
        assertEquals("https://lh3.googleusercontent.com/thumbnail", result?.url)
        assertEquals("Local", result?.provider)
    }

    private fun createFailingDeezer(): DeezerArtworkProvider {
        return DeezerArtworkProvider(okhttp3.OkHttpClient(), matcher)
    }

    private fun createFailingItunes(): ITunesArtworkProvider {
        return ITunesArtworkProvider(okhttp3.OkHttpClient(), matcher)
    }

    private fun createStubYt(): YouTubeMusicArtworkProvider {
        val enc = com.myplayer.app.security.StringEncryptionManager()
        val api = com.myplayer.app.data.online.InnertubeApi(okhttp3.OkHttpClient(), enc)
        return YouTubeMusicArtworkProvider(api, matcher)
    }

    private class FakeContext(private val baseDir: File) : android.content.ContextWrapper(null) {
        override fun getCacheDir(): File = baseDir
        override fun getApplicationContext(): Context = this
    }
}
