package com.example.myplayer.ui.screens.settings

import android.content.Context
import java.io.IOException
import java.net.UnknownHostException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.backup.BackupRestoreManager
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.local.prefs.PreferencesManager
import com.example.myplayer.data.update.NoInternetException
import com.example.myplayer.data.update.UpdateChecker
import com.example.myplayer.data.update.UpdateInfo
import com.example.myplayer.data.preferences.AudioPlaybackConfig
import com.example.myplayer.data.preferences.AudioSettingsRepository
import com.example.myplayer.data.preferences.ClarityPreset
import com.example.myplayer.data.preferences.StreamQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupRestoreState {
    object Idle : BackupRestoreState
    data class Loading(val progress: String? = null) : BackupRestoreState
    data class Success(val message: String) : BackupRestoreState
    data class Error(val error: String) : BackupRestoreState
}

sealed interface ManualUpdateState {
    object Idle : ManualUpdateState
    object Checking : ManualUpdateState
    data class Available(val updateInfo: UpdateInfo) : ManualUpdateState
    object UpToDate : ManualUpdateState
    data class NoInternet(val message: String) : ManualUpdateState
    data class Error(val message: String) : ManualUpdateState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val preferencesManager: PreferencesManager,
    private val backupRestoreManager: BackupRestoreManager,
    private val updateChecker: UpdateChecker,
    val spotifyManager: com.example.myplayer.data.spotify.SpotifyIntegrationManager,
    private val audioSettingsRepository: AudioSettingsRepository,
    val audioPlaybackStore: com.example.myplayer.data.preferences.AudioPlaybackStore,
    val audioFeatureManagers: com.example.myplayer.playback.AudioFeatureManagers
) : ViewModel() {

    val audioConfig = audioSettingsRepository.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioPlaybackConfig())

    val isSpotifyLoggedIn = spotifyManager.isLoggedIn
    val spotifyUsername = spotifyManager.username
    val isSpotifySyncing = spotifyManager.isSyncing

    private val _spotifySyncMessage = MutableStateFlow<String?>(null)
    val spotifySyncMessage = _spotifySyncMessage.asStateFlow()

    fun clearSpotifySyncMessage() {
        _spotifySyncMessage.value = null
    }

    fun syncSpotify() {
        viewModelScope.launch {
            val res = spotifyManager.syncAllSpotifyLibraryAndPlaylists()
            res.onSuccess { result ->
                _spotifySyncMessage.value = "Synced ${result.playlistsCount} playlists & ${result.totalTracksSynced} songs from Spotify!"
            }.onFailure { e ->
                _spotifySyncMessage.value = "Spotify sync failed: ${e.message}"
            }
        }
    }

    fun syncSpotifyTaste() {
        syncSpotify()
    }

    fun logoutSpotify() {
        spotifyManager.logout()
    }

    private val _backupState = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val backupState = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val restoreState = _restoreState.asStateFlow()

    private val _updateState = MutableStateFlow<ManualUpdateState>(ManualUpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    val isAutoplayEnabled = settingsDataStore.isAutoplayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val downloadFolderUri = preferencesManager.downloadFolderUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userName = settingsDataStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex Rivera")

    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoplayEnabled(enabled)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsDataStore.setUserName(name)
        }
    }

    fun setDownloadFolder(uri: String?) {
        viewModelScope.launch {
            preferencesManager.setDownloadFolderUri(uri)
        }
    }

    fun updateWifiQuality(quality: StreamQuality) {
        viewModelScope.launch { audioSettingsRepository.updateWifiQuality(quality) }
    }

    fun updateMobileQuality(quality: StreamQuality) {
        viewModelScope.launch { audioSettingsRepository.updateMobileQuality(quality) }
    }

    fun updateClarityPreset(preset: ClarityPreset) {
        viewModelScope.launch { audioSettingsRepository.updateClarityPreset(preset) }
    }

    fun updateTrueDolby(enabled: Boolean, width: Float = 0.45f) {
        viewModelScope.launch { audioSettingsRepository.updateTrueDolby(enabled, width) }
    }

    fun updateLoudness(enabled: Boolean, gainMb: Int = 600) {
        viewModelScope.launch { audioSettingsRepository.updateLoudness(enabled, gainMb) }
    }

    fun updateSkipSilence(enabled: Boolean) {
        viewModelScope.launch { audioSettingsRepository.updateSkipSilence(enabled) }
    }

    fun resetStates() {
        _backupState.value = BackupRestoreState.Idle
        _restoreState.value = BackupRestoreState.Idle
    }

    fun checkForUpdates() {
        _updateState.value = ManualUpdateState.Checking
        viewModelScope.launch {
            try {
                if (!updateChecker.isOnline()) {
                    _updateState.value = ManualUpdateState.NoInternet(
                        "No internet connection. Please turn off Airplane Mode or connect to Wi-Fi / mobile data and try again."
                    )
                    return@launch
                }
                val update = updateChecker.checkForUpdate()
                if (update != null) {
                    _updateState.value = ManualUpdateState.Available(update)
                } else {
                    _updateState.value = ManualUpdateState.UpToDate
                }
            } catch (e: NoInternetException) {
                _updateState.value = ManualUpdateState.NoInternet(
                    e.message ?: "No internet connection detected."
                )
            } catch (e: UnknownHostException) {
                _updateState.value = ManualUpdateState.NoInternet(
                    "Unable to reach GitHub. Please check your internet connection or turn off Airplane Mode."
                )
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "No internet connection. Please turn off Airplane Mode or check your network."
                    e.message?.contains("rate limit", ignoreCase = true) == true ->
                        "GitHub rate limit reached. Please try again later."
                    else ->
                        e.message ?: "Failed to check for updates. Please try again later."
                }
                _updateState.value = ManualUpdateState.Error(errorMsg)
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = ManualUpdateState.Idle
    }

    fun exportBackup(uri: Uri) {
        _backupState.value = BackupRestoreState.Loading()
        viewModelScope.launch {
            try {
                val isArchive = runCatching { isZipBackup(uri) }.getOrDefault(false)
                val message = if (isArchive) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("Could not open file for writing")
                    val packed = backupRestoreManager.generateBackupArchive(output) { packed, total ->
                        _backupState.value = BackupRestoreState.Loading("Copying songs... $packed / $total")
                    }
                    "Backup created successfully! ($packed downloaded songs included)"
                } else {
                    // Legacy JSON export (metadata only)
                    val json = backupRestoreManager.generateBackup()
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    }
                    "Backup created successfully!"
                }
                _backupState.value = BackupRestoreState.Success(message)
            } catch (e: Exception) {
                _backupState.value = BackupRestoreState.Error(e.message ?: "Failed to write backup file")
            }
        }
    }

    fun importBackup(uri: Uri) {
        _restoreState.value = BackupRestoreState.Loading()
        viewModelScope.launch {
            try {
                val isArchive = runCatching { isZipBackup(uri) }.getOrDefault(false)
                if (isArchive) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        backupRestoreManager.restoreBackupArchive(input)
                    } ?: throw IOException("Could not read backup file")
                } else {
                    // Legacy JSON backup (metadata only; missing downloads re-fetch in background)
                    val json = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().use { it.readText() }
                    } ?: throw IOException("Could not read backup file")
                    backupRestoreManager.restoreBackup(json)
                }
                _restoreState.value = BackupRestoreState.Success("Backup restored successfully!")
            } catch (e: Exception) {
                _restoreState.value = BackupRestoreState.Error(e.message ?: "Failed to restore backup file")
            }
        }
    }

    /** ZIP archives start with the "PK" magic bytes. */
    private suspend fun isZipBackup(uri: Uri): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val magic = ByteArray(2)
            val read = input.read(magic)
            read == 2 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()
        } ?: false
    }
}


