package com.example.myplayer.data.recommendation.strategy

/**
 * Extracts language / mood / genre signals from song text (title, artist, album).
 * Pure Kotlin, zero network, zero Android dependency.
 *
 * Why this was rewritten: the old version only fired on literal keywords such as "bollywood",
 * "hindi" or "happy". Real YouTube titles ("Gallan Goodiyan", "Kesariya - Arijit Singh") contain
 * none of them, so the context came out EMPTY and the continuity logic silently did nothing.
 *
 * Language now comes from, in order of strength:
 *   1. Script (Devanagari / Gurmukhi / Tamil / Telugu / Arabic / Hangul)
 *   2. Explicit words (bollywood, hindi, hollywood ...)
 *   3. Known artist / label lists (Indian, Western, Latin, K-pop)
 *   4. Hinglish vocabulary (tere, mere, dil, pyaar, kya, nahi ...)
 *   5. "Looks like plain English" heuristic (only if nothing Indian was found)
 *
 * Tags: "indian" (umbrella for Hindi/Punjabi/Tamil/Telugu ...), "western", "latin", "korean",
 * "arabic" plus regional extras ("punjabi", "tamil", "telugu").
 */
object SongContextClassifier {

    data class SongContext(
        val language: Set<String> = emptySet(),
        val mood: Set<String> = emptySet(),
        val genre: Set<String> = emptySet()
    ) {
        val isEmpty: Boolean get() = language.isEmpty() && mood.isEmpty() && genre.isEmpty()
    }

    // ---- tokenizer (declared first: the keyword lists below are normalised with it) ----
    private val SPLIT = Regex("[^\\p{L}\\p{M}\\p{N}']+")
    private val BRACKETS = Regex("\\([^)]*\\)|\\[[^\\]]*\\]")

    private fun tokens(s: String): List<String> =
        s.lowercase().split(SPLIT).map { it.replace("'", "") }.filter { it.isNotEmpty() }

    private fun padded(s: String): String = " " + tokens(s).joinToString(" ") + " "

    private fun norm(vararg words: String): List<String> =
        words.map { " " + tokens(it).joinToString(" ") + " " }

    private fun hits(p: String, kws: List<String>): Boolean = kws.any { p.contains(it) }

    // ---- language lexicons ----
    private val NOISE = setOf(
        "official", "video", "audio", "lyrics", "lyrical", "lyric", "full", "song", "songs", "hd", "hq",
        "4k", "ft", "feat", "featuring", "from", "movie", "film", "version", "music", "new", "latest",
        "vevo", "topic", "visualizer", "mv", "remastered"
    )

    /** Hinglish words that are very unlikely in an English title: ONE hit is enough. */
    private val HI_STRONG = setOf(
        "tere", "tera", "teri", "mere", "mera", "meri", "tumhe", "tumhara", "tumse", "mujhe", "mujhko",
        "tujhe", "tujhko", "tujhse", "mujhse", "dil", "dilbar", "pyar", "pyaar", "ishq", "ishqa",
        "mohabbat", "muhabbat", "jaan", "jaana", "zindagi", "kya", "nahi", "nahin", "kabhi", "abhi",
        "bhi", "hai", "hain", "hoon", "sajna", "sajan", "sanam", "balam", "piya", "saiyaan", "saiyan",
        "yaara", "yaar", "yaaron", "dosti", "duniya", "aankhen", "aankhon", "nazar", "nazrein", "raat",
        "baarish", "barsaat", "mausam", "kaise", "kyun", "kyu", "kuch", "sabse", "mein", "tujh", "sirf",
        "bina", "rab", "khuda", "rangeen", "kudi", "munda", "jatt", "gabru", "nasha", "sharab", "daru",
        "mahi", "mahiya", "jugni", "dhol", "bhangra", "naach", "nacho", "masti", "dhamaal", "gallan",
        "goodiyan", "badtameez", "chashma", "aaj", "phir", "chhod", "kal", "tumhi", "dhun", "ghar", "wapas"
    )

    /** Ambiguous short words: need TWO different hits. */
    private val HI_WEAK = setOf(
        "hum", "tum", "aap", "tu", "ve", "tha", "thi", "ek", "yeh", "ye", "woh", "wo", "jo", "ki", "ka",
        "ke", "ko", "se", "par", "pe", "aaja"
    )

    private val INDIAN_ARTISTS = listOf(
        "arijit singh", "pritam", "atif aslam", "neha kakkar", "shreya ghoshal", "sonu nigam", "badshah",
        "honey singh", "yo yo honey singh", "jubin nautiyal", "b praak", "darshan raval", "armaan malik",
        "vishal mishra", "mohit chauhan", "kk", "kishore kumar", "lata mangeshkar", "mohammed rafi",
        "asha bhosle", "udit narayan", "kumar sanu", "alka yagnik", "sunidhi chauhan", "shaan",
        "sukhwinder singh", "ar rahman", "a r rahman", "amit trivedi", "vishal shekhar", "salim sulaiman",
        "benny dayal", "shankar mahadevan", "mika singh", "guru randhawa", "diljit dosanjh", "ap dhillon",
        "karan aujla", "sidhu moose wala", "tulsi kumar", "dhvani bhanushali", "palak muchhal",
        "rahat fateh ali khan", "nusrat fateh ali khan", "ayushmann khurrana", "anirudh ravichander",
        "sachin jigar", "tanishk bagchi", "mithoon", "ankit tiwari", "kanika kapoor", "jonita gandhi",
        "t series", "zee music company", "tips official", "sony music india", "yrf", "saregama music",
        "eros now music", "speed records", "white hill music", "times music", "tips music"
    )

    private val WESTERN_ARTISTS = listOf(
        "taylor swift", "ed sheeran", "the weeknd", "weeknd", "dua lipa", "justin bieber", "ariana grande",
        "billie eilish", "eminem", "drake", "coldplay", "imagine dragons", "maroon 5", "bruno mars",
        "post malone", "adele", "rihanna", "katy perry", "lady gaga", "selena gomez", "charlie puth",
        "shawn mendes", "sia", "linkin park", "the chainsmokers", "chainsmokers", "marshmello",
        "alan walker", "calvin harris", "david guetta", "avicii", "kygo", "martin garrix", "one direction",
        "harry styles", "sam smith", "lewis capaldi", "olivia rodrigo", "doja cat", "travis scott",
        "kanye west", "kendrick lamar", "50 cent", "jay z", "beyonce", "nirvana", "queen", "metallica",
        "ac dc", "guns n roses", "the beatles", "michael jackson", "madonna", "lana del rey", "khalid",
        "lil nas x", "juice wrld", "xxxtentacion", "the kid laroi", "glass animals", "sabrina carpenter",
        "camila cabello", "rema", "tones and i", "david kushner", "miley cyrus", "sza", "ellie goulding"
    )

    private val LATIN_ARTISTS = listOf(
        "shakira", "bad bunny", "daddy yankee", "luis fonsi", "j balvin", "ozuna", "maluma", "karol g", "anitta"
    )

    private val KOREAN_ARTISTS = listOf(
        "bts", "blackpink", "twice", "stray kids", "psy", "newjeans", "exo", "seventeen", "aespa"
    )

    private val ENGLISH = setOf(
        "the", "a", "an", "and", "or", "of", "to", "in", "on", "at", "for", "with", "you", "your", "me", "my",
        "i", "it", "is", "are", "was", "be", "we", "us", "they", "he", "she", "him", "her", "love", "baby",
        "girl", "boy", "night", "day", "time", "life", "heart", "world", "like", "just", "dont", "cant", "im",
        "ive", "youre", "wont", "all", "one", "no", "not", "so", "if", "up", "down", "out", "back", "now",
        "never", "ever", "over", "again", "away", "better", "good", "bad", "feel", "feeling", "know", "want",
        "need", "make", "take", "give", "get", "got", "go", "going", "come", "home", "mind", "eyes", "dream",
        "dreams", "fire", "rain", "light", "dark", "sky", "sun", "moon", "stars", "say", "tell", "think",
        "thing", "things", "call", "run", "stay", "wait", "fall", "falling", "dance", "dancing", "summer",
        "tonight", "forever", "without", "believe", "hold", "touch", "burn", "break", "broken", "lose", "lost",
        "found", "changes", "people", "someone", "somebody", "nothing", "everything", "by", "as", "that",
        "this", "what", "when", "where", "why", "how", "who", "whats", "let", "lets", "till", "until",
        "through", "into", "around", "under", "between", "after", "before", "more", "most", "than", "too",
        "very", "can", "could", "would", "should", "will"
    )

    private val INDIAN_REGIONAL = setOf("punjabi", "tamil", "telugu")

    // ---- mood / genre lexicons (token-matched, so "dj" no longer matches "adjust") ----
    private val HAPPY_KW = norm(
        "happy", "khushi", "khush", "celebration", "feel good", "upbeat", "positive", "masti", "zindagi",
        "dhamaal", "balle", "gallan", "jashn", "mauj", "dhoom", "badtameez", "swag"
    )
    private val PARTY_KW = norm(
        "party", "dance", "naach", "nacho", "club", "dj", "remix", "edm", "bass", "disco", "groove", "rave",
        "nonstop", "non stop", "mashup", "bhangra", "wedding", "shaadi", "sangeet", "mehendi", "baraat",
        "holi", "garba", "dandiya"
    )
    private val SAD_KW = norm(
        "sad", "heartbreak", "heart break", "broken", "cry", "alone", "pain", "lonely", "bewafa", "dard",
        "judaai", "judai", "tanha", "tanhai", "alvida", "aansu", "gham", "toota", "bichad", "bichhad"
    )
    private val ROMANTIC_KW = norm("romantic", "love", "pyar", "pyaar", "ishq", "dil", "sajan", "junoon", "valentine")
    private val CHILL_KW = norm("chill", "lofi", "lo-fi", "relax", "calm", "sleep", "ambient", "soft", "peace")
    private val WORKOUT_KW = norm("workout", "gym", "fitness", "energy", "power", "motivation", "beast", "training", "pump")
    private val FOCUS_KW = norm("focus", "study", "concentration", "instrumental", "binaural", "deep work")

    private val UPBEAT = setOf("happy", "party", "workout")
    private val SLOW = setOf("sad", "chill", "focus")

    private val GENRE_FILMY = norm("bollywood", "desi", "filmy", "film song")
    private val GENRE_POP = norm("pop", "chart", "billboard", "top 40")
    private val GENRE_HIPHOP = norm("hip hop", "hiphop", "rap", "trap", "drill", "freestyle", "cypher", "boom bap")
    private val GENRE_ROCK = norm("rock", "grunge", "punk", "metal", "guitar", "hard rock")
    private val GENRE_ELECTRONIC = norm("edm", "electronic", "house music", "deep house", "techno", "trance", "dubstep", "synthwave", "electro")
    private val GENRE_CLASSICAL = norm("classical", "sitar", "tabla", "raga", "ghazal", "qawwali", "sufi", "carnatic")
    private val GENRE_ACOUSTIC = norm("acoustic", "unplugged", "folk", "singer-songwriter")
    private val GENRE_BLUES = norm("blues", "jazz", "soul", "r&b", "rhythm & blues")
    private val GENRE_DEVOTIONAL = norm(
        "devotional", "bhajan", "bhakti", "gurbani", "bhajan mandir", "shabad", "kirtan", "satsang",
        "aarti", "mantra", "chalisa", "jaap",
        "naat", "naat sharif", "nasheed", "nasheeds", "hamd", "qasida", "manqabat", "noha", "marsiya",
        "milad", "islamic", "muslim", "allah", "muhammad", "mohammad", "madina", "madine", "ya ali",
        "ya hussain", "muharram", "ramzan", "ramadan", "azan", "quran", "surah", "tilawat"
    )

    /** Artists whose catalogue is (almost) entirely religious. */
    private val RELIGIOUS_ARTISTS = listOf(
        "maher zain", "sami yusuf", "junaid jamshed", "owais raza qadri", "muhammad owais raza qadri",
        "hafiz tahir qadri", "amjad sabri", "sabri brothers", "mishary rashid", "alafasy", "anuradha paudwal"
    )

    // ---- public API ----

    /** Back-compat entry point: parts = [title, artist, album]. */
    fun analyze(vararg textParts: String?): SongContext =
        analyzeSong(
            textParts.getOrNull(0).orEmpty(),
            textParts.getOrNull(1).orEmpty(),
            textParts.getOrNull(2).orEmpty()
        )

    fun analyzeSong(title: String, artist: String, album: String = ""): SongContext {
        val all = "$title $artist $album".trim()
        if (all.isEmpty()) return SongContext()
        return SongContext(
            language = languageOf(title, artist, album),
            mood = moodOf(all),
            genre = genreOf(all, artist)
        )
    }

    /** True when both songs have a language and they share none (e.g. Bollywood vs Hollywood). */
    fun languageConflict(a: SongContext, b: SongContext): Boolean =
        a.language.isNotEmpty() && b.language.isNotEmpty() && a.language.intersect(b.language).isEmpty()

    /** Religious / devotional content (naat, nasheed, bhajan, aarti, gurbani ...). */
    fun isDevotional(c: SongContext): Boolean = "devotional" in c.genre

    /** True when one side is clearly upbeat and the other clearly slow/sad (happy -> sad shift). */
    fun moodOpposes(a: Set<String>, b: Set<String>): Boolean {
        val aUp = a.any { it in UPBEAT }
        val aSlow = a.any { it in SLOW }
        val bUp = b.any { it in UPBEAT }
        val bSlow = b.any { it in SLOW }
        return (aUp && bSlow && !bUp) || (aSlow && bUp && !bSlow)
    }

    // ---- internals ----

    private fun languageOf(title: String, artist: String, album: String): Set<String> {
        val raw = "$title $artist $album".lowercase()
        val all = padded(raw)
        var ind = 0
        var west = 0
        val extra = mutableSetOf<String>()

        // 1. Script
        if (raw.any { it in '\u0900'..'\u097F' }) ind += 3                                   // Devanagari
        if (raw.any { it in '\u0A00'..'\u0A7F' }) { ind += 3; extra += "punjabi" }           // Gurmukhi
        if (raw.any { it in '\u0B80'..'\u0BFF' }) { ind += 3; extra += "tamil" }             // Tamil (old range was inverted = never matched)
        if (raw.any { it in '\u0C00'..'\u0C7F' }) { ind += 3; extra += "telugu" }            // Telugu
        if (raw.any { it in '\u0600'..'\u06FF' }) extra += "arabic"
        if (raw.any { it in '\uAC00'..'\uD7AF' }) extra += "korean"

        // 2. Explicit words
        if (hits(all, norm("bollywood", "hindi", "desi", "gaana", "indian song", "hindi gaana"))) ind += 3
        if (hits(all, norm("hollywood", "english song", "western song"))) west += 3
        if (hits(all, norm("punjabi", "punjab", "gurmukhi"))) { ind += 3; extra += "punjabi" }
        if (hits(all, norm("tollywood", "telugu"))) { ind += 3; extra += "telugu" }
        if (hits(all, norm("kollywood", "tamil song"))) { ind += 3; extra += "tamil" }
        if (hits(all, norm("arabic", "arab"))) extra += "arabic"
        if (hits(all, norm("spanish", "reggaeton", "latin music"))) extra += "latin"

        // 3. Artist / label lists (artist field only)
        val artistPadded = padded(artist)
        if (INDIAN_ARTISTS.any { artistPadded.contains(" $it ") }) ind += 3
        if (WESTERN_ARTISTS.any { artistPadded.contains(" $it ") }) west += 3
        if (LATIN_ARTISTS.any { artistPadded.contains(" $it ") }) extra += "latin"
        if (KOREAN_ARTISTS.any { artistPadded.contains(" $it ") }) extra += "korean"

        // 4. Hinglish vocabulary in the title (movie/version brackets removed, noise words dropped)
        val core = tokens(BRACKETS.replace(title.lowercase(), " ")).filter { it !in NOISE }
        val coreSet = core.toSet()
        val strong = coreSet.count { it in HI_STRONG }
        val weak = coreSet.count { it in HI_WEAK }
        ind += minOf(strong, 2) * 2
        if (weak >= 2) ind += 1

        // 5. Plain-English heuristic, only when nothing Indian was found
        if (ind == 0 && core.size >= 2 && core.count { it in ENGLISH }.toDouble() / core.size >= 0.6) west += 2

        val out = mutableSetOf<String>()
        if (ind >= 2 && ind >= west) out += "indian"
        else if (west >= 2 && west > ind) out += "western"
        out += extra
        if (extra.any { it in INDIAN_REGIONAL }) out += "indian"
        return out
    }

    private fun moodOf(text: String): Set<String> {
        val p = padded(text)
        return buildSet {
            if (hits(p, HAPPY_KW)) add("happy")
            if (hits(p, PARTY_KW)) add("party")
            if (hits(p, SAD_KW)) add("sad")
            if (hits(p, ROMANTIC_KW)) add("romantic")
            if (hits(p, CHILL_KW)) add("chill")
            if (hits(p, WORKOUT_KW)) add("workout")
            if (hits(p, FOCUS_KW)) add("focus")
        }
    }

    private fun genreOf(text: String, artist: String = ""): Set<String> {
        val p = padded(text)
        val artistPadded = padded(artist)
        return buildSet {
            if (hits(p, GENRE_FILMY)) add("filmy")
            if (hits(p, GENRE_POP)) add("pop")
            if (hits(p, GENRE_HIPHOP)) add("hip hop")
            if (hits(p, GENRE_ROCK)) add("rock")
            if (hits(p, GENRE_ELECTRONIC)) add("electronic")
            if (hits(p, GENRE_CLASSICAL)) add("indian classical")
            if (hits(p, GENRE_ACOUSTIC)) add("acoustic")
            if (hits(p, GENRE_BLUES)) add("blues/soul")
            if (hits(p, GENRE_DEVOTIONAL) || RELIGIOUS_ARTISTS.any { artistPadded.contains(" $it ") }) add("devotional")
        }
    }
}
