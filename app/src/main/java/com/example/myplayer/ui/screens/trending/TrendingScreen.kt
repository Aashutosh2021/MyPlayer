package com.example.myplayer.ui.screens.trending

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.screens.home.HomeViewModel
import com.example.myplayer.ui.theme.*

@Composable
fun TrendingScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingRecommendations.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBackground)
            .statusBarsPadding()
    ) {
        // ── Top Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDE8E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = "Trending",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = CharcoalPrimary
                )
                Text(
                    text = "Top charts & viral tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSecondary
                )
            }
        }

        if (isLoading && recommendations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CharcoalPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (recommendations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trending songs available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateSecondary
                )
            }
        } else {
            val firstTrack = recommendations.firstOrNull()

            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomPadding + 20.dp, top = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Hero Banner Card (Image 1 Right) ─────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x22000000))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF262C38)),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtImage(
                                uri = firstTrack?.thumbnailUrl,
                                title = firstTrack?.title ?: "Trending",
                                artist = firstTrack?.artist ?: "Hotlist",
                                size = 92.dp,
                                shape = RoundedCornerShape(20.dp),
                                iconSize = 36.dp
                            )
                        }

                        Spacer(Modifier.width(18.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Global Top Hits",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Hotlist • ${recommendations.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Updated Daily",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LightSlateMuted
                            )
                        }
                    }

                    // ── Main Action Buttons: [> Play] and [Shuffle] (Image 1 Right) ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.playRecommendedSongs(recommendations, 0)
                                onNavigateToNowPlaying()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CharcoalPrimary,
                                contentColor = PureWhite
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shuffled = recommendations.shuffled()
                                viewModel.playRecommendedSongs(shuffled, 0)
                                onNavigateToNowPlaying()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = PureWhite,
                                contentColor = CharcoalPrimary
                            ),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // ── Numbered Track List Items (01, 02, 03...) ─────────────────
                itemsIndexed(
                    items = recommendations,
                    key = { _, song -> song.videoId },
                    contentType = { _, _ -> "trending_song" }
                ) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playRecommendedSongs(recommendations, index)
                                onNavigateToNowPlaying()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "%02d".format(index + 1),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = SlateSecondary,
                            modifier = Modifier.width(32.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = CharcoalPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${song.artist} • ${formatDuration(song.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
