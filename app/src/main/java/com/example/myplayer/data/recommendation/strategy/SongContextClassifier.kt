package com.example.myplayer.data.recommendation.strategy

/**
 * Extracts language / mood / genre signals from plain song text (title, artist, album).
 * Pure Kotlin, zero network, zero Android dependency — used by the ranking strategy so
 * autoplay keeps the playing song's region and mood (e.g. a Bollywood happy track does
 * not hand over to a Hollywood/global-chart track).
 *
 * Multi-tag example: a mix titled "Bollywood Happy Songs 2025 | Nonstop Party Mix" yields
 * language=indian, mood={happy, party}, genre={filmy}.
 */
object SongContextClassifier {

    data class SongContext(
        val language: Set<String> = emptySet(),
        val mood: Set<String> = emptySet(),
        val genre: Set<String> = emptySet()
    ) {
        val isEmpty: Boolean get() = language.isEmpty() && mood.isEmpty() && genre.isEmpty()
    }

    fun analyze(vararg textParts: String?): SongContext {
        val text = textParts.filterNotNull().joinToString(" ").trim().lowercase()
        if (text.isEmpty()) return SongContext()

        val language = languageOf(text)
        return SongContext(language, moodOf(text), genreOf(text))
    }

    /** Script ranges first (fonts on devanagari/punjabi/tamil/arabic lyrics titles), then keywords. */
    private	fun languageOf(text: String): Set<String> = buildSet {
        if (text.any { it in '\u0900'..'\u097F' }) add("indian")    // Devanagari
        if (text.any { it in '\u0A00'..'\u0A7F' }) add("punjabi")   // Gurmukhi
        if (text.any { it in '\u0B80'..'\u0B7F' }) add("tamil")     // Tamil
        if (text.any { it in '\u0600'..'\u06FF' }) add("arabic")    // Arabic

        if (listOf("bollywood", "hindi", "desi", "gaana", "indian song", "hindi gaana").any { text.contains(it) }) add("indian")
        if (listOf("hollywood", "english song", "western song").any { text.contains(it) }) add("western")
        if (listOf("punjabi", "punjab", "gurmukhi").any { text.contains(it) }) add("punjabi")
        if (listOf("tollywood", "telugu").any { text.contains(it) }) add("telugu")
        if (listOf("kollywood", "tamil song").any { text.contains(it) }) add("tamil")
        if (listOf("arabic", "arab ").any { text.contains(it) }) add("arabic")
        if (listOf("spanish", "reggaeton", "latin music").any { text.contains(it) }) add("latin")
    }

    private fun moodOf(text: String): Set<String> = buildSet {
        if (listOf("happy", "khushi", "khush", "celebration", "feel good", "upbeat", "positive", "masti", "zindagi").any { text.contains(it) }) add("happy")
        if (listOf("party", "dance", "club", "dj", "remix", "edm", "bass ", "disco", "groove", "rave", "nonstop", "non stop", "mashup").any { text.contains(it) }) add("party")
        if (listOf("sad", "heartbreak", "heart break", "broken", "cry", "alone", "pain", "lonely", "bewafa", "dard").any { text.contains(it) }) add("sad")
        if (listOf("romantic", "love", "pyar", "pyaar", "ishq", "dil ", "sajan", "junoon", "valentine").any { text.contains(it) }) add("romantic")
        if (listOf("chill", "lofi", "lo-fi", "relax", "calm", "sleep", "ambient", "soft ", "peace").any { text.contains(it) }) add("chill")
        if (listOf("workout", "gym", "fitness", "energy", "power", "motivation", "beast", "training", "pump").any { text.contains(it) }) add("workout")
        if (listOf("focus", "study", "concentration", "instrumental", "binaural", "deep work").any { text.contains(it) }) add("focus")
    }

    private fun genreOf(text: String): Set<String> = buildSet {
        if (listOf("bollywood", "desi", "filmy", "film song").any { text.contains(it) }) add("filmy")
        if (listOf("pop", "chart", "billboard", "top 40").any { text.contains(it) }) add("pop")
        if (listOf("hip hop", "hiphop", "rap", "trap", "drill", "freestyle", "cypher", "boom bap").any { text.contains(it) }) add("hip hop")
        if (listOf("rock", "grunge", "punk", "metal", "guitar", "hard rock").any { text.contains(it) }) add("rock")
        if (listOf("edm", "electronic", "house music", "deep house", "techno", "trance", "dubstep", "synthwave", "electro").any { text.contains(it) }) add("electronic")
        if (listOf("classical", "sitar", "tabla", "raga", "ghazal", "qawwali", "sufi", "carnatic").any { text.contains(it) }) add("indian classical")
        if (listOf("acoustic", "unplugged", "folk", "singer-songwriter").any { text.contains(it) }) add("acoustic")
        if (listOf("blues", "jazz", "soul", "r&b", "rhythm & blues").any { text.contains(it) }) add("blues/soul")
        if (listOf("devotional", "bhajan", "bhakti", "gurbani", "bhajan mandir").any { text.contains(it) }) add("devotional")
    }
}
