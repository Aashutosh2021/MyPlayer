package com.myplayer.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myplayer.app.data.preferences.*

private val BgDarkOlive = Color(0xFF12160E)
private val SurfaceOlive = Color(0xFF1A2216)
private val AccentNeonLime = Color(0xFFD2F83A)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioPlaybackSettingsSection(
    config: AudioPlaybackConfig,
    onWifiQualityChange: (StreamQuality) -> Unit,
    onMobileQualityChange: (StreamQuality) -> Unit,
    onClarityChange: (ClarityPreset) -> Unit,
    onDolbyToggle: (Boolean, Float) -> Unit,
    onLoudnessToggle: (Boolean, Int) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDarkOlive)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AUDIO & PLAYBACK ENGINE",
            color = AccentNeonLime,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. Streaming Quality Card (Wi-Fi & Mobile Data)
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceOlive),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Streaming Audio Quality (YouTube / NewPipe)", color = Color.White, fontWeight = FontWeight.SemiBold)

                Text("Wi-Fi Streaming", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = config.wifiQuality == quality,
                            onClick = { onWifiQualityChange(quality) },
                            label = { Text(quality.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentNeonLime,
                                selectedLabelColor = BgDarkOlive
                            )
                        )
                    }
                }

                Text("Mobile Data Streaming", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = config.mobileQuality == quality,
                            onClick = { onMobileQualityChange(quality) },
                            label = { Text(quality.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentNeonLime,
                                selectedLabelColor = BgDarkOlive
                            )
                        )
                    }
                }
            }
        }

        // 2. LastWave Clarity Presets & True-Dolby Spatial Audio
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceOlive),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Clarity & Spatial DSP", color = Color.White, fontWeight = FontWeight.SemiBold)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClarityPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = config.clarityPreset == preset,
                            onClick = { onClarityChange(preset) },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentNeonLime,
                                selectedLabelColor = BgDarkOlive
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("True-Dolby Spatial Widening", color = Color.White)
                        Text("3D Mid-Side soundstage expansion (Auto-pauses in Dual Bud Mode)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = config.trueDolbySpatial,
                        onCheckedChange = { onDolbyToggle(it, config.spatialWidth) },
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDarkOlive, checkedTrackColor = AccentNeonLime)
                    )
                }

                if (config.trueDolbySpatial) {
                    Text("Spatial Soundstage Width: ${(config.spatialWidth * 100).toInt()}%", color = AccentNeonLime, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = config.spatialWidth,
                        onValueChange = { onDolbyToggle(true, it) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentNeonLime, activeTrackColor = AccentNeonLime)
                    )
                }
            }
        }

        // 3. Loudness Normalization & Skip Silence
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceOlive),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Loudness Normalization", color = Color.White)
                        Text("Boost quiet tracks & balance volume (+${config.loudnessGainMb / 100} dB)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = config.loudnessNormalize,
                        onCheckedChange = { onLoudnessToggle(it, config.loudnessGainMb) },
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDarkOlive, checkedTrackColor = AccentNeonLime)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Skip Silence", color = Color.White)
                        Text("Automatically trim dead air in tracks", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = config.skipSilence,
                        onCheckedChange = onSkipSilenceToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = BgDarkOlive, checkedTrackColor = AccentNeonLime)
                    )
                }
            }
        }
    }
}

/**
 * Modern modal dialog wrapper to present Audio & Playback settings.
 */
@Composable
fun AudioPlaybackSettingsDialog(
    config: AudioPlaybackConfig,
    onDismiss: () -> Unit,
    onWifiQualityChange: (StreamQuality) -> Unit,
    onMobileQualityChange: (StreamQuality) -> Unit,
    onClarityChange: (ClarityPreset) -> Unit,
    onDolbyToggle: (Boolean, Float) -> Unit,
    onLoudnessToggle: (Boolean, Int) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = BgDarkOlive
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio & Playback",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        AudioPlaybackSettingsSection(
                            config = config,
                            onWifiQualityChange = onWifiQualityChange,
                            onMobileQualityChange = onMobileQualityChange,
                            onClarityChange = onClarityChange,
                            onDolbyToggle = onDolbyToggle,
                            onLoudnessToggle = onLoudnessToggle,
                            onSkipSilenceToggle = onSkipSilenceToggle
                        )
                    }
                }
            }
        }
    }
}
