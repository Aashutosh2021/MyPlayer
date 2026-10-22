package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.playback.equalizer.StudioEqualizerAudioProcessor
import com.example.myplayer.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioEqualizerSheet(
    processor: StudioEqualizerAudioProcessor,
    onDismiss: () -> Unit
) {
    val eqState by processor.state.collectAsStateWithLifecycle()
    val presets = listOf("Normal", "Only Vocals", "Only Instrumental", "Bass Boost", "Lo-Fi Chill")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header + Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Studio Equalizer & Stem Mixer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = { processor.applyPreset("Normal") }) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick Mode Presets (Normal, Only Vocals, Only Instrumental, Bass Boost, Lo-Fi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = eqState.presetName == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                BorderStroke(1.dp, CardBorderOlive),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { processor.applyPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 1. Vocals Slider (0% = No Vocals/Karaoke, 100% = Normal, 150% = Vocal Boost)
            EqSliderRow(
                label = "Vocals (Singer Voice)",
                valueText = "${(eqState.vocalLevel * 100).roundToInt()}%",
                value = eqState.vocalLevel,
                valueRange = 0f..1.5f,
                onValueChange = { processor.setVocalLevel(it) }
            )

            // 2. Instrumentals Slider (0% = Only Vocals, 100% = Normal, 150% = Instrumental Boost)
            EqSliderRow(
                label = "Instrumentals (Background Music)",
                valueText = "${(eqState.instrumentalLevel * 100).roundToInt()}%",
                value = eqState.instrumentalLevel,
                valueRange = 0f..1.5f,
                onValueChange = { processor.setInstrumentalLevel(it) }
            )

            // 3. Bass Slider (-100% to +100%)
            EqSliderRow(
                label = "Bass (Sub & Kick)",
                valueText = "${if (eqState.bassLevel >= 0) "+" else ""}${(eqState.bassLevel * 100).roundToInt()}%",
                value = eqState.bassLevel,
                valueRange = -1f..1f,
                onValueChange = { processor.setBass(it) }
            )

            // 4. Treble Slider (-100% to +100%)
            EqSliderRow(
                label = "Treble (Clarity & Highs)",
                valueText = "${if (eqState.trebleLevel >= 0) "+" else ""}${(eqState.trebleLevel * 100).roundToInt()}%",
                value = eqState.trebleLevel,
                valueRange = -1f..1f,
                onValueChange = { processor.setTreble(it) }
            )

            // 5. Volume Preamp Boost (0% to 200%)
            EqSliderRow(
                label = "Volume Boost (Preamp)",
                valueText = "${(eqState.volumeGain * 100).roundToInt()}%",
                value = eqState.volumeGain,
                valueRange = 0f..2f,
                onValueChange = { processor.setVolumeGain(it) }
            )
        }
    }
}

@Composable
private fun EqSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
