package com.example.myplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplayer.ui.theme.*
import kotlin.math.*

/**
 * Signature Radial / Circular Audio Controller from Stitch UI Redesign.
 * Features:
 * - Circular arc seekbar (135° to 405°, 270° sweep) with drag & tap scrub support.
 * - Glowing green favorite heart icon at top crest (270°).
 * - Central Neon Lime Play/Pause button with concentric pulsating sound wave rings.
 * - Current position & total duration timestamps at bottom-left and bottom-right.
 */
@Composable
fun RadialAudioController(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(1L)
    val actualProgress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val displayedProgress = if (isDragging) dragProgress else actualProgress

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 6.dp.toPx() }
    val thumbRadiusPx = with(density) { 7.dp.toPx() }

    // Pulsing aura animation for sound waves
    val infiniteTransition = rememberInfiniteTransition(label = "radialAura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Helper to calculate progress from touch coordinates
    fun calculateProgressFromOffset(offset: Offset, sizePx: Float): Float {
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val dx = offset.x - centerX
        val dy = offset.y - centerY
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0) angle += 360f

        // Arc runs from 135° clockwise to 405° (45°), sweep = 270°
        // Bottom gap is from 45° to 135° (90°)
        if (angle in 45f..135f) {
            return if (angle < 90f) 1f else 0f
        }
        val adjustedAngle = if (angle < 135f) angle + 360f else angle
        return ((adjustedAngle - 135f) / 270f).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── 1. Arc Canvas (Seekbar & Concentric Rings) ────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val p = calculateProgressFromOffset(offset, size.width.toFloat())
                        onSeek((p * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = calculateProgressFromOffset(offset, size.width.toFloat())
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgress * duration).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = calculateProgressFromOffset(change.position, size.width.toFloat())
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val arcRadius = (min(width, height) / 2f) - strokeWidthPx - 14.dp.toPx()

            // Concentric sound wave rings behind central play button
            if (isPlaying) {
                val basePulse = if (isPlaying) pulseScale else 1f
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = 0.05f),
                    radius = 82.dp.toPx() * basePulse,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = 0.12f),
                    radius = 66.dp.toPx() * basePulse,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = 0.22f),
                    radius = 52.dp.toPx() * basePulse,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            val arcTopLeft = Offset(centerX - arcRadius, centerY - arcRadius)
            val arcSize = Size(arcRadius * 2, arcRadius * 2)

            // 1. Inactive Track (Dark olive track)
            drawArc(
                color = Color(0xFF24301B),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // 2. Active Track (Neon lime)
            val activeSweep = 270f * displayedProgress
            if (activeSweep > 0.5f) {
                drawArc(
                    color = NeonLimePrimary,
                    startAngle = 135f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // 3. Thumb Bead at the end of active track
                val currentAngleRad = Math.toRadians((135f + activeSweep).toDouble())
                val thumbX = (centerX + arcRadius * cos(currentAngleRad)).toFloat()
                val thumbY = (centerY + arcRadius * sin(currentAngleRad)).toFloat()

                // Glow halo
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = 0.35f),
                    radius = thumbRadiusPx + 4f,
                    center = Offset(thumbX, thumbY)
                )
                // Solid thumb
                drawCircle(
                    color = NeonLimePrimary,
                    radius = thumbRadiusPx,
                    center = Offset(thumbX, thumbY)
                )
                // Center white reflection dot
                drawCircle(
                    color = Color.White,
                    radius = thumbRadiusPx * 0.45f,
                    center = Offset(thumbX, thumbY)
                )
            }
        }

        // ── 2. Top Crest Favorite Heart Button ────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceLight)
                .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleFavorite
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) NeonLimePrimary else OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── 3. Central Play/Pause Action Button ───────────────────────────────
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = NeonLimePrimary)
                .clip(CircleShape)
                .background(NeonLimePrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayPauseClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = OnNeonLime,
                modifier = Modifier.size(38.dp)
            )
        }

        // ── 4. Current Time (Bottom Left of Dial) ─────────────────────────────
        Text(
            text = formatRadialDuration(if (isDragging) (dragProgress * duration).toLong() else positionMs),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = OnSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 10.dp)
        )

        // ── 5. Total Duration (Bottom Right of Dial) ──────────────────────────
        Text(
            text = formatRadialDuration(duration),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = OnSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 10.dp)
        )
    }
}

private fun formatRadialDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
