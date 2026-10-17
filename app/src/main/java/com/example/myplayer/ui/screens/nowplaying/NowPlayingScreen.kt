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
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.myplayer.R
import com.example.myplayer.playback.AudioQualityInfo
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.components.ProfessionalMusicProgressBar
import com.example.myplayer.ui.components.RadialAudioController
import com.example.myplayer.ui.components.WaveformAudioSeeker
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


    if (showQualityDetails) {
        AlertDialog(
            onDismissRequest = { showQualityDetails = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                        color = if (audioQuality.isLossless) MaterialTheme.colorScheme.primary else TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDetails = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(Background)
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

            // ── Top Navigation Bar (Stitch Screen 5) ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = OnSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM ALBUM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = Secondary,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (hasSong) title!! else "MyPlayer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showSleepTimer = true }) {
                    Icon(
                        imageVector = if (sleepTimerRemaining > 0) Icons.Filled.Timer else Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue / Timer",
                        tint = if (sleepTimerRemaining > 0) Primary else Secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Album Artwork (Squircle with 3D Tilt) ─────────────────────────
            Interactive3DAlbumArt(
                artUri = artUri,
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                hasSong = hasSong
            )

            Spacer(Modifier.height(28.dp))

            // ── Track Details & Quick Actions Row (Image 1 Left) ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Favorite Heart Button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unlike" else "Like",
                        tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center: Title & Artist
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (hasSong) title!! else "No song playing",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Right: Options Menu (...)
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Studio Engine (${audioQuality.displayBadge})", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                showQualityDetails = true
                            }
                        )
                        if (hasSong && canDownload) {
                            if (isDownloaded) {
                                DropdownMenuItem(
                                    text = { Text("Remove Download", color = Color(0xFFBA1A1A)) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                                    onClick = {
                                        showMenu = false
                                        showRemoveConfirm = true
                                    }
                                )
                            } else if (!isDownloading) {
                                DropdownMenuItem(
                                    text = { Text("Download Song", color = MaterialTheme.colorScheme.primary) },
                                    leadingIcon = { Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMenu = false
                                        onDownloadClick()
                                    }
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("Settings", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Waveform Audio Seeker (Image 1 Left) ─────────────────────────
            IsolatedPlaybackControls(
                positionState = positionState,
                durationMs = duration,
                isPlaying = isPlaying,
                onSeek = onSeek,
                onPlayPauseClick = onPlayPauseClick
            )

            Spacer(Modifier.height(20.dp))

            // ── Bottom Playback Control Row (Image 1 Left) ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = onPreviousClick) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Solid Charcoal Play/Pause Circle (Image 1 Left)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(12.dp, CircleShape, spotColor = Color(0x35000000))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
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
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next
                IconButton(onClick = onNextClick) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onCycleRepeatMode) {
                    val (icon, tint, desc) = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Triple(Icons.Filled.RepeatOne, MaterialTheme.colorScheme.primary, "Repeat One")
                        Player.REPEAT_MODE_ALL -> Triple(Icons.Filled.Repeat, MaterialTheme.colorScheme.primary, "Repeat All")
                        else -> Triple(Icons.Filled.Repeat, MaterialTheme.colorScheme.onPrimaryContainer, "Repeat Off")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Synced Lyrics Section ────────────────────────────────────────
            val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
            val lyricsPositionState = lyricsViewModel.currentPosition.collectAsStateWithLifecycle()
            LyricsTab(
                lyricsState = lyricsState,
                positionMs = lyricsPositionState,
                onSeek = onSeek,
                onAddCustomLyrics = { showCustomLyricsDialog = true },
                onRetryLyrics = { lyricsViewModel.retryLyrics() },
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
    onPlayPauseClick: () -> Unit
) {
    com.example.myplayer.ui.components.ProfessionalMusicProgressBar(
        positionMs = positionState.value,
        durationMs = durationMs,
        onSeek = onSeek,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                    contentColor = Color.Black,
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

@Composable
fun Interactive3DAlbumArt(
    artUri: String?,
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    hasSong: Boolean
) {
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }

    val animatedTiltX by animateFloatAsState(
        targetValue = if (isTouching) tiltX else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = if (isTouching) tiltY else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tiltY"
    )
    val artScale by animateFloatAsState(
        targetValue = when {
            isTouching -> 1.03f
            isPlaying -> 1f
            else -> 0.93f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "artScale"
    )

    // 3D Flip when song changes
    androidx.compose.animation.AnimatedContent(
        targetState = Triple(artUri, title, artist),
        transitionSpec = {
            (androidx.compose.animation.fadeIn(tween(280)) +
                androidx.compose.animation.scaleIn(initialScale = 0.86f, animationSpec = tween(280)))
                .togetherWith(
                    androidx.compose.animation.fadeOut(tween(200)) +
                        androidx.compose.animation.scaleOut(targetScale = 0.86f, animationSpec = tween(200))
                )
        },
        label = "songArt3DTransition"
    ) { (currentArt, currentTitle, currentArtist) ->
        Box(
            modifier = Modifier
                .size(280.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isTouching = true
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            tiltY = ((offset.x - cx) / cx).coerceIn(-1f, 1f) * 14f
                            tiltX = -((offset.y - cy) / cy).coerceIn(-1f, 1f) * 14f
                        },
                        onDrag = { change, _ ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            tiltY = ((change.position.x - cx) / cx).coerceIn(-1f, 1f) * 14f
                            tiltX = -((change.position.y - cy) / cy).coerceIn(-1f, 1f) * 14f
                        },
                        onDragEnd = { isTouching = false },
                        onDragCancel = { isTouching = false }
                    )
                }
                .graphicsLayer {
                    scaleX = artScale
                    scaleY = artScale
                    rotationX = animatedTiltX
                    rotationY = animatedTiltY
                    cameraDistance = 14f * density
                    shadowElevation = if (isPlaying) 24.dp.toPx() else 8.dp.toPx()
                    shape = RoundedCornerShape(28.dp)
                    clip = true
                }
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(28.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasSong) {
                AlbumArtImage(
                    uri = currentArt ?: "",
                    title = currentTitle,
                    artist = currentArtist,
                    size = 264.dp,
                    shape = RoundedCornerShape(22.dp),
                    iconSize = 80.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(72.dp))
                }
            }
        }
    }
}
