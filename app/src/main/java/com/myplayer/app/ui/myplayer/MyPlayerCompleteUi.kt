package com.myplayer.app.ui.myplayer







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
import androidx.compose.material.icons.automirrored.filled.MenuBook



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



import androidx.compose.ui.graphics.drawscope.DrawScope



import androidx.compose.ui.graphics.drawscope.Stroke



import androidx.compose.ui.graphics.drawscope.rotate



import androidx.compose.ui.graphics.vector.ImageVector



import androidx.compose.ui.input.pointer.pointerInput



import androidx.compose.ui.layout.ContentScale



import androidx.compose.ui.layout.boundsInWindow



import androidx.compose.ui.layout.onGloballyPositioned



import androidx.compose.ui.platform.LocalView



import com.myplayer.app.myplayerfx.audio.MyPlayerAudioTap



import kotlinx.coroutines.flow.first



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



import com.myplayer.app.data.local.entity.SongEntity



import com.myplayer.app.data.recommendation.model.RecommendationSong



import com.myplayer.app.data.spotify.SpotifyIntegrationManager



import com.myplayer.app.playback.AudioQualityInfo



import com.myplayer.app.ui.common.AlbumArtImage



import androidx.hilt.navigation.compose.hiltViewModel



import androidx.lifecycle.compose.collectAsStateWithLifecycle



import androidx.media3.common.Player



import com.myplayer.app.ui.screens.nowplaying.CustomLyricsDialog



import com.myplayer.app.ui.screens.nowplaying.NowPlayingVisual



import com.myplayer.app.ui.screens.nowplaying.LyricsTab



import com.myplayer.app.ui.screens.nowplaying.LyricsUiState



import com.myplayer.app.ui.screens.nowplaying.LyricsViewModel



import com.myplayer.app.ui.screens.nowplaying.SleepTimerDialog



import com.myplayer.app.ui.screens.settings.SpotifyLoginDialog



import com.myplayer.app.ui.theme.*



import kotlinx.coroutines.delay



import kotlinx.coroutines.launch



import java.text.SimpleDateFormat



import java.util.*



import kotlin.math.*



import kotlin.random.Random







// ═════════════════════════════════════════════════════════════════════════════



// 1. MYPLAYER DARK THEME PALETTE & PLAYER STYLE ENUM (INCLUDES MYPLAYER RADIAL!)



// ═════════════════════════════════════════════════════════════════════════════







val MyPlayerBlack: Color get() = if (ThemeState.isDarkMode) Color(0xFF070709) else Color(0xFFF7F8FA)



val MyPlayerSurface: Color get() = if (ThemeState.isDarkMode) Color(0xFF131318) else Color(0xFFFFFFFF)



val MyPlayerSurfaceHigh: Color get() = if (ThemeState.isDarkMode) Color(0xFF1E1E26) else Color(0xFFEFEFF2)



val MyPlayerCardBorder: Color get() = if (ThemeState.isDarkMode) Color(0xFF1F1F27) else Color(0xFFE2E2E8)



val MyPlayerTextPrimary: Color get() = if (ThemeState.isDarkMode) Color(0xFFF5F5F7) else Color(0xFF17171C)



val MyPlayerTextSecondary: Color get() = if (ThemeState.isDarkMode) Color(0xFF9A9AA6) else Color(0xFF6B6B76)



val MyPlayerAccentGreen = Color(0xFF38E54D)



val MyPlayerAccentPurple = Color(0xFF8B5CF6)







/**



 * All 6 Selectable Player Styles in Settings -> Appearance -> Customize Player:



 * Includes the 5 MyPlayer styles from the video + your signature MYPLAYER RADIAL screen!



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







enum class MyPlayerEqPreset(val label: String, val gains: FloatArray) {



    STUDIO_MASTER("Studio Master", floatArrayOf(2.6f, 2.8f, 2.2f, 0.6f, -1.8f, -2.6f, 1.2f, 2.4f, 4.0f, 4.8f)),

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



class MyPlayerUiStateHolder(context: Context) {



    private val prefs = context.getSharedPreferences("myplayer_ui_prefs", Context.MODE_PRIVATE)







    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarding_done", false))



    var featureTourDone by mutableStateOf(prefs.getBoolean("feature_tour_done", false))



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



    var eqEnabled by mutableStateOf(prefs.getBoolean("myplayer_eq_enabled", true))



    var eqPreset by mutableStateOf(MyPlayerEqPreset.STUDIO_MASTER)



    var eqBands by mutableStateOf(MyPlayerEqPreset.STUDIO_MASTER.gains.toList())



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



        eqEnabled = prefs.getBoolean("myplayer_eq_enabled", eqEnabled)



        prefs.getString("al_eq_preset", null)?.let { n ->



            MyPlayerEqPreset.values().firstOrNull { it.name == n }?.let { eqPreset = it }



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



            .putBoolean("myplayer_eq_enabled", eqEnabled)



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



        username = name



        pickedArtists = artists



        onboardingDone = true



        prefs.edit()



            .putBoolean("onboarding_done", true)



            .putString("username", username)



            .putStringSet("picked_artists", artists)



            .apply()



    }







    fun completeFeatureTour() {



        featureTourDone = true



        prefs.edit().putBoolean("feature_tour_done", true).apply()



    }







    fun startFeatureTour() {



        featureTourDone = false



        prefs.edit().putBoolean("feature_tour_done", false).apply()



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



private var sharedMyPlayerUiStateHolder: MyPlayerUiStateHolder? = null







@Composable



fun rememberMyPlayerUiStateHolder(): MyPlayerUiStateHolder {



    val context = LocalContext.current



    return remember {



        sharedMyPlayerUiStateHolder ?: MyPlayerUiStateHolder(context.applicationContext).also { sharedMyPlayerUiStateHolder = it }



    }



}







// ═════════════════════════════════════════════════════════════════════════════



// 2. MYPLAYER PLAY LOGO & METALLIC PROFILE ID CARD (00:01 - 00:18, 00:38, 01:24)



// ═════════════════════════════════════════════════════════════════════════════







@Composable



fun MyPlayerPlayLogo(



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



fun MyPlayerProfileIdCard(



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



// 3. 5-STEP MYPLAYER ONBOARDING FLOW (00:04 - 00:39 IN VIDEO)



// ═════════════════════════════════════════════════════════════════════════════







private val ONBOARDING_ARTISTS = listOf(



    "Arijit Singh", "Karan Aujla", "Diljit Dosanjh", "Sidhu Moose Wala", "Atif Aslam",



    "Pritam", "Shreya Ghoshal", "Dhanda Nyoliwala", "Masoom Sharma", "AP Dhillon",



    "Seedhe Maut", "KR\$NA", "Yo Yo Honey Singh", "Badshah", "KK", "A.R. Rahman",



    "Vishal Mishra", "Anuv Jain", "The Weeknd", "Eminem", "Shubh", "Talha Anjum",



    "Nusrat Fateh Ali Khan", "Anirudh Ravichander", "Neha Kakkar", "Jubin Nautiyal",



    "Darshan Raval", "B Praak", "Armaan Malik", "Sonu Nigam", "Mohit Chauhan",



    "Kishore Kumar", "Lata Mangeshkar", "Mohammed Rafi", "Ed Sheeran", "Taylor Swift",



    "Drake", "Imagine Dragons", "BTS"



)







@Composable



fun MyPlayerOnboardingFlow(



    myplayerState: MyPlayerUiStateHolder,



    spotifyManager: SpotifyIntegrationManager? = null,



    onComplete: (username: String, selectedArtists: Set<String>) -> Unit



) {



    var step by remember { mutableIntStateOf(0) } // 0..5 matching 00:04..00:39



    // "Yes!" on the import step opens the real Spotify login screen; onboarding



    // advances to the profile step ONLY after a successful login & account link.



    var showSpotifyLogin by remember { mutableStateOf(false) }



    // System Back goes to the previous onboarding step instead of closing the whole app.



    // Gated while the Spotify login dialog is open — the dialog handles its own back.



    BackHandler(enabled = step in 1..5 && !showSpotifyLogin) { step -= 1 }







    if (showSpotifyLogin && spotifyManager != null) {



        SpotifyLoginDialog(



            spotifyManager = spotifyManager,



            onDismiss = { showSpotifyLogin = false },



            onLoginSuccess = { step = 2 }



        )



    }



    var nameInput by remember { mutableStateOf("") }



    var artistQuery by remember { mutableStateOf("") }



    var selectedArtists by remember { mutableStateOf(setOf<String>()) }







    val allArtists = remember(selectedArtists) {



        (ONBOARDING_ARTISTS + selectedArtists).distinct()



    }



    val filteredArtists = remember(artistQuery, allArtists) {



        val q = artistQuery.trim()



        if (q.isBlank()) allArtists



        else {



            val matches = allArtists.filter { it.contains(q, ignoreCase = true) }



            if (matches.none { it.equals(q, ignoreCase = true) }) listOf(q) + matches else matches



        }



    }







    Surface(



        modifier = Modifier.fillMaxSize(),



        color = MyPlayerBlack



    ) {



        AnimatedContent(



            targetState = step,



            transitionSpec = {



                (fadeIn(tween(260)) + slideInHorizontally { it / 4 })



                    .togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })



            },



            label = "myplayerOnboardingStep"



        ) { currentStep ->



            when (currentStep) {



                // STEP 0 (00:04 - 00:06): "Welcome to MyPlayer!" + Vinyl Tonearm Animation



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



                                color = MyPlayerTextPrimary



                            )



                            Spacer(Modifier.height(10.dp))



                            Text(



                                text = "A way to listen and own your\nmusic, differently.",



                                fontSize = 16.sp,



                                color = MyPlayerTextSecondary



                            )



                            Spacer(Modifier.height(44.dp))



                            MyPlayerPrimaryPillButton(



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



                                    .background(MyPlayerSurfaceHigh),



                                contentAlignment = Alignment.Center



                            ) {



                                Icon(Icons.Default.Download, contentDescription = null, tint = MyPlayerTextPrimary)



                            }



                            Spacer(Modifier.height(20.dp))



                            Text(



                                text = "Importing music",



                                fontSize = 32.sp,



                                fontWeight = FontWeight.ExtraBold,



                                color = MyPlayerTextPrimary



                            )



                            Spacer(Modifier.height(12.dp))



                            Text(



                                text = "Do you want to import music from other platforms like Spotify, Apple Music, Tidal or SoundCloud, or sync your data with YouTube Music?",



                                fontSize = 15.sp,



                                lineHeight = 22.sp,



                                color = MyPlayerTextSecondary



                            )



                        }







                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {



                            MyPlayerPrimaryPillButton(



                                text = "Yes!",



                                onClick = {



                                    if (spotifyManager != null) showSpotifyLogin = true else step = 2



                                }



                            )



                            MyPlayerSecondaryPillButton(



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



                                    .background(MyPlayerSurfaceHigh),



                                contentAlignment = Alignment.Center



                            ) {



                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = MyPlayerTextPrimary)



                            }



                            Spacer(Modifier.height(18.dp))



                            Text(



                                text = "Let's create your\nprofile!",



                                fontSize = 32.sp,



                                lineHeight = 38.sp,



                                fontWeight = FontWeight.ExtraBold,



                                color = MyPlayerTextPrimary



                            )



                            Spacer(Modifier.height(22.dp))



                            Text("what's your username?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MyPlayerTextPrimary)



                            Spacer(Modifier.height(10.dp))



                            OutlinedTextField(



                                value = nameInput,



                                onValueChange = { nameInput = it },



                                singleLine = true,



                                placeholder = { Text("your name", color = MyPlayerTextSecondary) },



                                shape = RoundedCornerShape(18.dp),



                                modifier = Modifier.fillMaxWidth(),



                                colors = OutlinedTextFieldDefaults.colors(



                                    focusedContainerColor = MyPlayerSurface,



                                    unfocusedContainerColor = MyPlayerSurface,



                                    focusedBorderColor = MyPlayerCardBorder,



                                    unfocusedBorderColor = Color.Transparent,



                                    focusedTextColor = MyPlayerTextPrimary,



                                    unfocusedTextColor = MyPlayerTextPrimary,



                                    cursorColor = MyPlayerTextPrimary



                                )



                            )



                            Spacer(Modifier.height(22.dp))



                            Text("Set a profile picture here!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MyPlayerTextPrimary)



                            Spacer(Modifier.height(12.dp))



                            MyPlayerProfileIdCard(



                                username = nameInput,



                                profileId = myplayerState.profileId,



                                joinedDate = myplayerState.joinedDate,



                                metallicLight = !ThemeState.isDarkMode



                            )



                        }







                        Column(horizontalAlignment = Alignment.CenterHorizontally) {



                            Text(



                                text = "MyPlayer will never store your data online!",



                                fontSize = 12.sp,



                                color = MyPlayerTextSecondary



                            )



                            Spacer(Modifier.height(12.dp))



                            MyPlayerPrimaryPillButton(



                                text = "Next",



                                onClick = { step = 3 }



                            )



                        }



                    }



                }







                // STEP 3 (00:16 - 00:18): Confetti + Tilted Metallic ID Card ("Hello! Welcome to MyPlayer!")



                3 -> {



                    LaunchedEffect(Unit) {



                        delay(2200)



                        step = 4



                    }



                    MyPlayerConfettiCardScreen(



                        username = nameInput,



                        profileId = myplayerState.profileId,



                        joinedDate = myplayerState.joinedDate,



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



                                .background(MyPlayerSurfaceHigh),



                            contentAlignment = Alignment.Center



                        ) {



                            Icon(Icons.Default.Home, contentDescription = null, tint = MyPlayerTextPrimary)



                        }



                        Spacer(Modifier.height(16.dp))



                        Text(



                            text = "Let's build your home",



                            fontSize = 30.sp,



                            fontWeight = FontWeight.ExtraBold,



                            color = MyPlayerTextPrimary



                        )



                        Spacer(Modifier.height(4.dp))



                        Text(



                            text = "Select any artist you really like",



                            fontSize = 15.sp,



                            color = MyPlayerTextSecondary



                        )



                        Spacer(Modifier.height(16.dp))







                        OutlinedTextField(



                            value = artistQuery,



                            onValueChange = { artistQuery = it },



                            singleLine = true,



                            placeholder = { Text("Search artists", color = MyPlayerTextSecondary) },



                            shape = RoundedCornerShape(24.dp),



                            modifier = Modifier.fillMaxWidth(),



                            colors = OutlinedTextFieldDefaults.colors(



                                focusedContainerColor = MyPlayerSurface,



                                unfocusedContainerColor = MyPlayerSurface,



                                focusedBorderColor = MyPlayerCardBorder,



                                unfocusedBorderColor = Color.Transparent,



                                focusedTextColor = MyPlayerTextPrimary,



                                unfocusedTextColor = MyPlayerTextPrimary,



                                cursorColor = MyPlayerTextPrimary



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



                                val isDark = ThemeState.isDarkMode



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



                                                if (isDark) {



                                                    Brush.linearGradient(



                                                        listOf(



                                                            Color(0xFF283048),



                                                            Color(0xFF181B24)



                                                        )



                                                    )



                                                } else {



                                                    Brush.linearGradient(



                                                        listOf(



                                                            Color(0xFFE6E9F0),



                                                            Color(0xFFCFD5E1)



                                                        )



                                                    )



                                                }



                                            )



                                            .border(



                                                width = if (isPicked) 2.5.dp else 1.dp,



                                                color = if (isPicked) (if (isDark) Color.White else MyPlayerAccentGreen) else MyPlayerCardBorder,



                                                shape = CircleShape



                                            ),



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = artist.split(" ").take(2).joinToString("") { it.take(1) },



                                            fontSize = 24.sp,



                                            fontWeight = FontWeight.Bold,



                                            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF2D313A).copy(alpha = 0.85f)



                                        )



                                        if (isPicked) {



                                            Box(



                                                modifier = Modifier



                                                    .fillMaxSize()



                                                    .background(if (isDark) Color.Black.copy(alpha = 0.48f) else Color(0xFF181B24).copy(alpha = 0.35f)),



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



                                        fontWeight = if (isPicked) FontWeight.Bold else FontWeight.SemiBold,



                                        color = if (isPicked && !isDark) Color(0xFF0F9D58) else MyPlayerTextPrimary,



                                        maxLines = 1,



                                        overflow = TextOverflow.Ellipsis,



                                        textAlign = TextAlign.Center



                                    )



                                }



                            }



                        }







                        Spacer(Modifier.height(12.dp))



                        MyPlayerPrimaryPillButton(



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



                            step = 6



                        }



                        MyPlayerConfettiCardScreen(



                            username = nameInput,



                            profileId = myplayerState.profileId,



                            joinedDate = myplayerState.joinedDate,



                            bottomTitle = "You're good to go",



                            bottomSubtitle = "Starting quick interactive feature tour...",



                            showTopCheck = true,



                            onTapContinue = {



                                step = 6



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



                                        .background(MyPlayerSurfaceHigh),



                                    contentAlignment = Alignment.Center



                                ) {



                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MyPlayerTextPrimary)



                                }



                                Spacer(Modifier.height(16.dp))



                                Text(



                                    text = "Last one",



                                    fontSize = 32.sp,



                                    fontWeight = FontWeight.ExtraBold,



                                    color = MyPlayerTextPrimary



                                )



                                Spacer(Modifier.height(4.dp))



                                Text(



                                    text = "Disable anything that you don't like",



                                    fontSize = 15.sp,



                                    color = MyPlayerTextSecondary



                                )



                                Spacer(Modifier.height(22.dp))







                                // Auto Crossfade Card with Dual Waveform Graphic (00:35)



                                Surface(



                                    shape = RoundedCornerShape(24.dp),



                                    color = MyPlayerSurface,



                                    border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                                                Text("Auto Crossfade", fontWeight = FontWeight.Bold, color = MyPlayerTextPrimary)



                                                Text(



                                                    "Automatically crossfades, analysing the pitch, tempo and more to pick the perfect moment",



                                                    fontSize = 12.sp,



                                                    color = MyPlayerTextSecondary



                                                )



                                            }



                                            Switch(



                                                checked = myplayerState.autoCrossfade,



                                                onCheckedChange = { myplayerState.autoCrossfade = it }



                                            )



                                        }



                                    }



                                }







                                Spacer(Modifier.height(14.dp))







                                // AI Artist Filter Card (00:35)



                                var aiArtistFilter by remember { mutableStateOf(true) }



                                Surface(



                                    shape = RoundedCornerShape(24.dp),



                                    color = MyPlayerSurface,



                                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                                    modifier = Modifier.fillMaxWidth()



                                ) {



                                    Row(



                                        modifier = Modifier.padding(18.dp),



                                        verticalAlignment = Alignment.CenterVertically,



                                        horizontalArrangement = Arrangement.SpaceBetween



                                    ) {



                                        Column(modifier = Modifier.weight(1f)) {



                                            Text("AI Artist filter", fontWeight = FontWeight.Bold, color = MyPlayerTextPrimary)



                                            Text(



                                                "Turning this off may give you AI slop recommendations",



                                                fontSize = 12.sp,



                                                color = MyPlayerTextSecondary



                                            )



                                        }



                                        Switch(



                                            checked = aiArtistFilter,



                                            onCheckedChange = { aiArtistFilter = it }



                                        )



                                    }



                                }



                            }







                            MyPlayerPrimaryPillButton(



                                text = "next",



                                onClick = { showFinalCard = true }



                            )



                        }



                    }



                }







                // STEP 6: INTERACTIVE CLICK-THROUGH SETTINGS & FEATURES TOUR



                6 -> {



                    MyPlayerInteractiveFeatureTour(



                        myplayerState = myplayerState,



                        onFinishTour = {



                            myplayerState.completeFeatureTour()



                            myplayerState.completeOnboarding(nameInput, selectedArtists)



                            onComplete(nameInput, selectedArtists)



                        }



                    )



                }



            }



        }



    }



}











// =============================================================================



// INTERACTIVE CLICK-THROUGH SETTINGS & FEATURES TOUR (7 steps)



// Appearance -> Customize Player -> 6 Styles -> Audio Lab -> EQ -> FX -> Crossfade



// =============================================================================



@Composable



fun MyPlayerInteractiveFeatureTour(



    myplayerState: MyPlayerUiStateHolder,



    onApplyEqToEngine: (enabled: Boolean, gains10: List<Float>, preampDb: Float) -> Unit = { _, _, _ -> },



    onFinishTour: () -> Unit



) {



    // 0 = Tap "Appearance" in Settings Hub



    // 1 = Tap "Customize player" inside Appearance



    // 2 = Explore 6 Player Styles in MyPlayerPlayerStylesPickerScreen -> Tap "Next: Audio Lab"



    // 3 = Tap "Audio Lab" in Settings Hub



    // 4 = Tap "Equalizer" inside Audio Lab -> Explore 10-Band EQ & AutoEQ -> Tap "Next: Audio FX"



    // 5 = Tap "Audio Effect (FX)" inside Audio Lab -> Explore Spatial Audio, Reverb, Loudness, Dolby Atmos -> Tap "Next"



    // 6 = Final Step: Playback, Auto Crossfade & Finish Tour!



    var tourStep by remember { mutableIntStateOf(0) }



    var eqOpenedInStep4 by remember { mutableStateOf(false) }



    var fxOpenedInStep5 by remember { mutableStateOf(false) }







    val stepTitles = listOf(



        "Step 1/7 • Open Appearance Settings",



        "Step 2/7 • Open Customize Player",



        "Step 3/7 • 6 Custom Now Playing Screens",



        "Step 4/7 • Open Audio Lab",



        "Step 5/7 • 10-Band Equalizer & AutoEQ",



        "Step 6/7 • Real-Time Audio FX & Spatial Audio",



        "Step 7/7 • User Manual, Crossfade & Ready to Play!"



    )







    val stepInstructions = listOf(



        "Neeche highlighted 'Appearance' card pe click karo! Yahan se tum player ke visual effects aur styles control kar sakte ho.",



        "Yahan Lyrics Shader glow, High Quality Bloom aur Glass Blur milte hain. Ab highlighted 'Customize player' card pe click karo!",



        "Swipe karke apna favorite Now Playing style chuno — Radial (MyPlayer 3D Tilt + Neon Dial), Turntable Vinyl, Poster, Timeline ya Lyrics Stage!",



        "Ab Settings me highlighted 'Audio Lab' card pe click karo! Yahan MyPlayer ka pro sound engine aur DSP effects hain.",



        if (!eqOpenedInStep4) "Highlighted 'Equalizer' card pe click karke 10-Band Rainbow EQ aur AutoEQ open karo!"



        else "Graph nodes drag karo, 10 Bands pe finger sweep karo, ya AutoEQ me apne headphones (AirPods, Sony, Sennheiser) select karo!",



        if (!fxOpenedInStep5) "Ab highlighted 'Audio Effect (FX)' card pe click karo jisme live 60fps animations ke saath Spatial Audio aur Reverb hai!"



        else "Try karke dekho: Studio Reverb (Hall/Cathedral), Loudness Booster, Spatial Audio (Crossfeed/Speakers), 3D Dolby Atmos aur Slowed + Reverb!",



        "Badhiya! Tumne MyPlayer ke pro features dekh liye. Neeche User Manual card pe tap karke gestures (slide to skip) aur feature paths padh sakte ho!"



    )







    Column(



        modifier = Modifier



            .fillMaxSize()



            .background(MyPlayerBlack)



            .statusBarsPadding()



            .navigationBarsPadding()



    ) {



        // ── TOP INTERACTIVE TOUR COACHMARK BANNER ─────────────────────────────



        MyPlayerTourCoachmarkHeader(



            stepIndex = tourStep,



            totalSteps = 7,



            title = stepTitles[tourStep],



            instruction = stepInstructions[tourStep],



            showNextButton = when (tourStep) {



                2 -> true



                4 -> eqOpenedInStep4



                5 -> fxOpenedInStep5



                6 -> true



                else -> false



            },



            nextButtonLabel = when (tourStep) {



                2 -> "Next: Audio Lab →"



                4 -> "Next: Audio FX →"



                5 -> "Next: Final Step →"



                6 -> "Finish Tour & Start Listening 🎵"



                else -> "Next →"



            },



            onNextClick = {



                when (tourStep) {



                    2 -> tourStep = 3



                    4 -> tourStep = 5



                    5 -> tourStep = 6



                    6 -> {



                        myplayerState.completeFeatureTour()



                        onFinishTour()



                    }



                }



            },



            onSkipClick = {



                myplayerState.completeFeatureTour()



                onFinishTour()



            }



        )







        // ── INTERACTIVE TARGET SCREEN AREA ────────────────────────────────────



        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {



            when (tourStep) {



                // STEP 0: Settings Hub with Pulsing Spotlight on "Appearance"



                0 -> {



                    MyPlayerTourSettingsRootPreview(



                        highlightedRoute = "appearance",



                        highlightBadgeText = "👆 TAP TO OPEN APPEARANCE",



                        onHighlightedCardClick = { tourStep = 1 }



                    )



                }







                // STEP 1: Appearance Screen with Pulsing Spotlight on "Customize player"



                1 -> {



                    Column(



                        modifier = Modifier



                            .fillMaxSize()



                            .verticalScroll(rememberScrollState())



                            .padding(horizontal = 20.dp, vertical = 12.dp),



                        verticalArrangement = Arrangement.spacedBy(14.dp)



                    ) {



                        Text("Appearance", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



                        Text("Player appearance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)







                        // Highlighted "Customize player" card that user clicks to open Step 2!



                        MyPlayerTourSpotlightBox(



                            badgeText = "👆 TAP TO OPEN PLAYER STYLES",



                            onClick = { tourStep = 2 }



                        ) {



                            Surface(



                                shape = RoundedCornerShape(22.dp),



                                color = MyPlayerSurfaceHigh,



                                modifier = Modifier.fillMaxWidth()



                            ) {



                                Row(



                                    modifier = Modifier.padding(18.dp),



                                    verticalAlignment = Alignment.CenterVertically



                                ) {



                                    Icon(Icons.Default.Album, null, tint = MyPlayerAccentGreen, modifier = Modifier.size(28.dp))



                                    Spacer(Modifier.width(14.dp))



                                    Column(modifier = Modifier.weight(1f)) {



                                        Text("Customize player", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



                                        Text(



                                            "Active: ${myplayerState.playerStyle.title} • Tap to preview all 6 styles",



                                            fontSize = 12.sp,



                                            color = MyPlayerAccentGreen



                                        )



                                    }



                                    Icon(Icons.Default.TouchApp, null, tint = MyPlayerTextPrimary)



                                }



                            }



                        }







                        // Live interactive Appearance Switches so user sees what else is in Appearance



                        Surface(



                            shape = RoundedCornerShape(24.dp),



                            color = MyPlayerSurface,



                            border = BorderStroke(1.dp, MyPlayerCardBorder),



                            modifier = Modifier.fillMaxWidth()



                        ) {



                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {



                                MyPlayerSwitchRow(



                                    title = "Lyrics Shader",



                                    subtitle = "Glow on sung lyrics",



                                    checked = myplayerState.lyricsShader,



                                    onCheckedChange = { myplayerState.lyricsShader = it }



                                )



                                MyPlayerSwitchRow(



                                    title = "High quality bloom",



                                    subtitle = "Extra soft bloom pass on lyrics",



                                    checked = myplayerState.highQualityBloom,



                                    onCheckedChange = { myplayerState.highQualityBloom = it }



                                )



                                MyPlayerSwitchRow(



                                    title = "Audio format badge",



                                    subtitle = "Shows lossless/kbps format badge beside song title",



                                    checked = myplayerState.audioFormatBadge,



                                    onCheckedChange = { myplayerState.audioFormatBadge = it }



                                )



                                MyPlayerSwitchRow(



                                    title = "Blur & Progressive Blur",



                                    subtitle = "Glass blur across player and navigation",



                                    checked = myplayerState.glassBlur,



                                    onCheckedChange = { myplayerState.glassBlur = it }



                                )



                            }



                        }



                    }



                }







                // STEP 2: Full Live 6-Style Player Picker (Radial MyPlayer, Turntable, Poster, Timeline, Lyrics Stage, Default)



                2 -> {



                    MyPlayerPlayerStylesPickerScreen(



                        myplayerState = myplayerState,



                        onBack = { tourStep = 3 }



                    )



                }







                // STEP 3: Settings Hub with Pulsing Spotlight on "Audio Lab"



                3 -> {



                    MyPlayerTourSettingsRootPreview(



                        highlightedRoute = "audio_lab",



                        highlightBadgeText = "👆 TAP TO OPEN AUDIO LAB",



                        onHighlightedCardClick = { tourStep = 4 }



                    )



                }







                // STEP 4: Audio Lab -> User clicks "Equalizer" -> Opens Live 10-Band Equalizer & AutoEQ!



                4 -> {



                    if (!eqOpenedInStep4) {



                        Column(



                            modifier = Modifier



                                .fillMaxSize()



                                .padding(horizontal = 20.dp, vertical = 14.dp),



                            verticalArrangement = Arrangement.spacedBy(16.dp)



                        ) {



                            Text("Audio Lab", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)







                            MyPlayerTourSpotlightBox(



                                badgeText = "👆 TAP TO OPEN 10-BAND EQUALIZER",



                                onClick = { eqOpenedInStep4 = true }



                            ) {



                                Surface(



                                    shape = RoundedCornerShape(26.dp),



                                    color = MyPlayerSurfaceHigh,



                                    modifier = Modifier



                                        .fillMaxWidth()



                                        .height(122.dp)



                                ) {



                                    Box(modifier = Modifier.padding(20.dp).fillMaxSize()) {



                                        Text(



                                            text = "● 10-BAND GRAPH • SWEEP SLIDERS • AUTOEQ",



                                            fontSize = 11.sp,



                                            fontWeight = FontWeight.Bold,



                                            color = MyPlayerAccentGreen,



                                            modifier = Modifier.align(Alignment.TopStart)



                                        )



                                        Text(



                                            text = "Equalizer",



                                            fontSize = 26.sp,



                                            fontWeight = FontWeight.ExtraBold,



                                            color = MyPlayerTextPrimary,



                                            modifier = Modifier.align(Alignment.BottomStart)



                                        )



                                        Icon(



                                            imageVector = Icons.Default.Tune,



                                            contentDescription = null,



                                            tint = MyPlayerTextPrimary.copy(alpha = 0.22f),



                                            modifier = Modifier.size(84.dp).align(Alignment.CenterEnd).rotate(-14f)



                                        )



                                    }



                                }



                            }







                            Surface(



                                shape = RoundedCornerShape(26.dp),



                                color = MyPlayerSurface.copy(alpha = 0.5f),



                                border = BorderStroke(1.dp, MyPlayerCardBorder),



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .height(110.dp)



                                    .alpha(0.55f)



                            ) {



                                Box(modifier = Modifier.padding(20.dp).fillMaxSize()) {



                                    Text("UP NEXT IN STEP 6", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                                    Text(



                                        text = "Audio Effect (FX)",



                                        fontSize = 24.sp,



                                        fontWeight = FontWeight.ExtraBold,



                                        color = MyPlayerTextPrimary,



                                        modifier = Modifier.align(Alignment.BottomStart)



                                    )



                                }



                            }



                        }



                    } else {



                        MyPlayerEqualizerScreen(



                            myplayerState = myplayerState,



                            onApplyGainsToEngine = onApplyEqToEngine,



                            onBack = { tourStep = 5 }



                        )



                    }



                }







                // STEP 5: Audio Lab -> User clicks "Audio Effect (FX)" -> Opens Full 60fps MyPlayerAudioFxScreen!



                5 -> {



                    if (!fxOpenedInStep5) {



                        Column(



                            modifier = Modifier



                                .fillMaxSize()



                                .padding(horizontal = 20.dp, vertical = 14.dp),



                            verticalArrangement = Arrangement.spacedBy(16.dp)



                        ) {



                            Text("Audio Lab", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)







                            Surface(



                                shape = RoundedCornerShape(26.dp),



                                color = MyPlayerSurface.copy(alpha = 0.5f),



                                border = BorderStroke(1.dp, MyPlayerCardBorder),



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .height(96.dp)



                                    .alpha(0.6f)



                            ) {



                                Box(modifier = Modifier.padding(18.dp).fillMaxSize()) {



                                    Text("✓ EXPLORED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MyPlayerAccentGreen)



                                    Text(



                                        text = "Equalizer",



                                        fontSize = 22.sp,



                                        fontWeight = FontWeight.ExtraBold,



                                        color = MyPlayerTextPrimary,



                                        modifier = Modifier.align(Alignment.BottomStart)



                                    )



                                }



                            }







                            MyPlayerTourSpotlightBox(



                                badgeText = "👆 TAP TO OPEN REAL-TIME AUDIO FX",



                                onClick = { fxOpenedInStep5 = true }



                            ) {



                                Surface(



                                    shape = RoundedCornerShape(26.dp),



                                    color = MyPlayerSurfaceHigh,



                                    modifier = Modifier



                                        .fillMaxWidth()



                                        .height(130.dp)



                                ) {



                                    Box(modifier = Modifier.padding(20.dp).fillMaxSize()) {



                                        Text(



                                            text = "● REVERB • SPATIAL AUDIO • DOLBY ATMOS • SLOWED+REVERB",



                                            fontSize = 11.sp,



                                            fontWeight = FontWeight.Bold,



                                            color = MyPlayerAccentGreen,



                                            modifier = Modifier.align(Alignment.TopStart)



                                        )



                                        Text(



                                            text = "Audio Effect",



                                            fontSize = 28.sp,



                                            fontWeight = FontWeight.ExtraBold,



                                            color = MyPlayerTextPrimary,



                                            modifier = Modifier.align(Alignment.BottomStart)



                                        )



                                        Text(



                                            text = "FX",



                                            fontSize = 68.sp,



                                            fontWeight = FontWeight.Black,



                                            color = MyPlayerTextPrimary.copy(alpha = 0.14f),



                                            modifier = Modifier.align(Alignment.CenterEnd).rotate(-10f)



                                        )



                                    }



                                }



                            }



                        }



                    } else {



                        MyPlayerAudioFxScreen(



                            myplayerState = myplayerState,



                            onBack = { tourStep = 6 }



                        )



                    }



                }







                // STEP 6: Final Step — Smart Crossfade, Playback & Tour Completion Summary



                6 -> {



                    Column(



                        modifier = Modifier



                            .fillMaxSize()



                            .verticalScroll(rememberScrollState())



                            .padding(horizontal = 20.dp, vertical = 14.dp),



                        verticalArrangement = Arrangement.spacedBy(14.dp)



                    ) {



                        Text("You're All Set! 🎉", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



                        Text(



                            "Quick recap of your active MyPlayer setup (you can change these anytime in Settings):",



                            fontSize = 13.sp,



                            color = MyPlayerTextSecondary



                        )







                        // Prominent User Manual Card in Feature Tour
                        var showManualInTour by remember { mutableStateOf(false) }
                        if (showManualInTour) {
                            com.myplayer.app.ui.screens.settings.UserManualDialog(
                                onDismiss = { showManualInTour = false }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MyPlayerSurface,
                            border = BorderStroke(1.5.dp, MyPlayerAccentPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualInTour = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MyPlayerAccentPurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MyPlayerAccentPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "User Manual & Feature Guide",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MyPlayerTextPrimary
                                    )
                                    Text(
                                        "Miniplayer slide gestures, Dual Buds, Sync Play & audio engine paths",
                                        fontSize = 12.sp,
                                        color = MyPlayerTextSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MyPlayerAccentPurple
                                ) {
                                    Text(
                                        "Read",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Quick Summary Badges



                        Surface(



                            shape = RoundedCornerShape(24.dp),



                            color = MyPlayerSurface,



                            border = BorderStroke(1.dp, MyPlayerCardBorder),



                            modifier = Modifier.fillMaxWidth()



                        ) {



                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {



                                Row(verticalAlignment = Alignment.CenterVertically) {



                                    Icon(Icons.Default.Album, null, tint = MyPlayerAccentGreen)



                                    Spacer(Modifier.width(12.dp))



                                    Column {



                                        Text("Selected Now Playing Screen", fontSize = 12.sp, color = MyPlayerTextSecondary)



                                        Text(myplayerState.playerStyle.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



                                    }



                                }



                                HorizontalDivider(color = MyPlayerCardBorder)



                                Row(verticalAlignment = Alignment.CenterVertically) {



                                    Icon(Icons.Default.GraphicEq, null, tint = MyPlayerAccentPurple)



                                    Spacer(Modifier.width(12.dp))



                                    Column {



                                        Text("Equalizer & Audio FX", fontSize = 12.sp, color = MyPlayerTextSecondary)



                                        Text(



                                            text = "EQ: ${if (myplayerState.eqEnabled) myplayerState.eqPreset.label else "Off"} • Spatial: ${if (myplayerState.spatialAudioEnabled) myplayerState.spatialMode else "Off"} • Reverb: ${if (myplayerState.reverbEnabled) myplayerState.reverbPreset else "Off"}",



                                            fontSize = 14.sp,



                                            fontWeight = FontWeight.Bold,



                                            color = Color.White



                                        )



                                    }



                                }



                            }



                        }







                        // Interactive Playback & Crossfade Card



                        Surface(



                            shape = RoundedCornerShape(24.dp),



                            color = MyPlayerSurface,



                            border = BorderStroke(1.dp, MyPlayerCardBorder),



                            modifier = Modifier.fillMaxWidth()



                        ) {



                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {



                                CrossfadeWaveformGraphic(labelLeft = "Track 1", labelRight = "Track 2")



                                MyPlayerSwitchRow(



                                    title = "Auto Crossfade",



                                    subtitle = "Smoothly blends outgoing and incoming songs",



                                    checked = myplayerState.autoCrossfade,



                                    onCheckedChange = { myplayerState.autoCrossfade = it }



                                )



                                MyPlayerSwitchRow(



                                    title = "Gapless albums",



                                    subtitle = "Keep live albums and transitions seamless",



                                    checked = myplayerState.keepGaplessAlbums,



                                    onCheckedChange = { myplayerState.keepGaplessAlbums = it }



                                )



                            }



                        }







                        Spacer(Modifier.height(8.dp))



                        Button(



                            onClick = {



                                myplayerState.completeFeatureTour()



                                onFinishTour()



                            },



                            shape = RoundedCornerShape(28.dp),



                            colors = ButtonDefaults.buttonColors(



                                containerColor = MyPlayerAccentGreen,



                                contentColor = Color.Black



                            ),



                            modifier = Modifier



                                .fillMaxWidth()



                                .height(56.dp)



                        ) {



                            Text(



                                text = "Finish Tour & Start Listening 🎵",



                                fontSize = 16.sp,



                                fontWeight = FontWeight.ExtraBold



                            )



                        }



                        Spacer(Modifier.height(24.dp))



                    }



                }



            }



        }



    }



}







@Composable



private fun MyPlayerTourCoachmarkHeader(



    stepIndex: Int,



    totalSteps: Int,



    title: String,



    instruction: String,



    showNextButton: Boolean,



    nextButtonLabel: String,



    onNextClick: () -> Unit,



    onSkipClick: () -> Unit



) {



    val isDark = ThemeState.isDarkMode



    Surface(



        color = if (isDark) Color(0xFF15151C) else Color(0xFFFFFFFF),



        border = BorderStroke(1.dp, if (isDark) MyPlayerAccentPurple.copy(alpha = 0.55f) else Color(0xFFD4D4E0)),



        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),



        modifier = Modifier.fillMaxWidth()



    ) {



        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {



            // Top Progress Dots + Skip Tour button



            Row(



                modifier = Modifier.fillMaxWidth(),



                horizontalArrangement = Arrangement.SpaceBetween,



                verticalAlignment = Alignment.CenterVertically



            ) {



                Row(



                    horizontalArrangement = Arrangement.spacedBy(6.dp),



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    repeat(totalSteps) { idx ->



                        val active = idx == stepIndex



                        val done = idx < stepIndex



                        Box(



                            modifier = Modifier



                                .height(6.dp)



                                .width(if (active) 24.dp else 10.dp)



                                .clip(RoundedCornerShape(3.dp))



                                .background(



                                    when {



                                        active -> MyPlayerAccentGreen



                                        done -> MyPlayerAccentPurple



                                        else -> if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)



                                    }



                                )



                        )



                    }



                }



                Text(



                    text = "Skip Tour ✕",



                    fontSize = 12.sp,



                    fontWeight = FontWeight.Bold,



                    color = MyPlayerTextSecondary,



                    modifier = Modifier.clickable(onClick = onSkipClick)



                )



            }







            Spacer(Modifier.height(10.dp))







            Text(



                text = title,



                fontSize = 16.sp,



                fontWeight = FontWeight.ExtraBold,



                color = if (isDark) MyPlayerAccentGreen else Color(0xFF0F9D58)



            )



            Spacer(Modifier.height(4.dp))



            Text(



                text = instruction,



                fontSize = 13.sp,



                lineHeight = 18.sp,



                color = MyPlayerTextPrimary



            )







            if (showNextButton) {



                Spacer(Modifier.height(12.dp))



                Button(



                    onClick = onNextClick,



                    shape = RoundedCornerShape(20.dp),



                    colors = ButtonDefaults.buttonColors(



                        containerColor = if (isDark) Color.White else Color(0xFF17171C),



                        contentColor = if (isDark) Color.Black else Color.White



                    ),



                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),



                    modifier = Modifier.align(Alignment.End)



                ) {



                    Text(



                        text = nextButtonLabel,



                        fontSize = 13.sp,



                        fontWeight = FontWeight.ExtraBold



                    )



                }



            }



        }



    }



}







@Composable



private fun MyPlayerTourSpotlightBox(



    badgeText: String,



    onClick: () -> Unit,



    content: @Composable () -> Unit



) {



    val infinite = rememberInfiniteTransition(label = "tourSpotlight")



    val pulseScale by infinite.animateFloat(



        initialValue = 1.0f,



        targetValue = 1.025f,



        animationSpec = infiniteRepeatable(



            animation = tween(750, easing = FastOutSlowInEasing),



            repeatMode = RepeatMode.Reverse



        ),



        label = "spotlightScale"



    )



    val badgeOffsetY by infinite.animateFloat(



        initialValue = 0f,



        targetValue = -6f,



        animationSpec = infiniteRepeatable(



            animation = tween(600, easing = FastOutSlowInEasing),



            repeatMode = RepeatMode.Reverse



        ),



        label = "badgeBounce"



    )







    Column(modifier = Modifier.fillMaxWidth()) {



        // Bouncing "TAP HERE" Indicator Pill



        Surface(



            shape = RoundedCornerShape(16.dp),



            color = MyPlayerAccentGreen,



            modifier = Modifier



                .align(Alignment.End)



                .graphicsLayer { translationY = badgeOffsetY }



                .padding(bottom = 6.dp)



                .clickable(onClick = onClick)



        ) {



            Text(



                text = badgeText,



                fontSize = 11.sp,



                fontWeight = FontWeight.ExtraBold,



                color = Color.Black,



                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)



            )



        }







        Box(



            modifier = Modifier



                .fillMaxWidth()



                .scale(pulseScale)



                .clip(RoundedCornerShape(24.dp))



                .border(



                    width = 2.5.dp,



                    brush = Brush.horizontalGradient(listOf(MyPlayerAccentGreen, MyPlayerAccentPurple, MyPlayerAccentGreen)),



                    shape = RoundedCornerShape(24.dp)



                )



                .clickable(onClick = onClick)



        ) {



            content()



        }



    }



}







@Composable



private fun MyPlayerTourSettingsRootPreview(



    highlightedRoute: String,



    highlightBadgeText: String,



    onHighlightedCardClick: () -> Unit



) {



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







    LazyColumn(



        modifier = Modifier.fillMaxSize(),



        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),



        verticalArrangement = Arrangement.spacedBy(12.dp)



    ) {



        item {



            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



            Spacer(Modifier.height(4.dp))



            Text(



                "Tap the highlighted section below to see what's inside:",



                fontSize = 13.sp,



                color = MyPlayerTextSecondary



            )



        }







        items(categories) { (title, icon, route) ->



            val isTarget = route == highlightedRoute



            if (isTarget) {



                MyPlayerTourSpotlightBox(



                    badgeText = highlightBadgeText,



                    onClick = onHighlightedCardClick



                ) {



                    Surface(



                        shape = RoundedCornerShape(22.dp),



                        color = MyPlayerSurfaceHigh,



                        modifier = Modifier.fillMaxWidth()



                    ) {



                        Row(



                            modifier = Modifier.padding(18.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MyPlayerAccentGreen.copy(alpha = 0.2f)),



                                contentAlignment = Alignment.Center



                            ) {



                                Icon(icon, contentDescription = null, tint = MyPlayerAccentGreen, modifier = Modifier.size(22.dp))



                            }



                            Spacer(Modifier.width(16.dp))



                            Column(modifier = Modifier.weight(1f)) {



                                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



                                Text(



                                    text = if (route == "appearance") "Customize Player (6 Styles), Lyrics Shader & Blur"



                                    else "10-Band Equalizer, AutoEQ, Spatial Audio & Reverb FX",



                                    fontSize = 12.sp,



                                    color = MyPlayerAccentGreen



                                )



                            }



                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = MyPlayerTextPrimary)



                        }



                    }



                }



            } else {



                Surface(



                    shape = RoundedCornerShape(22.dp),



                    color = MyPlayerSurface.copy(alpha = 0.45f),



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                    modifier = Modifier



                        .fillMaxWidth()



                        .alpha(0.45f)



                ) {



                    Row(



                        modifier = Modifier.padding(16.dp),



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        Box(



                            modifier = Modifier.size(42.dp).clip(CircleShape).background(MyPlayerSurfaceHigh),



                            contentAlignment = Alignment.Center



                        ) {



                            Icon(icon, contentDescription = null, tint = MyPlayerTextPrimary, modifier = Modifier.size(20.dp))



                        }



                        Spacer(Modifier.width(16.dp))



                        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextPrimary)



                    }



                }



            }



        }



        item { Spacer(Modifier.height(40.dp)) }



    }



}



@Composable



private fun MyPlayerConfettiCardScreen(



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



                            color = if (ThemeState.isDarkMode) Color.White.copy(alpha = 0.75f) else Color(0xFF2C2C35).copy(alpha = 0.6f),



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



                    color = MyPlayerTextPrimary



                )



                Spacer(Modifier.height(24.dp))



            }







            Box(modifier = Modifier.graphicsLayer { rotationZ = -6f }) {



                MyPlayerProfileIdCard(



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



                    color = MyPlayerTextPrimary



                )



                if (bottomSubtitle.isNotBlank()) {



                    Text(



                        text = bottomSubtitle,



                        fontSize = 18.sp,



                        fontWeight = FontWeight.SemiBold,



                        color = MyPlayerTextSecondary



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



    val isDark = ThemeState.isDarkMode



    Box(



        modifier = modifier



            .fillMaxWidth()



            .height(110.dp)



            .clip(RoundedCornerShape(16.dp))



            .background(if (isDark) Color(0xFF0E0E12) else Color(0xFFEFEFF2))



            .border(1.dp, MyPlayerCardBorder, RoundedCornerShape(16.dp))



            .padding(12.dp)



    ) {



        Canvas(modifier = Modifier.fillMaxSize()) {



            val bars = 68



            val stepX = size.width / bars



            val midY = size.height / 2f



            val baseColor = if (isDark) Color.White else Color(0xFF17171C)



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



                    color = baseColor.copy(alpha = 0.35f),



                    start = Offset(x, midY - hOut),



                    end = Offset(x, midY + hOut),



                    strokeWidth = 2.dp.toPx()



                )



                drawLine(



                    color = baseColor.copy(alpha = 0.75f),



                    start = Offset(x, midY - hIn),



                    end = Offset(x, midY + hIn),



                    strokeWidth = 2.dp.toPx()



                )



            }



        }



        Text(



            text = labelLeft,



            fontSize = 10.sp,



            color = MyPlayerTextSecondary,



            modifier = Modifier.align(Alignment.BottomStart)



        )



        Text(



            text = labelRight,



            fontSize = 10.sp,



            fontWeight = FontWeight.Bold,



            color = MyPlayerTextPrimary,



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



//    - Fixed: `styleOverride` in `MyPlayerPlayerStylesPickerScreen` so each card in the



//      HorizontalPager previews its own distinct style!



// ═════════════════════════════════════════════════════════════════════════════







@OptIn(ExperimentalMaterial3Api::class)



@Composable



fun MyPlayerMultiStyleNowPlayingScreen(



    myplayerState: MyPlayerUiStateHolder,



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



    val activeStyle = styleOverride ?: myplayerState.playerStyle







    // ── Day/Night bridge: custom player styles flip with the app-wide theme ──



    val isDarkPlayer = ThemeState.isDarkMode



    val onBg = if (isDarkPlayer) Color.White else CharcoalPrimary



    val myplayerSec = if (isDarkPlayer) MyPlayerTextSecondary else SlateSecondary



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



                containerColor = if (isDarkPlayer) MyPlayerSurface else PureWhite,



                titleContentColor = onBg,



                textContentColor = myplayerSec,



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



                containerColor = if (isDarkPlayer) MyPlayerSurface else PureWhite,



                titleContentColor = onBg,



                textContentColor = myplayerSec,



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



                        Text("Cancel", color = myplayerSec)



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



                    containerColor = if (isDarkPlayer) MyPlayerSurface else PureWhite



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



                                    color = onBg



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



                        text = { Text("Sleep Timer", color = onBg) },



                        leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null, tint = NeonLimePrimary) },



                        onClick = {



                            showOptionsMenu = false



                            showSleepTimer = true



                        }



                    )



                    DropdownMenuItem(



                        text = { Text("Audio Details", color = onBg) },



                        leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NeonLimePrimary) },



                        onClick = {



                            showOptionsMenu = false



                            showQualityDetails = true



                        }



                    )



                    DropdownMenuItem(



                        text = { Text("Settings", color = myplayerSec) },



                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = myplayerSec) },



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



                containerColor = if (isDarkPlayer) MyPlayerBlack else PureWhite,



                contentColor = onBg



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



                                color = onBg,



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis



                            )



                            Text(



                                text = artist ?: "",



                                fontSize = 13.sp,



                                color = myplayerSec,



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







                // ── STYLE 2: MYPLAYER DEFAULT STYLE (01:10 - 01:13, 01:39) ─────────────



        PlayerStyleMode.DEFAULT -> {



            Box(



                modifier = Modifier



                    .fillMaxSize()



                    .background(Color(if (isDarkPlayer) 0xFF08141E else 0xFFF4F6F9))



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



                                        Color(if (isDarkPlayer) 0xFF08141E else 0xFFF4F6F9)



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



                    MyPlayerCircleIconButton(icon = Icons.Default.KeyboardArrowDown, onClick = onBackClick)



                    Box(



                        modifier = Modifier



                            .width(44.dp)



                            .height(4.dp)



                            .clip(CircleShape)



                            .background(onBg.copy(alpha = 0.45f))



                    )



                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {



                        MyPlayerCircleIconButton(



                            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,



                            onClick = { if (isDownloaded) showRemoveConfirm = true else onDownloadClick() }



                        )



                        MyPlayerCircleIconButton(



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



                                    color = onBg,



                                    maxLines = 1,



                                    overflow = TextOverflow.Ellipsis,



                                    modifier = Modifier.weight(1f, fill = false)



                                )



                                if (myplayerState.audioFormatBadge) {



                                    Spacer(Modifier.width(8.dp))



                                    Box(modifier = Modifier.clickable { showQualityDetails = true }) {



                                        MyPlayerFormatBadge(audioQuality.format.ifBlank { "AAC" })



                                    }



                                }



                            }



                            Spacer(Modifier.height(2.dp))



                            Text(



                                text = artist ?: "",



                                fontSize = 15.sp,



                                color = onBg.copy(alpha = 0.72f),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis



                            )



                        }







                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



                            MyPlayerCircleIconButton(



                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,



                                onClick = onToggleFavorite



                            )



                            // Opens Lyrics Sheet (NOT Style Picker!)



                            MyPlayerCircleIconButton(



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



                            thumbColor = onBg,



                            activeTrackColor = onBg,



                            inactiveTrackColor = onBg.copy(alpha = 0.22f)



                        )



                    )



                    Row(



                        modifier = Modifier.fillMaxWidth(),



                        horizontalArrangement = Arrangement.SpaceBetween



                    ) {



                        Text(formatMs(positionMs), fontSize = 12.sp, color = onBg.copy(alpha = 0.75f))



                        Text(formatMs(safeDuration), fontSize = 12.sp, color = onBg.copy(alpha = 0.75f))



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



                                tint = if (isShuffleOn) NeonLimePrimary else onBg.copy(alpha = 0.55f)



                            )



                        }



                        IconButton(onClick = onPreviousClick, modifier = Modifier.size(56.dp)) {



                            Icon(Icons.Default.FastRewind, null, tint = onBg, modifier = Modifier.size(38.dp))



                        }



                        IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(68.dp)) {



                            Icon(



                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,



                                null,



                                tint = onBg,



                                modifier = Modifier.size(48.dp)



                            )



                        }



                        IconButton(onClick = onNextClick, modifier = Modifier.size(56.dp)) {



                            Icon(Icons.Default.FastForward, null, tint = onBg, modifier = Modifier.size(38.dp))



                        }



                        IconButton(onClick = onCycleRepeatMode) {



                            Icon(



                                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,



                                null,



                                tint = if (repeatMode != 0) NeonLimePrimary else onBg.copy(alpha = 0.55f)



                            )



                        }



                    }







                    Spacer(Modifier.height(14.dp))







                    // Bottom Frosted Lyrics Pill — shows live synced lyric line & opens Lyrics Sheet on tap!



                    Surface(



                        shape = RoundedCornerShape(24.dp),



                        color = onBg.copy(alpha = 0.10f),



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



                                color = onBg.copy(alpha = 0.85f),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis,



                                modifier = Modifier.weight(1f)



                            )



                            Icon(



                                Icons.Default.KeyboardArrowUp,



                                contentDescription = "Expand Lyrics",



                                tint = onBg.copy(alpha = 0.7f)



                            )



                        }



                    }



                }



            }



        }







        // ── STYLE 3: MYPLAYER TURNTABLE STYLE (FIXED CENTER ALIGNMENT & LYRICS) ─



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



                            listOf(



                                if (isDarkPlayer) Color(0xFF0A141D) else Color(0xFFF8F9FB),



                                if (isDarkPlayer) Color(0xFF071A24) else Color(0xFFEFF2F6),



                                if (isDarkPlayer) Color(0xFF050A0E) else Color(0xFFE4E9EF)



                            )



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



                    MyPlayerCircleIconButton(



                        icon = Icons.Default.KeyboardArrowDown,



                        onClick = onBackClick



                    )



                    MyPlayerCircleIconButton(



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



                            .size(376.dp)



                            .offset(x = (-48).dp, y = 34.dp),



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



                                .size(216.dp)



                                .rotate(effectiveAngle)



                                .clip(CircleShape)



                                .border(4.dp, Color(0xFF1A1A1F), CircleShape),



                            contentAlignment = Alignment.Center



                        ) {



                            AlbumArtImage(



                                uri = artUri ?: "",



                                title = title,



                                artist = artist,



                                size = 216.dp,



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







                // Bottom Track Info + Horizontal Controls + Seekbar + Lyrics Pill



                Column(



                    modifier = Modifier



                        .align(Alignment.BottomStart)



                        .fillMaxWidth()



                        .padding(horizontal = 24.dp, vertical = 14.dp)



                ) {



                    // Song Title & Artist



                    Row(



                        verticalAlignment = Alignment.CenterVertically,



                        horizontalArrangement = Arrangement.spacedBy(12.dp),



                        modifier = Modifier.fillMaxWidth()



                    ) {



                        // Opens Lyrics Sheet (NOT Style Picker!)



                        MyPlayerCircleIconButton(



                            icon = Icons.Default.KeyboardArrowUp,



                            onClick = { showLyricsBottomSheet = true }



                        )



                        Column(modifier = Modifier.weight(1f)) {



                            Row(verticalAlignment = Alignment.CenterVertically) {



                                Text(



                                    text = title ?: "Khat",



                                    fontSize = 22.sp,



                                    fontWeight = FontWeight.ExtraBold,



                                    color = onBg,



                                    maxLines = 1,



                                    overflow = TextOverflow.Ellipsis,



                                    modifier = Modifier.weight(1f, fill = false)



                                )



                                Spacer(Modifier.width(8.dp))



                                Box(modifier = Modifier.clickable { showQualityDetails = true }) {



                                    MyPlayerFormatBadge(audioQuality.format.ifBlank { "AAC" })



                                }



                            }



                            Text(



                                text = artist ?: "Navjot Ahuja",



                                fontSize = 14.sp,



                                color = onBg.copy(alpha = 0.72f),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis



                            )



                        }



                    }







                    Spacer(Modifier.height(10.dp))







                    // Horizontal Playback Controls (Prev, Play/Pause, Next, Like) between Song Name & Seekbar



                    Row(



                        modifier = Modifier.fillMaxWidth(),



                        horizontalArrangement = Arrangement.SpaceEvenly,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        MyPlayerPillControlButton(



                            icon = Icons.Default.FastRewind,



                            onClick = onPreviousClick



                        )



                        MyPlayerPillControlButton(



                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,



                            onClick = onPlayPauseClick



                        )



                        MyPlayerPillControlButton(



                            icon = Icons.Default.FastForward,



                            onClick = onNextClick



                        )



                        MyPlayerPillControlButton(



                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,



                            onClick = onToggleFavorite,



                            tint = if (isFavorite) Color(0xFFFF3B30) else onBg



                        )



                    }







                    Spacer(Modifier.height(8.dp))







                    // Seekbar / Slider



                    Slider(



                        value = progress,



                        onValueChange = { onSeek((it * safeDuration).toLong()) },



                        colors = SliderDefaults.colors(



                            thumbColor = onBg,



                            activeTrackColor = onBg,



                            inactiveTrackColor = onBg.copy(alpha = 0.20f)



                        ),



                        modifier = Modifier.fillMaxWidth()



                    )







                    Row(



                        modifier = Modifier



                            .fillMaxWidth()



                            .padding(horizontal = 4.dp),



                        horizontalArrangement = Arrangement.SpaceBetween



                    ) {



                        Text(



                            text = formatMs(positionMs),



                            fontSize = 12.sp,



                            color = onBg.copy(alpha = 0.6f)



                        )



                        Text(



                            text = formatMs(durationMs),



                            fontSize = 12.sp,



                            color = onBg.copy(alpha = 0.6f)



                        )



                    }







                    Spacer(Modifier.height(6.dp))







                    // Bottom Lyrics Pill — shows live synced lyric line & opens Lyrics Sheet on tap!



                    Surface(



                        shape = RoundedCornerShape(24.dp),



                        color = onBg.copy(alpha = 0.08f),



                        border = BorderStroke(1.dp, onBg.copy(alpha = 0.1f)),



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



                                color = onBg.copy(alpha = 0.85f),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis,



                                modifier = Modifier.weight(1f)



                            )



                            Icon(



                                Icons.Default.Lyrics,



                                contentDescription = "Lyrics",



                                tint = onBg.copy(alpha = 0.65f),



                                modifier = Modifier.size(18.dp)



                            )



                        }



                    }



                }



            }



        }







        // ── STYLE 4: MYPLAYER POSTER STYLE (01:43 - 01:45, FIXED BOUNDS) ────────



        PlayerStyleMode.POSTER -> {



            Box(



                modifier = Modifier



                    .fillMaxSize()



                    .background(if (isDarkPlayer) Color(0xFF06131C) else Color(0xFFF5F7FA))



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



                    MyPlayerCircleIconButton(



                        icon = Icons.Default.Lyrics,



                        onClick = { showLyricsBottomSheet = true }



                    )



                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {



                        MyPlayerCircleIconButton(



                            icon = Icons.Default.MoreVert,



                            onClick = { showOptionsMenu = true }



                        )



                        MyPlayerCircleIconButton(



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



                        color = onBg,



                        maxLines = 2,



                        overflow = TextOverflow.Ellipsis



                    )



                    Spacer(Modifier.height(4.dp))



                    Text(



                        text = artist ?: "Navjot Ahuja",



                        fontSize = 15.sp,



                        color = onBg.copy(alpha = 0.72f),



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis



                    )



                    Spacer(Modifier.height(10.dp))



                    Slider(



                        value = progress,



                        onValueChange = { onSeek((it * safeDuration).toLong()) },



                        colors = SliderDefaults.colors(



                            thumbColor = onBg,



                            activeTrackColor = onBg,



                            inactiveTrackColor = onBg.copy(alpha = 0.22f)



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



                    MyPlayerPillControlButton(



                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,



                        onClick = onToggleFavorite



                    )



                    MyPlayerPillControlButton(icon = Icons.Default.FastRewind, onClick = onPreviousClick)



                    MyPlayerPillControlButton(



                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,



                        onClick = onPlayPauseClick



                    )



                    MyPlayerPillControlButton(icon = Icons.Default.FastForward, onClick = onNextClick)



                }



            }



        }







        // ── STYLE 5 & 6: MYPLAYER TIMELINE & LYRICS STAGE (01:46 - 01:49) ──────



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



                        .background(if (isDarkPlayer) Color(0xFF536878) else Color(0xFFD5DEE6))



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



                                color = onBg.copy(alpha = 0.65f),



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



                                    color = onBg.copy(alpha = 0.85f)



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



                                Text(title ?: "Khat", fontWeight = FontWeight.Bold, color = onBg, maxLines = 1)



                                Text(artist ?: "Navjot Ahuja", fontSize = 12.sp, color = onBg.copy(alpha = 0.75f), maxLines = 1)



                            }



                        }



                    }







                    Row(



                        modifier = Modifier.align(Alignment.TopEnd),



                        horizontalArrangement = Arrangement.spacedBy(8.dp)



                    ) {



                        MyPlayerCircleIconButton(



                            icon = Icons.Default.MoreVert,



                            onClick = { showOptionsMenu = true }



                        )



                        MyPlayerCircleIconButton(



                            icon = Icons.Default.KeyboardArrowDown,



                            onClick = onBackClick



                        )



                    }







                    Text(



                        text = formatMs(positionMs),



                        fontSize = 64.sp,



                        fontWeight = FontWeight.ExtraBold,



                        color = onBg,



                        modifier = Modifier.align(Alignment.BottomStart)



                    )



                }







                Box(



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(0.48f)



                        .background(if (isDarkPlayer) Color(0xFF34434E) else Color(0xFFE7EDF3))



                        .padding(24.dp)



                ) {



                    Text(



                        text = formatMs(safeDuration),



                        fontSize = 64.sp,



                        fontWeight = FontWeight.ExtraBold,



                        color = onBg.copy(alpha = 0.55f),



                        modifier = Modifier.align(Alignment.TopStart)



                    )







                    Slider(



                        value = progress,



                        onValueChange = { onSeek((it * safeDuration).toLong()) },



                        colors = SliderDefaults.colors(



                            thumbColor = onBg,



                            activeTrackColor = onBg,



                            inactiveTrackColor = onBg.copy(alpha = 0.25f)



                        ),



                        modifier = Modifier



                            .fillMaxWidth()



                            .align(Alignment.Center)



                    )







                    Row(



                        modifier = Modifier.align(Alignment.BottomCenter),



                        horizontalArrangement = Arrangement.spacedBy(12.dp)



                    ) {



                        MyPlayerCircleIconButton(icon = Icons.Default.FastRewind, onClick = onPreviousClick)



                        MyPlayerCircleIconButton(



                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,



                            onClick = onPlayPauseClick



                        )



                        MyPlayerCircleIconButton(icon = Icons.Default.FastForward, onClick = onNextClick)



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



fun MyPlayerPlayerStylesPickerScreen(



    myplayerState: MyPlayerUiStateHolder,



    previewTitle: String = "Khat",



    previewArtist: String = "Navjot Ahuja",



    previewArtUri: String? = null,



    onBack: () -> Unit



) {



    val styles = PlayerStyleMode.entries



    val initialPage = styles.indexOf(myplayerState.playerStyle).coerceAtLeast(0)



    val pagerState = rememberPagerState(initialPage = initialPage) { styles.size }



    val currentHighlighted = styles.getOrElse(pagerState.currentPage.coerceIn(0, styles.lastIndex)) { styles.first() }







    Column(



        modifier = Modifier



            .fillMaxSize()



            .background(MyPlayerBlack)



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



                    .background(MyPlayerSurface)



            ) {



                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)



            }







            Column(



                modifier = Modifier.align(Alignment.Center),



                horizontalAlignment = Alignment.CenterHorizontally



            ) {



                Text("Styles", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)



                Text(currentHighlighted.title, fontSize = 13.sp, color = MyPlayerTextSecondary)



            }



        }







        HorizontalPager(



            state = pagerState,



            contentPadding = PaddingValues(horizontal = 36.dp),



            pageSpacing = 16.dp,



            modifier = Modifier.weight(1f)



        ) { page ->



            val style = styles[page]



            val isSelected = myplayerState.playerStyle == style



            Surface(



                shape = RoundedCornerShape(32.dp),



                border = BorderStroke(



                    width = if (isSelected) 2.5.dp else 1.dp,



                    color = if (isSelected) NeonLimePrimary else MyPlayerCardBorder



                ),



                modifier = Modifier



                    .fillMaxSize()



                    .clip(RoundedCornerShape(32.dp))



            ) {



                val dummyPos = remember { mutableLongStateOf(36_000L) }



                MyPlayerMultiStyleNowPlayingScreen(



                    myplayerState = myplayerState,



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



            MyPlayerSecondaryPillButton(



                text = "Cancel",



                modifier = Modifier.weight(1f),



                onClick = onBack



            )



            MyPlayerPrimaryPillButton(



                text = if (myplayerState.playerStyle == currentHighlighted) "Active ✓" else "Set Style",



                modifier = Modifier.weight(1f),



                onClick = {



                    val targetStyle = styles.getOrElse(pagerState.currentPage.coerceIn(0, styles.lastIndex)) { currentHighlighted }



                    runCatching { myplayerState.updatePlayerStyle(targetStyle) }



                    onBack()



                }



            )



        }



    }



}







// ═════════════════════════════════════════════════════════════════════════════



// 5. MYPLAYER SEARCH SCREEN: MOODS & MOMENTS + GENRES + TOP RESULT (00:46 - 01:09)



// ═════════════════════════════════════════════════════════════════════════════







data class MyPlayerMoodGenreItem(



    val title: String,



    val bgColor: Color,



    val coverUrl1: String = "",



    val coverUrl2: String = ""



)







val MYPLAYER_MOODS = listOf(



    MyPlayerMoodGenreItem(



        title = "Chill",



        bgColor = Color(0xFF425468),



        coverUrl1 = "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Commute",



        bgColor = Color(0xFF6B653B),



        coverUrl1 = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/kTJczUoc26U/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Diwali",



        bgColor = Color(0xFF684738),



        coverUrl1 = "https://i.ytimg.com/vi/BddP6PYo2gs/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/RLzC55ai0eo/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Energize",



        bgColor = Color(0xFF3E6340),



        coverUrl1 = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/7wtfhZwyrcc/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Feel good",



        bgColor = Color(0xFF3C5E3E),



        coverUrl1 = "https://i.ytimg.com/vi/ZbZSe6N_BXs/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Focus",



        bgColor = Color(0xFF3B3B3F),



        coverUrl1 = "https://i.ytimg.com/vi/DWcJFNfaw9c/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/lTRiuFIWV54/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Gaming",



        bgColor = Color(0xFF3A4968),



        coverUrl1 = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/m7Bc3pLyij0/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Party",



        bgColor = Color(0xFF526638),



        coverUrl1 = "https://i.ytimg.com/vi/IcrbM1l_BoI/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Romance",



        bgColor = Color(0xFF5E2F36),



        coverUrl1 = "https://i.ytimg.com/vi/Umqb9KENgmk/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/IJq0yyWug1k/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Sad",



        bgColor = Color(0xFF3D385B),



        coverUrl1 = "https://i.ytimg.com/vi/hLQl3WQQoQ0/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Sleep",



        bgColor = Color(0xFF46365E),



        coverUrl1 = "https://i.ytimg.com/vi/1ZYbU82GVz4/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/2OEL4P1Rz04/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Workout",



        bgColor = Color(0xFF664938),



        coverUrl1 = "https://i.ytimg.com/vi/btPJPFnesV4/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/uelHwf8o7_U/hqdefault.jpg"



    )



)







val MYPLAYER_GENRES = listOf(



    MyPlayerMoodGenreItem(



        title = "African",



        bgColor = Color(0xFF3B663E),



        coverUrl1 = "https://i.ytimg.com/vi/WcIcVapfqXw/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/ye9J4aG78J0/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Arabic",



        bgColor = Color(0xFF684738),



        coverUrl1 = "https://i.ytimg.com/vi/N7SLI2ab3F8/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/SmM0653YvXU/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Bengali",



        bgColor = Color(0xFF385E3D),



        coverUrl1 = "https://i.ytimg.com/vi/qfdShSZZxlg/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/6wNFJIbTxNk/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Bhojpuri",



        bgColor = Color(0xFF593866),



        coverUrl1 = "https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Carnatic classical",



        bgColor = Color(0xFF3B5A68),



        coverUrl1 = "https://i.ytimg.com/vi/YR12Z8f1Dh8/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/jADTdg-o8i0/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Classical",



        bgColor = Color(0xFF424248),



        coverUrl1 = "https://i.ytimg.com/vi/4Tr0otuiQuU/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/GRxofEmo3HA/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Country & Americana",



        bgColor = Color(0xFF4A6638),



        coverUrl1 = "https://i.ytimg.com/vi/1vrEljMfXYo/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/bg1sT4ILG0w/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Dance & electronic",



        bgColor = Color(0xFF385E56),



        coverUrl1 = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/IcrbM1l_BoI/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Desi hip-hop",



        bgColor = Color(0xFF5D3A3A),



        coverUrl1 = "https://i.ytimg.com/vi/pnMQLrS5s64/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/3tmd-ClpJxA/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Hindi",



        bgColor = Color(0xFF663B48),



        coverUrl1 = "https://i.ytimg.com/vi/RLzC55ai0eo/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/BddP6PYo2gs/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Hip-hop",



        bgColor = Color(0xFF684B38),



        coverUrl1 = "https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/DRS_PpOrUZ4/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Indian Indie",



        bgColor = Color(0xFF563866),



        coverUrl1 = "https://i.ytimg.com/vi/5Eqb_-j3FDA/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/SlPhMPnQ58k/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Indian pop",



        bgColor = Color(0xFF633845),



        coverUrl1 = "https://i.ytimg.com/vi/kffacxfA7G4/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Indie & alternative",



        bgColor = Color(0xFF384D66),



        coverUrl1 = "https://i.ytimg.com/vi/bpOSxM0rNPM/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "J-Pop",



        bgColor = Color(0xFF463866),



        coverUrl1 = "https://i.ytimg.com/vi/ZRtdQ81jPUQ/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/by4SYYWlhEs/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Jazz",



        bgColor = Color(0xFF685838),



        coverUrl1 = "https://i.ytimg.com/vi/vmDDOFXSgAs/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/zqNTltOGh5c/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "K-Pop",



        bgColor = Color(0xFF523866),



        coverUrl1 = "https://i.ytimg.com/vi/gdZLi9oWNZg/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/IHNzOHi8sJs/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Kannada",



        bgColor = Color(0xFF586638),



        coverUrl1 = "https://i.ytimg.com/vi/P2KRKxAb2ek/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/hcMzwMrr1tE/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Latin",



        bgColor = Color(0xFF685F38),



        coverUrl1 = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/OSUXR6sP6H8/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Malayalam",



        bgColor = Color(0xFF38663E),



        coverUrl1 = "https://i.ytimg.com/vi/0G2VxhV_gXM/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/tOM-nWPcR4U/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Marathi",



        bgColor = Color(0xFF385366),



        coverUrl1 = "https://i.ytimg.com/vi/H6be8dM8xWw/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Metal",



        bgColor = Color(0xFF3C3C42),



        coverUrl1 = "https://i.ytimg.com/vi/CD-E-LDc384/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/CSvFpBOe8eY/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Monsoon",



        bgColor = Color(0xFF5D6638),



        coverUrl1 = "https://i.ytimg.com/vi/Umqb9KENgmk/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/IJq0yyWug1k/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Pop",



        bgColor = Color(0xFF5D3866),



        coverUrl1 = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "Punjabi",



        bgColor = Color(0xFF684838),



        coverUrl1 = "https://i.ytimg.com/vi/twCHVhk8iMU/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/6xoB4ZiKKn0/hqdefault.jpg"



    ),



    MyPlayerMoodGenreItem(



        title = "R&B & soul",



        bgColor = Color(0xFF384766),



        coverUrl1 = "https://i.ytimg.com/vi/fHI8X4OXluQ/hqdefault.jpg",



        coverUrl2 = "https://i.ytimg.com/vi/uPD0QOGTmMI/hqdefault.jpg"



    )



)







@Composable



fun MyPlayerMoodGenreCard(



    item: MyPlayerMoodGenreItem,



    onClick: () -> Unit,



    modifier: Modifier = Modifier,



    overrideCoverUrl1: String? = null,



    overrideCoverUrl2: String? = null



) {



    val thumb1 = overrideCoverUrl1?.takeIf { it.isNotBlank() } ?: item.coverUrl1



    val thumb2 = overrideCoverUrl2?.takeIf { it.isNotBlank() } ?: item.coverUrl2







    Box(



        modifier = modifier



            .height(82.dp)



            .clip(RoundedCornerShape(18.dp))



            .background(item.bgColor)



            .clickable(onClick = onClick)



            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp)



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







        // Two Tilted Overlapping Album Thumbnails on the Right (exact match to MyPlayer-Music Search screen)



        Box(



            modifier = Modifier



                .align(Alignment.CenterEnd)



                .size(width = 78.dp, height = 64.dp)



        ) {



            // Back tilted album card (left, rotated -12 deg)



            Box(



                modifier = Modifier



                    .size(46.dp)



                    .align(Alignment.CenterStart)



                    .graphicsLayer {



                        rotationZ = -12f



                        shadowElevation = 8f



                    }



                    .clip(RoundedCornerShape(10.dp))



                    .background(Color.Black.copy(alpha = 0.45f))



                    .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))



            ) {



                if (thumb1.isNotBlank()) {



                    AlbumArtImage(



                        uri = thumb1,



                        title = item.title,



                        artist = null,



                        size = 46.dp,



                        shape = RoundedCornerShape(10.dp),



                        iconSize = 20.dp



                    )



                }



            }







            // Front tilted album card (right, rotated +10 deg)



            Box(



                modifier = Modifier



                    .size(48.dp)



                    .align(Alignment.CenterEnd)



                    .graphicsLayer {



                        rotationZ = 10f



                        shadowElevation = 12f



                    }



                    .clip(RoundedCornerShape(10.dp))



                    .background(Color.Black.copy(alpha = 0.65f))



                    .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))



            ) {



                if (thumb2.isNotBlank()) {



                    AlbumArtImage(



                        uri = thumb2,



                        title = item.title,



                        artist = null,



                        size = 48.dp,



                        shape = RoundedCornerShape(10.dp),



                        iconSize = 20.dp



                    )



                }



            }



        }



    }



}







// ═════════════════════════════════════════════════════════════════════════════



// 6. MYPLAYER AUDIO LAB: 10-BAND GRAPH & SWEEPABLE BANDS + AUTOEQ + AUDIO FX



//    (02:46 - 04:34 IN VIDEO)



// ═════════════════════════════════════════════════════════════════════════════







@Composable



fun MyPlayerEqualizerScreen(



    myplayerState: MyPlayerUiStateHolder,



    onApplyGainsToEngine: (enabled: Boolean, gains10: List<Float>, preampDb: Float) -> Unit,



    onBack: () -> Unit



) {



    var selectedTab by remember { mutableStateOf("Bands") } // Graph | Bands | AutoEQ



    var selectedNodeIdx by remember { mutableIntStateOf(0) }







    Column(



        modifier = Modifier



            .fillMaxSize()



            .background(MyPlayerBlack)



            .statusBarsPadding()



            .verticalScroll(rememberScrollState())



            .padding(horizontal = 20.dp, vertical = 14.dp)



    ) {



        // Header



        Row(verticalAlignment = Alignment.CenterVertically) {



            Surface(



                shape = RoundedCornerShape(20.dp),



                color = MyPlayerSurface,



                modifier = Modifier.clickable(onClick = onBack)



            ) {



                Row(



                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MyPlayerTextPrimary, modifier = Modifier.size(18.dp))



                    Spacer(Modifier.width(6.dp))



                    Text("Audio Lab", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MyPlayerTextPrimary)



                }



            }



        }







        Spacer(Modifier.height(14.dp))



        Text("Equalizer", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MyPlayerTextPrimary)



        Spacer(Modifier.height(14.dp))







        // Master Equalizer On/Off Pill Card (02:49, 03:05)



        Surface(



            shape = RoundedCornerShape(24.dp),



            color = MyPlayerSurface,



            border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                            .background(if (myplayerState.eqEnabled) Color.White else Color.Gray)



                    )



                    Spacer(Modifier.width(14.dp))



                    Column {



                        Text("Equalizer", fontWeight = FontWeight.Bold, color = Color.White)



                        Text(



                            if (myplayerState.eqEnabled) "On • ${myplayerState.eqPreset.label}" else "Off",



                            fontSize = 12.sp,



                            color = MyPlayerTextSecondary



                        )



                    }



                }



                Switch(



                    checked = myplayerState.eqEnabled,



                    onCheckedChange = {



                        myplayerState.eqEnabled = it



                        onApplyGainsToEngine(it, myplayerState.eqBands, myplayerState.preampDb)



                    }



                )



            }



        }







        Spacer(Modifier.height(14.dp))







        // 3-Tab Switcher: Graph | Bands | AutoEQ (02:49 - 03:22)



        Surface(



            shape = RoundedCornerShape(26.dp),



            color = MyPlayerSurface,



            modifier = Modifier.fillMaxWidth()



        ) {



            Row(modifier = Modifier.padding(4.dp)) {



                listOf("Graph", "Bands", "AutoEQ").forEach { tab ->



                    val active = selectedTab == tab



                    Box(



                        modifier = Modifier



                            .weight(1f)



                            .clip(RoundedCornerShape(22.dp))



                            .background(if (active) MyPlayerTextPrimary else Color.Transparent)



                            .clickable { selectedTab = tab }



                            .padding(vertical = 10.dp),



                        contentAlignment = Alignment.Center



                    ) {



                        Text(



                            text = tab,



                            fontWeight = FontWeight.Bold,



                            color = if (active) MyPlayerBlack else MyPlayerTextPrimary



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



                Text("PRESETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {



                    Text(



                        text = "↻ Reset",



                        fontSize = 13.sp,



                        fontWeight = FontWeight.SemiBold,



                        color = Color.White,



                        modifier = Modifier.clickable {



                            myplayerState.eqPreset = MyPlayerEqPreset.FLAT



                            myplayerState.eqBands = MyPlayerEqPreset.FLAT.gains.toList()



                            onApplyGainsToEngine(myplayerState.eqEnabled, myplayerState.eqBands, myplayerState.preampDb)



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



                items(MyPlayerEqPreset.entries) { preset ->



                    val active = myplayerState.eqPreset == preset



                    Surface(



                        shape = RoundedCornerShape(20.dp),



                        color = if (active) Color.White else MyPlayerSurface,



                        modifier = Modifier.clickable {



                            myplayerState.eqPreset = preset



                            myplayerState.eqBands = preset.gains.toList()



                            myplayerState.eqEnabled = true



                            onApplyGainsToEngine(true, myplayerState.eqBands, myplayerState.preampDb)



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



                Text("GRAPH EDITOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                Spacer(Modifier.height(8.dp))



                Surface(



                    shape = RoundedCornerShape(24.dp),



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Column(modifier = Modifier.padding(16.dp)) {



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.SpaceBetween



                        ) {



                            Text("FREQUENCY RESPONSE", fontSize = 11.sp, color = MyPlayerTextSecondary)



                            val nodeFreq = Eq10Frequencies[selectedNodeIdx]



                            val nodeGain = myplayerState.eqBands[selectedNodeIdx]



                            Text(



                                "${nodeFreq}Hz  ${"%.1f".format(nodeGain)} dB",



                                fontSize = 11.sp,



                                fontWeight = FontWeight.Bold,



                                color = EqRainbowColors[selectedNodeIdx]



                            )



                        }



                        Spacer(Modifier.height(10.dp))



                        MyPlayerRainbowEqCanvas(



                            bands = myplayerState.eqBands,



                            onBandChanged = { idx, newDb ->



                                selectedNodeIdx = idx



                                val updated = myplayerState.eqBands.toMutableList()



                                updated[idx] = newDb



                                myplayerState.eqBands = updated



                                myplayerState.eqPreset = MyPlayerEqPreset.CUSTOM



                                myplayerState.eqEnabled = true



                                onApplyGainsToEngine(true, updated, myplayerState.preampDb)



                            }



                        )



                        Spacer(Modifier.height(8.dp))



                        Text(



                            "Drag a node to shape the sound • double-tap to reset",



                            fontSize = 11.sp,



                            color = MyPlayerTextSecondary,



                            modifier = Modifier.align(Alignment.CenterHorizontally)



                        )



                    }



                }



            }







            // ── TAB 2: SWEEPABLE 10-BAND SLIDERS (02:59 - 03:21, 03:28) ────



            "Bands" -> {



                Text("BANDS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                Spacer(Modifier.height(8.dp))



                Surface(



                    shape = RoundedCornerShape(24.dp),



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Column(modifier = Modifier.padding(16.dp)) {



                        MyPlayerSweepable10BandSliders(



                            bands = myplayerState.eqBands,



                            onBandsUpdated = { updated ->



                                myplayerState.eqBands = updated



                                myplayerState.eqPreset = MyPlayerEqPreset.CUSTOM



                                myplayerState.eqEnabled = true



                                onApplyGainsToEngine(true, updated, myplayerState.preampDb)



                            }



                        )



                        Spacer(Modifier.height(10.dp))



                        Text(



                            text = "Sweep across the sliders to draw a curve",



                            fontSize = 11.sp,



                            color = MyPlayerTextSecondary



                        )



                    }



                }







                Spacer(Modifier.height(18.dp))



                Text("OUTPUT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                Spacer(Modifier.height(8.dp))



                Surface(



                    shape = RoundedCornerShape(24.dp),



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Column(modifier = Modifier.padding(18.dp)) {



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.SpaceBetween



                        ) {



                            Text("Preamp", fontWeight = FontWeight.Bold, color = Color.White)



                            Text("${"%.1f".format(myplayerState.preampDb)} dB", color = MyPlayerTextSecondary)



                        }



                        Slider(



                            value = myplayerState.preampDb,



                            onValueChange = {



                                myplayerState.preampDb = it



                                onApplyGainsToEngine(myplayerState.eqEnabled, myplayerState.eqBands, it)



                            },



                            valueRange = -12f..12f



                        )



                        HorizontalDivider(color = MyPlayerCardBorder)



                        Spacer(Modifier.height(10.dp))



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.SpaceBetween,



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Column(modifier = Modifier.weight(1f)) {



                                Text("Auto preamp", fontWeight = FontWeight.Bold, color = Color.White)



                                val maxBoost = (myplayerState.eqBands.maxOrNull() ?: 0f).coerceAtLeast(0f)



                                Text(



                                    "Lowering by ${"%.1f".format(maxBoost)} dB, the limiter catches the rest",



                                    fontSize = 12.sp,



                                    color = MyPlayerTextSecondary



                                )



                            }



                            Switch(



                                checked = myplayerState.autoPreamp,



                                onCheckedChange = { myplayerState.autoPreamp = it }



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



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Column(modifier = Modifier.padding(18.dp)) {



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.SpaceBetween



                        ) {



                            Text("Database", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)



                            Text("6,033 models", fontSize = 12.sp, color = MyPlayerTextSecondary)



                        }



                        Text("AutoEq • measured by reviewers", fontSize = 12.sp, color = MyPlayerTextSecondary)



                        Spacer(Modifier.height(12.dp))



                        OutlinedTextField(



                            value = searchModel,



                            onValueChange = { searchModel = it },



                            singleLine = true,



                            placeholder = { Text("Search model (e.g. HD 600)", color = MyPlayerTextSecondary) },



                            shape = RoundedCornerShape(16.dp),



                            modifier = Modifier.fillMaxWidth()



                        )



                        Spacer(Modifier.height(12.dp))



                        models.filter { it.first.contains(searchModel, ignoreCase = true) }.forEach { (name, source) ->



                            Row(



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .clickable {



                                        myplayerState.selectedAutoEqModel = name



                                        myplayerState.eqBands = listOf(2.5f, 3.0f, 1.5f, -0.5f, -1.5f, 0.5f, 2.5f, 3.5f, 2.0f, 1.0f)



                                        myplayerState.eqEnabled = true



                                        onApplyGainsToEngine(true, myplayerState.eqBands, myplayerState.preampDb)



                                    }



                                    .padding(vertical = 10.dp),



                                verticalAlignment = Alignment.CenterVertically



                            ) {



                                Icon(Icons.Default.Headphones, null, tint = MyPlayerTextSecondary)



                                Spacer(Modifier.width(12.dp))



                                Column {



                                    Text(name, fontWeight = FontWeight.Bold, color = Color.White)



                                    Text(source, fontSize = 12.sp, color = MyPlayerTextSecondary)



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



fun MyPlayerSweepable10BandSliders(



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



                    color = if (abs(db) > 0.1f) Color.White else MyPlayerTextSecondary,



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



                    color = MyPlayerTextSecondary,



                    modifier = Modifier.width(28.dp),



                    textAlign = TextAlign.Center



                )



            }



        }



    }



}







@Composable



fun MyPlayerRainbowEqCanvas(



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



// 7. SHARED MYPLAYER UI ATOMS (PILL BUTTONS, BADGES, CIRCLE CONTROLS)



// ═════════════════════════════════════════════════════════════════════════════







@Composable



fun MyPlayerPrimaryPillButton(



    text: String,



    enabled: Boolean = true,



    modifier: Modifier = Modifier.fillMaxWidth(),



    onClick: () -> Unit



) {



    val isDark = ThemeState.isDarkMode



    Button(



        onClick = onClick,



        enabled = enabled,



        shape = RoundedCornerShape(28.dp),



        colors = ButtonDefaults.buttonColors(



            containerColor = if (isDark) Color(0xFF3A3A42) else Color(0xFF1E1E24),



            contentColor = Color.White,



            disabledContainerColor = if (isDark) Color(0xFF222228) else Color(0xFFE4E4EB),



            disabledContentColor = MyPlayerTextSecondary



        ),



        modifier = modifier.height(56.dp)



    ) {



        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)



    }



}







@Composable



fun MyPlayerSecondaryPillButton(



    text: String,



    modifier: Modifier = Modifier.fillMaxWidth(),



    onClick: () -> Unit



) {



    val isDark = ThemeState.isDarkMode



    Button(



        onClick = onClick,



        shape = RoundedCornerShape(28.dp),



        colors = ButtonDefaults.buttonColors(



            containerColor = if (isDark) Color(0xFF1B1B20) else Color(0xFFEAEBED),



            contentColor = if (isDark) Color.White else Color(0xFF17171C)



        ),



        modifier = modifier.height(56.dp)



    ) {



        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)



    }



}







@Composable



fun MyPlayerFormatBadge(formatText: String) {



    val isDark = ThemeState.isDarkMode



    Surface(



        shape = RoundedCornerShape(6.dp),



        color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)



    ) {



        Text(



            text = formatText.uppercase(),



            fontSize = 10.sp,



            fontWeight = FontWeight.ExtraBold,



            color = if (isDark) Color.White else CharcoalPrimary,



            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)



        )



    }



}







@Composable



fun MyPlayerCircleIconButton(



    icon: ImageVector,



    onClick: () -> Unit,



    modifier: Modifier = Modifier,



    tint: Color = if (ThemeState.isDarkMode) Color.White else CharcoalPrimary,



    circleBg: Color = if (ThemeState.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)



) {



    Box(



        modifier = modifier



            .size(42.dp)



            .clip(CircleShape)



            .background(circleBg)



            .clickable(onClick = onClick),



        contentAlignment = Alignment.Center



    ) {



        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))



    }



}







@Composable



fun MyPlayerPillControlButton(



    icon: ImageVector,



    onClick: () -> Unit,



    tint: Color = Color.Unspecified



) {



    val isDark = ThemeState.isDarkMode



    val resolvedTint = if (tint == Color.Unspecified) {



        if (isDark) Color.White else CharcoalPrimary



    } else tint



    Box(



        modifier = Modifier



            .size(width = 58.dp, height = 44.dp)



            .clip(RoundedCornerShape(22.dp))



            .background(if (isDark) Color(0xFF18222C).copy(alpha = 0.88f) else PureWhite.copy(alpha = 0.92f))



            .border(



                1.dp,



                if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),



                RoundedCornerShape(22.dp)



            )



            .clickable(onClick = onClick),



        contentAlignment = Alignment.Center



    ) {



        Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(22.dp))



    }



}







private fun formatMs(ms: Long): String {



    val totalSec = (ms / 1000L).coerceAtLeast(0L)



    return "%d:%02d".format(totalSec / 60L, totalSec % 60L)



}







// ═════════════════════════════════════════════════════════════════════════════



// 8. MYPLAYER HOME SCREEN + HERO NOW-PLAYING BANNER + PROFILE SHEET (00:40-00:45, 01:23, 04:45-04:50)



// ═════════════════════════════════════════════════════════════════════════════







@Composable



fun MyPlayerProfileSheetDialog(



    myplayerState: MyPlayerUiStateHolder,



    totalListeningHours: Int = 0,



    totalListeningMinutes: Int = 0,



    onDismiss: () -> Unit,



    onOpenSettings: () -> Unit



) {



    var editingName by remember { mutableStateOf(false) }



    var tempName by remember { mutableStateOf(myplayerState.username) }







    Dialog(



        onDismissRequest = onDismiss,



        properties = DialogProperties(usePlatformDefaultWidth = false)



    ) {



        Surface(



            modifier = Modifier



                .fillMaxWidth(0.94f)



                .wrapContentHeight(),



            shape = RoundedCornerShape(28.dp),



            color = MyPlayerBlack,



            border = BorderStroke(1.dp, MyPlayerCardBorder)



        ) {



            Column(modifier = Modifier.padding(20.dp)) {



                Row(



                    verticalAlignment = Alignment.CenterVertically,



                    horizontalArrangement = Arrangement.spacedBy(10.dp)



                ) {



                    MyPlayerPlayLogo(modifier = Modifier.size(28.dp))



                    Text(



                        text = "Hello again, ${myplayerState.username}!",



                        fontSize = 20.sp,



                        fontWeight = FontWeight.ExtraBold,



                        color = Color.White



                    )



                }







                Spacer(Modifier.height(16.dp))







                MyPlayerProfileIdCard(



                    username = myplayerState.username,



                    profileId = myplayerState.profileId,



                    joinedDate = myplayerState.joinedDate,



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



                        val scope = rememberCoroutineScope()



                        val context = LocalContext.current



                        Button(



                            onClick = {



                                val trimmed = tempName.trim()



                                myplayerState.completeOnboarding(trimmed, myplayerState.pickedArtists)



                                scope.launch {



                                    runCatching {



                                        com.myplayer.app.data.local.datastore.SettingsDataStore(context).setUserName(trimmed)



                                    }



                                }



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



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                                Text("h  ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                                Text("|  ", fontSize = 26.sp, color = MyPlayerCardBorder)



                                Text("$totalListeningMinutes", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)



                                Text("m", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                            }



                            Text("Total Listening\ntime", fontSize = 12.sp, color = MyPlayerTextSecondary, textAlign = TextAlign.End)



                        }



                        Spacer(Modifier.height(8.dp))



                        Text("View Stats >", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MyPlayerTextSecondary)



                    }



                }







                Spacer(Modifier.height(12.dp))







                // Version & About App Card ("8.2.3" - 01:24)



                Surface(



                    shape = RoundedCornerShape(22.dp),



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Row(



                        modifier = Modifier.padding(18.dp).fillMaxWidth(),



                        horizontalArrangement = Arrangement.SpaceBetween,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        Column {



                            Text("Version & about app", fontSize = 12.sp, color = MyPlayerTextSecondary)



                            Text("8.2.3", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)



                        }



                        val context = LocalContext.current



                        Surface(



                            shape = RoundedCornerShape(18.dp),



                            color = MyPlayerSurfaceHigh,



                            modifier = Modifier.clickable {



                                runCatching {



                                    val intent = android.content.Intent(



                                        android.content.Intent.ACTION_VIEW,



                                        android.net.Uri.parse(com.myplayer.app.data.update.UpdateChecker.UPDATE_WEBSITE_URL)



                                    ).apply {



                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)



                                    }



                                    context.startActivity(intent)



                                }



                            }



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



                Text("Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                Spacer(Modifier.height(8.dp))







                Surface(



                    shape = RoundedCornerShape(20.dp),



                    color = MyPlayerSurface,



                    border = BorderStroke(1.dp, MyPlayerCardBorder),



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



fun MyPlayerHeroNowPlayingBanner(



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



            Text("Nothing here yet", fontSize = 13.sp, color = MyPlayerTextSecondary)



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



                                MyPlayerBlack



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



// 9. MYPLAYER LIBRARY SCREEN (ALL | LOCAL | CACHED | IMPORTED - 00:51 - 00:59)



// ═════════════════════════════════════════════════════════════════════════════







@Composable



fun MyPlayerLibraryHeaderAndTabs(



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



                    color = MyPlayerTextSecondary



                )



            },



            shape = RoundedCornerShape(24.dp),



            modifier = Modifier.fillMaxWidth(),



            colors = OutlinedTextFieldDefaults.colors(



                focusedContainerColor = MyPlayerSurface,



                unfocusedContainerColor = MyPlayerSurface,



                focusedBorderColor = MyPlayerCardBorder,



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



                    color = if (active) Color.White else MyPlayerSurface,



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



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



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



            Text("ALL MUSIC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



            Spacer(Modifier.height(8.dp))



            MyPlayerPlatformCountGrid(spotifyCount = spotifyTrackCount, youtubeCount = youtubeTrackCount)







            Spacer(Modifier.height(18.dp))



            Text("PLAYLISTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



            Spacer(Modifier.height(8.dp))



            MyPlayerPlatformCountGrid(spotifyCount = 0, youtubeCount = 0)



        }



    }



}







@Composable



fun MyPlayerPlatformCountGrid(spotifyCount: Int, youtubeCount: Int) {



    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {



        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



            MyPlayerPlatformStatCard("Spotify", spotifyCount, Color(0xFF1DB954), Modifier.weight(1f))



            MyPlayerPlatformStatCard("YouTube", youtubeCount, Color(0xFFFF0033), Modifier.weight(1f))



        }



        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



            MyPlayerPlatformStatCard("Apple Music", 0, Color(0xFFFA2D48), Modifier.weight(1f))



            MyPlayerPlatformStatCard("Tidal", 0, Color.White, Modifier.weight(1f))



        }



    }



}







@Composable



private fun MyPlayerPlatformStatCard(name: String, count: Int, dotColor: Color, modifier: Modifier = Modifier) {



    Surface(



        shape = RoundedCornerShape(20.dp),



        color = MyPlayerSurface,



        border = BorderStroke(1.dp, MyPlayerCardBorder),



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



// 10. MYPLAYER SETTINGS HUB, APPEARANCE, AUDIO LAB & AUDIO FX (01:26 - 04:44)



// ═════════════════════════════════════════════════════════════════════════════







@Composable



fun MyPlayerSettingsHubScreen(



    myplayerState: MyPlayerUiStateHolder,



    onApplyEqToEngine: (enabled: Boolean, gains10: List<Float>, preampDb: Float) -> Unit = { _, _, _ -> },



    onBack: () -> Unit



) {



    // Automatically open the Interactive Feature Tour if not completed yet (first Settings visit).



    var subScreen by remember { mutableStateOf(if (!myplayerState.featureTourDone) "feature_tour" else "root") }



    var searchQuery by remember { mutableStateOf("") }







    when (subScreen) {



        "feature_tour" -> {



            MyPlayerInteractiveFeatureTour(



                myplayerState = myplayerState,



                onApplyEqToEngine = onApplyEqToEngine,



                onFinishTour = {



                    myplayerState.completeFeatureTour()



                    subScreen = "root"



                }



            )



        }



        "styles_picker" -> {



            MyPlayerPlayerStylesPickerScreen(



                myplayerState = myplayerState,



                onBack = { subScreen = "appearance" }



            )



        }



        "equalizer" -> {



            MyPlayerEqualizerScreen(



                myplayerState = myplayerState,



                onApplyGainsToEngine = onApplyEqToEngine,



                onBack = { subScreen = "audio_lab" }



            )



        }



        "appearance" -> {



            MyPlayerAppearanceSettingsScreen(



                myplayerState = myplayerState,



                onOpenStylesPicker = { subScreen = "styles_picker" },



                onBack = { subScreen = "root" }



            )



        }



        "audio_lab" -> {



            MyPlayerAudioLabHubScreen(



                myplayerState = myplayerState,



                onOpenEqualizer = { subScreen = "equalizer" },



                onBack = { subScreen = "root" }



            )



        }



        else -> {



            // Root Settings Menu (01:26, 04:35)



            LazyColumn(



                modifier = Modifier



                    .fillMaxSize()



                    .background(MyPlayerBlack)



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



                        MyPlayerPlayLogo(modifier = Modifier.size(32.dp))



                        MyPlayerCircleIconButton(



                            icon = Icons.Default.PersonOutline,



                            onClick = onBack,



                            tint = Color.White,



                            circleBg = Color.White.copy(alpha = 0.12f)



                        )



                    }



                    Spacer(Modifier.height(14.dp))



                    OutlinedTextField(



                        value = searchQuery,



                        onValueChange = { searchQuery = it },



                        singleLine = true,



                        placeholder = { Text("Search any settings", color = MyPlayerTextSecondary) },



                        shape = RoundedCornerShape(24.dp),



                        modifier = Modifier.fillMaxWidth(),



                        colors = OutlinedTextFieldDefaults.colors(



                            focusedContainerColor = MyPlayerSurface,



                            unfocusedContainerColor = MyPlayerSurface,



                            focusedBorderColor = MyPlayerCardBorder,



                            unfocusedBorderColor = Color.Transparent,



                            focusedTextColor = Color.White,



                            unfocusedTextColor = Color.White



                        )



                    )



                }







                // Interactive Feature Tour Card (replay the guided tour anytime!)



                item {



                    Surface(



                        shape = RoundedCornerShape(22.dp),



                        color = MyPlayerSurfaceHigh,



                        border = BorderStroke(1.2.dp, MyPlayerAccentGreen.copy(alpha = 0.65f)),



                        modifier = Modifier



                            .fillMaxWidth()



                            .clickable {



                                myplayerState.startFeatureTour()



                                subScreen = "feature_tour"



                            }



                    ) {



                        Row(



                            modifier = Modifier.padding(16.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier



                                    .size(44.dp)



                                    .clip(CircleShape)



                                    .background(MyPlayerAccentGreen.copy(alpha = 0.18f)),



                                contentAlignment = Alignment.Center



                            ) {



                                Icon(Icons.Default.Explore, null, tint = MyPlayerAccentGreen)



                            }



                            Spacer(Modifier.width(14.dp))



                            Column(modifier = Modifier.weight(1f)) {



                                Text("Interactive Feature Tour", fontWeight = FontWeight.ExtraBold, color = Color.White)



                                Text(



                                    "Step-by-step guided tour: Custom Player Styles, 10-Band EQ & Audio FX",



                                    fontSize = 12.sp,



                                    color = MyPlayerAccentGreen



                                )



                            }



                        }



                    }



                }







                // Recommended Item Card ("Configure Equalizer settings" - 01:34, 04:43)



                item {



                    Text("Recommended item", fontSize = 12.sp, color = MyPlayerTextSecondary)



                    Spacer(Modifier.height(6.dp))



                    Surface(



                        shape = RoundedCornerShape(22.dp),



                        color = MyPlayerSurface,



                        border = BorderStroke(1.dp, MyPlayerCardBorder),



                        modifier = Modifier



                            .fillMaxWidth()



                            .clickable { subScreen = "equalizer" }



                    ) {



                        Row(



                            modifier = Modifier.padding(16.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MyPlayerSurfaceHigh),



                                contentAlignment = Alignment.Center



                            ) {



                                Icon(Icons.Default.GraphicEq, null, tint = Color.White)



                            }



                            Spacer(Modifier.width(14.dp))



                            Column {



                                Text("Configure Equalizer settings", fontWeight = FontWeight.Bold, color = Color.White)



                                Text("settings>audio lab", fontSize = 12.sp, color = MyPlayerTextSecondary)



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



                        color = MyPlayerSurface,



                        border = BorderStroke(1.dp, MyPlayerCardBorder),



                        modifier = Modifier



                            .fillMaxWidth()



                            .clickable { subScreen = route }



                    ) {



                        Row(



                            modifier = Modifier.padding(16.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier.size(42.dp).clip(CircleShape).background(MyPlayerSurfaceHigh),



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



fun MyPlayerAppearanceSettingsScreen(



    myplayerState: MyPlayerUiStateHolder,



    onOpenStylesPicker: () -> Unit,



    onBack: () -> Unit



) {



    LazyColumn(



        modifier = Modifier



            .fillMaxSize()



            .background(MyPlayerBlack)



            .statusBarsPadding(),



        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),



        verticalArrangement = Arrangement.spacedBy(12.dp)



    ) {



        item {



            IconButton(



                onClick = onBack,



                modifier = Modifier.size(42.dp).clip(CircleShape).background(MyPlayerSurface)



            ) {



                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)



            }



            Spacer(Modifier.height(10.dp))



            Text("Appearance", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)



            Spacer(Modifier.height(10.dp))



            Text("Player appearance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



        }







        // Customize Player Row -> Opens the 6-Style Pager (including Radial MyPlayer!)



        item {



            Surface(



                shape = RoundedCornerShape(22.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                            "Active: ${myplayerState.playerStyle.title} • Tap to switch (Radial, Turntable, Poster...)",



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



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



                modifier = Modifier.fillMaxWidth()



            ) {



                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {



                    MyPlayerSwitchRow(



                        title = "Lyrics Shader",



                        subtitle = "Glow on sung lyrics",



                        checked = myplayerState.lyricsShader,



                        onCheckedChange = { myplayerState.lyricsShader = it }



                    )



                    MyPlayerSwitchRow(



                        title = "High quality bloom",



                        subtitle = "Extra soft bloom pass on lyrics",



                        checked = myplayerState.highQualityBloom,



                        onCheckedChange = { myplayerState.highQualityBloom = it }



                    )



                    MyPlayerSwitchRow(



                        title = "Audio format badge",



                        subtitle = "What the song is playing as, beside title",



                        checked = myplayerState.audioFormatBadge,



                        onCheckedChange = { myplayerState.audioFormatBadge = it }



                    )



                    MyPlayerSwitchRow(



                        title = "Show sample rate",



                        subtitle = "The stream's sample rate after its format",



                        checked = myplayerState.showSampleRate,



                        onCheckedChange = { myplayerState.showSampleRate = it }



                    )



                    MyPlayerSwitchRow(



                        title = "Enable animated canvas",



                        subtitle = "Looping visuals for supported tracks",



                        checked = myplayerState.enableAnimatedCanvas,



                        onCheckedChange = { myplayerState.enableAnimatedCanvas = it }



                    )



                }



            }



        }







        item {



            Text("Global Appearance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



            Spacer(Modifier.height(6.dp))



            Surface(



                shape = RoundedCornerShape(24.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



                modifier = Modifier.fillMaxWidth()



            ) {



                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {



                    MyPlayerSwitchRow(



                        title = "Blur",



                        subtitle = "Glass blur across player and navigation",



                        checked = myplayerState.glassBlur,



                        onCheckedChange = { myplayerState.glassBlur = it }



                    )



                    MyPlayerSwitchRow(



                        title = "Progressive blur",



                        subtitle = "Smooth progressive gradient blur",



                        checked = myplayerState.progressiveBlur,



                        onCheckedChange = { myplayerState.progressiveBlur = it }



                    )



                    Column {



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.SpaceBetween



                        ) {



                            Text("Blur strength", fontWeight = FontWeight.Bold, color = Color.White)



                            Text("${myplayerState.blurStrengthPx.toInt()}px", color = MyPlayerTextSecondary)



                        }



                        Slider(



                            value = myplayerState.blurStrengthPx,



                            onValueChange = { myplayerState.blurStrengthPx = it },



                            valueRange = 0f..50f



                        )



                    }



                    MyPlayerSwitchRow(



                        title = "Animation",



                        subtitle = "Smooth UI transitions and element blurs",



                        checked = myplayerState.smoothAnimations,



                        onCheckedChange = { myplayerState.smoothAnimations = it }



                    )



                }



            }



        }



        item { Spacer(Modifier.height(90.dp)) }



    }



}







// 11. REAL-TIME MYPLAYER AUDIO FX DSP ENGINE & FULL PIXEL-ACCURATE AUDIO FX SCREEN



//     (100% Matching 00:00 - 01:01 of Uploaded Video + MyPlayer-Music DSP Formulas)



// ═════════════════════════════════════════════════════════════════════════════







enum class MyPlayerReverbPreset(



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







enum class MyPlayerLoudnessPreset(val label: String, val boostDb: Float, val compression: Float) {



    GENTLE("Gentle", 2.0f, 0.15f),



    PUNCHY("Punchy", 4.0f, 0.30f),



    NIGHT_MODE("Night mode", 3.0f, 0.85f),



    MAX("Max", 8.0f, 0.50f)



}







@Composable



fun MyPlayerAudioLabHubScreen(



    myplayerState: MyPlayerUiStateHolder,



    onOpenEqualizer: () -> Unit = {},



    onBack: () -> Unit



) {



    MyPlayerAudioFxScreen(



        myplayerState = myplayerState,



        onBack = onBack



    )



}







/**



 * COMPLETE MYPLAYER AUDIO FX SCREEN (00:00 - 01:01 in Uploaded Video)



 * Every single card, animated visualizer, preset chip, and fine-tune slider is wired



 * to `MyPlayerAudioFxDspBridge` for immediate real-time audio transformation!



 */



@Composable



fun MyPlayerAudioFxScreen(



    myplayerState: MyPlayerUiStateHolder,



    onBack: () -> Unit



) {



    var selectedReverbPreset by remember { mutableStateOf(MyPlayerReverbPreset.ROOM) }



    var selectedLoudnessPreset by remember { mutableStateOf(MyPlayerLoudnessPreset.PUNCHY) }



    val fxContext = androidx.compose.ui.platform.LocalContext.current.applicationContext



    // feed the audio tap while the FX screen shows its live pictures



    MyPlayerLabListenToAudio()



    var loudnessBoostDb by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.loudnessBoostDb) }



    var loudnessCompression by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.loudnessCompression) }



    var reverbDry by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.reverbDry) }



    var reverbWidth by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.reverbWidth) }



    var reverbPreDelayMs by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.reverbPreDelayMs) }



    var reverbLowCutHz by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.reverbLowCutHz) }



    var speakerAngleDeg by remember { mutableFloatStateOf(MyPlayerAudioFxDspBridge.speakerAngleDeg) }







    // Sync any state change straight into the real-time DSP bridge!



    LaunchedEffect(



        myplayerState.reverbEnabled, myplayerState.reverbMix, myplayerState.reverbRoomSize,



        myplayerState.reverbDamping, myplayerState.reverbWet, reverbDry, reverbWidth,



        reverbPreDelayMs, reverbLowCutHz, myplayerState.loudnessEnabled, loudnessBoostDb,



        loudnessCompression, myplayerState.evenOutSongs, myplayerState.replayGainPreampDb,



        myplayerState.spatialAudioEnabled, myplayerState.spatialMode, myplayerState.headShadowHz,



        myplayerState.crossfeedDb, speakerAngleDeg, myplayerState.spatialAmount,



        myplayerState.monoAudio, myplayerState.dolbyAtmosSurround, myplayerState.playbackSpeed,



        myplayerState.preservePitch, myplayerState.pitchSemitones, myplayerState.exponentialVolume



    ) {



        MyPlayerAudioFxDspBridge.reverbDry = reverbDry



        MyPlayerAudioFxDspBridge.reverbWidth = reverbWidth



        MyPlayerAudioFxDspBridge.reverbPreDelayMs = reverbPreDelayMs



        MyPlayerAudioFxDspBridge.reverbLowCutHz = reverbLowCutHz



        MyPlayerAudioFxDspBridge.loudnessBoostDb = loudnessBoostDb



        MyPlayerAudioFxDspBridge.loudnessCompression = loudnessCompression



        MyPlayerAudioFxDspBridge.speakerAngleDeg = speakerAngleDeg



        MyPlayerAudioFxDspBridge.syncFromUiState(myplayerState, selectedLoudnessPreset)



        MyPlayerAudioFxDspBridge.saveFineTune(fxContext)



    }







    val activeFxNames = remember(



        myplayerState.reverbEnabled, myplayerState.loudnessEnabled,



        myplayerState.spatialAudioEnabled, myplayerState.monoAudio,



        myplayerState.dolbyAtmosSurround, myplayerState.playbackSpeed,



        myplayerState.exponentialVolume



    ) {



        buildList {



            if (myplayerState.loudnessEnabled) add("LOUDNESS")



            if (myplayerState.reverbEnabled) add("REVERB")



            if (myplayerState.spatialAudioEnabled) add("SPATIAL")



            if (myplayerState.dolbyAtmosSurround) add("SURROUND")



            if (myplayerState.monoAudio) add("MONO")



            if (myplayerState.playbackSpeed != 1.0f || !myplayerState.preservePitch) add("SPEED")



            if (myplayerState.exponentialVolume) add("VOLUME")



        }



    }







    LazyColumn(



        modifier = Modifier



            .fillMaxSize()



            .background(MyPlayerBlack)



            .statusBarsPadding(),



        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),



        verticalArrangement = Arrangement.spacedBy(16.dp)



    ) {



        // ── TOP HEADER: "Audio / FX" + Tilted Active FX Card Stack (Screenshot match) ──



        item {



            Row(



                modifier = Modifier



                    .fillMaxWidth()



                    .padding(top = 4.dp),



                horizontalArrangement = Arrangement.SpaceBetween,



                verticalAlignment = Alignment.Top



            ) {



                Column {



                    IconButton(



                        onClick = onBack,



                        modifier = Modifier



                            .size(44.dp)



                            .clip(CircleShape)



                            .background(MyPlayerSurfaceHigh)



                    ) {



                        Icon(



                            Icons.AutoMirrored.Filled.ArrowBack,



                            contentDescription = "Back",



                            tint = MyPlayerTextPrimary,



                            modifier = Modifier.size(20.dp)



                        )



                    }



                    Spacer(Modifier.height(16.dp))



                    Text(



                        text = "Audio",



                        fontSize = 34.sp,



                        fontWeight = FontWeight.Bold,



                        color = MyPlayerTextSecondary



                    )



                    Text(



                        text = "FX",



                        fontSize = 64.sp,



                        lineHeight = 64.sp,



                        fontWeight = FontWeight.Black,



                        color = MyPlayerTextPrimary



                    )



                }







                // Tilted Active FX Card on Top-Right (exact match to screenshot)



                Box(



                    modifier = Modifier



                        .padding(top = 6.dp, end = 4.dp)



                        .graphicsLayer { rotationZ = -7f }



                ) {



                    val activeFxCount = activeFxNames.size



                    val topFxName = activeFxNames.firstOrNull() ?: "SPATIAL"



                    Surface(



                        shape = RoundedCornerShape(22.dp),



                        color = MyPlayerSurface,



                        border = BorderStroke(1.dp, MyPlayerCardBorder),



                        modifier = Modifier.size(width = 138.dp, height = 142.dp)



                    ) {



                        Column(



                            modifier = Modifier.padding(14.dp),



                            verticalArrangement = Arrangement.SpaceBetween



                        ) {



                            Column {



                                Row(



                                    modifier = Modifier.fillMaxWidth(),



                                    horizontalArrangement = Arrangement.SpaceBetween,



                                    verticalAlignment = Alignment.CenterVertically



                                ) {



                                    Text(



                                        text = if (activeFxCount > 0) "ON" else "OFF",



                                        fontSize = 10.sp,



                                        fontWeight = FontWeight.Bold,



                                        color = if (activeFxCount > 0) MyPlayerAccentGreen else MyPlayerTextSecondary



                                    )



                                    Text(



                                        text = "${if (activeFxCount > 0) 1 else 1} of ${activeFxCount.coerceAtLeast(1)}",



                                        fontSize = 10.sp,



                                        fontWeight = FontWeight.Medium,



                                        color = MyPlayerTextSecondary



                                    )



                                }



                                Spacer(Modifier.height(10.dp))



                                Text(



                                    text = topFxName,



                                    fontSize = 17.sp,



                                    fontWeight = FontWeight.ExtraBold,



                                    color = MyPlayerTextPrimary,



                                    maxLines = 1,



                                    overflow = TextOverflow.Ellipsis



                                )



                            }







                            // 12 Spectrum bars matching the screenshot



                            Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {



                                val bars = listOf(0.38f, 0.62f, 0.45f, 0.76f, 0.52f, 0.92f, 0.68f, 0.48f, 0.78f, 0.58f, 0.68f, 0.42f)



                                val barWidth = 2.8.dp.toPx()



                                val totalBarsWidth = bars.size * barWidth



                                val spacing = (size.width - totalBarsWidth) / (bars.size - 1).coerceAtLeast(1)



                                val barColor = if (ThemeState.isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF17171C).copy(alpha = 0.65f)



                                bars.forEachIndexed { i, factor ->



                                    val x = i * (barWidth + spacing) + barWidth / 2f



                                    val h = (size.height * factor).coerceIn(4f, size.height)



                                    drawLine(



                                        color = barColor,



                                        start = Offset(x, size.height),



                                        end = Offset(x, size.height - h),



                                        strokeWidth = barWidth,



                                        cap = StrokeCap.Round



                                    )



                                }



                            }



                        }



                    }



                }



            }



        }







        // ── CARD 1: REVERB (Exact match to screenshot) ───────────────────────



        item {



            Surface(



                shape = RoundedCornerShape(28.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



                modifier = Modifier.fillMaxWidth()



            ) {



                Column(modifier = Modifier.padding(16.dp)) {



                    // Acoustic Reflection Container



                    Box(



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(118.dp)



                            .clip(RoundedCornerShape(18.dp))



                            .background(if (ThemeState.isDarkMode) Color(0xFF09090D) else Color(0xFF121318))



                            .border(1.dp, if (ThemeState.isDarkMode) Color(0xFF181820) else Color(0xFF22232B), RoundedCornerShape(18.dp))



                    ) {



                        MyPlayerReverbPicture(



                            enabled = myplayerState.reverbEnabled,



                            roomSize = myplayerState.reverbRoomSize,



                            damping = myplayerState.reverbDamping,



                            wet = myplayerState.reverbWet,



                            mix = myplayerState.reverbMix



                        )



                    }







                    Spacer(Modifier.height(16.dp))







                    MyPlayerSwitchRow(



                        title = "Reverb",



                        subtitle = "Reverb the music",



                        checked = myplayerState.reverbEnabled,



                        onCheckedChange = { myplayerState.reverbEnabled = it }



                    )







                    AnimatedVisibility(visible = myplayerState.reverbEnabled) {



                        Column {



                            Spacer(Modifier.height(14.dp))



                            // 8 Reverb Presets Row



                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {



                                items(MyPlayerReverbPreset.entries) { preset ->



                                    val selected = selectedReverbPreset == preset



                                    Surface(



                                        shape = RoundedCornerShape(20.dp),



                                        color = if (selected) MyPlayerTextPrimary else MyPlayerSurfaceHigh,



                                        modifier = Modifier.clickable {



                                            selectedReverbPreset = preset



                                            myplayerState.reverbPreset = preset.label



                                            myplayerState.reverbMix = preset.mix



                                            myplayerState.reverbRoomSize = preset.roomSize



                                            myplayerState.reverbDamping = preset.damping



                                            myplayerState.reverbWet = preset.wet



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



                                            color = if (selected) MyPlayerBlack else MyPlayerTextPrimary,



                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)



                                        )



                                    }



                                }



                            }







                            Spacer(Modifier.height(16.dp))







                            MyPlayerLabeledSlider(



                                label = "Mix",



                                valueText = "${(myplayerState.reverbMix * 100).roundToInt()}%",



                                value = myplayerState.reverbMix,



                                onValueChange = { myplayerState.reverbMix = it }



                            )



                            MyPlayerLabeledSlider(



                                label = "Room size",



                                valueText = "${(myplayerState.reverbRoomSize * 100).roundToInt()}%",



                                value = myplayerState.reverbRoomSize,



                                onValueChange = { myplayerState.reverbRoomSize = it }



                            )



                            MyPlayerLabeledSlider(



                                label = "Damping",



                                valueText = "${(myplayerState.reverbDamping * 100).roundToInt()}%",



                                value = myplayerState.reverbDamping,



                                onValueChange = { myplayerState.reverbDamping = it }



                            )



                            MyPlayerLabeledSlider(



                                label = "Wet",



                                valueText = "${(myplayerState.reverbWet * 100).roundToInt()}%",



                                value = myplayerState.reverbWet,



                                onValueChange = { myplayerState.reverbWet = it }



                            )







                            Spacer(Modifier.height(8.dp))



                            Text("FINE TUNE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextSecondary)



                            Spacer(Modifier.height(8.dp))







                            MyPlayerLabeledSlider(



                                label = "Dry",



                                valueText = "${(reverbDry * 100).roundToInt()}%",



                                value = reverbDry,



                                onValueChange = { reverbDry = it }



                            )



                            MyPlayerLabeledSlider(



                                label = "Width",



                                valueText = "${(reverbWidth * 100).roundToInt()}%",



                                value = reverbWidth,



                                onValueChange = { reverbWidth = it }



                            )



                            MyPlayerLabeledSlider(



                                label = "Pre-delay",



                                valueText = "${reverbPreDelayMs.roundToInt()} ms",



                                value = reverbPreDelayMs,



                                valueRange = 0f..100f,



                                onValueChange = { reverbPreDelayMs = it }



                            )



                            MyPlayerLabeledSlider(



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







        // ── CARD 2: LOUDNESS & REPLAYGAIN (Exact match to screenshot) ─────────



        item {



            Surface(



                shape = RoundedCornerShape(28.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



                modifier = Modifier.fillMaxWidth()



            ) {



                Column(modifier = Modifier.padding(16.dp)) {



                    // Twin-Envelope Waveform Compression Visualizer



                    Box(



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(116.dp)



                            .clip(RoundedCornerShape(18.dp))



                            .background(if (ThemeState.isDarkMode) Color(0xFF09090D) else Color(0xFF121318))



                            .border(1.dp, if (ThemeState.isDarkMode) Color(0xFF181820) else Color(0xFF22232B), RoundedCornerShape(18.dp))



                    ) {



                        MyPlayerLoudnessPicture(



                            enabled = myplayerState.loudnessEnabled,



                            boostDb = loudnessBoostDb,



                            compression = loudnessCompression



                        )



                    }







                    Spacer(Modifier.height(16.dp))







                    MyPlayerSwitchRow(



                        title = "Loudness",



                        subtitle = "Louder and fuller, never clipping",



                        checked = myplayerState.loudnessEnabled,



                        onCheckedChange = { myplayerState.loudnessEnabled = it }



                    )







                    AnimatedVisibility(visible = myplayerState.loudnessEnabled) {



                        Column {



                            Spacer(Modifier.height(14.dp))



                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {



                                MyPlayerLoudnessPreset.entries.forEach { preset ->



                                    val active = selectedLoudnessPreset == preset



                                    Surface(



                                        shape = RoundedCornerShape(20.dp),



                                        color = if (active) MyPlayerTextPrimary else MyPlayerSurfaceHigh,



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



                                            color = if (active) MyPlayerBlack else MyPlayerTextPrimary,



                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)



                                        )



                                    }



                                }



                            }







                            Spacer(Modifier.height(16.dp))



                            MyPlayerLabeledSlider(



                                label = "Boost",



                                valueText = "+${"%.1f".format(loudnessBoostDb)} dB",



                                value = loudnessBoostDb,



                                valueRange = 0f..12f,



                                onValueChange = { loudnessBoostDb = it }



                            )



                            MyPlayerLabeledSlider(



                                label = "Compression",



                                valueText = "${(loudnessCompression * 100).roundToInt()}%",



                                value = loudnessCompression,



                                valueRange = 0f..1f,



                                onValueChange = { loudnessCompression = it }



                            )



                            Text(



                                text = "Compression lifts the quiet parts up toward the loud ones. Night mode keeps whispers and explosions close.",



                                fontSize = 12.sp,



                                color = Color(0xFF757582)



                            )



                        }



                    }







                    Spacer(Modifier.height(18.dp))







                    MyPlayerSwitchRow(



                        title = "Even out songs",



                        subtitle = "Plays every song at the same loudness",



                        checked = myplayerState.evenOutSongs,



                        onCheckedChange = { myplayerState.evenOutSongs = it }



                    )







                    Spacer(Modifier.height(16.dp))



                    Text(



                        text = "REPLAYGAIN",



                        fontSize = 11.sp,



                        fontWeight = FontWeight.Bold,



                        color = Color(0xFF6E6E78),



                        letterSpacing = 0.8.sp



                    )



                    Spacer(Modifier.height(10.dp))



                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



                        listOf("Track", "Album").forEach { mode ->



                            val selected = myplayerState.replayGainMode == mode



                            Surface(



                                shape = RoundedCornerShape(20.dp),



                                color = if (selected) MyPlayerTextPrimary else MyPlayerSurfaceHigh,



                                modifier = Modifier.clickable { myplayerState.replayGainMode = mode }



                            ) {



                                Text(



                                    text = mode,



                                    fontSize = 14.sp,



                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,



                                    color = if (selected) MyPlayerBlack else MyPlayerTextPrimary,



                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp)



                                )



                            }



                        }



                    }







                    Spacer(Modifier.height(16.dp))



                    MyPlayerLabeledSlider(



                        label = "Preamp",



                        valueText = "${if (myplayerState.replayGainPreampDb >= 0f) "+" else ""}${"%.1f".format(myplayerState.replayGainPreampDb)} dB",



                        value = myplayerState.replayGainPreampDb,



                        valueRange = -12f..12f,



                        onValueChange = { myplayerState.replayGainPreampDb = it }



                    )



                }



            }



        }







        // ── CARD 3: SPATIAL AUDIO (CROSSFEED & SPEAKERS - 00:29 - 00:38) ─────



        item {



            Surface(



                shape = RoundedCornerShape(28.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                            .border(1.dp, MyPlayerCardBorder, RoundedCornerShape(20.dp))



                    ) {



                        MyPlayerSpatialPicture(



                            enabled = myplayerState.spatialAudioEnabled,



                            amount = myplayerState.spatialAmount,



                            speakers = myplayerState.spatialMode == "Speakers",



                            speakerAngleDeg = speakerAngleDeg,



                            feedDb = myplayerState.crossfeedDb



                        )



                    }







                    Spacer(Modifier.height(14.dp))







                    MyPlayerSwitchRow(



                        title = "Spatial audio",



                        subtitle = "Takes stereo out of your head and puts it in front of you",



                        checked = myplayerState.spatialAudioEnabled,



                        onCheckedChange = { myplayerState.spatialAudioEnabled = it }



                    )







                    Spacer(Modifier.height(14.dp))







                    // Crossfeed | Speakers Switcher



                    Surface(



                        shape = RoundedCornerShape(24.dp),



                        color = MyPlayerSurfaceHigh,



                        modifier = Modifier.fillMaxWidth()



                    ) {



                        Row(modifier = Modifier.padding(4.dp)) {



                            listOf("Crossfeed", "Speakers").forEach { tab ->



                                val active = myplayerState.spatialMode == tab



                                Box(



                                    modifier = Modifier



                                        .weight(1f)



                                        .clip(RoundedCornerShape(20.dp))



                                        .background(if (active) MyPlayerTextPrimary else Color.Transparent)



                                        .clickable {



                                            myplayerState.spatialMode = tab



                                            myplayerState.spatialAudioEnabled = true



                                        }



                                        .padding(vertical = 10.dp),



                                    contentAlignment = Alignment.Center



                                ) {



                                    Text(



                                        text = tab,



                                        fontWeight = FontWeight.Bold,



                                        color = if (active) MyPlayerBlack else MyPlayerTextPrimary



                                    )



                                }



                            }



                        }



                    }







                    Spacer(Modifier.height(14.dp))







                    if (myplayerState.spatialMode == "Crossfeed") {



                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {



                            listOf(



                                Triple("Light", 700f, -4.5f),



                                Triple("Natural", 700f, -6.0f),



                                Triple("Strong", 650f, -9.5f)



                            ).forEach { (presetName, hz, db) ->



                                val active = myplayerState.spatialPreset == presetName



                                Surface(



                                    shape = RoundedCornerShape(20.dp),



                                    color = if (active) MyPlayerTextPrimary else MyPlayerSurfaceHigh,



                                    modifier = Modifier.clickable {



                                        myplayerState.spatialPreset = presetName



                                        myplayerState.headShadowHz = hz



                                        myplayerState.crossfeedDb = db



                                        myplayerState.spatialAudioEnabled = true



                                    }



                                ) {



                                    Text(



                                        text = presetName,



                                        fontSize = 13.sp,



                                        fontWeight = FontWeight.Bold,



                                        color = if (active) MyPlayerBlack else MyPlayerTextPrimary,



                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)



                                    )



                                }



                            }



                        }







                        Spacer(Modifier.height(14.dp))



                        MyPlayerLabeledSlider(



                            label = "Head shadow",



                            valueText = "${myplayerState.headShadowHz.roundToInt()} Hz",



                            value = myplayerState.headShadowHz,



                            valueRange = 300f..1200f,



                            onValueChange = { myplayerState.headShadowHz = it }



                        )



                        MyPlayerLabeledSlider(



                            label = "Crossfeed",



                            valueText = "${"%.1f".format(myplayerState.crossfeedDb)} dB",



                            value = myplayerState.crossfeedDb,



                            valueRange = -15f..-2f,



                            onValueChange = { myplayerState.crossfeedDb = it }



                        )



                    } else {



                        MyPlayerLabeledSlider(



                            label = "Speaker angle",



                            valueText = "±${speakerAngleDeg.roundToInt()}°",



                            value = speakerAngleDeg,



                            valueRange = 15f..60f,



                            onValueChange = { speakerAngleDeg = it }



                        )



                    }







                    MyPlayerLabeledSlider(



                        label = "Amount",



                        valueText = "${(myplayerState.spatialAmount * 100).roundToInt()}%",



                        value = myplayerState.spatialAmount,



                        valueRange = 0f..1f,



                        onValueChange = { myplayerState.spatialAmount = it }



                    )







                    if (myplayerState.spatialMode == "Speakers") {



                        Text(



                            text = "Two virtual speakers in front of a modelled head; the far ear hears each one a little later and darker, as in a room. 30° is a studio pair.",



                            fontSize = 12.sp,



                            color = MyPlayerTextSecondary



                        )



                    }



                }



            }



        }







        // ── CARD 4: MONO AUDIO (00:38 - 00:42) ───────────────────────────────



        item {



            Surface(



                shape = RoundedCornerShape(24.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



                modifier = Modifier.fillMaxWidth()



            ) {



                Box(modifier = Modifier.padding(18.dp)) {



                    MyPlayerSwitchRow(



                        title = "Mono audio",



                        subtitle = "Both ears hear the whole mix: for one earbud, or hearing in one ear",



                        checked = myplayerState.monoAudio,



                        onCheckedChange = { myplayerState.monoAudio = it }



                    )



                }



            }



        }







        // ── CARD 5: DOLBY ATMOS & SURROUND (00:39 - 00:45) ───────────────────



        item {



            Surface(



                shape = RoundedCornerShape(28.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                            .border(1.dp, MyPlayerCardBorder, RoundedCornerShape(20.dp))



                    ) {



                        MyPlayerAtmosPicture(



                            enabled = myplayerState.dolbyAtmosSurround,



                            renderOnHeadphones = myplayerState.dolbyAtmosSurround



                        )



                    }







                    Spacer(Modifier.height(14.dp))







                    MyPlayerSwitchRow(



                        title = "Dolby Atmos & surround",



                        subtitle = "Plays 5.1, 7.1 and Atmos beds as 3D sound on any headphones, with MyPlayer's head model, instead of folding them to stereo.",



                        checked = myplayerState.dolbyAtmosSurround,



                        onCheckedChange = { myplayerState.dolbyAtmosSurround = it }



                    )







                    Spacer(Modifier.height(12.dp))



                    HorizontalDivider(color = MyPlayerCardBorder)



                    Spacer(Modifier.height(10.dp))



                    Row(



                        modifier = Modifier.fillMaxWidth(),



                        horizontalArrangement = Arrangement.spacedBy(28.dp)



                    ) {



                        Column {



                            Text("ATMOS DECODER", fontSize = 10.sp, color = MyPlayerTextSecondary)



                            Text(



                                text = if (myplayerState.dolbyAtmosSurround) "Binaural Active" else "Standby",



                                fontSize = 12.sp,



                                fontWeight = FontWeight.Bold,



                                color = Color.White



                            )



                        }



                        Column {



                            Text("ANDROID SPATIALIZER", fontSize = 10.sp, color = MyPlayerTextSecondary)



                            Text(



                                text = if (myplayerState.dolbyAtmosSurround) "Head Model 3D" else "Off",



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



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



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



                            .border(1.dp, MyPlayerCardBorder, RoundedCornerShape(20.dp))



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



                            val freq = myplayerState.playbackSpeed * 2.5f



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



                            Text("Playback speed", fontWeight = FontWeight.Bold, color = MyPlayerTextPrimary)



                            Text(



                                text = if (myplayerState.preservePitch) "Pitch stays where it is" else "Pitch follows the speed, like a record",



                                fontSize = 12.sp,



                                color = MyPlayerTextSecondary



                            )



                        }







                        Surface(



                            shape = RoundedCornerShape(20.dp),



                            color = MyPlayerSurfaceHigh



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



                                    color = MyPlayerTextPrimary,



                                    modifier = Modifier.clickable {



                                        myplayerState.playbackSpeed = (myplayerState.playbackSpeed - 0.05f).coerceIn(0.50f, 2.00f)



                                    }



                                )



                                Text(



                                    text = "${"%.2f".format(myplayerState.playbackSpeed)}×",



                                    fontSize = 14.sp,



                                    fontWeight = FontWeight.ExtraBold,



                                    color = MyPlayerTextPrimary



                                )



                                Text(



                                    text = "+",



                                    fontSize = 18.sp,



                                    fontWeight = FontWeight.Bold,



                                    color = MyPlayerTextPrimary,



                                    modifier = Modifier.clickable {



                                        myplayerState.playbackSpeed = (myplayerState.playbackSpeed + 0.05f).coerceIn(0.50f, 2.00f)



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



                            val selected = myplayerState.playbackSpeedMode == mode



                            Surface(



                                shape = RoundedCornerShape(20.dp),



                                color = if (selected) MyPlayerTextPrimary else MyPlayerSurfaceHigh,



                                modifier = Modifier.clickable {



                                    myplayerState.playbackSpeedMode = mode



                                    when (mode) {



                                        "Normal" -> {



                                            myplayerState.playbackSpeed = 1.00f



                                            myplayerState.preservePitch = true



                                            myplayerState.pitchSemitones = 0f



                                        }



                                        "Slowed" -> {



                                            myplayerState.playbackSpeed = 0.85f



                                            myplayerState.preservePitch = false



                                        }



                                        "Slowed + reverb" -> {



                                            myplayerState.playbackSpeed = 0.85f



                                            myplayerState.preservePitch = false



                                            // Automatically enable Hall Reverb (exact match to 00:52 & 01:00!)



                                            myplayerState.reverbEnabled = true



                                            selectedReverbPreset = MyPlayerReverbPreset.HALL



                                            myplayerState.reverbPreset = "Hall"



                                            myplayerState.reverbMix = 0.50f



                                            myplayerState.reverbRoomSize = 0.78f



                                            myplayerState.reverbDamping = 0.35f



                                            myplayerState.reverbWet = 0.60f



                                            reverbPreDelayMs = 25f



                                            reverbLowCutHz = 150f



                                        }



                                        "Nightcore" -> {



                                            myplayerState.playbackSpeed = 1.25f



                                            myplayerState.preservePitch = false



                                        }



                                        "0.75×" -> { myplayerState.playbackSpeed = 0.75f; myplayerState.preservePitch = true }



                                        "1.25×" -> { myplayerState.playbackSpeed = 1.25f; myplayerState.preservePitch = true }



                                        "1.5×" -> { myplayerState.playbackSpeed = 1.50f; myplayerState.preservePitch = true }



                                        "2×" -> { myplayerState.playbackSpeed = 2.00f; myplayerState.preservePitch = true }



                                    }



                                }



                            ) {



                                Text(



                                    text = mode,



                                    fontSize = 13.sp,



                                    fontWeight = FontWeight.Bold,



                                    color = if (selected) MyPlayerBlack else MyPlayerTextPrimary,



                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)



                                )



                            }



                        }



                    }







                    Spacer(Modifier.height(14.dp))







                    MyPlayerLabeledSlider(



                        label = "Speed",



                        valueText = "${"%.2f".format(myplayerState.playbackSpeed)}×",



                        value = myplayerState.playbackSpeed,



                        valueRange = 0.50f..2.00f,



                        onValueChange = { myplayerState.playbackSpeed = it }



                    )







                    MyPlayerSwitchRow(



                        title = "Preserve pitch",



                        subtitle = "Keep the original key when changing speed",



                        checked = myplayerState.preservePitch,



                        onCheckedChange = { myplayerState.preservePitch = it }



                    )







                    AnimatedVisibility(visible = myplayerState.preservePitch) {



                        Column {



                            Spacer(Modifier.height(10.dp))



                            MyPlayerLabeledSlider(



                                label = "Pitch",



                                valueText = "${myplayerState.pitchSemitones.roundToInt()} semitones",



                                value = myplayerState.pitchSemitones,



                                valueRange = -12f..12f,



                                onValueChange = { myplayerState.pitchSemitones = it }



                            )



                        }



                    }







                    Spacer(Modifier.height(6.dp))



                    Text(



                        text = "↻ Reset",



                        fontSize = 13.sp,



                        fontWeight = FontWeight.Bold,



                        color = MyPlayerTextSecondary,



                        modifier = Modifier



                            .align(Alignment.End)



                            .clickable {



                                myplayerState.playbackSpeedMode = "Normal"



                                myplayerState.playbackSpeed = 1.00f



                                myplayerState.preservePitch = true



                                myplayerState.pitchSemitones = 0f



                            }



                    )



                }



            }



        }







        // ── CARD 7: EXPONENTIAL VOLUME (00:56 - 00:59) ───────────────────────



        item {



            Surface(



                shape = RoundedCornerShape(28.dp),



                color = MyPlayerSurface,



                border = BorderStroke(1.dp, MyPlayerCardBorder),



                modifier = Modifier.fillMaxWidth()



            ) {



                Column(modifier = Modifier.padding(18.dp)) {



                    Box(



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(104.dp)



                            .clip(RoundedCornerShape(20.dp))



                            .background(Color(0xFF0A0A0D))



                            .border(1.dp, MyPlayerCardBorder, RoundedCornerShape(20.dp))



                            .padding(16.dp)



                    ) {



                        Canvas(modifier = Modifier.fillMaxSize()) {



                            val bars = 12



                            val step = size.width / bars



                            for (i in 0 until bars) {



                                val t = (i + 1).toFloat() / bars



                                val h = size.height * (if (myplayerState.exponentialVolume) t.pow(2.1f) else t)



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







                    MyPlayerSwitchRow(



                        title = "Exponential volume",



                        subtitle = "A logarithmic curve, for finer control at low volume",



                        checked = myplayerState.exponentialVolume,



                        onCheckedChange = { myplayerState.exponentialVolume = it }



                    )



                }



            }



        }







        item { Spacer(Modifier.height(90.dp)) }



    }



}







@Composable



private fun MyPlayerLabeledSlider(



    label: String,



    valueText: String,



    value: Float,



    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,



    onValueChange: (Float) -> Unit



) {



    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MyPlayerTextPrimary)



            Text(valueText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MyPlayerTextPrimary)



        }



        Spacer(Modifier.height(4.dp))



        Slider(



            value = value,



            onValueChange = onValueChange,



            valueRange = valueRange,



            colors = SliderDefaults.colors(



                thumbColor = MyPlayerTextPrimary,



                activeTrackColor = MyPlayerTextPrimary,



                inactiveTrackColor = if (ThemeState.isDarkMode) Color(0xFF282832) else Color(0xFFD6D9E0)



            )



        )



    }



}







// ═════════════════════════════════════════════════════════════════════════════



// AUDIO FX LIVE PICTURES â the Audio Lab live cards



// each draws what its



// effect does to the sound, follows the real audio level via MyPlayerAudioTap,



// and only runs on the frame clock while on screen.



// ═════════════════════════════════════════════════════════════════════════════







/** keeps the audio tap fed while the FX screen shows a live picture */



@Composable



internal fun MyPlayerLabListenToAudio() {



    DisposableEffect(Unit) {



        MyPlayerAudioTap.listen()



        onDispose { MyPlayerAudioTap.stopListening() }



    }



}







/** a frame clock and a music follower, shared by every picture */



internal class MyPlayerPictureClock {



    val level = MyPlayerLabLevelFollower()



    private var lastTime = -1f







    /** seconds between this frame and the one before */



    var dt = 0f



        private set







    /** once per drawn frame, works out dt and keeps the music level current */



    fun tick(t: Float) {



        if (t == lastTime) {



            dt = 0f



            return



        }



        dt = if (lastTime < 0f) 0f else (t - lastTime).coerceIn(0f, 0.05f)



        level.update(dt)



        lastTime = t



    }



}







/** a smoothed music level 0..1 for pictures to breathe with, updated from the frame clock */



internal class MyPlayerLabLevelFollower {



    private val scratch = FloatArray(1024)



    var level = 0f



        private set



    var punch = 0f



        private set



    private var slow = 0f







    /** call once per frame with the seconds since the last one */



    fun update(dt: Float) {



        val now = MyPlayerAudioTap.level(scratch)



        level += (now - level) * (if (now > level) 0.5f else 0.12f)



        slow += (now - slow) * 0.03f



        // how far above its own recent average it is: a kick or a downbeat



        val p = ((now - slow * 1.25f) * 6f).coerceIn(0f, 1f)



        punch = if (p > punch) p else (punch - dt * 3f).coerceAtLeast(0f)



    }



}







@Composable



internal fun MyPlayerPictureCanvas(draw: DrawScope.(t: Float, clock: MyPlayerPictureClock) -> Unit) {



    val onScreen = remember { mutableStateOf(true) }



    val time = remember { mutableFloatStateOf(0f) }



    val clock = remember { MyPlayerPictureClock() }



    LaunchedEffect(Unit) {



        var last = 0L



        while (true) {



            if (!onScreen.value) {



                snapshotFlow { onScreen.value }.first { it }



                last = 0L



            }



            withFrameNanos { now ->



                if (last != 0L) time.floatValue += ((now - last) / 1e9f).coerceAtMost(0.05f)



                last = now



            }



        }



    }



    Canvas(Modifier.fillMaxSize().myplayerTrackOnScreen(onScreen)) {



        val t = time.floatValue



        clock.tick(t)



        draw(t, clock)



    }



}







/** true while any part of this is inside the window; written only when it flips */



@Composable



private fun Modifier.myplayerTrackOnScreen(state: MutableState<Boolean>): Modifier {



    val view = LocalView.current



    return onGloballyPositioned { coords ->



        val bounds = coords.boundsInWindow()



        val visible = bounds.height > 0f && bounds.bottom > 0f && bounds.top < view.height.toFloat()



        if (visible != state.value) state.value = visible



    }



}







// ---- loudness: the music's level, lifted by the boost and flattened under the ceiling -----------







private class MyPlayerLevelHistory {



    val values = FloatArray(56)



    var head = 0



    var accumulated = 0f



}







@Composable



fun MyPlayerLoudnessPicture(enabled: Boolean, boostDb: Float, compression: Float) {



    val on by animateFloatAsState(if (enabled) 1f else 0f, tween(400), label = "on")



    val history = remember { MyPlayerLevelHistory() }



    MyPlayerPictureCanvas { t, clock ->



        history.accumulated += clock.dt



        if (history.accumulated > 1f / 24f) {



            history.accumulated = 0f



            val v = if (MyPlayerAudioTap.isLive) clock.level.level * 2.6f



            else 0.18f + 0.12f * sin(t * 2.1f) + 0.1f * sin(t * 5.3f + 1f) + 0.06f * sin(t * 11f)



            history.values[history.head] = v.coerceIn(0.005f, 1.4f)



            history.head = (history.head + 1) % history.values.size



        }



        val w = size.width



        val h = size.height



        val top = 18.dp.toPx()



        val bottom = h - 14.dp.toPx()



        fun yOf(db: Float) = bottom - ((db + 42f) / 42f).coerceIn(0f, 1f) * (bottom - top)



        val ceiling = yOf(-0.5f)







        val compression = compression



        val threshold = -10f - 24f * compression



        val slope = 1f - 1f / (1f + 3f * compression)



        val makeup = if (compression > 0f) -threshold * slope * 0.5f else 0f







        val n = history.values.size



        val barW = (w - 24.dp.toPx()) / n



        for (k in 0 until n) {



            val v = history.values[(history.head + k) % n]



            val inDb = 20f * log10(max(v, 1e-4f))



            val reduced = if (inDb > threshold) (threshold - inDb) * slope else 0f



            val outDb = (inDb + (boostDb + makeup + reduced) * on).coerceAtMost(-0.5f)



            val x = 12.dp.toPx() + k * barW



            val yIn = yOf(min(inDb, 0f))



            val yOut = yOf(outDb)



            val mid = (top + bottom) / 2f



            // what came in, faint; what goes out, solid, mirrored around the middle like a waveform



            val inHalf = (bottom - yIn) / 2f



            val outHalf = (bottom - yOut) / 2f



            drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(x, mid - inHalf), Size(barW * 0.62f, inHalf * 2),



                CornerRadius(barW * 0.3f))



            val limited = on > 0.5f && outDb >= -0.6f



            drawRoundRect(



                if (limited) Color.White else Color.White.copy(alpha = 0.72f),



                Offset(x, mid - outHalf), Size(barW * 0.62f, outHalf * 2), CornerRadius(barW * 0.3f),



            )



        }



        // the ceiling the limiter holds, top and bottom



        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))



        val mid = (top + bottom) / 2f



        val half = (bottom - ceiling) / 2f



        drawLine(Color.White.copy(alpha = 0.3f * on + 0.08f), Offset(10.dp.toPx(), mid - half), Offset(w - 10.dp.toPx(), mid - half),



            1.dp.toPx(), pathEffect = dash)



        drawLine(Color.White.copy(alpha = 0.3f * on + 0.08f), Offset(10.dp.toPx(), mid + half), Offset(w - 10.dp.toPx(), mid + half),



            1.dp.toPx(), pathEffect = dash)



        // how hard it's working right now



        val reduction = (-MyPlayerAudioTap.loudnessReductionDb).coerceIn(0f, 12f) / 12f * on



        if (reduction > 0.01f) {



            val meterH = (bottom - top) * reduction



            drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(w - 7.dp.toPx(), top), Size(3.dp.toPx(), meterH), CornerRadius(2.dp.toPx()))



        }



    }



}







// ---- spatial: a head from above, and where the sound reaches it from -------------------------







@Composable



fun MyPlayerSpatialPicture(enabled: Boolean, amount: Float, speakers: Boolean, speakerAngleDeg: Float, feedDb: Float) {



    val on by animateFloatAsState(if (enabled) amount else 0f, tween(400), label = "on")



    val onSpeakers by animateFloatAsState(if (speakers) 1f else 0f, tween(500), label = "mode")



    val angle by animateFloatAsState(speakerAngleDeg, spring(dampingRatio = 0.8f, stiffness = 140f), label = "angle")



    val feed by animateFloatAsState(feedDb, tween(300), label = "feed")



    MyPlayerPictureCanvas { t, clock ->



        val w = size.width



        val h = size.height



        val head = Offset(w / 2f, h * 0.66f)



        val r = 17.dp.toPx()



        val earL = Offset(head.x - r, head.y)



        val earR = Offset(head.x + r, head.y)



        val pulse = clock.level.punch







        // head, nose up, ears



        drawCircle(Color.White.copy(alpha = 0.9f), r, head, style = Stroke(1.6.dp.toPx()))



        drawPath(Path().apply {



            moveTo(head.x - 4.dp.toPx(), head.y - r + 1.dp.toPx())



            lineTo(head.x, head.y - r - 6.dp.toPx())



            lineTo(head.x + 4.dp.toPx(), head.y - r + 1.dp.toPx())



        }, Color.White.copy(alpha = 0.9f), style = Stroke(1.6.dp.toPx()))



        drawCircle(Color.White, 3.dp.toPx(), earL)



        drawCircle(Color.White, 3.dp.toPx(), earR)







        // stereo as headphones play it: each side only in its own ear, inside the head



        val inside = 1f - on



        if (inside > 0.01f) {



            drawCircle(Color(0xFF6BBAF2).copy(alpha = 0.7f * inside), 5.dp.toPx() + 2.dp.toPx() * pulse, Offset(head.x - r * 0.45f, head.y))



            drawCircle(Color(0xFFF28BA8).copy(alpha = 0.7f * inside), 5.dp.toPx() + 2.dp.toPx() * pulse, Offset(head.x + r * 0.45f, head.y))



        }







        // virtual speakers out in front, each heard by both ears (the far one later and darker)



        val ring = min(w * 0.36f, h * 0.55f)



        val crossfeedAmount = on * (1f - onSpeakers)



        val speakerAmount = on * onSpeakers



        val rad = Math.toRadians(angle.toDouble())



        val spL = Offset(head.x - (sin(rad) * ring).toFloat(), head.y - (cos(rad) * ring).toFloat())



        val spR = Offset(head.x + (sin(rad) * ring).toFloat(), head.y - (cos(rad) * ring).toFloat())



        if (speakerAmount > 0.01f) {



            listOf(spL to Color(0xFF6BBAF2), spR to Color(0xFFF28BA8)).forEachIndexed { side, (sp, color) ->



                val near = if (side == 0) earL else earR



                val far = if (side == 0) earR else earL



                myplayerFlowLine(sp, near, color.copy(alpha = 0.8f * speakerAmount), t, 1f)



                myplayerFlowLine(sp, far, color.copy(alpha = 0.32f * speakerAmount), t - 0.08f, 0.6f)



                drawRoundRect(color.copy(alpha = speakerAmount), Offset(sp.x - 6.dp.toPx(), sp.y - 8.dp.toPx()),



                    Size(12.dp.toPx(), 16.dp.toPx()), CornerRadius(3.dp.toPx()))



            }



        }



        // crossfeed: each side bleeds round the head into the other ear, softer the lower the feed



        if (crossfeedAmount > 0.01f) {



            val strength = (1f - (feed - 1f) / 14f).coerceIn(0.2f, 1f)



            val lift = r * 2.4f



            listOf(earL to earR to Color(0xFF6BBAF2), earR to earL to Color(0xFFF28BA8)).forEach { (pair, color) ->



                val (from, to) = pair



                val path = Path().apply {



                    moveTo(from.x, from.y - 4.dp.toPx())



                    cubicTo(from.x, from.y - lift, to.x, to.y - lift, to.x, to.y - 4.dp.toPx())



                }



                drawPath(path, color.copy(alpha = 0.55f * crossfeedAmount * strength), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round,



                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), -t * 40.dp.toPx())))



            }



            drawCircle(Color(0xFF6BBAF2).copy(alpha = 0.8f * crossfeedAmount), 5.dp.toPx() + 2.dp.toPx() * pulse, Offset(earL.x - 10.dp.toPx(), earL.y))



            drawCircle(Color(0xFFF28BA8).copy(alpha = 0.8f * crossfeedAmount), 5.dp.toPx() + 2.dp.toPx() * pulse, Offset(earR.x + 10.dp.toPx(), earR.y))



        }



    }



}







/** a line with dots running along it, from one point to the other */



private fun DrawScope.myplayerFlowLine(from: Offset, to: Offset, color: Color, t: Float, weight: Float) {



    drawLine(color.copy(alpha = color.alpha * 0.35f), from, to, 1.2.dp.toPx() * weight)



    for (k in 0 until 3) {



        val p = ((t * 0.9f + k / 3f) % 1f)



        drawCircle(color, 2.2.dp.toPx() * weight, Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p))



    }



}







// ---- dolby atmos: a dome of speakers over the head, objects moving through it ----------------







@Composable



fun MyPlayerAtmosPicture(enabled: Boolean, renderOnHeadphones: Boolean) {



    val on by animateFloatAsState(if (enabled) 1f else 0f, tween(500), label = "on")



    val render by animateFloatAsState(if (enabled && renderOnHeadphones) 1f else 0f, tween(500), label = "render")



    MyPlayerPictureCanvas { t, clock ->



        val w = size.width



        val h = size.height



        val c = Offset(w / 2f, h * 0.74f)



        val rx = w * 0.36f



        val ry = h * 0.2f



        val dome = h * 0.56f



        val line = Color.White.copy(alpha = 0.12f + 0.2f * on)







        // floor ring and dome



        drawOval(line, Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(1.dp.toPx()))



        val domePath = Path().apply {



            moveTo(c.x - rx, c.y)



            cubicTo(c.x - rx, c.y - dome * 1.3f, c.x + rx, c.y - dome * 1.3f, c.x + rx, c.y)



        }



        drawPath(domePath, line.copy(alpha = line.alpha * on), style = Stroke(1.dp.toPx(),



            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))))







        // the bed speakers (7.1) on the floor, four heights in the dome



        val bed = listOf(-30f, 30f, 0f, -110f, 110f, -150f, 150f)



        bed.forEach { az ->



            val a = Math.toRadians(az.toDouble() - 90)



            val p = Offset(c.x + (cos(a) * rx).toFloat(), c.y + (sin(a) * ry).toFloat())



            drawCircle(Color.White.copy(alpha = 0.25f + 0.45f * on), 2.6.dp.toPx(), p)



        }



        if (on > 0.01f) {



            listOf(-45f, 45f, -135f, 135f).forEach { az ->



                val a = Math.toRadians(az.toDouble() - 90)



                val p = Offset(c.x + (cos(a) * rx * 0.62f).toFloat(), c.y + (sin(a) * ry * 0.62f).toFloat() - dome * 0.72f)



                drawCircle(Color.White.copy(alpha = 0.55f * on), 2.6.dp.toPx(), p)



            }



        }







        // the listener



        drawCircle(Color.White.copy(alpha = 0.9f), 9.dp.toPx(), Offset(c.x, c.y - 4.dp.toPx()), style = Stroke(1.5.dp.toPx()))







        // objects: with atmos they fly around and above, without they flatten into stereo



        val colors = listOf(Color(0xFF6BBAF2), Color(0xFFF28BA8), Color(0xFF8FD98B))



        colors.forEachIndexed { k, color ->



            val speed = 0.35f + k * 0.17f



            val phase = t * speed + k * 2.1f



            val height = (0.25f + 0.5f * (0.5f + 0.5f * sin(phase * 1.3f + k))) * dome * on



            val x = c.x + cos(phase) * rx * 0.82f * (0.35f + 0.65f * on)



            val depth = sin(phase)



            val y = c.y + depth * ry * 0.82f * on - height - 4.dp.toPx() * (1f - on)



            val scale = 1f + 0.35f * depth * on



            val glow = 0.25f + 0.2f * clock.level.punch



            drawCircle(color.copy(alpha = glow), 9.dp.toPx() * scale, Offset(x, y))



            drawCircle(color, 4.dp.toPx() * scale, Offset(x, y))



            if (render > 0.01f) {



                drawLine(color.copy(alpha = 0.35f * render), Offset(x, y), Offset(c.x, c.y - 4.dp.toPx()), 1.dp.toPx())



            }



        }



    }



}







// ---- reverb: waves leave the speaker, bounce between the walls and die away -------------------







private class MyPlayerRoomWaves {



    val born = FloatArray(14) { -100f }



    val strength = FloatArray(14)



    var next = 0



    var nextEmit = 0f



    var lastEmit = -10f







    fun emit(t: Float, s: Float) {



        born[next] = t



        strength[next] = s



        next = (next + 1) % born.size



        lastEmit = t



    }



}







@Composable



fun MyPlayerReverbPicture(enabled: Boolean, roomSize: Float, damping: Float, wet: Float, mix: Float) {



    val room by animateFloatAsState(roomSize, spring(dampingRatio = 0.8f, stiffness = 120f), label = "room")



    val on by animateFloatAsState(if (enabled) 1f else 0f, tween(400), label = "on")



    val waves = remember { MyPlayerRoomWaves() }



    MyPlayerPictureCanvas { t, clock ->



        val w = size.width



        val h = size.height



        val cy = h / 2f



        val speakerX = w * 0.15f



        val backX = w * 0.05f



        val wallX = w * (0.52f + 0.4f * room)



        val speed = w * 0.42f







        // a wave on every beat when music plays, a calm pulse when it doesn't



        val live = MyPlayerAudioTap.isLive



        val punch = clock.level.punch



        if (t >= waves.nextEmit || (live && punch > 0.55f && t - waves.lastEmit > 0.28f)) {



            waves.emit(t, if (live) 0.55f + 0.45f * min(1f, clock.level.level * 3f + punch) else 0.75f)



            waves.nextEmit = t + if (live) 0.9f else 1.3f



        }







        val reflectivity = (0.35f + 0.55f * roomSize) * (1f - 0.55f * damping)



        val echoAlpha = on * (0.35f + 0.65f * wet) * (0.4f + 0.6f * mix)



        var flash = 0f



        val span = wallX - backX



        for (i in waves.born.indices) {



            val age = t - waves.born[i]



            if (age < 0f || age > 8f) continue



            val d = age * speed



            val first = wallX - speakerX



            if (d <= first || on < 0.01f) {



                // the direct sound; with the reverb off it passes straight through the wall



                val x = speakerX + d



                if (x > w + 20f) continue



                val a = waves.strength[i] * exp(-d / (w * 1.6f))



                myplayerReverbWave(x, cy, h, d, a * (if (d > first) 1f - on else 1f), rightward = true, softness = 0f)



                continue



            }



            // after the first hit it folds back and forth between the walls, losing energy each time



            val past = d - first



            val bounces = 1 + (past / span).toInt()



            val inLeg = past % span



            val rightward = bounces % 2 == 0



            val x = if (rightward) backX + inLeg else wallX - inLeg



            val a = waves.strength[i] * reflectivity.pow(bounces) * exp(-d / (w * 2.2f)) * echoAlpha



            if (a < 0.02f) continue



            if (inLeg < span * 0.08f && !rightward) flash = max(flash, a * (1f - inLeg / (span * 0.08f)))



            myplayerReverbWave(x, cy, h, d, a, rightward, softness = damping)



        }







        // the wall, lit for a moment by each wave that reaches it



        val wallH = h * 0.62f



        drawRoundRect(



            Color.White.copy(alpha = (0.18f + 0.55f * on + 0.6f * flash).coerceAtMost(1f)),



            Offset(wallX, cy - wallH / 2), Size(5.dp.toPx(), wallH), CornerRadius(3.dp.toPx()),



        )



        myplayerSpeakerIcon(Offset(speakerX, cy), 1f + clock.level.punch * 0.12f)



    }



}







private fun DrawScope.myplayerReverbWave(x: Float, cy: Float, h: Float, travelled: Float, alpha: Float, rightward: Boolean, softness: Float) {



    if (alpha <= 0.005f) return



    val reach = min(h * 0.42f, 14.dp.toPx() + travelled * 0.18f)



    val bulge = reach * 0.42f * if (rightward) 1f else -1f



    val path = Path().apply {



        moveTo(x - bulge * 0.2f, cy - reach)



        quadraticTo(x + bulge, cy, x - bulge * 0.2f, cy + reach)



    }



    if (softness > 0.15f) {



        drawPath(path, Color.White.copy(alpha = alpha * 0.25f * softness), style = Stroke(7.dp.toPx() * softness, cap = StrokeCap.Round))



    }



    drawPath(path, Color.White.copy(alpha = alpha.coerceAtMost(1f)), style = Stroke((2.2f - softness).dp.toPx(), cap = StrokeCap.Round))



}







private fun DrawScope.myplayerSpeakerIcon(center: Offset, scale: Float) {



    val s = 9.dp.toPx() * scale



    val body = Path().apply {



        moveTo(center.x - s * 1.1f, center.y - s * 0.45f)



        lineTo(center.x - s * 0.4f, center.y - s * 0.45f)



        lineTo(center.x + s * 0.5f, center.y - s * 1.1f)



        lineTo(center.x + s * 0.5f, center.y + s * 1.1f)



        lineTo(center.x - s * 0.4f, center.y + s * 0.45f)



        lineTo(center.x - s * 1.1f, center.y + s * 0.45f)



        close()



    }



    drawPath(body, Color.White.copy(alpha = 0.92f), style = Stroke(1.6.dp.toPx()))



}







@Composable



private fun MyPlayerSwitchRow(



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



        Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {



            Text(



                text = title,



                fontSize = 17.sp,



                fontWeight = FontWeight.SemiBold,



                color = MyPlayerTextPrimary



            )



            Spacer(Modifier.height(2.dp))



            Text(



                text = subtitle,



                fontSize = 13.sp,



                lineHeight = 17.sp,



                color = MyPlayerTextSecondary



            )



        }



        Switch(



            checked = checked,



            onCheckedChange = onCheckedChange,



            colors = SwitchDefaults.colors(



                checkedThumbColor = if (ThemeState.isDarkMode) Color(0xFF16161D) else Color.White,



                checkedTrackColor = if (ThemeState.isDarkMode) Color.White else Color(0xFF17171C),



                uncheckedThumbColor = if (ThemeState.isDarkMode) Color(0xFF485060) else Color(0xFF9EA3AF),



                uncheckedTrackColor = if (ThemeState.isDarkMode) Color(0xFF2C3240) else Color(0xFFE2E4E9),



                uncheckedBorderColor = Color.Transparent,



                checkedBorderColor = Color.Transparent



            )



        )



    }



}