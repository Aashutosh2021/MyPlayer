package com.myplayer.app.ui.screens.nowplaying

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myplayer.app.data.local.entity.SongEntity
import com.myplayer.app.ui.common.AlbumArtImage
import java.util.Locale

// ── Theme colors strictly matching the tablet reference screenshot ──────────
private val TabletBackground      = Color(0xFF0E121A)
private val QueueSidebarBg        = Color(0xFF111520)
private val QueueActiveItemBg     = Color(0xFF1B2436)
private val AccentBlue            = Color(0xFF4D84E8)
private val TextWhite             = Color(0xFFFFFFFF)
private val TextSecondary         = Color(0xFF8E99A8)
private val TextMuted             = Color(0xFF6B7687)
private val TrackInactiveColor    = Color(0xFF262D3D)
private val SidebarBorderColor    = Color(0xFF181F2D)

@Composable
fun TabletPlayerScreen(
    title: String?,
    artist: String?,
    artUri: String?,
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isShuffleOn: Boolean,
    repeatMode: Int,
    queue: List<SongEntity>,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onSleepTimerClick: () -> Unit,
    onAudioQualityClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onLyricsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(0L)
    val currentIndex = remember(queue, title, artist) {
        val idx = queue.indexOfFirst {
            it.title.equals(title, ignoreCase = true) ||
            (!artist.isNullOrBlank() && it.artist.equals(artist, ignoreCase = true))
        }
        if (idx >= 0) idx else 0
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (queue.isNotEmpty() && currentIndex in queue.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    val (currentVolume, setVolume) = rememberSystemVolume()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TabletBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── LEFT PANE: Queue / Playlist Sidebar ───────────────────────────────
        Column(
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight()
                .background(QueueSidebarBg)
                .padding(top = 16.dp, start = 20.dp, end = 16.dp, bottom = 12.dp)
        ) {
            // Header Top Bar: [v] chevron on left, [+] [edit] on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { /* Add to playlist */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Edit playlist/queue */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit queue",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Subheader: "= Recommended" (or Queue name) on left, "1/50" counter on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "= Recommended",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                val totalCount = queue.size.coerceAtLeast(1)
                val displayPosition = (currentIndex + 1).coerceAtMost(totalCount)
                Text(
                    text = "$displayPosition/$totalCount",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            // Queue Song List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (queue.isEmpty()) {
                    // Fallback single item matching current track
                    item {
                        QueueSongRow(
                            title = title ?: "No Song Playing",
                            artist = artist ?: "Unknown",
                            artUri = artUri,
                            isActive = true,
                            isPlaying = isPlaying,
                            onClick = {},
                            onOptionsClick = {}
                        )
                    }
                } else {
                    itemsIndexed(
                        items = queue,
                        key = { index, item -> "${item.id}_$index" }
                    ) { index, song ->
                        val isActive = index == currentIndex
                        QueueSongRow(
                            title = song.title,
                            artist = song.artist,
                            artUri = song.albumArt,
                            isActive = isActive,
                            isPlaying = isActive && isPlaying,
                            onClick = { onPlayQueueIndex(index) },
                            onOptionsClick = {}
                        )
                    }
                }
            }
        }

        // Thin separator between left queue and right player
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(SidebarBorderColor)
        )

        // ── RIGHT PANE: Now Playing Hero & Transport Controls ────────────────
        Column(
            modifier = Modifier
                .weight(0.64f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Action Bar: Sleep Timer, Equalizer, More Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSleepTimerClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Sleep timer",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onAudioQualityClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Equalizer,
                        contentDescription = "Audio quality & equalizer",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.08f))

            // Center Content: Album Artwork Hero
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black.copy(alpha = 0.8f)
                    )
                    .clip(RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    artUri = artUri,
                    title = title,
                    artist = artist,
                    size = 280.dp,
                    cornerRadius = 26.dp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Track Title & Artist
            Text(
                text = title?.ifBlank { "Unknown Title" } ?: "No Song Playing",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = artist?.ifBlank { "Unknown Artist" } ?: "",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(20.dp))

            // Secondary Actions Row: [≡ Lyrics/Notes]   [♥ Favorite]   [+ Add]
            Row(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLyricsClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = "Lyrics",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE53935) else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { /* Add to playlist */ }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add to playlist",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Custom Scrubbable Progress Bar with Thumb & Time Labels
            TabletProgressBar(
                positionMs = positionMs,
                durationMs = duration,
                onSeek = onSeek,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            // Transport Controls Row: [Shuffle] [Prev] [Big Play/Pause] [Next] [Repeat]
            Row(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) AccentBlue else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextWhite,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Large white circular Play/Pause button
                Surface(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    color = TextWhite,
                    modifier = Modifier.size(64.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = TextWhite,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = onCycleRepeatMode) {
                    val repeatIcon = if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != 0) AccentBlue else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.12f))

            // Bottom Volume Bar: [Speaker Icon] [Blue Slider] [100%]
            TabletVolumeBar(
                volumeFraction = currentVolume,
                onVolumeChange = setVolume,
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Queue Song Item Component ────────────────────────────────────────────────
@Composable
private fun QueueSongRow(
    title: String,
    artist: String,
    artUri: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val containerColor = if (isActive) QueueActiveItemBg else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            artUri = artUri,
            title = title,
            artist = artist,
            size = 46.dp,
            cornerRadius = 10.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = if (isActive) AccentBlue else TextWhite,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = artist,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        if (isActive) {
            EqualizerBars(
                isPlaying = isPlaying,
                color = AccentBlue,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Options",
                tint = if (isActive) TextSecondary else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── 3-Bar Equalizer Animation ────────────────────────────────────────────────
@Composable
private fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AccentBlue
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        modifier = modifier.size(width = 16.dp, height = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val h1 = if (isPlaying) bar1 else 0.4f
        val h2 = if (isPlaying) bar2 else 0.7f
        val h3 = if (isPlaying) bar3 else 0.5f

        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight(h1)
                .background(color, RoundedCornerShape(1.dp))
        )
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight(h2)
                .background(color, RoundedCornerShape(1.dp))
        )
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight(h3)
                .background(color, RoundedCornerShape(1.dp))
        )
    }
}

// ── Custom Scrubbable Progress Bar with Thumb Cursor & Timers ────────────────
@Composable
private fun TabletProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val currentFraction = if (isDragging) dragFraction else (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(safeDuration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        dragFraction = (down.position.x / size.width).coerceIn(0f, 1f)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                        isDragging = false
                        onSeek((dragFraction * safeDuration).toLong())
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                val trackHeight = 4.dp.toPx()
                val centerY = size.height / 2f
                val cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)

                // Background inactive track
                drawRoundRect(
                    color = TrackInactiveColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(size.width, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Active played progress
                val activeWidth = size.width * currentFraction
                if (activeWidth > 0f) {
                    drawRoundRect(
                        color = TextWhite,
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Vertical pill / rounded cursor thumb
                val thumbWidth = 4.dp.toPx()
                val thumbHeight = 18.dp.toPx()
                val thumbX = (activeWidth - thumbWidth / 2).coerceIn(0f, size.width - thumbWidth)
                drawRoundRect(
                    color = TextWhite,
                    topLeft = Offset(thumbX, centerY - thumbHeight / 2),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        // Time labels below progress bar: [1:33] on left, [3:41] on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPosition = if (isDragging) (dragFraction * safeDuration).toLong() else positionMs
            Text(
                text = formatDuration(displayPosition),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDuration(durationMs),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Volume Bar Row (Speaker, Blue Track Slider, Percentage) ───────────────────
@Composable
private fun TabletVolumeBar(
    volumeFraction: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val currentFraction = if (isDragging) dragFraction else volumeFraction.coerceIn(0f, 1f)

    Row(
        modifier = modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val speakerIcon = when {
            currentFraction <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
            currentFraction < 0.5f  -> Icons.AutoMirrored.Filled.VolumeDown
            else                   -> Icons.AutoMirrored.Filled.VolumeUp
        }

        Icon(
            imageVector = speakerIcon,
            contentDescription = "Volume",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        // Custom Blue Slider with seamless tap and smooth sliding
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        val startVal = (down.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = startVal
                        onVolumeChange(startVal)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = frac
                            onVolumeChange(frac)
                        }
                        isDragging = false
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                val trackHeight = 4.dp.toPx()
                val centerY = size.height / 2f
                val cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)

                // Inactive track
                drawRoundRect(
                    color = TrackInactiveColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(size.width, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Active blue track
                val activeWidth = size.width * currentFraction
                if (activeWidth > 0f) {
                    drawRoundRect(
                        color = AccentBlue,
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Vertical thumb
                val thumbWidth = 4.dp.toPx()
                val thumbHeight = 16.dp.toPx()
                val thumbX = (activeWidth - thumbWidth / 2).coerceIn(0f, size.width - thumbWidth)
                drawRoundRect(
                    color = AccentBlue,
                    topLeft = Offset(thumbX, centerY - thumbHeight / 2),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        val percentage = (currentFraction * 100).toInt()
        Text(
            text = "$percentage%",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp)
        )
    }
}

// ── Native System Volume Helper ──────────────────────────────────────────────
@Composable
private fun rememberSystemVolume(): Pair<Float, (Float) -> Unit> {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember(audioManager) { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var currentFraction by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
    }

    DisposableEffect(context, audioManager, maxVol) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                currentFraction = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) { }

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) { }
        }
    }

    val setVolume: (Float) -> Unit = { fraction ->
        val target = (fraction.coerceIn(0f, 1f) * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        currentFraction = fraction.coerceIn(0f, 1f)
    }

    return currentFraction to setVolume
}
