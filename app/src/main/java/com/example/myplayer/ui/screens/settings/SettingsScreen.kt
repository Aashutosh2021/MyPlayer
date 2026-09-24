package com.example.myplayer.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToDualBud: () -> Unit = {},
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

    var showNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
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
                showDialogText = "Creating backup file..."
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
                showDialogText = "Restoring database & downloads..."
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
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = NeonLimePrimary) },
            title = { Text("Set Display Name", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your name to show on the Home Screen:")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonLimePrimary,
                            unfocusedBorderColor = CardBorderOlive,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = NeonLimePrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Your Name", color = TextMuted) }
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
                        containerColor = NeonLimePrimary,
                        contentColor = OnNeonLime
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNameDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
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
            containerColor = SurfaceLight,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant,
            title = {
                Text(if (isLoadingDialog) "Please Wait" else "Backup & Restore")
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoadingDialog) {
                        CircularProgressIndicator(color = ClayPrimary)
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(showDialogText!!)
                }
            },
            confirmButton = {
                if (!isLoadingDialog) {
                    TextButton(
                        onClick = {
                            showDialogText = null
                            viewModel.resetStates()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                    ) {
                        Text("OK")
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
                containerColor = SurfaceLight,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant,
                title = { Text("Checking for Updates") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = ClayPrimary)
                        Spacer(Modifier.width(16.dp))
                        Text("Connecting to GitHub...")
                    }
                },
                confirmButton = {}
            )
        }
        is ManualUpdateState.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = SurfaceLight,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant,
                title = { Text("Update Available") },
                text = {
                    Column {
                        Text(
                            "A newer version (${state.updateInfo.versionName}) is available!",
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        if (state.updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.updateInfo.releaseNotes.take(300),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetUpdateState()
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                Uri.parse(state.updateInfo.releaseUrl)
                            ).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLimePrimary,
                            contentColor = OnNeonLime
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                    ) {
                        Text("Later")
                    }
                }
            )
        }
        ManualUpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = SurfaceLight,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant,
                title = { Text("Up to Date") },
                text = {
                    Text("You're using the latest version of MyPlayer (v${com.example.myplayer.BuildConfig.VERSION_NAME}).")
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        is ManualUpdateState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                containerColor = SurfaceLight,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant,
                title = { Text("Check Failed") },
                text = {
                    Text("Could not check for updates: ${state.message}")
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.resetUpdateState() },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClayPrimary)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
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
                Spacer(Modifier.width(16.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
                )
            }
        }

        // Section: User Profile
        item { SettingsSectionHeader(title = "Profile") }

        item {
            SettingsNavigationRow(
                icon = Icons.Filled.Person,
                title = "Display Name",
                subtitle = userName,
                onClick = {
                    editNameInput = userName
                    showNameDialog = true
                }
            )
        }

        // Section: Backup & Restore
        item { SettingsSectionHeader(title = "Backup & Restore") }


        item {
            SettingsNavigationRow(
                icon = Icons.Filled.Backup,
                title = "Create Backup",
                subtitle = "Export your library, playlists, and settings to JSON",
                onClick = { backupLauncher.launch("myplayer_backup.json") }
            )
        }

        item {
            SettingsNavigationRow(
                icon = Icons.Filled.SettingsBackupRestore,
                title = "Restore Backup",
                subtitle = "Import playlists and auto-restore downloaded music",
                onClick = { restoreLauncher.launch(arrayOf("application/json")) }
            )
        }

        // Section: Experimental Features
        item { SettingsSectionHeader(title = "Experimental Features") }

        item {
            SettingsNavigationRow(
                icon = Icons.Filled.Headphones,
                title = "Dual Bud Mode",
                subtitle = "Play two independent songs simultaneously in left & right earbuds",
                onClick = onNavigateToDualBud
            )
        }

        // Section: About
        item { SettingsSectionHeader(title = "About") }

        item {
            SettingsNavigationRow(
                icon = Icons.Filled.SystemUpdate,
                title = "Check for Updates",
                subtitle = "Check GitHub for the latest MyPlayer release",
                onClick = { viewModel.checkForUpdates() }
            )
        }

        item {
            SettingsInfoRow(
                icon = Icons.Filled.Info,
                title = "Version",
                subtitle = com.example.myplayer.BuildConfig.VERSION_NAME
            )
        }

        item {
            SettingsInfoRow(
                icon = Icons.Filled.MusicNote,
                title = "Powered By",
                subtitle = "FlexFly Company Pvt Ltd"
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = ClayPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clayConcave(borderRadius = 12.dp, backgroundColor = SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnPrimary,
                checkedTrackColor = ClayPrimary,
                uncheckedThumbColor = OnSurfaceVariant,
                uncheckedTrackColor = SurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clayConcave(borderRadius = 12.dp, backgroundColor = SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .claySurface(borderRadius = 20.dp, backgroundColor = SurfaceLight)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clayConcave(borderRadius = 12.dp, backgroundColor = SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
    }
}
