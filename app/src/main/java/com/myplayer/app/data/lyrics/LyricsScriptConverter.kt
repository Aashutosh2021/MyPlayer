package com.myplayer.app.data.lyrics

import android.icu.text.Transliterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** What the user picks in the lyrics screen. */
enum class LyricsScript(val label: String) {
    ORIGINAL("Original"), HINGLISH("Hinglish"), HINDI("हिन्दी"), URDU("اردو")
}

/** What the lyrics are written in. English lyrics are never converted. */
enum class DetectedScript { DEVANAGARI, URDU, HINGLISH, ENGLISH, UNKNOWN;
    val convertible: Boolean get() = this == DEVANAGARI || this == URDU || this == HINGLISH
}

/**
 * Hindi (Devanagari) <-> Hinglish (Latin) <-> Urdu (Arabic script) for song lyrics.
 *  - Devanagari -> Latin: offline (ICU + Hinglish clean-up: schwa drop, ā->a, ṁ->n ...)
 *  - Latin -> Devanagari / Urdu: Google Input Tools (online, word-aware); Devanagari falls back to ICU offline
 *  - Urdu -> Latin: offline ICU; Urdu does not write short vowels, so this direction is approximate
 */
@Singleton
class LyricsScriptConverter @Inject constructor(private val client: OkHttpClient) {

    private val cache = ConcurrentHashMap<String, String>()
    private val devaToLatin by lazy { Transliterator.getInstance("Devanagari-Latin") }
    private val latinToDeva by lazy { Transliterator.getInstance("Latin-Devanagari") }
    private val latinAscii by lazy { Transliterator.getInstance("Latin-ASCII") }
    private val arabicToLatin by lazy { Transliterator.getInstance("Arabic-Latin; Latin-ASCII") }

    fun detect(lines: List<String>): DetectedScript {
        var deva = 0; var arab = 0; var latin = 0
        val words = ArrayList<String>()
        for (line in lines) for (ch in line) when {
            ch in '\u0900'..'\u097F' -> deva++
            ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' || ch in '\uFB50'..'\uFDFF' || ch in '\uFE70'..'\uFEFF' -> arab++
            ch in 'a'..'z' || ch in 'A'..'Z' -> latin++
        }
        val total = deva + arab + latin
        if (total < 20) return DetectedScript.UNKNOWN
        if (deva > total * 0.3 && deva >= arab) return DetectedScript.DEVANAGARI
        if (arab > total * 0.3) return DetectedScript.URDU
        for (line in lines) for (w in WORD.findAll(line.lowercase())) words += w.value
        if (words.size < 8) return DetectedScript.UNKNOWN
        val hits = words.count { it in HINGLISH_WORDS }
        return if (hits >= 4 && hits >= words.size * 0.12) DetectedScript.HINGLISH else DetectedScript.ENGLISH
    }

    /** Same number of lines back; a line that fails to convert stays as it was. */
    suspend fun convert(lines: List<String>, from: DetectedScript, target: LyricsScript): List<String> {
        if (target == LyricsScript.ORIGINAL || !from.convertible) return lines
        val gate = Semaphore(6)
        return coroutineScope {
            lines.map { line -> async(Dispatchers.IO) { gate.withPermit { convertLine(line, from, target) } } }.awaitAll()
        }
    }

    private suspend fun convertLine(line: String, from: DetectedScript, target: LyricsScript): String {
        if (line.isBlank() || TAG.matches(line.trim())) return line
        val key = "$from|$target|$line"
        cache[key]?.let { return it }
        val out = runCatching {
            when (from) {
                DetectedScript.DEVANAGARI -> when (target) {
                    LyricsScript.HINDI -> line
                    LyricsScript.HINGLISH -> hindiToHinglish(line)
                    else -> toIndic(hindiToHinglish(line), "ur") ?: line
                }
                DetectedScript.HINGLISH -> when (target) {
                    LyricsScript.HINGLISH -> line
                    LyricsScript.HINDI -> toIndic(line, "hi") ?: latinToDeva.transliterate(line)
                    else -> toIndic(line, "ur") ?: line
                }
                DetectedScript.URDU -> {
                    val latin = urduToHinglish(line)
                    when (target) {
                        LyricsScript.HINGLISH -> latin
                        LyricsScript.HINDI -> toIndic(latin, "hi") ?: latinToDeva.transliterate(latin)
                        else -> line
                    }
                }
                else -> line
            }
        }.getOrDefault(line)
        if (cache.size > 6000) cache.clear()
        cache[key] = out
        return out
    }

    // ---- Devanagari -> Hinglish ----
    private fun hindiToHinglish(line: String): String {
        val latin = devaToLatin.transliterate(line)
        return WORD_LATIN.replace(latin) { m -> hinglishWord(m.value) }
    }

    private fun hinglishWord(raw: String): String {
        var w = raw.lowercase()
        // a word ending in a short "a" after a consonant lost its vowel in speech: pyāra -> pyār
        if (w.length > 2 && w.last() == 'a' && w[w.length - 2] !in VOWELS) w = w.dropLast(1)
        w = w.replace("m̐", "n").replace("ṁ", "n").replace("ṃ", "n").replace("ṅ", "n").replace("ñ", "n")
            .replace("ṇ", "n").replace("ṭ", "t").replace("ḍ", "d").replace("ṛ", "r").replace("ś", "sh")
            .replace("ṣ", "sh").replace("ḥ", "h").replace("ṝ", "ri")
            .replace("ā", "a").replace("ī", "i").replace("ū", "u").replace("ē", "e").replace("ō", "o")
        w = latinAscii.transliterate(w)
        return FIXES[w] ?: w
    }

    // ---- Urdu -> Hinglish (approximate: short vowels are not written) ----
    private fun urduToHinglish(line: String): String {
        val latin = arabicToLatin.transliterate(line).lowercase()
        return WORD_LATIN.replace(latin) { m ->
            val w = m.value
            // no vowel at all: a consonant skeleton, give it an "a" between consonants
            if (w.length > 1 && w.none { it in VOWELS }) w.dropLast(1).map { "${it}a" }.joinToString("") + w.last() else w
        }
    }

    // ---- Latin -> Hindi / Urdu through Google Input Tools ----
    private fun toIndic(text: String, lang: String): String? {
        val url = "https://inputtools.google.com/request?text=${URLEncoder.encode(text, "UTF-8")}" +
            "&itc=$lang-t-i0-und&num=1&cp=0&cs=1&ie=utf-8&oe=utf-8&app=test"
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val root = JSONArray(resp.body?.string() ?: return@use null)
                if (root.optString(0) != "SUCCESS") return@use null
                root.getJSONArray(1).getJSONArray(0).getJSONArray(1).optString(0).takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null // offline / blocked: callers fall back
        }
    }

    private companion object {
        val WORD = Regex("[a-z']+")
        val WORD_LATIN = Regex("[\\p{L}\\u0310\\u0307\\u0323]+")
        val TAG = Regex("""\[[^\]]*\]|\([^)]*\)""")
        const val VOWELS = "aeiouāīūēō"
        val FIXES = mapOf("men" to "mein", "nahin" to "nahi")

        /** Hinglish words that are not English words (so "to", "me", "do", "main" are left out on purpose). */
        val HINGLISH_WORDS = setOf(
            "hai", "hain", "tha", "thi", "ho", "hoon", "hu", "hoga", "hogi", "tu", "tum", "tera", "teri", "tere",
            "mera", "meri", "mere", "hum", "humne", "humein", "tumhe", "tumhari", "tumhara", "mujhe", "mujhko", "mein",
            "mai", "ka", "ki", "ke", "ko", "se", "pe", "par", "aur", "na", "nahi", "nahin", "mat", "kya", "kyun", "kyu",
            "kaise", "kab", "kahan", "kaun", "jab", "tab", "jo", "woh", "wo", "yeh", "ye", "ab", "abhi", "phir", "bas",
            "sirf", "bhi", "toh", "lekin", "magar", "agar", "dil", "pyar", "pyaar", "ishq", "mohabbat", "jaan",
            "zindagi", "zindagi", "dard", "khushi", "gham", "yaad", "yaadein", "raat", "din", "subah", "aankhen", "nazar",
            "chehra", "saath", "sath", "mil", "milna", "mile", "jaana", "jaa", "jaaye", "aaja", "aaye", "aana", "aa",
            "kar", "karna", "kiya", "karte", "karo", "de", "dena", "diya", "le", "lena", "liya", "chal", "chalo",
            "dekh", "dekho", "dekha", "bol", "bolo", "suno", "sun", "kuch", "koi", "sab", "sabse", "bahut", "bohot",
            "accha", "acha", "bura", "naya", "purana", "dost", "yaar", "sanam", "sajna", "saajan", "rab", "khuda",
            "duniya", "waqt", "waqt", "baatein", "baat", "kal", "aaj", "ek", "teen", "bina", "tak", "liye",
            "wala", "wali", "rahe", "raha", "rahi", "rehna", "reh", "gaya", "gayi", "gaye", "hua", "hui", "hue",
        )
    }
}
