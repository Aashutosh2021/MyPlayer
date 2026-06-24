package com.example.myplayer.ui.screens.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.screens.home.formatDurationHome
import com.example.myplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    localViewModel: SearchViewModel = hiltViewModel(),
    onlineViewModel: OnlineSearchViewModel = hiltViewModel()
) {
    val localQuery by localViewModel.query.collectAsState()
    val onlineQuery by onlineViewModel.query.collectAsState()
    val results by localViewModel.searchResults.collectAsState()
    val currentSong by localViewModel.currentSong.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // The single shared search bar drives whichever VM is active
    val activeQuery = if (selectedTab == 0) localQuery else onlineQuery
    val onQueryChange: (String) -> Unit = if (selectedTab == 0)
        localViewModel::onQueryChange
    else
        onlineViewModel::onQueryChange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding()
    ) {
        
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
            Spacer(Modifier.height(20.dp))
            // ── Tabs ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayConcave(borderRadius = 20.dp, backgroundColor = SurfaceContainerLow)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Local" to Icons.Filled.PhoneAndroid, "Online" to Icons.Filled.Language).forEachIndexed { index, (label, icon) ->
                    val active = selectedTab == index
                    val tabModifier = if (active) {
                        Modifier
                            .weight(1f)
                            .claySurface(borderRadius = 14.dp, backgroundColor = SurfaceLight)
                            .padding(vertical = 12.dp)
                    } else {
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp)
                    }
                    
                    Row(
                        modifier = tabModifier,
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = if (active) ClayPrimary else OnSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) ClayPrimary else OnSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── Single unified search bar (works for both Local & Online) ─────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claySurface(borderRadius = 24.dp, backgroundColor = SurfaceLight)
            ) {
                TextField(
                    value = activeQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    placeholder = {
                        Text(
                            if (selectedTab == 0) "Search local songs, artists…" else "Search online songs, artists…",
                            color = OnSurfaceVariant
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = ClayPrimary, modifier = Modifier.size(24.dp)) },
                    trailingIcon = {
                        if (activeQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface)
                )
            }

            

            
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> PremiumLocalSearchContent(
                query = localQuery,
                results = results,
                currentSong = currentSong,
                onQueryChange = localViewModel::onQueryChange,
                bottomPadding = bottomPadding,
                onSongClick = { songs, index ->
                    localViewModel.playSong(songs, index)
                    onNavigateToNowPlaying()
                }
            )
            1 -> OnlineSearchScreen(
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                bottomPadding = bottomPadding,
                viewModel = onlineViewModel  // share the SAME instance — query already set
            )
        }
    }
}

@Composable
fun PremiumLocalSearchContent(
    query: String,
    results: List<com.example.myplayer.data.local.entity.SongEntity>,
    currentSong: com.example.myplayer.data.local.entity.SongEntity?,
    onQueryChange: (String) -> Unit,
    bottomPadding: Dp,
    onSongClick: (List<com.example.myplayer.data.local.entity.SongEntity>, Int) -> Unit
) {
    if (query.isBlank()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Search your local library", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Spacer(Modifier.height(8.dp))
                Text("Find songs, artists, or albums", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }
    } else if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SearchOff, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("No results for \"$query\"", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Spacer(Modifier.height(8.dp))
                Text("Try the Online tab to find it on YouTube Music", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomPadding, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            item {
                Text("${results.size} result(s)", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }
            itemsIndexed(results) { index, song ->
                val isPlaying = currentSong?.id == song.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
                        .clickable { onSongClick(results, index) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clayConcave(borderRadius = 14.dp)
                            .padding(2.dp)
                    ) {
                        AlbumArtImage(uri = song.path, size = 48.dp, shape = RoundedCornerShape(12.dp), iconSize = 20.dp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium),
                            color = if (isPlaying) ClayPrimary else OnSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(formatDurationHome(song.duration), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
        }
    }
}
