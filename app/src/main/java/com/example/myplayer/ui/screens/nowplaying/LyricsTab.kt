package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplayer.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Lyrics tab for NowPlayingScreen.
 * Displays synced lyrics with real-time line highlighting,
 * or falls back to plain text.
 *
 * [positionMs] is passed as State (not a raw Long) so only the composables
 * that actually read it recompose on each position tick.
 */
@Composable
fun LyricsTab(
    lyricsState: LyricsUiState,
    positionMs: State<Long>,
    onSeek: (Long) -> Unit = {},
    onAddCustomLyrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CloudBlueBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        when (lyricsState) {
            is LyricsUiState.Idle,
            is LyricsUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (lyricsState is LyricsUiState.Loading) {
                        CircularProgressIndicator(color = ClayPrimary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Fetching lyrics...", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clayConcave(borderRadius = 40.dp, backgroundColor = SurfaceLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Play a song to see lyrics", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    }
                }
            }

            is LyricsUiState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clayConcave(borderRadius = 36.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("No lyrics found", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Lyrics not available online for this song",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onAddCustomLyrics,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLimePrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Add Custom Lyrics",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            is LyricsUiState.Found -> {
                if (lyricsState.syncedLines.isNotEmpty()) {
                    SyncedLyricsView(
                        lines = lyricsState.syncedLines,
                        positionMs = positionMs,
                        onSeek = onSeek
                    )
                } else {
                    PlainLyricsView(text = lyricsState.result.plainLyrics ?: "")
                }

                IconButton(
                    onClick = onAddCustomLyrics,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Lyrics",
                        tint = OnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<Pair<Long, String>>,
    positionMs: State<Long>,
    onSeek: (Long) -> Unit
) {
    // Read positionMs.value INSIDE derivedStateOf so Compose tracks it as a
    // dependency and re-derives on every tick — but only notifies readers when
    // the active *line index* actually changes.
    val activeIndexState = remember(lines) {
        derivedStateOf {
            val pos = positionMs.value
            // Binary search: last line with timestamp <= pos
            var lo = 0
            var hi = lines.size - 1
            var idx = -1
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                if (lines[mid].first <= pos) { idx = mid; lo = mid + 1 } else hi = mid - 1
            }
            idx
        }
    }
    val activeIndex by activeIndexState

    val listState = rememberLazyListState()

    // Auto-scroll only when the active line changes (snapshotFlow deduplicates).
    LaunchedEffect(lines) {
        snapshotFlow { activeIndexState.value }.collect { index ->
            if (index >= 0) {
                listState.animateScrollToItem(index = (index - 2).coerceAtLeast(0))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lines, key = { index, _ -> "synced_$index" }) { index, (timestampMs, lyricLine) ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "lyricScale"
            )
            val color by animateColorAsState(
                targetValue = when {
                    isActive -> ClayPrimary
                    isPast -> OnSurface.copy(alpha = 0.6f)
                    else -> OnSurfaceVariant.copy(alpha = 0.35f)
                },
                animationSpec = tween(300),
                label = "lyricColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable { onSeek(timestampMs) }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lyricLine.ifBlank { "•" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = if (isActive) 18.sp else 16.sp,
                        lineHeight = 28.sp
                    ),
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }
    }
}

@Composable
private fun PlainLyricsView(text: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val linesOfText = text.lines()
        items(linesOfText.size, key = { "plain_$it" }) { i ->
            Text(
                text = linesOfText[i],
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
