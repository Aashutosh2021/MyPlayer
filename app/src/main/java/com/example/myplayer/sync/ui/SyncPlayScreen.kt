package com.example.myplayer.sync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.sync.model.*
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

@Composable
fun SyncPlayScreen(
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: SyncPlayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val currentDuration by viewModel.currentDuration.collectAsStateWithLifecycle()

    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var roomNameInput by remember { mutableStateOf("") }

    // Auto-scan when opening screen in NONE role
    LaunchedEffect(uiState.role) {
        if (uiState.role == SyncRole.NONE) {
            viewModel.startScanning()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (uiState.role == SyncRole.NONE) {
                viewModel.stopScanning()
            }
        }
    }

    // Error Dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = NeonLimePrimary) },
            title = { Text("Sync Play", fontWeight = FontWeight.Bold) },
            text = { Text(uiState.errorMessage ?: "") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearError() },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonLimePrimary)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Create Room Dialog
    if (showCreateRoomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            icon = { Icon(Icons.Filled.CastConnected, contentDescription = null, tint = NeonLimePrimary) },
            title = { Text("Host a Sync Room", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nearby devices on this Wi-Fi network can discover and join your room to play songs synchronously.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        singleLine = true,
                        placeholder = { Text("Room Name (e.g. Living Room)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonLimePrimary,
                            unfocusedBorderColor = CardBorderOlive,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = NeonLimePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createRoom(roomNameInput.trim().ifBlank { null })
                        showCreateRoomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLimePrimary,
                        contentColor = OnNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Room", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateRoomDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CloudBlueBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconButton(onClick = onBack, size = 44.dp) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sync Play",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Text(
                        text = when (uiState.role) {
                            SyncRole.MASTER -> "Host Mode • Source of Truth"
                            SyncRole.SLAVE -> "Follower Mode • Synced Playback"
                            SyncRole.NONE -> "Multi-Device Audio Sync"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                // Wi-Fi indicator badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (uiState.isWifiConnected) SurfaceContainer else Color(0xFF3B1818))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (uiState.isWifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = null,
                            tint = if (uiState.isWifiConnected) NeonLimePrimary else Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (uiState.isWifiConnected) "Wi-Fi LAN" else "No Wi-Fi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isWifiConnected) OnSurface else Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            // Body content according to role
            when (uiState.role) {
                SyncRole.NONE -> {
                    NoneRoleContent(
                        uiState = uiState,
                        onHostRoomClick = {
                            roomNameInput = ""
                            showCreateRoomDialog = true
                        },
                        onScanClick = { viewModel.startScanning() },
                        onJoinSession = { session -> viewModel.joinRoom(session) }
                    )
                }

                SyncRole.MASTER -> {
                    MasterRoleContent(
                        uiState = uiState,
                        currentPosition = currentPosition,
                        currentDuration = currentDuration,
                        onPlayPause = { viewModel.onMasterPlayPause() },
                        onSeek = { viewModel.onMasterSeek(it) },
                        onNext = { viewModel.onMasterNext() },
                        onPrev = { viewModel.onMasterPrevious() },
                        onLeave = { viewModel.leaveRoom() },
                        onOpenNowPlaying = onNavigateToNowPlaying
                    )
                }

                SyncRole.SLAVE -> {
                    SlaveRoleContent(
                        uiState = uiState,
                        onLeave = { viewModel.leaveRoom() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoneRoleContent(
    uiState: SyncUiState,
    onHostRoomClick: () -> Unit,
    onScanClick: () -> Unit,
    onJoinSession: (DiscoveredSession) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero info card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLight)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.GroupWork, contentDescription = null, tint = NeonLimePrimary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Synchronized Sound", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                            Text("Play together on the same Wi-Fi", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Sync Play connects 2–4 nearby phones over local Wi-Fi. The Master phone controls playback (play, pause, seek, tracks) while Slaves play the matching local songs in exact lockstep.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onHostRoomClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonLimePrimary,
                                contentColor = OnNeonLime
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Host Room", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onScanClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
                            border = BorderStroke(1.dp, CardBorderOlive),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan Rooms")
                        }
                    }
                }
            }
        }

        // Discovered rooms header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Nearby Sync Rooms (${uiState.discoveredSessions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonLimePrimary
                )
                if (uiState.connectionStatus == ConnectionStatus.DISCOVERING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = NeonLimePrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Searching...", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        if (uiState.discoveredSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLight.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Sensors, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No active rooms found", style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Start a room on another phone connected to this Wi-Fi network to begin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.discoveredSessions) { session ->
                DiscoveredSessionCard(session = session, onJoin = { onJoinSession(session) })
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun DiscoveredSessionCard(
    session: DiscoveredSession,
    onJoin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLight)
            .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(18.dp))
            .clickable { onJoin() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.SpeakerGroup, contentDescription = null, tint = NeonLimePrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.sessionName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = OnSurface)
            Text("Host: ${session.masterName} • ${session.sessionId}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onJoin,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonLimePrimary,
                contentColor = OnNeonLime
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MasterRoleContent(
    uiState: SyncUiState,
    currentPosition: Long,
    currentDuration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLeave: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master status badge card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLight)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👑", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("MASTER ROOM", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NeonLimePrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(uiState.session?.sessionId ?: "HOST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLimePrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.session?.sessionName ?: "MyPlayer Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = "Any song you play, pause, or seek on this phone will broadcast in lockstep to connected devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // Active Playback Controller Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainer)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenNowPlaying() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = NeonLimePrimary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.currentTrack?.title ?: "No track playing",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uiState.currentTrack?.artist?.ifBlank { "Unknown Artist" } ?: "Select a song in Library or Home",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    if (currentDuration > 0) {
                        Spacer(Modifier.height(10.dp))
                        Slider(
                            value = (currentPosition.toFloat() / currentDuration.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                onSeek((frac * currentDuration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NeonLimePrimary,
                                activeTrackColor = NeonLimePrimary,
                                inactiveTrackColor = CardBorderOlive
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrev) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = OnSurface)
                        }
                        Spacer(Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(NeonLimePrimary)
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = OnNeonLime,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = onNext) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = OnSurface)
                        }
                    }
                }
            }
        }

        // Connected devices section
        item {
            Text(
                "Connected Devices (${uiState.connectedDevices.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = NeonLimePrimary
            )
        }

        items(uiState.connectedDevices) { device ->
            DeviceRosterItem(device = device, isMasterSelf = device.role == "MASTER")
        }

        // Leave / End button
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLeave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                border = BorderStroke(1.dp, Color(0xFF552222)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("End Sync Room", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SlaveRoleContent(
    uiState: SyncUiState,
    onLeave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Slave connected card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLight)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = NeonLimePrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("SYNCED TO MASTER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NeonLimePrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(uiState.session?.sessionId ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLimePrimary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = uiState.session?.sessionName ?: "Connected Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = "Master: ${uiState.session?.masterDeviceName ?: "Master"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RTT: ${uiState.latencyMs} ms", fontSize = 12.sp, color = OnSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Sync, contentDescription = null, tint = NeonLimePrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Drift: ${uiState.driftMs} ms", fontSize = 12.sp, color = NeonLimePrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Current Song Status on Slave
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainer)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("NOW RECEIVING", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.currentTrack?.title ?: "Waiting for Master to select a song...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = uiState.currentTrack?.artist?.ifBlank { "Unknown Artist" } ?: "Playback will start automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))
                    if (!uiState.isTrackAvailable) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B1818))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = uiState.unavailableReason ?: "Track not in local library. Master will play, sync will resume on next song.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFB4B4)
                                )
                            }
                        }
                    } else if (uiState.currentTrack != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryContainer)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NeonLimePrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (uiState.isPlaying) "Playing in sync with Master" else "Ready — paused by Master",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Room devices
        item {
            Text(
                "Devices in Room (${uiState.connectedDevices.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = NeonLimePrimary
            )
        }

        items(uiState.connectedDevices) { device ->
            DeviceRosterItem(device = device, isMasterSelf = false)
        }

        // Leave button
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLeave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                border = BorderStroke(1.dp, Color(0xFF552222)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Leave Sync Room", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DeviceRosterItem(device: SyncDevice, isMasterSelf: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLight)
            .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (device.role == "MASTER") PrimaryContainer else SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (device.role == "MASTER") Icons.Filled.Stars else Icons.Filled.Smartphone,
                contentDescription = null,
                tint = if (device.role == "MASTER") NeonLimePrimary else OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                if (isMasterSelf) {
                    Spacer(Modifier.width(6.dp))
                    Text("(You)", fontSize = 11.sp, color = TextMuted)
                }
            }
            Text(
                text = if (device.role == "MASTER") "Master Host" else (device.currentTrackTitle?.let { "Track: $it" } ?: "Connected Slave"),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        device.role == "MASTER" -> PrimaryContainer
                        !device.isTrackAvailable -> Color(0xFF3B1818)
                        device.isReady -> Color(0xFF1B2D17)
                        else -> SurfaceContainerHigh
                    }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = when {
                    device.role == "MASTER" -> "MASTER"
                    !device.isTrackAvailable -> "NO SONG"
                    device.isReady -> "SYNCED ✓"
                    else -> "WAITING"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    device.role == "MASTER" -> NeonLimePrimary
                    !device.isTrackAvailable -> Color(0xFFFF6B6B)
                    device.isReady -> NeonLimePrimary
                    else -> OnSurfaceVariant
                }
            )
        }
    }
}
