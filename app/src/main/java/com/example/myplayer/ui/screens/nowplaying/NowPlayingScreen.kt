package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.myplayer.playback.AudioQualityInfo
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    title: String?,
    artist: String?,
    artUri: String?,
    durationMs: Long,
    isPlaying: Boolean,
    positionState: State<Long>,
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
    canDownload: Boolean = true,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onRemoveDownloadClick: () -> Unit = {},
    audioQuality: AudioQualityInfo = AudioQualityInfo(),
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    var showSleepTimer by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showQualityDetails by remember { mutableStateOf(false) }
    var showCustomLyricsDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    val hasSong = !title.isNullOrBlank()
    val duration = durationMs.coerceAtLeast(1L)

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
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

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove Download?") },
            text = { Text("This will remove the downloaded audio from your device.") },
            confirmButton = {
                TextButton(
                    onClick = { onRemoveDownloadClick(); showRemoveConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            },
            containerColor = PureWhite,
            titleContentColor = CharcoalPrimary,
            textContentColor = SlateSecondary
        )
    }

    if (showCustomLyricsDialog) {
        val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
        val currentLyrics = when (val state = lyricsState) {
            is LyricsUiState.Found -> state.result.syncedLyrics ?: state.result.plainLyrics ?: ""
            else -> ""
        }
        val isCustomPresent = lyricsState is LyricsUiState.Found
        CustomLyricsDialog(
            initialLyrics = currentLyrics,
            trackTitle = title,
            trackArtist = artist,
            onDismiss = { showCustomLyricsDialog = false },
            onSave = { lyricsViewModel.saveCustomLyrics(it) },
            onDelete = if (isCustomPresent) ({ lyricsViewModel.deleteLyrics() }) else null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(14.dp))

        // ── Minimal Editorial Top Bar (Matches Image 2 Left Screen) ──────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Back",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = CharcoalPrimary
            )

            IconButton(onClick = { showSleepTimer = true }) {
                Icon(
                    if (sleepTimerRemaining > 0) Icons.Filled.Timer else Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue / Sleep Timer",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Hero Rounded Square Album Art with Soft Diffused Shadow ──────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = artScale
                    scaleY = artScale
                    shadowElevation = 28.dp.toPx()
                    shape = RoundedCornerShape(30.dp)
                    spotShadowColor = CharcoalPrimary.copy(alpha = 0.25f)
                    ambientShadowColor = CharcoalPrimary.copy(alpha = 0.15f)
                    clip = true
                }
                .background(SoftGraySurface),
            contentAlignment = Alignment.Center
        ) {
            if (hasSong) {
                AlbumArtImage(
                    uri = artUri ?: "",
                    title = title,
                    artist = artist,
                    size = 280.dp,
                    shape = RoundedCornerShape(30.dp),
                    iconSize = 80.dp
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = SilverMuted, modifier = Modifier.size(72.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Heart (Left) + Title & Artist (Center) + More/Download (Right) ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) CharcoalPrimary else SilverMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasSong) title!! else "No song playing",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = CharcoalPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = artist ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = SlateSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Box {
                IconButton(onClick = { showMoreSheet = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Options",
                        tint = SilverMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (showMoreSheet) {
                    DropdownMenu(
                        expanded = showMoreSheet,
                        onDismissRequest = { showMoreSheet = false },
                        modifier = Modifier.background(PureWhite)
                    ) {
                        if (hasSong && canDownload) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            isDownloaded -> "Remove Offline Download"
                                            isDownloading -> "Downloading..."
                                            else -> "Download Offline"
                                        },
                                        color = CharcoalPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                                        null,
                                        tint = CharcoalPrimary
                                    )
                                },
                                onClick = {
                                    showMoreSheet = false
                                    if (isDownloaded) showRemoveConfirm = true else if (!isDownloading) onDownloadClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Audio Quality (${audioQuality.displayBadge})", color = CharcoalPrimary) },
                            leadingIcon = { Icon(Icons.Filled.GraphicEq, null, tint = CharcoalPrimary) },
                            onClick = { showMoreSheet = false; showQualityDetails = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = CharcoalPrimary) },
                            leadingIcon = { Icon(Icons.Filled.Settings, null, tint = CharcoalPrimary) },
                            onClick = { showMoreSheet = false; onNavigateToSettings() }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Beat-Reactive Music Bar & Transport Controls ─────────────────────
        IsolatedPlaybackControls(
            positionState = positionState,
            durationMs = duration,
            isPlaying = isPlaying,
            onSeek = onSeek,
            onPlayPauseClick = onPlayPauseClick,
            isShuffleOn = isShuffleOn,
            repeatMode = repeatMode,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode
        )

        Spacer(Modifier.height(32.dp))

        // ── Synced Lyrics Section ────────────────────────────────────────────
        val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
        val lyricsPositionState = lyricsViewModel.currentPosition.collectAsStateWithLifecycle()
        LyricsTab(
            lyricsState = lyricsState,
            positionMs = lyricsPositionState,
            onSeek = onSeek,
            onAddCustomLyrics = { showCustomLyricsDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 360.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun IsolatedPlaybackControls(
    positionState: State<Long>,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    isShuffleOn: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BeatReactiveMusicBar(
            positionMs = positionState.value,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Central Play/Pause Hero Button surrounded by Shuffle, Previous, Next, Repeat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleOn) CharcoalPrimary else SilverMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onPreviousClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Central Play/Pause Hero Button
            Box(
                modifier = Modifier
                    .size(72.dp)
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
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onNextClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = onCycleRepeatMode) {
                val (icon, tint) = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne to CharcoalPrimary
                    Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat to CharcoalPrimary
                    else -> Icons.Filled.Repeat to SilverMuted
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun BeatReactiveMusicBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 48
) {
    val duration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val currentProgress = if (isDragging) dragProgress else actualProgress

    // Current heights & peak-hold heights for each bar (physics state)
    val barHeights = remember { FloatArray(barCount) { 0.15f } }
    val barVelocities = remember { FloatArray(barCount) { 0f } }
    val fftMagnitudes = remember { FloatArray(barCount) { 0f } }
    var hasLiveVisualizer by remember { mutableStateOf(false) }

    // 1. Try attaching Android's real hardware Audio Visualizer (works if RECORD_AUDIO is granted)
    DisposableEffect(isPlaying) {
        var visualizer: android.media.audiofx.Visualizer? = null
        if (isPlaying) {
            try {
                visualizer = android.media.audiofx.Visualizer(0).apply {
                    captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128)
                    setDataCaptureListener(
                        object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                v: android.media.audiofx.Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) {}

                            override fun onFftDataCapture(
                                v: android.media.audiofx.Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (fft == null || fft.size < 16) return
                                var nonZeroSum = 0f
                                val maxBins = (fft.size / 2).coerceAtLeast(1)
                                for (i in 0 until barCount) {
                                    val binIdx = ((i.toFloat() / barCount) * (maxBins * 0.65f)).toInt().coerceIn(1, maxBins - 1)
                                    val re = fft[binIdx * 2].toInt()
                                    val im = fft[binIdx * 2 + 1].toInt()
                                    val mag = kotlin.math.sqrt((re * re + im * im).toFloat())
                                    val normalized = (mag / 72f).coerceIn(0f, 1f)
                                    fftMagnitudes[i] = normalized
                                    nonZeroSum += normalized
                                }
                                hasLiveVisualizer = nonZeroSum > 0.08f
                            }
                        },
                        android.media.audiofx.Visualizer.getMaxCaptureRate() / 2,
                        false,
                        true
                    )
                    enabled = true
                }
            } catch (_: Exception) {
                hasLiveVisualizer = false
            }
        }
        onDispose {
            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (_: Exception) {}
        }
    }

    // 2. 60/120 FPS Beat Physics Engine (Fast Attack + Gravity Decay + Multi-Band Kick/Snare/Vocal Simulation)
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        var lastNanos = 0L
        var beatPhase = 0f
        var kickImpulse = 0f
        var snareImpulse = 0f
        var subBassWave = 0f
        var nextBeatInterval = 0.42f // Dynamic BPM interval so pattern never feels robotic

        while (isPlaying) {
            withFrameNanos { nanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                lastNanos = nanos
                beatPhase += dt
                subBassWave += dt * 4.3f

                // Trigger dynamic Kick & Snare impulses with organic syncopation
                if (beatPhase >= nextBeatInterval) {
                    beatPhase = 0f
                    val rand = kotlin.random.Random.nextFloat()
                    kickImpulse = 0.75f + rand * 0.25f
                    if (rand > 0.45f) snareImpulse = 0.65f + (rand * 0.35f)
                    // Vary interval between double-kick, groove, and drop beats
                    nextBeatInterval = when {
                        rand < 0.22f -> 0.21f // Fast double-kick beat!
                        rand < 0.75f -> 0.43f // Main groove beat
                        else -> 0.62f         // Syncopated drop
                    }
                } else {
                    // Exponential decay of kick and snare energy
                    kickImpulse *= kotlin.math.exp(-dt * 11f)
                    snareImpulse *= kotlin.math.exp(-dt * 16f)
                }

                val playheadBar = (currentProgress * barCount).toInt().coerceIn(0, barCount - 1)

                for (i in 0 until barCount) {
                    val normPos = i.toFloat() / barCount
                    val targetHeight = if (hasLiveVisualizer) {
                        // Real hardware FFT frequency energy
                        (0.14f + fftMagnitudes[i] * 0.86f).coerceIn(0.12f, 1f)
                    } else {
                        // Multi-band acoustic beat synthesis:
                        // Center & left bars react strongly to Bass Kick; mid bars to Vocals; outer bars to Hi-Hats/Snare
                        val centerDist = 1f - kotlin.math.abs(normPos - 0.5f) * 1.6f
                        val playheadBoost = (1f - (kotlin.math.abs(i - playheadBar) / 6f).coerceIn(0f, 1f)) * 0.32f

                        val bassBand = kickImpulse * centerDist.coerceIn(0.2f, 1f) *
                            (0.6f + 0.4f * kotlin.math.abs(kotlin.math.sin(i * 0.9f + subBassWave)))
                        val vocalMidBand = 0.35f * kotlin.math.abs(
                            kotlin.math.sin(subBassWave * 1.7f + i * 0.55f) *
                                kotlin.math.cos(subBassWave * 0.8f - i * 0.3f)
                        )
                        val hiHatBand = snareImpulse * (0.3f + 0.7f * kotlin.random.Random.nextFloat()) *
                            (if (i % 2 == 0) 0.85f else 0.45f)

                        (0.12f + bassBand * 0.55f + vocalMidBand + hiHatBand * 0.45f + playheadBoost)
                            .coerceIn(0.12f, 1f)
                    }

                    // Fast attack when beat hits, smooth gravity fall when beat drops
                    val current = barHeights[i]
                    if (targetHeight > current) {
                        barHeights[i] = current + (targetHeight - current) * (dt * 28f).coerceAtMost(0.85f)
                        barVelocities[i] = 0f
                    } else {
                        barVelocities[i] += 2.8f * dt // gravity acceleration
                        barHeights[i] = (current - barVelocities[i] * dt).coerceAtLeast(0.12f)
                    }
                }
                frameTick = nanos
            }
        }
        // Smoothly settle bars when paused
        for (i in 0 until barCount) {
            barHeights[i] = 0.16f
        }
        frameTick++
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val p = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((p * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek((dragProgress * duration).toLong())
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false }
                    )
                }
        ) {
            // Read frameTick inside Canvas draw scope so only Canvas redraws at 120 FPS
            val unusedTick = frameTick
            val barWidth = 3.2.dp.toPx()
            val totalBarsWidth = barCount * barWidth
            val gap = ((size.width - totalBarsWidth) / (barCount - 1).coerceAtLeast(1)).coerceAtLeast(1.5f)
            val centerY = size.height / 2f
            val activeX = currentProgress * size.width

            for (i in 0 until barCount) {
                val x = i * (barWidth + gap)
                val amplitude = barHeights[i]
                val barH = (amplitude * size.height).coerceAtLeast(5.dp.toPx())
                val top = centerY - barH / 2f
                val isPlayed = x <= activeX
                val isNearPlayhead = kotlin.math.abs(x - activeX) < (barWidth + gap) * 2f

                // Played bars glow brighter when they jump high on a beat!
                val barColor = when {
                    isNearPlayhead -> if (NeonLimePrimary == PureWhite || NeonLimePrimary == CharcoalPrimary) CharcoalPrimary else Color.White
                    isPlayed -> if (amplitude > 0.72f) NeonLimePrimary else NeonLimePrimary.copy(alpha = 0.85f)
                    else -> OnSurfaceVariant.copy(alpha = 0.30f)
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayedPositionMs = if (isDragging) (dragProgress * duration).toLong() else positionMs
            Text(
                text = formatDuration(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceVariant
            )
        }
    }
}

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
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = { Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (isActive) "Sleep Timer Active" else "Set Sleep Timer") },
        text = {
            Column {
                if (isActive) {
                    Text(
                        text = "Pausing in ${currentRemaining / 60}m ${currentRemaining % 60}s",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, CardBorderOlive)
                                ) {
                                    Text("${minutes}m", fontWeight = FontWeight.SemiBold)
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
                    TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))) {
                        Text("Cancel Timer")
                    }
                }
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
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

@Composable
fun CustomLyricsDialog(
    initialLyrics: String = "",
    trackTitle: String?,
    trackArtist: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by remember(initialLyrics) { mutableStateOf(initialLyrics) }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                Icons.Filled.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Add Custom Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!trackTitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$trackTitle ${if (!trackArtist.isNullOrBlank()) "• $trackArtist" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter or paste plain text lyrics or synced LRC lines (e.g. [00:15.20] line):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            "Paste lyrics here...\n\nPlain text or [mm:ss.xx] timestamped lines supported.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderOlive,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                text = clip
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paste from Clipboard", style = MaterialTheme.typography.labelMedium)
                    }

                    if (text.isNotBlank()) {
                        Text(
                            text = "${text.lines().size} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text)
                        onDismiss()
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Lyrics", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("Remove")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}