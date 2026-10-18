package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myplayer.data.local.entity.SongEntity
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Exact Reference Color Palette ────────────────────────────────────────────
private val PlayerBlack     = Color(0xFF171C26)
private val PlayerDarkNavy  = Color(0xFF0F141D)
private val PlayerSurface   = Color(0xFF1C2330)
private val PlayerGrey      = Color(0xFF6C7890)
private val PlayerMuted     = Color(0xFFA4AAB7)
private val PlayerWhite     = Color(0xFFFFFFFF)
private val GlowCyan        = Color(0xFF88B8E6)
private val AccentPillColor = Color(0xFFC4E0FA)

/**
 * Premium Dark Radial Player UI.
 * 1. Progress arc is locked directly on the circle border of the spherical dome.
 * 2. Like button is aligned directly below the top apex of the arc.
 * 3. Integrated vertical scroll: Swiping up scrolls the entire screen upward to show
 *    the synced lyrics directly below the transport controls (no separate sliding drawer).
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
    val duration = durationMs.coerceAtLeast(1L)
    var isDraggingProgress by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val rawProgress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val displayProgress = if (isDraggingProgress) dragFraction else rawProgress

    val animatedProgress by animateFloatAsState(
        targetValue = displayProgress,
        animationSpec = spring(stiffness = 260f),
        label = "radialProgress"
    )

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerBlack)
            .verticalScroll(scrollState)
    ) {
        // ── 1. Hero Player Viewport (Fills initial screen height) ─────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight)
        ) {
            // Album Art Background (Fills top ~62% of initial viewport)
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

            // Cinematic Dark Gradient Overlay (blends artwork smoothly into background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to PlayerBlack.copy(alpha = 0.40f),
                                0.22f to Color.Transparent,
                                0.50f to PlayerBlack.copy(alpha = 0.45f),
                                0.78f to PlayerBlack.copy(alpha = 0.88f),
                                1f to PlayerBlack
                            )
                        )
                    )
            )

            // Top Bar: Back (<) and Options (...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Back Button
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

                // Circular Options Button
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

            // Song Title & Artist (Centered in upper area over artwork)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = screenHeight * 0.30f),
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

            // ── 2. The Spherical Dome Console (Locked Arc on Circle Border) ──
            val domeHeight = screenHeight * 0.54f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(domeHeight)
                    .align(Alignment.BottomCenter)
            ) {
                val yApexDp = 28.dp
                val startAngle = 216f
                val sweepAngle = 108f

                // Canvas draws Dome background & Music Bar directly on the circle border
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(duration) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val yApexPx = yApexDp.toPx()
                                val radius = size.width * 0.58f
                                val cy = yApexPx + radius
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dy < 0 && abs(dist - radius) <= 36.dp.toPx()) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    val p = ((angle - startAngle) / sweepAngle).coerceIn(0f, 1f)
                                    onSeek((p * duration).toLong())
                                }
                            }
                        }
                        .pointerInput(duration) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val cx = size.width / 2f
                                    val yApexPx = yApexDp.toPx()
                                    val radius = size.width * 0.58f
                                    val cy = yApexPx + radius
                                    val dx = offset.x - cx
                                    val dy = offset.y - cy
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dy < 0 && abs(dist - radius) <= 40.dp.toPx()) {
                                        isDraggingProgress = true
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f
                                        dragFraction = ((angle - startAngle) / sweepAngle).coerceIn(0f, 1f)
                                    }
                                },
                                onHorizontalDrag = { change, _ ->
                                    if (isDraggingProgress) {
                                        val cx = size.width / 2f
                                        val yApexPx = yApexDp.toPx()
                                        val radius = size.width * 0.58f
                                        val cy = yApexPx + radius
                                        val dx = change.position.x - cx
                                        val dy = change.position.y - cy
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f
                                        dragFraction = ((angle - startAngle) / sweepAngle).coerceIn(0f, 1f)
                                    }
                                },
                                onDragEnd = {
                                    if (isDraggingProgress) {
                                        onSeek((dragFraction * duration).toLong())
                                        isDraggingProgress = false
                                    }
                                },
                                onDragCancel = {
                                    isDraggingProgress = false
                                }
                            )
                        }
                ) {
                    val cx = size.width / 2f
                    val yApexPx = yApexDp.toPx()
                    val radius = size.width * 0.58f
                    val cy = yApexPx + radius
                    val strokeW = 4.dp.toPx()

                    val dxEdge = -cx
                    val dyEdge = -sqrt((radius * radius - dxEdge * dxEdge).coerceAtLeast(0f))
                    val yEdge = cy + dyEdge
                    val edgeAngleLeft = 210.5f
                    val edgeSweep = 119f

                    // A. Spherical Dome Path: top boundary matches the exact circular arc
                    val domePath = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, yEdge)
                        arcTo(
                            rect = Rect(
                                left = cx - radius,
                                top = cy - radius,
                                right = cx + radius,
                                bottom = cy + radius
                            ),
                            startAngleDegrees = edgeAngleLeft,
                            sweepAngleDegrees = edgeSweep,
                            forceMoveTo = false
                        )
                        lineTo(size.width, size.height)
                        close()
                    }

                    // Fill dome with deep dark radial gradient
                    drawPath(
                        path = domePath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF263346),
                                PlayerDarkNavy,
                                PlayerBlack
                            ),
                            center = Offset(cx, cy * 0.75f),
                            radius = radius * 1.3f
                        )
                    )

                    // B. Subtle outer edge rim on the dome boundary
                    drawArc(
                        color = PlayerWhite.copy(alpha = 0.08f),
                        startAngle = edgeAngleLeft,
                        sweepAngle = edgeSweep,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // C. Inactive Music Bar Track on the Dome Circle Border
                    drawArc(
                        color = PlayerGrey.copy(alpha = 0.35f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // D. Active Played Arc Track on the Dome Circle Border
                    drawArc(
                        color = PlayerWhite,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // E. Glowing White Circular Thumb Knob on the Circle Border
                    val currentAngleDeg = startAngle + (sweepAngle * animatedProgress)
                    val rad = Math.toRadians(currentAngleDeg.toDouble())
                    val thumbX = cx + radius * cos(rad).toFloat()
                    val thumbY = cy + radius * sin(rad).toFloat()

                    // Thumb outer ambient halo
                    drawCircle(
                        color = GlowCyan.copy(alpha = 0.45f),
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

                // ── Like (Heart) Button: Aligned directly below the arc apex ──
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = yApexDp + 14.dp)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else PlayerWhite.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // ── Shuffle, Curved Accent Pill, Repeat ───────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 92.dp)
                        .padding(horizontal = 46.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Shuffle Button
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

                    // Accent Pill
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(AccentPillColor.copy(alpha = 0.70f))
                    )

                    // Circular Repeat Button
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

                // ── Central Transport Pod: [|◀]  (  [▶ / ⏸]  )  [▶|] ─────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 156.dp)
                        .padding(horizontal = 38.dp),
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

                    // Central Hero Play/Pause with Concentric Vortex Rings
                    Box(
                        modifier = Modifier.size(118.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

                        // Inner Solid White Circle Button
                        Box(
                            modifier = Modifier
                                .size(78.dp)
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

                // ── Timestamps (Current Position & Duration) ──────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 282.dp)
                        .padding(horizontal = 50.dp),
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

                // ── Swipe Up for Lyrics Prompt ────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                        )
                        .padding(bottom = 18.dp),
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

        // ── 2. Synced Lyrics Section (Revealed smoothly on swipe up / scroll) ─
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lyrics,
                    contentDescription = null,
                    tint = PlayerWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Lyrics",
                    color = PlayerWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            lyricsContent()
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
