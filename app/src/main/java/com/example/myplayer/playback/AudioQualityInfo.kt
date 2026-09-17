package com.example.myplayer.playback

/**
 * Encapsulates audio format and stream quality metadata,
 * inspired by audiophile standards in LastWave.
 */
data class AudioQualityInfo(
    val format: String = "AUTO",
    val bitDepth: Int? = null,
    val sampleRateHz: Int? = null,
    val bitrateKbps: Int? = null,
    val isLossless: Boolean = false,
    val sourceName: String = "Online Stream"
) {
    val displayBadge: String
        get() = when {
            isLossless && (bitDepth != null && bitDepth >= 24 || (sampleRateHz != null && sampleRateHz > 48000)) ->
                "HI-RES LOSSLESS"
            isLossless ->
                "LOSSLESS"
            format.equals("OPUS", ignoreCase = true) ->
                "HQ OPUS"
            format.equals("MP3", ignoreCase = true) && (bitrateKbps ?: 0) >= 320 ->
                "HQ 320K"
            else ->
                "STANDARD"
        }

    val technicalSummary: String
        get() = buildString {
            append(format.uppercase())
            if (bitDepth != null) append(" • ${bitDepth}-bit")
            if (sampleRateHz != null) {
                val khz = sampleRateHz / 1000.0
                append(" • ${if (khz % 1.0 == 0.0) khz.toInt().toString() else "%.1f".format(khz)} kHz")
            }
            if (bitrateKbps != null) append(" • ${bitrateKbps} kbps")
        }
}
