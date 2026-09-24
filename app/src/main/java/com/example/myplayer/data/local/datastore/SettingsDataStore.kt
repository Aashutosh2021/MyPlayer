package com.example.myplayer.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
        val USER_NAME = stringPreferencesKey("user_display_name")
    }

    val isAutoplayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTOPLAY_ENABLED] ?: true
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTOPLAY_ENABLED] = enabled
        }
    }

    val userName: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: "Alex Rivera"
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = name.trim().ifBlank { "User" }
        }
    }
}

