package com.example.myplayer.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.data.update.UpdateChecker
import com.example.myplayer.ui.components.WhatsNewDialog
import com.example.myplayer.ui.mochy.*
import com.example.myplayer.ui.theme.ThemeState

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToDualBud: () -> Unit = {},
    onNavigateToSyncPlay: () -> Unit = {},
    onNavigateToAudioPlayback: (() -> Unit)? = null,
    mochyState: MochyUiStateHolder? = null,
    onNavigateToPlayerStyles: () -> Unit = {},
    onNavigateToMochyAppearance: () -> Unit = {},
    onNavigateToMochyAudioLab: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsStateWithLifecycle()
    val downloadFolderUri by viewModel.downloadFolderUri.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showDialogText by remember { mutableStateOf<String?>(null) }
    var isLoadingDialog by remember { mutableStateOf(false) }

    val isSpotifyLoggedIn by viewModel.isSpotifyLoggedIn.collectAsStateWithLifecycle()
    val spotifyUsername by viewModel.spotifyUsername.collectAsStateWithLifecycle()
    val isSpotifySyncing by viewModel.isSpotifySyncing.collectAsStateWithLifecycle()
    val spotifySyncMessage by viewModel.spotifySyncMessage.collectAsStateWithLifecycle()
    var showSpotifyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(spotifySyncMessage) {
        spotifySyncMessage?.let {
            showDialogText = it
            viewModel.clearSpotifySyncMessage()
        }
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val audioConfig by viewModel.audioConfig.collectAsStateWithLifecycle()
    var showAudioSettingsDialog by remember { mutableStateOf(false) }
    var showAudioPlaybackScreen by remember { mutableStateOf(false) }

    if (showAudioPlaybackScreen) {
        com.example.myplayer.ui.settings.AudioAndPlaybackScreen(
            store = viewModel.audioPlaybackStore,
            featureManagers = viewModel.audioFeatureManagers,
            onBack = { showAudioPlaybackScreen = false },
            mochyState = mochyState
        )
        return
    }

    if (showAudioSettingsDialog) {
        com.example.myplayer.ui.settings.AudioPlaybackSettingsDialog(
            config = audioConfig,
            onDismiss = { showAudioSettingsDialog = false },
            onWifiQualityChange = { viewModel.updateWifiQuality(it) },
            onMobileQualityChange = { viewModel.updateMobileQuality(it) },
            onClarityChange = { viewModel.updateClarityPreset(it) },
            onDolbyToggle = { enabled, width -> viewModel.updateTrueDolby(enabled, width) },
            onLoudnessToggle = { enabled, gain -> viewModel.updateLoudness(enabled, gain) },
            onSkipSilenceToggle = { viewModel.updateSkipSilence(it) }
        )
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(onDismiss = { showWhatsNewDialog = false })
    }

    if (showSpotifyDialog) {
        SpotifyLoginDialog(
            spotifyManager = viewModel.spotifyManager,
            onDismiss = { showSpotifyDialog = false },
            onLoginSuccess = { user ->
                showDialogText = "Connected to Spotify as $user! Taste profile synced into AI recommendations."
            }
        )
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupRestoreState.Loading -> {
                isLoadingDialog = true
                showDialogText = state.progress ?: "Creating backup file..."
            }
            is BackupRestoreState.Success -> {
                isLoadingDialog = false
                showDialogText = state.message
            }
            is BackupRestoreState.Error -> {
                isLoadingDialog = false
                showDialogText = "Error: ${state.error}"
            }
            BackupRestoreState.Idle -> {}
        }
    }

    LaunchedEffect(restoreState) {
        when (val state = restoreState) {
            is BackupRestoreState.Loading -> {
                isLoadingDialog = true
                showDialogText = state.progress ?: "Restoring database & downloads..."
            }
            is BackupRestoreState.Success -> {
                isLoadingDialog = false
                showDialogText = state.message
            }
            is BackupRestoreState.Error -> {
                isLoadingDialog = false
                showDialogText = "Error: ${state.error}"
            }
            BackupRestoreState.Idle -> {}
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFD1D1D6),
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White) },
            title = { Text("Set Display Name", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Enter your name to show on the Home Screen:", color = Color(0xFF8E8E93))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF3A3A3C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Your Name", color = Color(0xFF636366)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            viewModel.setUserName(editNameInput.trim())
                        }
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNameDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E8E93))
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDialogText != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoadingDialog) {
                    showDialogText = null
                    viewModel.resetStates()
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFD1D1D6),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(if (isLoadingDialog) "Please Wait" else "Backup & Restore", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoadingDialog) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(showDialogText!!, color = Color(0xFFD1D1D6))
                }
            },
            confirmButton = {
                if (!isLoadingDialog) {
                    TextButton(
                        onClick = {
                            showDialogText = null
                            viewModel.resetStates()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    when (val state = updateState) {
        ManualUpdateState.Idle -> {}
        ManualUpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFD1D1D6),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Checking for Updates", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Connecting to GitHub...", color = Color(0xFFD1D1D6))
                    }
                },
                confirmButton = {}
            )
        }
        is ManualUpdateState.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFD1D1D6),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Update Available", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "A newer version (${state.updateInfo.versionName}) is available!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Visit our official website to download the latest update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93)
                        )
                        if (state.updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.updateInfo.releaseNotes.take(300),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetUpdateState()
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(state.updateInfo.releaseUrl)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E8E93))
                    ) {
                        Text("Later")
                    }
                }
            )
        }
        ManualUpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFD1D1D6),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Up to Date", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "You're using the latest version of MyPlayer (v${com.example.myplayer.BuildConfig.VERSION_NAME}).",
                        color = Color(0xFFD1D1D6)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        is ManualUpdateState.NoInternet -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFD1D1D6),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("No Internet Connection", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(state.message, color = Color(0xFFD1D1D6))
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        is ManualUpdateState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFD1D1D6),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Update Check", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(state.message, color = Color(0xFFD1D1D6))
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 32.dp)
    ) {
        // Mochy Style Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp)
                ) {
                    Text(
                        text = "MYPLAYER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                }

                // Floating Status Card (Screenshot Matching)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
                    modifier = Modifier.size(width = 92.dp, height = 74.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${com.example.myplayer.BuildConfig.VERSION_NAME}",
                                color = Color(0xFF8E8E93),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "1 of 1",
                                color = Color(0xFF636366),
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = "MYPLAYER",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                            val barWidth = 3.dp.toPx()
                            val count = 7
                            val gap = (size.width - (count * barWidth)) / (count - 1).toFloat()
                            val heights = listOf(0.4f, 0.75f, 0.5f, 1.0f, 0.65f, 0.85f, 0.45f)
                            heights.forEachIndexed { index, fraction ->
                                val x = index * (barWidth + gap)
                                val barHeight = size.height * fraction
                                val y = size.height - barHeight
                                drawRoundRect(
                                    color = Color(0xFF8E8E93),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Profile
        item { MochySectionHeader(title = "Profile") }
        item {
            MochySettingsCard {
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Person,
                    title = "Display Name",
                    subtitle = userName,
                    onClick = {
                        editNameInput = userName
                        showNameDialog = true
                    }
                )
            }
        }

        // Section: Appearance
        item { MochySectionHeader(title = "Appearance") }
        item {
            MochySettingsCard {
                MochySettingsToggleRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark Mode",
                    subtitle = if (ThemeState.isDarkMode) "Dark theme active" else "Light theme active",
                    checked = ThemeState.isDarkMode,
                    onCheckedChange = { enabled ->
                        ThemeState.setDark(enabled)
                    }
                )
                MochySettingsDivider()
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Palette,
                    title = "Customize Player Styles",
                    subtitle = "Active: ${mochyState?.playerStyle?.title ?: "Radial (MyPlayer)"} • 6 unique styles",
                    onClick = onNavigateToPlayerStyles
                )
            }
        }

        // Section: Playback
        item { MochySectionHeader(title = "Playback & Audio") }
        item {
            MochySettingsCard {
                MochySettingsNavigationRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Audio & Playback Engine",
                    subtitle = "15-Band EQ, Audio Lab FX, 7 Quality Tiers & Bit-Perfect",
                    onClick = {
                        if (onNavigateToAudioPlayback != null) {
                            onNavigateToAudioPlayback()
                        } else {
                            showAudioPlaybackScreen = true
                        }
                    }
                )
                MochySettingsDivider()
                MochySettingsToggleRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Autoplay Recommendations",
                    subtitle = "Automatically play similar songs when the queue ends",
                    checked = isAutoplayEnabled,
                    onCheckedChange = { viewModel.setAutoplayEnabled(it) }
                )
            }
        }

        // Section: Spotify & AI Taste
        item { MochySectionHeader(title = "Spotify & AI Taste") }
        item {
            MochySettingsCard {
                if (!isSpotifyLoggedIn) {
                    MochySettingsNavigationRow(
                        icon = Icons.Filled.MusicNote,
                        title = "Connect Spotify",
                        subtitle = "Import your Spotify top tracks & taste profile",
                        onClick = { showSpotifyDialog = true }
                    )
                } else {
                    MochySettingsInfoRow(
                        icon = Icons.Filled.CheckCircle,
                        title = "Spotify Connected",
                        subtitle = "Logged in as @$spotifyUsername • AI Taste Profile Synced"
                    )
                    MochySettingsDivider()
                    MochySettingsNavigationRow(
                        icon = Icons.Filled.Delete,
                        title = "Disconnect Spotify",
                        subtitle = "Clear Spotify session cookies and tokens",
                        onClick = {
                            viewModel.logoutSpotify()
                            showDialogText = "Disconnected from Spotify."
                        }
                    )
                }
            }
        }

        // Section: Backup & Restore
        item { MochySectionHeader(title = "Backup & Restore") }
        item {
            MochySettingsCard {
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Backup,
                    title = "Create Backup",
                    subtitle = "Export library, playlists, settings + downloaded song files (ZIP)",
                    onClick = { backupLauncher.launch("myplayer_backup.zip") }
                )
                MochySettingsDivider()
                MochySettingsNavigationRow(
                    icon = Icons.Filled.SettingsBackupRestore,
                    title = "Restore Backup",
                    subtitle = "Import playlists and auto-restore downloaded music",
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) }
                )
            }
        }

        // Section: Experimental Features
        item { MochySectionHeader(title = "Experimental Features") }
        item {
            MochySettingsCard {
                MochySettingsNavigationRow(
                    icon = Icons.Filled.GroupWork,
                    title = "Sync Play",
                    subtitle = "Synchronize playback with nearby phones over local Wi-Fi",
                    onClick = onNavigateToSyncPlay
                )
                MochySettingsDivider()
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Headphones,
                    title = "Dual Bud Mode",
                    subtitle = "Play two independent songs simultaneously in left & right earbuds",
                    onClick = onNavigateToDualBud
                )
            }
        }

        // Section: Contact Developer
        item { MochySectionHeader(title = "Contact Developer") }
        item {
            MochySettingsCard {
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Feedback,
                    title = "Complaint & Bug Report",
                    subtitle = "Send complaints, bugs, or feedback to $DEVELOPER_EMAIL",
                    onClick = {
                        sendEmail(
                            context = context,
                            recipient = DEVELOPER_EMAIL,
                            subject = "[MyPlayer Complaint / Bug Report] v${com.example.myplayer.BuildConfig.VERSION_NAME}",
                            body = """
                                Hi Developer,

                                --- Complaint / Bug Details ---
                                [Please describe your complaint, bug, or feedback here]

                                --- Device & System Diagnostics ---
                                App Version: ${com.example.myplayer.BuildConfig.VERSION_NAME} (${com.example.myplayer.BuildConfig.VERSION_CODE})
                                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                                Android Version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                            """.trimIndent()
                        )
                    }
                )
            }
        }

        // Section: About
        item { MochySectionHeader(title = "About") }
        item {
            MochySettingsCard {
                MochySettingsNavigationRow(
                    icon = Icons.Filled.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = "Check official website & GitHub for updates",
                    onClick = { viewModel.checkForUpdates() }
                )
                MochySettingsDivider()
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Language,
                    title = "Official Website",
                    subtitle = UpdateChecker.UPDATE_WEBSITE_URL,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.UPDATE_WEBSITE_URL)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                MochySettingsDivider()
                MochySettingsNavigationRow(
                    icon = Icons.Filled.Info,
                    title = "What's New in v${com.example.myplayer.BuildConfig.VERSION_NAME}",
                    subtitle = "See new features and improvements in this release",
                    onClick = { showWhatsNewDialog = true }
                )
                MochySettingsDivider()
                MochySettingsInfoRow(
                    icon = Icons.Filled.MusicNote,
                    title = "Powered By",
                    subtitle = "FlexFly Company Pvt Ltd"
                )
            }
        }
    }
}

@Composable
private fun MochySectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = Color(0xFF8E8E93),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun MochySettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF16161A),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun MochySettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222228)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF16161A),
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color(0xFF636366),
                uncheckedTrackColor = Color(0xFF2C2C32),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun MochySettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222228)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF636366),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MochySettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222228)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
private fun MochySettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        color = Color(0xFF222228),
        thickness = 0.8.dp
    )
}

private const val DEVELOPER_EMAIL = "flexflycompany.in@gmail.com"

private fun sendEmail(
    context: Context,
    recipient: String,
    subject: String,
    body: String = ""
) {
    try {
        val mailtoUri = Uri.parse("mailto:$recipient?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(emailIntent, "Send Email via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Developer Email", recipient))
            Toast.makeText(
                context,
                "No email app found. Email copied to clipboard: $recipient",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Please contact: $recipient",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
