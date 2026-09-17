package com.example.myplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myplayer.ui.theme.ClayPrimary
import com.example.myplayer.ui.theme.ClaySecondary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Expressive animated Wavy Seekbar inspired by LastWave v4.1.
 * Features an undulating sinusoidal audio wave when music is actively playing,
 * and seamlessly flattens to a clean straight line when paused or scrubbed.
 */
@Composable
fun ClayWavySeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = ClayPrimary,
    inactiveColor: Color = ClayPrimary.copy(alpha = 0.2f),
    waveHeight: Dp = 5.dp,
    waveLength: Dp = 26.dp,
    strokeWidth: Dp = 4.dp
) {
    val density = LocalDensity.current
    val waveHeightPx = with(density) { waveHeight.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { 7.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayedProgress = if (isDragging) dragProgress else actualProgress

    // Animate wave amplitude: full amplitude while playing, flat (0f) when paused or scrubbing
    val amplitudeTransition by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) waveHeightPx else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavyAmplitude"
    )

    // Continuous smooth phase progression when active
    val infiniteTransition = rememberInfiniteTransition(label = "wavePhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(durationMs) {
                detectTapGestures(
                    onPress = { offset ->
                        val tapProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((tapProgress * durationMs.coerceAtLeast(1L)).toLong())
                    }
                )
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((dragProgress * durationMs.coerceAtLeast(1L)).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val delta = dragAmount / size.width.toFloat()
                        dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val activeEndPx = (displayedProgress * width).coerceIn(0f, width)

            // 1. Draw Inactive Track (Remaining part of the song)
            if (activeEndPx < width) {
                drawLine(
                    color = inactiveColor,
                    start = androidx.compose.ui.geometry.Offset(activeEndPx, centerY),
                    end = androidx.compose.ui.geometry.Offset(width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw Active Track (Sinusoidal wave or straight line based on amplitude)
            if (activeEndPx > 0f) {
                val activePath = Path()
                activePath.moveTo(0f, centerY)

                if (amplitudeTransition > 0.5f) {
                    var x = 0f
                    val stepPx = 3f
                    while (x <= activeEndPx) {
                        val angle = (x / waveLengthPx) * 2f * PI.toFloat() - phase
                        val y = centerY + sin(angle) * amplitudeTransition
                        activePath.lineTo(x, y)
                        x += stepPx
                    }
                    // Ensure line reaches exact active end point
                    val finalAngle = (activeEndPx / waveLengthPx) * 2f * PI.toFloat() - phase
                    val finalY = centerY + sin(finalAngle) * amplitudeTransition
                    activePath.lineTo(activeEndPx, finalY)
                } else {
                    activePath.lineTo(activeEndPx, centerY)
                }

                drawPath(
                    path = activePath,
                    color = activeColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // 3. Draw Thumb Head
                val thumbY = if (amplitudeTransition > 0.5f) {
                    val finalAngle = (activeEndPx / waveLengthPx) * 2f * PI.toFloat() - phase
                    centerY + sin(finalAngle) * amplitudeTransition
                } else centerY

                // Outer soft halo
                drawCircle(
                    color = activeColor.copy(alpha = 0.25f),
                    radius = thumbRadiusPx + 4f,
                    center = androidx.compose.ui.geometry.Offset(activeEndPx, thumbY)
                )
                // Solid thumb circle
                drawCircle(
                    color = activeColor,
                    radius = thumbRadiusPx,
                    center = androidx.compose.ui.geometry.Offset(activeEndPx, thumbY)
                )
                // Inner bright dot
                drawCircle(
                    color = Color.White,
                    radius = thumbRadiusPx * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(activeEndPx, thumbY)
                )
            }
        }
    }
}
