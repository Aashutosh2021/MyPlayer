package com.myplayer.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.audioPlaybackDataStore by preferencesDataStore(name = "audio_playback_master_prefs")

enum class AudioQualityTier(
    val title: String,
    val badge: String,
    val subtitle: String,
    val qobuzFormatId: Int?
) {
    DOLBY_ATMOS("Dolby Atmos", "ATMOS", "Spatial Immersive Audio • Master Audio", 28),
    HI_RES_MAX("Hi-Res Audio (Max)", "24-BIT / 192k", "Up to 24-bit / 192 kHz • Lossless Studio FLAC", 27),
    HI_RES_96("Hi-Res Audio", "24-BIT / 96k", "24-bit / 96 kHz • Lossless Studio FLAC", 7),
    CD_LOSSLESS("CD Lossless", "16-BIT / 44.1k", "16-bit / 44.1 kHz • Lossless CD FLAC", 6),
    STANDARD_320("Standard Quality", "320 kbps", "320 kbps • MP3 / AAC", 5),
    DATA_SAVER("Data Saver", "96 kbps", "96 kbps • High Efficiency AAC", 4),
    YOUTUBE_MUSIC("YouTube Music", "YOUTUBE", "128-256 kbps • YouTube Music AAC / Opus stream", -1)
}

enum class LoudnessMode(val id: String, val title: String, val description: String) {
    OFF("off", "Off", "Play tracks at their original level"),
    TRACK("track", "Track", "Match every track to -14 LUFS (needs ReplayGain tags)"),
    ALBUM("album", "Album", "Keep intentional album dynamics (falls back to track tags)")
}

enum class SpatialMode(val title: String, val description: String) {
    SPEAKERS("Virtual 3D Speakers", "Simulates external stereo speakers ±30° in front of you"),
    CROSSFEED("BS2B Crossfeed", "Blends binaural channels to eliminate headphone fatigue")
}

enum class ClarityOutputPreset(
    val index: Int,
    val key: String,
    val title: String,
    val description: String,
    val trimsDb: FloatArray
) {
    REFERENCE(0, "reference", "Reference", "Unmodified shipping curve; every stage trim is 0 dB.", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    SPEAKER(1, "speaker", "Speaker", "Reduced sub-bass lift (-1.5 dB) and gentler air (-0.5 dB) for small drivers.", floatArrayOf(0f, -1.5f, 0f, 0f, 0f, -0.5f, 0f, 0f)),
    HEADPHONE(2, "headphone", "Headphone", "Lighter bass (-0.5 dB) and softer presence (-1.0 dB) for close-coupled drivers.", floatArrayOf(0f, -0.5f, 0f, 0f, -1.0f, 0f, 0f, 0f)),
    DAC(3, "dac", "DAC", "Gentler air (-1.5 dB shelf, -1.0 dB exciter) for revealing chains.", floatArrayOf(0f, 0f, 0f, 0f, 0f, -1.5f, 0f, -1.0f))
}

enum class Eq15Preset(val label: String, val gainsDb: FloatArray) {
    // 15 Standard ISO 2/3-octave center frequencies: 25, 40, 63, 100, 160, 250, 400, 630, 1000, 1600, 2500, 4000, 6300, 10000, 16000 Hz
    DEFAULT("Default", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    STUDIO_MASTER("Studio Master", floatArrayOf(2.6f, 2.8f, 2.2f, 0.6f, -1.8f, -2.6f, -1.2f, 0.0f, 1.2f, 2.4f, 3.6f, 4.0f, 4.2f, 4.5f, 4.8f)),
    ACOUSTIC("Acoustic", floatArrayOf(3.0f, 2.5f, 2.0f, 1.5f, 0.5f, 1.0f, 1.5f, 2.0f, 2.0f, 1.5f, 1.0f, 1.0f, 1.0f, 1.5f, 1.5f)),
    VOCAL_CLARITY("Vocal Clarity", floatArrayOf(-1.5f, -1.0f, -0.5f, 0f, 0.5f, 1.0f, 2.0f, 3.0f, 3.5f, 3.0f, 2.5f, 2.0f, 1.5f, 1.5f, 1.0f)),
    CLASSICAL("Classical", floatArrayOf(3.0f, 2.5f, 2.0f, 1.5f, 0.5f, 0f, 0f, 0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.0f)),
    JAZZ("Jazz", floatArrayOf(2.5f, 2.0f, 1.5f, 1.0f, 0f, -0.5f, 0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.0f, 2.5f, 2.5f, 2.5f)),
    ROCK("Rock", floatArrayOf(4.0f, 3.5f, 3.0f, 1.5f, 0f, -1.0f, -1.0f, -0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f, 4.0f, 4.0f)),
    POP("Pop", floatArrayOf(-1.0f, -0.5f, 0.0f, 1.0f, 1.5f, 2.0f, 2.0f, 1.5f, 1.0f, 0.5f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f)),
    TREBLE_AIR("Treble Air", floatArrayOf(0f, 0f, 0f, -0.5f, -0.5f, -0.5f, 0f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 3.5f, 4.5f, 5.0f)),
    DEEP_BASS_CLEAN("Deep Bass Clean", floatArrayOf(5.0f, 4.5f, 3.5f, 2.5f, 1.0f, 0f, -0.5f, -0.5f, 0f, 0f, 0f, 0f, 0.5f, 0.5f, 0.5f)),
    ELECTRONIC("Electronic", floatArrayOf(4.5f, 4.0f, 3.0f, 2.0f, 0.5f, -0.5f, 0f, 1.0f, 2.0f, 2.5f, 2.5f, 2.5f, 3.0f, 3.5f, 4.0f)),
    RNB("R&B", floatArrayOf(5.0f, 4.5f, 3.5f, 2.0f, 1.0f, -0.5f, -1.0f, -0.5f, 0.5f, 1.5f, 2.0f, 2.5f, 3.0f, 3.0f, 3.0f))
}

data class AudioPlaybackState(
    val streamingQuality: AudioQualityTier = AudioQualityTier.DOLBY_ATMOS,
    val downloadQuality: AudioQualityTier = AudioQualityTier.DOLBY_ATMOS,
    val bitPerfectMode: Boolean = false,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 6,
    val keepScreenAwakeLyrics: Boolean = false,
    val downloadSyncedLyrics: Boolean = true,
    val usbExclusiveOutput: Boolean = false,
    val internalDirectOutput: Boolean = false,
    val softwareVolumeUsb: Boolean = false,
    val loudnessMode: LoudnessMode = LoudnessMode.OFF,
    val eqEnabled: Boolean = false,
    val eqPreset: Eq15Preset = Eq15Preset.STUDIO_MASTER,
    val eqBandGains: List<Float> = Eq15Preset.STUDIO_MASTER.gainsDb.toList(),
    val systemAudioEffects: Boolean = false,
    val studioMasterClarity: Boolean = true,
    val clarityOutputPreset: ClarityOutputPreset = ClarityOutputPreset.REFERENCE,
    val claritySpatialBypass: Boolean = false,
    val dolbySurroundAtmos: Boolean = false,
    val spatialAudioEnabled: Boolean = false,
    val spatialMode: SpatialMode = SpatialMode.SPEAKERS,
    val spatialSpeakerAngle: Float = 30f,
    val spatialAmount: Float = 1.0f
)

object AudioQualityFallbackHelper {
    fun getQualityFallbackChain(selectedTier: AudioQualityTier): List<AudioQualityTier> {
        return when (selectedTier) {
            // Strictly plays YouTube Music as-is (never upgrades)
            AudioQualityTier.YOUTUBE_MUSIC -> listOf(AudioQualityTier.YOUTUBE_MUSIC)
            AudioQualityTier.DATA_SAVER -> listOf(AudioQualityTier.DATA_SAVER)

            // Atmos -> Hi-Res Max -> Hi-Res 96 -> CD -> 320 kbps -> YouTube
            AudioQualityTier.DOLBY_ATMOS -> listOf(
                AudioQualityTier.DOLBY_ATMOS,
                AudioQualityTier.HI_RES_MAX,
                AudioQualityTier.HI_RES_96,
                AudioQualityTier.CD_LOSSLESS,
                AudioQualityTier.STANDARD_320,
                AudioQualityTier.YOUTUBE_MUSIC
            )

            // Hi-Res Max -> Hi-Res 96 -> CD -> 320 -> YouTube
            AudioQualityTier.HI_RES_MAX -> listOf(
                AudioQualityTier.HI_RES_MAX,
                AudioQualityTier.HI_RES_96,
                AudioQualityTier.CD_LOSSLESS,
                AudioQualityTier.STANDARD_320,
                AudioQualityTier.YOUTUBE_MUSIC
            )

            AudioQualityTier.HI_RES_96 -> listOf(
                AudioQualityTier.HI_RES_96,
                AudioQualityTier.CD_LOSSLESS,
                AudioQualityTier.STANDARD_320,
                AudioQualityTier.YOUTUBE_MUSIC
            )

            AudioQualityTier.CD_LOSSLESS -> listOf(
                AudioQualityTier.CD_LOSSLESS,
                AudioQualityTier.STANDARD_320,
                AudioQualityTier.YOUTUBE_MUSIC
            )

            AudioQualityTier.STANDARD_320 -> listOf(
                AudioQualityTier.STANDARD_320,
                AudioQualityTier.YOUTUBE_MUSIC
            )
        }
    }
}

@Singleton
class AudioPlaybackStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val STREAM_QUALITY = stringPreferencesKey("stream_quality")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val BIT_PERFECT = booleanPreferencesKey("bit_perfect")
        val CROSSFADE = booleanPreferencesKey("crossfade")
        val KEEP_AWAKE_LYRICS = booleanPreferencesKey("keep_awake_lyrics")
        val DOWNLOAD_LRC = booleanPreferencesKey("download_lrc")
        val USB_EXCLUSIVE = booleanPreferencesKey("usb_exclusive")
        val INTERNAL_DIRECT = booleanPreferencesKey("internal_direct")
        val SW_VOLUME_USB = booleanPreferencesKey("sw_volume_usb")
        val LOUDNESS_MODE = stringPreferencesKey("loudness_mode")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val SYSTEM_FX = booleanPreferencesKey("system_fx")
        val STUDIO_CLARITY = booleanPreferencesKey("studio_clarity")
        val CLARITY_PRESET = stringPreferencesKey("clarity_preset")
        val CLARITY_SPATIAL_BYPASS = booleanPreferencesKey("clarity_spatial_bypass")
        val DOLBY_SURROUND_ATMOS = booleanPreferencesKey("dolby_surround_atmos")
        val SPATIAL_AUDIO_ENABLED = booleanPreferencesKey("spatial_audio_enabled")
        val SPATIAL_MODE = stringPreferencesKey("spatial_mode")
        val SPATIAL_SPEAKER_ANGLE = androidx.datastore.preferences.core.floatPreferencesKey("spatial_speaker_angle")
        val SPATIAL_AMOUNT = androidx.datastore.preferences.core.floatPreferencesKey("spatial_amount")
    }

    val stateFlow: Flow<AudioPlaybackState> = context.audioPlaybackDataStore.data.map { prefs ->
        val preset = runCatching { Eq15Preset.valueOf(prefs[Keys.EQ_PRESET] ?: "STUDIO_MASTER") }.getOrDefault(Eq15Preset.STUDIO_MASTER)
        val savedBands = prefs[Keys.EQ_BANDS_CSV]?.split(",")?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 15 } ?: preset.gainsDb.toList()

        AudioPlaybackState(
            streamingQuality = runCatching { AudioQualityTier.valueOf(prefs[Keys.STREAM_QUALITY] ?: "DOLBY_ATMOS") }.getOrDefault(AudioQualityTier.DOLBY_ATMOS),
            downloadQuality = runCatching { AudioQualityTier.valueOf(prefs[Keys.DOWNLOAD_QUALITY] ?: "DOLBY_ATMOS") }.getOrDefault(AudioQualityTier.DOLBY_ATMOS),
            bitPerfectMode = prefs[Keys.BIT_PERFECT] ?: false,
            crossfadeEnabled = prefs[Keys.CROSSFADE] ?: false,
            keepScreenAwakeLyrics = prefs[Keys.KEEP_AWAKE_LYRICS] ?: false,
            downloadSyncedLyrics = prefs[Keys.DOWNLOAD_LRC] ?: true,
            usbExclusiveOutput = prefs[Keys.USB_EXCLUSIVE] ?: false,
            internalDirectOutput = prefs[Keys.INTERNAL_DIRECT] ?: false,
            softwareVolumeUsb = prefs[Keys.SW_VOLUME_USB] ?: false,
            loudnessMode = runCatching { LoudnessMode.valueOf(prefs[Keys.LOUDNESS_MODE] ?: "OFF") }.getOrDefault(LoudnessMode.OFF),
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            eqPreset = preset,
            eqBandGains = savedBands,
            systemAudioEffects = prefs[Keys.SYSTEM_FX] ?: false,
            studioMasterClarity = prefs[Keys.STUDIO_CLARITY] ?: true,
            clarityOutputPreset = runCatching { ClarityOutputPreset.valueOf(prefs[Keys.CLARITY_PRESET] ?: "REFERENCE") }.getOrDefault(ClarityOutputPreset.REFERENCE),
            claritySpatialBypass = prefs[Keys.CLARITY_SPATIAL_BYPASS] ?: false,
            dolbySurroundAtmos = prefs[Keys.DOLBY_SURROUND_ATMOS] ?: false,
            spatialAudioEnabled = prefs[Keys.SPATIAL_AUDIO_ENABLED] ?: false,
            spatialMode = runCatching { SpatialMode.valueOf(prefs[Keys.SPATIAL_MODE] ?: "SPEAKERS") }.getOrDefault(SpatialMode.SPEAKERS),
            spatialSpeakerAngle = prefs[Keys.SPATIAL_SPEAKER_ANGLE] ?: 30f,
            spatialAmount = prefs[Keys.SPATIAL_AMOUNT] ?: 1.0f
        )
    }

    suspend fun update(transform: (MutablePreferences) -> Unit) {
        context.audioPlaybackDataStore.edit(transform)
    }

    suspend fun setStreamingQuality(tier: AudioQualityTier) = update { it[Keys.STREAM_QUALITY] = tier.name }
    suspend fun setDownloadQuality(tier: AudioQualityTier) = update { it[Keys.DOWNLOAD_QUALITY] = tier.name }
    suspend fun setBitPerfect(enabled: Boolean) = update {
        it[Keys.BIT_PERFECT] = enabled
        if (!enabled) it[Keys.USB_EXCLUSIVE] = false
    }
    suspend fun setCrossfade(enabled: Boolean) = update { it[Keys.CROSSFADE] = enabled }
    suspend fun setKeepAwakeLyrics(enabled: Boolean) = update { it[Keys.KEEP_AWAKE_LYRICS] = enabled }
    suspend fun setDownloadSyncedLyrics(enabled: Boolean) = update { it[Keys.DOWNLOAD_LRC] = enabled }
    suspend fun setUsbExclusive(enabled: Boolean) = update {
        it[Keys.USB_EXCLUSIVE] = enabled
        if (enabled) it[Keys.BIT_PERFECT] = true
    }
    suspend fun setInternalDirect(enabled: Boolean) = update { it[Keys.INTERNAL_DIRECT] = enabled }
    suspend fun setSoftwareVolumeUsb(enabled: Boolean) = update { it[Keys.SW_VOLUME_USB] = enabled }
    suspend fun setLoudnessMode(mode: LoudnessMode) = update { it[Keys.LOUDNESS_MODE] = mode.name }
    suspend fun setEqEnabled(enabled: Boolean) = update { it[Keys.EQ_ENABLED] = enabled }
    suspend fun setEqPreset(preset: Eq15Preset) = update {
        it[Keys.EQ_PRESET] = preset.name
        it[Keys.EQ_BANDS_CSV] = preset.gainsDb.joinToString(",")
        // Picking a preset must audibly apply it, even if the EQ switch was off before.
        it[Keys.EQ_ENABLED] = true
    }
    suspend fun setEqBandGain(index: Int, gainDb: Float, currentBands: List<Float>) = update {
        if (index !in 0..14) return@update
        // Read the freshest saved bands inside the transaction; the passed list may be stale
        // when several slider events arrive before DataStore has emitted the previous write.
        val stored = it[Keys.EQ_BANDS_CSV]?.split(",")?.mapNotNull { v -> v.toFloatOrNull() }
            ?.takeIf { l -> l.size == 15 }
        val base = stored ?: currentBands.takeIf { l -> l.size == 15 } ?: List(15) { 0f }
        val updated = base.toMutableList().apply { this[index] = gainDb.coerceIn(-8f, 8f) }
        it[Keys.EQ_BANDS_CSV] = updated.joinToString(",")
    }
    suspend fun setSystemAudioEffects(enabled: Boolean) = update { it[Keys.SYSTEM_FX] = enabled }
    suspend fun setStudioMasterClarity(enabled: Boolean) = update { it[Keys.STUDIO_CLARITY] = enabled }
    suspend fun setClarityOutputPreset(preset: ClarityOutputPreset) = update { it[Keys.CLARITY_PRESET] = preset.name }
    suspend fun setClaritySpatialBypass(enabled: Boolean) = update { it[Keys.CLARITY_SPATIAL_BYPASS] = enabled }
    suspend fun setDolbySurroundAtmos(enabled: Boolean) = update { it[Keys.DOLBY_SURROUND_ATMOS] = enabled }
    suspend fun setSpatialAudioEnabled(enabled: Boolean) = update { it[Keys.SPATIAL_AUDIO_ENABLED] = enabled }
    suspend fun setSpatialMode(mode: SpatialMode) = update { it[Keys.SPATIAL_MODE] = mode.name }
    suspend fun setSpatialSpeakerAngle(angle: Float) = update { it[Keys.SPATIAL_SPEAKER_ANGLE] = angle.coerceIn(15f, 60f) }
    suspend fun setSpatialAmount(amount: Float) = update { it[Keys.SPATIAL_AMOUNT] = amount.coerceIn(0f, 1f) }
}
