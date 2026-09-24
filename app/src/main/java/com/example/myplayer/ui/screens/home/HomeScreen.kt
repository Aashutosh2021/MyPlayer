package com.example.myplayer.ui.screens.home

import java.util.Calendar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.myplayer.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    val mostPlayed  by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val allSongs    by viewModel.allSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember {
        listOf("All", "Party", "Blues", "Sad", "Hip Hop", "Chill", "Workout", "Pop")
    }

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning!"
            in 12..16 -> "Good Afternoon!"
            in 17..20 -> "Good Evening!"
            else      -> "Good Night!"
        }
    }

    // Filter songs if a specific mood/category is tapped
    val displayedSongs = remember(selectedCategory, allSongs) {
        if (selectedCategory == "All") {
            allSongs
        } else {
            val query = selectedCategory.lowercase()
            allSongs.filter {
                it.title.lowercase().contains(query) ||
                it.artist.lowercase().contains(query) ||
                it.album.lowercase().contains(query)
            }.ifEmpty { allSongs }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepOliveBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
    ) {
        // ── Top Header (Avatar, Greeting & Notification Bell) ─────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar with subtle neon lime accent
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceLight)
                            .border(BorderStroke(1.5.dp, CardBorderOlive), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = NeonLimePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "Alex Rivera",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )
                    }
                }

                // Notification Bell with Neon Dot
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceLight)
                        .border(BorderStroke(1.dp, CardBorderOlive), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = OnSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    // Vibrant neon lime dot indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 11.dp, end = 12.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NeonLimePrimary)
                    )
                }
            }
        }

        // ── Category Filter Pills ─────────────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) NeonLimePrimary else SurfaceLight,
                        label = "pillBg_$category"
                    )
                    val textTint by animateColorAsState(
                        targetValue = if (isSelected) OnNeonLime else OnSurfaceVariant,
                        label = "textTint_$category"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(pillBg)
                            .then(
                                if (!isSelected) Modifier.border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(24.dp))
                                else Modifier
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textTint
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Popular Songs Section ─────────────────────────────────────────────
        val popularPool = (mostPlayed.ifEmpty { recentSongs.ifEmpty { allSongs } })
        if (popularPool.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular Songs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NeonLimePrimary
                    )
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    itemsIndexed(popularPool.take(8), key = { _, song -> song.id }) { index, song ->
                        PopularSongCard(
                            song = song,
                            isPlaying = currentSong?.id == song.id,
                            onClick = {
                                viewModel.playSong(popularPool, index)
                                onNavigateToNowPlaying()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Featured "New Collection" Banner Card ─────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF26331C), Color(0xFF161E13))
                        )
                    )
                    .border(BorderStroke(1.dp, Color(0xFF354427)), RoundedCornerShape(24.dp))
                    .clickable {
                        if (allSongs.isNotEmpty()) {
                            viewModel.playSong(allSongs, 0)
                            onNavigateToNowPlaying()
                        }
                    }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A2216))
                                .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "New Collection",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonLimePrimary
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Top Songs Global",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Discover ${allSongs.size.coerceAtLeast(86)} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }

                    // Circle Play Action Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = NeonLimePrimary)
                            .clip(CircleShape)
                            .background(NeonLimePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Collection",
                            tint = OnNeonLime,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Favorite Songs (2-Column Grid) ────────────────────────────────────
        if (favoriteSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorite Songs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                }
            }
            item {
                val chunked = favoriteSongs.take(6).chunked(2)
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    chunked.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { song ->
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
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Recently Played Row ───────────────────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(recentSongs.take(10), key = { _, song -> song.id }) { index, song ->
                        PopularSongCard(
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

        // ── All Songs List ────────────────────────────────────────────────────
        if (displayedSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "All") "All Songs" else "$selectedCategory Songs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                }
            }

            itemsIndexed(displayedSongs.take(10), key = { _, song -> song.id }) { index, song ->
                DarkSongListItem(
                    song = song,
                    isPlaying = currentSong?.id == song.id,
                    onClick = {
                        viewModel.playSong(displayedSongs, index)
                        onNavigateToNowPlaying()
                    }
                )
            }
        }

        // ── Empty State ───────────────────────────────────────────────────────
        if (allSongs.isEmpty() && recentSongs.isEmpty()) {
            item { DarkEmptyState() }
        }
    }
}

// ─── Popular Song Card (Dark Olive with square album art) ────────────────────

@Composable
private fun PopularSongCard(
    song: SongEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLight)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) NeonLimePrimary.copy(alpha = 0.6f) else CardBorderOlive
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    uri = song.albumArt,
                    title = song.title,
                    artist = song.artist,
                    size = 132.dp,
                    shape = RoundedCornerShape(14.dp),
                    iconSize = 44.dp
                )

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(NeonLimePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = "Playing",
                            tint = OnNeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPlaying) NeonLimePrimary else OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Favorite Song Card (2-Col Grid) ─────────────────────────────────────────

@Composable
private fun FavoriteSongCard(
    song: SongEntity,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLight)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) NeonLimePrimary.copy(alpha = 0.5f) else CardBorderOlive
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtImage(
                uri = song.albumArt,
                title = song.title,
                artist = song.artist,
                size = 42.dp,
                shape = RoundedCornerShape(10.dp),
                iconSize = 20.dp
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPlaying) NeonLimePrimary else OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Dark Song List Item ─────────────────────────────────────────────────────

@Composable
private fun DarkSongListItem(
    song: SongEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLight)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) NeonLimePrimary.copy(alpha = 0.5f) else CardBorderOlive
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtImage(
                uri = song.albumArt,
                title = song.title,
                artist = song.artist,
                size = 48.dp,
                shape = RoundedCornerShape(12.dp),
                iconSize = 22.dp
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPlaying) NeonLimePrimary else OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isPlaying) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = "Playing",
                tint = NeonLimePrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun DarkEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceLight)
                .border(BorderStroke(1.dp, CardBorderOlive), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = NeonLimePrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No music yet", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "Search online to stream or download songs",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

fun formatDurationHome(durationMs: Long): String {
    val s = durationMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
