package com.example.myplayer.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.local.entity.RecentSearchEntity
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: OnlineSearchViewModel = hiltViewModel() // parent can pass its own instance
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
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
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            title = { Text("Remove Download?") },
            text = { Text("This will remove the downloaded audio and any associated offline resources from your device. The song will remain available for online streaming.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownload(songToRemoveDownload!!.videoId)
                        songToRemoveDownload = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToRemoveDownload = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) { Text("Cancel") }
            }
        )
    }

    if (songToAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
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
                                    .claySurface(borderRadius = 12.dp, backgroundColor = SurfaceLight)
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
                                    color = OnSurface
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
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
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
                                    .claySurface(borderRadius = 12.dp, backgroundColor = SurfaceLight)
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
                                    color = OnSurface
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
            RecentSearchesSection(
                searches = recentSearches,
                onSearchClick = { q -> viewModel.onSearch(q) },
                onDeleteClick = { q -> viewModel.deleteRecentSearch(q) }
            )
        } else {
            OnlineResultsList(
                results = searchResults,
                downloadedIds = downloadedIds,
                isLoadingStream = isLoadingStream,
                downloadProgress = downloadProgress,
                onPlayClick = { song ->
                    viewModel.streamSong(song)
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
fun RecentSearchesSection(
    searches: List<RecentSearchEntity>,
    onSearchClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    if (searches.isEmpty()) {
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
                    Icon(Icons.Filled.Language, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Search online", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Spacer(Modifier.height(8.dp))
                Text("Try \"Kesariya\" or \"Blinding Lights\"", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Recent Searches",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                color = OnSurfaceVariant
            )
        }
        items(searches.size, key = { i -> searches[i].query }) { i ->
            val s = searches[i]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .claySurface(borderRadius = 16.dp, backgroundColor = SurfaceLight)
                    .clickable { onSearchClick(s.query) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    s.query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDeleteClick(s.query) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun OnlineResultsList(
    results: LazyPagingItems<OnlineSong>,
    downloadedIds: Set<String>,
    isLoadingStream: String?,
    downloadProgress: Map<String, Int>,
    onPlayClick: (OnlineSong) -> Unit,
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
                        color = OnSurfaceVariant
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
                            .clayConcave(borderRadius = 40.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Couldn't reach server", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(e.message ?: "Unknown error", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBA1A1A))
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
                song = song,
                isDownloaded = isDownloaded,
                isStreaming = isStreaming,
                isDownloading = isDownloading,
                onPlayClick = { onPlayClick(song) },
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
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
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
            val artworkRequest = remember(song.title, song.artist, song.thumbnailUrl) {
                com.example.myplayer.data.artwork.model.ArtworkModel(
                    title = song.title,
                    artist = song.artist,
                    localUri = song.thumbnailUrl
                )
            }
            AsyncImage(
                model = artworkRequest,
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
                color = OnSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                if (song.durationText.isNotBlank()) {
                    Text(
                        " • ${song.durationText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
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
                            tint = OnSurfaceVariant
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
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceLight)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = OnSurface) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = OnSurfaceVariant) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylistClick()
                        }
                    )
                    if (isDownloaded) {
                        DropdownMenuItem(
                            text = { Text("Remove Download", color = Color(0xFFBA1A1A)) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
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
