package com.example.myplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myplayer.data.update.UpdateChecker
import com.example.myplayer.data.update.UpdateNotifier
import com.example.myplayer.ui.screens.main.MainScreen
import com.example.myplayer.ui.theme.MyPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var updateChecker: UpdateChecker
    @Inject lateinit var updateNotifier: UpdateNotifier

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkForUpdatesOnLaunch()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        checkForUpdatesOnLaunch()

        setContent {
            MyPlayerTheme {
                MainScreen()
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
                if (update != null && updateChecker.shouldNotifyFor(update)) {
                    updateNotifier.show(update)
                    updateChecker.markNotified(update)
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Launch update check failed: ${e.message}")
            }
        }
    }
}