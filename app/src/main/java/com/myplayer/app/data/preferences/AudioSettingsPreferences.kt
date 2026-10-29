package com.myplayer.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.audioDataStore by preferencesDataStore(name = "audio_playback_settings")

enum class StreamQuality(val label: String, val maxBitrateKbps: Int) {
    LOW("Low (Data Saver • 64 kbps)", 64),
    MEDIUM("Normal (128 kbps)", 128),
    HIGH("High (Best Opus/AAC • 160-256 kbps)", 320)
}

enum class ClarityPreset(val label: String, val highShelfGainDb: Float, val bassGainDb: Float) {
    OFF("Off (Flat)", 0f, 0f),
    VOCAL_CLARITY("Vocal Presence", 3.5f, -1.0f),
    CRISP_HIGH("Crisp & Detailed", 5.0f, 1.5f),
    WARM_STUDIO("Warm Studio", 2.0f, 3.0f)
}

data class AudioPlaybackConfig(
    val wifiQuality: StreamQuality = StreamQuality.HIGH,
    val mobileQuality: StreamQuality = StreamQuality.MEDIUM,
    val downloadQuality: StreamQuality = StreamQuality.HIGH,
    val clarityPreset: ClarityPreset = ClarityPreset.OFF,
    val trueDolbySpatial: Boolean = false,
    val spatialWidth: Float = 0.45f, // 0.0f to 1.0f
    val loudnessNormalize: Boolean = false,
    val loudnessGainMb: Int = 600,   // millibels (600mB = +6dB)
    val skipSilence: Boolean = false,
    val enableFloatHighRes: Boolean = true
)

@Singleton
class AudioSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val WIFI_QUALITY = stringPreferencesKey("wifi_quality")
        val MOBILE_QUALITY = stringPreferencesKey("mobile_quality")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val CLARITY_PRESET = stringPreferencesKey("clarity_preset")
        val TRUE_DOLBY_SPATIAL = booleanPreferencesKey("true_dolby_spatial")
        val SPATIAL_WIDTH = floatPreferencesKey("spatial_width")
        val LOUDNESS_NORMALIZE = booleanPreferencesKey("loudness_normalize")
        val LOUDNESS_GAIN_MB = intPreferencesKey("loudness_gain_mb")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val FLOAT_HIGH_RES = booleanPreferencesKey("float_high_res")
    }

    val configFlow: Flow<AudioPlaybackConfig> = context.audioDataStore.data.map { prefs ->
        AudioPlaybackConfig(
            wifiQuality = StreamQuality.valueOf(prefs[Keys.WIFI_QUALITY] ?: StreamQuality.HIGH.name),
            mobileQuality = StreamQuality.valueOf(prefs[Keys.MOBILE_QUALITY] ?: StreamQuality.MEDIUM.name),
            downloadQuality = StreamQuality.valueOf(prefs[Keys.DOWNLOAD_QUALITY] ?: StreamQuality.HIGH.name),
            clarityPreset = ClarityPreset.valueOf(prefs[Keys.CLARITY_PRESET] ?: ClarityPreset.OFF.name),
            trueDolbySpatial = prefs[Keys.TRUE_DOLBY_SPATIAL] ?: false,
            spatialWidth = prefs[Keys.SPATIAL_WIDTH] ?: 0.45f,
            loudnessNormalize = prefs[Keys.LOUDNESS_NORMALIZE] ?: false,
            loudnessGainMb = prefs[Keys.LOUDNESS_GAIN_MB] ?: 600,
            skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
            enableFloatHighRes = prefs[Keys.FLOAT_HIGH_RES] ?: true
        )
    }

    suspend fun updateWifiQuality(quality: StreamQuality) =
        context.audioDataStore.edit { it[Keys.WIFI_QUALITY] = quality.name }

    suspend fun updateMobileQuality(quality: StreamQuality) =
        context.audioDataStore.edit { it[Keys.MOBILE_QUALITY] = quality.name }

    suspend fun updateClarityPreset(preset: ClarityPreset) =
        context.audioDataStore.edit { it[Keys.CLARITY_PRESET] = preset.name }

    suspend fun updateTrueDolby(enabled: Boolean, width: Float = 0.45f) =
        context.audioDataStore.edit {
            it[Keys.TRUE_DOLBY_SPATIAL] = enabled
            it[Keys.SPATIAL_WIDTH] = width
        }

    suspend fun updateLoudness(enabled: Boolean, gainMb: Int = 600) =
        context.audioDataStore.edit {
            it[Keys.LOUDNESS_NORMALIZE] = enabled
            it[Keys.LOUDNESS_GAIN_MB] = gainMb
        }

    suspend fun updateSkipSilence(enabled: Boolean) =
        context.audioDataStore.edit { it[Keys.SKIP_SILENCE] = enabled }
}
