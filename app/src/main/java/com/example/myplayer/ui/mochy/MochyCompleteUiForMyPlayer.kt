package com.example.myplayer.ui.mochy

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import com.example.myplayer.ui.screens.nowplaying.LyricsTab
import com.example.myplayer.ui.screens.nowplaying.LyricsViewModel
import com.example.myplayer.ui.screens.nowplaying.NowPlayingVisual
import com.example.myplayer.ui.theme.NeonLimePrimary
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    IPOD("iPod Classic", "Retro click wheel with tactile click feel & classic LCD display"),
    POSTER("Poster", "Editorial full-bleed poster with curved wave seek arc"),
    TIMELINE("Timeline", "Minimalist split-screen giant timer card"),
    LYRICS_STAGE("Lyrics stage", "Immersive synced lyrics stage with split timer")
}

enum class CoverStyleMode(val title: String, val desc: String) {
    RADIAL_3D("Radial 3D Dome", "Spherical dome with dynamic 3D tilt"),
    ROUNDED_SQUARE("Rounded Card", "Modern floating album art card"),
    CIRCLE_DISC("Floating Disc", "Circular disc with depth drop shadow"),
    VINYL_LP("Vinyl LP Record", "Grooved vinyl with realistic shine & center label"),
    CASSETTE("Retro Cassette", "Vintage tape cassette with spinning reels"),
    CD_JEWEL("Jewel CD Case", "Clear crystal case with light refraction"),
    POSTER_BLEED("Full Poster", "Edge-to-edge cinematic bleed")
}

enum class ControlButtonStyleMode(val title: String, val desc: String) {
    DEFAULT("Modern Minimal", "Clean high-contrast transport controls"),
    PILL_DOCK("Pill Floating Dock", "Unified pill background for transport buttons"),
    NEUMORPHIC("Neumorphic 3D", "Soft tactile embossed physical buttons"),
    NEON_GLOW("Neon Ring Glow", "Signature Cyan & Lime luminous glow rings"),
    MINIMAL_LIGHT("Ultra Wireframe", "Featherlight high-contrast outline glyphs")
}

enum class SeekbarStyleMode(val title: String, val desc: String) {
    DEFAULT("Linear Bar", "Smooth gradient track with rounded thumb"),
    WAVY("Animated Sine Wave", "Rippling sine wave in sync with music playback"),
    SQUIGGLY("Squiggly Wave", "Playful expressive audio waveform"),
    DISC_ARM("Tonearm Needle", "Realistic turntable needle tracking the record"),
    CIRCULAR_ARC("Radial Arc Dial", "270° neon curved progress dial")
}

enum class BottomBarStyleMode(val title: String, val desc: String) {
    CARD_STACK("Card Deck Stack", "Stacked cards: Up Next queue preview + Lyrics pill"),
    VOLUME_BAR("Volume Slider Dock", "Integrated volume control slider with icon"),
    MINIMAL_BAR("Minimalist Bar", "Subtle compact navigation handle pill")
}

enum class TimestampStyleMode(val title: String, val desc: String) {
    DURATION("Song Duration", "Total length format (e.g. 03:45)"),
    REMAINING("Remaining Countdown", "Time left format (e.g. -01:20)"),
    CLOCK_TIME("Wall Clock End Time", "Calculated end time (e.g. Ends at 5:42 PM)")
}

enum class BackgroundAtmosphereMode(val title: String, val desc: String) {
    ALBUM_COLOR_GRADIENT("Artwork Gradient", "Dynamic extracted vibrant color palette"),
    FROSTED_GLASS_BLUR("Frosted Glass Blur", "Deep 25px gaussian glass blur"),
    OLED_BLACK("OLED Pitch Black", "True black #000000 for contrast & battery saving"),
    RADIAL_GLOW("Luminous Radial Bloom", "Centered soft ambient bloom light"),
    NEON_CYAN_LIME("Cyberpunk Neon Glow", "Signature MyPlayer Cyan & Neon Lime aura")
}

enum class IpodThemeMode(val title: String, val desc: String) {
    SILVER("Classic Silver", "Original white faceplate with polished steel"),
    BLACK("Stealth Black", "Deep space grey fading to matte black"),
    U2_EDITION("U2 Special Edition", "Jet black body with bold red click wheel"),
    TRUE_DARK("True Dark", "Monochrome midnight edition")
}

enum class TurntableArmColorMode(val title: String) {
    CHROME_SILVER("Chrome Silver"),
    BRASS_GOLD("Brass Gold"),
    MATTE_BLACK("Matte Black")
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

    // Mochy Player Customizer Options (Full studio controls)
    var coverStyle by mutableStateOf(
        runCatching { CoverStyleMode.valueOf(prefs.getString("cover_style", CoverStyleMode.RADIAL_3D.name)!!) }.getOrDefault(CoverStyleMode.RADIAL_3D)
    )
    var controlButtonStyle by mutableStateOf(
        runCatching { ControlButtonStyleMode.valueOf(prefs.getString("control_button_style", ControlButtonStyleMode.DEFAULT.name)!!) }.getOrDefault(ControlButtonStyleMode.DEFAULT)
    )
    var seekbarStyle by mutableStateOf(
        runCatching { SeekbarStyleMode.valueOf(prefs.getString("seekbar_style", SeekbarStyleMode.DEFAULT.name)!!) }.getOrDefault(SeekbarStyleMode.DEFAULT)
    )
    var bottomBarStyle by mutableStateOf(
        runCatching { BottomBarStyleMode.valueOf(prefs.getString("bottom_bar_style", BottomBarStyleMode.CARD_STACK.name)!!) }.getOrDefault(BottomBarStyleMode.CARD_STACK)
    )
    var timestampStyle by mutableStateOf(
        runCatching { TimestampStyleMode.valueOf(prefs.getString("timestamp_style", TimestampStyleMode.DURATION.name)!!) }.getOrDefault(TimestampStyleMode.DURATION)
    )
    var backgroundAtmosphere by mutableStateOf(
        runCatching { BackgroundAtmosphereMode.valueOf(prefs.getString("background_atmosphere", BackgroundAtmosphereMode.ALBUM_COLOR_GRADIENT.name)!!) }.getOrDefault(BackgroundAtmosphereMode.ALBUM_COLOR_GRADIENT)
    )
    var ipodTheme by mutableStateOf(
        runCatching { IpodThemeMode.valueOf(prefs.getString("ipod_theme", IpodThemeMode.SILVER.name)!!) }.getOrDefault(IpodThemeMode.SILVER)
    )
    var turntableArmColor by mutableStateOf(
        runCatching { TurntableArmColorMode.valueOf(prefs.getString("turntable_arm_color", TurntableArmColorMode.CHROME_SILVER.name)!!) }.getOrDefault(TurntableArmColorMode.CHROME_SILVER)
    )
    var turntableRpm by mutableIntStateOf(prefs.getInt("turntable_rpm", 33))

    fun updateCoverStyle(style: CoverStyleMode) {
        coverStyle = style
        prefs.edit().putString("cover_style", style.name).apply()
    }
    fun updateControlButtonStyle(style: ControlButtonStyleMode) {
        controlButtonStyle = style
        prefs.edit().putString("control_button_style", style.name).apply()
    }
    fun updateSeekbarStyle(style: SeekbarStyleMode) {
        seekbarStyle = style
        prefs.edit().putString("seekbar_style", style.name).apply()
    }
    fun updateBottomBarStyle(style: BottomBarStyleMode) {
        bottomBarStyle = style
        prefs.edit().putString("bottom_bar_style", style.name).apply()
    }
    fun updateTimestampStyle(style: TimestampStyleMode) {
        timestampStyle = style
        prefs.edit().putString("timestamp_style", style.name).apply()
    }
    fun updateBackgroundAtmosphere(style: BackgroundAtmosphereMode) {
        backgroundAtmosphere = style
        prefs.edit().putString("background_atmosphere", style.name).apply()
    }
    fun updateIpodTheme(theme: IpodThemeMode) {
        ipodTheme = theme
        prefs.edit().putString("ipod_theme", theme.name).apply()
    }
    fun updateTurntableArmColor(arm: TurntableArmColorMode) {
        turntableArmColor = arm
        prefs.edit().putString("turntable_arm_color", arm.name).apply()
    }
    fun updateTurntableRpm(rpm: Int) {
        turntableRpm = rpm
        prefs.edit().putInt("turntable_rpm", rpm).apply()
    }

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
        prefs.edit().putString("player_style", style.name).apply()
    }
}

@Composable
fun rememberMochyUiStateHolder(): MochyUiStateHolder {
    val context = LocalContext.current
    return remember { MochyUiStateHolder(context) }
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
// 4. MULTI-STYLE NOW PLAYING SCREEN & CUSTOMIZE PLAYER CAROUSEL (01:10, 01:38-01:59)
//    Includes:
//    1) RADIAL (MYPLAYER SIGNATURE)
//    2) DEFAULT
//    3) TURNTABLE (Spinning Vinyl + Tonearm)
//    4) POSTER (Curved Wave Seek Arc)
//    5) TIMELINE (Split Timer)
//    6) LYRICS STAGE (Split Synced Lyrics + Timer)
// ═════════════════════════════════════════════════════════════════════════════

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
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    isShuffleOn: Boolean,
    onToggleShuffle: () -> Unit,
    repeatMode: Int,
    onCycleRepeatMode: () -> Unit,
    canDownload: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    audioQuality: AudioQualityInfo,
    onOpenCustomizeStyles: () -> Unit,
    lyricsViewModel: LyricsViewModel? = null
) {
    val activeStyle = mochyState.playerStyle
    val positionMs = positionState.value
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    when (activeStyle) {
        // ── STYLE 1: YOUR SIGNATURE MYPLAYER RADIAL CONTROLLER SCREEN ──────
        PlayerStyleMode.RADIAL_MYPLAYER -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E140B))
            ) {
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
                    onOptions = onOpenCustomizeStyles,
                    lyricsContent = {
                        lyricsViewModel?.let { lvm ->
                            val lyricsState by lvm.lyricsState.collectAsStateWithLifecycle()
                            val lyricsPositionState = lvm.currentPosition.collectAsStateWithLifecycle()
                            LyricsTab(
                                lyricsState = lyricsState,
                                positionMs = lyricsPositionState,
                                onSeek = onSeek,
                                onAddCustomLyrics = {},
                                onRetryLyrics = { lvm.retryLyrics() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                            )
                        }
                    }
                )
            }
        }

        // ── STYLE 2: MOCHY DEFAULT / FLAGSHIP CUSTOMIZABLE STYLE ─────────────
        PlayerStyleMode.DEFAULT -> {
            val bgModifier = when (mochyState.backgroundAtmosphere) {
                BackgroundAtmosphereMode.OLED_BLACK -> Modifier.background(Color.Black)
                BackgroundAtmosphereMode.FROSTED_GLASS_BLUR -> Modifier.background(Color(0xFF0F151F))
                BackgroundAtmosphereMode.RADIAL_GLOW -> Modifier.background(
                    Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF0A0F16), Color.Black))
                )
                BackgroundAtmosphereMode.NEON_CYAN_LIME -> Modifier.background(
                    Brush.verticalGradient(listOf(Color(0xFF001A24), Color(0xFF0D1E13), Color(0xFF060D08)))
                )
                BackgroundAtmosphereMode.ALBUM_COLOR_GRADIENT -> Modifier.background(
                    Brush.verticalGradient(listOf(Color(0xFF0E1A29), Color(0xFF09121E), Color(0xFF05080C)))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(bgModifier)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Grab Handle & Back / Customize Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    IconButton(
                        onClick = onOpenCustomizeStyles,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Default.Palette, null, tint = NeonLimePrimary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Cover Art (respects mochyState.coverStyle)
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        MochyCoverArtView(
                            coverStyle = mochyState.coverStyle,
                            artUri = artUri,
                            title = title,
                            artist = artist,
                            isPlaying = isPlaying
                        )
                    }

                    // 2. Metadata (Title, Artist, Format Badge)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title ?: "No song playing",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (mochyState.audioFormatBadge) {
                                        Spacer(Modifier.width(8.dp))
                                        MochyFormatBadge(audioQuality.format.ifBlank { "HI-RES" })
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = artist ?: "Unknown Artist",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFFF4081) else Color.White
                                )
                            }
                        }
                    }

                    // 3. Seekbar & Progress (respects mochyState.seekbarStyle & mochyState.timestampStyle)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when (mochyState.seekbarStyle) {
                            SeekbarStyleMode.WAVY -> {
                                MochyWavyProgressBar(
                                    progress = progress,
                                    onSeek = { onSeek((it * safeDuration).toLong()) },
                                    isPlaying = isPlaying
                                )
                            }
                            SeekbarStyleMode.SQUIGGLY -> {
                                MochySquigglyProgressBar(
                                    progress = progress,
                                    onSeek = { onSeek((it * safeDuration).toLong()) },
                                    isPlaying = isPlaying
                                )
                            }
                            else -> {
                                Slider(
                                    value = progress,
                                    onValueChange = { onSeek((it * safeDuration).toLong()) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonLimePrimary,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val elapsedText = formatMs(positionMs)
                            val endText = when (mochyState.timestampStyle) {
                                TimestampStyleMode.DURATION -> formatMs(safeDuration)
                                TimestampStyleMode.REMAINING -> "-${formatMs((safeDuration - positionMs).coerceAtLeast(0L))}"
                                TimestampStyleMode.CLOCK_TIME -> "Ends ${formatEndTime((safeDuration - positionMs).coerceAtLeast(0L))}"
                            }
                            Text(elapsedText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                            Text(endText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 4. Transport Controls (respects mochyState.controlButtonStyle)
                    MochyTransportButtonsRow(
                        buttonStyle = mochyState.controlButtonStyle,
                        isPlaying = isPlaying,
                        onPrevious = onPreviousClick,
                        onPlayPause = onPlayPauseClick,
                        onNext = onNextClick
                    )

                    Spacer(Modifier.height(12.dp))

                    // 5. Bottom Dock (respects mochyState.bottomBarStyle)
                    when (mochyState.bottomBarStyle) {
                        BottomBarStyleMode.CARD_STACK -> {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clickable { onOpenCustomizeStyles() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = NeonLimePrimary, modifier = Modifier.size(18.dp))
                                        Text("Next in Queue • Synced Lyrics", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                        BottomBarStyleMode.VOLUME_BAR -> {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.VolumeDown, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Slider(
                                        value = 0.7f,
                                        onValueChange = {},
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )
                                    Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        BottomBarStyleMode.MINIMAL_BAR -> {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(44.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                                    .clickable { onOpenCustomizeStyles() }
                            )
                        }
                    }
                }
            }
        }

        // ── STYLE 3: MOCHY TURNTABLE STYLE (01:40 - 01:42, 01:51 - 01:59) ───
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
                // Rotating Vinyl LP Disc + Artwork Label + Tonearm (Top-Left/Center, 01:53)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(470.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width * 0.28f
                        val cy = size.height * 0.46f
                        val r = size.width * 0.64f

                        // Outer Vinyl Disc
                        drawCircle(color = Color(0xFF0B0B0D), radius = r, center = Offset(cx, cy))
                        for (ring in 1..9) {
                            drawCircle(
                                color = Color.White.copy(alpha = if (ring % 2 == 0) 0.06f else 0.03f),
                                radius = r * (0.48f + ring * 0.05f),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // Rotating Center Album Artwork inside the Vinyl
                    Box(
                        modifier = Modifier
                            .offset(x = (-42).dp, y = 76.dp)
                            .size(260.dp)
                            .rotate(effectiveAngle)
                            .clip(CircleShape)
                            .border(4.dp, Color(0xFF1A1A1F), CircleShape)
                    ) {
                        AlbumArtImage(
                            uri = artUri ?: "",
                            title = title,
                            artist = artist,
                            size = 260.dp,
                            shape = CircleShape,
                            iconSize = 64.dp
                        )
                    }

                    // Tonearm on the Right (01:53)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pivotX = size.width * 0.87f
                        val pivotY = size.height * 0.13f
                        drawRoundRect(
                            color = Color(0xFF1C2026),
                            topLeft = Offset(pivotX - 30.dp.toPx(), pivotY - 26.dp.toPx()),
                            size = Size(60.dp.toPx(), 68.dp.toPx()),
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                        )
                        // Metallic silver arm rod
                        val endX = size.width * 0.65f
                        val endY = size.height * 0.72f
                        drawLine(
                            color = Color(0xFFD0D5DD),
                            start = Offset(pivotX, pivotY + 20.dp.toPx()),
                            end = Offset(endX, endY),
                            strokeWidth = 7.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Headshell cartridge
                        rotate(24f, pivot = Offset(endX, endY)) {
                            drawRoundRect(
                                color = Color(0xFF15181E),
                                topLeft = Offset(endX - 16.dp.toPx(), endY - 10.dp.toPx()),
                                size = Size(32.dp.toPx(), 54.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }

                // Vertical Floating Control Column on Bottom-Right (Heart, Prev, Pause, Next - 01:53)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 112.dp),
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

                // Bottom-Left Track Metadata + Bottom Lyrics Bar (01:53)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MochyCircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            onClick = onOpenCustomizeStyles
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title ?: "Khat",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(8.dp))
                                MochyFormatBadge(audioQuality.format.ifBlank { "AAC" })
                            }
                            Text(
                                text = artist ?: "Navjot Ahuja",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .clickable { onOpenCustomizeStyles() }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "No lyrics for this song",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }

        // ── STYLE 4: MOCHY POSTER STYLE (01:43 - 01:45) ─────────────────────
        PlayerStyleMode.POSTER -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06131C))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Poster Image + Curved Wave Progress Arc
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.68f)
                ) {
                    AlbumArtImage(
                        uri = artUri ?: "",
                        title = title,
                        artist = artist,
                        size = 520.dp,
                        shape = RoundedCornerShape(0.dp),
                        iconSize = 96.dp
                    )
                    // Curved Wave Progress Line at bottom of poster (01:44)
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

                // Top-Right Collapse Button
                MochyCircleIconButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
                )

                // Large Poster Title on Left (01:44)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 36.dp)
                ) {
                    Text(
                        text = title ?: "Khat",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = artist ?: "Navjot Ahuja",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f)
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
                // Top Half (Lighter Steel Blue #546A7B)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.46f)
                        .background(Color(0xFF536878))
                        .padding(24.dp)
                ) {
                    if (isLyricsStage) {
                        Column(modifier = Modifier.align(Alignment.TopStart)) {
                            Text(
                                text = title ?: "Khat",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "No synced lyrics for this song.",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.align(Alignment.TopStart)
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
                                Text(title ?: "Khat", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(artist ?: "Navjot Ahuja", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                    }

                    MochyCircleIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    // Huge Current Time right above split line (01:46)
                    Text(
                        text = formatMs(positionMs),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }

                // Bottom Half (Darker Slate #34434E)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.54f)
                        .background(Color(0xFF34434E))
                        .padding(24.dp)
                ) {
                    Text(
                        text = formatMs(safeDuration),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.TopStart)
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

        // ── STYLE 7: MOCHY IPOD CLASSIC RETRO STYLE ────────────────────────
        PlayerStyleMode.IPOD -> {
            MochyIpodPlayerScreen(
                mochyState = mochyState,
                title = title ?: "No song playing",
                artist = artist ?: "Unknown Artist",
                artUri = artUri,
                positionMs = positionMs,
                durationMs = safeDuration,
                isPlaying = isPlaying,
                onPlayPause = onPlayPauseClick,
                onPrevious = onPreviousClick,
                onNext = onNextClick,
                onSeek = onSeek,
                onBack = onBackClick,
                onOpenCustomize = onOpenCustomizeStyles
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. MOCHY PLAYER STUDIO & COMPLETE CUSTOMIZER (01:39 - 02:30 in video)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MochyWavyProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavyProgress")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val activeWidth = w * progress.coerceIn(0f, 1f)

        // Inactive track (smooth line)
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(activeWidth, centerY),
            end = Offset(w, centerY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Active track (animated sine wave)
        val path = Path()
        val step = 4f
        val wavelength = 36f
        val amp = if (isPlaying) 5.dp.toPx() else 1.5.dp.toPx()
        val activePhase = if (isPlaying) phase else 0f

        var x = 0f
        path.moveTo(0f, centerY + kotlin.math.sin(activePhase) * amp)
        while (x <= activeWidth) {
            val y = centerY + kotlin.math.sin((x / wavelength) * (2 * Math.PI.toFloat()) + activePhase) * amp
            path.lineTo(x, y.toFloat())
            x += step
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Thumb glow & circle at activeWidth
        val thumbY = centerY + kotlin.math.sin((activeWidth / wavelength) * (2 * Math.PI.toFloat()) + activePhase) * amp
        drawCircle(
            color = NeonLimePrimary.copy(alpha = 0.35f),
            radius = 10.dp.toPx(),
            center = Offset(activeWidth, thumbY.toFloat())
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(activeWidth, thumbY.toFloat())
        )
    }
}

@Composable
fun MochySquigglyProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val activeWidth = w * progress.coerceIn(0f, 1f)

        // Inactive line
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(activeWidth, centerY),
            end = Offset(w, centerY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Squiggly active path
        val path = Path()
        val step = 10f
        var toggle = 1f
        var x = 0f
        path.moveTo(0f, centerY)
        while (x <= activeWidth) {
            val y = centerY + (if (isPlaying) toggle * 5.dp.toPx() else 0f)
            path.lineTo(x, y)
            toggle = -toggle
            x += step
        }
        drawPath(
            path = path,
            color = NeonLimePrimary,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(activeWidth, centerY)
        )
    }
}

@Composable
fun MochyCoverArtView(
    coverStyle: CoverStyleMode,
    artUri: String?,
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    when (coverStyle) {
        CoverStyleMode.RADIAL_3D -> {
            Box(
                modifier = modifier
                    .size(270.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF283648), Color(0xFF141C26), Color(0xFF090E14)),
                            radius = 450f
                        )
                    )
                    .border(BorderStroke(2.dp, Color(0xFF88B8E6).copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    uri = artUri ?: "",
                    title = title,
                    artist = artist,
                    size = 200.dp,
                    shape = CircleShape,
                    iconSize = 64.dp
                )
            }
        }
        CoverStyleMode.ROUNDED_SQUARE -> {
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = modifier.size(270.dp)
            ) {
                AlbumArtImage(
                    uri = artUri ?: "",
                    title = title,
                    artist = artist,
                    size = 270.dp,
                    shape = RoundedCornerShape(26.dp),
                    iconSize = 80.dp
                )
            }
        }
        CoverStyleMode.CIRCLE_DISC -> {
            Box(
                modifier = modifier
                    .size(270.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212))
                    .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.2f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    for (i in 1..5) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = r * (0.4f + i * 0.1f),
                            style = Stroke(1.dp.toPx())
                        )
                    }
                }
                AlbumArtImage(
                    uri = artUri ?: "",
                    title = title,
                    artist = artist,
                    size = 140.dp,
                    shape = CircleShape,
                    iconSize = 48.dp
                )
            }
        }
        CoverStyleMode.VINYL_LP -> {
            val infinite = rememberInfiniteTransition(label = "vinylSpin")
            val angle by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
                label = "vinylAngle"
            )
            val effectiveAngle = if (isPlaying) angle else 0f
            Box(
                modifier = modifier
                    .size(270.dp)
                    .rotate(effectiveAngle)
                    .clip(CircleShape)
                    .background(Color(0xFF141416)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    for (i in 1..8) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = r * (0.35f + i * 0.07f),
                            style = Stroke(1.2.dp.toPx())
                        )
                    }
                }
                AlbumArtImage(
                    uri = artUri ?: "",
                    title = title,
                    artist = artist,
                    size = 115.dp,
                    shape = CircleShape,
                    iconSize = 40.dp
                )
            }
        }
        CoverStyleMode.CASSETTE -> {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF22262C),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)),
                modifier = modifier.size(width = 280.dp, height = 190.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("STEREO CASSETTE • TYPE I", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121418))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2A303A)).border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape))
                        AlbumArtImage(
                            uri = artUri ?: "",
                            title = title,
                            artist = artist,
                            size = 64.dp,
                            shape = RoundedCornerShape(8.dp),
                            iconSize = 26.dp
                        )
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2A303A)).border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape))
                    }
                    Text(title ?: "Audio Cassette", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
        CoverStyleMode.CD_JEWEL -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF181D24),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                modifier = modifier.size(260.dp)
            ) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(16.dp).fillMaxHeight().background(Color(0xFF0E1116)).border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))))
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        AlbumArtImage(
                            uri = artUri ?: "",
                            title = title,
                            artist = artist,
                            size = 230.dp,
                            shape = RoundedCornerShape(0.dp),
                            iconSize = 68.dp
                        )
                    }
                }
            }
        }
        CoverStyleMode.POSTER_BLEED -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = modifier.fillMaxWidth(0.92f).height(340.dp)
            ) {
                AlbumArtImage(
                    uri = artUri ?: "",
                    title = title,
                    artist = artist,
                    size = 500.dp,
                    shape = RoundedCornerShape(16.dp),
                    iconSize = 96.dp
                )
            }
        }
    }
}

@Composable
fun MochyTransportButtonsRow(
    buttonStyle: ControlButtonStyleMode,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (buttonStyle) {
        ControlButtonStyleMode.DEFAULT -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.FastRewind, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(68.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }
        }
        ControlButtonStyleMode.PILL_DOCK -> {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth().height(66.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White)) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        ControlButtonStyleMode.NEUMORPHIC -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262E3B))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(onClick = onPrevious),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FastRewind, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E3848))
                        .border(2.dp, NeonLimePrimary.copy(alpha = 0.65f), CircleShape)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = NeonLimePrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262E3B))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
        ControlButtonStyleMode.NEON_GLOW -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                        .clickable(onClick = onPrevious),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FastRewind, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(26.dp))
                }
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(2.dp, NeonLimePrimary, CircleShape)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = NeonLimePrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FastForward, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(26.dp))
                }
            }
        }
        ControlButtonStyleMode.MINIMAL_LIGHT -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(60.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
fun MochyIpodPlayerScreen(
    mochyState: MochyUiStateHolder,
    title: String,
    artist: String,
    artUri: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenCustomize: () -> Unit
) {
    val theme = mochyState.ipodTheme
    val bodyBrush = when (theme) {
        IpodThemeMode.SILVER -> Brush.linearGradient(
            listOf(Color(0xFFE8ECEF), Color(0xFFCBD0D6), Color(0xFFD6DBE0))
        )
        IpodThemeMode.BLACK -> Brush.linearGradient(
            listOf(Color(0xFF282C34), Color(0xFF1E2127), Color(0xFF16181D))
        )
        IpodThemeMode.U2_EDITION -> Brush.linearGradient(
            listOf(Color(0xFF14171A), Color(0xFF0C0E10), Color(0xFF101215))
        )
        IpodThemeMode.TRUE_DARK -> Brush.linearGradient(
            listOf(Color(0xFF0A0C0E), Color(0xFF050607), Color(0xFF08090A))
        )
    }

    val wheelBg = when (theme) {
        IpodThemeMode.SILVER -> Color(0xFFF0F3F6)
        IpodThemeMode.BLACK -> Color(0xFF2B303A)
        IpodThemeMode.U2_EDITION -> Color(0xFFD32F2F)
        IpodThemeMode.TRUE_DARK -> Color(0xFF1A1D22)
    }
    val wheelText = if (theme == IpodThemeMode.SILVER) Color(0xFF4A5568) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06080B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            shadowElevation = 18.dp,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bodyBrush)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // LCD Screen
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEDF2F7),
                        border = BorderStroke(2.dp, Color(0xFF8A95A5)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.48f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = Color(0xFF2D3748),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Now Playing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("1 of 1", fontSize = 10.sp, color = Color(0xFF718096))
                                    Icon(Icons.Default.BatteryFull, null, tint = Color(0xFF2D3748), modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.size(105.dp)
                                ) {
                                    AlbumArtImage(
                                        uri = artUri ?: "",
                                        title = title,
                                        artist = artist,
                                        size = 105.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        iconSize = 36.dp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A202C),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = artist,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4A5568),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "MyPlayer",
                                        fontSize = 11.sp,
                                        color = Color(0xFF718096)
                                    )
                                }
                            }

                            Column {
                                val progressFrac = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                                LinearProgressIndicator(
                                    progress = { progressFrac },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF3182CE),
                                    trackColor = Color(0xFFCBD5E0)
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(formatMs(positionMs), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A5568))
                                    val rem = (durationMs - positionMs).coerceAtLeast(0L)
                                    Text("-${formatMs(rem)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A5568))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Click Wheel
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(wheelBg)
                            .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f)), CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.x > 5 || dragAmount.y > 5) {
                                        val newPos = (positionMs + 3000L).coerceAtMost(durationMs)
                                        onSeek(newPos)
                                    } else if (dragAmount.x < -5 || dragAmount.y < -5) {
                                        val newPos = (positionMs - 3000L).coerceAtLeast(0L)
                                        onSeek(newPos)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MENU",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = wheelText,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .clickable { onOpenCustomize() }
                        )

                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Previous",
                            tint = wheelText,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 18.dp)
                                .size(28.dp)
                                .clickable { onPrevious() }
                        )

                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Next",
                            tint = wheelText,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 18.dp)
                                .size(28.dp)
                                .clickable { onNext() }
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp)
                                .clickable { onPlayPause() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = wheelText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(bodyBrush)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), CircleShape)
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/**
 * Mochy Player Studio & Complete Customizer Screen
 * Lets user customize ALL player dimensions from Mochy + keeps MyPlayer Radial screen!
 */
@Composable
fun MochyPlayerStylesPickerScreen(
    mochyState: MochyUiStateHolder,
    previewTitle: String = "Khat",
    previewArtist: String = "Navjot Ahuja",
    previewArtUri: String? = null,
    onBack: () -> Unit
) {
    val categories = remember {
        listOf(
            "Styles" to Icons.Default.Palette,
            "Cover Style" to Icons.Default.Album,
            "Controls" to Icons.Default.PlayCircle,
            "Seekbar" to Icons.Default.GraphicEq,
            "Bottom Dock" to Icons.Default.ViewAgenda,
            "Timestamp" to Icons.Default.Schedule,
            "Atmosphere" to Icons.Default.WbSunny,
            "iPod Theme" to Icons.Default.Smartphone,
            "Turntable" to Icons.Default.Speed,
            "Visuals" to Icons.Default.AutoAwesome
        )
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val (activeCategoryName, _) = categories[selectedCategoryIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MochyBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MochySurface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Customize Player Studio", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Active Style: ${mochyState.playerStyle.title}", fontSize = 12.sp, color = NeonLimePrimary)
            }
            MochyPrimaryPillButton(
                text = "Done",
                onClick = onBack,
                modifier = Modifier.width(76.dp).height(38.dp)
            )
        }

        // Horizontal Category Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            items(categories.size) { idx ->
                val (catName, catIcon) = categories[idx]
                val isSelected = selectedCategoryIndex == idx
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) NeonLimePrimary else MochySurface,
                    border = BorderStroke(1.dp, if (isSelected) NeonLimePrimary else MochyCardBorder),
                    modifier = Modifier.clickable { selectedCategoryIndex = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(catIcon, null, tint = if (isSelected) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = catName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Live Miniature Preview Card (height 280dp)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0C1017),
            border = BorderStroke(1.5.dp, NeonLimePrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(270.dp)
        ) {
            val dummyPos = remember { mutableLongStateOf(48_000L) }
            MochyMultiStyleNowPlayingScreen(
                mochyState = mochyState,
                title = previewTitle,
                artist = previewArtist,
                artUri = previewArtUri,
                durationMs = 240_000L,
                isPlaying = true,
                positionState = dummyPos,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onBackClick = {},
                isFavorite = true,
                onToggleFavorite = {},
                isShuffleOn = false,
                onToggleShuffle = {},
                repeatMode = 0,
                onCycleRepeatMode = {},
                canDownload = false,
                isDownloaded = false,
                isDownloading = false,
                onDownloadClick = {},
                audioQuality = AudioQualityInfo(),
                onOpenCustomizeStyles = {}
            )
        }

        Spacer(Modifier.height(10.dp))

        // Options List for Active Category
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            when (activeCategoryName) {
                "Styles" -> {
                    items(PlayerStyleMode.entries.size) { i ->
                        val style = PlayerStyleMode.entries[i]
                        val isSelected = mochyState.playerStyle == style
                        MochyCustomizerOptionCard(
                            title = style.title,
                            desc = style.subtitle,
                            isSelected = isSelected,
                            badge = if (style == PlayerStyleMode.RADIAL_MYPLAYER) "MyPlayer Signature" else "Mochy Style",
                            onClick = { mochyState.updatePlayerStyle(style) }
                        )
                    }
                }
                "Cover Style" -> {
                    items(CoverStyleMode.entries.size) { i ->
                        val style = CoverStyleMode.entries[i]
                        val isSelected = mochyState.coverStyle == style
                        MochyCustomizerOptionCard(
                            title = style.title,
                            desc = style.desc,
                            isSelected = isSelected,
                            onClick = { mochyState.updateCoverStyle(style) }
                        )
                    }
                }
                "Controls" -> {
                    items(ControlButtonStyleMode.entries.size) { i ->
                        val btn = ControlButtonStyleMode.entries[i]
                        val isSelected = mochyState.controlButtonStyle == btn
                        MochyCustomizerOptionCard(
                            title = btn.title,
                            desc = btn.desc,
                            isSelected = isSelected,
                            onClick = { mochyState.updateControlButtonStyle(btn) }
                        )
                    }
                }
                "Seekbar" -> {
                    items(SeekbarStyleMode.entries.size) { i ->
                        val seek = SeekbarStyleMode.entries[i]
                        val isSelected = mochyState.seekbarStyle == seek
                        MochyCustomizerOptionCard(
                            title = seek.title,
                            desc = seek.desc,
                            isSelected = isSelected,
                            onClick = { mochyState.updateSeekbarStyle(seek) }
                        )
                    }
                }
                "Bottom Dock" -> {
                    items(BottomBarStyleMode.entries.size) { i ->
                        val bar = BottomBarStyleMode.entries[i]
                        val isSelected = mochyState.bottomBarStyle == bar
                        MochyCustomizerOptionCard(
                            title = bar.title,
                            desc = bar.desc,
                            isSelected = isSelected,
                            onClick = { mochyState.updateBottomBarStyle(bar) }
                        )
                    }
                }
                "Timestamp" -> {
                    items(TimestampStyleMode.entries.size) { i ->
                        val ts = TimestampStyleMode.entries[i]
                        val isSelected = mochyState.timestampStyle == ts
                        MochyCustomizerOptionCard(
                            title = ts.title,
                            desc = ts.desc,
                            isSelected = isSelected,
                            onClick = { mochyState.updateTimestampStyle(ts) }
                        )
                    }
                }
                "Atmosphere" -> {
                    items(BackgroundAtmosphereMode.entries.size) { i ->
                        val bg = BackgroundAtmosphereMode.entries[i]
                        val isSelected = mochyState.backgroundAtmosphere == bg
                        MochyCustomizerOptionCard(
                            title = bg.title,
                            desc = bg.desc,
                            isSelected = isSelected,
                            onClick = { mochyState.updateBackgroundAtmosphere(bg) }
                        )
                    }
                }
                "iPod Theme" -> {
                    items(IpodThemeMode.entries.size) { i ->
                        val theme = IpodThemeMode.entries[i]
                        val isSelected = mochyState.ipodTheme == theme
                        MochyCustomizerOptionCard(
                            title = theme.title,
                            desc = theme.desc,
                            isSelected = isSelected,
                            onClick = {
                                mochyState.updateIpodTheme(theme)
                                mochyState.updatePlayerStyle(PlayerStyleMode.IPOD)
                            }
                        )
                    }
                }
                "Turntable" -> {
                    item {
                        Text("Turntable Arm Finish", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                    }
                    items(TurntableArmColorMode.entries.size) { i ->
                        val arm = TurntableArmColorMode.entries[i]
                        val isSelected = mochyState.turntableArmColor == arm
                        MochyCustomizerOptionCard(
                            title = arm.title,
                            desc = "Physical metallic finish for needle tonearm",
                            isSelected = isSelected,
                            onClick = {
                                mochyState.updateTurntableArmColor(arm)
                                mochyState.updatePlayerStyle(PlayerStyleMode.TURNTABLE)
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Vinyl RPM Speed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MochyTextSecondary)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(33 to "33⅓ RPM (Standard)", 45 to "45 RPM (Faster)").forEach { (rpm, label) ->
                                val isSelected = mochyState.turntableRpm == rpm
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) NeonLimePrimary else MochySurface,
                                    modifier = Modifier.weight(1f).clickable {
                                        mochyState.updateTurntableRpm(rpm)
                                        mochyState.updatePlayerStyle(PlayerStyleMode.TURNTABLE)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                "Visuals" -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MochySurface,
                            border = BorderStroke(1.dp, MochyCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                MochySwitchRow("High Quality Bloom", "Extra soft bloom pass on artwork & lyrics", mochyState.highQualityBloom) { mochyState.highQualityBloom = it }
                                MochySwitchRow("Lyrics Shader Glow", "Glow effect on singing lines", mochyState.lyricsShader) { mochyState.lyricsShader = it }
                                MochySwitchRow("Audio Format Badge", "Lossless, Hi-Res, Dolby Atmos indicator", mochyState.audioFormatBadge) { mochyState.audioFormatBadge = it }
                                MochySwitchRow("Smooth Animations", "UI transition interpolations", mochyState.smoothAnimations) { mochyState.smoothAnimations = it }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MochyCustomizerOptionCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF131D16) else MochySurface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) NeonLimePrimary else MochyCardBorder
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) NeonLimePrimary else Color.Transparent)
                    .border(2.dp, if (isSelected) NeonLimePrimary else Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonLimePrimary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonLimePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 12.sp, color = MochyTextSecondary)
            }
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

private fun formatEndTime(remainingMs: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.MILLISECOND, remainingMs.toInt().coerceAtLeast(0))
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(cal.time)
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
fun MochyPlatformStatCard(name: String, count: Int, dotColor: Color, modifier: Modifier = Modifier) {
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

@Composable
fun MochyAudioLabHubScreen(
    mochyState: MochyUiStateHolder,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit
) {
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

        // 1. Equalizer Banner Card (02:47)
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

        // 2. Audio Effect (Reverb, Loudness, Spatial Audio, Dolby Atmos, Speed) Cards (03:36 - 04:17)
        item {
            Text("Audio FX", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MochySurface,
                border = BorderStroke(1.dp, MochyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MochySwitchRow(
                        title = "Reverb (${mochyState.reverbPreset})",
                        subtitle = "Room • Studio • Bathroom • Plate acoustic space",
                        checked = mochyState.reverbEnabled,
                        onCheckedChange = { mochyState.reverbEnabled = it }
                    )
                    MochySwitchRow(
                        title = "Loudness & ReplayGain",
                        subtitle = "Louder and fuller, never clipping (+${mochyState.replayGainPreampDb} dB)",
                        checked = mochyState.loudnessEnabled,
                        onCheckedChange = { mochyState.loudnessEnabled = it }
                    )
                    MochySwitchRow(
                        title = "Spatial audio (${mochyState.spatialMode})",
                        subtitle = "Takes stereo out of your head and puts it in front of you",
                        checked = mochyState.spatialAudioEnabled,
                        onCheckedChange = { mochyState.spatialAudioEnabled = it }
                    )
                    MochySwitchRow(
                        title = "Dolby Atmos & surround",
                        subtitle = "Plays 5.1, 7.1 and Atmos beds as 3D sound on any headphones",
                        checked = mochyState.dolbyAtmosSurround,
                        onCheckedChange = { mochyState.dolbyAtmosSurround = it }
                    )
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
fun MochySwitchRow(
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