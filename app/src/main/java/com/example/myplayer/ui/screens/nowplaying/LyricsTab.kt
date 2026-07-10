package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clayConcave(borderRadius = 40.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No lyrics found", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Lyrics not available for this song",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            is LyricsUiState.Found -> {
                if (lyricsState.syncedLines.isNotEmpty()) {
                    SyncedLyricsView(
                        lines = lyricsState.syncedLines,
                        positionMs = positionMs
                    )
                } else {
                    PlainLyricsView(text = lyricsState.result.plainLyrics ?: "")
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<Pair<Long, String>>,
    positionMs: State<Long>
) {
    // Read positionMs.value INSIDE derivedStateOf so Compose tracks it as a
    // dependency and re-derives on every tick \u2014 but only notifies readers when
    // the active *line index* actually changes. (Capturing a raw Long parameter
    // here would freeze the calculation at composition time.)
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
        itemsIndexed(lines, key = { index, _ -> "synced_$index" }) { index, (_, lyricLine) ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            // Animate color + scale instead of swapping font size via AnimatedContent:
            // scale runs in the draw phase (graphicsLayer) \u2014 no relayout, no
            // composition churn, buttery on every frame.
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.12f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "lyricScale"
            )
            val color by animateColorAsState(
                targetValue = when {
                    isActive -> OnSurface
                    isPast -> OnSurfaceVariant.copy(alpha = 0.5f)
                    else -> OnSurfaceVariant.copy(alpha = 0.35f)
                },
                animationSpec = tween(300),
                label = "lyricColor"
            )

            Text(
                text = lyricLine.ifBlank { "\u2022" }, // bullet for empty lines
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                ),
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
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
