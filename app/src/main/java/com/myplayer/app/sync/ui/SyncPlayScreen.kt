package com.myplayer.app.sync.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myplayer.app.data.local.entity.SongEntity
import com.myplayer.app.sync.*
import com.myplayer.app.ui.common.AlbumArtImage
import com.myplayer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPlayScreen(
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: SyncPlayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ltState by viewModel.ltState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    // 0 = Online Room Code (Listen Together), 1 = Local Wi-Fi Sync Play
    var selectedTab by rememberSaveable {
        mutableIntStateOf(if (state.role != SyncRole.NONE) 1 else 0)
    }

    // Automatically switch tab if a session is active in one of the modes
    LaunchedEffect(state.role, ltState.role) {
        if (ltState.role != LtRole.NONE) selectedTab = 0
        else if (state.role != SyncRole.NONE) selectedTab = 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Groups else Icons.Filled.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "Listen Together" else "Local Wi-Fi Sync",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Mode Switcher Pill (Online Room Code vs Local Wi-Fi Sync) ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceContainer)
                    .border(BorderStroke(1.dp, CardBorderOlive), RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModeTabButton(
                    title = "Listen Together (Code)",
                    icon = Icons.Filled.Public,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                ModeTabButton(
                    title = "Local Wi-Fi Sync",
                    icon = Icons.Filled.Wifi,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            if (selectedTab == 0) {
                OnlineListenTogetherSection(
                    ltState = ltState,
                    currentSong = currentSong,
                    viewModel = viewModel,
                    onNavigateToNowPlaying = onNavigateToNowPlaying
                )
            } else {
                LocalWifiSyncSection(
                    state = state,
                    currentSong = currentSong,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NeonLimePrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) OnNeonLime else OnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) OnNeonLime else OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// TAB 1: ONLINE LISTEN TOGETHER (MELD ROOM CODE OVER INTERNET)
// ============================================================================

@Composable
private fun OnlineListenTogetherSection(
    ltState: LtRoomUiState,
    currentSong: SongEntity?,
    viewModel: SyncPlayViewModel,
    onNavigateToNowPlaying: () -> Unit
) {
    val context = LocalContext.current
    var usernameInput by rememberSaveable(ltState.username) { mutableStateOf(ltState.username) }
    var roomCodeInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ltState.errorMessage?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (ltState.role == LtRole.NONE) {
            // Create Online Room Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorderOlive),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NeonLimePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Headphones, null, tint = OnNeonLime, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Listen Together Anywhere",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Not on the same Wi-Fi? Create a 6-digit Room Code and listen in real-time with friends over Mobile Data / Internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Your Display Name") },
                        placeholder = { Text("e.g. Aashutosh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.createOnlineRoom(usernameInput) },
                        enabled = ltState.connectionState != LtConnectionState.CONNECTING,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLimePrimary,
                            contentColor = OnNeonLime
                        )
                    ) {
                        if (ltState.connectionState == LtConnectionState.CONNECTING && !ltState.waitingForApproval) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnNeonLime, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Icon(Icons.Filled.AddCircleOutline, null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Create Room Code (Host)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Join Online Room by Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorderOlive),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Join with Room Code",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { roomCodeInput = it.uppercase().take(8) },
                        label = { Text("Enter 6-Digit Room Code") },
                        placeholder = { Text("e.g. AB12CD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.joinOnlineRoom(roomCodeInput, usernameInput) },
                        enabled = roomCodeInput.isNotBlank() && !ltState.waitingForApproval,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface
                        )
                    ) {
                        if (ltState.waitingForApproval) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonLimePrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Waiting for Host Approval...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Filled.Login, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Join Room", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val isHost = ltState.role == LtRole.HOST

            // Active Room Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, NeonLimePrimary),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHost) "HOSTING ONLINE ROOM" else "CONNECTED AS GUEST",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonLimePrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = ltState.roomCode,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        ),
                        color = OnSurface
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Room Code", ltState.roomCode))
                                Toast.makeText(context, "Room code ${ltState.roomCode} copied!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Code")
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join my Listen Together room on MyPlayer! Room Code: ${ltState.roomCode}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Room Code"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonLimePrimary,
                                contentColor = OnNeonLime
                            )
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isHost) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-approve join requests", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                            Switch(
                                checked = ltState.autoApproveJoins,
                                onCheckedChange = { viewModel.setAutoApprove(it) }
                            )
                        }
                    }
                }
            }

            // Currently Synced Track Card
            val activeTitle = ltState.currentTrack?.title ?: currentSong?.title
            val activeArtist = ltState.currentTrack?.artist ?: currentSong?.artist
            val activeArt = ltState.currentTrack?.thumbnail ?: currentSong?.albumArt

            if (!activeTitle.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, CardBorderOlive),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNowPlaying() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(
                            uri = activeArt,
                            title = activeTitle,
                            artist = activeArtist,
                            size = 50.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Synced Track", style = MaterialTheme.typography.labelSmall, color = NeonLimePrimary)
                            Text(activeTitle, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(activeArtist ?: "", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 1)
                        }
                        if (!isHost && currentSong != null) {
                            OutlinedButton(
                                onClick = { viewModel.suggestCurrentSong() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Suggest", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Pending Join Requests (Host Only)
            if (isHost && ltState.pendingJoinRequests.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pending Join Requests", fontWeight = FontWeight.Bold, color = OnSurface)
                        ltState.pendingJoinRequests.forEach { req ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(req.username, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                Row {
                                    IconButton(onClick = { viewModel.approveJoin(req.userId) }) {
                                        Icon(Icons.Filled.CheckCircle, "Approve", tint = NeonLimePrimary)
                                    }
                                    IconButton(onClick = { viewModel.rejectJoin(req.userId) }) {
                                        Icon(Icons.Filled.Cancel, "Reject", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending Song Suggestions (Host Only)
            if (isHost && ltState.pendingSuggestions.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Suggested Songs", fontWeight = FontWeight.Bold, color = OnSurface)
                        ltState.pendingSuggestions.forEach { sug ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sug.track.title, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1)
                                    Text("by ${sug.fromUsername}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.approveSuggestion(sug) }) {
                                        Icon(Icons.Filled.PlayCircle, "Play", tint = NeonLimePrimary)
                                    }
                                    IconButton(onClick = { viewModel.rejectSuggestion(sug.suggestionId) }) {
                                        Icon(Icons.Filled.Cancel, "Reject", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Connected Users List
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, CardBorderOlive),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Listeners in Room (${ltState.users.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    ltState.users.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (user.isHost) Icons.Filled.Star else Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (user.isHost) NeonLimePrimary else OnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${user.username}${if (user.userId == ltState.userId) " (You)" else ""}",
                                    fontWeight = if (user.isHost) FontWeight.Bold else FontWeight.Medium,
                                    color = OnSurface
                                )
                            }
                            if (isHost && user.userId != ltState.userId) {
                                Row {
                                    TextButton(onClick = { viewModel.transferHost(user.userId) }) {
                                        Text("Make Host", fontSize = 12.sp, color = NeonLimePrimary)
                                    }
                                    IconButton(onClick = { viewModel.kickUser(user.userId) }) {
                                        Icon(Icons.Filled.PersonRemove, "Kick", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.leaveOnlineRoom() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = PureWhite
                )
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Leave Online Room", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// TAB 2: LOCAL WI-FI / HOTSPOT SYNC PLAY (ORIGINAL DIRECT LAN SYNC)
// ============================================================================

@Composable
private fun LocalWifiSyncSection(
    state: SyncUiState,
    currentSong: SongEntity?,
    viewModel: SyncPlayViewModel
) {
    var showJoinByIpDialog by remember { mutableStateOf(false) }
    var inputIp by remember { mutableStateOf("") }
    var inputPort by remember { mutableStateOf("45200") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        when (state.role) {
            SyncRole.NONE -> {
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
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Local Wi-Fi / Hotspot Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Play music simultaneously across devices connected to the same Wi-Fi network or Mobile Hotspot.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (currentSong != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtImage(
                                uri = currentSong.albumArt,
                                title = currentSong.title,
                                artist = currentSong.artist,
                                album = currentSong.album,
                                size = 42.dp,
                                shape = RoundedCornerShape(8.dp),
                                iconSize = 20.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentSong.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(currentSong.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                Button(
                    onClick = { viewModel.createRoom(Build.MODEL) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLimePrimary, contentColor = OnNeonLime)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("CREATE WI-FI ROOM (HOST)", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.joinNearbyRoom() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("JOIN NEARBY ROOM (AUTO)", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showJoinByIpDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HOST MODE (MASTER)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Room: MyPlayer-${Build.MODEL}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))

                        state.localIpAddress?.let { ip ->
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Host IP: $ip", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Port: ${state.boundPort}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        StatusBadge(connectionState = state.connectionState)
                        Spacer(Modifier.height(12.dp))

                        if (state.connectionState == SyncConnectionState.CONNECTED) {
                            Text("Connected Devices (${state.devices.size})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            for (device in state.devices) {
                                DeviceItemCard(
                                    device = device,
                                    onRemove = { viewModel.removeSlave(device.deviceId) }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        } else {
                            Text("Waiting for nearby devices to join...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (state.playbackState == SyncPlaybackState.PLAYING) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("PAUSE SYNC PLAY", fontWeight = FontWeight.Bold)
                    }
                } else if (state.playbackState == SyncPlaybackState.PAUSED) {
                    Button(
                        onClick = { viewModel.resume() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("RESUME SYNC PLAY", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.startSyncPlayback() },
                        enabled = state.connectionState == SyncConnectionState.CONNECTED,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("START SYNC PLAY", fontWeight = FontWeight.Bold)
                    }
                }
            }

            SyncRole.SLAVE -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CLIENT MODE (SLAVE)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        StatusBadge(connectionState = state.connectionState)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (state.connectionState == SyncConnectionState.CONNECTED) "Connected to Host (${state.roomHostAddress ?: ""})" else "Searching / Connecting...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (state.role != SyncRole.NONE) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.leaveRoom() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("LEAVE WI-FI ROOM", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showJoinByIpDialog) {
        AlertDialog(
            onDismissRequest = { showJoinByIpDialog = false },
            title = { Text("Join Room by IP") },
            text = {
                Column {
                    Text("Enter the IP address of the Host device:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it },
                        label = { Text("Host IP Address") },
                        placeholder = { Text("e.g. 192.168.1.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputPort,
                        onValueChange = { inputPort = it },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.joinByIp(inputIp, inputPort.toIntOrNull() ?: 45200)
                        showJoinByIpDialog = false
                    },
                    enabled = inputIp.isNotBlank()
                ) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinByIpDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatusBadge(connectionState: SyncConnectionState) {
    val (color, text) = when (connectionState) {
        SyncConnectionState.IDLE -> MaterialTheme.colorScheme.outline to "Idle"
        SyncConnectionState.ADVERTISING -> MaterialTheme.colorScheme.primary to "Broadcasting room..."
        SyncConnectionState.DISCOVERING -> MaterialTheme.colorScheme.primary to "Searching nearby..."
        SyncConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary to "Connecting..."
        SyncConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary to "Connected"
        SyncConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline to "Disconnected"
        SyncConnectionState.ERROR -> MaterialTheme.colorScheme.error to "Connection error"
    }

    Surface(color = color.copy(alpha = 0.15f), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(device.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(device.status.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                if (onRemove != null && !device.isMaster) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
