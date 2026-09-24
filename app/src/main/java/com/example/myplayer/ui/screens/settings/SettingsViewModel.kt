package com.example.myplayer.ui.screens.settings

import android.content.Context
import java.io.IOException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.backup.BackupRestoreManager
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.local.prefs.PreferencesManager
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val preferencesManager: PreferencesManager,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val backupState = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val restoreState = _restoreState.asStateFlow()

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
