package com.example.myplayer.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*
import kotlinx.coroutines.launch

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
    var showSpatialDialog by remember { mutableStateOf(false) }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ClayIconButton(onClick = onBack, size = 42.dp) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    "Audio & Playback",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(42.dp))
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
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )
            }

            // 1. Streaming Quality
            item {
                SettingsRowNav(
                    icon = Icons.Default.HighQuality,
                    title = "Streaming Quality",
                    subtitle = "${state.streamingQuality.title} (${state.streamingQuality.subtitle})",
                    onClick = { currentSubScreen = SubScreen.STREAMING_QUALITY }
                )
            }

            // 2. Download Quality
            item {
                SettingsRowNav(
                    icon = Icons.Default.CloudDownload,
                    title = "Download Quality",
                    subtitle = "${state.downloadQuality.title} (${state.downloadQuality.subtitle})",
                    onClick = { currentSubScreen = SubScreen.DOWNLOAD_QUALITY }
                )
            }

            // 3. Bit-Perfect Mode
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Tune,
                    title = "Bit-Perfect Mode (Direct DAC)",
                    subtitle = "Bypass DSP for bit-exact hardware output",
                    checked = state.bitPerfectMode,
                    onCheckedChange = { scope.launch { store.setBitPerfect(it) } }
                )
            }

            // 4. Crossfade
            item {
                SettingsRowToggle(
                    icon = Icons.Default.GraphicEq,
                    title = "Crossfade",
                    subtitle = "Blend the end of a track into the next",
                    checked = state.crossfadeEnabled,
                    onCheckedChange = { scope.launch { store.setCrossfade(it) } }
                )
            }

            // 5. Keep Screen Awake on Lyrics
            item {
                SettingsRowToggle(
                    icon = Icons.Default.LightMode,
                    title = "Keep Screen Awake on Lyrics",
                    subtitle = "Prevent screen from turning off while lyrics are open",
                    checked = state.keepScreenAwakeLyrics,
                    onCheckedChange = { scope.launch { store.setKeepAwakeLyrics(it) } }
                )
            }

            // 6. Download Synced Lyrics
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Lyrics,
                    title = "Download Synced Lyrics",
                    subtitle = "Save .lrc companion files & embedded tags",
                    checked = state.downloadSyncedLyrics,
                    onCheckedChange = { scope.launch { store.setDownloadSyncedLyrics(it) } }
                )
            }

            // 7. Background Playback (Battery Unrestricted)
            item {
                SettingsRowNav(
                    icon = Icons.Default.Bolt,
                    title = "Background Playback (Battery)",
                    subtitle = if (isBatteryUnrestricted) "Unrestricted • Exempt from background killer"
                    else "Restricted • Tap to exempt from background kill",
                    onClick = {
                        runCatching { context.startActivity(featureManagers.buildBatteryExemptionIntent()) }
                        isBatteryUnrestricted = featureManagers.isBatteryUnrestricted()
                    }
                )
            }

            item {
                Text(
                    "Output & Loudness",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp)
                )
            }

            // 8. USB Exclusive Output
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Usb,
                    title = "USB Exclusive Output",
                    subtitle = if (state.usbExclusiveOutput) "On • Direct Bit-Perfect USB active"
                    else "Off • Needs a USB DAC and Bit-Perfect mode",
                    checked = state.usbExclusiveOutput,
                    onCheckedChange = { scope.launch { store.setUsbExclusive(it) } }
                )
            }

            // 9. Internal Direct Output
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Memory,
                    title = "Internal Direct Output",
                    subtitle = "Direct hardware path without OS resampling",
                    checked = state.internalDirectOutput,
                    onCheckedChange = { scope.launch { store.setInternalDirect(it) } }
                )
            }

            // 10. Software Volume (USB DAC)
            item {
                SettingsRowToggle(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Software Volume (USB DAC)",
                    subtitle = "Use DAC hardware volume when available",
                    checked = state.softwareVolumeUsb,
                    onCheckedChange = { scope.launch { store.setSoftwareVolumeUsb(it) } }
                )
            }

            // 11. Loudness Normalization Dialog Trigger
            item {
                SettingsRowNav(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    title = "Loudness Normalization",
                    subtitle = "${state.loudnessMode.title} • ${state.loudnessMode.description}",
                    onClick = { showLoudnessDialog = true }
                )
            }

            // 12. 15-Band Equalizer Screen Trigger
            item {
                SettingsRowNav(
                    icon = Icons.Default.Equalizer,
                    title = "Equalizer",
                    subtitle = "${if (state.eqEnabled) "On" else "Off"} • ${state.eqPreset.label} • 15-band",
                    onClick = { currentSubScreen = SubScreen.EQUALIZER_15 }
                )
            }

            // 13. System Audio Effects
            item {
                SettingsRowToggle(
                    icon = Icons.Default.SurroundSound,
                    title = "System Audio Effects",
                    subtitle = "Let Dolby / system equalizer process audio",
                    checked = state.systemAudioEffects,
                    onCheckedChange = { scope.launch { store.setSystemAudioEffects(it) } }
                )
            }

            // 13b. Dolby Atmos & 3D Surround
            item {
                SettingsRowToggle(
                    icon = Icons.Default.SurroundSound,
                    title = "Dolby Atmos & 3D Surround",
                    subtitle = if (state.dolbySurroundAtmos) "Active • Multichannel 5.1/7.1 Atmos binaural virtualizer"
                    else "Off • Standard stereo downmix",
                    checked = state.dolbySurroundAtmos,
                    onCheckedChange = { scope.launch { store.setDolbySurroundAtmos(it) } }
                )
            }

            // 13c. Headphone Spatial Audio & Soundstage
            item {
                SettingsRowNav(
                    icon = Icons.Default.Hearing,
                    title = "Headphone Spatial Audio (3D Soundstage)",
                    subtitle = if (state.spatialAudioEnabled) "Active • ${state.spatialMode.title} • ${(state.spatialAmount * 100).toInt()}%"
                    else "Off • Natural headphone stereo",
                    onClick = { showSpatialDialog = true }
                )
            }

            // 14. Studio Master Clarity
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Waves,
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
                    title = "Clarity Output Preset",
                    subtitle = "${state.clarityOutputPreset.title} • ${state.clarityOutputPreset.description}",
                    onClick = { showClarityPresetDialog = true }
                )
            }

            // 16. Clarity Spatial Bypass
            item {
                SettingsRowToggle(
                    icon = Icons.Default.Hearing,
                    title = "Clarity Spatial Bypass",
                    subtitle = if (state.claritySpatialBypass) "On • Bypass Clarity on Atmos/Spatial"
                    else "Off • Clarity processes all sources",
                    checked = state.claritySpatialBypass,
                    onCheckedChange = { scope.launch { store.setClaritySpatialBypass(it) } }
                )
            }
        }
    }

    // LOUDNESS NORMALIZATION DIALOG
    if (showLoudnessDialog) {
        Dialog(onDismissRequest = { showLoudnessDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Loudness\nNormalization",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Evens out volume jumps between tracks. Bypassed in Bit-Perfect and USB exclusive modes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LoudnessMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { scope.launch { store.setLoudnessMode(mode) } }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.loudnessMode == mode,
                                onClick = { scope.launch { store.setLoudnessMode(mode) } },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    mode.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    mode.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showLoudnessDialog = false },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // CLARITY OUTPUT PRESET DIALOG
    if (showClarityPresetDialog) {
        Dialog(onDismissRequest = { showClarityPresetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Clarity Output\nPreset",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Tunes the Studio Master Clarity chain to the output. Reference is the unmodified shipping curve.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    ClarityOutputPreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { scope.launch { store.setClarityOutputPreset(preset) } }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.clarityOutputPreset == preset,
                                onClick = { scope.launch { store.setClarityOutputPreset(preset) } },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    preset.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    preset.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showClarityPresetDialog = false },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3D SPATIAL AUDIO DIALOG
    if (showSpatialDialog) {
        Dialog(onDismissRequest = { showSpatialDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "3D Spatial Audio\n& Soundstage",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Projects headphone audio outside your head using Brown & Duda's spherical head model, eliminating in-head fatigue.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Enable Spatial Audio",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = state.spatialAudioEnabled,
                            onCheckedChange = { scope.launch { store.setSpatialAudioEnabled(it) } }
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "Virtualization Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SpatialMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { scope.launch { store.setSpatialMode(mode) } }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.spatialMode == mode,
                                onClick = { scope.launch { store.setSpatialMode(mode) } }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    mode.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (state.spatialMode == SpatialMode.SPEAKERS) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Virtual Speaker Angle", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "±${state.spatialSpeakerAngle.toInt()}°",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = state.spatialSpeakerAngle,
                            onValueChange = { scope.launch { store.setSpatialSpeakerAngle(it) } },
                            valueRange = 15f..60f,
                            steps = 9
                        )
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Spatial Intensity", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${(state.spatialAmount * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.spatialAmount,
                        onValueChange = { scope.launch { store.setSpatialAmount(it) } },
                        valueRange = 0f..1f
                    )

                    TextButton(
                        onClick = { showSpatialDialog = false },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * STREAMING & DOWNLOAD QUALITY SUB-SCREEN
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconButton(onClick = onBack, size = 42.dp) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Audio & Playback",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                            .clayConcave(borderRadius = 12.dp, backgroundColor = MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isDownload) Icons.Default.CloudDownload else Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                val actionWord = if (isDownload) "downloads" else "plays"
                Text(
                    "YouTube Music $actionWord strictly as-is (never upgrades). Dolby Atmos $actionWord only when selected — otherwise it never $actionWord. Atmos falls back to Hi-Res → CD → 320 kbps → YouTube. Hi-Res steps down to lower lossless tiers, then YouTube last.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            itemsIndexed(AudioQualityTier.entries) { _, tier ->
                val isSelected = selected == tier
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(
                            borderRadius = 18.dp,
                            backgroundColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            } else Modifier
                        )
                        .clickable { onSelect(tier) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tier.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    tier.badge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tier.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 15-BAND EQUALIZER SCREEN
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    ClayIconButton(onClick = onBack, size = 42.dp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Equalizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "15-band hardware acoustic tuning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .claySurface(borderRadius = 20.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onReset() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Reset",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
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
                        .claySurface(borderRadius = 20.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Enable Equalizer",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Shaping your music in real-time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.eqEnabled,
                        onCheckedChange = onToggleEq,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }

            // Real-Time 15-Point Frequency Response Curve Canvas
            item {
                val lineColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .claySurface(borderRadius = 18.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f
                        drawLine(outlineColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 2f)

                        val bands = state.eqBandGains
                        val stepX = w / (bands.size - 1).coerceAtLeast(1)
                        val path = Path()

                        bands.forEachIndexed { idx, db ->
                            val x = idx * stepX
                            val y = midY - (db / 12f) * (midY * 0.85f)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                        bands.forEachIndexed { idx, db ->
                            val x = idx * stepX
                            val y = midY - (db / 12f) * (midY * 0.85f)
                            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
                        }
                    }
                    Text(
                        "+12dB",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    Text(
                        "0dB",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    Text(
                        "-12dB",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // Presets FlowRow
            item {
                Text(
                    "Presets",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
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
                        .claySurface(borderRadius = 20.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("+8 dB (Boost)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("-8 dB (Cut)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(MasterAudioDspProcessor.BAND_LABELS) { index, label ->
                            val currentGain = state.eqBandGains.getOrElse(index) { 0f }
                            MyPlayerVerticalFader(
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun MyPlayerVerticalFader(
    gainDb: Float,
    label: String,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val normalized = ((gainDb + 8f) / 16f).coerceIn(0f, 1f)
    val latestGain by rememberUpdatedState(gainDb)
    val latestOnGainChange by rememberUpdatedState(onGainChange)

    Column(
        modifier = modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain value Pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                !enabled -> MaterialTheme.colorScheme.surface
                gainDb != 0f -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ) {
            Text(
                text = if (gainDb > 0f) "+${"%.1f".format(gainDb)}" else "%.1f".format(gainDb),
                fontSize = 11.sp,
                fontWeight = if (gainDb != 0f) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    gainDb != 0f -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(22.dp))
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
                    .background(MaterialTheme.colorScheme.outlineVariant)
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "=",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Frequency Label
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun SettingsRowNav(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 20.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clayConcave(borderRadius = 12.dp, backgroundColor = MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRowToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 20.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clayConcave(borderRadius = 12.dp, backgroundColor = MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

private fun eqGainForTouch(y: Float, heightPx: Float, thumbPx: Float): Float {
    val travel = (heightPx - thumbPx).coerceAtLeast(1f)
    val fraction = (1f - (y - thumbPx / 2f) / travel).coerceIn(0f, 1f)
    val raw = fraction * 16f - 8f
    return if (raw in -0.3f..0.3f) 0f else Math.round(raw * 2f) / 2f
}
