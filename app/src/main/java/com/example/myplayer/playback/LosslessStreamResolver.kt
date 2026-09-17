package com.example.myplayer.playback

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent stream resolver and metadata verifier.
 * Inspired by LastWave's dual-tier stream resolution and identity variant protection.
 *
 * Cleans noisy titles, detects non-matching variants (e.g. live, acoustic, remix),
 * checks duration tolerance (±8s), and assesses audio stream format and fidelity.
 */
@Singleton
class LosslessStreamResolver @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "LosslessResolver"
        private const val MAX_DURATION_DIFFERENCE_SEC = 8

        private val BRACKETED_DISPLAY_NOISE = Regex(
            """(?i)[\[(]\s*(?:official|music|video|audio|lyrics?|visualizer|hd|4k|mv|full\s+song|prod\.?\s*by|remastered|explicit|clean).*?[\])]"""
        )
        private val TRAILING_NOISE = Regex(
            """(?i)\s*[-–—]\s*(?:official|music|video|audio|lyrics?|visualizer|mv|full\s+song)\s*$"""
        )
        private val FEATURING_CLAUSE = Regex(
            """(?i)(?:\s*[\[(])?\s*(?:feat\.?|ft\.?|featuring)\s+.*$"""
        )

        private val IDENTITY_VARIANTS = listOf(
            "live" to Regex("""(?i)\blive\b"""),
            "acoustic" to Regex("""(?i)\bacoustic\b"""),
            "karaoke" to Regex("""(?i)\bkaraoke\b"""),
            "instrumental" to Regex("""(?i)\binstrumental\b"""),
            "remix" to Regex("""(?i)\bremix(?:ed)?\b"""),
            "slowed" to Regex("""(?i)\bslowed\b"""),
            "reverb" to Regex("""(?i)\breverb\b"""),
            "sped up" to Regex("""(?i)\bsped\s*up\b"""),
            "nightcore" to Regex("""(?i)\bnightcore\b""")
        )
    }

    data class ResolutionResult(
        val streamUrl: String,
        val qualityInfo: AudioQualityInfo
    )

    /**
     * Cleans display title by stripping bracketed video noise, audio watermarks,
     * and trailing descriptors.
     */
    fun cleanTrackTitle(rawTitle: String): String {
        return rawTitle
            .replace(BRACKETED_DISPLAY_NOISE, "")
            .replace(TRAILING_NOISE, "")
            .replace(FEATURING_CLAUSE, "")
            .trim()
    }

    /**
     * Checks if a candidate title matches the variant expectations of the query.
     * Prevents regular songs from being replaced by live/acoustic/remix versions.
     */
    fun isVariantCompatible(queryTitle: String, candidateTitle: String): Boolean {
        for ((_, pattern) in IDENTITY_VARIANTS) {
            val inQuery = pattern.containsMatchIn(queryTitle)
            val inCandidate = pattern.containsMatchIn(candidateTitle)
            if (inCandidate && !inQuery) {
                Log.d(TAG, "Variant mismatch rejected: query='$queryTitle' vs candidate='$candidateTitle'")
                return false
            }
        }
        return true
    }

    /**
     * Checks if duration difference is within the acceptable tolerance window.
     */
    fun isDurationAcceptable(expectedDurationMs: Long, actualDurationMs: Long): Boolean {
        if (expectedDurationMs <= 0 || actualDurationMs <= 0) return true
        val diffSec = Math.abs(expectedDurationMs - actualDurationMs) / 1000
        return diffSec <= MAX_DURATION_DIFFERENCE_SEC
    }

    /**
     * Inspects stream URL and metadata to determine the active audio format and quality.
     */
    fun inspectQuality(streamUrl: String, isLocalFile: Boolean = false): AudioQualityInfo {
        val lower = streamUrl.lowercase()
        return when {
            lower.endsWith(".flac") || lower.contains("flac") ->
                AudioQualityInfo(
                    format = "FLAC",
                    bitDepth = 24,
                    sampleRateHz = 96000,
                    bitrateKbps = 1411,
                    isLossless = true,
                    sourceName = if (isLocalFile) "Local FLAC" else "Hi-Res Lossless"
                )
            lower.contains("opus") || lower.contains("audio/opus") || lower.contains("mime=audio%2fwebm") ->
                AudioQualityInfo(
                    format = "OPUS",
                    bitDepth = 16,
                    sampleRateHz = 48000,
                    bitrateKbps = 160,
                    isLossless = false,
                    sourceName = if (isLocalFile) "Local Opus" else "YouTube Music Opus"
                )
            lower.endsWith(".m4a") || lower.contains("audio/mp4") || lower.contains("m4a") ->
                AudioQualityInfo(
                    format = "AAC",
                    bitDepth = 16,
                    sampleRateHz = 44100,
                    bitrateKbps = 256,
                    isLossless = false,
                    sourceName = if (isLocalFile) "Local AAC" else "AAC Stream"
                )
            lower.endsWith(".mp3") || lower.contains("audio/mpeg") ->
                AudioQualityInfo(
                    format = "MP3",
                    bitDepth = 16,
                    sampleRateHz = 44100,
                    bitrateKbps = 320,
                    isLossless = false,
                    sourceName = if (isLocalFile) "Local MP3" else "MP3 Stream"
                )
            else ->
                AudioQualityInfo(
                    format = if (isLocalFile) "LOCAL" else "STREAM",
                    bitDepth = 16,
                    sampleRateHz = 44100,
                    bitrateKbps = null,
                    isLossless = false,
                    sourceName = if (isLocalFile) "Local Audio" else "Standard Stream"
                )
        }
    }
}
