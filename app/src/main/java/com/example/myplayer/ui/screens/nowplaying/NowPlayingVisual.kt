package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myplayer.data.local.entity.SongEntity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ── Cinematic Dark Radial Palette ────────────────────────────────────────────
private val PlayerBlack     = Color(0xFF171C26)
private val PlayerDarkNavy  = Color(0xFF0F141D)
private val PlayerSurface   = Color(0xFF1C2330)
private val PlayerGrey      = Color(0xFF6C7890)
private val PlayerMuted     = Color(0xFFA4AAB7)
private val PlayerWhite     = Color(0xFFFFFFFF)
private val GlowCyan        = Color(0xFF88B8E6)
private val AccentPillColor = Color(0xFFC4E0FA)

/**
 * High-end Dark Radial Player UI inspired by modern cinematic audio controllers.
 * Features a full-bleed atmospheric album background, upper curved progress arc with thumb seek,
 * apex favorite heart, concentric vortex soundwave rings, and a bottom sheet lyrics drawer.
 */
@Composable
fun NowPlayingVisual(
    title: String?,
    artist: String?,
    artUri: String?,
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    isFavorite: Boolean = false,
    isShuffleEnabled: Boolean = false,
    repeatMode: Int = 0,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onFavorite: () -> Unit = {},
    onShuffle: () -> Unit = {},
    onRepeat: () -> Unit = {},
    onBack: () -> Unit = {},
    onOptions: () -> Unit = {},
    modifier: Modifier = Modifier,
    lyricsContent: @Composable () -> Unit = {}
) {
    var lyricsVisible by remember { mutableStateOf(false) }
    var verticalDrag by remember { mutableFloatStateOf(0f) }

    val duration = durationMs.coerceAtLeast(1L)
    var isDraggingProgress by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val rawProgress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val displayProgress = if (isDraggingProgress) dragFraction else rawProgress

    val animatedProgress by animateFloatAsState(
        targetValue = displayProgress,
        animationSpec = spring(stiffness = 240f),
        label = "animatedProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerBlack)
            .pointerInput(lyricsVisible) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount ->
                        verticalDrag += amount
                    },
                    onDragEnd = {
                        if (verticalDrag < -45f) {
                            lyricsVisible = true
                        } else if (verticalDrag > 45f) {
                            lyricsVisible = false
                        }
                        verticalDrag = 0f
                    },
                    onDragCancel = {
                        verticalDrag = 0f
                    }
                )
            }
    ) {
        // ── 1. Top Atmospheric Album Art Background (62% of Screen) ───────────
        if (!artUri.isNullOrBlank()) {
            AsyncImage(
                model = artUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.64f)
                    .align(Alignment.TopCenter)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.64f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2B374A), PlayerDarkNavy, PlayerBlack),
                            radius = 600f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PlayerMuted.copy(alpha = 0.35f),
                    modifier = Modifier.size(96.dp)
                )
            }
        }

        // ── 2. Cinematic Dark Fog & Vignette Overlays ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to PlayerBlack.copy(alpha = 0.40f),
                            0.25f to Color.Transparent,
                            0.50f to PlayerBlack.copy(alpha = 0.45f),
                            0.80f to PlayerBlack.copy(alpha = 0.88f),
                            1f to PlayerBlack
                        )
                    )
                )
        )

        // ── 3. Bottom Radial Spherical Dome Console ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.56f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 200.dp, topEnd = 200.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF263244),
                            PlayerDarkNavy,
                            PlayerBlack
                        ),
                        radius = 850f,
                        center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    BorderStroke(1.dp, Brush.verticalGradient(listOf(PlayerWhite.copy(alpha = 0.20f), Color.Transparent))),
                    RoundedCornerShape(topStart = 200.dp, topEnd = 200.dp)
                )
        )

        // ── 4. Main Player Controls & Content ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Action Bar (Glass Circular Buttons) ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (<)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(BorderStroke(1.dp, PlayerWhite.copy(alpha = 0.22f)), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = PlayerWhite),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = PlayerWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // More Options Button (...)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(BorderStroke(1.dp, PlayerWhite.copy(alpha = 0.22f)), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = PlayerWhite),
                            onClick = onOptions
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Options",
                        tint = PlayerWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.9f))

            // ── Song Title & Artist Info ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title?.ifBlank { "No song playing" } ?: "No song playing",
                    color = PlayerWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
                    color = PlayerMuted,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Curved Radial Progress Arc & Apex Favorite Heart ──────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val startAngle = 145f
                val sweepAngle = 250f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(duration) {
                            detectTapGestures { offset ->
                                val arcW = size.width * 0.88f
                                val arcH = size.height * 2.2f
                                val cx = size.width / 2f
                                val cy = arcH / 2f + 10.dp.toPx()
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                if (angle < 90f) angle += 360f
                                val p = ((angle - startAngle) / sweepAngle).coerceIn(0f, 1f)
                                onSeek((p * duration).toLong())
                            }
                        }
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDraggingProgress = true
                                    val arcW = size.width * 0.88f
                                    val arcH = size.height * 2.2f
                                    val cx = size.width / 2f
                                    val cy = arcH / 2f + 10.dp.toPx()
                                    val dx = offset.x - cx
                                    val dy = offset.y - cy
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    if (angle < 90f) angle += 360f
                                    dragFraction = ((angle - startAngle) / sweepAngle).coerceIn(0f, 1f)
                                },
                                onDrag = { change, _ ->
                                    val arcW = size.width * 0.88f
                                    val arcH = size.height * 2.2f
                                    val cx = size.width / 2f
                                    val cy = arcH / 2f + 10.dp.toPx()
                                    val dx = change.position.x - cx
                                    val dy = change.position.y - cy
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    if (angle < 90f) angle += 360f
                                    dragFraction = ((angle - startAngle) / sweepAngle).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    onSeek((dragFraction * duration).toLong())
                                    isDraggingProgress = false
                                },
                                onDragCancel = {
                                    isDraggingProgress = false
                                }
                            )
                        }
                ) {
                    val strokeW = 3.2.dp.toPx()
                    val arcW = size.width * 0.88f
                    val arcH = size.height * 2.2f
                    val left = (size.width - arcW) / 2f
                    val top = 10.dp.toPx()

                    // Background Inactive Arc Track
                    drawArc(
                        color = PlayerGrey.copy(alpha = 0.38f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(arcW, arcH),
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )

                    // Active Played Glow Arc
                    drawArc(
                        color = PlayerWhite,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(arcW, arcH),
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )

                    // Position of White Circular Thumb on the Arc
                    val currentAngleDeg = startAngle + (sweepAngle * animatedProgress)
                    val rad = Math.toRadians(currentAngleDeg.toDouble())
                    val cx = size.width / 2f
                    val cy = top + arcH / 2f
                    val rx = arcW / 2f
                    val ry = arcH / 2f
                    val thumbX = cx + rx * cos(rad).toFloat()
                    val thumbY = cy + ry * sin(rad).toFloat()

                    // Thumb outer ambient halo
                    drawCircle(
                        color = GlowCyan.copy(alpha = 0.35f),
                        radius = 12.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                    // Thumb solid white core
                    drawCircle(
                        color = PlayerWhite,
                        radius = 7.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                }

                // Heart Icon Centered Directly Below Apex
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else PlayerWhite.copy(alpha = 0.60f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Shuffle, Segmented Accent Pill, Repeat Row ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button (Circular dark pill)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PlayerSurface)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isShuffleEnabled) PlayerWhite.copy(alpha = 0.45f) else PlayerWhite.copy(alpha = 0.12f)
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = PlayerWhite),
                            onClick = onShuffle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) PlayerWhite else PlayerMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Segmented Curved Highlight Pill Above Central Wheel
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentPillColor.copy(alpha = 0.65f))
                )

                // Repeat Button (Circular dark pill)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PlayerSurface)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (repeatMode > 0) PlayerWhite.copy(alpha = 0.45f) else PlayerWhite.copy(alpha = 0.12f)
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = PlayerWhite),
                            onClick = onRepeat
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val repeatIcon = if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) PlayerWhite else PlayerMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Central Transport Pod: [|◀]  (  [▶ / ⏸]  )  [▶|] ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = PlayerWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Central Hero Glowing Play/Pause Button in Concentric Soundwave Aura
                Box(
                    modifier = Modifier.size(118.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Vortex / Concentric Wave Glow
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GlowCyan.copy(alpha = 0.38f),
                                    Color(0xFF263346).copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                        // Concentric Soundwave Rings
                        drawCircle(
                            color = GlowCyan.copy(alpha = 0.45f),
                            style = Stroke(1.5.dp.toPx()),
                            radius = size.minDimension * 0.46f
                        )
                        drawCircle(
                            color = PlayerWhite.copy(alpha = 0.25f),
                            style = Stroke(1.dp.toPx()),
                            radius = size.minDimension * 0.41f
                        )
                    }

                    // Solid White Inner Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(
                                elevation = 22.dp,
                                shape = CircleShape,
                                spotColor = GlowCyan,
                                ambientColor = PlayerWhite.copy(alpha = 0.5f)
                            )
                            .clip(CircleShape)
                            .background(PlayerWhite)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = PlayerBlack),
                                onClick = onPlayPause
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = PlayerBlack,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Skip Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = PlayerWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Timestamps (Current Position & Duration) ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPos = if (isDraggingProgress) (dragFraction * duration).toLong() else positionMs
                Text(
                    text = formatTime(currentPos),
                    color = PlayerWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatTime(duration),
                    color = PlayerMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.weight(0.7f))

            // ── Swipe-Up Hint for Lyrics ──────────────────────────────────────
            if (!lyricsVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { lyricsVisible = true }
                        )
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Swipe up for lyrics",
                        tint = PlayerMuted.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "SWIPE UP FOR LYRICS",
                        color = PlayerMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // ── 5. Sliding Lyrics Overlay Sheet (Revealed on Swipe Up) ────────────
        AnimatedVisibility(
            visible = lyricsVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PlayerSurface.copy(alpha = 0.98f),
                                PlayerDarkNavy.copy(alpha = 0.99f),
                                PlayerBlack
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, PlayerWhite.copy(alpha = 0.15f)),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lyrics",
                        color = PlayerWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { lyricsVisible = false }) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Hide lyrics",
                            tint = PlayerWhite
                        )
                    }
                }

                HorizontalDivider(color = PlayerMuted.copy(alpha = 0.25f))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    lyricsContent()
                }
            }
        }
    }
}

/**
 * Overload accepting [SongEntity] directly for clean standalone consumption.
 */
@Composable
fun NowPlayingVisual(
    song: SongEntity,
    isPlaying: Boolean,
    progress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    isFavorite: Boolean = false,
    isShuffleEnabled: Boolean = false,
    repeatEnabled: Boolean = false,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onFavorite: () -> Unit = {},
    onShuffle: () -> Unit = {},
    onRepeat: () -> Unit = {},
    onBack: () -> Unit = {},
    onQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
    lyricsContent: @Composable () -> Unit
) {
    NowPlayingVisual(
        title = song.title,
        artist = song.artist,
        artUri = song.albumArt,
        durationMs = durationMs,
        positionMs = currentPositionMs,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        isShuffleEnabled = isShuffleEnabled,
        repeatMode = if (repeatEnabled) 1 else 0,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onSeek = { frac -> onSeek(frac.toFloat() / durationMs.coerceAtLeast(1L).toFloat()) },
        onFavorite = onFavorite,
        onShuffle = onShuffle,
        onRepeat = onRepeat,
        onBack = onBack,
        onOptions = onQueue,
        modifier = modifier,
        lyricsContent = lyricsContent
    )
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
