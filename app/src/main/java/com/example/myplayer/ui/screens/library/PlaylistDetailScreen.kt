package com.example.myplayer.ui.screens.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.repository.HybridLibraryRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlaySong: (List<PlayableSong>, Int) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(playlistId) { viewModel.setPlaylistId(playlistId) }

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var songToRemoveDownload by remember { mutableStateOf<PlayableSong?>(null) }

    songToRemoveDownload?.let { song ->
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
                        viewModel.deleteDownload(song.id)
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

    // Drag state
    var draggedSongId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    var localSongs by remember { mutableStateOf(songs) }
    
    // Sync localSongs with DB songs only when not dragging
    LaunchedEffect(songs) {
        if (draggedSongId == null) {
            localSongs = songs
        }
    }

    if (showAddSheet) {
        AddSongsSheet(
            onDismiss = { showAddSheet = false },
            onAddSongs = { selectedSongs ->
                viewModel.addSongs(selectedSongs)
                showAddSheet = false
            },
            viewModel = viewModel
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayIconButton(onClick = onBack, size = 48.dp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
            }

            Spacer(Modifier.width(16.dp))

            Text(
                playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                modifier = Modifier.weight(1f)
            )

            ClayIconButton(onClick = { showAddSheet = true }, size = 48.dp) {
                Icon(Icons.Filled.Add, "Add Songs", tint = ClayPrimary)
            }
        }

        if (localSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clayConcave(borderRadius = 50.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "No songs yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface
                    )
                    Spacer(Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .claySurface(borderRadius = 24.dp, backgroundColor = SurfaceLight)
                            .clickable { showAddSheet = true }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, null, tint = ClayPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Music", style = MaterialTheme.typography.labelLarge, color = ClayPrimary)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${localSongs.size} song${if (localSongs.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· Hold & drag to reorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                itemsIndexed(localSongs, key = { _, s -> s.id }) { index, song ->
                    val isDragged = draggedSongId == song.id
                    val elevation by animateDpAsState(if (isDragged) 16.dp else 0.dp, label = "drag_elev")

                    PlaylistSongRow(
                        song = song,
                        index = index,
                        totalCount = localSongs.size,
                        isDragging = isDragged,
                        elevation = elevation,
                        dragOffsetY = if (isDragged) dragOffsetY else 0f,
                        onPlayClick = { onPlaySong(localSongs, index) },
                        onRemove = { viewModel.removeSong(song.id) },
                        onRemoveDownload = { songToRemoveDownload = song },
                        onDownload = if (song is PlayableSong.Online) { { viewModel.downloadSong(song) } } else null,
                        onDragStart = { draggedSongId = song.id; dragOffsetY = 0f },
                        onDrag = { deltaY ->
                            dragOffsetY += deltaY
                            val currentIndex = localSongs.indexOfFirst { it.id == song.id }
                            if (currentIndex == -1) return@PlaylistSongRow

                            val itemHeight = 92f // approximate row height + spacing
                            var newIndex = currentIndex
                            if (dragOffsetY > itemHeight) {
                                newIndex = currentIndex + 1
                                dragOffsetY -= itemHeight
                            } else if (dragOffsetY < -itemHeight) {
                                newIndex = currentIndex - 1
                                dragOffsetY += itemHeight
                            }

                            newIndex = newIndex.coerceIn(0, localSongs.size - 1)
                            
                            if (newIndex != currentIndex) {
                                val currentList = localSongs.toMutableList()
                                val itemToMove = currentList.removeAt(currentIndex)
                                currentList.add(newIndex, itemToMove)
                                localSongs = currentList
                            }
                        },
                        onDragEnd = { 
                            draggedSongId = null 
                            dragOffsetY = 0f 
                            viewModel.saveReorder(localSongs.map { it.id })
                        }
                    )
                }

                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

// ── Playlist Song Row ─────────────────────────────────────────────────────────

@Composable
fun PlaylistSongRow(
    song: PlayableSong,
    index: Int,
    totalCount: Int,
    isDragging: Boolean = false,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    dragOffsetY: Float = 0f,
    onPlayClick: () -> Unit,
    onRemove: () -> Unit,
    onRemoveDownload: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .offset(y = if (isDragging) dragOffsetY.dp / 3 else 0.dp)
            .shadow(elevation, RoundedCornerShape(20.dp))
            .claySurface(borderRadius = 20.dp, backgroundColor = if (isDragging) SurfaceContainerHigh else SurfaceLight)
            .clickable(onClick = onPlayClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle (long-press to drag)
        Box(
            modifier = Modifier
                .size(36.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { currentOnDragStart() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount.y)
                        },
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = OnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Number badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clayConcave(borderRadius = 16.dp, backgroundColor = SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}",
                color = ClayPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.width(12.dp))

        // Song Art
        Box(
            modifier = Modifier
                .size(46.dp)
                .clayConcave(borderRadius = 12.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            val artUri = when (song) {
                is PlayableSong.Local -> song.entity.albumArt
                is PlayableSong.Downloaded -> song.entity.thumbnailUrl
                is PlayableSong.Online -> song.thumbnailUrl
            }
            val artworkRequest = remember(song.title, song.artist, artUri) {
                com.example.myplayer.data.artwork.model.ArtworkModel(
                    title = song.title,
                    artist = song.artist,
                    localUri = artUri
                )
            }
            AsyncImage(
                model = artworkRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        // Remove button
        Box {
            ClayIconButton(onClick = { showMenu = true }, size = 36.dp) {
                Icon(Icons.Filled.MoreVert, "More options", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SurfaceLight)
            ) {
                DropdownMenuItem(
                    text = { Text("Remove", color = Color(0xFFBA1A1A)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                    onClick = { showMenu = false; onRemove() }
                )
                if (song is PlayableSong.Downloaded && onRemoveDownload != null) {
                    DropdownMenuItem(
                        text = { Text("Remove Download", color = Color(0xFFBA1A1A)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFBA1A1A)) },
                        onClick = { showMenu = false; onRemoveDownload() }
                    )
                }
                if (song is PlayableSong.Online && onDownload != null) {
                    DropdownMenuItem(
                        text = { Text("Download", color = ClayPrimary) },
                        leadingIcon = { Icon(Icons.Filled.Download, null, tint = ClayPrimary) },
                        onClick = { showMenu = false; onDownload() }
                    )
                }
            }
        }
    }
}

// ── Add Songs Bottom Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsSheet(
    onDismiss: () -> Unit,
    onAddSongs: (List<PlayableSong>) -> Unit,
    viewModel: PlaylistDetailViewModel
) {
    val library by viewModel.libraryForPicker.collectAsStateWithLifecycle()
    val playlistSongs by viewModel.songs.collectAsStateWithLifecycle()
    val playlistSongIds = remember(playlistSongs) { playlistSongs.map { it.id }.toSet() }

    var searchQuery by remember { mutableStateOf("") }
    // Multi-select: set of selected song IDs
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filtered = remember(library, searchQuery) {
        val q = searchQuery.trim().lowercase()
        library.filter { song ->
            q.isEmpty() ||
            song.title.lowercase().contains(q) ||
            song.artist.lowercase().contains(q)
        }
    }

    // Map id -> song for fast lookup when confirming
    val songById = remember(library) { library.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CloudBlueBackground
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.88f)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Add Songs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                    if (selectedIds.isNotEmpty()) {
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayPrimary
                        )
                    }
                }
                ClayIconButton(onClick = onDismiss, size = 36.dp) {
                    Icon(Icons.Filled.Close, "Close", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (filtered.isNotEmpty()) {
                    val availableToAdd = filtered.filter { it.id !in playlistSongIds }
                    if (availableToAdd.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val isAllSelected = selectedIds.size == availableToAdd.size
                                selectedIds = if (isAllSelected) {
                                    emptySet()
                                } else {
                                    availableToAdd.map { it.id }.toSet()
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                        ) {
                            val isAllSelected = selectedIds.size == availableToAdd.size
                            Text(if (isAllSelected) "Deselect All" else "Select All")
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (library.isEmpty()) "No songs in your library yet.\nAdd a music folder from the Library tab."
                        else "No songs match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        bottom = if (selectedIds.isNotEmpty()) 100.dp else 24.dp,
                        top = 8.dp, start = 24.dp, end = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(filtered, key = { _, s -> s.id }) { _, song ->
                        val alreadyAdded = song.id in playlistSongIds
                        val isSelected = song.id in selectedIds

                        val rowBg = when {
                            alreadyAdded -> SurfaceContainerLow
                            isSelected   -> ClayPrimary.copy(alpha = 0.10f)
                            else         -> SurfaceLight
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .claySurface(borderRadius = 16.dp, backgroundColor = rowBg)
                                .clickable(enabled = !alreadyAdded) {
                                    if (!alreadyAdded) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - song.id
                                        } else {
                                            selectedIds + song.id
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox / check state
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    alreadyAdded -> Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Already in playlist",
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    isSelected -> Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = ClayPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    else -> Icon(
                                        Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = "Not selected",
                                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clayConcave(borderRadius = 10.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val artUri = when (song) {
                                    is PlayableSong.Local -> song.entity.albumArt
                                    is PlayableSong.Downloaded -> song.entity.thumbnailUrl
                                    is PlayableSong.Online -> song.thumbnailUrl
                                }
                                if (!artUri.isNullOrBlank()) {
                                    AsyncImage(model = artUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Filled.MusicNote, null, tint = TextMuted)
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                    color = if (alreadyAdded) OnSurfaceVariant else OnSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        song.artist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (song is PlayableSong.Downloaded) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .claySurface(borderRadius = 4.dp, backgroundColor = SurfaceContainerLow)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                "OFFLINE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = ClayPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Sticky "Add X Songs" action bar ──────────────────────────
                if (selectedIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CloudBlueBackground)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .claySurface(borderRadius = 24.dp, backgroundColor = ClayPrimary)
                                .clickable {
                                    val toAdd = selectedIds.mapNotNull { id -> songById[id] }
                                    onAddSongs(toAdd)
                                }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LibraryAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Add ${selectedIds.size} Song${if (selectedIds.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
