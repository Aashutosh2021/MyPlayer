package com.example.myplayer.ui.mochy

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.playback.AudioQualityInfo
import com.example.myplayer.ui.common.AlbumArtImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.myplayer.ui.screens.nowplaying.CustomLyricsDialog
import com.example.myplayer.ui.screens.nowplaying.NowPlayingVisual
import com.example.myplayer.ui.screens.nowplaying.LyricsTab
import com.example.myplayer.ui.screens.nowplaying.LyricsUiState
import com.example.myplayer.ui.screens.nowplaying.LyricsViewModel
import com.example.myplayer.ui.screens.nowplaying.SleepTimerDialog
import com.example.myplayer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

// ═════════════════════════════════════════════════════════════════════════════
// 1. MOCHY DARK THEME PALETTE & PLAYER STYLE ENUM (INCLUDES MYPLAYER RADIAL!)
// ═════════════════════════════════════════════════════════════════════════════

val MochyBlack = Color(0xFF070709)
val MochySurface = Color(0xFF141418)
val MochySurfaceHigh = Color(0xFF1E1E24)
val MochyCardBorder = Color(0xFF282830)
val MochyTextPrimary = Color(0xFFF5F5F7)
val MochyTextSecondary = Color(0xFF9A9AA6)
val MochyAccentGreen = Color(0xFF38E54D)
val MochyAccentPurple = Color(0xFF8B5CF6)

/**
 * All 6 Selectable Player Styles in Settings -> Appearance -> Customize Player:
 * Includes the 5 Mochy styles from the video + your signature MYPLAYER RADIAL screen!
 */
enum class PlayerStyleMode(val title: String, val subtitle: String) {
    RADIAL_MYPLAYER("Radial (MyPlayer)", "Signature Neon Radial Dial + 3D Tilt Art + Synced Lyrics"),
    DEFAULT("Default", "Full-bleed gradient cover, format badge & bottom lyrics pill"),
    TURNTABLE("Turntable", "Spinning vinyl LP record with realistic tonearm & floating controls"),
    POSTER("Poster", "Editorial full-bleed poster with curved wave seek arc"),
    TIMELINE("Timeline", "Minimalist split-screen giant timer card"),
    LYRICS_STAGE("Lyrics stage", "Immersive synced lyrics stage with split timer")
}

//Rainbow 10-band colors matching video (02:49 - 03:33)
val EqRainbowColors = listOf(
    Color(0xFFE57399), // 31 Hz
    Color(0xFFF08CAE), // 62 Hz
    Color(0xFFBA8FF7), // 125 Hz
    Color(0xFF8B9DF8), // 250 Hz
    Color(0xFF64B5F6), // 500 Hz
    Color(0xFF4DD0E1), // 1.0k Hz
    Color(0xFF81C784), // 2.0k Hz
    Color(0xFFDCE775), // 4.0k Hz
    Color(0xFFFFD54F), // 8.0k Hz
    Color(0xFFFF8A65)  // 16k Hz
)

val Eq10Frequencies = listOf("31", "62", "125", "250", "500", "1.0k", "2.0k", "4.0k", "8.0k", "16k")

enum class MochyEqPreset(val label: String, val gains: FloatArray) {
    CUSTOM("Custom", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 7f, 12f, 0f)),
    FLAT("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    ACOUSTIC("Acoustic", floatArrayOf(4.5f, 4.5f, 3.5f, 1f, 1.5f, 1.5f, 3f, 3.5f, 3f, 1.5f)),
    BASS_BOOSTER("Bass Booster", floatArrayOf(5.5f, 4.5f, 3.5f, 2f, 0.5f, 0f, 0f, 0f, 0f, 0f)),
    BASS_REDUCER("Bass Reducer", floatArrayOf(-5f, -4f, -3f, -2f, -0.5f, 0f, 0f, 0.5f, 1f, 1.5f)),
    CLASSICAL("Classical", floatArrayOf(4.5f, 3.5f, 3f, 2.5f, -1.5f, -1.5f, 0f, 2f, 3f, 3.5f)),
    DANCE("Dance", floatArrayOf(3.5f, 6.5f, 5f, 0f, 2f, 3.5f, 5f, 4.5f, 3.5f, 0f)),
    DEEP("Deep", floatArrayOf(5f, 4f, 2f, 1f, 1.5f, 1f, 0.5f, -1.5f, -3f, -4f)),
    ELECTRONIC("Electronic", floatArrayOf(4.5f, 4f, 1.5f, 0f, -2f, 2f, 1f, 1.5f, 4f, 4.5f)),
    HIP_HOP("Hip-Hop", floatArrayOf(5f, 4.5f, 1.5f, 3f, -1f, -1f, 1.5f, -0.5f, 2f, 3f)),
    JAZZ("Jazz", floatArrayOf(4f, 3f, 1.5f, 2f, -1.5f, -1.5f, 0f, 1.5f, 3f, 4f)),
    LOUDNESS("Loudness", floatArrayOf(5.5f, 4f, 0f, 0f, -2f, 0f, -0.5f, -4.5f, 5f, 1f)),
    LOUNGE("Lounge", floatArrayOf(-3f, -1.5f, -0.5f, 1.5f, 4f, 2.5f, 0f, -1.5f, 2f, 1f)),
    PIANO("Piano", floatArrayOf(3f, 2f, 0f, 2.5f, 3f, 1.5f, 3.5f, 4.5f, 3f, 3.5f)),
    POP("Pop", floatArrayOf(-1.5f, -1f, 0f, 2f, 4.5f, 4.5f, 2f, 0f, -1f, -1.5f)),
    RNB("R&B", floatArrayOf(3f, 6.5f, 5.5f, 1.5f, -2f, -1.5f, 2.5f, 3f, 3f, 4f)),
    ROCK("Rock", floatArrayOf(5f, 3f, -3f, -4.5f, -2f, 2f, 5.5f, 6.5f, 6.5f, 6.5f)),
    TREBLE_BOOSTER("Treble Booster", floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 2.5f, 4f, 5f, 6f)),
    VOCAL_BOOSTER("Vocal Booster", floatArrayOf(-1.5f, -3f, -3f, 1.5f, 4f, 4f, 3f, 1.5f, 0f, -1.5f))
}

@Stable
class MochyUiStateHolder(context: Context) {
    private val prefs = context.getSharedPreferences("mochy_ui_prefs", Context.MODE_PRIVATE)

    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarding_done", false))
    var username by mutableStateOf(prefs.getString("username", "Aashutosh") ?: "Aashutosh")
    var profileId by mutableStateOf(prefs.getString("profile_id", "USR#6014956565") ?: "USR#6014956565")
    var joinedDate by mutableStateOf(
        prefs.getString(
            "joined_date",
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        ) ?: "Oct 28, 2026"
    )
    var pickedArtists by mutableStateOf(
        prefs.getStringSet("picked_artists", setOf("Arijit Singh", "Atif Aslam", "Pritam"))?.toSet()
            ?: setOf("Arijit Singh", "Atif Aslam", "Pritam")
    )

    // Player Appearance & Style (Defaulting to RADIAL_MYPLAYER or TURNTABLE - user selectable!)
    var playerStyle by mutableStateOf(
        runCatching {
            PlayerStyleMode.valueOf(prefs.getString("player_style", PlayerStyleMode.RADIAL_MYPLAYER.name)!!)
        }.getOrDefault(PlayerStyleMode.RADIAL_MYPLAYER)
    )
    var lyricsAnimStyle by mutableIntStateOf(prefs.getInt("lyrics_anim_style", 1))
    var lyricsShader by mutableStateOf(prefs.getBoolean("lyrics_shader", false))
    var highQualityBloom by mutableStateOf(prefs.getBoolean("hq_bloom", true))
    var audioFormatBadge by mutableStateOf(prefs.getBoolean("audio_format_badge", true))
    var showSampleRate by mutableStateOf(prefs.getBoolean("show_sample_rate", false))
    var enableYoutubeBg by mutableStateOf(prefs.getBoolean("yt_bg", false))
    var enableAnimatedCanvas by mutableStateOf(prefs.getBoolean("anim_canvas", true))
    var canvasWifiOnly by mutableStateOf(prefs.getBoolean("canvas_wifi_only", true))
    var glassBlur by mutableStateOf(prefs.getBoolean("glass_blur", true))
    var progressiveBlur by mutableStateOf(prefs.getBoolean("progressive_blur", true))
    var blurStrengthPx by mutableFloatStateOf(prefs.getFloat("blur_strength", 25f))
    var smoothAnimations by mutableStateOf(prefs.getBoolean("smooth_animations", true))

    // General & Playback Settings
    var disableUpdateNotice by mutableStateOf(prefs.getBoolean("disable_update_notice", false))
    var offlineMode by mutableStateOf(prefs.getBoolean("offline_mode", false))
    var localAutoMetadata by mutableStateOf(prefs.getBoolean("local_auto_metadata", true))
    var disableAutoPlayVideo by mutableStateOf(prefs.getBoolean("disable_yt_video", false))
    var dontSkipOnFailure by mutableStateOf(prefs.getBoolean("dont_skip_failure", false))
    var skipTimeoutSec by mutableFloatStateOf(prefs.getFloat("skip_timeout_sec", 6f))
    var keepPlayingWhenClosed by mutableStateOf(prefs.getBoolean("keep_playing_closed", true))
    var savePlayerLastState by mutableStateOf(prefs.getBoolean("save_last_state", true))
    var skipSilence by mutableStateOf(prefs.getBoolean("skip_silence", false))

    // Audio Lab: Equalizer, Reverb, Loudness, Spatial, Speed, Crossfade
    var eqEnabled by mutableStateOf(prefs.getBoolean("mochy_eq_enabled", true))
    var eqPreset by mutableStateOf(MochyEqPreset.FLAT)
    var eqBands by mutableStateOf(MochyEqPreset.FLAT.gains.toList())
    var preampDb by mutableFloatStateOf(0f)
    var autoPreamp by mutableStateOf(true)
    var selectedAutoEqModel by mutableStateOf<String?>(null)

    var reverbEnabled by mutableStateOf(false)
    var reverbPreset by mutableStateOf("Room")
    var reverbMix by mutableFloatStateOf(0.50f)
    var reverbRoomSize by mutableFloatStateOf(0.55f)
    var reverbDamping by mutableFloatStateOf(0.45f)
    var reverbWet by mutableFloatStateOf(0.60f)

    var loudnessEnabled by mutableStateOf(false)
    var evenOutSongs by mutableStateOf(true)
    var replayGainMode by mutableStateOf("Track")
    var replayGainPreampDb by mutableFloatStateOf(3.0f)

    var spatialAudioEnabled by mutableStateOf(false)
    var spatialMode by mutableStateOf("Crossfeed") // Crossfeed | Speakers
    var spatialPreset by mutableStateOf("Light")   // Light | Natural | Strong
    var headShadowHz by mutableFloatStateOf(700f)
    var crossfeedDb by mutableFloatStateOf(-4.5f)
    var spatialAmount by mutableFloatStateOf(1.0f)
    var monoAudio by mutableStateOf(false)
    var dolbyAtmosSurround by mutableStateOf(false)

    var playbackSpeedMode by mutableStateOf("Normal")
    var playbackSpeed by mutableFloatStateOf(1.0f)
    var preservePitch by mutableStateOf(true)
    var pitchSemitones by mutableFloatStateOf(0f)
    var exponentialVolume by mutableStateOf(false)

    var autoCrossfade by mutableStateOf(false)
    var manualCrossfade by mutableStateOf(false)
    var crossfadeLengthSec by mutableFloatStateOf(5f)
    var keepGaplessAlbums by mutableStateOf(true)
    var enablePauseFade by mutableStateOf(true)
    var pauseFadeLengthSec by mutableFloatStateOf(2f)

    init {
        // All property initialisers above have run; restore the saved Audio Lab settings.
        loadAudioLabFromPrefs()
    }

    /** Every Audio Lab value that must reach the engine / be saved. snapshotFlow watches this. */
    fun audioLabSignature(): List<Any?> = listOf(
        eqEnabled, eqPreset, eqBands, preampDb, autoPreamp, selectedAutoEqModel,
        reverbEnabled, reverbPreset, reverbMix, reverbRoomSize, reverbDamping, reverbWet,
        loudnessEnabled, evenOutSongs, replayGainMode, replayGainPreampDb,
        spatialAudioEnabled, spatialMode, spatialPreset, headShadowHz, crossfeedDb, spatialAmount,
        monoAudio, dolbyAtmosSurround,
        playbackSpeedMode, playbackSpeed, preservePitch, pitchSemitones, exponentialVolume,
        autoCrossfade, manualCrossfade, crossfadeLengthSec, keepGaplessAlbums, enablePauseFade, pauseFadeLengthSec
    )

    private fun loadAudioLabFromPrefs() {
        eqEnabled = prefs.getBoolean("mochy_eq_enabled", eqEnabled)
        prefs.getString("al_eq_preset", null)?.let { n ->
            MochyEqPreset.values().firstOrNull { it.name == n }?.let { eqPreset = it }
        }
        prefs.getString("al_eq_bands", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 10 }?.let { eqBands = it }
        preampDb = prefs.getFloat("al_preamp", preampDb)
        autoPreamp = prefs.getBoolean("al_auto_preamp", autoPreamp)
        selectedAutoEqModel = prefs.getString("al_autoeq_model", selectedAutoEqModel)

        reverbEnabled = prefs.getBoolean("al_reverb_on", reverbEnabled)
        reverbPreset = prefs.getString("al_reverb_preset", reverbPreset) ?: reverbPreset
        reverbMix = prefs.getFloat("al_reverb_mix", reverbMix)
        reverbRoomSize = prefs.getFloat("al_reverb_room", reverbRoomSize)
        reverbDamping = prefs.getFloat("al_reverb_damp", reverbDamping)
        reverbWet = prefs.getFloat("al_reverb_wet", reverbWet)

        loudnessEnabled = prefs.getBoolean("al_loud_on", loudnessEnabled)
        evenOutSongs = prefs.getBoolean("al_even_out", evenOutSongs)
        replayGainMode = prefs.getString("al_rg_mode", replayGainMode) ?: replayGainMode
        replayGainPreampDb = prefs.getFloat("al_rg_preamp", replayGainPreampDb)

        spatialAudioEnabled = prefs.getBoolean("al_spatial_on", spatialAudioEnabled)
        spatialMode = prefs.getString("al_spatial_mode", spatialMode) ?: spatialMode
        spatialPreset = prefs.getString("al_spatial_preset", spatialPreset) ?: spatialPreset
        headShadowHz = prefs.getFloat("al_head_shadow", headShadowHz)
        crossfeedDb = prefs.getFloat("al_crossfeed_db", crossfeedDb)
        spatialAmount = prefs.getFloat("al_spatial_amount", spatialAmount)
        monoAudio = prefs.getBoolean("al_mono", monoAudio)
        dolbyAtmosSurround = prefs.getBoolean("al_dolby", dolbyAtmosSurround)

        playbackSpeedMode = prefs.getString("al_speed_mode", playbackSpeedMode) ?: playbackSpeedMode
        playbackSpeed = prefs.getFloat("al_speed", playbackSpeed)
        preservePitch = prefs.getBoolean("al_preserve_pitch", preservePitch)
        pitchSemitones = prefs.getFloat("al_pitch_semis", pitchSemitones)
        exponentialVolume = prefs.getBoolean("al_exp_volume", exponentialVolume)

        autoCrossfade = prefs.getBoolean("al_auto_xfade", autoCrossfade)
        manualCrossfade = prefs.getBoolean("al_manual_xfade", manualCrossfade)
        crossfadeLengthSec = prefs.getFloat("al_xfade_len", crossfadeLengthSec)
        keepGaplessAlbums = prefs.getBoolean("al_gapless", keepGaplessAlbums)
        enablePauseFade = prefs.getBoolean("al_pause_fade", enablePauseFade)
        pauseFadeLengthSec = prefs.getFloat("al_pause_fade_len", pauseFadeLengthSec)
    }

    fun saveAudioLabToPrefs() {
        prefs.edit()
            .putBoolean("mochy_eq_enabled", eqEnabled)
            .putString("al_eq_preset", eqPreset.name)
            .putString("al_eq_bands", eqBands.joinToString(","))
            .putFloat("al_preamp", preampDb)
            .putBoolean("al_auto_preamp", autoPreamp)
            .putString("al_autoeq_model", selectedAutoEqModel)
            .putBoolean("al_reverb_on", reverbEnabled)
            .putString("al_reverb_preset", reverbPreset)
            .putFloat("al_reverb_mix", reverbMix)
            .putFloat("al_reverb_room", reverbRoomSize)
            .putFloat("al_reverb_damp", reverbDamping)
            .putFloat("al_reverb_wet", reverbWet)
            .putBoolean("al_loud_on", loudnessEnabled)
            .putBoolean("al_even_out", evenOutSongs)
            .putString("al_rg_mode", replayGainMode)
            .putFloat("al_rg_preamp", replayGainPreampDb)
            .putBoolean("al_spatial_on", spatialAudioEnabled)
            .putString("al_spatial_mode", spatialMode)
            .putString("al_spatial_preset", spatialPreset)
            .putFloat("al_head_shadow", headShadowHz)
            .putFloat("al_crossfeed_db", crossfeedDb)
            .putFloat("al_spatial_amount", spatialAmount)
            .putBoolean("al_mono", monoAudio)
            .putBoolean("al_dolby", dolbyAtmosSurround)
            .putString("al_speed_mode", playbackSpeedMode)
            .putFloat("al_speed", playbackSpeed)
            .putBoolean("al_preserve_pitch", preservePitch)
            .putFloat("al_pitch_semis", pitchSemitones)
            .putBoolean("al_exp_volume", exponentialVolume)
            .putBoolean("al_auto_xfade", autoCrossfade)
            .putBoolean("al_manual_xfade", manualCrossfade)
            .putFloat("al_xfade_len", crossfadeLengthSec)
            .putBoolean("al_gapless", keepGaplessAlbums)
            .putBoolean("al_pause_fade", enablePauseFade)
            .putFloat("al_pause_fade_len", pauseFadeLengthSec)
            .apply()
    }

    fun completeOnboarding(name: String, artists: Set<String>) {
        username = name.ifBlank { "Aashutosh" }
        pickedArtists = artists
        onboardingDone = true
        prefs.edit()
            .putBoolean("onboarding_done", true)
            .putString("username", username)
            .putStringSet("picked_artists", artists)
            .apply()
    }

    fun updatePlayerStyle(style: PlayerStyleMode) {
        playerStyle = style
        try {
            prefs.edit().putString("player_style", style.name).apply()
        } catch (_: Exception) {}
    }
}

/** Process-wide shared holder — every screen reads/writes the same instance,
 *  so a style set in the picker applies live everywhere (incl. NowPlayingScreen). */
private var sharedMochyUiStateHolder: MochyUiStateHolder? = null

@Composable
fun rememberMochyUiStateHolder(): MochyUiStateHolder {
    val context = LocalContext.current
    return remember {
        sharedMochyUiStateHolder ?: MochyUiStateHolder(context.applicationContext).also { sharedMochyUiStateHolder = it }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. MOCHY PLAY LOGO & METALLIC PROFILE ID CARD (00:01 - 00:18, 00:38, 01:24)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochyPlayLogo(
    modifier: Modifier = Modifier.size(34.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.22f, h * 0.84f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Curved inner arrow from video (00:01)
        drawArc(
            color = tint,
            startAngle = 90f,
            sweepAngle = 135f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.32f),
            size = Size(w * 0.36f, h * 0.36f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MochyProfileIdCard(
    username: String,
    profileId: String,
    joinedDate: String,
    metallicLight: Boolean = false,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayUser = username.ifBlank { "{username}" }
    val bgBrush = if (metallicLight) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFE6E8EC), Color(0xFFBDC2C9), Color(0xFFD8DCE2)),
            start = Offset.Zero,
            end = Offset(700f, 350f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF232329), Color(0xFF19191E), Color(0xFF282830)),
            start = Offset.Zero,
            end = Offset(700f, 350f)
        )
    }
    val textColor = if (metallicLight) Color(0xFF121215) else Color.White
    val subTextColor = if (metallicLight) Color(0xFF494B52) else Color(0xFF9A9AA6)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .shadow(16.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(bgBrush)
            .border(1.dp, Color.White.copy(alpha = if (metallicLight) 0.6f else 0.12f), RoundedCornerShape(22.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Main Pass Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROFILE $profileId",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = subTextColor,
                    letterSpacing = 0.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (metallicLight) Color(0xFF5A5D64) else Color(0xFF32323A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Avatar",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        if (metallicLight) {
                            Text(
                                text = "ID CARD OF",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = subTextColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayUser,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onEditClick != null) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = subTextColor,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onEditClick() }
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Joined at $joinedDate",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                    }
                }
            }

            // Perforated Vertical Divider
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .padding(vertical = 10.dp)
            ) {
                val dashHeight = 8f
                val gap = 8f
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = subTextColor.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(0f, (y + dashHeight).coerceAtMost(size.height)),
                        strokeWidth = 2f
                    )
                    y += dashHeight + gap
                }
            }

            // Right Vertical Stub ("Aashutosh's CARD")
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(42.dp)
                    .background(Color.Black.copy(alpha = if (metallicLight) 0.08f else 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$displayUser's CARD •",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = subTextColor,
                    maxLines = 1,
                    modifier = Modifier.rotate(90f)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. 5-STEP MOCHY ONBOARDING FLOW (00:04 - 00:39 IN VIDEO)
// ═════════════════════════════════════════════════════════════════════════════

private val ONBOARDING_ARTISTS = listOf(
    "Arijit Singh", "Atif Aslam", "Pritam", "Vishal Mishra", "Shreya Ghoshal", "Mithoon",
    "A.R. Rahman", "Karan Aujla", "Diljit Dosanjh", "Sidhu Moose Wala", "AP Dhillon",
    "KK", "Mohit Chauhan", "Sonu Nigam", "Anirudh Ravichander", "The Weeknd",
    "Shubh", "Seedhe Maut", "KR\$NA", "Yo Yo Honey Singh", "Badshah", "Taylor Swift"
)

@Composable
fun MochyOnboardingFlow(
    mochyState: MochyUiStateHolder,
    onImportFromSpotifyClick: () -> Unit = {},
    onComplete: (username: String, selectedArtists: Set<String>) -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0..5 matching 00:04..00:39
    // System Back goes to the previous onboarding step instead of closing the whole app.
    BackHandler(enabled = step > 0) { step -= 1 }
    var nameInput by remember { mutableStateOf(mochyState.username) }
    var artistQuery by remember { mutableStateOf("") }
    var selectedArtists by remember { mutableStateOf(setOf<String>()) }

    val filteredArtists = remember(artistQuery) {
        if (artistQuery.isBlank()) ONBOARDING_ARTISTS
        else ONBOARDING_ARTISTS.filter { it.contains(artistQuery.trim(), ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MochyBlack
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInHorizontally { it / 4 })
                    .togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
            },
            label = "mochyOnboardingStep"
        ) { currentStep ->
            when (currentStep) {
                // STEP 0 (00:04 - 00:06): "Welcome to mochy / MyPlayer!" + Vinyl Tonearm Animation
                0 -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "vinylOnboarding")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
                        label = "vinylSpin"
                    )
                    val armAngle by animateFloatAsState(
                        targetValue = 22f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "toneArmSwing"
                    )

                    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        // Top-right giant vinyl + tonearm (exact match to 00:04 - 00:06)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width * 0.92f
                            val cy = size.height * 0.30f
                            val radius = size.width * 0.62f

                            // Soft green glow behind vinyl
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF1DB954).copy(alpha = 0.16f), Color.Transparent),
                                    center = Offset(cx * 0.6f, cy * 1.2f),
                                    radius = radius * 1.2f
                                ),
                                center = Offset(cx * 0.6f, cy * 1.2f),
                                radius = radius * 1.2f
                            )

                            rotate(rotation, pivot = Offset(cx, cy)) {
                                drawCircle(color = Color(0xFF1F1F22), radius = radius, center = Offset(cx, cy))
                                drawCircle(
                                    color = Color(0xFF2D2D32),
                                    radius = radius * 0.78f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF2D2D32),
                                    radius = radius * 0.56f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(color = Color(0xFF121214), radius = radius * 0.24f, center = Offset(cx, cy))
                                drawCircle(color = Color(0xFF2B2B30), radius = radius * 0.08f, center = Offset(cx, cy))
                            }

                            // Tonearm swinging in from bottom-left toward vinyl
                            rotate(armAngle, pivot = Offset(size.width * 0.1f, size.height * 0.48f)) {
                                drawLine(
                                    color = Color(0xFF3E3E44),
                                    start = Offset(size.width * 0.05f, size.height * 0.50f),
                                    end = Offset(size.width * 0.58f, size.height * 0.33f),
                                    strokeWidth = 8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawRoundRect(
                                    color = Color(0xFF5A5A62),
                                    topLeft = Offset(size.width * 0.54f, size.height * 0.31f),
                                    size = Size(54.dp.toPx(), 24.dp.toPx()),
                                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            Text(
                                text = "Welcome to\nMyPlayer!",
                                fontSize = 40.sp,
                                lineHeight = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "A way to listen and own your\nmusic, differently.",
                                fontSize = 16.sp,
                                color = MochyTextSecondary
                            )
                            Spacer(Modifier.height(44.dp))
                            MochyPrimaryPillButton(
                                text = "Next",
                                onClick = { step = 1 }
                            )
                        }
                    }
                }

                // STEP 1 (00:07 - 00:09): "Importing music"
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Spacer(Modifier.height(24.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MochySurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "Importing music",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Do you want to import music from other platforms like Spotify, Apple Music, Tidal or SoundCloud, or sync your data with YouTube Music?",
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MochyTextSecondary
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MochyPrimaryPillButton(
                                text = "Yes!",
                                onClick = {
                                    onImportFromSpotifyClick()
                                    step = 2
                                }
                            )
                            MochySecondaryPillButton(
                                text = "Skip",
                                onClick = { step = 2 }
                            )
                        }
                    }
                }

                // STEP 2 (00:10 - 00:15): "Let's create your profile!" + Live ID Card
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MochySurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "Let's create your\nprofile!",
                                fontSize = 32.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(22.dp))
                            Text("what's your username?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                singleLine = true,
                                placeholder = { Text("username", color = MochyTextSecondary) },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MochySurface,
                                    unfocusedContainerColor = MochySurface,
                                    focusedBorderColor = MochyCardBorder,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                )
                            )
                            Spacer(Modifier.height(22.dp))
                            Text("Set a profile picture here!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            MochyProfileIdCard(
                                username = nameInput,
                                profileId = mochyState.profileId,
                                joinedDate = mochyState.joinedDate,
                                metallicLight = false
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MyPlayer will never store your data online!",
                                fontSize = 12.sp,
                                color = MochyTextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            MochyPrimaryPillButton(
                                text = "Next",
                                onClick = { step = 3 }
                            )
                        }
                    }
                }

                // STEP 3 (00:16 - 00:18): Confetti + Tilted Metallic ID Card ("Hello! welcome to mochy!")
                3 -> {
                    LaunchedEffect(Unit) {
                        delay(2200)
                        step = 4
                    }
                    MochyConfettiCardScreen(
                        username = nameInput.ifBlank { "Aashutosh" },
                        profileId = mochyState.profileId,
                        joinedDate = mochyState.joinedDate,
                        bottomTitle = "Hello!",
                        bottomSubtitle = "welcome to MyPlayer!",
                        onTapContinue = { step = 4 }
                    )
                }

                // STEP 4 (00:19 - 00:33): "Let's build your home" (Pick 3+ Artists with Search)
                4 -> {
                    val remaining = (3 - selectedArtists.size).coerceAtLeast(0)
                    val buttonLabel = when {
                        remaining >= 2 -> "you need to pick $remaining artists"
                        remaining == 1 -> "one more artist to pick"
                        else -> "next"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MochySurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Let's build your home",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Select any artist you really like",
                            fontSize = 15.sp,
                            color = MochyTextSecondary
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = artistQuery,
                            onValueChange = { artistQuery = it },
                            singleLine = true,
                            placeholder = { Text("Search artists", color = MochyTextSecondary) },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MochySurface,
                                unfocusedContainerColor = MochySurface,
                                focusedBorderColor = MochyCardBorder,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(18.dp))

                        // 3-Column Circular Artist Grid (exact match to 00:21 - 00:33)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredArtists, key = { it }) { artist ->
                                val isPicked = artist in selectedArtists
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        selectedArtists = if (isPicked) selectedArtists - artist else selectedArtists + artist
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(92.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFF283048),
                                                        Color(0xFF181B24)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = if (isPicked) 2.5.dp else 1.dp,
                                                color = if (isPicked) Color.White else MochyCardBorder,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = artist.split(" ").take(2).joinToString("") { it.take(1) },
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                        if (isPicked) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.48f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = artist,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        MochyPrimaryPillButton(
                            text = buttonLabel,
                            enabled = remaining == 0,
                            onClick = { step = 5 }
                        )
                    }
                }

                // STEP 5 (00:34 - 00:39): "Last one" (Auto Crossfade + AI Artist Filter) -> Finish!
                5 -> {
                    var showFinalCard by remember { mutableStateOf(false) }

                    if (showFinalCard) {
                        LaunchedEffect(Unit) {
                            delay(1600)
                            mochyState.completeOnboarding(nameInput, selectedArtists)
                            onComplete(nameInput.ifBlank { "Aashutosh" }, selectedArtists)
                        }
                        MochyConfettiCardScreen(
                            username = nameInput.ifBlank { "Aashutosh" },
                            profileId = mochyState.profileId,
                            joinedDate = mochyState.joinedDate,
                            bottomTitle = "You're good to go",
                            bottomSubtitle = "",
                            showTopCheck = true,
                            onTapContinue = {
                                mochyState.completeOnboarding(nameInput, selectedArtists)
                                onComplete(nameInput.ifBlank { "Aashutosh" }, selectedArtists)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MochySurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White)
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Last one",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Disable anything that you don't like",
                                    fontSize = 15.sp,
                                    color = MochyTextSecondary
                                )
                                Spacer(Modifier.height(22.dp))

                                // Auto Crossfade Card with Dual Waveform Graphic (00:35)
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MochySurface,
                                    border = BorderStroke(1.dp, MochyCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        CrossfadeWaveformGraphic(
                                            labelLeft = "i miss you",
                                            labelRight = "aiii ai"
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Auto Crossfade", fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(
                                                    "Automatically crossfades, analysing the pitch, tempo and more to pick the perfect moment",
                                                    fontSize = 12.sp,
                                                    color = MochyTextSecondary
                                                )
                                            }
                                            Switch(
                                                checked = mochyState.autoCrossfade,
                                                onCheckedChange = { mochyState.autoCrossfade = it }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // AI Artist Filter Card (00:35)
                                var aiArtistFilter by remember { mutableStateOf(true) }
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MochySurface,
                                    border = BorderStroke(1.dp, MochyCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("AI Artist filter", fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                "Turning this off may give you AI slop recommendations",
                                                fontSize = 12.sp,
                                                color = MochyTextSecondary
                                            )
                                        }
                                        Switch(
                                            checked = aiArtistFilter,
                                            onCheckedChange = { aiArtistFilter = it }
                                        )
                                    }
                                }
                            }

                            MochyPrimaryPillButton(
                                text = "next",
                                onClick = { showFinalCard = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MochyConfettiCardScreen(
    username: String,
    profileId: String,
    joinedDate: String,
    bottomTitle: String,
    bottomSubtitle: String,
    showTopCheck: Boolean = false,
    onTapContinue: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "confettiFall")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confettiY"
    )
    val particles = remember {
        List(36) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 360f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onTapContinue)
            .padding(24.dp)
    ) {
        if (!showTopCheck) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { (rx, ry, rot) ->
                    val x = rx * size.width
                    val y = ((ry + progress) % 1f) * size.height
                    rotate(rot + progress * 180f, pivot = Offset(x, y)) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(x, y),
                            size = Size(7.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTopCheck) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCE0E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = bottomTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(24.dp))
            }

            Box(modifier = Modifier.graphicsLayer { rotationZ = -6f }) {
                MochyProfileIdCard(
                    username = username,
                    profileId = profileId,
                    joinedDate = joinedDate,
                    metallicLight = true
                )
            }
        }

        if (!showTopCheck) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = bottomTitle,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (bottomSubtitle.isNotBlank()) {
                    Text(
                        text = bottomSubtitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun CrossfadeWaveformGraphic(
    labelLeft: String = "i miss you",
    labelRight: String = "aiii ai",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E0E12))
            .border(1.dp, MochyCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bars = 68
            val stepX = size.width / bars
            val midY = size.height / 2f
            for (i in 0 until bars) {
                val t = i.toFloat() / bars
                // Outgoing envelope (fades out) & Incoming envelope (fades in)
                val outEnv = (1f - t).coerceIn(0f, 1f)
                val inEnv = t.coerceIn(0f, 1f)
                val wave = (sin(i * 0.55f) * 0.4f + 0.6f)
                val hOut = midY * 0.85f * outEnv * wave
                val hIn = midY * 0.85f * inEnv * wave
                val x = i * stepX
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(x, midY - hOut),
                    end = Offset(x, midY + hOut),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(x, midY - hIn),
                    end = Offset(x, midY + hIn),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        Text(
            text = labelLeft,
            fontSize = 10.sp,
            color = MochyTextSecondary,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Text(
            text = labelRight,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. MULTI-STYLE NOW PLAYING SCREEN & CUSTOMIZE PLAYER CAROUSEL (FIXED!)
//    - Fixed: No button on NowPlayingScreen ever opens Customize Styles unexpectedly!
//    - Fixed: Real Synced Lyrics (`LyricsViewModel` + `LyricsTab` + `CustomLyricsDialog`)
//      work across ALL styles (Radial, Default, Turntable, Poster, Lyrics Stage)!
//    - Fixed: Vinyl LP & Album Art center alignment on Turntable + Poster layout bounds
//    - Fixed: `styleOverride` in `MochyPlayerStylesPickerScreen` so each card in the
//      HorizontalPager previews its own distinct style!
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochyMultiStyleNowPlayingScreen(
    mochyState: MochyUiStateHolder,
    title: String?,
    artist: String?,
    artUri: String?,
    durationMs: Long,
    isPlaying: Boolean,
    positionState: State<Long>,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onBackClick: () -> Unit,
    sleepTimerRemaining: Long = -1L,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    isShuffleOn: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    repeatMode: Int = 0,
    onCycleRepeatMode: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    canDownload: Boolean = true,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onRemoveDownloadClick: () -> Unit = {},
    audioQuality: AudioQualityInfo = AudioQualityInfo(),
    onOpenCustomizeStyles: () -> Unit = {},
    styleOverride: PlayerStyleMode? = null,
    isPreviewMode: Boolean = false,
    lyricsViewModel: LyricsViewModel? = if (isPreviewMode) null else hiltViewModel()
) {
    val activeStyle = styleOverride ?: mochyState.playerStyle
    val positionMs = positionState.value.coerceAtLeast(0L)
    val safeDuration = if (durationMs > 0L) durationMs else 1L
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).let {
        if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f)
    }
    val hasSong = !title.isNullOrBlank()

    var showSleepTimer by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showQualityDetails by remember { mutableStateOf(false) }
    var showCustomLyricsDialog by remember { mutableStateOf(false) }
    var showLyricsBottomSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Observe real synced lyrics from LyricsViewModel (when not in static style-picker preview)
    val lyricsState = lyricsViewModel?.lyricsState?.collectAsStateWithLifecycle()?.value
    val lyricsPositionState = lyricsViewModel?.currentPosition?.collectAsStateWithLifecycle()
        ?: positionState

    // Extract current active lyric line for the compact bottom lyrics pill on Default/Turntable/Poster
    val currentLyricSnippet = remember(lyricsState, positionMs) {
        when (val st = lyricsState) {
            is LyricsUiState.Loading -> "Loading lyrics..."
            is LyricsUiState.Found -> {
                val synced = st.result.syncedLyrics
                if (!synced.isNullOrBlank()) {
                    val currentLine = parseCurrentSyncedLine(synced, positionMs)
                    currentLine.ifBlank { st.result.plainLyrics?.lines()?.firstOrNull { it.isNotBlank() } ?: "Tap to view lyrics" }
                } else {
                    st.result.plainLyrics?.lines()?.firstOrNull { it.isNotBlank() } ?: "Tap to view lyrics"
                }
            }
            else -> "No lyrics found • Tap to add lyrics"
        }
    }

    // Dialogs (Audio Quality, Sleep Timer, Remove Download, Custom Lyrics, Full Lyrics Sheet)
    if (!isPreviewMode) {
        if (showQualityDetails) {
            AlertDialog(
                onDismissRequest = { showQualityDetails = false },
                containerColor = MochySurface,
                titleContentColor = Color.White,
                textContentColor = MochyTextSecondary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NeonLimePrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Audio Stream Details", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Source: ${audioQuality.sourceName}")
                        Text("Codec / Format: ${audioQuality.format.uppercase()}")
                        audioQuality.bitDepth?.let { Text("Bit Depth: $it-bit") }
                        audioQuality.sampleRateHz?.let { Text("Sample Rate: ${it / 1000.0} kHz") }
                        audioQuality.bitrateKbps?.let { Text("Bitrate: ~$it kbps") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDetails = false }) {
                        Text("Close", color = NeonLimePrimary)
                    }
                }
            )
        }

        if (showSleepTimer) {
            SleepTimerDialog(
                currentRemaining = sleepTimerRemaining,
                onDismiss = { showSleepTimer = false },
                onStart = { minutes -> onStartSleepTimer(minutes); showSleepTimer = false },
                onCancel = { onCancelSleepTimer(); showSleepTimer = false }
            )
        }

        if (showRemoveConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirm = false },
                containerColor = MochySurface,
                titleContentColor = Color.White,
                textContentColor = MochyTextSecondary,
                title = { Text("Remove Download?") },
                text = { Text("This will remove the downloaded audio from your device.") },
                confirmButton = {
                    TextButton(
                        onClick = { onRemoveDownloadClick(); showRemoveConfirm = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirm = false }) {
                        Text("Cancel", color = MochyTextSecondary)
                    }
                }
            )
        }

        if (showCustomLyricsDialog && lyricsViewModel != null) {
            val currentLyricsText = when (val st = lyricsState) {
                is LyricsUiState.Found -> st.result.syncedLyrics ?: st.result.plainLyrics ?: ""
                else -> ""
            }
            CustomLyricsDialog(
                initialLyrics = currentLyricsText,
                trackTitle = title,
                trackArtist = artist,
                onDismiss = { showCustomLyricsDialog = false },
                onSave = { customText -> lyricsViewModel.saveCustomLyrics(customText) },
                onDelete = if (lyricsState is LyricsUiState.Found) {
                    { lyricsViewModel.deleteLyrics() }
                } else null
            )
        }

        if (showOptionsMenu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 14.dp, end = 22.dp)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    containerColor = MochySurface
                ) {
                    if (hasSong && canDownload) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when {
                                        isDownloaded -> "Remove Offline Download"
                                        isDownloading -> "Downloading..."
                                        else -> "Download Offline"
                                    },
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = NeonLimePrimary
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                if (isDownloaded) showRemoveConfirm = true else if (!isDownloading) onDownloadClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Sleep Timer", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null, tint = NeonLimePrimary) },
                        onClick = {
                            showOptionsMenu = false
                            showSleepTimer = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Audio Details", color = Color.White) },
                        leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NeonLimePrimary) },
                        onClick = {
                            showOptionsMenu = false
                            showQualityDetails = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = MochyTextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = MochyTextSecondary) },
                        onClick = {
                            showOptionsMenu = false
                            onNavigateToSettings()
                        }
                    )
                }
            }
        }

        if (showLyricsBottomSheet && lyricsViewModel != null && activeStyle != PlayerStyleMode.RADIAL_MYPLAYER) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsBottomSheet = false },
                containerColor = MochyBlack,
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title ?: "Lyrics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist ?: "",
                                fontSize = 13.sp,
                                color = MochyTextSecondary,
                                maxLines = 1
                            )
                        }
                        TextButton(onClick = {
                            showLyricsBottomSheet = false
                            showCustomLyricsDialog = true
                        }) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = NeonLimePrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Lyrics", color = NeonLimePrimary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (lyricsState != null) {
                        LyricsTab(
                            lyricsState = lyricsState,
                            positionMs = lyricsPositionState,
                            onSeek = onSeek,
                            onAddCustomLyrics = {
                                showLyricsBottomSheet = false
                                showCustomLyricsDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    when (activeStyle) {
        // ── STYLE 1: YOUR COMPLETE SIGNATURE MYPLAYER RADIAL SCREEN ──────────
        PlayerStyleMode.RADIAL_MYPLAYER -> {
            NowPlayingVisual(
                title = title,
                artist = artist,
                artUri = artUri,
                durationMs = safeDuration,
                positionMs = positionMs,
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                isShuffleEnabled = isShuffleOn,
                repeatMode = repeatMode,
                onPlayPause = onPlayPauseClick,
                onPrevious = onPreviousClick,
                onNext = onNextClick,
                onSeek = onSeek,
                onFavorite = onToggleFavorite,
                onShuffle = onToggleShuffle,
                onRepeat = onCycleRepeatMode,
                onBack = onBackClick,
                onOptions = { showOptionsMenu = true },
                lyricsContent = {
                    if (!isPreviewMode && lyricsState != null) {
                        LyricsTab(
                            lyricsState = lyricsState,
                            positionMs = lyricsPositionState,
                            onSeek = onSeek,
                            onAddCustomLyrics = { showCustomLyricsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                        )
                    }
                }
            )
        }

                // ── STYLE 2: MOCHY DEFAULT STYLE (01:10 - 01:13, 01:39) ─────────────
        PlayerStyleMode.DEFAULT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF08141E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.60f)
                ) {
                    AlbumArtImage(
                        uri = artUri ?: "",
                        title = title,
                        artist = artist,
                        size = 500.dp,
                        shape = RoundedCornerShape(0.dp),
                        iconSize = 96.dp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color(0xFF08141E)
                                    )
                                )
                            )
                    )
                }

                // Top Bar: Back Button on Left + Grab Handle Center + Sleep/Download on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MochyCircleIconButton(icon = Icons.Default.KeyboardArrowDown, onClick = onBackClick)
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MochyCircleIconButton(
                            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            onClick = { if (isDownloaded) showRemoveConfirm = true else onDownloadClick() }
                        )
                        MochyCircleIconButton(
                            icon = Icons.Default.MoreVert,
                            onClick = { showOptionsMenu = true }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title ?: "No song playing",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (mochyState.audioFormatBadge) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(modifier = Modifier.clickable { showQualityDetails = true }) {
                                        MochyFormatBadge(audioQuality.format.ifBlank { "AAC" })
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = artist ?: "",
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MochyCircleIconButton(
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                onClick = onToggleFavorite
                            )
                            // Opens Lyrics Sheet (NOT Style Picker!)
                            MochyCircleIconButton(
                                icon = Icons.Default.Lyrics,
                                onClick = { showLyricsBottomSheet = true }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Slider(
                        value = progress,
                        onValueChange = { onSeek((it * safeDuration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(positionMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(formatMs(safeDuration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                null,
                                tint = if (isShuffleOn) NeonLimePrimary else Color.White.copy(alpha = 0.55f)
                            )
                        }
                        IconButton(onClick = onPreviousClick, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.FastRewind, null, tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                        IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(68.dp)) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = onNextClick, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                        IconButton(onClick = onCycleRepeatMode) {
                            Icon(
                                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                null,
                                tint = if (repeatMode != 0) NeonLimePrimary else Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Bottom Frosted Lyrics Pill — shows live synced lyric line & opens Lyrics Sheet on tap!
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clickable { showLyricsBottomSheet = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentLyricSnippet,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand Lyrics",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // ── STYLE 3: MOCHY TURNTABLE STYLE (FIXED CENTER ALIGNMENT & LYRICS) ─
        PlayerStyleMode.TURNTABLE -> {
            val infinite = rememberInfiniteTransition(label = "turntableSpin")
            val vinylAngle by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
                label = "lpRotation"
            )
            val effectiveAngle = if (isPlaying) vinylAngle else 0f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0A141D), Color(0xFF071A24), Color(0xFF050A0E))
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar: Back Button on Left and Options on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MochyCircleIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = onBackClick
                    )
                    MochyCircleIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { showOptionsMenu = true }
                    )
                }

                // Unified Turntable Platter Box: Vinyl Grooves & Center Artwork share the EXACT same center!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .padding(top = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .offset(x = (-48).dp, y = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            drawCircle(color = Color(0xFF0B0B0D), radius = r)
                            for (ring in 1..8) {
                                drawCircle(
                                    color = Color.White.copy(alpha = if (ring % 2 == 0) 0.07f else 0.03f),
                                    radius = r * (0.54f + ring * 0.052f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }

                        // Perfectly centered rotating Album Art inside the Vinyl LP
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .rotate(effectiveAngle)
                                .clip(CircleShape)
                                .border(4.dp, Color(0xFF1A1A1F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtImage(
                                uri = artUri ?: "",
                                title = title,
                                artist = artist,
                                size = 196.dp,
                                shape = CircleShape,
                                iconSize = 56.dp
                            )
                            // Vinyl center spindle hole
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0B0B0D))
                                    .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            )
                        }
                    }

                    // Tonearm on the Right (01:53)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pivotX = size.width * 0.86f
                        val pivotY = size.height * 0.12f
                        drawRoundRect(
                            color = Color(0xFF1C2026),
                            topLeft = Offset(pivotX - 28.dp.toPx(), pivotY - 22.dp.toPx()),
                            size = Size(56.dp.toPx(), 64.dp.toPx()),
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                        )
                        val endX = size.width * 0.62f
                        val endY = size.height * 0.72f
                        drawLine(
                            color = Color(0xFFD0D5DD),
                            start = Offset(pivotX, pivotY + 18.dp.toPx()),
                            end = Offset(endX, endY),
                            strokeWidth = 6.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        rotate(24f, pivot = Offset(endX, endY)) {
                            drawRoundRect(
                                color = Color(0xFF15181E),
                                topLeft = Offset(endX - 15.dp.toPx(), endY - 8.dp.toPx()),
                                size = Size(30.dp.toPx(), 50.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }

                // Vertical Floating Control Column on Bottom-Right (Heart, Prev, Pause, Next)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 136.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MochyPillControlButton(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        onClick = onToggleFavorite
                    )
                    MochyPillControlButton(icon = Icons.Default.FastRewind, onClick = onPreviousClick)
                    MochyPillControlButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = onPlayPauseClick
                    )
                    MochyPillControlButton(icon = Icons.Default.FastForward, onClick = onNextClick)
                }

                // Bottom Track Info + Seekbar + Lyrics Pill
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(end = 76.dp)
                    ) {
                        // Opens Lyrics Sheet (NOT Style Picker!)
                        MochyCircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            onClick = { showLyricsBottomSheet = true }
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title ?: "Khat",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.clickable { showQualityDetails = true }) {
                                    MochyFormatBadge(audioQuality.format.ifBlank { "AAC" })
                                }
                            }
                            Text(
                                text = artist ?: "Navjot Ahuja",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Slider(
                        value = progress,
                        onValueChange = { onSeek((it * safeDuration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    // Bottom Lyrics Pill — shows live synced lyric line & opens Lyrics Sheet on tap!
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { showLyricsBottomSheet = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentLyricSnippet,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Lyrics,
                                contentDescription = "Lyrics",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── STYLE 4: MOCHY POSTER STYLE (01:43 - 01:45, FIXED BOUNDS) ────────
        PlayerStyleMode.POSTER -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06131C))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.66f)
                ) {
                    AlbumArtImage(
                        uri = artUri ?: "",
                        title = title,
                        artist = artist,
                        size = 520.dp,
                        shape = RoundedCornerShape(0.dp),
                        iconSize = 96.dp
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val wavePath = Path().apply {
                            moveTo(0f, h * 0.90f)
                            quadraticBezierTo(w * 0.55f, h * 0.96f, w, h * 0.74f)
                        }
                        drawPath(
                            path = wavePath,
                            color = Color.White.copy(alpha = 0.35f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MochyCircleIconButton(
                        icon = Icons.Default.Lyrics,
                        onClick = { showLyricsBottomSheet = true }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MochyCircleIconButton(
                            icon = Icons.Default.MoreVert,
                            onClick = { showOptionsMenu = true }
                        )
                        MochyCircleIconButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            onClick = onBackClick
                        )
                    }
                }

                // Large Poster Title on Left with end padding so it never collides with right controls!
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 96.dp, bottom = 28.dp)
                ) {
                    Text(
                        text = title ?: "Khat",
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = artist ?: "Navjot Ahuja",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Slider(
                        value = progress,
                        onValueChange = { onSeek((it * safeDuration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        )
                    )
                }

                // Vertical Control Column on Bottom-Right
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MochyPillControlButton(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        onClick = onToggleFavorite
                    )
                    MochyPillControlButton(icon = Icons.Default.FastRewind, onClick = onPreviousClick)
                    MochyPillControlButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = onPlayPauseClick
                    )
                    MochyPillControlButton(icon = Icons.Default.FastForward, onClick = onNextClick)
                }
            }
        }

        // ── STYLE 5 & 6: MOCHY TIMELINE & LYRICS STAGE (01:46 - 01:49) ──────
        PlayerStyleMode.TIMELINE, PlayerStyleMode.LYRICS_STAGE -> {
            val isLyricsStage = activeStyle == PlayerStyleMode.LYRICS_STAGE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.52f)
                        .background(Color(0xFF536878))
                        .padding(24.dp)
                ) {
                    if (isLyricsStage) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 72.dp)
                        ) {
                            Text(
                                text = title ?: "Khat",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            if (!isPreviewMode && lyricsState != null) {
                                LyricsTab(
                                    lyricsState = lyricsState,
                                    positionMs = lyricsPositionState,
                                    onSeek = onSeek,
                                    onAddCustomLyrics = { showCustomLyricsDialog = true },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            } else {
                                Text(
                                    text = currentLyricSnippet,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(end = 52.dp)
                        ) {
                            AlbumArtImage(
                                uri = artUri ?: "",
                                title = title,
                                artist = artist,
                                size = 48.dp,
                                shape = RoundedCornerShape(10.dp),
                                iconSize = 24.dp
                            )
                            Column {
                                Text(title ?: "Khat", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                Text(artist ?: "Navjot Ahuja", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MochyCircleIconButton(
                            icon = Icons.Default.MoreVert,
                            onClick = { showOptionsMenu = true }
                        )
                        MochyCircleIconButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            onClick = onBackClick
                        )
                    }

                    Text(
                        text = formatMs(positionMs),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.48f)
                        .background(Color(0xFF34434E))
                        .padding(24.dp)
                ) {
                    Text(
                        text = formatMs(safeDuration),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    Slider(
                        value = progress,
                        onValueChange = { onSeek((it * safeDuration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MochyCircleIconButton(icon = Icons.Default.FastRewind, onClick = onPreviousClick)
                        MochyCircleIconButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = onPlayPauseClick
                        )
                        MochyCircleIconButton(icon = Icons.Default.FastForward, onClick = onNextClick)
                    }
                }
            }
        }
    }
}

private fun parseCurrentSyncedLine(lrcText: String, positionMs: Long): String {
    val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")
    var latestLine = ""
    for (rawLine in lrcText.lines()) {
        val match = regex.find(rawLine.trim()) ?: continue
        val min = match.groupValues[1].toLongOrNull() ?: 0L
        val sec = match.groupValues[2].toLongOrNull() ?: 0L
        val msStr = match.groupValues[3]
        val ms = when (msStr.length) {
            2 -> (msStr.toLongOrNull() ?: 0L) * 10L
            3 -> msStr.toLongOrNull() ?: 0L
            else -> 0L
        }
        val timeMs = (min * 60_000L) + (sec * 1_000L) + ms
        val text = match.groupValues[4].trim()
        if (timeMs <= positionMs + 300L && text.isNotBlank()) {
            latestLine = text
        }
    }
    return latestLine
}

/**
 * Player Style Picker Carousel Screen (`Styles` screen at 01:39 - 01:52 in video)
 * Each card passes `styleOverride = style` and `isPreviewMode = true` so all 6 styles
 * (Radial MyPlayer, Default, Turntable, Poster, Timeline, Lyrics stage) render distinctly!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MochyPlayerStylesPickerScreen(
    mochyState: MochyUiStateHolder,
    previewTitle: String = "Khat",
    previewArtist: String = "Navjot Ahuja",
    previewArtUri: String? = null,
    onBack: () -> Unit
) {
    val styles = PlayerStyleMode.entries
    val initialPage = styles.indexOf(mochyState.playerStyle).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { styles.size }
    val currentHighlighted = styles.getOrElse(pagerState.currentPage.coerceIn(0, styles.lastIndex)) { styles.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MochyBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MochySurface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Styles", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(currentHighlighted.title, fontSize = 13.sp, color = MochyTextSecondary)
            }
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.weight(1f)
        ) { page ->
            val style = styles[page]
            val isSelected = mochyState.playerStyle == style
            Surface(
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) NeonLimePrimary else MochyCardBorder
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
            ) {
                val dummyPos = remember { mutableLongStateOf(36_000L) }
                MochyMultiStyleNowPlayingScreen(
                    mochyState = mochyState,
                    title = previewTitle,
                    artist = previewArtist,
                    artUri = previewArtUri,
                    durationMs = 296_000L,
                    isPlaying = true,
                    positionState = dummyPos,
                    onPlayPauseClick = {},
                    onNextClick = {},
                    onPreviousClick = {},
                    onSeek = {},
                    onBackClick = {},
                    isFavorite = true,
                    styleOverride = style,
                    isPreviewMode = true
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            styles.forEachIndexed { idx, _ ->
                val active = idx == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.28f))
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MochySecondaryPillButton(
                text = "Cancel",
                modifier = Modifier.weight(1f),
                onClick = onBack
            )
            MochyPrimaryPillButton(
                text = if (mochyState.playerStyle == currentHighlighted) "Active ✓" else "Set Style",
                modifier = Modifier.weight(1f),
                onClick = {
                    val targetStyle = styles.getOrElse(pagerState.currentPage.coerceIn(0, styles.lastIndex)) { currentHighlighted }
                    runCatching { mochyState.updatePlayerStyle(targetStyle) }
                    onBack()
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. MOCHY SEARCH SCREEN: MOODS & MOMENTS + GENRES + TOP RESULT (00:46 - 01:09)
// ═════════════════════════════════════════════════════════════════════════════

data class MochyMoodGenreItem(val title: String, val bgColor: Color)

val MOCHY_MOODS = listOf(
    MochyMoodGenreItem("Chill", Color(0xFF425468)),
    MochyMoodGenreItem("Commute", Color(0xFF6B653B)),
    MochyMoodGenreItem("Energize", Color(0xFF3E6340)),
    MochyMoodGenreItem("Feel good", Color(0xFF3C5E3E)),
    MochyMoodGenreItem("Focus", Color(0xFF3B3B3F)),
    MochyMoodGenreItem("Gaming", Color(0xFF3A4968)),
    MochyMoodGenreItem("Party", Color(0xFF526638)),
    MochyMoodGenreItem("Romance", Color(0xFF5E2F36)),
    MochyMoodGenreItem("Sad", Color(0xFF3D385B)),
    MochyMoodGenreItem("Sleep", Color(0xFF46365E)),
    MochyMoodGenreItem("Workout", Color(0xFF664938))
)

val MOCHY_GENRES = listOf(
    MochyMoodGenreItem("African", Color(0xFF3B663E)),
    MochyMoodGenreItem("Arabic", Color(0xFF684738)),
    MochyMoodGenreItem("Bengali", Color(0xFF385E3D)),
    MochyMoodGenreItem("Bhojpuri", Color(0xFF593866)),
    MochyMoodGenreItem("Carnatic classical", Color(0xFF3B5A68)),
    MochyMoodGenreItem("Classical", Color(0xFF424248)),
    MochyMoodGenreItem("Country & Americana", Color(0xFF4A6638)),
    MochyMoodGenreItem("Dance & electronic", Color(0xFF385E56)),
    MochyMoodGenreItem("Desi hip-hop", Color(0xFF5D3A3A)),
    MochyMoodGenreItem("Hindi", Color(0xFF663B48)),
    MochyMoodGenreItem("Hip-hop", Color(0xFF684B38)),
    MochyMoodGenreItem("Indian Indie", Color(0xFF563866)),
    MochyMoodGenreItem("Indian pop", Color(0xFF633845)),
    MochyMoodGenreItem("Indie & alternative", Color(0xFF384D66)),
    MochyMoodGenreItem("J-Pop", Color(0xFF463866)),
    MochyMoodGenreItem("Jazz", Color(0xFF685838)),
    MochyMoodGenreItem("K-Pop", Color(0xFF523866)),
    MochyMoodGenreItem("Kannada", Color(0xFF586638)),
    MochyMoodGenreItem("Latin", Color(0xFF685F38)),
    MochyMoodGenreItem("Malayalam", Color(0xFF38663E)),
    MochyMoodGenreItem("Marathi", Color(0xFF385366)),
    MochyMoodGenreItem("Metal", Color(0xFF3C3C42)),
    MochyMoodGenreItem("Monsoon", Color(0xFF5D6638)),
    MochyMoodGenreItem("Pop", Color(0xFF5D3866)),
    MochyMoodGenreItem("Punjabi", Color(0xFF684838)),
    MochyMoodGenreItem("R&B & soul", Color(0xFF384766))
)

@Composable
fun MochyMoodGenreCard(
    item: MochyMoodGenreItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(item.bgColor)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)
    ) {
        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.55f)
        )

        // Two Tilted Overlapping Album Thumbnails on the Right (exact match to 00:46 - 00:50)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 76.dp, height = 62.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer { rotationZ = -12f }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer { rotationZ = 10f }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. MOCHY AUDIO LAB: 10-BAND GRAPH & SWEEPABLE BANDS + AUTOEQ + AUDIO FX
//    (02:46 - 04:34 IN VIDEO)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochyEqualizerScreen(
    mochyState: MochyUiStateHolder,
    onApplyGainsToEngine: (enabled: Boolean, gains10: List<Float>, preampDb: Float) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Bands") } // Graph | Bands | AutoEQ
    var selectedNodeIdx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MochyBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MochySurface,
                modifier = Modifier.clickable(onClick = onBack)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Audio Lab", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Equalizer", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(14.dp))

        // Master Equalizer On/Off Pill Card (02:49, 03:05)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MochySurface,
            border = BorderStroke(1.dp, MochyCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (mochyState.eqEnabled) Color.White else Color.Gray)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Equalizer", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (mochyState.eqEnabled) "On • ${mochyState.eqPreset.label}" else "Off",
                            fontSize = 12.sp,
                            color = MochyTextSecondary
                        )
                    }
                }
                Switch(
                    checked = mochyState.eqEnabled,
                    onCheckedChange = {
                        mochyState.eqEnabled = it
                        onApplyGainsToEngine(it, mochyState.eqBands, mochyState.preampDb)
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 3-Tab Switcher: Graph | Bands | AutoEQ (02:49 - 03:22)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MochySurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf("Graph", "Bands", "AutoEQ").forEach { tab ->
                    val active = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (active) Color.White else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (selectedTab != "AutoEQ") {
            // Presets Header + Reset / + Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PRESETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "↻ Reset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.clickable {
                            mochyState.eqPreset = MochyEqPreset.FLAT
                            mochyState.eqBands = MochyEqPreset.FLAT.gains.toList()
                            onApplyGainsToEngine(mochyState.eqEnabled, mochyState.eqBands, mochyState.preampDb)
                        }
                    )
                    Text(
                        text = "+ Save",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Horizontal Presets Row (Flat, Acoustic, Bass Booster, Classical, Dance, Pop, Rock...)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MochyEqPreset.entries) { preset ->
                    val active = mochyState.eqPreset == preset
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) Color.White else MochySurface,
                        modifier = Modifier.clickable {
                            mochyState.eqPreset = preset
                            mochyState.eqBands = preset.gains.toList()
                            mochyState.eqEnabled = true
                            onApplyGainsToEngine(true, mochyState.eqBands, mochyState.preampDb)
                        }
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }

        when (selectedTab) {
            // ── TAB 1: GRAPH EDITOR (02:49 - 02:58) ─────────────────────────
            "Graph" -> {
                Text("GRAPH EDITOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FREQUENCY RESPONSE", fontSize = 11.sp, color = MochyTextSecondary)
                            val nodeFreq = Eq10Frequencies[selectedNodeIdx]
                            val nodeGain = mochyState.eqBands[selectedNodeIdx]
                            Text(
                                "${nodeFreq}Hz  ${"%.1f".format(nodeGain)} dB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EqRainbowColors[selectedNodeIdx]
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        MochyRainbowEqCanvas(
                            bands = mochyState.eqBands,
                            onBandChanged = { idx, newDb ->
                                selectedNodeIdx = idx
                                val updated = mochyState.eqBands.toMutableList()
                                updated[idx] = newDb
                                mochyState.eqBands = updated
                                mochyState.eqPreset = MochyEqPreset.CUSTOM
                                mochyState.eqEnabled = true
                                onApplyGainsToEngine(true, updated, mochyState.preampDb)
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Drag a node to shape the sound • double-tap to reset",
                            fontSize = 11.sp,
                            color = MochyTextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            // ── TAB 2: SWEEPABLE 10-BAND SLIDERS (02:59 - 03:21, 03:28) ────
            "Bands" -> {
                Text("BANDS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MochySweepable10BandSliders(
                            bands = mochyState.eqBands,
                            onBandsUpdated = { updated ->
                                mochyState.eqBands = updated
                                mochyState.eqPreset = MochyEqPreset.CUSTOM
                                mochyState.eqEnabled = true
                                onApplyGainsToEngine(true, updated, mochyState.preampDb)
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Sweep across the sliders to draw a curve",
                            fontSize = 11.sp,
                            color = MochyTextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("OUTPUT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Preamp", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${"%.1f".format(mochyState.preampDb)} dB", color = MochyTextSecondary)
                        }
                        Slider(
                            value = mochyState.preampDb,
                            onValueChange = {
                                mochyState.preampDb = it
                                onApplyGainsToEngine(mochyState.eqEnabled, mochyState.eqBands, it)
                            },
                            valueRange = -12f..12f
                        )
                        HorizontalDivider(color = MochyCardBorder)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto preamp", fontWeight = FontWeight.Bold, color = Color.White)
                                val maxBoost = (mochyState.eqBands.maxOrNull() ?: 0f).coerceAtLeast(0f)
                                Text(
                                    "Lowering by ${"%.1f".format(maxBoost)} dB, the limiter catches the rest",
                                    fontSize = 12.sp,
                                    color = MochyTextSecondary
                                )
                            }
                            Switch(
                                checked = mochyState.autoPreamp,
                                onCheckedChange = { mochyState.autoPreamp = it }
                            )
                        }
                    }
                }
            }

            // ── TAB 3: AUTOEQ HEADPHONE DATABASE (03:22 - 03:27) ───────────
            "AutoEQ" -> {
                var searchModel by remember { mutableStateOf("") }
                val models = remember {
                    listOf(
                        "1Custom SA02" to "crinacle - 711",
                        "1Custom SA03" to "crinacle - 711",
                        "1MORE Aero (ANC Off)" to "HypetheSonics - GRAS RA0045",
                        "1MORE Aero (ANC On)" to "HypetheSonics - GRAS RA0045",
                        "1MORE ComfoBuds Mini" to "DHRME",
                        "Apple AirPods Pro 2" to "oratory1990",
                        "Sony WH-1000XM5" to "oratory1990",
                        "Sennheiser HD 600" to "oratory1990"
                    )
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Database", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("6,033 models", fontSize = 12.sp, color = MochyTextSecondary)
                        }
                        Text("AutoEq • measured by reviewers", fontSize = 12.sp, color = MochyTextSecondary)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchModel,
                            onValueChange = { searchModel = it },
                            singleLine = true,
                            placeholder = { Text("Search model (e.g. HD 600)", color = MochyTextSecondary) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        models.filter { it.first.contains(searchModel, ignoreCase = true) }.forEach { (name, source) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mochyState.selectedAutoEqModel = name
                                        mochyState.eqBands = listOf(2.5f, 3.0f, 1.5f, -0.5f, -1.5f, 0.5f, 2.5f, 3.5f, 2.0f, 1.0f)
                                        mochyState.eqEnabled = true
                                        onApplyGainsToEngine(true, mochyState.eqBands, mochyState.preampDb)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Headphones, null, tint = MochyTextSecondary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(source, fontSize = 12.sp, color = MochyTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

/**
 * Interactive 10-Band Slider Matrix where dragging horizontally OR vertically sweeps
 * across all 10 rainbow bands smoothly ("Sweep across the sliders to draw a curve" - 03:03 & 03:28)
 */
@Composable
fun MochySweepable10BandSliders(
    bands: List<Float>,
    onBandsUpdated: (List<Float>) -> Unit
) {
    val currentBands by rememberUpdatedState(bands)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top dB Value Labels (+4.5, +6.5, etc.)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            currentBands.forEach { db ->
                val text = when {
                    abs(db) < 0.1f -> "0"
                    db > 0f -> "+${if (db % 1f == 0f) db.toInt() else "%.1f".format(db)}"
                    else -> "${if (db % 1f == 0f) db.toInt() else "%.1f".format(db)}"
                }
                Text(
                    text = text,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (abs(db) > 0.1f) Color.White else MochyTextSecondary,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Interactive Canvas supporting both single-band vertical drag AND multi-band horizontal sweep!
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(Unit) {
                    fun updateAtPointer(pos: Offset) {
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val bandIdx = ((pos.x / w) * 10f).toInt().coerceIn(0, 9)
                        val fraction = (1f - (pos.y / h)).coerceIn(0f, 1f)
                        val rawDb = (fraction * 24f) - 12f
                        val snappedDb = if (abs(rawDb) < 0.4f) 0f else (round(rawDb * 2f) / 2f)
                        val mutable = currentBands.toMutableList()
                        if (mutable[bandIdx] != snappedDb) {
                            mutable[bandIdx] = snappedDb
                            onBandsUpdated(mutable)
                        }
                    }

                    detectTapGestures(onPress = { pos -> updateAtPointer(pos) })
                }
                .pointerInput(Unit) {
                    fun updateAtPointer(pos: Offset) {
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val bandIdx = ((pos.x / w) * 10f).toInt().coerceIn(0, 9)
                        val fraction = (1f - (pos.y / h)).coerceIn(0f, 1f)
                        val rawDb = (fraction * 24f) - 12f
                        val snappedDb = if (abs(rawDb) < 0.4f) 0f else (round(rawDb * 2f) / 2f)
                        val mutable = currentBands.toMutableList()
                        if (mutable[bandIdx] != snappedDb) {
                            mutable[bandIdx] = snappedDb
                            onBandsUpdated(mutable)
                        }
                    }

                    detectDragGestures(
                        onDragStart = { pos -> updateAtPointer(pos) },
                        onDrag = { change, _ ->
                            change.consume()
                            updateAtPointer(change.position)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val colW = w / 10f

            // Connect curve line behind thumbs
            val curvePath = Path()
            currentBands.forEachIndexed { idx, db ->
                val cx = colW * idx + colW / 2f
                val cy = midY - (db / 12f) * (midY * 0.85f)
                if (idx == 0) curvePath.moveTo(cx, cy) else curvePath.lineTo(cx, cy)
            }
            drawPath(
                path = curvePath,
                color = Color.White.copy(alpha = 0.16f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw each band track, active rainbow bar, and pill thumb
            currentBands.forEachIndexed { idx, db ->
                val cx = colW * idx + colW / 2f
                val cy = midY - (db / 12f) * (midY * 0.85f)
                val bandColor = EqRainbowColors[idx]

                // Vertical track line
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(cx, h * 0.06f),
                    end = Offset(cx, h * 0.94f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Active colored bar from center (0 dB) to current dB
                drawLine(
                    color = bandColor,
                    start = Offset(cx, midY),
                    end = Offset(cx, cy),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Horizontal white pill thumb (exact match to 03:08 - 03:20)
                drawRoundRect(
                    color = if (abs(db) > 0.1f) Color.White else Color(0xFF7C7C86),
                    topLeft = Offset(cx - 11.dp.toPx(), cy - 5.dp.toPx()),
                    size = Size(22.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom Frequency Labels (31, 62, 125, 250, 500, 1.0k, 2.0k, 4.0k, 8.0k, 16k)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Eq10Frequencies.forEach { freq ->
                Text(
                    text = freq,
                    fontSize = 10.sp,
                    color = MochyTextSecondary,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MochyRainbowEqCanvas(
    bands: List<Float>,
    onBandChanged: (index: Int, newGainDb: Float) -> Unit
) {
    val currentBands by rememberUpdatedState(bands)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .pointerInput(Unit) {
                fun handleTouch(pos: Offset) {
                    val idx = ((pos.x / size.width.toFloat()) * 10f).toInt().coerceIn(0, 9)
                    val frac = (1f - (pos.y / size.height.toFloat())).coerceIn(0f, 1f)
                    val db = round(((frac * 24f) - 12f) * 2f) / 2f
                    onBandChanged(idx, db.coerceIn(-12f, 12f))
                }
                detectDragGestures(
                    onDragStart = { handleTouch(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        handleTouch(change.position)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1.dp.toPx())

        val stepX = w / 9f
        val path = Path()
        currentBands.forEachIndexed { i, db ->
            val x = i * stepX
            val y = midY - (db / 12f) * (midY * 0.82f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = 2.5.dp.toPx()))

        currentBands.forEachIndexed { i, db ->
            val x = i * stepX
            val y = midY - (db / 12f) * (midY * 0.82f)
            drawCircle(color = EqRainbowColors[i], radius = 7.dp.toPx(), center = Offset(x, y))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 7. SHARED MOCHY UI ATOMS (PILL BUTTONS, BADGES, CIRCLE CONTROLS)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochyPrimaryPillButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3A3A42),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF222228),
            disabledContentColor = MochyTextSecondary
        ),
        modifier = modifier.height(56.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MochySecondaryPillButton(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1B1B20),
            contentColor = Color.White
        ),
        modifier = modifier.height(56.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MochyFormatBadge(formatText: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Text(
            text = formatText.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MochyCircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun MochyPillControlButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF18222C).copy(alpha = 0.88f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60L, totalSec % 60L)
}

// ═════════════════════════════════════════════════════════════════════════════
// 8. MOCHY HOME SCREEN + HERO NOW-PLAYING BANNER + PROFILE SHEET (00:40-00:45, 01:23, 04:45-04:50)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochyProfileSheetDialog(
    mochyState: MochyUiStateHolder,
    totalListeningHours: Int = 0,
    totalListeningMinutes: Int = 0,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var editingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(mochyState.username) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MochyBlack,
            border = BorderStroke(1.dp, MochyCardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MochyPlayLogo(modifier = Modifier.size(28.dp))
                    Text(
                        text = "Hello again, ${mochyState.username}!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                MochyProfileIdCard(
                    username = mochyState.username,
                    profileId = mochyState.profileId,
                    joinedDate = mochyState.joinedDate,
                    metallicLight = false,
                    onEditClick = { editingName = !editingName }
                )

                if (editingName) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                mochyState.completeOnboarding(tempName, mochyState.pickedArtists)
                                editingName = false
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Total Listening Time Card ("0h | 0m Total Listening time" - 01:24)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$totalListeningHours", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("h  ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                                Text("|  ", fontSize = 26.sp, color = MochyCardBorder)
                                Text("$totalListeningMinutes", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("m", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                            }
                            Text("Total Listening\ntime", fontSize = 12.sp, color = MochyTextSecondary, textAlign = TextAlign.End)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("View Stats >", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MochyTextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Version & About App Card ("8.2.3" - 01:24)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Version & about app", fontSize = 12.sp, color = MochyTextSecondary)
                            Text("8.2.3", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MochySurfaceHigh
                        ) {
                            Text(
                                text = "About app",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MochySurface,
                    border = BorderStroke(1.dp, MochyCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenSettings()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Open settings", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MochyHeroNowPlayingBanner(
    currentTitle: String?,
    currentArtist: String?,
    currentArtUri: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onBannerClick: () -> Unit
) {
    if (currentTitle.isNullOrBlank()) {
        // Empty Hero State (00:40 - "Nothing here yet / Play something and it shows up here")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text("Nothing here yet", fontSize = 13.sp, color = MochyTextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Play something and it shows up\nhere",
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    } else {
        // Active Full-Bleed Hero Banner (04:46 - 04:50 - "Now playing / Khat - Navjot Ahuja / Pause")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clickable(onClick = onBannerClick)
        ) {
            AlbumArtImage(
                uri = currentArtUri ?: "",
                title = currentTitle,
                artist = currentArtist,
                size = 420.dp,
                shape = RoundedCornerShape(0.dp),
                iconSize = 84.dp
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                MochyBlack
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Text("Now playing", fontSize = 13.sp, color = Color.White.copy(alpha = 0.78f))
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$currentTitle - ${currentArtist ?: ""}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.clickable(onClick = onPlayPauseClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 9. MOCHY LIBRARY SCREEN (ALL | LOCAL | CACHED | IMPORTED - 00:51 - 00:59)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochyLibraryHeaderAndTabs(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    spotifyTrackCount: Int = 0,
    youtubeTrackCount: Int = 0,
    onImportClick: () -> Unit,
    onManageCacheClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text("Library", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            singleLine = true,
            placeholder = {
                Text(
                    if (selectedTab == "Cached") "Search your cached music" else "Search anything on your library",
                    color = MochyTextSecondary
                )
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MochySurface,
                unfocusedContainerColor = MochySurface,
                focusedBorderColor = MochyCardBorder,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(14.dp))

        // Filter Pills: All | Local | Cached | Imported (00:51 - 00:56)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Local", "Cached", "Imported").forEach { tab ->
                val active = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) Color.White else MochySurface,
                    modifier = Modifier.clickable { onSelectTab(tab) }
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTab == "All" || selectedTab == "Imported") {
            // "Import your music here!" Card with Platform Tiles (00:51 - 00:58)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onImportClick)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            Color(0xFF1DB954) to "S",
                            Color(0xFFFA2D48) to "♪",
                            Color(0xFF1E1E24) to "❖",
                            Color(0xFFFF0033) to "▶"
                        ).forEach { (bg, symbol) ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Import your music here!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        if (selectedTab == "Imported") {
            // 2x2 Platform Counter Grids for ALL MUSIC & PLAYLISTS (00:56)
            Spacer(Modifier.height(18.dp))
            Text("ALL MUSIC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
            Spacer(Modifier.height(8.dp))
            MochyPlatformCountGrid(spotifyCount = spotifyTrackCount, youtubeCount = youtubeTrackCount)

            Spacer(Modifier.height(18.dp))
            Text("PLAYLISTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
            Spacer(Modifier.height(8.dp))
            MochyPlatformCountGrid(spotifyCount = 0, youtubeCount = 0)
        }
    }
}

@Composable
fun MochyPlatformCountGrid(spotifyCount: Int, youtubeCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MochyPlatformStatCard("Spotify", spotifyCount, Color(0xFF1DB954), Modifier.weight(1f))
            MochyPlatformStatCard("YouTube", youtubeCount, Color(0xFFFF0033), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MochyPlatformStatCard("Apple Music", 0, Color(0xFFFA2D48), Modifier.weight(1f))
            MochyPlatformStatCard("Tidal", 0, Color.White, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MochyPlatformStatCard(name: String, count: Int, dotColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MochySurface,
        border = BorderStroke(1.dp, MochyCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(8.dp))
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text("$count", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 10. MOCHY SETTINGS HUB, APPEARANCE, AUDIO LAB & AUDIO FX (01:26 - 04:44)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochySettingsHubScreen(
    mochyState: MochyUiStateHolder,
    onApplyEqToEngine: (enabled: Boolean, gains10: List<Float>, preampDb: Float) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    var subScreen by remember { mutableStateOf("root") }
    var searchQuery by remember { mutableStateOf("") }

    when (subScreen) {
        "styles_picker" -> {
            MochyPlayerStylesPickerScreen(
                mochyState = mochyState,
                onBack = { subScreen = "appearance" }
            )
        }
        "equalizer" -> {
            MochyEqualizerScreen(
                mochyState = mochyState,
                onApplyGainsToEngine = onApplyEqToEngine,
                onBack = { subScreen = "audio_lab" }
            )
        }
        "appearance" -> {
            MochyAppearanceSettingsScreen(
                mochyState = mochyState,
                onOpenStylesPicker = { subScreen = "styles_picker" },
                onBack = { subScreen = "root" }
            )
        }
        "audio_lab" -> {
            MochyAudioLabHubScreen(
                mochyState = mochyState,
                onOpenEqualizer = { subScreen = "equalizer" },
                onBack = { subScreen = "root" }
            )
        }
        else -> {
            // Root Settings Menu (01:26, 04:35)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MochyBlack)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MochyPlayLogo(modifier = Modifier.size(32.dp))
                        MochyCircleIconButton(icon = Icons.Default.PersonOutline, onClick = onBack)
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Search any settings", color = MochyTextSecondary) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MochySurface,
                            unfocusedContainerColor = MochySurface,
                            focusedBorderColor = MochyCardBorder,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Recommended Item Card ("Configure Equalizer settings" - 01:34, 04:43)
                item {
                    Text("Recommended item", fontSize = 12.sp, color = MochyTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MochySurface,
                        border = BorderStroke(1.dp, MochyCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = "equalizer" }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MochySurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Configure Equalizer settings", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("settings>audio lab", fontSize = 12.sp, color = MochyTextSecondary)
                            }
                        }
                    }
                }

                // Settings Category Cards (General, Appearance, Playback, Gesture, Storage, Audio Lab, Modules, Integrations)
                val categories = listOf(
                    Triple("General", Icons.Default.Settings, "general"),
                    Triple("Appearance", Icons.Default.Contrast, "appearance"),
                    Triple("Playback", Icons.Default.PlayArrow, "playback"),
                    Triple("Gesture", Icons.Default.TouchApp, "gesture"),
                    Triple("Storage", Icons.Default.Storage, "storage"),
                    Triple("Audio Lab", Icons.Default.GraphicEq, "audio_lab"),
                    Triple("Modules", Icons.Default.Widgets, "modules"),
                    Triple("Integrations", Icons.Default.Extension, "integrations")
                )

                items(categories.filter { it.first.contains(searchQuery, ignoreCase = true) }) { (title, icon, route) ->
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MochySurface,
                        border = BorderStroke(1.dp, MochyCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subScreen = route }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(MochySurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                item { Spacer(Modifier.height(90.dp)) }
            }
        }
    }
}

@Composable
fun MochyAppearanceSettingsScreen(
    mochyState: MochyUiStateHolder,
    onOpenStylesPicker: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MochyBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MochySurface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text("Appearance", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text("Player appearance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
        }

        // Customize Player Row -> Opens the 6-Style Pager (including Radial MyPlayer!)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenStylesPicker)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Album, null, tint = Color.White)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Customize player", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Active: ${mochyState.playerStyle.title} • Tap to switch (Radial, Turntable, Poster...)",
                            fontSize = 12.sp,
                            color = NeonLimePrimary
                        )
                    }
                }
            }
        }

        // Player & Global Appearance Toggles (02:00 - 02:18)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MochySwitchRow(
                        title = "Lyrics Shader",
                        subtitle = "Glow on sung lyrics",
                        checked = mochyState.lyricsShader,
                        onCheckedChange = { mochyState.lyricsShader = it }
                    )
                    MochySwitchRow(
                        title = "High quality bloom",
                        subtitle = "Extra soft bloom pass on lyrics",
                        checked = mochyState.highQualityBloom,
                        onCheckedChange = { mochyState.highQualityBloom = it }
                    )
                    MochySwitchRow(
                        title = "Audio format badge",
                        subtitle = "What the song is playing as, beside title",
                        checked = mochyState.audioFormatBadge,
                        onCheckedChange = { mochyState.audioFormatBadge = it }
                    )
                    MochySwitchRow(
                        title = "Show sample rate",
                        subtitle = "The stream's sample rate after its format",
                        checked = mochyState.showSampleRate,
                        onCheckedChange = { mochyState.showSampleRate = it }
                    )
                    MochySwitchRow(
                        title = "Enable animated canvas",
                        subtitle = "Looping visuals for supported tracks",
                        checked = mochyState.enableAnimatedCanvas,
                        onCheckedChange = { mochyState.enableAnimatedCanvas = it }
                    )
                }
            }
        }

        item {
            Text("Global Appearance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MochySwitchRow(
                        title = "Blur",
                        subtitle = "Glass blur across player and navigation",
                        checked = mochyState.glassBlur,
                        onCheckedChange = { mochyState.glassBlur = it }
                    )
                    MochySwitchRow(
                        title = "Progressive blur",
                        subtitle = "Smooth progressive gradient blur",
                        checked = mochyState.progressiveBlur,
                        onCheckedChange = { mochyState.progressiveBlur = it }
                    )
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Blur strength", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${mochyState.blurStrengthPx.toInt()}px", color = MochyTextSecondary)
                        }
                        Slider(
                            value = mochyState.blurStrengthPx,
                            onValueChange = { mochyState.blurStrengthPx = it },
                            valueRange = 0f..50f
                        )
                    }
                    MochySwitchRow(
                        title = "Animation",
                        subtitle = "Smooth UI transitions and element blurs",
                        checked = mochyState.smoothAnimations,
                        onCheckedChange = { mochyState.smoothAnimations = it }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

// 11. REAL-TIME MOCHY AUDIO FX DSP ENGINE & FULL PIXEL-ACCURATE AUDIO FX SCREEN
//     (100% Matching 00:00 - 01:01 of Uploaded Video + Mochy-Music DSP Formulas)
// ═════════════════════════════════════════════════════════════════════════════

enum class MochyReverbPreset(
    val label: String,
    val mix: Float,
    val roomSize: Float,
    val damping: Float,
    val wet: Float,
    val dry: Float,
    val width: Float,
    val preDelayMs: Float,
    val lowCutHz: Float
) {
    ROOM("Room", 0.50f, 0.55f, 0.45f, 0.60f, 1.00f, 1.00f, 12f, 180f),
    STUDIO("Studio", 0.40f, 0.30f, 0.65f, 0.45f, 1.00f, 0.80f, 5f, 250f),
    BATHROOM("Bathroom", 0.50f, 0.42f, 0.05f, 0.70f, 1.00f, 0.60f, 2f, 120f),
    PLATE("Plate", 0.45f, 0.65f, 0.10f, 0.55f, 1.00f, 1.00f, 0f, 300f),
    HALL("Hall", 0.50f, 0.78f, 0.35f, 0.60f, 1.00f, 1.00f, 25f, 150f),
    CATHEDRAL("Cathedral", 0.55f, 0.95f, 0.20f, 0.70f, 1.00f, 1.00f, 40f, 120f),
    STADIUM("Stadium", 0.60f, 0.90f, 0.50f, 0.65f, 1.00f, 1.00f, 60f, 200f),
    DREAMY("Dreamy", 0.70f, 0.93f, 0.60f, 0.85f, 1.00f, 1.00f, 30f, 300f)
}

enum class MochyLoudnessPreset(val label: String, val boostDb: Float, val compression: Float) {
    GENTLE("Gentle", 2.0f, 0.15f),
    PUNCHY("Punchy", 4.0f, 0.30f),
    NIGHT_MODE("Night mode", 3.0f, 0.85f),
    MAX("Max", 8.0f, 0.50f)
}

@Composable
fun MochyAudioLabHubScreen(
    mochyState: MochyUiStateHolder,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit
) {
    var showFullAudioFx by remember { mutableStateOf(false) }

    if (showFullAudioFx) {
        MochyAudioFxScreen(
            mochyState = mochyState,
            onBack = { showFullAudioFx = false }
        )
        return
    }

    val activeFxCount = listOf(
        mochyState.reverbEnabled,
        mochyState.loudnessEnabled,
        mochyState.spatialAudioEnabled,
        mochyState.monoAudio,
        mochyState.dolbyAtmosSurround,
        mochyState.playbackSpeed != 1.0f || !mochyState.preservePitch,
        mochyState.exponentialVolume
    ).count { it }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MochyBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MochySurface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text("Audio\nLab", fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }

        // 1. Equalizer Card
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clickable(onClick = onOpenEqualizer)
            ) {
                Box(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    Text(
                        text = if (mochyState.eqEnabled) "● ${mochyState.eqPreset.label.uppercase()}" else "● OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mochyState.eqEnabled) MochyAccentGreen else MochyTextSecondary,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    Text(
                        text = "Equalizer",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.size(84.dp).align(Alignment.CenterEnd).rotate(-14f)
                    )
                }
            }
        }

        // 2. Audio Effect Card -> Opens the full 00:00-01:01 MochyAudioFxScreen!
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clickable { showFullAudioFx = true }
            ) {
                Box(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    Text(
                        text = if (activeFxCount > 0) "● $activeFxCount ACTIVE" else "● ALL OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeFxCount > 0) MochyAccentGreen else MochyTextSecondary,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    Text(
                        text = "Audio Effect",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    Text(
                        text = "FX",
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.align(Alignment.CenterEnd).rotate(-10f)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}

/**
 * COMPLETE MOCHY AUDIO FX SCREEN (00:00 - 01:01 in Uploaded Video)
 * Every single card, animated visualizer, preset chip, and fine-tune slider is wired
 * to `MochyAudioFxDspBridge` for immediate real-time audio transformation!
 */
@Composable
fun MochyAudioFxScreen(
    mochyState: MochyUiStateHolder,
    onBack: () -> Unit
) {
    var selectedReverbPreset by remember { mutableStateOf(MochyReverbPreset.ROOM) }
    var selectedLoudnessPreset by remember { mutableStateOf(MochyLoudnessPreset.PUNCHY) }
    val fxContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var loudnessBoostDb by remember { mutableFloatStateOf(MochyAudioFxDspBridge.loudnessBoostDb) }
    var loudnessCompression by remember { mutableFloatStateOf(MochyAudioFxDspBridge.loudnessCompression) }
    var reverbDry by remember { mutableFloatStateOf(MochyAudioFxDspBridge.reverbDry) }
    var reverbWidth by remember { mutableFloatStateOf(MochyAudioFxDspBridge.reverbWidth) }
    var reverbPreDelayMs by remember { mutableFloatStateOf(MochyAudioFxDspBridge.reverbPreDelayMs) }
    var reverbLowCutHz by remember { mutableFloatStateOf(MochyAudioFxDspBridge.reverbLowCutHz) }
    var speakerAngleDeg by remember { mutableFloatStateOf(MochyAudioFxDspBridge.speakerAngleDeg) }

    // Sync any state change straight into the real-time DSP bridge!
    LaunchedEffect(
        mochyState.reverbEnabled, mochyState.reverbMix, mochyState.reverbRoomSize,
        mochyState.reverbDamping, mochyState.reverbWet, reverbDry, reverbWidth,
        reverbPreDelayMs, reverbLowCutHz, mochyState.loudnessEnabled, loudnessBoostDb,
        loudnessCompression, mochyState.evenOutSongs, mochyState.replayGainPreampDb,
        mochyState.spatialAudioEnabled, mochyState.spatialMode, mochyState.headShadowHz,
        mochyState.crossfeedDb, speakerAngleDeg, mochyState.spatialAmount,
        mochyState.monoAudio, mochyState.dolbyAtmosSurround, mochyState.playbackSpeed,
        mochyState.preservePitch, mochyState.pitchSemitones, mochyState.exponentialVolume
    ) {
        MochyAudioFxDspBridge.reverbDry = reverbDry
        MochyAudioFxDspBridge.reverbWidth = reverbWidth
        MochyAudioFxDspBridge.reverbPreDelayMs = reverbPreDelayMs
        MochyAudioFxDspBridge.reverbLowCutHz = reverbLowCutHz
        MochyAudioFxDspBridge.loudnessBoostDb = loudnessBoostDb
        MochyAudioFxDspBridge.loudnessCompression = loudnessCompression
        MochyAudioFxDspBridge.speakerAngleDeg = speakerAngleDeg
        MochyAudioFxDspBridge.syncFromUiState(mochyState, selectedLoudnessPreset)
        MochyAudioFxDspBridge.saveFineTune(fxContext)
    }

    val activeFxNames = remember(
        mochyState.reverbEnabled, mochyState.loudnessEnabled,
        mochyState.spatialAudioEnabled, mochyState.monoAudio,
        mochyState.dolbyAtmosSurround, mochyState.playbackSpeed,
        mochyState.exponentialVolume
    ) {
        buildList {
            if (mochyState.loudnessEnabled) add("LOUDNESS")
            if (mochyState.reverbEnabled) add("REVERB")
            if (mochyState.spatialAudioEnabled) add("SPATIAL")
            if (mochyState.dolbyAtmosSurround) add("SURROUND")
            if (mochyState.monoAudio) add("MONO")
            if (mochyState.playbackSpeed != 1.0f || !mochyState.preservePitch) add("SPEED")
            if (mochyState.exponentialVolume) add("VOLUME")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MochyBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── TOP HEADER: "Audio / FX" + Tilted Active FX Card Stack (00:00, 01:00) ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(MochySurface)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Audio", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C7C86))
                    Text("FX", fontSize = 68.sp, lineHeight = 68.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                // Tilted Active FX Stack on Top-Right (exact match to 00:02 & 01:00)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = 18.dp)
                ) {
                    if (activeFxNames.size > 3) {
                        Text(
                            text = "and ${activeFxNames.size - 3} other",
                            fontSize = 11.sp,
                            color = MochyTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp, end = 12.dp)
                        )
                    }
                    Box(modifier = Modifier.size(width = 175.dp, height = 165.dp)) {
                        val displayCards = if (activeFxNames.isEmpty()) listOf("SPATIAL") else activeFxNames.take(3)
                        displayCards.asReversed().forEachIndexed { revIdx, fxName ->
                            val idx = displayCards.size - 1 - revIdx
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF17171C),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier
                                    .size(width = 132.dp, height = 145.dp)
                                    .offset(x = (idx * (-16)).dp, y = (idx * 8).dp)
                                    .graphicsLayer { rotationZ = -10f + idx * 4f }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (activeFxNames.isNotEmpty()) "ON" else "OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeFxNames.isNotEmpty()) MochyAccentGreen else MochyTextSecondary
                                            )
                                            Text(
                                                text = "${idx + 1} of ${activeFxNames.size.coerceAtLeast(1)}",
                                                fontSize = 9.sp,
                                                color = MochyTextSecondary
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = fxName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }

                                    // Mini bar visualizer inside the tilted card
                                    Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                                        val bars = 11
                                        val step = size.width / bars
                                        for (b in 0 until bars) {
                                            val h = size.height * (0.25f + 0.65f * abs(sin(b * 0.7f)))
                                            drawLine(
                                                color = Color.White.copy(alpha = 0.55f),
                                                start = Offset(b * step + 4f, size.height),
                                                end = Offset(b * step + 4f, size.height - h),
                                                strokeWidth = 3f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── CARD 1: REVERB (00:02 - 00:18) ───────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Animated Acoustic Wave Reflections Canvas (00:02 - 00:17)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A0A0D))
                            .border(1.dp, MochyCardBorder, RoundedCornerShape(20.dp))
                    ) {
                        val waveAnim = rememberInfiniteTransition(label = "reverbWave")
                        val phase by waveAnim.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
                            label = "wavePhase"
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cy = h / 2f
                            // Reflecting wall on right
                            val wallX = w * (0.58f + mochyState.reverbRoomSize * 0.24f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.65f),
                                start = Offset(wallX, h * 0.22f),
                                end = Offset(wallX, h * 0.78f),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            // Forward & reflected sound wave arcs
                            val arcCount = if (mochyState.reverbEnabled) 4 else 1
                            for (i in 0 until arcCount) {
                                val t = if (mochyState.reverbEnabled) ((phase + i * 0.25f) % 1f) else 0.55f
                                val arcX = w * 0.18f + t * (wallX - w * 0.24f)
                                val alpha = if (mochyState.reverbEnabled) (1f - t * 0.7f) * mochyState.reverbWet else 0.45f
                                drawArc(
                                    color = Color.White.copy(alpha = alpha.coerceIn(0.1f, 0.9f)),
                                    startAngle = -38f,
                                    sweepAngle = 76f,
                                    useCenter = false,
                                    topLeft = Offset(arcX - 40.dp.toPx(), cy - 42.dp.toPx()),
                                    size = Size(80.dp.toPx(), 84.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 26.dp)
                                .size(26.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    MochySwitchRow(
                        title = "Reverb",
                        subtitle = "Reverb the music",
                        checked = mochyState.reverbEnabled,
                        onCheckedChange = { mochyState.reverbEnabled = it }
                    )

                    AnimatedVisibility(visible = mochyState.reverbEnabled) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            // 8 Reverb Presets Row: Room, Studio, Bathroom, Plate, Hall, Cathedral, Stadium, Dreamy
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MochyReverbPreset.entries) { preset ->
                                    val selected = selectedReverbPreset == preset
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (selected) Color.White else MochySurfaceHigh,
                                        modifier = Modifier.clickable {
                                            selectedReverbPreset = preset
                                            mochyState.reverbPreset = preset.label
                                            mochyState.reverbMix = preset.mix
                                            mochyState.reverbRoomSize = preset.roomSize
                                            mochyState.reverbDamping = preset.damping
                                            mochyState.reverbWet = preset.wet
                                            reverbDry = preset.dry
                                            reverbWidth = preset.width
                                            reverbPreDelayMs = preset.preDelayMs
                                            reverbLowCutHz = preset.lowCutHz
                                        }
                                    ) {
                                        Text(
                                            text = preset.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.Black else Color.White,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            MochyLabeledSlider(
                                label = "Mix",
                                valueText = "${(mochyState.reverbMix * 100).roundToInt()}%",
                                value = mochyState.reverbMix,
                                onValueChange = { mochyState.reverbMix = it }
                            )
                            MochyLabeledSlider(
                                label = "Room size",
                                valueText = "${(mochyState.reverbRoomSize * 100).roundToInt()}%",
                                value = mochyState.reverbRoomSize,
                                onValueChange = { mochyState.reverbRoomSize = it }
                            )
                            MochyLabeledSlider(
                                label = "Damping",
                                valueText = "${(mochyState.reverbDamping * 100).roundToInt()}%",
                                value = mochyState.reverbDamping,
                                onValueChange = { mochyState.reverbDamping = it }
                            )
                            MochyLabeledSlider(
                                label = "Wet",
                                valueText = "${(mochyState.reverbWet * 100).roundToInt()}%",
                                value = mochyState.reverbWet,
                                onValueChange = { mochyState.reverbWet = it }
                            )

                            Spacer(Modifier.height(8.dp))
                            Text("FINE TUNE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                            Spacer(Modifier.height(8.dp))

                            MochyLabeledSlider(
                                label = "Dry",
                                valueText = "${(reverbDry * 100).roundToInt()}%",
                                value = reverbDry,
                                onValueChange = { reverbDry = it }
                            )
                            MochyLabeledSlider(
                                label = "Width",
                                valueText = "${(reverbWidth * 100).roundToInt()}%",
                                value = reverbWidth,
                                onValueChange = { reverbWidth = it }
                            )
                            MochyLabeledSlider(
                                label = "Pre-delay",
                                valueText = "${reverbPreDelayMs.roundToInt()} ms",
                                value = reverbPreDelayMs,
                                valueRange = 0f..100f,
                                onValueChange = { reverbPreDelayMs = it }
                            )
                            MochyLabeledSlider(
                                label = "Low cut",
                                valueText = "${reverbLowCutHz.roundToInt()} Hz",
                                value = reverbLowCutHz,
                                valueRange = 20f..500f,
                                onValueChange = { reverbLowCutHz = it }
                            )
                        }
                    }
                }
            }
        }

        // ── CARD 2: LOUDNESS & REPLAYGAIN (00:18 - 00:29) ────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Waveform Compression Visualizer (00:19 - 00:26)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A0A0D))
                            .border(1.dp, MochyCardBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bars = 64
                            val step = size.width / bars
                            val midY = size.height / 2f
                            val boostFactor = if (mochyState.loudnessEnabled) (1f + loudnessBoostDb / 14f) else 0.72f
                            val compFactor = if (mochyState.loudnessEnabled) loudnessCompression else 0f
                            for (i in 0 until bars) {
                                val rawEnv = if (i in 34..45) 0.32f else 0.68f + 0.25f * sin(i * 0.35f)
                                val liftedEnv = rawEnv + (1f - rawEnv) * compFactor * 0.75f
                                val h = (midY * liftedEnv * boostFactor).coerceAtMost(midY * 0.95f)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.82f),
                                    start = Offset(i * step, midY - h),
                                    end = Offset(i * step, midY + h),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    MochySwitchRow(
                        title = "Loudness",
                        subtitle = "Louder and fuller, never clipping",
                        checked = mochyState.loudnessEnabled,
                        onCheckedChange = { mochyState.loudnessEnabled = it }
                    )

                    AnimatedVisibility(visible = mochyState.loudnessEnabled) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MochyLoudnessPreset.entries.forEach { preset ->
                                    val active = selectedLoudnessPreset == preset
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (active) Color.White else MochySurfaceHigh,
                                        modifier = Modifier.clickable {
                                            selectedLoudnessPreset = preset
                                            loudnessBoostDb = preset.boostDb
                                            loudnessCompression = preset.compression
                                        }
                                    ) {
                                        Text(
                                            text = preset.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.Black else Color.White,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            MochyLabeledSlider(
                                label = "Boost",
                                valueText = "+${"%.1f".format(loudnessBoostDb)} dB",
                                value = loudnessBoostDb,
                                valueRange = 0f..12f,
                                onValueChange = { loudnessBoostDb = it }
                            )
                            MochyLabeledSlider(
                                label = "Compression",
                                valueText = "${(loudnessCompression * 100).roundToInt()}%",
                                value = loudnessCompression,
                                valueRange = 0f..1f,
                                onValueChange = { loudnessCompression = it }
                            )
                            Text(
                                text = "Compression lifts the quiet parts up toward the loud ones. Night mode keeps whispers and explosions close.",
                                fontSize = 12.sp,
                                color = MochyTextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MochyCardBorder)
                    Spacer(Modifier.height(14.dp))

                    MochySwitchRow(
                        title = "Even out songs",
                        subtitle = "Plays every song at the same loudness",
                        checked = mochyState.evenOutSongs,
                        onCheckedChange = { mochyState.evenOutSongs = it }
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("REPLAYGAIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Track", "Album").forEach { mode ->
                            val selected = mochyState.replayGainMode == mode
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) Color.White else MochySurfaceHigh,
                                modifier = Modifier.clickable { mochyState.replayGainMode = mode }
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    MochyLabeledSlider(
                        label = "Preamp",
                        valueText = "${if (mochyState.replayGainPreampDb >= 0f) "+" else ""}${"%.1f".format(mochyState.replayGainPreampDb)} dB",
                        value = mochyState.replayGainPreampDb,
                        valueRange = -12f..12f,
                        onValueChange = { mochyState.replayGainPreampDb = it }
                    )
                }
            }
        }

        // ── CARD 3: SPATIAL AUDIO (CROSSFEED & SPEAKERS - 00:29 - 00:38) ─────
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Animated Head & Binaural Soundstage Diagram (00:30 - 00:36)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A0A0D))
                            .border(1.dp, MochyCardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = if (mochyState.spatialMode == "Speakers") size.height * 0.68f else size.height * 0.56f
                            val headR = 22.dp.toPx()

                            if (mochyState.spatialMode == "Speakers") {
                                // Two Virtual Speakers in front of the listener head (00:35)
                                val spkOffset = (speakerAngleDeg / 45f) * 74.dp.toPx()
                                val leftSpk = Offset(cx - spkOffset, size.height * 0.24f)
                                val rightSpk = Offset(cx + spkOffset, size.height * 0.24f)
                                drawLine(Color(0xFFE57399).copy(alpha = 0.65f), leftSpk, Offset(cx - headR, cy), strokeWidth = 3f)
                                drawLine(Color(0xFF64B5F6).copy(alpha = 0.65f), rightSpk, Offset(cx + headR, cy), strokeWidth = 3f)
                                drawCircle(Color(0xFFE57399), radius = 6.dp.toPx(), center = leftSpk)
                                drawCircle(Color(0xFF64B5F6), radius = 6.dp.toPx(), center = rightSpk)
                            } else {
                                // Crossfeed arc in front of head (00:30)
                                drawArc(
                                    color = Color.White.copy(alpha = 0.35f),
                                    startAngle = 200f,
                                    sweepAngle = 140f,
                                    useCenter = false,
                                    topLeft = Offset(cx - headR * 1.6f, cy - headR * 1.6f),
                                    size = Size(headR * 3.2f, headR * 3.2f),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Listener Head + Ears
                            drawCircle(Color.White, radius = headR, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                            drawCircle(Color(0xFFE57399), radius = 5.dp.toPx(), center = Offset(cx - headR - 3.dp.toPx(), cy))
                            drawCircle(Color(0xFF64B5F6), radius = 5.dp.toPx(), center = Offset(cx + headR + 3.dp.toPx(), cy))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    MochySwitchRow(
                        title = "Spatial audio",
                        subtitle = "Takes stereo out of your head and puts it in front of you",
                        checked = mochyState.spatialAudioEnabled,
                        onCheckedChange = { mochyState.spatialAudioEnabled = it }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Crossfeed | Speakers Switcher
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MochySurfaceHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            listOf("Crossfeed", "Speakers").forEach { tab ->
                                val active = mochyState.spatialMode == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (active) Color.White else Color.Transparent)
                                        .clickable {
                                            mochyState.spatialMode = tab
                                            mochyState.spatialAudioEnabled = true
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (mochyState.spatialMode == "Crossfeed") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("Light", 700f, -4.5f),
                                Triple("Natural", 700f, -6.0f),
                                Triple("Strong", 650f, -9.5f)
                            ).forEach { (presetName, hz, db) ->
                                val active = mochyState.spatialPreset == presetName
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (active) Color.White else MochySurfaceHigh,
                                    modifier = Modifier.clickable {
                                        mochyState.spatialPreset = presetName
                                        mochyState.headShadowHz = hz
                                        mochyState.crossfeedDb = db
                                        mochyState.spatialAudioEnabled = true
                                    }
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        MochyLabeledSlider(
                            label = "Head shadow",
                            valueText = "${mochyState.headShadowHz.roundToInt()} Hz",
                            value = mochyState.headShadowHz,
                            valueRange = 300f..1200f,
                            onValueChange = { mochyState.headShadowHz = it }
                        )
                        MochyLabeledSlider(
                            label = "Crossfeed",
                            valueText = "${"%.1f".format(mochyState.crossfeedDb)} dB",
                            value = mochyState.crossfeedDb,
                            valueRange = -15f..-2f,
                            onValueChange = { mochyState.crossfeedDb = it }
                        )
                    } else {
                        MochyLabeledSlider(
                            label = "Speaker angle",
                            valueText = "±${speakerAngleDeg.roundToInt()}°",
                            value = speakerAngleDeg,
                            valueRange = 15f..60f,
                            onValueChange = { speakerAngleDeg = it }
                        )
                    }

                    MochyLabeledSlider(
                        label = "Amount",
                        valueText = "${(mochyState.spatialAmount * 100).roundToInt()}%",
                        value = mochyState.spatialAmount,
                        valueRange = 0f..1f,
                        onValueChange = { mochyState.spatialAmount = it }
                    )

                    if (mochyState.spatialMode == "Speakers") {
                        Text(
                            text = "Two virtual speakers in front of a modelled head; the far ear hears each one a little later and darker, as in a room. 30° is a studio pair.",
                            fontSize = 12.sp,
                            color = MochyTextSecondary
                        )
                    }
                }
            }
        }

        // ── CARD 4: MONO AUDIO (00:38 - 00:42) ───────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(18.dp)) {
                    MochySwitchRow(
                        title = "Mono audio",
                        subtitle = "Both ears hear the whole mix: for one earbud, or hearing in one ear",
                        checked = mochyState.monoAudio,
                        onCheckedChange = { mochyState.monoAudio = it }
                    )
                }
            }
        }

        // ── CARD 5: DOLBY ATMOS & SURROUND (00:39 - 00:45) ───────────────────
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // 3D Orbiting Sound Dome Canvas (00:43 - 00:45)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A0A0D))
                            .border(1.dp, MochyCardBorder, RoundedCornerShape(20.dp))
                    ) {
                        val orbitAnim = rememberInfiniteTransition(label = "atmosOrbit")
                        val angle by orbitAnim.animateFloat(
                            initialValue = 0f,
                            targetValue = (2 * Math.PI).toFloat(),
                            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
                            label = "orbitAngle"
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height * 0.62f
                            val rx = size.width * 0.36f
                            val ry = size.height * 0.22f

                            drawOval(
                                color = Color.White.copy(alpha = 0.24f),
                                topLeft = Offset(cx - rx, cy - ry),
                                size = Size(rx * 2, ry * 2),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            if (mochyState.dolbyAtmosSurround) {
                                // Upper 3D dome arc when Atmos Surround is enabled (00:44)
                                drawArc(
                                    color = Color.White.copy(alpha = 0.22f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(cx - rx * 0.85f, cy - ry * 2.8f),
                                    size = Size(rx * 1.7f, ry * 3.2f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                            drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                            val gX = cx + cos(angle) * (rx * 0.55f)
                            val gY = cy - sin(angle) * ( if (mochyState.dolbyAtmosSurround) ry * 1.5f else ry * 0.4f )
                            drawCircle(Color(0xFF4CAF50), radius = 6.dp.toPx(), center = Offset(gX, gY))
                            drawCircle(Color(0xFF64B5F6), radius = 5.dp.toPx(), center = Offset(cx + rx * 0.45f, cy + ry * 0.2f))
                            drawCircle(Color(0xFFE57399), radius = 5.dp.toPx(), center = Offset(cx - rx * 0.55f, cy - ry * 0.3f))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    MochySwitchRow(
                        title = "Dolby Atmos & surround",
                        subtitle = "Plays 5.1, 7.1 and Atmos beds as 3D sound on any headphones, with MyPlayer's head model, instead of folding them to stereo.",
                        checked = mochyState.dolbyAtmosSurround,
                        onCheckedChange = { mochyState.dolbyAtmosSurround = it }
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MochyCardBorder)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Column {
                            Text("ATMOS DECODER", fontSize = 10.sp, color = MochyTextSecondary)
                            Text(
                                text = if (mochyState.dolbyAtmosSurround) "Binaural Active" else "Standby",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text("ANDROID SPATIALIZER", fontSize = 10.sp, color = MochyTextSecondary)
                            Text(
                                text = if (mochyState.dolbyAtmosSurround) "Head Model 3D" else "Off",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ── CARD 6: PLAYBACK SPEED (NORMAL | SLOWED | SLOWED + REVERB | NIGHTCORE - 00:46 - 00:56) ──
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Mini Vinyl + Sine Wave Speed Visualizer (00:47 - 00:55)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A0A0D))
                            .border(1.dp, MochyCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val discX = 38.dp.toPx()
                            val cy = size.height / 2f
                            drawCircle(Color(0xFF232329), radius = 32.dp.toPx(), center = Offset(discX, cy))
                            drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(discX, cy))

                            val startX = 96.dp.toPx()
                            val w = size.width - startX - 12.dp.toPx()
                            val path = Path()
                            val freq = mochyState.playbackSpeed * 2.5f
                            for (px in 0..w.toInt() step 3) {
                                val t = px.toFloat() / w
                                val y = cy + sin(t * Math.PI * 2 * freq).toFloat() * 22.dp.toPx()
                                if (px == 0) path.moveTo(startX + px, y) else path.lineTo(startX + px, y)
                            }
                            drawPath(path, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Playback speed", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = if (mochyState.preservePitch) "Pitch stays where it is" else "Pitch follows the speed, like a record",
                                fontSize = 12.sp,
                                color = MochyTextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MochySurfaceHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        mochyState.playbackSpeed = (mochyState.playbackSpeed - 0.05f).coerceIn(0.50f, 2.00f)
                                    }
                                )
                                Text(
                                    text = "${"%.2f".format(mochyState.playbackSpeed)}×",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "+",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        mochyState.playbackSpeed = (mochyState.playbackSpeed + 0.05f).coerceIn(0.50f, 2.00f)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Speed Presets Row: Normal, Slowed, Slowed + reverb, Nightcore, 0.75x, 1.25x, 1.5x, 2x
                    val speedPresets = listOf("Normal", "Slowed", "Slowed + reverb", "Nightcore", "0.75×", "1.25×", "1.5×", "2×")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(speedPresets) { mode ->
                            val selected = mochyState.playbackSpeedMode == mode
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) Color.White else MochySurfaceHigh,
                                modifier = Modifier.clickable {
                                    mochyState.playbackSpeedMode = mode
                                    when (mode) {
                                        "Normal" -> {
                                            mochyState.playbackSpeed = 1.00f
                                            mochyState.preservePitch = true
                                            mochyState.pitchSemitones = 0f
                                        }
                                        "Slowed" -> {
                                            mochyState.playbackSpeed = 0.85f
                                            mochyState.preservePitch = false
                                        }
                                        "Slowed + reverb" -> {
                                            mochyState.playbackSpeed = 0.85f
                                            mochyState.preservePitch = false
                                            // Automatically enable Hall Reverb (exact match to 00:52 & 01:00!)
                                            mochyState.reverbEnabled = true
                                            selectedReverbPreset = MochyReverbPreset.HALL
                                            mochyState.reverbPreset = "Hall"
                                            mochyState.reverbMix = 0.50f
                                            mochyState.reverbRoomSize = 0.78f
                                            mochyState.reverbDamping = 0.35f
                                            mochyState.reverbWet = 0.60f
                                            reverbPreDelayMs = 25f
                                            reverbLowCutHz = 150f
                                        }
                                        "Nightcore" -> {
                                            mochyState.playbackSpeed = 1.25f
                                            mochyState.preservePitch = false
                                        }
                                        "0.75×" -> { mochyState.playbackSpeed = 0.75f; mochyState.preservePitch = true }
                                        "1.25×" -> { mochyState.playbackSpeed = 1.25f; mochyState.preservePitch = true }
                                        "1.5×" -> { mochyState.playbackSpeed = 1.50f; mochyState.preservePitch = true }
                                        "2×" -> { mochyState.playbackSpeed = 2.00f; mochyState.preservePitch = true }
                                    }
                                }
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    MochyLabeledSlider(
                        label = "Speed",
                        valueText = "${"%.2f".format(mochyState.playbackSpeed)}×",
                        value = mochyState.playbackSpeed,
                        valueRange = 0.50f..2.00f,
                        onValueChange = { mochyState.playbackSpeed = it }
                    )

                    MochySwitchRow(
                        title = "Preserve pitch",
                        subtitle = "Keep the original key when changing speed",
                        checked = mochyState.preservePitch,
                        onCheckedChange = { mochyState.preservePitch = it }
                    )

                    AnimatedVisibility(visible = mochyState.preservePitch) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            MochyLabeledSlider(
                                label = "Pitch",
                                valueText = "${mochyState.pitchSemitones.roundToInt()} semitones",
                                value = mochyState.pitchSemitones,
                                valueRange = -12f..12f,
                                onValueChange = { mochyState.pitchSemitones = it }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "↻ Reset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MochyTextSecondary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                mochyState.playbackSpeedMode = "Normal"
                                mochyState.playbackSpeed = 1.00f
                                mochyState.preservePitch = true
                                mochyState.pitchSemitones = 0f
                            }
                    )
                }
            }
        }

        // ── CARD 7: EXPONENTIAL VOLUME (00:56 - 00:59) ───────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A0A0D))
                            .border(1.dp, MochyCardBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bars = 12
                            val step = size.width / bars
                            for (i in 0 until bars) {
                                val t = (i + 1).toFloat() / bars
                                val h = size.height * (if (mochyState.exponentialVolume) t.pow(2.1f) else t)
                                drawRect(
                                    color = Color.White.copy(alpha = 0.14f),
                                    topLeft = Offset(i * step + 4f, size.height - h),
                                    size = Size(step - 8f, h)
                                )
                            }
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(size.width, 0f))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    MochySwitchRow(
                        title = "Exponential volume",
                        subtitle = "A logarithmic curve, for finer control at low volume",
                        checked = mochyState.exponentialVolume,
                        onCheckedChange = { mochyState.exponentialVolume = it }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun MochyLabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(valueText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}



@Composable
private fun MochySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = MochyTextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}