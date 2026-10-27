package com.example.myplayer.ui.screens.search

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.strategy.SongContextClassifier

/**
 * Builds the playback queue for a song the user taps in SEARCH RESULTS so the next track
 * continues the same vibe instead of walking the raw YouTube search order.
 *
 * Two stages:
 *  1. [compose]          : instant queue from the search list (language + devotional gate,
 *                          opposite-mood / same-title demotion). Used while playback starts.
 *  2. [composeRadioTail] : a moment later the upcoming part of the queue is REPLACED by
 *                          YouTube Music's own radio for the selected song (same engine that makes
 *                          "Aaj Ki Raat" -> other Bollywood party tracks), vetted by the same gates.
 *
 * Why stage 2: search results are matched on words, not on vibe. A search for "aaj ki raat"
 * also returns naats, qawwalis and old songs that merely contain those words, all of them
 * "Hindi/Urdu", so no language rule can tell them apart from a Stree 2 party track.
 */
object SearchQueueComposer {

    private const val MIN_FILL = 5
    private const val MIN_RADIO_TAIL = 8

    private data class Scored(
        val song: OnlineSong,
        val score: Int,
        val index: Int,
        val opposite: Boolean
    )

    /** True when [cand] must never follow [seed]. */
    private fun mustNotFollow(
        seed: SongContextClassifier.SongContext,
        cand: SongContextClassifier.SongContext
    ): Boolean {
        // Bollywood -> Hollywood (or any known language shift)
        if (SongContextClassifier.languageConflict(seed, cand)) return true
        // Non-religious seed -> naat / nasheed / bhajan / aarti ... never.
        if (SongContextClassifier.isDevotional(cand) && !SongContextClassifier.isDevotional(seed)) return true
        return false
    }

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

            // 1. Hard gates (language, religious content)
            if (mustNotFollow(seed, cand)) return@forEachIndexed

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
            // No language gate possible: keep everything that passed the other gates, soft-ordered.
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

    /**
     * Upcoming queue built from YouTube Music's radio for [selected].
     * Radio order is kept (it is YouTube's own vibe ranking); we only strip what must not follow:
     * the song itself, other versions of the same title, language shifts and religious tracks.
     * Only if the radio gives fewer than [MIN_RADIO_TAIL] usable songs, the already-vetted
     * search items ([vettedSearch]) are appended as backup.
     */
    fun composeRadioTail(
        selected: OnlineSong,
        radio: List<OnlineSong>,
        vettedSearch: List<OnlineSong>
    ): List<OnlineSong> {
        val seed = SongContextClassifier.analyzeSong(selected.title, selected.artist)
        val selectedNorm = normalizeTitle(selected.title)
        val seen = mutableSetOf(selected.videoId)
        val out = mutableListOf<OnlineSong>()

        for (song in radio) {
            if (song.videoId in seen) continue
            if (isSameNamed(selectedNorm, song.title)) continue
            val cand = SongContextClassifier.analyzeSong(song.title, song.artist)
            if (mustNotFollow(seed, cand)) continue
            seen += song.videoId
            out += song
        }

        if (out.size < MIN_RADIO_TAIL) {
            for (song in vettedSearch) {
                if (song.videoId in seen) continue
                seen += song.videoId
                out += song
            }
        }
        return out
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
