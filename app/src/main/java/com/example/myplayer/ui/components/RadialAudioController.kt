package com.example.myplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplayer.ui.theme.*
import kotlin.math.*

/**
 * Signature Radial / Circular Audio Controller inspired by Stitch UI redesign.
 * Features concentric glowing aura waves, a central electric neon lime Play/Pause button,
 * an interactive 270-degree radial seek arc, apex favorite heart, and elapsed / duration time indicators.
 */
@Composable
fun RadialAudioController(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 250.dp
) {
    // positionMs accepted as Long; caller already passes positionState.value so
    // reads are scoped to this composable only (Phase 4 handled in caller).
    val duration = durationMs.coerceAtLeast(1L)
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val currentProgress = if (isDragging) dragProgress else actualProgress

    // Concentric pulsing aura animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "radialAura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Favorite heart bounce
    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )

    Box(
        modifier = modifier
            .size(dialSize)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val strokeWidthPx = with(density) { 5.dp.toPx() }
        val thumbRadiusPx = with(density) { 7.dp.toPx() }

        // ── Phase 2: Pulse-only Canvas (reads pulseScale/pulseAlpha; never triggers arc redraws) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = 38.dp.toPx()
            if (isPlaying) {
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = pulseAlpha * 0.9f),
                    radius = (baseRadius + 14.dp.toPx()) * pulseScale,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = pulseAlpha * 0.5f),
                    radius = (baseRadius + 28.dp.toPx()) * pulseScale,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = NeonLimePrimary.copy(alpha = pulseAlpha * 0.25f),
                    radius = (baseRadius + 42.dp.toPx()) * pulseScale,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
            } else {
                drawCircle(
                    color = CardBorderOlive.copy(alpha = 0.5f),
                    radius = baseRadius + 14.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // ── Phase 1: Arc + gesture Canvas (reads currentProgress only; never reads pulse state) ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(duration) {
                    // Tap: update local progress only; fire seek on release.
                    fun angleToProgress(offset: Offset): Float {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f
                        val relativeAngle = if (angleDeg >= 135f) angleDeg - 135f else (angleDeg + 360f) - 135f
                        return if (relativeAngle <= 270f) (relativeAngle / 270f).coerceIn(0f, 1f)
                        else if (relativeAngle < 315f) 1f else 0f
                    }

                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            dragProgress = angleToProgress(offset)
                            tryAwaitRelease()
                            // Seek fires exactly once — on release.
                            onSeek((dragProgress * duration).toLong())
                            isDragging = false
                        }
                    )
                }
                .pointerInput(duration) {
                    fun angleToProgress(offset: Offset): Float {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f
                        val relativeAngle = if (angleDeg >= 135f) angleDeg - 135f else (angleDeg + 360f) - 135f
                        return if (relativeAngle <= 270f) (relativeAngle / 270f).coerceIn(0f, 1f)
                        else if (relativeAngle < 315f) 1f else 0f
                    }

                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            // Capture start position for immediate visual feedback.
                            dragProgress = angleToProgress(offset)
                        },
                        onDrag = { change, _ ->
                            // Only update local UI state — NO onSeek here.
                            dragProgress = angleToProgress(change.position)
                        },
                        onDragEnd = {
                            // Seek fires exactly once per drag interaction.
                            onSeek((dragProgress * duration).toLong())
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val arcRadius = (min(size.width, size.height) / 2f) - thumbRadiusPx - 4.dp.toPx()

            // 1. Background Inactive Arc (135° to 405°, sweep 270°)
            val arcRectTopLeft = Offset(centerX - arcRadius, centerY - arcRadius)
            val arcRectSize = Size(arcRadius * 2, arcRadius * 2)

            drawArc(
                color = Color(0xFF222C1A),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcRectSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // 3. Active Progress Arc
            val activeSweep = (currentProgress * 270f).coerceIn(0.1f, 270f)
            drawArc(
                color = NeonLimePrimary,
                startAngle = 135f,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcRectSize,
                style = Stroke(width = strokeWidthPx + 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Glowing Thumb Bead
            val currentAngleRad = Math.toRadians((135f + activeSweep).toDouble())
            val thumbX = (centerX + arcRadius * cos(currentAngleRad)).toFloat()
            val thumbY = (centerY + arcRadius * sin(currentAngleRad)).toFloat()

            // Outer glow
            drawCircle(
                color = NeonLimePrimary.copy(alpha = 0.35f),
                radius = thumbRadiusPx * 1.6f,
                center = Offset(thumbX, thumbY)
            )
            // Solid thumb
            drawCircle(
                color = NeonLimePrimary,
                radius = thumbRadiusPx,
                center = Offset(thumbX, thumbY)
            )
            // Center highlight
            drawCircle(
                color = Color.White,
                radius = thumbRadiusPx * 0.45f,
                center = Offset(thumbX, thumbY)
            )
        }

        // ── Apex Favorite Heart (Top center of the circular dial) ─────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleFavorite
                )
                .scale(heartScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) NeonLimePrimary else OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Central Play/Pause Button (Hero neon lime circle) ─────────────────
        Box(
            modifier = Modifier
                .size(74.dp)
                .shadow(
                    elevation = if (isPlaying) 16.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = NeonLimePrimary
                )
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

        // ── Time Labels: Elapsed (Left) & Total Duration (Right) ───────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 2.dp)
        ) {
            val displayedPositionMs = if (isDragging) (dragProgress * duration).toLong() else positionMs
            Text(
                text = formatDuration(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val s = durationMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

