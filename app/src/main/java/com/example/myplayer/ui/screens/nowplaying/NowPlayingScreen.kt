package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.verticalScroll
import com.example.myplayer.ui.screens.recommendation.RecommendationViewModel
import com.example.myplayer.ui.components.recommendation.RecommendationQueuePreview
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    title: String?,
    artist: String?,
    artUri: String?,
    durationMs: Long,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onBackClick: () -> Unit,
    sleepTimerRemaining: Long = -1L,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    isShuffleOn: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    repeatMode: Int = 0,
    onCycleRepeatMode: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    canDownload: Boolean = false,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownloadClick: () -> Unit = {},
    recommendationViewModel: RecommendationViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    var showSleepTimer by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val hasSong = !title.isNullOrBlank()
    val displayPosition = if (isSeeking) seekPosition.toLong() else currentPosition
    val duration = durationMs.coerceAtLeast(1L)
    val progress = (displayPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "artScale"
    )

    if (showSleepTimer) {
        SleepTimerDialog(
            currentRemaining = sleepTimerRemaining,
            onDismiss = { showSleepTimer = false },
            onStart = { minutes -> onStartSleepTimer(minutes); showSleepTimer = false },
            onCancel = { onCancelSleepTimer(); showSleepTimer = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
    ) {
        val scrollState = androidx.compose.foundation.rememberScrollState()
        val windowInfo = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeight = windowInfo.screenHeightDp.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconButton(onClick = onBackClick, size = 48.dp) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = OnSurface, modifier = Modifier.size(28.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Now Playing", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                }

                Row {
                    ClayIconButton(onClick = { showSleepTimer = true }, size = 48.dp) {
                        Icon(
                            if (sleepTimerRemaining > 0) Icons.Filled.Timer else Icons.AutoMirrored.Filled.QueueMusic,
                            null,
                            tint = if (sleepTimerRemaining > 0) ClayPrimary else OnSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    ClayIconButton(onClick = onNavigateToSettings, size = 48.dp) {
                        Icon(Icons.Filled.Settings, null, tint = OnSurface, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(Modifier.height(screenHeight * 0.05f))

            // ── Album Art ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .clayConcave(borderRadius = 32.dp, backgroundColor = SurfaceLight)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasSong) {
                    AlbumArtImage(
                        uri = artUri ?: "",
                        size = 284.dp,
                        shape = RoundedCornerShape(24.dp),
                        iconSize = 80.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(80.dp))
                    }
                }
            }

            Spacer(Modifier.height(screenHeight * 0.05f))

            // ── Song Info ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasSong) title!! else "No song playing",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = artist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Download button (only for online/downloadable songs)
                if (canDownload) {
                    Spacer(Modifier.width(12.dp))
                    when {
                        isDownloaded -> Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = ClayPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        isDownloading -> CircularProgressIndicator(
                            color = ClayPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = OnSurfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onDownloadClick() }
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) ClayPrimary else OnSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onToggleFavorite() }
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Seek Bar ─────────────────────────────────────────────────────
            Slider(
                value = progress,
                onValueChange = { v ->
                    isSeeking = true
                    seekPosition = (v * duration).toLong().toFloat()
                },
                onValueChangeFinished = {
                    onSeek(seekPosition.toLong())
                    isSeeking = false
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = ClayPrimary,
                    activeTrackColor = ClayPrimary,
                    inactiveTrackColor = SurfaceContainerHigh
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(displayPosition), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Spacer(Modifier.height(32.dp))

            // ── Controls ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        null,
                        tint = if (isShuffleOn) ClayPrimary else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Previous
                ClayIconButton(onClick = onPreviousClick, size = 60.dp) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = OnSurface, modifier = Modifier.size(32.dp))
                }
                
                // Play/Pause (Primary Clay Button)
                val playInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .claySurface(
                            borderRadius = 40.dp,
                            backgroundColor = ClayPrimary,
                            innerLightColor = Color.White.copy(alpha = 0.5f),
                            innerDarkColor = Color.Black.copy(alpha = 0.2f),
                            interactionSource = playInteractionSource
                        )
                        .clickable(
                            interactionSource = playInteractionSource,
                            indication = null,
                            onClick = onPlayPauseClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = OnPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                ClayIconButton(onClick = onNextClick, size = 60.dp) {
                    Icon(Icons.Filled.SkipNext, null, tint = OnSurface, modifier = Modifier.size(32.dp))
                }
                
                // Repeat
                IconButton(onClick = onCycleRepeatMode) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        null,
                        tint = if (repeatMode > 0) ClayPrimary else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // ── Lyrics / Queue Tabs ─────────────────────────────────────
            var bottomTab by remember { mutableIntStateOf(0) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayConcave(borderRadius = 20.dp, backgroundColor = SurfaceContainerLow)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Lyrics", "Up Next").forEachIndexed { index, label ->
                    val active = bottomTab == index
                    val tabMod = if (active)
                        Modifier.weight(1f).claySurface(borderRadius = 14.dp, backgroundColor = SurfaceLight).padding(vertical = 10.dp)
                    else
                        Modifier.weight(1f).clickable { bottomTab = index }.padding(vertical = 10.dp)
                    Box(tabMod, contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) ClayPrimary else OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
            val lyricsPosition by lyricsViewModel.currentPosition.collectAsStateWithLifecycle()

            AnimatedContent(
                targetState = bottomTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "bottom_tab"
            ) { tab ->
                when (tab) {
                    0 -> LyricsTab(
                        lyricsState = lyricsState,
                        currentPositionMs = lyricsPosition,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp)
                    )
                    else -> {
                        val recs by recommendationViewModel.recommendations.collectAsStateWithLifecycle()
                        RecommendationQueuePreview(
                            recommendations = recs,
                            onSongClick = { song -> recommendationViewModel.playNow(song) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ── Sleep Timer Dialog ────────────────────────────────────────────────────────

@Composable
fun SleepTimerDialog(
    currentRemaining: Long,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val options = listOf(5, 10, 15, 30, 45, 60)
    val isActive = currentRemaining > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        icon = { Icon(Icons.Filled.Timer, null, tint = ClayPrimary) },
        title = { Text(if (isActive) "Sleep Timer Active" else "Set Sleep Timer") },
        text = {
            Column {
                if (isActive) {
                    Text(
                        text = "Pausing in ${currentRemaining / 60}m ${currentRemaining % 60}s",
                        color = ClayPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text("Music will pause automatically after the selected time.")
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { minutes ->
                                OutlinedButton(
                                    onClick = { onStart(minutes) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClayPrimary),
                                    border = BorderStroke(1.dp, ClayPrimary.copy(0.4f))
                                ) {
                                    Text("${minutes}m")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))) {
                        Text("Cancel Timer")
                    }
                }
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)) {
                    Text("Close")
                }
            }
        }
    )
}

fun formatDuration(durationMs: Long): String {
    val s = durationMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
