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
    val duration = durationMs.coerceAtLeast(0L)

    val positionMs = positionState.value

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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showQualityDetails) {
        AlertDialog(
            onDismissRequest = { showQualityDetails = false },
            title = { Text("Audio Quality") },
            text = {
                Column {
                    Text("Badge: ${audioQuality.displayBadge}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Format: ${audioQuality.format}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Technical: ${audioQuality.technicalSummary}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Source: ${audioQuality.sourceName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDetails = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
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
            onSave = { lyricsViewModel.saveCustomLyrics(it) },
            onDelete = if (isCustomPresent) ({ lyricsViewModel.deleteLyrics() }) else null
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NowPlayingVisual(
            title = title,
            artist = artist,
            artUri = artUri,
            durationMs = duration,
            positionMs = positionMs,
            isPlaying = isPlaying,
            isFavorite = isFavorite,
            isShuffleEnabled = isShuffleOn,
            repeatMode = repeatMode,
            onPlayPause = onPlayPauseClick,
            onPrevious = onPreviousClick,
            onNext = onNextClick,
            onSeek = onSeek,
            onFavorite = onToggleFavorite,
            onShuffle = onToggleShuffle,
            onRepeat = onCycleRepeatMode,
            onBack = onBackClick,
            onOptions = { showMoreSheet = true },
            lyricsContent = {
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
                        .height(450.dp)
                )
            }
        )

        // Options Dropdown Menu anchored at top right
        if (showMoreSheet) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 14.dp, end = 22.dp)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                DropdownMenu(
                    expanded = showMoreSheet,
                    onDismissRequest = { showMoreSheet = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showMoreSheet = false
                                if (isDownloaded) showRemoveConfirm = true else if (!isDownloading) onDownloadClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Sleep Timer", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showMoreSheet = false; showSleepTimer = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Audio Quality (${audioQuality.displayBadge})", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showMoreSheet = false; showQualityDetails = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showMoreSheet = false; onNavigateToSettings() }
                    )
                }
            }
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
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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