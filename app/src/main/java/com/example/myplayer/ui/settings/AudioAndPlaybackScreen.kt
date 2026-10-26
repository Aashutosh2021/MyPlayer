package com.example.myplayer.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myplayer.data.preferences.*
import com.example.myplayer.playback.AudioFeatureManagers
import com.example.myplayer.playback.dsp.MasterAudioDspProcessor
import kotlinx.coroutines.launch

private val BgDeep = Color(0xFF0E0B0B)
private val CardBg = Color(0xFF1A1616)
private val CardSelectedBorder = Color(0xFFE0B6B1)
private val AccentRose = Color(0xFFD9A7A0)
private val IconBoxRose = Color(0xFF382224)
private val IconBoxGreen = Color(0xFF1E3329)
private val AccentMint = Color(0xFF75C89B)

private enum class SubScreen { MAIN, STREAMING_QUALITY, DOWNLOAD_QUALITY, EQUALIZER_15 }

@Composable
fun AudioAndPlaybackScreen(
    store: AudioPlaybackStore,
    featureManagers: AudioFeatureManagers,
    onBack: () -> Unit
) {
    val state by store.stateFlow.collectAsState(initial = AudioPlaybackState())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentSubScreen by remember { mutableStateOf(SubScreen.MAIN) }
    var showLoudnessDialog by remember { mutableStateOf(false) }
    var showClarityPresetDialog by remember { mutableStateOf(false) }
    var isBatteryUnrestricted by remember { mutableStateOf(featureManagers.isBatteryUnrestricted()) }

    when (currentSubScreen) {
        SubScreen.STREAMING_QUALITY -> {
            QualityPickerSubScreen(
                title = "Streaming Quality",
                subtitle = "Select preferred audio resolution & bit depth",
                isDownload = false,
                selected = state.streamingQuality,
                onSelect = { scope.launch { store.setStreamingQuality(it) } },
                onBack = { currentSubScreen = SubScreen.MAIN }
            )
            return
        }
        SubScreen.DOWNLOAD_QUALITY -> {
            QualityPickerSubScreen(
                title = "Download Quality",
                subtitle = "Select offline audio resolution & bit depth",
                isDownload = true,
                selected = state.downloadQuality,
                onSelect = { scope.launch { store.setDownloadQuality(it) } },
                onBack = { currentSubScreen = SubScreen.MAIN }
            )
            return
        }
        SubScreen.EQUALIZER_15 -> {
            Equalizer15BandScreen(
                state = state,
                onToggleEq = { scope.launch { store.setEqEnabled(it) } },
                onSelectPreset = { scope.launch { store.setEqPreset(it) } },
                onBandChange = { idx, db -> scope.launch { store.setEqBandGain(idx, db, state.eqBandGains) } },
                onReset = { scope.launch { store.setEqPreset(Eq15Preset.DEFAULT) } },
                onBack = { currentSubScreen = SubScreen.MAIN }
            )
            return
        }
        SubScreen.MAIN -> Unit
    }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(CircleShape).background(CardBg)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Audio & Playback", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {},
                    modifier = Modifier.clip(CircleShape).background(CardBg)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Text(
                    "Audio & Streaming",
                    color = AccentRose,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // 1. Streaming Quality
            item {
                SettingsRowNav(
                    icon = Icons.Default.HighQuality,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Streaming Quality",
                    subtitle = "${state.streamingQuality.title} (${state.streamingQuality.subtitle})",
                    onClick = { currentSubScreen = SubScreen.STREAMING_QUALITY }
                )
            }

            // 2. Download Quality
            item {
                SettingsRowNav(
                    icon = Icons.Default.CloudDownload,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Download Quality",
                    subtitle = "${state.downloadQuality.title} (${state.downloadQuality.subtitle})",
                    onClick = { currentSubScreen = SubScreen.DOWNLOAD_QUALITY }
                )
            }

            // 3. Bit-Perfect Mode
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Tune,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Bit-Perfect Mode (Direct ...",
                    subtitle = "Bypass DSP and, with a USB DAC...",
                    checked = state.bitPerfectMode,
                    onCheckedChange = { scope.launch { store.setBitPerfect(it) } }
                )
            }

            // 4. Crossfade
            item {
                SettingsRowToggle(
                    icon = Icons.Default.GraphicEq,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Crossfade",
                    subtitle = "Blend the end of a track into the...",
                    checked = state.crossfadeEnabled,
                    onCheckedChange = { scope.launch { store.setCrossfade(it) } }
                )
            }

            // 5. Keep Screen Awake on Lyrics
            item {
                SettingsRowToggle(
                    icon = Icons.Default.LightMode,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Keep Screen Awake on Ly...",
                    subtitle = "Prevent screen from turning off ...",
                    checked = state.keepScreenAwakeLyrics,
                    onCheckedChange = { scope.launch { store.setKeepAwakeLyrics(it) } }
                )
            }

            // 6. Download Synced Lyrics
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Lyrics,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Download Synced Lyrics",
                    subtitle = "Save .lrc companion files & emb...",
                    checked = state.downloadSyncedLyrics,
                    onCheckedChange = { scope.launch { store.setDownloadSyncedLyrics(it) } }
                )
            }

            // 7. Background Playback (Battery Unrestricted)
            item {
                SettingsRowNav(
                    icon = Icons.Default.Bolt,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Background Playback (Battery ...",
                    subtitle = if (isBatteryUnrestricted) "Unrestricted • Exempt from background killer"
                    else "Restricted • Tap to exempt from Sams...",
                    onClick = {
                        runCatching { context.startActivity(featureManagers.buildBatteryExemptionIntent()) }
                        isBatteryUnrestricted = featureManagers.isBatteryUnrestricted()
                    }
                )
            }

            item {
                Text(
                    "Output & Loudness",
                    color = AccentRose,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                )
            }

            // 8. USB Exclusive Output
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Usb,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "USB Exclusive Output",
                    subtitle = if (state.usbExclusiveOutput) "On • Direct libusb Bit-Perfect active"
                    else "Off • Needs a USB DAC and Bit-P...",
                    checked = state.usbExclusiveOutput,
                    onCheckedChange = { scope.launch { store.setUsbExclusive(it) } }
                )
            }

            // 9. Internal Direct Output
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Memory,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Internal direct output",
                    subtitle = "Off • Like UAPP HiRes Direct, wit...",
                    checked = state.internalDirectOutput,
                    onCheckedChange = { scope.launch { store.setInternalDirect(it) } }
                )
            }

            // 10. Software Volume (USB DAC)
            item {
                SettingsRowToggle(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Software volume (USB DA...",
                    subtitle = "DAC hardware volume when avai...",
                    checked = state.softwareVolumeUsb,
                    onCheckedChange = { scope.launch { store.setSoftwareVolumeUsb(it) } }
                )
            }

            // 11. Loudness Normalization Dialog Trigger
            item {
                SettingsRowNav(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Loudness Normalization",
                    subtitle = "${state.loudnessMode.title} • ${state.loudnessMode.description}",
                    onClick = { showLoudnessDialog = true }
                )
            }

            // 12. 15-Band Equalizer Screen Trigger
            item {
                SettingsRowNav(
                    icon = Icons.Default.Equalizer,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Equalizer",
                    subtitle = "${if (state.eqEnabled) "On" else "Off"} • ${state.eqPreset.label} • 15-band",
                    onClick = { currentSubScreen = SubScreen.EQUALIZER_15 }
                )
            }

            // 13. System Audio Effects
            item {
                SettingsRowToggle(
                    icon = Icons.Default.SurroundSound,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "System Audio Effects",
                    subtitle = "Let Dolby / equalizer apps proce...",
                    checked = state.systemAudioEffects,
                    onCheckedChange = { scope.launch { store.setSystemAudioEffects(it) } }
                )
            }

            // 14. Studio Master Clarity
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Waves,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Studio Master Clarity",
                    subtitle = if (state.studioMasterClarity) "Active • Studio Master Clarity chain"
                    else "Original unshaped output",
                    checked = state.studioMasterClarity,
                    onCheckedChange = { scope.launch { store.setStudioMasterClarity(it) } }
                )
            }

            // 15. Clarity Output Preset Dialog Trigger
            item {
                SettingsRowNav(
                    icon = Icons.Default.Tune,
                    iconBg = IconBoxGreen,
                    iconTint = AccentMint,
                    title = "Clarity Output Preset",
                    subtitle = "${state.clarityOutputPreset.title} • ${state.clarityOutputPreset.description}",
                    onClick = { showClarityPresetDialog = true }
                )
            }

            // 16. Clarity Spatial Bypass
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Hearing,
                    iconBg = IconBoxRose,
                    iconTint = AccentRose,
                    title = "Clarity Spatial Bypass",
                    subtitle = if (state.claritySpatialBypass) "On • Bypass Clarity on Atmos/Spatial"
                    else "Off • Clarity processes every sou...",
                    checked = state.claritySpatialBypass,
                    onCheckedChange = { scope.launch { store.setClaritySpatialBypass(it) } }
                )
            }
        }
    }

    // LOUDNESS NORMALIZATION DIALOG (00:13 in video)
    if (showLoudnessDialog) {
        Dialog(onDismissRequest = { showLoudnessDialog = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = CardBg) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Loudness\nNormalization", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Evens out volume jumps between tracks. Bypassed in Bit-Perfect and USB exclusive modes.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    LoudnessMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { store.setLoudnessMode(mode) } }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.loudnessMode == mode,
                                onClick = { scope.launch { store.setLoudnessMode(mode) } },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentRose)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(mode.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(mode.description, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    TextButton(
                        onClick = { showLoudnessDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done", color = AccentRose, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // CLARITY OUTPUT PRESET DIALOG (00:21 in video)
    if (showClarityPresetDialog) {
        Dialog(onDismissRequest = { showClarityPresetDialog = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = CardBg) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Clarity Output\nPreset", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Tunes the Studio Master Clarity chain to the output. Reference is the unmodified shipping curve.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    ClarityOutputPreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { store.setClarityOutputPreset(preset) } }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.clarityOutputPreset == preset,
                                onClick = { scope.launch { store.setClarityOutputPreset(preset) } },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentRose)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(preset.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(preset.description, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    TextButton(
                        onClick = { showClarityPresetDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done", color = AccentRose, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * STREAMING & DOWNLOAD QUALITY SUB-SCREEN (00:02 & 00:05 in Video)
 */
@Composable
private fun QualityPickerSubScreen(
    title: String,
    subtitle: String,
    isDownload: Boolean,
    selected: AudioQualityTier,
    onSelect: (AudioQualityTier) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = BgDeep,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(CardBg)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("Audio & Playback", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IconBoxRose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isDownload) Icons.Default.CloudDownload else Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = AccentRose
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            item {
                val actionWord = if (isDownload) "downloads" else "plays"
                Text(
                    "YouTube Music $actionWord strictly as-is (never upgrades). Dolby Atmos $actionWord only when selected — otherwise it never $actionWord. Atmos falls back to Hi-Res → CD → 320 kbps → YouTube. Hi-Res steps down to lower lossless tiers, then YouTube last.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            itemsIndexed(AudioQualityTier.entries) { _, tier ->
                val isSelected = selected == tier
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Color(0xFF332022) else CardBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) CardSelectedBorder else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelect(tier) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tier.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    tier.badge,
                                    color = AccentRose,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(tier.subtitle, color = Color.LightGray, fontSize = 12.sp)
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(AccentRose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BgDeep, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 15-BAND EQUALIZER SCREEN (00:15 - 00:17 in Video)
 */
@Composable
private fun Equalizer15BandScreen(
    state: AudioPlaybackState,
    onToggleEq: (Boolean) -> Unit,
    onSelectPreset: (Eq15Preset) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = BgDeep,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(CardBg)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Equalizer", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("15-band hardware acoustic tuning", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CardBg)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset", color = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Enable Equalizer Toggle Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2E1E20))
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Equalizer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Shaping your music in real-time", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.eqEnabled,
                        onCheckedChange = onToggleEq,
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDeep, checkedTrackColor = AccentRose)
                    )
                }
            }

            // Real-Time 15-Point Frequency Response Curve Canvas
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f
                        drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, midY), Offset(w, midY), strokeWidth = 2f)

                        val bands = state.eqBandGains
                        val stepX = w / (bands.size - 1).coerceAtLeast(1)
                        val path = Path()

                        bands.forEachIndexed { idx, db ->
                            val x = idx * stepX
                            val y = midY - (db / 12f) * (midY * 0.85f)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = AccentRose, style = Stroke(width = 5f, cap = StrokeCap.Round))
                        bands.forEachIndexed { idx, db ->
                            val x = idx * stepX
                            val y = midY - (db / 12f) * (midY * 0.85f)
                            drawCircle(color = Color.White, radius = 7f, center = Offset(x, y))
                        }
                    }
                    Text("+12dB", color = AccentRose, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopEnd))
                    Text("0dB", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterEnd))
                    Text("-12dB", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd))
                }
            }

            // Presets FlowRow (Exact 12 Presets from Video)
            item {
                Text("Presets", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Eq15Preset.entries.forEach { preset ->
                        val isSelected = state.eqPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4A3234),
                                selectedLabelColor = Color.White,
                                containerColor = CardBg,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
            }

            // 15 Vertical Faders (25Hz SUB to 16kHz SPARKLE)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .padding(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("+8 dB (Boost)", color = Color.Gray, fontSize = 11.sp)
                        Text("-8 dB (Cut)", color = Color.Gray, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(MasterAudioDspProcessor.BAND_LABELS) { index, label ->
                            val currentGain = state.eqBandGains.getOrElse(index) { 0f }
                            LastWaveVerticalFader(
                                gainDb = currentGain,
                                label = label,
                                enabled = state.eqEnabled,
                                onGainChange = { newGain -> onBandChange(index, newGain) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.eqPreset.label} preset active • touch any bar to customize",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun LastWaveVerticalFader(
    gainDb: Float,
    label: String,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val normalized = ((gainDb + 8f) / 16f).coerceIn(0f, 1f)
    // The pointerInput blocks below are keyed only by `enabled`, so they must NOT capture
    // gainDb / onGainChange directly (they would stay frozen at the first composition's values).
    val latestGain by rememberUpdatedState(gainDb)
    val latestOnGainChange by rememberUpdatedState(onGainChange)

    Column(
        modifier = modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain value Pill matching LastWave
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                !enabled -> Color(0xFF221E1E)
                gainDb > 0f -> Color(0xFF4A2528)
                gainDb < 0f -> Color(0xFF203628)
                else -> Color(0xFF282323)
            }
        ) {
            Text(
                text = if (gainDb > 0f) "+${"%.1f".format(gainDb)}" else "%.1f".format(gainDb),
                fontSize = 11.sp,
                fontWeight = if (gainDb != 0f) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    !enabled -> Color.Gray.copy(alpha = 0.5f)
                    gainDb > 0f -> Color(0xFFE8B4AD)
                    gainDb < 0f -> Color(0xFFA5D6A7)
                    else -> Color.LightGray
                },
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Capsule Track (44dp wide x 160dp tall)
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF241F1F))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val thumbPx = 26.dp.toPx()
                    fun applyTouch(y: Float) {
                        val g = eqGainForTouch(y, size.height.toFloat(), thumbPx)
                        if (g != latestGain) {
                            if (g == 0f) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                            latestOnGainChange(g)
                        }
                    }
                    // Absolute finger position -> gain: the thumb follows the finger 1:1.
                    detectVerticalDragGestures(
                        onDragStart = { offset -> applyTouch(offset.y) },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            applyTouch(change.position.y)
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val thumbPx = 26.dp.toPx()
                    // Tap anywhere on the bar jumps the thumb there ("touch any bar to customize").
                    detectTapGestures { offset ->
                        val g = eqGainForTouch(offset.y, size.height.toFloat(), thumbPx)
                        if (g != latestGain) latestOnGainChange(g)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Center Baseline (0 dB)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // Active Level Fill (between 0 dB baseline and thumb)
            val baselineFraction = 0.5f
            val topFraction = if (normalized >= baselineFraction) 1f - normalized else 1f - baselineFraction
            val heightFraction = Math.abs(normalized - baselineFraction)

            if (heightFraction > 0.01f && enabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(heightFraction)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            translationY = size.height * topFraction
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (gainDb > 0f) {
                                Brush.verticalGradient(listOf(Color(0xFFE0B6B1), Color(0xFF8A464B)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xFF2D593D), Color(0xFF75C89B)))
                            }
                        )
                )
            }

            // Draggable Thumb with "=" Grip
            val thumbY = (1f - normalized) * (160f - 26f)
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(26.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = thumbY
                    }
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFE0B6B1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "=",
                    color = Color(0xFF2B1416),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Frequency Label (e.g. 25 \n SUB)
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}


@Composable
private fun SettingsRowNav(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
private fun SettingsRowToggle(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgDeep,
                checkedTrackColor = AccentRose,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.4f)
            )
        )
    }
}

/**
 * Maps a touch Y (px inside the 160dp track) to an EQ gain in dB.
 * The thumb's centre travels from thumbPx/2 (+8 dB) to height - thumbPx/2 (-8 dB).
 * Snaps to 0.5 dB steps with a small detent around 0 dB.
 */
private fun eqGainForTouch(y: Float, heightPx: Float, thumbPx: Float): Float {
    val travel = (heightPx - thumbPx).coerceAtLeast(1f)
    val fraction = (1f - (y - thumbPx / 2f) / travel).coerceIn(0f, 1f)
    val raw = fraction * 16f - 8f
    return if (raw in -0.3f..0.3f) 0f else Math.round(raw * 2f) / 2f
}
