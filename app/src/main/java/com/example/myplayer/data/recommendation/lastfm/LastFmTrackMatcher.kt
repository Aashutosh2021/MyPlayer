package com.example.myplayer.data.recommendation.lastfm

import android.util.Log
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Resolves Last.fm metadata candidates to playable YouTube Music tracks.
 * Uses bounded concurrency (Semaphore(4)) and scoring to reject incorrect matches.
 */
@Singleton
class LastFmTrackMatcher @Inject constructor(
    private val innertubeApi: InnertubeApi
) {
    companion object {
        private const val TAG = "LastFmTrackMatcher"
        private const val MAX_CONCURRENT_MATCHES = 4
    }

    private val semaphore = Semaphore(MAX_CONCURRENT_MATCHES)

    suspend fun matchTracks(tracks: List<LastFmTrackMetadata>): List<OnlineSong> = withContext(Dispatchers.IO) {
        val candidates = tracks.take(12)
        val deferred = candidates.map { track ->
            async {
                semaphore.withPermit {
                    matchSingleTrack(track)
                }
            }
        }
        deferred.awaitAll().filterNotNull()
    }

    private suspend fun matchSingleTrack(track: LastFmTrackMetadata): OnlineSong? {
        val query = "${track.artist} ${track.name}".trim()
        if (query.isBlank()) return null

        return try {
            val searchPage = innertubeApi.search(query)
            val candidates = searchPage.songs
            if (candidates.isEmpty()) {
                Log.d(TAG, "LASTFM_MATCH_FAILED: No YouTube results for '$query'")
                return null
            }

            val bestMatch = findBestMatch(track, candidates)
            if (bestMatch != null) {
                Log.d(TAG, "LASTFM_MATCH_SUCCESS: '$query' -> '${bestMatch.title}' by '${bestMatch.artist}' (${bestMatch.videoId})")
            } else {
                Log.d(TAG, "LASTFM_MATCH_REJECTED: Candidates for '$query' did not meet accuracy threshold")
            }
            bestMatch
        } catch (e: Exception) {
            Log.w(TAG, "Error matching Last.fm candidate '$query'", e)
            null
        }
    }

    /**
     * Scores candidates to pick the most accurate match.
     * Rejects candidates that have no matching artist or title tokens.
     */
    fun findBestMatch(track: LastFmTrackMetadata, candidates: List<OnlineSong>): OnlineSong? {
        val cleanExpectedTitle = cleanString(track.name)
        val cleanExpectedArtist = cleanString(track.artist)

        var bestScore = -1
        var bestCandidate: OnlineSong? = null

        for (candidate in candidates) {
            val cleanCandTitle = cleanString(candidate.title)
            val cleanCandArtist = cleanString(candidate.artist)

            var score = 0

            // 1. Artist matching
            val artistMatches = cleanCandArtist.contains(cleanExpectedArtist) ||
                                cleanExpectedArtist.contains(cleanCandArtist)
            if (artistMatches) {
                score += 40
            } else {
                // Check token intersection for multi-artist or featured artist
                val expectedArtistTokens = cleanExpectedArtist.split(" ").filter { it.length > 2 }
                val candArtistTokens = cleanCandArtist.split(" ").filter { it.length > 2 }
                if (expectedArtistTokens.any { candArtistTokens.contains(it) }) {
                    score += 25
                }
            }

            // 2. Title matching
            val titleMatches = cleanCandTitle.contains(cleanExpectedTitle) ||
                               cleanExpectedTitle.contains(cleanCandTitle)
            if (titleMatches) {
                score += 40
            } else {
                val expectedTitleTokens = cleanExpectedTitle.split(" ").filter { it.length > 2 }
                val candTitleTokens = cleanCandTitle.split(" ").filter { it.length > 2 }
                val commonTokens = expectedTitleTokens.intersect(candTitleTokens.toSet())
                if (commonTokens.isNotEmpty()) {
                    score += commonTokens.size * 10
                }
            }

            // 3. Duration alignment (if Last.fm gave duration)
            if (track.durationSeconds > 10L && candidate.durationMs > 10000L) {
                val expectedDurMs = track.durationSeconds * 1000L
                val diffSec = abs(expectedDurMs - candidate.durationMs) / 1000L
                if (diffSec <= 15) {
                    score += 15
                } else if (diffSec <= 45) {
                    score += 5
                }
            }

            // Minimum acceptance threshold: Must have at least moderate title and artist correlation
            if (score >= 40 && score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        return bestCandidate
    }

    private fun cleanString(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
