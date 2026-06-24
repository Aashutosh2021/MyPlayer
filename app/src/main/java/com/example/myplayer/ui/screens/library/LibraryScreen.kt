package com.example.myplayer.ui.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myplayer.data.local.entity.FolderEntity
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlaylistClick: (Long) -> Unit,
    onPlaySong: (PlayableSong) -> Unit,
    bottomPadding: Dp = 100.dp,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val hybridLibrary by viewModel.hybridLibrary.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val context = LocalContext.current

    // SAF folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: uri.toString()
            viewModel.addFolder(uri.toString(), folderName)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf<FolderEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<PlayableSong?>(null) }

    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            title = { Text("Remove Folder") },
            text = { Text("Remove \"${folder.name}\" and delete all its songs from your library?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.removeFolder(folder); showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                ) { Text("Remove") }
            },
            dismissButton = { 
                TextButton(
                    onClick = { showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) { Text("Cancel") } 
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false; newPlaylistName = "" },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClayPrimary,
                        focusedLabelColor = ClayPrimary,
                        unfocusedBorderColor = SurfaceContainerHigh,
                        unfocusedLabelColor = OnSurfaceVariant,
                        cursorColor = ClayPrimary
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
                    colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false; newPlaylistName = "" },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) { Text("Cancel") }
            }
        )
    }

    songToAddToPlaylist?.let { song ->
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
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .claySurface(borderRadius = 12.dp, backgroundColor = SurfaceLight)
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, song)
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
                ) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
                )
                Text(
                    "${hybridLibrary.size} songs total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = ClayPrimary)
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clayConcave(borderRadius = 20.dp, backgroundColor = SurfaceContainerLow)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                "All Songs" to Icons.Filled.LibraryMusic,
                "Folders" to Icons.Filled.Folder,
                "Playlists" to Icons.AutoMirrored.Filled.QueueMusic
            )
            
            tabs.forEachIndexed { index, (label, icon) ->
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
                    Icon(icon, null, tint = if (active) ClayPrimary else OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label, 
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium), 
                        color = if (active) ClayPrimary else OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> HybridSongsTab(
                songs = hybridLibrary,
                onPlaySong = onPlaySong,
                onAddToPlaylist = { songToAddToPlaylist = it },
                bottomPadding = bottomPadding
            )
            1 -> FoldersTab(
                folders = folders,
                onAddFolder = { folderPickerLauncher.launch(null) },
                onRescan = viewModel::rescanFolder,
                onDelete = { showDeleteDialog = it },
                bottomPadding = bottomPadding
            )
            2 -> PlaylistsTab(
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
fun HybridSongsTab(
    songs: List<PlayableSong>,
    onPlaySong: (PlayableSong) -> Unit,
    onAddToPlaylist: (PlayableSong) -> Unit,
    bottomPadding: Dp
) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LibraryMusic, null, modifier = Modifier.size(48.dp), tint = TextMuted)
                }
                Spacer(Modifier.height(24.dp))
                Text("No songs yet", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Spacer(Modifier.height(8.dp))
                Text("Add folders or download songs online", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding, top = 8.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            HybridSongItem(
                song = song,
                onPlaySong = { onPlaySong(song) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
fun HybridSongItem(song: PlayableSong, onPlaySong: () -> Unit, onAddToPlaylist: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable(onClick = onPlaySong)
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
            val artUrl = song.thumbnailUrl
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(model = artUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = TextMuted)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                // Source badge
                val isDownloaded = song is PlayableSong.Downloaded
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .claySurface(borderRadius = 6.dp, backgroundColor = SurfaceContainerLow)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDownloaded) Icons.Filled.CloudDone else Icons.Filled.PhoneAndroid,
                            null, modifier = Modifier.size(10.dp),
                            tint = if (isDownloaded) ClayPrimary else OnSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isDownloaded) "SAVED" else "LOCAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isDownloaded) ClayPrimary else OnSurfaceVariant
                        )
                    }
                }
            }
        }
        Box {
            ClayIconButton(onClick = { showMenu = true }, size = 36.dp) {
                Icon(Icons.Filled.MoreVert, "More", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceLight)) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = OnSurface) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = OnSurfaceVariant) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
            }
        }
    }
}

@Composable
fun FoldersTab(
    folders: List<FolderEntity>,
    onAddFolder: () -> Unit,
    onRescan: (FolderEntity) -> Unit,
    onDelete: (FolderEntity) -> Unit,
    bottomPadding: Dp
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding, top = 8.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${folders.size} folder(s)", style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant)
                
                Box(
                    modifier = Modifier
                        .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
                        .clickable(onClick = onAddFolder)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp), tint = ClayPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Folder", style = MaterialTheme.typography.labelMedium, color = ClayPrimary)
                    }
                }
            }
        }
        
        if (folders.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(48.dp), tint = TextMuted)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("No folders added yet", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    }
                }
            }
        } else {
            items(folders, key = { it.uri }) { folder -> FolderItem(folder, { onRescan(folder) }, { onDelete(folder) }) }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<com.example.myplayer.data.local.entity.PlaylistEntity>,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (com.example.myplayer.data.local.entity.PlaylistEntity) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    bottomPadding: Dp
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding, top = 8.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${playlists.size} playlist(s)", style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant)
                
                Box(
                    modifier = Modifier
                        .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
                        .clickable(onClick = onCreatePlaylist)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp), tint = ClayPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("New Playlist", style = MaterialTheme.typography.labelMedium, color = ClayPrimary)
                    }
                }
            }
        }
        
        if (playlists.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, modifier = Modifier.size(48.dp), tint = TextMuted)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("No playlists yet", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    }
                }
            }
        } else {
            items(playlists, key = { it.id }) { playlist -> 
                PlaylistItem(
                    name = playlist.name, 
                    onClick = { onPlaylistClick(playlist.id) },
                    onDelete = { onDeletePlaylist(playlist) }
                ) 
            }
        }
    }
}

@Composable
fun FolderItem(folder: FolderEntity, onRescan: () -> Unit, onDelete: () -> Unit) {
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
                .size(48.dp)
                .clayConcave(borderRadius = 12.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Folder, null, tint = ClayPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(folder.uri, style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            ClayIconButton(onClick = { showMenu = true }, size = 36.dp) {
                Icon(Icons.Filled.MoreVert, "More", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceLight)) {
                DropdownMenuItem(text = { Text("Rescan", color = OnSurface) },
                    leadingIcon = { Icon(Icons.Filled.Refresh, null, tint = OnSurfaceVariant) },
                    onClick = { showMenu = false; onRescan() })
                DropdownMenuItem(text = { Text("Remove", color = Color(0xFFBA1A1A)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                    onClick = { showMenu = false; onDelete() })
            }
        }
    }
}

@Composable
fun PlaylistItem(name: String, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clayConcave(borderRadius = 12.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = ClayPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = OnSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box {
            ClayIconButton(onClick = { showMenu = true }, size = 36.dp) {
                Icon(Icons.Filled.MoreVert, "More", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceLight)) {
                DropdownMenuItem(text = { Text("Delete", color = Color(0xFFBA1A1A)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                    onClick = { showMenu = false; onDelete() })
            }
        }
    }
}
