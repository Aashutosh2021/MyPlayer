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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<PlayableSong?>(null) }
    var songToRemoveDownload by remember { mutableStateOf<PlayableSong?>(null) }

    songToRemoveDownload?.let { song ->
        AlertDialog(
            onDismissRequest = { songToRemoveDownload = null },
            containerColor = PureWhite,
            titleContentColor = CharcoalPrimary,
            textContentColor = SlateSecondary,
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
                    colors = ButtonDefaults.textButtonColors(contentColor = SlateSecondary)
                ) { Text("Cancel") }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false; newPlaylistName = "" },
            containerColor = PureWhite,
            titleContentColor = CharcoalPrimary,
            textContentColor = SlateSecondary,
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CharcoalPrimary,
                        focusedLabelColor = CharcoalPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        unfocusedLabelColor = SlateSecondary,
                        cursorColor = CharcoalPrimary
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
                    colors = ButtonDefaults.textButtonColors(contentColor = CharcoalPrimary)
                ) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false; newPlaylistName = "" },
                    colors = ButtonDefaults.textButtonColors(contentColor = SlateSecondary)
                ) { Text("Cancel") }
            }
        )
    }

    songToAddToPlaylist?.let { song ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = PureWhite,
            titleContentColor = CharcoalPrimary,
            textContentColor = SlateSecondary,
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
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = CharcoalPrimary)
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = CharcoalPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { songToAddToPlaylist = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = CharcoalPrimary)
                ) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBackground)
            .statusBarsPadding()
    ) {
        // ── Top Bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = CharcoalPrimary
            )

            // Tab Switcher Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PureWhite)
                    .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                val tabs = listOf("Downloaded", "Playlist")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) CharcoalPrimary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PureWhite else SlateSecondary
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> DownloadedSongsTab(
                songs = downloadedSongs,
                onPlaySong = onPlaySong,
                onAddToPlaylist = { songToAddToPlaylist = it },
                onRemoveDownload = { songToRemoveDownload = it },
                bottomPadding = bottomPadding
            )
            1 -> PlaylistsTab(
                playlists = playlists,
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
                    Icon(Icons.Filled.DownloadDone, null, modifier = Modifier.size(40.dp), tint = LightSlateMuted)
                }
                Spacer(Modifier.height(18.dp))
                Text("No downloaded songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Download songs from Search or Online charts", style = MaterialTheme.typography.bodyMedium, color = SlateSecondary)
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
                        color = CharcoalPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Alphabetical • ${songs.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Offline Library",
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
                // Play Pill
                Button(
                    onClick = { onPlaySong(songs, 0) },
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
            color = SlateSecondary,
            modifier = Modifier.width(32.dp)
        )

        Spacer(Modifier.width(12.dp))

        // Title and Artist • Duration
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

        // Options Menu (...)
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "Options",
                    tint = LightSlateMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(PureWhite)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = CharcoalPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = CharcoalPrimary) },
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
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${playlists.size} Playlist${if (playlists.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalPrimary
                )

                Button(
                    onClick = onCreatePlaylist,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalPrimary,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Playlist", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(40.dp), tint = LightSlateMuted)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No playlists yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Create a custom playlist to organize your favorite tracks", style = MaterialTheme.typography.bodyMedium, color = SlateSecondary)
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
                        containerColor = PureWhite,
                        titleContentColor = CharcoalPrimary,
                        textContentColor = SlateSecondary,
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
                        .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = Color(0x12000000))
                        .clip(RoundedCornerShape(16.dp))
                        .background(PureWhite)
                        .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(16.dp))
                        .clickable { onPlaylistClick(playlist.id) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F2F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = CharcoalPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = CharcoalPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Created ${formatDate(playlist.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondary
                        )
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = LightSlateMuted)
                    }
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
