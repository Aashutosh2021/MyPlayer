package com.example.myplayer.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "myplayer_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
    }

    val downloadFolderUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DOWNLOAD_FOLDER_URI]
    }

    suspend fun setDownloadFolderUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(DOWNLOAD_FOLDER_URI)
            } else {
                preferences[DOWNLOAD_FOLDER_URI] = uri
            }
        }
    }
}
