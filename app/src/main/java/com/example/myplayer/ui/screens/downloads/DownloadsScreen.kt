package com.example.myplayer.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.filteredDownloads.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    var songToDelete by remember { mutableStateOf<DownloadedSongEntity?>(null) }

    // Delete confirmation dialog
    songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Remove Download?") },
            text = { Text("This will remove the downloaded audio and any associated offline resources from your device. The song will remain available for online streaming.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSong(song)
                        songToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) { Text("Cancel") }
            },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Downloads",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Text(
                        "${downloads.size} song${if (downloads.size != 1) "s" else ""} saved offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clayConcave(borderRadius = 24.dp, backgroundColor = SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = ClayPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search downloads…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClayPrimary,
                    unfocusedBorderColor = SurfaceContainerHigh,
                    focusedContainerColor = SurfaceLight,
                    unfocusedContainerColor = SurfaceLight,
                    cursorColor = ClayPrimary
                )
            )
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clayConcave(borderRadius = 60.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DownloadForOffline,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = TextMuted
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        if (query.isBlank()) "No downloads yet" else "No results for \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface
                    )
                    if (query.isBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Search online and tap download to save",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { song ->
                    DownloadedSongItem(
                        song = song,
                        onPlayClick = {
                            viewModel.playSong(song)
                            onNavigateToNowPlaying()
                        },
                        onDeleteClick = { songToDelete = song }
                    )
                }
                item { Spacer(Modifier.height(bottomPadding + 20.dp)) }
            }
        }
    }
}

@Composable
fun DownloadedSongItem(
    song: DownloadedSongEntity,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable(onClick = onPlayClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Art
        Box(
            modifier = Modifier
                .size(52.dp)
                .clayConcave(borderRadius = 12.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (song.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = TextMuted)
            }
        }
        
        Spacer(Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Offline badge
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .claySurface(borderRadius = 4.dp, backgroundColor = SurfaceContainerLow)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.OfflinePin,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = ClayPrimary
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "OFFLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = ClayPrimary
                        )
                    }
                }
            }
        }

        // Options
        Box {
            ClayIconButton(onClick = { showMenu = true }, size = 36.dp) {
                Icon(Icons.Filled.MoreVert, "Options", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SurfaceLight)
            ) {
                DropdownMenuItem(
                    text = { Text("Play", color = OnSurface) },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, null, tint = OnSurfaceVariant) },
                    onClick = { showMenu = false; onPlayClick() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFBA1A1A)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                    onClick = { showMenu = false; onDeleteClick() }
                )
            }
        }
    }
}
