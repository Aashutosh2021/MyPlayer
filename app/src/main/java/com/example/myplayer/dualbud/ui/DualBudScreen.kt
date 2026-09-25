package com.example.myplayer.dualbud.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.dualbud.model.DualBudModeState
import com.example.myplayer.dualbud.model.DualChannelId
import com.example.myplayer.dualbud.model.DualChannelState
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualBudScreen(
    onBack: () -> Unit,
    viewModel: DualBudViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val loadingChannel by viewModel.loadingChannel.collectAsStateWithLifecycle()
    val resolvingSongId by viewModel.resolvingSongId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var pickingChannel by remember { mutableStateOf<DualChannelId?>(null) }
    var selectedPickerTab by remember { mutableIntStateOf(0) } // 0: Online Search, 1: Downloaded

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Dual Bud Mode",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                        Text(
                            "Split-Channel Stereo Playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                },
                actions = {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isEnabled) ClayPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (uiState.isEnabled) "ACTIVE" else "OFF",
                            color = if (uiState.isEnabled) ClayPrimary else OnSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CloudBlueBackground)
            )
        },
        containerColor = CloudBlueBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Channel Mapping Summary Header
            item {
                DualBudStatusBanner(
                    state = uiState,
                    onSwap = { viewModel.swapChannels() }
                )
            }

            // Left Earbud Panel
            item {
                ChannelControlCard(
                    channelId = DualChannelId.LEFT,
                    channelLabel = "LEFT EARBUD",
                    channelState = uiState.leftChannel,
                    isPhysicallyRouted = !uiState.isSwapped,
                    isChannelLoading = loadingChannel == DualChannelId.LEFT,
                    onPickSong = {
                        pickingChannel = DualChannelId.LEFT
                        selectedPickerTab = 0
                    },
                    onPlayPause = { viewModel.togglePlayPause(DualChannelId.LEFT) },
                    onSeek = { viewModel.seekTo(DualChannelId.LEFT, it) },
                    onVolumeChange = { viewModel.setVolume(DualChannelId.LEFT, it) }
                )
            }

            // Swap Quick Button
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.swapChannels() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SurfaceLight,
                            contentColor = ClayPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Swap Left ↔ Right",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Right Earbud Panel
            item {
                ChannelControlCard(
                    channelId = DualChannelId.RIGHT,
                    channelLabel = "RIGHT EARBUD",
                    channelState = uiState.rightChannel,
                    isPhysicallyRouted = uiState.isSwapped,
                    isChannelLoading = loadingChannel == DualChannelId.RIGHT,
                    onPickSong = {
                        pickingChannel = DualChannelId.RIGHT
                        selectedPickerTab = 0
                    },
                    onPlayPause = { viewModel.togglePlayPause(DualChannelId.RIGHT) },
                    onSeek = { viewModel.seekTo(DualChannelId.RIGHT, it) },
                    onVolumeChange = { viewModel.setVolume(DualChannelId.RIGHT, it) }
                )
            }

            // Master Bottom Controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.toggleDualBudMode()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isEnabled) Color(0xFFD32F2F) else ClayPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isEnabled) "Exit Dual Bud Mode" else "Enable Dual Bud Mode",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Outputs standard stereo PCM. Player A plays to Left ear, Player B plays to Right ear simultaneously.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Online & Downloaded Song Selection Bottom Sheet
    pickingChannel?.let { targetChannel ->
        ModalBottomSheet(
            onDismissRequest = {
                pickingChannel = null
                viewModel.clearSearch()
            },
            containerColor = SurfaceLight,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Song",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                        Text(
                            text = "For ${if (targetChannel == DualChannelId.LEFT) "Left" else "Right"} Earbud",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ClayPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (targetChannel == DualChannelId.LEFT) "LEFT BUD" else "RIGHT BUD",
                            color = ClayPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            if (selectedPickerTab == 0) "Search online songs (YouTube)..." else "Filter downloaded songs...",
                            color = OnSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = ClayPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = OnSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClayPrimary,
                        unfocusedBorderColor = SurfaceContainerHigh,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = ClayPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Tabs: Online Search vs Downloaded Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "Search Online" to Icons.Filled.Cloud,
                        "Downloaded (${downloadedSongs.size})" to Icons.Filled.DownloadDone
                    )
                    tabs.forEachIndexed { index, (label, icon) ->
                        val active = selectedPickerTab == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) SurfaceLight else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPickerTab = index }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (active) ClayPrimary else OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (active) ClayPrimary else OnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Tab Content
                if (selectedPickerTab == 0) {
                    // Online Search Tab
                    when {
                        isSearching -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = ClayPrimary,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text("Searching online...", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        searchQuery.trim().length < 2 -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Search, null, tint = ClayPrimary.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Search Online Music", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Type artist, song, or title to stream directly", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                        }
                        searchResults.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No online songs found for '$searchQuery'", color = OnSurfaceVariant)
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchResults, key = { it.videoId }) { song ->
                                    OnlineSongPickerRow(
                                        song = song,
                                        isResolving = resolvingSongId == song.videoId,
                                        onClick = {
                                            viewModel.selectOnlineSong(targetChannel, song) {
                                                pickingChannel = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Downloaded Tab (Only music saved from downloaded section)
                    val filteredDownloads = remember(downloadedSongs, searchQuery) {
                        if (searchQuery.isBlank()) downloadedSongs
                        else downloadedSongs.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredDownloads.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Download, null, tint = OnSurfaceVariant, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (downloadedSongs.isEmpty()) "No Downloaded Music" else "No matching downloads",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Save songs from online search to play offline here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredDownloads, key = { it.id }) { downloaded ->
                                DownloadedSongPickerRow(
                                    song = downloaded,
                                    onClick = {
                                        viewModel.selectDownloadedSong(targetChannel, downloaded) {
                                            pickingChannel = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualBudStatusBanner(
    state: DualBudModeState,
    onSwap: () -> Unit
) {
    val leftSongTitle = state.leftChannel.song?.title ?: "Empty"
    val rightSongTitle = state.rightChannel.song?.title ?: "Empty"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Active Earbud Output",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClayPrimary
            )
            if (state.isSwapped) {
                Text(
                    "SWAPPED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFB74D)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Left = $leftSongTitle",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Right = $rightSongTitle",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChannelControlCard(
    channelId: DualChannelId,
    channelLabel: String,
    channelState: DualChannelState,
    isPhysicallyRouted: Boolean,
    isChannelLoading: Boolean,
    onPickSong: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val song = channelState.song
    val accentColor = ClayPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 24.dp, backgroundColor = SurfaceLight)
            .padding(16.dp)
            .animateContentSize()
    ) {
        // Channel Tag Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = channelLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }

            TextButton(onClick = onPickSong) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    if (song == null) "Select Song" else "Change",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Song Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(
                uri = song?.albumArt ?: song?.path,
                title = song?.title,
                artist = song?.artist,
                album = song?.album,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "Tap 'Select Song' to choose",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song?.artist ?: "Online stream or downloaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                channelState.errorMessage?.let { errorMsg ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF5350)
                    )
                }
            }

            // Transport: Play/Pause Button
            IconButton(
                onClick = onPlayPause,
                enabled = song != null && !isChannelLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clayConcave(borderRadius = 24.dp, backgroundColor = SurfaceContainerLow)
            ) {
                if (channelState.isLoading || isChannelLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = accentColor
                    )
                } else {
                    Icon(
                        if (channelState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (channelState.isPlaying) "Pause" else "Play",
                        tint = if (song != null) accentColor else OnSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Seekbar & Timing
        if (song != null) {
            val duration = if (channelState.durationMs > 0) channelState.durationMs else (song.duration)
            val currentPos = channelState.positionMs.coerceIn(0L, maxOf(1L, duration))
            val progress = (currentPos.toFloat() / maxOf(1L, duration).toFloat()).coerceIn(0f, 1f)

            Slider(
                value = progress,
                onValueChange = { frac ->
                    val targetMs = (frac * duration).toLong()
                    onSeek(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = SurfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
        }

        // Volume Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (channelState.volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Volume",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = channelState.volume,
                onValueChange = onVolumeChange,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = SurfaceContainerLow
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${(channelState.volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun OnlineSongPickerRow(
    song: OnlineSong,
    isResolving: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .clickable(enabled = !isResolving, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            val art = song.thumbnailUrl.ifBlank {
                "https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg"
            }
            AsyncImage(
                model = art,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
        if (isResolving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = ClayPrimary
            )
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(song.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ClayPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ONLINE",
                        color = ClayPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedSongPickerRow(
    song: DownloadedSongEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonLimePrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "SAVED",
                    color = NeonLimePrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
