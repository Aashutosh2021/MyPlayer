package com.example.myplayer.ui.screens.search

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.strategy.SongContextClassifier

/**
 * Builds the playback queue for a song the user taps in SEARCH RESULTS, so the next track
 * continues the same vibe instead of walking the raw YouTube search order.
 *
 * Rules (selected = the tapped song, always index 0):
 *  1. LANGUAGE GATE (hard): a candidate whose language is known and shares nothing with the
 *     selected song's language is DROPPED (Bollywood -> Hollywood never reaches the queue).
 *  2. MOOD: an explicitly opposite mood (happy/party/workout vs sad/chill/focus) is demoted out
 *     of the main list; it is only used as a last resort filler.
 *  3. Same-title covers / versions are pushed back (the next track must be a different song).
 *  4. Unknown-language candidates (no signal at all) only fill the queue up to [MIN_FILL];
 *     they are never trusted over confirmed matches.
 *  5. When the matched list is short, the queue simply ends sooner and the existing autoplay
 *     (YouTube radio + SmartRankingStrategy) continues the vibe, which is far better than
 *     appending random search results.
 *
 * If the selected song's language cannot be detected at all, the gate is skipped and only the
 * soft ordering (mood / genre / artist / same-title) is applied.
 */
object SearchQueueComposer {

    private const val MIN_FILL = 5

    private data class Scored(
        val song: OnlineSong,
        val score: Int,
        val index: Int,
        val opposite: Boolean
    )

    fun compose(selected: OnlineSong, context: List<OnlineSong>): Pair<List<OnlineSong>, Int> {
        if (context.size < 2) {
            val index = context.indexOfFirst { it.videoId == selected.videoId }
            return if (index >= 0) context to index else (listOf(selected) + context) to 0
        }

        val head = context.firstOrNull { it.videoId == selected.videoId } ?: selected
        val rest = context.filterNot { it.videoId == selected.videoId }

        val seed = SongContextClassifier.analyzeSong(head.title, head.artist)
        if (seed.isEmpty) return (listOf(head) + rest) to 0

        val seedLanguageKnown = seed.language.isNotEmpty()
        val selectedNorm = normalizeTitle(head.title)
        val seedArtist = head.artist.trim().lowercase()

        val confirmed = mutableListOf<Scored>()   // language known and matching (or no gate)
        val unknown = mutableListOf<Scored>()     // gate active, candidate has no language signal

        rest.forEachIndexed { originalIndex, song ->
            val cand = SongContextClassifier.analyzeSong(song.title, song.artist)

            // 1. Hard language gate
            if (seedLanguageKnown && SongContextClassifier.languageConflict(seed, cand)) return@forEachIndexed

            var score = 0
            if (seedLanguageKnown && cand.language.isNotEmpty()) score += 18

            val opposite = SongContextClassifier.moodOpposes(seed.mood, cand.mood)
            if (seed.mood.isNotEmpty() && cand.mood.isNotEmpty() && seed.mood.intersect(cand.mood).isNotEmpty()) score += 12
            if (opposite) score -= 30

            if (seed.genre.isNotEmpty() && cand.genre.isNotEmpty() && seed.genre.intersect(cand.genre).isNotEmpty()) score += 8
            if (seedArtist.isNotEmpty() && song.artist.trim().lowercase() == seedArtist) score += 6
            if (isSameNamed(selectedNorm, song.title)) score -= 40

            val item = Scored(song, score, originalIndex, opposite)
            if (seedLanguageKnown && cand.language.isEmpty()) unknown += item else confirmed += item
        }

        val byScore = compareByDescending<Scored> { it.score }.thenBy { it.index }
        confirmed.sortWith(byScore)
        unknown.sortWith(byScore)

        val result = mutableListOf<OnlineSong>()

        if (!seedLanguageKnown) {
            // No language gate possible: keep everything, just soft-ordered.
            confirmed.forEach { result += it.song }
            return (listOf(head) + result) to 0
        }

        val good = confirmed.filter { it.score >= 0 && !it.opposite }
        val weak = confirmed.filter { it.score < 0 || it.opposite }

        good.forEach { result += it.song }
        if (result.size < MIN_FILL) unknown.take(MIN_FILL - result.size).forEach { result += it.song }
        if (result.size < MIN_FILL) weak.take(MIN_FILL - result.size).forEach { result += it.song }

        return (listOf(head) + result) to 0
    }

    private fun isSameNamed(selectedNorm: String, otherTitle: String): Boolean {
        if (selectedNorm.isBlank()) return false
        val other = normalizeTitle(otherTitle)
        if (other.isBlank()) return false
        return other == selectedNorm ||
            other.startsWith(selectedNorm) ||
            selectedNorm.startsWith(other)
    }

    /** Strips YouTube noise so "Tere Liye (Official Video)" == "Tere Liye". */
    private fun normalizeTitle(title: String): String =
        title.lowercase()
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("\\b(official|lyrics?|audio|video|songs?|full|hd|hq|4k|mv|music|remastered|visualizer|lyrical)\\b"), " ")
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
