package com.example.myplayer.data.lyrics

import com.example.myplayer.data.local.dao.CachedLyricsDao
import com.example.myplayer.data.local.entity.CachedLyricsEntity
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LyricsRepositoryTest {

    private lateinit var fakeDao: FakeCachedLyricsDao
    private lateinit var repository: LyricsRepository

    @Before
    fun setup() {
        fakeDao = FakeCachedLyricsDao()
        repository = LyricsRepository(
            okHttpClient = OkHttpClient(),
            cachedLyricsDao = fakeDao
        )
    }

    // ==========================================
    // 1. Artist Cleaning Tests
    // ==========================================

    @Test
    fun cleanArtistName_removesTopicSuffix() {
        assertEquals("The Weeknd", repository.cleanArtistName("The Weeknd - Topic"))
        assertEquals("Coldplay", repository.cleanArtistName("Coldplay-Topic"))
    }

    @Test
    fun cleanArtistName_removesVevoSuffix() {
        assertEquals("Adele", repository.cleanArtistName("Adele VEVO"))
        assertEquals("Taylor Swift", repository.cleanArtistName("Taylor SwiftVEVO"))
    }

    @Test
    fun cleanArtistName_removesOfficialTag() {
        assertEquals("Ed Sheeran", repository.cleanArtistName("Ed Sheeran Official"))
    }

    @Test
    fun cleanArtistName_trimsExtraWhitespace() {
        assertEquals("Dua Lipa", repository.cleanArtistName("   Dua   Lipa   "))
    }

    // ==========================================
    // 2. Track Title Cleaning Tests
    // ==========================================

    @Test
    fun cleanTrackTitle_removesParentheticalDescriptors() {
        assertEquals(
            "Blinding Lights",
            repository.cleanTrackTitle("Blinding Lights (Official Music Video)", "The Weeknd")
        )
        assertEquals(
            "Shape of You",
            repository.cleanTrackTitle("Shape of You [Official Lyric Video]", "Ed Sheeran")
        )
        assertEquals(
            "Yellow",
            repository.cleanTrackTitle("Yellow (Live at Glastonbury 2021)", "Coldplay")
        )
    }

    @Test
    fun cleanTrackTitle_removesArtistPrefix() {
        assertEquals(
            "Rolling in the Deep",
            repository.cleanTrackTitle("Adele - Rolling in the Deep", "Adele")
        )
        assertEquals(
            "Starboy",
            repository.cleanTrackTitle("The Weeknd – Starboy", "The Weeknd")
        )
    }

    @Test
    fun cleanTrackTitle_removesPipeAndDelimiterSuffixes() {
        assertEquals(
            "Bohemian Rhapsody",
            repository.cleanTrackTitle("Bohemian Rhapsody | Remastered 2011", "Queen")
        )
    }

    @Test
    fun cleanTrackTitle_removesFeaturedArtistClauses() {
        assertEquals(
            "Levitating",
            repository.cleanTrackTitle("Levitating (feat. Daft Punk)", "Dua Lipa")
        )
    }

    // ==========================================
    // 3. Synced Lyrics Parsing Tests
    // ==========================================

    @Test
    fun parseSyncedLyrics_correctlyParsesDifferentFractionalFormats() {
        val lrc = """
            [00:05.5] Half second line
            [00:15.30] Two digit centisecond line
            [01:05.123] Three digit millisecond line
            [02:00] No fraction line
        """.trimIndent()

        val parsed = repository.parseSyncedLyrics(lrc)
        assertEquals(4, parsed.size)

        // 5.5s = 5500ms
        assertEquals(5500L, parsed[0].first)
        assertEquals("Half second line", parsed[0].second)

        // 15.30s = 15300ms
        assertEquals(15300L, parsed[1].first)
        assertEquals("Two digit centisecond line", parsed[1].second)

        // 1m 5.123s = 65123ms
        assertEquals(65123L, parsed[2].first)
        assertEquals("Three digit millisecond line", parsed[2].second)

        // 2m = 120000ms
        assertEquals(120000L, parsed[3].first)
        assertEquals("No fraction line", parsed[3].second)
    }

    @Test
    fun parseSyncedLyrics_sortsChronologicallyAndSkipsMalformedLines() {
        val lrc = """
            [ar: The Weeknd]
            [ti: Blinding Lights]
            [01:10.00] Second chorus
            [00:10.00] Intro verse
            Invalid timestamp line
            [00:40.50] First chorus
        """.trimIndent()

        val parsed = repository.parseSyncedLyrics(lrc)
        assertEquals(3, parsed.size)
        assertEquals(10000L, parsed[0].first)
        assertEquals("Intro verse", parsed[0].second)
        assertEquals(40500L, parsed[1].first)
        assertEquals("First chorus", parsed[1].second)
        assertEquals(70000L, parsed[2].first)
        assertEquals("Second chorus", parsed[2].second)
    }

    // ==========================================
    // 4. Room Cache Integration Tests
    // ==========================================

    @Test
    fun fetchLyrics_returnsCachedLyricsWhenAvailable() = runBlocking {
        fakeDao.insertLyrics(
            CachedLyricsEntity(
                songId = "song_123",
                plainLyrics = "Cached plain lyrics",
                syncedLyrics = "[00:10.00] Cached synced lyrics",
                trackName = "Test Track",
                artistName = "Test Artist"
            )
        )

        val result = repository.fetchLyrics(
            songId = "song_123",
            trackName = "Test Track",
            artistName = "Test Artist"
        )

        assertNotNull(result)
        assertEquals("Cached plain lyrics", result?.plainLyrics)
        assertEquals("[00:10.00] Cached synced lyrics", result?.syncedLyrics)
        assertEquals("Test Track", result?.trackName)
        assertEquals("Test Artist", result?.artistName)
    }

    // ==========================================
    // Test Double for Room DAO
    // ==========================================

    private class FakeCachedLyricsDao : CachedLyricsDao {
        private val cache = mutableMapOf<String, CachedLyricsEntity>()

        override suspend fun getLyricsForSong(songId: String): CachedLyricsEntity? {
            return cache[songId]
        }

        override suspend fun insertLyrics(lyrics: CachedLyricsEntity) {
            cache[lyrics.songId] = lyrics
        }

        override suspend fun deleteLyricsForSong(songId: String) {
            cache.remove(songId)
        }

        override fun getAllCachedLyricsSync(): List<CachedLyricsEntity> {
            return cache.values.toList()
        }

        override fun insertCachedLyrics(lyrics: List<CachedLyricsEntity>) {
            lyrics.forEach { cache[it.songId] = it }
        }
    }
}
