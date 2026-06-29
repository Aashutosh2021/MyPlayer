package com.example.myplayer.ui.screens.home

import java.util.Calendar
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.components.*
import com.example.myplayer.ui.theme.*

import com.example.myplayer.ui.screens.recommendation.RecommendationViewModel
import com.example.myplayer.ui.components.recommendation.RecommendationSection

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: HomeViewModel = hiltViewModel(),
    recommendationViewModel: RecommendationViewModel = hiltViewModel()
) {
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    val mostPlayed  by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val allSongs    by viewModel.allSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            in 17..20 -> "Good Evening,"
            else      -> "Good Night,"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold, color = OnSurface
                            )
                        )
                        Text(
                            text = "User.",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold, color = ClayPrimary
                            )
                        )
                    }
                    // Avatar circle (Clay surface)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .claySurface(borderRadius = 26.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        // ── Favorite Songs ────────────────────────────────────────────────────
        if (favoriteSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Favorite Songs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
                }
            }
            item {
                // 2-column grid of favorites
                val chunked = favoriteSongs.take(6).chunked(2)
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    chunked.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEachIndexed { i, song ->
                                val index = favoriteSongs.indexOf(song)
                                FavoriteSongCard(
                                    song = song,
                                    isPlaying = currentSong?.id == song.id,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.playSong(favoriteSongs, index)
                                        onNavigateToNowPlaying()
                                    }
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        // ── Recently Played ──────────────────────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            item {
                PremiumSectionHeader(title = "Recently Played", actionLabel = ">")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(156.dp)
                ) {
                    itemsIndexed(recentSongs.take(10), key = { _, song -> song.id }) { index, song ->
                        RecentlyPlayedCard(
                            song = song,
                            isPlaying = currentSong?.id == song.id,
                            onClick = {
                                viewModel.playSong(recentSongs, index)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Recommended For You ───────────────────────────────────────────────
        item {
            val recs by recommendationViewModel.recommendations.collectAsStateWithLifecycle()
            val isLoading by recommendationViewModel.isLoading.collectAsStateWithLifecycle()
            
            RecommendationSection(
                title = "Recommended For You",
                recommendations = recs,
                isLoading = isLoading,
                onPlayClick = { song -> 
                    recommendationViewModel.playNow(song)
                    onNavigateToNowPlaying()
                },
                onMoreClick = { song ->
                    // For now, hide on More click to simulate action
                    recommendationViewModel.hideRecommendation(song)
                },
                onRefreshClick = { recommendationViewModel.refreshRecommendations() }
            )
        }

        // ── Most Played ──────────────────────────────────────────────────────────
        if (mostPlayed.isNotEmpty()) {
            item {
                PremiumSectionHeader(title = "Most Played")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(156.dp)
                ) {
                    itemsIndexed(mostPlayed.take(10), key = { _, song -> song.id }) { index, song ->
                        RecentlyPlayedCard(
                            song = song,
                            isPlaying = currentSong?.id == song.id,
                            onClick = {
                                viewModel.playSong(mostPlayed, index)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── All Local Songs ──────────────────────────────────────────────────
        if (allSongs.isNotEmpty()) {
            item {
                PremiumSectionHeader(title = "All Songs")
            }
            itemsIndexed(allSongs.take(8), key = { _, song -> song.id }) { index, song ->
                TrendingListItem(
                    song = song,
                    isPlaying = currentSong?.id == song.id,
                    onClick = {
                        viewModel.playSong(allSongs, index)
                        onNavigateToNowPlaying()
                    }
                )
            }
        }

        // ── Empty state ───────────────────────────────────────────────────────
        if (allSongs.isEmpty() && recentSongs.isEmpty()) {
            item { PremiumEmptyState() }
        }
    }
}

// ─── Recently Played Card (Clay card with inset well) ────────────────────────

@Composable
private fun RecentlyPlayedCard(
    song: SongEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPlaying) SurfaceLight else SurfaceLight

    Box(
        modifier = Modifier
            .width(160.dp)
            .claySurface(
                borderRadius = 28.dp,
                backgroundColor = backgroundColor
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clayConcave(borderRadius = 20.dp, backgroundColor = SurfaceContainerLow)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    uri = song.albumArt,
                    size = 120.dp,
                    shape = RoundedCornerShape(16.dp),
                    iconSize = 48.dp
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .claySurface(borderRadius = 14.dp, backgroundColor = ClayPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ─── Favorite Song Card (2-col grid) ─────────────────────────────────────────

@Composable
private fun FavoriteSongCard(
    song: SongEntity,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clayConcave(borderRadius = 12.dp)
                .padding(2.dp)
        ) {
            AlbumArtImage(uri = song.albumArt, size = 40.dp, shape = RoundedCornerShape(10.dp), iconSize = 20.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPlaying) ClayPrimary else OnSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Trending List Item ───────────────────────────────────────────────────────

@Composable
private fun TrendingListItem(
    song: SongEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clayConcave(borderRadius = 14.dp)
                .padding(2.dp)
        ) {
            AlbumArtImage(uri = song.albumArt, size = 48.dp, shape = RoundedCornerShape(12.dp), iconSize = 22.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isPlaying) ClayPrimary else OnSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        ClayIconButton(onClick = {}, size = 36.dp) {
            Icon(Icons.Filled.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun PremiumEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LibraryMusic, null, tint = TextMuted, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("No music yet", style = MaterialTheme.typography.titleLarge, color = OnSurface)
        Spacer(Modifier.height(8.dp))
        Text("Go to Library to add a music folder", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
    }
}

fun formatDurationHome(durationMs: Long): String {
    val s = durationMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
