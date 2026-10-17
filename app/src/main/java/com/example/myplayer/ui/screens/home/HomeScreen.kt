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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import com.example.myplayer.R
import com.example.myplayer.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
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
    val popularPool = remember(mostPlayed, recentSongs, allSongs) {
        (mostPlayed.ifEmpty { recentSongs.ifEmpty { allSongs } }).take(8)
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val displayedDownloadedSongs by viewModel.displayedDownloadedSongs.collectAsStateWithLifecycle()
    val categories = remember {
        listOf("All", "Deep Focus", "Acoustic Chill", "Late Night", "Discoveries", "Party", "Blues", "Sad", "Hip Hop", "Chill", "Workout", "Pop")
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = CardBorderOlive,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val jumpBackInSongs = remember(popularPool, recentSongs, allSongs) {
        (recentSongs.ifEmpty { popularPool.ifEmpty { allSongs } }).take(8)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
    ) {
        // ── Top Header Bar (Stitch Screen 7) ──────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.inside_logo),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                    Column {
                        Text(
                            text = "MYPLAYER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onNavigateToSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Welcome & Greeting (Stitch Screen 7) ──────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .clickable {
                            nameInput = userName
                            showEditNameDialog = true
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "WELCOME BACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$greeting${if (userName.isNotBlank()) ", $userName" else ""}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        label = "pillBg_$category"
                    )
                    val textTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            // ── Stitch Screen 7: Jump Back In (Horizontal Scroll Section) ────────
            if (jumpBackInSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jump Back In",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                    }
                }

                item {
                    val jumpBackInListState = rememberLazyListState()
                    LazyRow(
                        state = jumpBackInListState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        itemsIndexed(jumpBackInSongs, key = { _, song -> song.id }) { index, song ->
                            JumpBackInCard(
                                song = song,
                                isPlaying = currentSong?.id == song.id,
                                modifier = Modifier.carouselSlidePop(jumpBackInListState, index),
                                onClick = {
                                    viewModel.playSong(jumpBackInSongs, index)
                                    onNavigateToNowPlaying()
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── Stitch Screen 7: Your Quick Mixes (2x2 Bento Tiles!) ────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Quick Mixes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Text(
                        text = "Updated today",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickMixBentoTile(
                            icon = Icons.Filled.GraphicEq,
                            title = "Deep Focus",
                            subtitle = "Drone, Lo-Fi & Ambient",
                            modifier = Modifier.weight(1f).slidePopIn(0),
                            onClick = { viewModel.selectCategory("Deep Focus") },
                            onPlayClick = {
                                viewModel.playCategory("Deep Focus") { onNavigateToNowPlaying() }
                            }
                        )
                        QuickMixBentoTile(
                            icon = Icons.Filled.MusicNote,
                            title = "Acoustic Chill",
                            subtitle = "Warm strings & vocals",
                            modifier = Modifier.weight(1f).slidePopIn(1),
                            onClick = { viewModel.selectCategory("Acoustic Chill") },
                            onPlayClick = {
                                viewModel.playCategory("Acoustic Chill") { onNavigateToNowPlaying() }
                            }
                        )
                    }
                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickMixBentoTile(
                            icon = Icons.Filled.Nightlight,
                            title = "Late Night",
                            subtitle = "Synthwave & Electro",
                            modifier = Modifier.weight(1f).slidePopIn(2),
                            onClick = { viewModel.selectCategory("Late Night") },
                            onPlayClick = {
                                viewModel.playCategory("Late Night") { onNavigateToNowPlaying() }
                            }
                        )
                        QuickMixBentoTile(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Discoveries",
                            subtitle = "Custom picks for you",
                            modifier = Modifier.weight(1f).slidePopIn(3),
                            onClick = { viewModel.selectCategory("Discoveries") },
                            onPlayClick = {
                                viewModel.playCategory("Discoveries") { onNavigateToNowPlaying() }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Stitch Screen 7: Recommended For You (High-Fidelity Track List) ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recommended For You",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Button(
                        onClick = {
                            if (recommendations.isNotEmpty()) {
                                viewModel.playRecommendedSongs(recommendations, 0)
                                onNavigateToNowPlaying()
                            } else if (allSongs.isNotEmpty()) {
                                viewModel.playSong(allSongs, 0)
                                onNavigateToNowPlaying()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryContainer,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Play all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (recommendations.isNotEmpty()) {
                itemsIndexed(recommendations.take(12), key = { index, reco -> "${reco.videoId}_$index" }) { index, reco ->
                    val isPlaying = currentSong?.videoId == reco.videoId || currentSong?.id == reco.videoId
                    StitchTrackRow(
                        title = reco.title,
                        artist = reco.artist,
                        album = reco.album,
                        artUri = reco.thumbnailUrl,
                        durationMs = reco.durationMs,
                        isPlaying = isPlaying,
                        modifier = Modifier.slidePopIn(index),
                        onClick = {
                            viewModel.playRecommendedSongs(recommendations, index)
                            onNavigateToNowPlaying()
                        }
                    )
                }
            } else if (allSongs.isNotEmpty()) {
                itemsIndexed(allSongs.take(12), key = { _, s -> s.id }) { index, s ->
                    val isPlaying = currentSong?.id == s.id
                    StitchTrackRow(
                        title = s.title,
                        artist = s.artist,
                        album = s.album,
                        artUri = s.albumArt,
                        durationMs = s.duration,
                        isPlaying = isPlaying,
                        modifier = Modifier.slidePopIn(index),
                        onClick = {
                            viewModel.playSong(allSongs, index)
                            onNavigateToNowPlaying()
                        }
                    )
                }
            }
        } else {
            // ── Category Specific Mode (Deep Focus, Acoustic Chill, Late Night, etc.) ─

            // 1. Header with back arrow to All, Category Title, and Play Mix button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLow)
                                .clickable { viewModel.selectCategory("All") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to All",
                                tint = OnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                    }

                    if (recommendations.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.playRecommendedSongs(recommendations, 0)
                                onNavigateToNowPlaying()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play Mix", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 1b. Downloaded in Category (if any)
            if (displayedDownloadedSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloaded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OnSurface
                        )
                        Text(
                            text = "${displayedDownloadedSongs.size} songs",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Primary
                        )
                    }
                }
            }

            if (displayedDownloadedSongs.isNotEmpty()) {
                item {
                    val downloadedState = rememberLazyListState()
                    LazyRow(
                        state = downloadedState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        itemsIndexed(displayedDownloadedSongs, key = { _, song -> song.id }) { index, song ->
                            PopularSongCard(
                                song = song,
                                isPlaying = currentSong?.id == song.id,
                                modifier = Modifier.carouselSlidePop(downloadedState, index),
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
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No downloaded $selectedCategory songs",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Stream from 30+ $selectedCategory recommendations below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 2. Loading State or Recommendations for Category
            if (recommendations.isEmpty() && isLoadingRecommendations) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Loading $selectedCategory tracks...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                        }
                    }
                }
            }

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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (recommendations.isNotEmpty()) {
                        Text(
                            text = "${recommendations.size} tracks",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                val catRecoState = rememberLazyListState()
                LazyRow(
                    state = catRecoState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recommendations.isNotEmpty()) {
                        itemsIndexed(recommendations.take(10), key = { _, reco -> reco.videoId }) { index, reco ->
                            RecommendedSongCard(
                                song = reco,
                                isPlaying = currentSong?.videoId == reco.videoId || currentSong?.id == reco.videoId,
                                modifier = Modifier.carouselSlidePop(catRecoState, index),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                itemsIndexed(recommendations, key = { index, s -> "${s.videoId}_$index" }) { index, reco ->
                    DarkRecommendationListItem(
                        index = index,
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
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    }
}

@Composable
private fun RecommendedSongCard(
    song: RecommendationSong,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else CardBorderOlive
                ),
                RoundedCornerShape(20.dp)
            )
            .bouncyPopClick(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
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
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizerBars(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DarkRecommendationListItem(
    index: Int = 0,
    song: RecommendationSong,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val durSec = (song.durationMs / 1000).coerceAtLeast(0L)
    val durText = if (durSec > 0) " • %d:%02d".format(durSec / 60, durSec % 60) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .slidePopIn(index)
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPlaying) SoftGraySurface else Color.Transparent)
            .bouncyPopClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(34.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isPlaying) {
                Icon(
                    Icons.Filled.Equalizer,
                    contentDescription = "Playing",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "%02d".format(index + 1),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateSecondary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = CharcoalPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.artist}$durText",
                style = MaterialTheme.typography.bodySmall,
                color = SlateSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Filled.MoreHoriz,
            contentDescription = null,
            tint = SilverMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Popular Song Card (Dark Olive with square album art) ────────────────────

@Composable
private fun PopularSongCard(
    song: SongEntity,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else CardBorderOlive
                ),
                RoundedCornerShape(20.dp)
            )
            .bouncyPopClick(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
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
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizerBars(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else CardBorderOlive
                ),
                RoundedCornerShape(18.dp)
            )
            .bouncyPopClick(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
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
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Dark Song List Item ─────────────────────────────────────────────────────

@Composable
private fun DarkSongListItem(
    index: Int = 0,
    song: SongEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val durSec = (song.duration / 1000).coerceAtLeast(0L)
    val durText = if (durSec > 0) " • %d:%02d".format(durSec / 60, durSec % 60) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .slidePopIn(index)
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPlaying) SoftGraySurface else Color.Transparent)
            .bouncyPopClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(34.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isPlaying) {
                Icon(
                    Icons.Filled.Equalizer,
                    contentDescription = "Playing",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "%02d".format(index + 1),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = SlateSecondary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = CharcoalPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.artist}$durText",
                style = MaterialTheme.typography.bodySmall,
                color = SlateSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Filled.MoreHoriz,
            contentDescription = null,
            tint = SilverMuted,
            modifier = Modifier.size(20.dp)
        )
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
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, CardBorderOlive), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No music yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "Search online to stream or download songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatDurationHome(durationMs: Long): String {
    val s = durationMs / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun AnimatedEqualizerBars(
    modifier: Modifier = Modifier.size(18.dp),
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "eqBars")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b3"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barWidth = size.width / 5f
        val maxH = size.height
        val heights = floatArrayOf(bar1, bar2, bar3)
        for (i in 0..2) {
            val h = (heights[i] * maxH).coerceAtLeast(barWidth)
            val x = i * (barWidth * 1.8f) + barWidth / 2f
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x, maxH - (maxH - h) / 2f),
                end = androidx.compose.ui.geometry.Offset(x, (maxH - h) / 2f),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

// ── Stitch Screen 7 Composables ──────────────────────────────────────────────

@Composable
private fun JumpBackInCard(
    song: SongEntity,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(144.dp)
            .bouncyPopClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color(0x20171C26))
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtImage(
                uri = song.albumArt,
                title = song.title,
                artist = song.artist,
                size = 144.dp,
                shape = RoundedCornerShape(16.dp),
                iconSize = 52.dp
            )

            // Play button overlay in center
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(6.dp, CircleShape, spotColor = Color(0x33000000))
                    .clip(CircleShape)
                    .background(PureWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Top-left "Recent" badge with pulsating green dot
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryContainer.copy(alpha = 0.85f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34D399))
                )
                Text(
                    text = "Recent",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = PureWhite
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickMixBentoTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = onClick
) {
    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .bouncyPopClick(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(2.dp, RoundedCornerShape(10.dp), spotColor = Color(0x0C000000))
                        .clip(RoundedCornerShape(10.dp))
                        .background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Mix",
                        tint = OnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StitchTrackRow(
    title: String,
    artist: String,
    album: String?,
    artUri: String?,
    durationMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) SurfaceContainerLow else Color.Transparent)
            .bouncyPopClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtImage(
                uri = artUri,
                title = title,
                artist = artist,
                size = 48.dp,
                shape = RoundedCornerShape(10.dp),
                iconSize = 22.dp
            )

            // Animated Sound Wave Pill Overlay when playing
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Primary.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizerBars(modifier = Modifier.size(18.dp), color = PureWhite)
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPlaying) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Primary)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$artist${if (!album.isNullOrBlank()) " • $album" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (durationMs > 0) {
            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            Text(
                text = String.format("%d:%02d", minutes, seconds),
                style = MaterialTheme.typography.labelSmall,
                color = Secondary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        IconButton(
            onClick = { /* more options */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = Secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}



