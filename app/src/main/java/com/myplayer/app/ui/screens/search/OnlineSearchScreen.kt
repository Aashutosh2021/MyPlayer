package com.myplayer.app.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.myplayer.app.data.online.model.OnlineSong
import com.myplayer.app.data.local.entity.RecentSearchEntity
import com.myplayer.app.data.local.entity.PlayedSongHistoryEntity
import com.myplayer.app.ui.components.ClayIconButton
import com.myplayer.app.ui.myplayer.*
import com.myplayer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: OnlineSearchViewModel = hiltViewModel() // parent can pass its own instance
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val playedHistory by viewModel.playedHistory.collectAsStateWithLifecycle()
    val isLoadingStream by viewModel.isLoadingStream.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

    var songToAddToPlaylist by remember { mutableStateOf<OnlineSong?>(null) }
    var showAddAllDialog by remember { mutableStateOf(false) }
    var songToRemoveDownload by remember { mutableStateOf<OnlineSong?>(null) }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    if (songToRemoveDownload != null) {
        AlertDialog(
            onDismissRequest = { songToRemoveDownload = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Remove Download?") },
            text = { Text("This will remove the downloaded audio and any associated offline resources from your device. The song will remain available for online streaming.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownload(songToRemoveDownload!!.videoId)
                        songToRemoveDownload = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToRemoveDownload = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("Cancel") }
            }
        )
    }

    if (songToAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Add to Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists available. Create one first.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .claySurface(borderRadius = 12.dp, backgroundColor = MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, songToAddToPlaylist!!)
                                        songToAddToPlaylist = null
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = ClayPrimary)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { songToAddToPlaylist = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddAllDialog) {
        AlertDialog(
            onDismissRequest = { showAddAllDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Add All Results to Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists available. Create one first.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .claySurface(borderRadius = 12.dp, backgroundColor = MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        val items = searchResults.itemSnapshotList.items.filterNotNull()
                                        viewModel.addSongsToPlaylist(playlist.id, items)
                                        showAddAllDialog = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = ClayPrimary)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAddAllDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content — no search bar here; it lives in the parent SearchScreen
        if (query.length < 2) {
            SearchHistorySection(
                playedHistory = playedHistory,
                searches = recentSearches,
                onPlayHistorySong = { song ->
                    viewModel.playHistorySong(song)
                    onNavigateToNowPlaying()
                },
                onDeleteHistorySong = { id -> viewModel.deletePlayedHistoryItem(id) },
                onClearHistory = { viewModel.clearPlayedHistory() },
                onSearchClick = { q -> viewModel.onSearch(q) },
                onDeleteSearchClick = { q -> viewModel.deleteRecentSearch(q) },
                bottomPadding = bottomPadding
            )
        } else {
            OnlineResultsList(
                results = searchResults,
                downloadedIds = downloadedIds,
                isLoadingStream = isLoadingStream,
                downloadProgress = downloadProgress,
                onPlayClick = { song, contextSongs ->
                    viewModel.playSongFromSearch(song, contextSongs)
                    onNavigateToNowPlaying()
                },
                onDownloadClick = { song -> viewModel.downloadSong(song) },
                onAddToPlaylistClick = { song -> songToAddToPlaylist = song },
                onRemoveDownloadClick = { songToRemoveDownload = it },
                onAddAllClick = { showAddAllDialog = true },
                bottomPadding = bottomPadding
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SearchHistorySection(
    playedHistory: List<PlayedSongHistoryEntity>,
    searches: List<RecentSearchEntity>,
    onPlayHistorySong: (PlayedSongHistoryEntity) -> Unit,
    onDeleteHistorySong: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSearchClick: (String) -> Unit,
    onDeleteSearchClick: (String) -> Unit,
    bottomPadding: Dp = 100.dp
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Clear Listening History?") },
            text = { Text("This will clear your played songs history from the last 15 days.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── MyPlayer Moods & Moments ──────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Moods & Moments",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "MyPlayer AI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    items(MYPLAYER_MOODS.size) { idx ->
                        val mood = MYPLAYER_MOODS[idx]
                        MyPlayerMoodGenreCard(
                            item = mood,
                            onClick = { onSearchClick(mood.title) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }

        // ── MyPlayer Explore Genres ───────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Explore Genres",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    items(MYPLAYER_GENRES.size) { idx ->
                        val genre = MYPLAYER_GENRES[idx]
                        MyPlayerMoodGenreCard(
                            item = genre,
                            onClick = { onSearchClick(genre.title) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }

        if (playedHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Played in last 15 days",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "${playedHistory.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            items(playedHistory.size, key = { i -> "history_${playedHistory[i].id}" }) { i ->
                val song = playedHistory[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .slidePopIn(i)
                        .claySurface(borderRadius = 16.dp, backgroundColor = MaterialTheme.colorScheme.surface)
                        .bouncyPopClick { onPlayHistorySong(song) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!song.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!song.isOnline) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (ThemeState.isDarkMode) Color(0xFF66BB6A) else Color(0xFF2E7D32).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FileDownloadDone,
                                            contentDescription = null,
                                            tint = if (ThemeState.isDarkMode) Color(0xFF66BB6A) else Color(0xFF2E7D32),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "Downloaded",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = if (ThemeState.isDarkMode) Color(0xFF66BB6A) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "Online",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${song.artist.ifBlank { "Unknown" }} • ${formatRelativeTime(song.playedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { onPlayHistorySong(song) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteHistorySong(song.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove from history",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (searches.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recent Searches",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(searches.size, key = { i -> "search_${searches[i].query}" }) { i ->
                val s = searches[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .slidePopIn(i)
                        .claySurface(borderRadius = 16.dp, backgroundColor = MaterialTheme.colorScheme.surface)
                        .bouncyPopClick { onSearchClick(s.query) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        s.query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteSearchClick(s.query) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0L)
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 15 -> "${days}d ago"
        else -> "15d ago"
    }
}

@Composable
fun RecentSearchesSection(
    searches: List<RecentSearchEntity>,
    onSearchClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    bottomPadding: Dp = 100.dp
) {
    SearchHistorySection(
        playedHistory = emptyList(),
        searches = searches,
        onPlayHistorySong = {},
        onDeleteHistorySong = {},
        onClearHistory = {},
        onSearchClick = onSearchClick,
        onDeleteSearchClick = onDeleteClick,
        bottomPadding = bottomPadding
    )
}

@Composable
fun OnlineResultsList(
    results: LazyPagingItems<OnlineSong>,
    downloadedIds: Set<String>,
    isLoadingStream: String?,
    downloadProgress: Map<String, Int>,
    onPlayClick: (OnlineSong, List<OnlineSong>) -> Unit,
    onDownloadClick: (OnlineSong) -> Unit,
    onAddToPlaylistClick: (OnlineSong) -> Unit,
    onRemoveDownloadClick: (OnlineSong) -> Unit,
    onAddAllClick: () -> Unit,
    bottomPadding: Dp = 100.dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = bottomPadding, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Loading header
        if (results.loadState.refresh is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ClayPrimary)
                }
            }
        }

        // Add All button
        if (results.itemCount > 0 && results.loadState.refresh !is LoadState.Loading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${results.itemCount} online results",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onAddAllClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add All to Playlist")
                    }
                }
            }
        }

        // Error state
        if (results.loadState.refresh is LoadState.Error) {
            item {
                val e = (results.loadState.refresh as LoadState.Error).error
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clayConcave(borderRadius = 40.dp, backgroundColor = MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Couldn't reach server", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(e.message ?: "Unknown error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { results.retry() }, colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary)) { Text("Retry") }
                }
            }
        }

        items(results.itemCount, key = results.itemKey { it.videoId }) { index ->
            val song = results[index] ?: return@items
            val isDownloaded = song.videoId in downloadedIds
            val isStreaming = isLoadingStream == song.videoId
            val isDownloading = isLoadingStream == song.videoId + "_dl" || song.videoId in downloadProgress
 
            OnlineSongCard(
                index = index,
                song = song,
                isDownloaded = isDownloaded,
                isStreaming = isStreaming,
                isDownloading = isDownloading,
                onPlayClick = {
                    val contextSongs = mutableListOf<OnlineSong>()
                    for (i in 0 until results.itemCount) {
                        results.peek(i)?.let { contextSongs.add(it) }
                    }
                    onPlayClick(song, contextSongs)
                },
                onDownloadClick = { onDownloadClick(song) },
                onAddToPlaylistClick = { onAddToPlaylistClick(song) },
                onRemoveDownloadClick = { onRemoveDownloadClick(song) }
            )
        }

        // Load more indicator
        if (results.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ClayPrimary)
                }
            }
        }
    }
}

@Composable
fun OnlineSongCard(
    index: Int = 0,
    song: OnlineSong,
    isDownloaded: Boolean,
    isStreaming: Boolean,
    isDownloading: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .slidePopIn(index)
            .bouncyPopClick(onClick = onPlayClick)
            .claySurface(borderRadius = 20.dp, backgroundColor = MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clayConcave(borderRadius = 14.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            val smallThumbUrl = remember(song.thumbnailUrl) {
                song.thumbnailUrl
                    .replace("=w800-h800-l90-rj", "=w226-h226-l90-rj")
                    .replace("w800-h800", "w226-h226")
            }
            AsyncImage(
                model = smallThumbUrl.ifBlank {
                    com.myplayer.app.data.artwork.model.ArtworkModel(
                        title = song.title,
                        artist = song.artist,
                        localUri = song.thumbnailUrl
                    )
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            // Play loading overlay
            if (isStreaming) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (song.durationText.isNotBlank()) {
                    Text(
                        " • ${song.durationText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Download button
            when {
                isDownloaded -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = ClayPrimary,
                        modifier = Modifier.size(20.dp).padding(2.dp)
                    )
                }
                isDownloading -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ClayPrimary)
                }
                else -> {
                    ClayIconButton(onClick = onDownloadClick, size = 36.dp) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Play button
            if (!isStreaming) {
                ClayIconButton(onClick = onPlayClick, size = 40.dp) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Stream",
                        tint = ClayPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // More Options (Add to Playlist) Dropdown
            Box {
                ClayIconButton(onClick = { showMenu = true }, size = 36.dp) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (showMenu) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                onAddToPlaylistClick()
                            }
                        )
                        if (isDownloaded) {
                            DropdownMenuItem(
                                text = { Text("Remove Download", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onRemoveDownloadClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


