package com.example.myplayer.playback

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class AudioFeatureManagers @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private var crossfadeJob: Job? = null

    /**
     * 1. SYSTEM AUDIO EFFECTS (Let Dolby / equalizer apps process MyPlayer audio)
     */
    fun toggleSystemAudioEffects(audioSessionId: Int, enable: Boolean) {
        if (audioSessionId <= 0) return
        val action = if (enable) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
        } else {
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
        }
        val intent = Intent(action).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        context.sendBroadcast(intent)
    }

    /**
     * 2. CROSSFADE ENGINE (Smoothly ramps volume down at track end and ramps up on next track)
     */
    fun attachCrossfadeMonitor(
        scope: CoroutineScope,
        exoPlayer: ExoPlayer,
        isEnabled: () -> Boolean,
        crossfadeDurationSec: Int = 5
    ) {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(250L)
                if (!isEnabled() || !exoPlayer.isPlaying || !exoPlayer.hasNextMediaItem()) {
                    if (exoPlayer.volume < 1f && !isEnabled()) exoPlayer.volume = 1f
                    continue
                }
                val duration = exoPlayer.duration
                val position = exoPlayer.currentPosition
                if (duration > 15_000L && position > 0L) {
                    val remainingMs = duration - position
                    val fadeWindowMs = crossfadeDurationSec * 1000L
                    if (remainingMs in 1..fadeWindowMs) {
                        // Ramp down smoothly
                        val fraction = (remainingMs.toFloat() / fadeWindowMs).coerceIn(0.05f, 1.0f)
                        exoPlayer.volume = fraction
                        if (remainingMs <= 350L) {
                            exoPlayer.seekToNextMediaItem()
                            // Ramp up new track
                            for (step in 1..10) {
                                exoPlayer.volume = step / 10f
                                delay(120L)
                            }
                        }
                    } else if (exoPlayer.volume < 1f) {
                        exoPlayer.volume = 1f
                    }
                }
            }
        }
    }

    /**
     * 3. DOWNLOAD SYNCED LYRICS (.lrc companion file from LRCLIB next to downloaded audio)
     */
    suspend fun downloadLrcCompanionFile(
        trackTitle: String,
        artistName: String,
        durationSeconds: Long,
        targetAudioFile: File
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val t = URLEncoder.encode(trackTitle, "UTF-8")
            val a = URLEncoder.encode(artistName, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$t&artist_name=$a&duration=$durationSeconds"
            val req = Request.Builder().url(url).header("User-Agent", "MyPlayer/4.4").build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string() ?: return@withContext null)
                val syncedLyrics = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
                    ?: json.optString("plainLyrics").takeIf { it.isNotBlank() }
                    ?: return@withContext null

                val lrcFile = File(targetAudioFile.parentFile, "${targetAudioFile.nameWithoutExtension}.lrc")
                lrcFile.writeText(syncedLyrics)
                lrcFile
            }
        }.getOrNull()
    }

    /**
     * 4. BACKGROUND PLAYBACK (Check & Request Battery Optimization Exemption)
     */
    fun isBatteryUnrestricted(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun buildBatteryExemptionIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
