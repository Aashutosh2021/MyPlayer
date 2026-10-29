package com.example.myplayer.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.ui.common.AlbumArtImage
import kotlin.math.roundToLong

// Signature Purple-to-Magenta Gradient from Reference Image (54566.jpg)
private val MiniPurpleStart = Color(0xFF5D218E)
private val MiniPurpleMid = Color(0xFF831886)
private val MiniMagentaEnd = Color(0xFFA70F7B)
private val MiniThumbCore = Color(0xFF24093A)

@Composable
fun MiniPlayer(
    song: SongEntity,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // Automatically observe live position, duration, favorite & repeat state from MainViewModel
    val livePositionMs by mainViewModel.currentPosition.collectAsStateWithLifecycle()
    val liveDurationMs by mainViewModel.currentDuration.collectAsStateWithLifecycle()
    val isFavorite by mainViewModel.isFavorite.collectAsStateWithLifecycle()
    val repeatMode by mainViewModel.repeatMode.collectAsStateWithLifecycle()

    val effectiveDurationMs = remember(song.duration, liveDurationMs) {
        when {
            liveDurationMs > 0L -> liveDurationMs
            song.duration > 0L -> song.duration
            else -> 1L
        }
    }
    val progress = (livePositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)

    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "miniSwipe"
    )

    // Outer container with top padding (18.dp) so the top-left Album Art pops out above the purple card!
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .graphicsLayer {
                translationX = animatedOffsetX * 0.35f
                rotationZ = (animatedOffsetX / 60f).coerceIn(-2.5f, 2.5f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-220f, 220f)
                    },
                    onDragEnd = {
                        if (swipeOffsetX < -100f) {
                            onNextClick()
                        } else if (swipeOffsetX > 100f) {
                            onPreviousClick()
                        }
                        swipeOffsetX = 0f
                    },
                    onDragCancel = { swipeOffsetX = 0f }
                )
            }
    ) {
        // 1. Main Purple-Magenta Gradient Card (shifted down by 16.dp so artwork overhangs top-left)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MiniPurpleStart, MiniPurpleMid, MiniMagentaEnd)
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayerClick
                )
                .padding(top = 10.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
        ) {
            // Top Section: Title & "by Artist" (placed to the right of the protruding Album Art)
            AnimatedContent(
                targetState = song.title to song.artist,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 114.dp),
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut(tween(150)))
                },
                label = "miniTitleArtistAnim"
            ) { (title, artist) ->
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = title.ifBlank { "Unknown Track" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color.White.copy(alpha = 0.80f),
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append("by  ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(artist.ifBlank { "Unknown Artist" })
                            }
                        },
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Bottom Section: Left Interactive Seekbar (0:59 — 3:22) + Right 5 Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Half: Custom Ring-Thumb Seekbar + Time Labels (0:59 ... 3:22)
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .padding(end = 12.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .pointerInput(effectiveDurationMs) {
                                detectTapGestures { offset ->
                                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    mainViewModel.seekTo((frac * effectiveDurationMs).roundToLong())
                                }
                            }
                    ) {
                        val w = size.width
                        val cy = size.height / 2f
                        val thumbX = (progress * w).coerceIn(7.dp.toPx(), (w - 7.dp.toPx()).coerceAtLeast(7.dp.toPx()))

                        // Full white track line
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f),
                            start = Offset(0f, cy),
                            end = Offset(w, cy),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Active progress line
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, cy),
                            end = Offset(thumbX, cy),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Signature Ring Thumb (White outer ring + dark purple center core)
                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx(),
                            center = Offset(thumbX, cy)
                        )
                        drawCircle(
                            color = MiniThumbCore,
                            radius = 4.dp.toPx(),
                            center = Offset(thumbX, cy)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMiniPlayerTime(livePositionMs),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.94f)
                        )
                        Text(
                            text = formatMiniPlayerTime(effectiveDurationMs),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.94f)
                        )
                    }
                }

                // Right Half: Heart | Previous | Solid White Circle Play/Pause | Next | Repeat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Favorite Heart
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { mainViewModel.toggleFavorite() }
                            )
                    )

                    // 2. Skip Previous
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPreviousClick
                            )
                    )

                    // 3. Center Solid White Circle Play/Pause Button with Purple Icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
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
                            tint = MiniPurpleMid,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // 4. Skip Next
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onNextClick
                            )
                    )

                    // 5. Repeat Mode
                    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color.White else Color.White.copy(alpha = 0.78f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { mainViewModel.cycleRepeatMode() }
                            )
                    )
                }
            }
        }

        // 2. Protruding Top-Left Rounded Rectangular Album Art (Juts 16.dp above the purple card!)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp)
                .size(width = 102.dp, height = 70.dp)
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF28123D))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayerClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtImage(
                uri = song.albumArt,
                title = song.title,
                artist = song.artist,
                size = 102.dp,
                shape = RoundedCornerShape(18.dp),
                iconSize = 28.dp
            )
        }
    }
}

private fun formatMiniPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}