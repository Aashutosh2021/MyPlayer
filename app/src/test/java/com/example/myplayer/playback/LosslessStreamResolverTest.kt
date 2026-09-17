package com.example.myplayer.playback

import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LosslessStreamResolverTest {

    private lateinit var resolver: LosslessStreamResolver

    @Before
    fun setup() {
        resolver = LosslessStreamResolver(OkHttpClient())
    }

    @Test
    fun cleanTrackTitle_stripsNoisyBracketsAndTrailingDescriptions() {
        val raw = "Bohemian Rhapsody [Official Video] (Remastered 2011)"
        val cleaned = resolver.cleanTrackTitle(raw)
        assertEquals("Bohemian Rhapsody", cleaned)
    }

    @Test
    fun cleanTrackTitle_stripsFeaturingClauses() {
        val raw = "Levitating feat. DaBaby"
        val cleaned = resolver.cleanTrackTitle(raw)
        assertEquals("Levitating", cleaned)
    }

    @Test
    fun isVariantCompatible_rejectsLiveCandidateWhenQueryIsNotLive() {
        val query = "Hotel California"
        val candidate = "Hotel California (Live at The Forum)"
        assertFalse(resolver.isVariantCompatible(query, candidate))
    }

    @Test
    fun isVariantCompatible_acceptsLiveCandidateWhenQueryExplicitlyRequestedLive() {
        val query = "Hotel California Live"
        val candidate = "Hotel California (Live at The Forum)"
        assertTrue(resolver.isVariantCompatible(query, candidate))
    }

    @Test
    fun isVariantCompatible_rejectsAcousticAndRemixVariants() {
        assertFalse(resolver.isVariantCompatible("Blinding Lights", "Blinding Lights (Major Lazer Remix)"))
        assertFalse(resolver.isVariantCompatible("Everlong", "Everlong - Acoustic Version"))
        assertFalse(resolver.isVariantCompatible("Starboy", "Starboy (Slowed & Reverb)"))
    }

    @Test
    fun isDurationAcceptable_acceptsWithinTolerance() {
        assertTrue(resolver.isDurationAcceptable(210_000L, 214_000L)) // 4s diff <= 8s
        assertTrue(resolver.isDurationAcceptable(180_000L, 173_000L)) // 7s diff <= 8s
        assertFalse(resolver.isDurationAcceptable(180_000L, 195_000L)) // 15s diff > 8s
    }

    @Test
    fun inspectQuality_detectsFlacAndLossless() {
        val flacQuality = resolver.inspectQuality("https://cdn.example.com/stream/track_123.flac")
        assertEquals("FLAC", flacQuality.format)
        assertTrue(flacQuality.isLossless)
        assertEquals(24, flacQuality.bitDepth)
        assertEquals("HI-RES LOSSLESS", flacQuality.displayBadge)
    }

    @Test
    fun inspectQuality_detectsOpusStream() {
        val opusQuality = resolver.inspectQuality("https://rr1---sn.googlevideo.com/videoplayback?mime=audio%2fwebm")
        assertEquals("OPUS", opusQuality.format)
        assertFalse(opusQuality.isLossless)
        assertEquals(16, opusQuality.bitDepth)
        assertEquals("HQ OPUS", opusQuality.displayBadge)
    }
}
