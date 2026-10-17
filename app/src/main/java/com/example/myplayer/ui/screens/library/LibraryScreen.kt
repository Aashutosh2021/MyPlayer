package com.example.myplayer.ui.screens.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
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
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.repository.PlayableSong
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.example.myplayer.R
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.theme.*

@Composable
fun LibraryScreen(
    onPlaylistClick: (Long) -> Unit,
    onPlaySong: (List<PlayableSong>, Int) -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<PlayableSong?>(null) }
    var songToRemoveDownload by remember { mutableStateOf<PlayableSong?>(null) }

    songToRemoveDownload?.let { song ->
        AlertDialog(
            onDismissRequest = { songToRemoveDownload = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.secondary,
            title = { Text("Remove Download?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove the downloaded audio file from your device storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDownload(song.id)
                        songToRemoveDownload = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                ) { Text("Remove", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToRemoveDownload = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Cancel") }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false; newPlaylistName = "" },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.secondary,
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""; showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false; newPlaylistName = "" },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Cancel") }
            }
        )
    }

    songToAddToPlaylist?.let { song ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.secondary,
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists available. Create one first.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(playlists.size, key = { playlists[it].id }) { idx ->
                            val playlist = playlists[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF4F6F9))
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, song)
                                        songToAddToPlaylist = null
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { songToAddToPlaylist = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Top Header Bar (Stitch Screen 8) ──────────────────────────────────
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
                    painter = painterResource(R.drawable.app_icon_spark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        text = "Library",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Stitch Screen 8: Category Filter Chips ────────────────────────────
        val categoryTabs = remember { listOf("Downloaded", "Playlists") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryTabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) PrimaryContainer else SurfaceContainer)
                        .clickable { selectedTab = index }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (title == "Downloaded") {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = if (isSelected) OnPrimary else Secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) OnPrimary else Secondary
                        )
                    }
                }
            }
        }

        // ── Stitch Screen 8: Sort & Action Row ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { /* Toggle sort */ }
            ) {
                Icon(Icons.Filled.SwapVert, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (selectedTab == 0) "Alphabetical" else "Recently Added",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = OnSurface
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Outline, modifier = Modifier.size(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = Secondary, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(2.dp, CircleShape, spotColor = Color(0x0C000000))
                        .clip(CircleShape)
                        .background(SurfaceContainer)
                        .clickable { showCreatePlaylistDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Playlist", tint = OnSurface, modifier = Modifier.size(18.dp))
                }
            }
        }

        when (selectedTab) {
            0 -> DownloadedSongsTab(
                songs = remember(downloadedSongs) { downloadedSongs.sortedBy { it.title.lowercase() } },
                onPlaySong = onPlaySong,
                onAddToPlaylist = { songToAddToPlaylist = it },
                onRemoveDownload = { songToRemoveDownload = it },
                bottomPadding = bottomPadding
            )
            else -> PlaylistsTab(
                playlists = playlists,
                favoriteSongs = favoriteSongs,
                onPlayFavoriteSongs = { onPlaySong(favoriteSongs, 0) },
                onCreatePlaylist = { showCreatePlaylistDialog = true },
                onDeletePlaylist = viewModel::deletePlaylist,
                onPlaylistClick = onPlaylistClick,
                bottomPadding = bottomPadding
            )
        }
    }
}

@Composable
fun DownloadedSongsTab(
    songs: List<PlayableSong>,
    onPlaySong: (List<PlayableSong>, Int) -> Unit,
    onAddToPlaylist: (PlayableSong) -> Unit,
    onRemoveDownload: (PlayableSong) -> Unit,
    bottomPadding: Dp
) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECEFF4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DownloadDone, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(18.dp))
                Text("No downloaded songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Download songs from Search or Online charts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        return
    }

    val firstSong = songs.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 20.dp, top = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Album / Collection Header Card (Image 1 Right) ───────────────────
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
                        uri = firstSong?.thumbnailUrl,
                        title = firstSong?.title ?: "Downloads",
                        artist = firstSong?.artist ?: "Offline",
                        size = 92.dp,
                        shape = RoundedCornerShape(20.dp),
                        iconSize = 36.dp
                    )
                }

                Spacer(Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Alphabetical • ${songs.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Offline Library",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                // Play Pill
                Button(
                    onClick = { onPlaySong(songs, 0) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Shuffle Pill
                OutlinedButton(
                    onClick = {
                        val shuffled = songs.shuffled()
                        onPlaySong(shuffled, 0)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // ── Song List Items (01, 02, 03... format) ───────────────────────────
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id },
            contentType = { _, _ -> "downloaded_song" }
        ) { index, song ->
            DownloadedSongListItem(
                index = index + 1,
                song = song,
                onPlay = { onPlaySong(songs, index) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onRemoveDownload = { onRemoveDownload(song) }
            )
        }
    }
}

@Composable
fun DownloadedSongListItem(
    index: Int,
    song: PlayableSong,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Number (01, 02, 03...)
        Text(
            text = "%02d".format(index),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(32.dp)
        )

        Spacer(Modifier.width(12.dp))

        // Title and Artist • Duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${song.artist} • ${formatDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Options Menu (...)
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Remove Download", color = Color(0xFFBA1A1A)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                    onClick = {
                        showMenu = false
                        onRemoveDownload()
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<PlaylistEntity>,
    favoriteSongs: List<PlayableSong>,
    onPlayFavoriteSongs: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    bottomPadding: Dp
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 20.dp, top = 8.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Pinned Feature: Liked Songs Hero Card (Stitch Screen 8) ───────────
        if (favoriteSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x10171C26))
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .clickable(onClick = onPlayFavoriteSongs)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33171C26))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(PrimaryContainer, Secondary, PrimaryContainer))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Liked Songs",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnSurface
                                )
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${favoriteSongs.size} tracks • Auto-synced",
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(2.dp, CircleShape, spotColor = Color(0x14000000))
                            .clip(CircleShape)
                            .background(SurfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ── Section Header (Stitch Screen 8) ─────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR COLLECTIONS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = Secondary
                )
                Text(
                    text = "${playlists.size} Playlist${if (playlists.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Outline
                )
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECEFF4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No playlists yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text("Create a custom playlist to organize your favorite tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        } else {
            items(playlists.size, key = { playlists[it].id }) { index ->
                val playlist = playlists[index]
                var showDeleteConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        textContentColor = MaterialTheme.colorScheme.secondary,
                        title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to delete '${playlist.name}'?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDeletePlaylist(playlist)
                                    showDeleteConfirm = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                            ) { Text("Delete", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLowest)
                        .clickable { onPlaylistClick(playlist.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(3.dp, RoundedCornerShape(12.dp), spotColor = Color(0x14171C26))
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Created ${formatDate(playlist.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Secondary)
                    }
                }
            }
        }

        // ── Smart Auto-Mix Tactile Well (Stitch Screen 8) ────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Smart Auto-Mix",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OnSurface
                        )
                        Text(
                            text = "Generate mix from recent moods",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                    }
                }
                Button(
                    onClick = onCreatePlaylist,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainer,
                        contentColor = OnSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Create", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}


