package com.example.myplayer.ui.screens.nowplaying

import androidx.compose.animation.*
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
 */
@Composable
fun LyricsTab(
    lyricsState: LyricsUiState,
    currentPositionMs: Long,
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
                        currentPositionMs = currentPositionMs
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
    currentPositionMs: Long
) {
    // Use derivedStateOf: only recomputes when currentPositionMs changes the active *line*,
    // not on every identical tick. This prevents scroll-animation spam.
    val activeIndex by remember(lines) {
        derivedStateOf {
            var idx = -1
            for (i in lines.indices) {
                if (lines[i].first <= currentPositionMs) idx = i else break
            }
            idx
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll: only fires when activeIndex *value* actually changes, not every tick.
    // LaunchedEffect(activeIndex) already handles this correctly when activeIndex
    // is a stable derived state.
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(
                index = (activeIndex - 2).coerceAtLeast(0)
            )
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

            AnimatedContent(
                targetState = isActive,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "lyric_line_$index"
            ) { active ->
                Text(
                    text = lyricLine.ifBlank { "\u2022" }, // bullet for empty lines
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (active) 20.sp else 16.sp,
                        lineHeight = if (active) 28.sp else 24.sp
                    ),
                    color = when {
                        active -> OnSurface
                        isPast -> OnSurfaceVariant.copy(alpha = 0.5f)
                        else -> OnSurfaceVariant.copy(alpha = 0.35f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
