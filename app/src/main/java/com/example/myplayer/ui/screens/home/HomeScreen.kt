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
import com.example.myplayer.data.recommendation.model.RecommendationSong
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
    val userName    by viewModel.userName.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val isLoadingRecommendations by viewModel.isLoadingRecommendations.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val displayedDownloadedSongs by viewModel.displayedDownloadedSongs.collectAsStateWithLifecycle()
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

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            icon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = NeonLimePrimary) },
            title = { Text("Set Display Name", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your name to personalize your player experience:")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonLimePrimary,
                            unfocusedBorderColor = CardBorderOlive,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = NeonLimePrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Your Name", color = TextMuted) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.setUserName(nameInput.trim())
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLimePrimary,
                        contentColor = OnNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            nameInput = userName
                            showEditNameDialog = true
                        }
                        .padding(4.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit name",
                                tint = OnSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
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
                            .clickable { viewModel.selectCategory(category) }
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

        if (selectedCategory == "All") {
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


            // ── Recommended / Trending Section ──────────────────────────────────
            if (recommendations.isNotEmpty() || isLoadingRecommendations) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (userState?.isColdStart != false) "Trending Music" else "Recommended For You",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (recommendations.isNotEmpty()) {
                            itemsIndexed(recommendations, key = { _, reco -> reco.videoId }) { index, reco ->
                                RecommendedSongCard(
                                    song = reco,
                                    isPlaying = currentSong?.videoId == reco.videoId || currentSong?.id == reco.videoId,
                                    onClick = {
                                        viewModel.playRecommendedSongs(recommendations, index)
                                        onNavigateToNowPlaying()
                                    }
                                )
                            }
                        } else {
                            items(4) {
                                RecommendationPlaceholderCard()
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
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
            if (displayedDownloadedSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Songs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                    }
                }

                itemsIndexed(displayedDownloadedSongs.take(10), key = { _, song -> song.id }) { index, song ->
                    DarkSongListItem(
                        song = song,
                        isPlaying = currentSong?.id == song.id,
                        onClick = {
                            viewModel.playSong(displayedDownloadedSongs, index)
                            onNavigateToNowPlaying()
                        }
                    )
                }
            }
        } else {
            // ── Category Specific Mode (Party, Blues, Sad, etc.) ───────────────────

            // 1. Downloaded in Category
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloaded in $selectedCategory",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    if (displayedDownloadedSongs.isNotEmpty()) {
                        Text(
                            text = "${displayedDownloadedSongs.size} songs",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeonLimePrimary
                        )
                    }
                }
            }

            if (displayedDownloadedSongs.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        itemsIndexed(displayedDownloadedSongs, key = { _, song -> song.id }) { index, song ->
                            PopularSongCard(
                                song = song,
                                isPlaying = currentSong?.id == song.id,
                                onClick = {
                                    viewModel.playSong(displayedDownloadedSongs, index)
                                    onNavigateToNowPlaying()
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceLight)
                            .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No downloaded $selectedCategory songs",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = OnSurface
                                )
                                Text(
                                    text = "Stream from 30+ $selectedCategory recommendations below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 2. Recommendations for Category (Top Carousel)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedCategory Recommendations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    if (recommendations.isNotEmpty()) {
                        Text(
                            text = "${recommendations.size} tracks",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NeonLimePrimary
                        )
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recommendations.isNotEmpty()) {
                        itemsIndexed(recommendations.take(10), key = { _, reco -> reco.videoId }) { index, reco ->
                            RecommendedSongCard(
                                song = reco,
                                isPlaying = currentSong?.videoId == reco.videoId || currentSong?.id == reco.videoId,
                                onClick = {
                                    viewModel.playRecommendedSongs(recommendations, index)
                                    onNavigateToNowPlaying()
                                }
                            )
                        }
                    } else {
                        items(4) {
                            RecommendationPlaceholderCard()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 3. All Category Recommendations List (Minimum 30 songs)
            if (recommendations.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All $selectedCategory Tracks (${recommendations.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                    }
                }

                itemsIndexed(recommendations, key = { index, s -> "${s.videoId}_$index" }) { index, reco ->
                    DarkRecommendationListItem(
                        song = reco,
                        isPlaying = currentSong?.videoId == reco.videoId || currentSong?.id == reco.videoId,
                        onClick = {
                            viewModel.playRecommendedSongs(recommendations, index)
                            onNavigateToNowPlaying()
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // ── Empty State ───────────────────────────────────────────────────────
        if (allSongs.isEmpty() && recentSongs.isEmpty() && recommendations.isEmpty()) {
            item { DarkEmptyState() }
        }
    }
}

// ─── Recommended Song Card (Trending / Radio discoveries) ───────────────────

@Composable
private fun RecommendationPlaceholderCard() {
    Box(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLight)
            .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerLow)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceContainerLow)
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceContainerLow)
            )
        }
    }
}

@Composable
private fun RecommendedSongCard(
    song: RecommendationSong,
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
                    uri = song.thumbnailUrl,
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
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
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

@Composable
private fun DarkRecommendationListItem(
    song: RecommendationSong,
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
                uri = song.thumbnailUrl,
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
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = NeonLimePrimary,
                modifier = Modifier.size(20.dp)
            )
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
