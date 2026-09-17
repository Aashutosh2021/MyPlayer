package com.example.myplayer.data.artwork.matching

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalizes metadata and performs fuzzy similarity evaluation to prevent incorrect album covers.
 */
@Singleton
class ArtworkMatcher @Inject constructor() {

    companion object {
        private val BRACKET_NOISE_REGEX = Regex("""[\(\[].*?[\)\]]""")
        private val KEYWORD_NOISE_REGEX = Regex(
            """(?i)\b(official|video|audio|lyrics?|lyrical|full song|song|hd|4k|mv|m/v|reprise|cover|version|visualizer|color coded|remastered(?:\s+\d{4})?)\b"""
        )
        private val WHITESPACE_REGEX = Regex("""\s+""")
        private val FEAT_REGEX = Regex("""(?i)\b(feat\.?|ft\.?)\s+.*$""")
        private val ARTIST_TOPIC_REGEX = Regex("""(?i)\s*-\s*Topic\b""")
        private val ARTIST_VEVO_REGEX = Regex("""(?i)VEVO\b""")
        private val ARTIST_OFFICIAL_REGEX = Regex("""(?i)\bOfficial\b""")

        // Variant keywords
        private val REMIX_REGEX = Regex("""(?i)\b(remix|club mix|extended mix|dub mix|vip mix)\b""")
        private val LIVE_REGEX = Regex("""(?i)\b(live|acoustic|unplugged|in concert|live at|live in)\b""")
        private val INSTRUMENTAL_REGEX = Regex("""(?i)\b(instrumental|karaoke|backing track)\b""")

        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.68
    }

    /**
     * Cleans common YouTube channel noise from artist names.
     */
    fun cleanArtist(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(ARTIST_TOPIC_REGEX, "")
            .replace(ARTIST_VEVO_REGEX, "")
            .replace(ARTIST_OFFICIAL_REGEX, "")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    /**
     * Cleans common video and audio metadata noise from track titles.
     */
    fun cleanTitle(raw: String, cleanArtist: String = ""): String {
        if (raw.isBlank()) return ""
        var title = raw

        // Strip parenthetical/bracketed descriptors
        title = title.replace(BRACKET_NOISE_REGEX, " ")
        // Strip delimiter suffixes
        title = title.split('|', '•', '/').first()
        // Strip common keywords
        title = title.replace(KEYWORD_NOISE_REGEX, " ")

        // Strip "Artist - Title" prefix if present
        val dashSeparators = listOf(" - ", " – ", " — ")
        for (sep in dashSeparators) {
            if (title.contains(sep)) {
                val parts = title.split(sep, limit = 2)
                val prefix = parts[0].trim().lowercase()
                val cleanA = cleanArtist.lowercase()
                if (cleanA.isNotBlank() && (prefix == cleanA || cleanA.contains(prefix) || prefix.contains(cleanA))) {
                    title = parts[1]
                    break
                }
            }
        }

        // Strip trailing featured artist clauses
        title = title.replace(FEAT_REGEX, " ")
        return title.replace(WHITESPACE_REGEX, " ").trim()
    }

    /**
     * Strict variant protection:
     * - Rejects remixes if the query is not a remix.
     * - Rejects live/acoustic tracks if the query is not live.
     * - Rejects instrumental tracks if the query is not instrumental.
     */
    fun isVariantCompatible(queryTitle: String, candidateTitle: String): Boolean {
        val q = queryTitle.lowercase()
        val c = candidateTitle.lowercase()

        // 1. Remix check
        val qIsRemix = REMIX_REGEX.containsMatchIn(q)
        val cIsRemix = REMIX_REGEX.containsMatchIn(c)
        if (!qIsRemix && cIsRemix) return false

        // 2. Live check
        val qIsLive = LIVE_REGEX.containsMatchIn(q)
        val cIsLive = LIVE_REGEX.containsMatchIn(c)
        if (!qIsLive && cIsLive) return false

        // 3. Instrumental check
        val qIsInst = INSTRUMENTAL_REGEX.containsMatchIn(q)
        val cIsInst = INSTRUMENTAL_REGEX.containsMatchIn(c)
        if (!qIsInst && cIsInst) return false

        return true
    }

    /**
     * Calculates confidence score between target and candidate metadata (0.0 to 1.0).
     */
    fun calculateScore(
        targetTitle: String,
        targetArtist: String,
        candTitle: String,
        candArtist: String
    ): Double {
        val cleanTargetTitle = cleanTitle(targetTitle, targetArtist)
        val cleanCandTitle = cleanTitle(candTitle, candArtist)
        val cleanTargetArtist = cleanArtist(targetArtist)
        val cleanCandArtist = cleanArtist(candArtist)

        val titleSim = normalizedSimilarity(cleanTargetTitle, cleanCandTitle)
        val artistSim = normalizedSimilarity(cleanTargetArtist, cleanCandArtist)

        // Substring bonus
        val titleSubBonus = if (cleanCandTitle.contains(cleanTargetTitle, ignoreCase = true) ||
            cleanTargetTitle.contains(cleanCandTitle, ignoreCase = true)) 0.1 else 0.0
        val artistSubBonus = if (cleanCandArtist.contains(cleanTargetArtist, ignoreCase = true) ||
            cleanTargetArtist.contains(cleanCandArtist, ignoreCase = true)) 0.1 else 0.0

        val effectiveTitleSim = (titleSim + titleSubBonus).coerceAtMost(1.0)
        val effectiveArtistSim = (artistSim + artistSubBonus).coerceAtMost(1.0)

        return (effectiveTitleSim * 0.6) + (effectiveArtistSim * 0.4)
    }

    /**
     * Returns true if candidate passes variant compatibility and minimum confidence threshold.
     */
    fun isValidMatch(
        targetTitle: String,
        targetArtist: String,
        candTitle: String,
        candArtist: String,
        minConfidence: Double = DEFAULT_CONFIDENCE_THRESHOLD
    ): Boolean {
        if (!isVariantCompatible(targetTitle, candTitle)) return false
        val score = calculateScore(targetTitle, targetArtist, candTitle, candArtist)
        return score >= minConfidence
    }

    /**
     * Generates a stable, filesystem-safe cache key from artist and title.
     */
    fun createStableCacheKey(artist: String, title: String): String {
        val cArtist = cleanArtist(artist).lowercase()
        val cTitle = cleanTitle(title, cArtist).lowercase()
        val rawKey = "${cArtist}_${cTitle}"
        val sanitized = rawKey.replace(Regex("""[^a-z0-9]+"""), "_").trim('_')
        return sanitized.ifBlank { "unknown_artwork" }
    }

    private fun normalizedSimilarity(s1: String, s2: String): Double {
        val a = s1.lowercase().trim()
        val b = s2.lowercase().trim()
        if (a == b) return 1.0
        val maxLength = maxOf(a.length, b.length)
        if (maxLength == 0) return 1.0
        val distance = levenshteinDistance(a, b)
        return 1.0 - (distance.toDouble() / maxLength)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        var dp = IntArray(len2 + 1) { it }
        var nextDp = IntArray(len2 + 1)

        for (i in 1..len1) {
            nextDp[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                nextDp[j] = minOf(
                    dp[j] + 1,
                    nextDp[j - 1] + 1,
                    dp[j - 1] + cost
                )
            }
            val temp = dp
            dp = nextDp
            nextDp = temp
        }
        return dp[len2]
    }
}
