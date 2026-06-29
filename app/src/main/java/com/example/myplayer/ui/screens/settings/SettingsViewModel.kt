package com.example.myplayer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.local.prefs.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val isAutoplayEnabled = settingsDataStore.isAutoplayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val downloadFolderUri = preferencesManager.downloadFolderUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoplayEnabled(enabled)
        }
    }

    fun setDownloadFolder(uri: String?) {
        viewModelScope.launch {
            preferencesManager.setDownloadFolderUri(uri)
        }
    }
}
