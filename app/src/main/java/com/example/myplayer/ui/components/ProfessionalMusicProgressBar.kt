package com.example.myplayer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplayer.ui.theme.Primary
import com.example.myplayer.ui.theme.Secondary

/**
 * Standard, sleek, professional music progress seeker bar.
 */
@Composable
fun ProfessionalMusicProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isDragging) {
        dragProgress
    } else {
        (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 6.dp,
        animationSpec = tween(150),
        label = "thumbRadius"
    )

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 4.dp,
        animationSpec = tween(150),
        label = "trackHeight"
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((newFraction * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek((dragProgress * duration).toLong())
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f
                val trackH = trackHeight.toPx()
                val radius = CornerRadius(trackH / 2f, trackH / 2f)

                // Background track
                drawRoundRect(
                    color = Color(0xFFE2E4EB),
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(canvasWidth, trackH),
                    cornerRadius = radius
                )

                // Active played track
                val activeWidth = (canvasWidth * currentProgress).coerceIn(0f, canvasWidth)
                if (activeWidth > 0f) {
                    drawRoundRect(
                        color = Primary,
                        topLeft = Offset(0f, centerY - trackH / 2f),
                        size = Size(activeWidth, trackH),
                        cornerRadius = radius
                    )
                }

                // Scrubber thumb
                val thumbX = activeWidth.coerceIn(0f, canvasWidth)
                val thumbR = thumbRadius.toPx()

                // Subtle shadow when dragging
                if (isDragging) {
                    drawCircle(
                        color = Color(0x33000000),
                        radius = thumbR + 3.dp.toPx(),
                        center = Offset(thumbX, centerY)
                    )
                }

                // Solid primary thumb
                drawCircle(
                    color = Primary,
                    radius = thumbR,
                    center = Offset(thumbX, centerY)
                )

                // Crisp inner white dot
                drawCircle(
                    color = Color.White,
                    radius = thumbR * 0.4f,
                    center = Offset(thumbX, centerY)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Time indicators (Elapsed on left, Total on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayMs = if (isDragging) (dragProgress * duration).toLong() else positionMs
            Text(
                text = formatDuration(displayMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 0.2.sp
                ),
                color = Secondary
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 0.2.sp
                ),
                color = Secondary
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
