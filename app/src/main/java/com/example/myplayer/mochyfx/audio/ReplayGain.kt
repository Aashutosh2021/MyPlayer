package com.example.myplayer.mochyfx.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import kotlin.math.min
import kotlin.math.pow

enum class ReplayGainMode { Track, Album }

/** how replaygain is applied: which of the file's two gains, and a preamp on top */
data class ReplayGainParams(
    val mode: ReplayGainMode = ReplayGainMode.Track,
    /** monochrome's default: replaygain's 89 db reference sits a little quiet for phones */
    val preampDb: Float = 3f,
)

/** one player's song replaygain from its file (monochrome flacs, many local files), read from vorbis comments on track change; it can only bring a song down and never takes a peak past full scale */
class ReplayGainTags {
    @Volatile var trackGain: Float? = null
        private set
    @Volatile var trackPeak: Float? = null
        private set
    @Volatile var albumGain: Float? = null
        private set
    @Volatile var albumPeak: Float? = null
        private set

    val present: Boolean get() = trackGain != null || albumGain != null

    @OptIn(UnstableApi::class)
    fun update(tracks: Tracks) {
        val comments = ArrayList<Pair<String, String>>()
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                if (!group.isTrackSelected(i)) continue
                val metadata = group.getTrackFormat(i).metadata ?: continue
                for (e in 0 until metadata.length()) {
                    when (val entry = metadata.get(e)) {
                        is VorbisComment -> comments += entry.key to entry.value
                        // mp3: REPLAYGAIN_* live in ID3 TXXX frames
                        is TextInformationFrame -> if (entry.id == "TXXX") {
                            comments += entry.description.orEmpty() to entry.values.firstOrNull().orEmpty()
                        }
                        else -> Unit
                    }
                }
            }
        }
        update(comments)
    }

    /** from the song's vorbis comments (key, value); a song without any clears the last one's */
    fun update(comments: List<Pair<String, String>>) {
        var tg: Float? = null
        var tp: Float? = null
        var ag: Float? = null
        var ap: Float? = null
        for ((key, text) in comments) {
            val value = number(text) ?: continue
            when (key.uppercase()) {
                "REPLAYGAIN_TRACK_GAIN" -> tg = value
                "REPLAYGAIN_TRACK_PEAK" -> tp = value
                "REPLAYGAIN_ALBUM_GAIN" -> ag = value
                "REPLAYGAIN_ALBUM_PEAK" -> ap = value
            }
        }
        trackGain = tg
        trackPeak = tp
        albumGain = ag
        albumPeak = ap
    }

    /** the volume factor for params; 1 when there's nothing to apply */
    fun linear(params: ReplayGainParams, normalize: Boolean): Float {
        if (!normalize) return 1f
        val album = params.mode == ReplayGainMode.Album
        val gain = (if (album) albumGain ?: trackGain else trackGain ?: albumGain) ?: return 1f
        val peak = if (album) albumPeak ?: trackPeak else trackPeak ?: albumPeak
        var factor = 10f.pow((gain + params.preampDb) / 20f)
        if (peak != null && peak > 0f) factor = min(factor, 1f / peak)
        return factor.coerceIn(0.03f, 1f)
    }

    companion object {
        /** "-8.89 db", "1.000000", "+2.1 db" */
        internal fun number(text: String): Float? =
            Regex("""[-+]?\d+(?:\.\d+)?""").find(text)?.value?.toFloatOrNull()
    }
}
