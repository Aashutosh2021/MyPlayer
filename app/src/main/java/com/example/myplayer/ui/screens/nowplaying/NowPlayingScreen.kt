package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplayer.playback.AudioQualityInfo
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.components.RadialAudioController
import com.example.myplayer.ui.theme.*

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.media3.common.Player

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

    val hasSong = !title.isNullOrBlank()
    val duration = durationMs.coerceAtLeast(1L)

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "artScale"
    )

    if (showQualityDetails) {
        AlertDialog(
            onDismissRequest = { showQualityDetails = false },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NeonLimePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Audio Stream Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Source: ${audioQuality.sourceName}", style = MaterialTheme.typography.bodyMedium)
                    Text("Codec / Format: ${audioQuality.format.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                    if (audioQuality.bitDepth != null) {
                        Text("Bit Depth: ${audioQuality.bitDepth}-bit", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (audioQuality.sampleRateHz != null) {
                        val khz = audioQuality.sampleRateHz / 1000.0
                        Text("Sample Rate: ${if (khz % 1.0 == 0.0) khz.toInt().toString() else "%.1f".format(khz)} kHz", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (audioQuality.bitrateKbps != null) {
                        Text("Bitrate: ~${audioQuality.bitrateKbps} kbps", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Lossless Fidelity: ${if (audioQuality.isLossless) "Yes (Bit-Perfect)" else "Standard (Compressed)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (audioQuality.isLossless) NeonLimePrimary else TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDetails = false }) {
                    Text("Close", color = NeonLimePrimary)
                }
            }
        )
    }

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
            text = { Text("This will remove the downloaded audio from your device. The song will remain available for online streaming.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveDownloadClick()
                        showRemoveConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) { Text("Cancel") }
            },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
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
            onSave = { customText ->
                lyricsViewModel.saveCustomLyrics(customText)
            },
            onDelete = if (isCustomPresent) {
                { lyricsViewModel.deleteLyrics() }
            } else null
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepOliveBackground)
    ) {
        val scrollState = androidx.compose.foundation.rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Top Navigation Bar ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (Dark circular pill)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceLight)
                        .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = OnSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = OnSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sleep Timer
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceLight)
                            .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                            .clickable { showSleepTimer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (sleepTimerRemaining > 0) Icons.Filled.Timer else Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining > 0) NeonLimePrimary else OnSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Settings / Options
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceLight)
                            .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                            .clickable(onClick = onNavigateToSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MoreHoriz,
                            contentDescription = "More Options",
                            tint = OnSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Album Artwork ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceLight)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(28.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasSong) {
                    AlbumArtImage(
                        uri = artUri ?: "",
                        title = title,
                        artist = artist,
                        size = 264.dp,
                        shape = RoundedCornerShape(22.dp),
                        iconSize = 80.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(72.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Track Title & Artist ─────────────────────────────────────────
            Text(
                text = if (hasSong) title!! else "No song playing",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(10.dp))

            // ── Audio Quality & Download Indicators ──────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio Quality Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (audioQuality.isLossless) NeonLimePrimary.copy(alpha = 0.15f) else SurfaceContainerHigh,
                    border = BorderStroke(1.dp, if (audioQuality.isLossless) NeonLimePrimary.copy(alpha = 0.4f) else CardBorderOlive),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showQualityDetails = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (audioQuality.isLossless) {
                            Icon(
                                Icons.Filled.HighQuality,
                                contentDescription = null,
                                tint = NeonLimePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = audioQuality.displayBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (audioQuality.isLossless) NeonLimePrimary else TextMuted
                        )
                    }
                }

                // Download Button
                if (hasSong && canDownload) {
                    when {
                        isDownloaded -> {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceLight)
                                    .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                                    .clickable { showRemoveConfirm = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = NeonLimePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        isDownloading -> {
                            CircularProgressIndicator(
                                color = NeonLimePrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceLight)
                                    .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                                    .clickable { onDownloadClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Hero Radial / Circular Audio Controller ──────────────────────
            // IsolatedPlaybackControls reads positionState.value internally so only
            // this subtree recomposes on every position tick (Phase 4).
            IsolatedPlaybackControls(
                positionState = positionState,
                durationMs = duration,
                isPlaying = isPlaying,
                onSeek = onSeek,
                onPlayPauseClick = onPlayPauseClick,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite
            )

            Spacer(Modifier.height(16.dp))

            // ── Bottom Playback Control Row ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) NeonLimePrimary else TextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Previous
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SurfaceLight)
                        .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                        .clickable(onClick = onPreviousClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = OnSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SurfaceLight)
                        .border(BorderStroke(1.dp, CardBorderOlive), CircleShape)
                        .clickable(onClick = onNextClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = OnSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onCycleRepeatMode) {
                    val (icon, tint, desc) = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Triple(Icons.Filled.RepeatOne, NeonLimePrimary, "Repeat One")
                        Player.REPEAT_MODE_ALL -> Triple(Icons.Filled.Repeat, NeonLimePrimary, "Repeat All")
                        else -> Triple(Icons.Filled.Repeat, TextMuted, "Repeat Off")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = tint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Synced Lyrics Section ────────────────────────────────────────
            val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
            val lyricsPositionState = lyricsViewModel.currentPosition.collectAsStateWithLifecycle()
            LyricsTab(
                lyricsState = lyricsState,
                positionMs = lyricsPositionState,
                onSeek = onSeek,
                onAddCustomLyrics = { showCustomLyricsDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Isolated Playback Controls (Phase 4: position read isolation) ─────────────
// This composable intentionally reads positionState.value so that only it —
// not NowPlayingScreen — recomposes on every 200 ms position tick.
@Composable
private fun IsolatedPlaybackControls(
    positionState: State<Long>,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    RadialAudioController(
        positionMs = positionState.value,
        durationMs = durationMs,
        isPlaying = isPlaying,
        onSeek = onSeek,
        onPlayPauseClick = onPlayPauseClick,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        modifier = Modifier.padding(vertical = 4.dp)
    )
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
        icon = { Icon(Icons.Filled.Timer, null, tint = NeonLimePrimary) },
        title = { Text(if (isActive) "Sleep Timer Active" else "Set Sleep Timer") },
        text = {
            Column {
                if (isActive) {
                    Text(
                        text = "Pausing in ${currentRemaining / 60}m ${currentRemaining % 60}s",
                        color = NeonLimePrimary,
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
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonLimePrimary),
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

// ── Custom Lyrics Dialog ──────────────────────────────────────────────────────

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
        containerColor = SurfaceLight,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        icon = {
            Icon(
                Icons.Filled.EditNote,
                contentDescription = null,
                tint = NeonLimePrimary,
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
                        color = OnSurfaceVariant,
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
                    color = OnSurfaceVariant
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
                        focusedBorderColor = NeonLimePrimary,
                        unfocusedBorderColor = CardBorderOlive,
                        cursorColor = NeonLimePrimary,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface
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
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonLimePrimary)
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
                    containerColor = NeonLimePrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = SurfaceContainerHigh,
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
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

