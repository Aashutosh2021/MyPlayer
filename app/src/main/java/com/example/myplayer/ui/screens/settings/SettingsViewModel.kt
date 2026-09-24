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
    object Loading : BackupRestoreState
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
    private val updateChecker: UpdateChecker
) : ViewModel() {

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
        _backupState.value = BackupRestoreState.Loading
        viewModelScope.launch {
            try {
                val json = backupRestoreManager.generateBackup()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                }
                _backupState.value = BackupRestoreState.Success("Backup created successfully!")
            } catch (e: Exception) {
                _backupState.value = BackupRestoreState.Error(e.message ?: "Failed to write backup file")
            }
        }
    }

    fun importBackup(uri: Uri) {
        _restoreState.value = BackupRestoreState.Loading
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().use { it.readText() }
                } ?: throw IOException("Could not read backup file")
                
                backupRestoreManager.restoreBackup(json)
                _restoreState.value = BackupRestoreState.Success("Backup restored successfully!")
            } catch (e: Exception) {
                _restoreState.value = BackupRestoreState.Error(e.message ?: "Failed to restore backup file")
            }
        }
    }
}
