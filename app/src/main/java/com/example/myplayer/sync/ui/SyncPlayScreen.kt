package com.example.myplayer.sync.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myplayer.sync.DeviceSyncStatus
import com.example.myplayer.sync.SyncConnectionState
import com.example.myplayer.sync.SyncDevice
import com.example.myplayer.sync.SyncPlaybackState
import com.example.myplayer.sync.SyncRole
import com.example.myplayer.ui.common.AlbumArtImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPlayScreen(
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: SyncPlayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    var showJoinByIpDialog by remember { mutableStateOf(false) }
    var inputIp by remember { mutableStateOf("") }
    var inputPort by remember { mutableStateOf("45200") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Play", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error banner if any
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            when (state.role) {
                SyncRole.NONE -> {
                    // Header card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Synchronized Listening",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Play music simultaneously across 2 devices on the same Wi-Fi network.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (state.isEmulator) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Emulator detected. For local playback with physical devices, we recommend hosting on the physical phone/tablet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Currently playing song indicator
                    if (currentSong != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtImage(
                                    uri = currentSong?.albumArt,
                                    title = currentSong?.title,
                                    artist = currentSong?.artist,
                                    album = currentSong?.album,
                                    size = 42.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    iconSize = 20.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        currentSong?.title ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        currentSong?.artist ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    } else {
                        Text(
                            "Tip: Start playing a song first if you plan to Host the room.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // Action buttons
                    Button(
                        onClick = { viewModel.createRoom(Build.MODEL) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("CREATE ROOM (HOST)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.joinNearbyRoom() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("JOIN NEARBY ROOM (AUTO)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showJoinByIpDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.SettingsEthernet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("JOIN BY IP ADDRESS", fontWeight = FontWeight.Medium)
                    }
                }

                SyncRole.MASTER -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "HOST MODE (MASTER)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Room: MyPlayer-${Build.MODEL}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))

                            // Show local IP & port so slave can connect directly
                            state.localIpAddress?.let { ip ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Host IP: $ip",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Port: ${state.boundPort}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            if (state.isEmulator) {
                                Text(
                                    "Note: Running in Android Emulator (10.0.2.x). To connect a physical device, please run Master on the physical device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            Spacer(Modifier.height(4.dp))
                            StatusBadge(connectionState = state.connectionState)
                            Spacer(Modifier.height(14.dp))

                            if (state.connectionState == SyncConnectionState.ADVERTISING) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Waiting for other device to join...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Connected Devices: 0",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (state.connectionState == SyncConnectionState.CONNECTED) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Connected Devices (${state.devices.size})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                for (device in state.devices) {
                                    DeviceItemCard(
                                        device = device,
                                        onRemove = { viewModel.removeSlave(device.deviceId) }
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                            } else if (state.connectionState == SyncConnectionState.ERROR) {
                                state.errorMessage?.let { msg ->
                                    Text(
                                        msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    val masterTrackTitle = currentSong?.title ?: state.currentTrack?.title
                    val masterTrackArtist = currentSong?.artist ?: state.currentTrack?.artist
                    val masterTrackArt = currentSong?.albumArt ?: state.currentTrack?.albumArt

                    if (!masterTrackTitle.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtImage(
                                    uri = masterTrackArt,
                                    title = masterTrackTitle,
                                    artist = masterTrackArtist,
                                    size = 46.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    iconSize = 22.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        masterTrackTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        masterTrackArtist ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    val buttonText = when (state.playbackState) {
                        SyncPlaybackState.WAITING_FOR_READY,
                        SyncPlaybackState.PREPARING -> "PREPARING TRACK..."
                        SyncPlaybackState.SCHEDULED -> "STARTING IN SYNC..."
                        SyncPlaybackState.PLAYING -> "PLAYING IN SYNC"
                        SyncPlaybackState.PAUSED -> "RESUME SYNC PLAY"
                        else -> if (state.connectionState == SyncConnectionState.CONNECTED) "START SYNC PLAY" else "WAITING FOR DEVICE TO JOIN..."
                    }

                    if (state.playbackState == SyncPlaybackState.PLAYING) {
                        Button(
                            onClick = { viewModel.pause() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("PAUSE SYNC PLAY", fontWeight = FontWeight.Bold)
                        }
                    } else if (state.playbackState == SyncPlaybackState.PAUSED) {
                        Button(
                            onClick = { viewModel.resume() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESUME SYNC PLAY", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startSyncPlayback() },
                            enabled = state.connectionState == SyncConnectionState.CONNECTED &&
                                    state.playbackState != SyncPlaybackState.WAITING_FOR_READY &&
                                    state.playbackState != SyncPlaybackState.PREPARING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (state.playbackState == SyncPlaybackState.WAITING_FOR_READY ||
                                state.playbackState == SyncPlaybackState.PREPARING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(buttonText, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                SyncRole.SLAVE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "CLIENT MODE (SLAVE)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            StatusBadge(connectionState = state.connectionState)
                            Spacer(Modifier.height(14.dp))

                            when (state.connectionState) {
                                SyncConnectionState.CONNECTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "Connecting to ${state.roomHostAddress ?: "Host"}...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Ensure Host room is open on the Master phone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SyncConnectionState.CONNECTED -> {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Connected to Host: ${state.roomHostAddress ?: ""}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    when (state.playbackState) {
                                        SyncPlaybackState.PREPARING -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Preparing track: ${state.currentTrack?.title ?: "..."}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        SyncPlaybackState.WAITING_FOR_READY -> {
                                            Text(
                                                "Track ready! Waiting for Host to schedule start...",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        SyncPlaybackState.SCHEDULED -> {
                                            Text(
                                                "Starting synchronized playback...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        SyncPlaybackState.PLAYING -> {
                                            Text(
                                                "Playing in sync with Host!",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        SyncPlaybackState.PAUSED -> {
                                            Text(
                                                "Playback paused by Host",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        else -> {
                                            Text(
                                                "Ready! Waiting for Host to start music...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    val slaveTrackTitle = state.currentTrack?.title ?: currentSong?.title
                                    val slaveTrackArtist = state.currentTrack?.artist ?: currentSong?.artist
                                    val slaveTrackArt = state.currentTrack?.albumArt ?: currentSong?.albumArt

                                    if (!slaveTrackTitle.isNullOrBlank()) {
                                        Spacer(Modifier.height(12.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AlbumArtImage(
                                                    uri = slaveTrackArt,
                                                    title = slaveTrackTitle,
                                                    artist = slaveTrackArtist,
                                                    size = 46.dp,
                                                    shape = RoundedCornerShape(8.dp),
                                                    iconSize = 22.dp
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        slaveTrackTitle,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        slaveTrackArtist ?: "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                SyncConnectionState.DISCOVERING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "Searching for nearby rooms...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                SyncConnectionState.ERROR -> {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "Connection Failed",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            state.errorMessage?.let { msg ->
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    msg,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                SyncConnectionState.DISCONNECTED -> {
                                    Text(
                                        "Disconnected from Host",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            if (state.role != SyncRole.NONE) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { viewModel.leaveRoom() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LEAVE ROOM", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Join by IP Dialog
    if (showJoinByIpDialog) {
        AlertDialog(
            onDismissRequest = { showJoinByIpDialog = false },
            title = { Text("Join Room by IP") },
            text = {
                Column {
                    Text(
                        "Enter the IP address of the Host (Master) device displayed on its Sync Play screen:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it },
                        label = { Text("Host IP Address") },
                        placeholder = { Text("e.g. 172.16.225.57") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputPort,
                        onValueChange = { inputPort = it },
                        label = { Text("Port") },
                        placeholder = { Text("45200") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val port = inputPort.toIntOrNull() ?: 45200
                        viewModel.joinByIp(inputIp, port)
                        showJoinByIpDialog = false
                    },
                    enabled = inputIp.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinByIpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(connectionState: SyncConnectionState) {
    val (color, text) = when (connectionState) {
        SyncConnectionState.IDLE -> MaterialTheme.colorScheme.outline to "Idle"
        SyncConnectionState.ADVERTISING -> MaterialTheme.colorScheme.primary to "Broadcasting room..."
        SyncConnectionState.DISCOVERING -> MaterialTheme.colorScheme.primary to "Searching for nearby room..."
        SyncConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary to "Connecting to room..."
        SyncConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary to "Connected"
        SyncConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline to "Disconnected"
        SyncConnectionState.ERROR -> MaterialTheme.colorScheme.error to "Connection error"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DeviceItemCard(
    device: SyncDevice,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    when (device.status) {
                        DeviceSyncStatus.IDLE -> {
                            Text(
                                "Connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DeviceSyncStatus.PREPARING -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Preparing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DeviceSyncStatus.DOWNLOADING -> {
                            Text(
                                "Downloading ${device.downloadProgress}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DeviceSyncStatus.READY -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DeviceSyncStatus.FAILED -> {
                            Text(
                                device.errorMessage ?: "Failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        DeviceSyncStatus.DISCONNECTED -> {
                            Text(
                                "Disconnected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (onRemove != null && !device.isMaster) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove device",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (device.status == DeviceSyncStatus.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (device.downloadProgress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
