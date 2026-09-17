package com.example.myplayer.data.artwork

import com.example.myplayer.data.artwork.matching.ArtworkMatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArtworkMatcherTest {

    private lateinit var matcher: ArtworkMatcher

    @Before
    fun setup() {
        matcher = ArtworkMatcher()
    }

    // ==========================================
    // 1. Normalization Tests
    // ==========================================

    @Test
    fun cleanArtist_stripsTopicVevoAndOfficial() {
        assertEquals("Coldplay", matcher.cleanArtist("Coldplay - Topic"))
        assertEquals("Adele", matcher.cleanArtist("AdeleVEVO"))
        assertEquals("Taylor Swift", matcher.cleanArtist("Taylor Swift Official"))
        assertEquals("The Weeknd", matcher.cleanArtist("  The   Weeknd  "))
    }

    @Test
    fun cleanTitle_stripsBracketsKeywordsAndArtistPrefix() {
        assertEquals(
            "Blinding Lights",
            matcher.cleanTitle("Blinding Lights (Official Music Video)", "The Weeknd")
        )
        assertEquals(
            "Shape of You",
            matcher.cleanTitle("Shape of You [Official Lyric Video]", "Ed Sheeran")
        )
        assertEquals(
            "Yellow",
            matcher.cleanTitle("Yellow (Remastered 2021)", "Coldplay")
        )
        assertEquals(
            "Rolling in the Deep",
            matcher.cleanTitle("Adele - Rolling in the Deep", "Adele")
        )
        assertEquals(
            "Bohemian Rhapsody",
            matcher.cleanTitle("Bohemian Rhapsody | Remastered 2011", "Queen")
        )
    }

    // ==========================================
    // 2. Exact Title / Artist Match Tests
    // ==========================================

    @Test
    fun isValidMatch_acceptsExactMatches() {
        assertTrue(
            matcher.isValidMatch(
                targetTitle = "Yellow",
                targetArtist = "Coldplay",
                candTitle = "Yellow",
                candArtist = "Coldplay"
            )
        )
        assertTrue(
            matcher.isValidMatch(
                targetTitle = "Blinding Lights (Official Video)",
                targetArtist = "The Weeknd - Topic",
                candTitle = "Blinding Lights",
                candArtist = "The Weeknd"
            )
        )
    }

    // ==========================================
    // 3. Variant Protection & Rejection Tests
    // ==========================================

    @Test
    fun isVariantCompatible_rejectsRemixWhenQueryIsNotRemix() {
        // Query is original, candidate is remix -> MUST REJECT
        assertFalse(
            matcher.isVariantCompatible(
                queryTitle = "Levitating",
                candidateTitle = "Levitating (The Blessed Madonna Remix)"
            )
        )
        assertFalse(
            matcher.isVariantCompatible(
                queryTitle = "Blinding Lights",
                candidateTitle = "Blinding Lights (Club Mix)"
            )
        )

        // Query IS a remix, candidate is a remix -> ACCEPT
        assertTrue(
            matcher.isVariantCompatible(
                queryTitle = "Levitating (Remix)",
                candidateTitle = "Levitating (Dua Lipa Remix)"
            )
        )
    }

    @Test
    fun isVariantCompatible_rejectsLiveWhenQueryIsNotLive() {
        // Query is studio, candidate is live -> MUST REJECT
        assertFalse(
            matcher.isVariantCompatible(
                queryTitle = "Yellow",
                candidateTitle = "Yellow (Live at Glastonbury)"
            )
        )
        assertFalse(
            matcher.isVariantCompatible(
                queryTitle = "Hotel California",
                candidateTitle = "Hotel California (Acoustic Live)"
            )
        )

        // Query IS live -> ACCEPT
        assertTrue(
            matcher.isVariantCompatible(
                queryTitle = "Yellow (Live)",
                candidateTitle = "Yellow (Live at Glastonbury 2021)"
            )
        )
    }

    @Test
    fun isVariantCompatible_rejectsInstrumentalWhenQueryIsNotInstrumental() {
        assertFalse(
            matcher.isVariantCompatible(
                queryTitle = "Someone Like You",
                candidateTitle = "Someone Like You (Instrumental Karaoke)"
            )
        )
    }

    @Test
    fun isValidMatch_rejectsCompletelyWrongSongs() {
        assertFalse(
            matcher.isValidMatch(
                targetTitle = "Shape of You",
                targetArtist = "Ed Sheeran",
                candTitle = "Bad Habits",
                candArtist = "Ed Sheeran"
            )
        )
        assertFalse(
            matcher.isValidMatch(
                targetTitle = "Yellow",
                targetArtist = "Coldplay",
                candTitle = "Yellow Submarine",
                candArtist = "The Beatles"
            )
        )
    }

    // ==========================================
    // 4. Stable Cache Key Tests
    // ==========================================

    @Test
    fun createStableCacheKey_generatesDeterministicFilesystemSafeKey() {
        val key1 = matcher.createStableCacheKey("Coldplay - Topic", "Yellow (Official Video)")
        val key2 = matcher.createStableCacheKey("Coldplay", "Yellow")
        assertEquals("coldplay_yellow", key1)
        assertEquals("coldplay_yellow", key2)

        val keyWithSpecialChars = matcher.createStableCacheKey("AC/DC", "T.N.T.")
        assertFalse(keyWithSpecialChars.contains("/"))
        assertFalse(keyWithSpecialChars.contains("."))
        assertTrue(keyWithSpecialChars.matches(Regex("""[a-z0-9_]+""")))
    }
}
