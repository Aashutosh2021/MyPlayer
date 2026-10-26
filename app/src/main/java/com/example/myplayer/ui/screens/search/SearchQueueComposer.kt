package com.example.myplayer.ui.screens.search

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.strategy.SongContextClassifier

/**
 * Reorders a search-results queue so autoplay continues in the selected song's
 * language/mood/genre instead of walking the raw YouTube search order
 * (which parks same-title covers/versions and unrelated ranks right after the click).
 *
 * Composition rule:
 *  1. The song the user clicked always plays first (index 0).
 *  2. Everything else is scored with [SongContextClassifier] continuity
 *     (language +18/-20, mood +12/-6, genre +8).
 *  3. Same-title versions (covers/remixes "(...)" edits) are demoted (-40) to the
 *     back so the NEXT different track is a different vibe, not another version.
 */
object SearchQueueComposer {

    fun compose(selected: OnlineSong, context: List<OnlineSong>): Pair<List<OnlineSong>, Int> {
        if (context.size < 2) {
            val index = context.indexOfFirst { it.videoId == selected.videoId }
            return if (index >= 0) context to index else (listOf(selected) + context) to 0
        }

        val head = context.firstOrNull { it.videoId == selected.videoId } ?: selected
        val rest = context.filterNot { it.videoId == selected.videoId }

        val seed = SongContextClassifier.analyze(head.title, head.artist)
        if (seed.isEmpty) return (listOf(head) + rest) to 0

        val selectedNorm = normalizeTitle(head.title)
        val orderedRest = rest
            .mapIndexed { originalIndex, song ->
                val candidate = SongContextClassifier.analyze(song.title, song.artist)
                var score = 0

                if (seed.language.isNotEmpty() && candidate.language.isNotEmpty()) {
                    score += if (seed.language.intersect(candidate.language).isNotEmpty()) 18 else -20
                }
                if (seed.mood.isNotEmpty() && candidate.mood.isNotEmpty()) {
                    score += if (seed.mood.intersect(candidate.mood).isNotEmpty()) 12 else -6
                }
                if (seed.genre.isNotEmpty() && candidate.genre.isNotEmpty() &&
                    seed.genre.intersect(candidate.genre).isNotEmpty()
                ) {
                    score += 8
                }

                // Same-title covers/versions queue ke peeche — next alag vibe pe ho.
                if (isSameNamed(selectedNorm, song.title)) score -= 40

                Triple(song, score, originalIndex)
            }
            .sortedWith(compareByDescending<Triple<OnlineSong, Int, Int>> { it.second }.thenBy { it.third })
            .map { it.first }

        return (listOf(head) + orderedRest) to 0
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
