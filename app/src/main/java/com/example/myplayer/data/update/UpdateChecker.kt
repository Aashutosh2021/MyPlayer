package com.example.myplayer.data.update

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.myplayer.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val releaseUrl: String,
    val releaseNotes: String
)

/**
 * Polls the GitHub Releases API for the latest published (non-draft, non-prerelease)
 * release and compares it against the currently installed app version.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UpdateChecker"

        private const val GITHUB_OWNER = "Aashutosh2021"
        private const val GITHUB_REPO = "MyPlayer"

        private const val RELEASES_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        private const val PREFS_NAME = "update_check_prefs"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns UpdateInfo if a newer release exists than the app's own versionName.
     * Returns null when: no update available, 404 (no releases yet), or the response is malformed.
     * Throws IOException for transient network errors (5xx / 403 rate-limit).
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "MyPlayer-UpdateChecker")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub releases check returned HTTP ${response.code}")
                if (response.code == 403 || response.code >= 500) {
                    throw IOException("GitHub API returned ${response.code}")
                }
                return@withContext null
            }

            val bodyStr = response.body?.string() ?: return@withContext null
            val json = try {
                JSONObject(bodyStr)
            } catch (e: Exception) {
                Log.w(TAG, "Malformed release JSON", e)
                return@withContext null
            }

            val tagName = json.optString("tag_name", "")
            if (tagName.isBlank()) return@withContext null

            val remoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val htmlUrl = json.optString(
                "html_url",
                "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
            )
            val notes = json.optString("body", "")
            val releaseName = json.optString("name", tagName).ifBlank { remoteVersion }

            if (!isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            UpdateInfo(
                versionName = releaseName,
                releaseUrl = htmlUrl,
                releaseNotes = notes
            )
        }
    }

    /** True only if this exact version hasn't already triggered a notification. */
    fun shouldNotifyFor(update: UpdateInfo): Boolean {
        return prefs.getString(KEY_LAST_NOTIFIED_VERSION, null) != update.versionName
    }

    fun markNotified(update: UpdateInfo) {
        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, update.versionName).apply()
    }

    /** Numeric, per-segment comparison — "2.10" > "2.9", unlike plain string comparison. */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(r.size, l.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}
