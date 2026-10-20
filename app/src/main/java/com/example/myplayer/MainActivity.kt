package com.example.myplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.update.UpdateChecker
import com.example.myplayer.data.update.UpdateInfo
import com.example.myplayer.data.update.UpdateNotifier
import com.example.myplayer.ui.screens.main.MainScreen
import com.example.myplayer.ui.theme.MyPlayerTheme
import com.example.myplayer.ui.theme.ThemeState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var updateChecker: UpdateChecker
    @Inject lateinit var updateNotifier: UpdateNotifier
    @Inject lateinit var settingsDataStore: SettingsDataStore

    private var launchUpdateAvailable by mutableStateOf<UpdateInfo?>(null)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkForUpdatesOnLaunch()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Read saved theme synchronously before 1st frame (prevents white flash on startup)
        val prefs = getSharedPreferences("theme_sync_prefs", MODE_PRIVATE)
        val initialDark = prefs.getBoolean("dark_mode_enabled", false)
        ThemeState.isDarkMode = initialDark

        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        checkForUpdatesOnLaunch()

        setContent {
            val scope = rememberCoroutineScope()
            val dataStoreDark by settingsDataStore.isDarkMode.collectAsStateWithLifecycle(
                initialValue = initialDark
            )

            // 2. When DataStore emits a value, keep ThemeState & SharedPreferences synced
            LaunchedEffect(dataStoreDark) {
                ThemeState.isDarkMode = dataStoreDark
                prefs.edit().putBoolean("dark_mode_enabled", dataStoreDark).apply()
            }

            // 3. When HomeScreen or SettingsScreen calls ThemeState.toggle() / setDark(),
            // automatically persist to both SharedPreferences and DataStore!
            DisposableEffect(Unit) {
                ThemeState.onThemeChanged = { enabled ->
                    prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
                    scope.launch {
                        settingsDataStore.setDarkMode(enabled)
                    }
                }
                onDispose {
                    ThemeState.onThemeChanged = null
                }
            }

            MyPlayerTheme(darkTheme = ThemeState.isDarkMode) {
                MainScreen()

                launchUpdateAvailable?.let { update ->
                    AlertDialog(
                        onDismissRequest = { launchUpdateAvailable = null },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = { Text("Update Available", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text(
                                    "A newer version (${update.versionName}) of MyPlayer is available!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Visit our official website to download the latest release APK.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (update.releaseNotes.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        update.releaseNotes.take(250),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val targetUrl = update.releaseUrl.ifBlank { UpdateChecker.UPDATE_WEBSITE_URL }
                                    launchUpdateAvailable = null
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Update Now", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { launchUpdateAvailable = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("Later")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkForUpdatesOnLaunch() {
        lifecycleScope.launch {
            try {
                val update = updateChecker.checkForUpdate()
                if (update != null) {
                    launchUpdateAvailable = update
                    if (updateChecker.shouldNotifyFor(update)) {
                        updateNotifier.show(update)
                        updateChecker.markNotified(update)
                    }
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Launch update check failed: ${e.message}")
            }
        }
    }
}