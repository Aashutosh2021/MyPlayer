package com.example.myplayer.data.recommendation.lastfm

import com.example.myplayer.BuildConfig
import com.example.myplayer.security.StringEncryptionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmConfig @Inject constructor(
    private val encryptionManager: StringEncryptionManager
) {
    @Volatile
    private var customApiKey: String? = null

    val baseUrl: String = "https://ws.audioscrobbler.com/2.0/"

    /**
     * Resolves the active Last.fm API key in priority order:
     * 1. Runtime override (for testing or user settings)
     * 2. BuildConfig.LASTFM_API_KEY (from local.properties / gradle.properties)
     * 3. Fallback public API key for demo / chart read access
     */
    val apiKey: String
        get() {
            customApiKey?.takeIf { it.isNotBlank() }?.let { return it }

            val buildConfigKey = BuildConfig.LASTFM_API_KEY
            if (buildConfigKey.isNotBlank()) {
                return buildConfigKey
            }

            // Public demonstration key for read-only charts
            return "b25b959554ed76058ac220b7b2e0a026"
        }

    fun hasValidKey(): Boolean = apiKey.isNotBlank()

    fun setCustomApiKey(key: String?) {
        customApiKey = key?.trim()
    }
}
