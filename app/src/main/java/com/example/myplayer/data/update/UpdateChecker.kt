package com.example.myplayer.data.update

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.myplayer.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val releaseUrl: String,
    val releaseNotes: String
)

class NoInternetException(
    message: String = "No internet connection detected. Please check your network or turn off Airplane Mode."
) : IOException(message)

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

        private val VERSION_REGEX = Regex("""(?i)\bv?(\d+(?:\.\d+)+)\b""")
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks if the device has an active internet-capable network connection.
     */
    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns UpdateInfo if a newer release exists than the app's own versionName.
     * Returns null when: no update available, 404 (no releases yet), or the response is malformed.
     * Throws NoInternetException for offline states or IOException for transient network errors.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            throw NoInternetException("No internet connection detected. Please turn off Airplane Mode or connect to Wi-Fi/mobile data.")
        }

        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "MyPlayer-UpdateChecker")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub releases check returned HTTP ${response.code}")
                    if (response.code == 403) {
                        throw IOException("GitHub API rate limit exceeded. Please try again later.")
                    }
                    if (response.code >= 500) {
                        throw IOException("GitHub server error (${response.code}). Please try again later.")
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
                val releaseName = json.optString("name", "")
                val notes = json.optString("body", "")

                // Look for direct APK asset download URL
                var apkDownloadUrl: String? = null
                var apkFileName: String? = null
                val assetsArray = json.optJSONArray("assets")
                if (assetsArray != null) {
                    for (i in 0 until assetsArray.length()) {
                        val asset = assetsArray.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkFileName = name
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val remoteVersion = extractVersion(tagName, releaseName, notes, apkFileName)
                val defaultUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
                val releaseHtmlUrl = json.optString("html_url", defaultUrl)
                val targetUrl = apkDownloadUrl?.takeIf { it.isNotBlank() } ?: releaseHtmlUrl
                val displayName = releaseName.ifBlank { "v$remoteVersion" }

                if (!isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                    return@withContext null
                }

                UpdateInfo(
                    versionName = displayName,
                    releaseUrl = targetUrl,
                    releaseNotes = notes
                )
            }
        } catch (e: UnknownHostException) {
            throw NoInternetException("Unable to reach GitHub. Please check your internet connection or turn off Airplane Mode.")
        } catch (e: ConnectException) {
            throw NoInternetException("Failed to connect to GitHub. Please check your network connection.")
        } catch (e: SocketTimeoutException) {
            throw IOException("Connection timed out while checking for updates. Please try again.")
        }
    }

    /** True only if this exact version hasn't already triggered a notification. */
    fun shouldNotifyFor(update: UpdateInfo): Boolean {
        return prefs.getString(KEY_LAST_NOTIFIED_VERSION, null) != update.versionName
    }

    fun markNotified(update: UpdateInfo) {
        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, update.versionName).apply()
    }

    /**
     * Extracts semantic version numbers (e.g. "3.1") from tagName, releaseName, APK name, or notes.
     */
    fun extractVersion(
        tagName: String,
        releaseName: String,
        body: String,
        assetName: String?
    ): String {
        // Priority 1: Check tagName (e.g. "v3.1", "3.2.0")
        VERSION_REGEX.find(tagName)?.groupValues?.get(1)?.let { return it }

        // Priority 2: Check release title (e.g. "MyPlayer version 3.1" -> "3.1")
        VERSION_REGEX.find(releaseName)?.groupValues?.get(1)?.let { return it }

        // Priority 3: Check APK asset filename (e.g. "MyPlayer-v3.1.apk" -> "3.1")
        if (!assetName.isNullOrBlank()) {
            VERSION_REGEX.find(assetName)?.groupValues?.get(1)?.let { return it }
        }

        // Priority 4: Check release notes / body (e.g. "v3.1")
        VERSION_REGEX.find(body)?.groupValues?.get(1)?.let { return it }

        // Fallback: strip leading 'v'
        val clean = tagName.removePrefix("v").removePrefix("V").trim()
        return clean.ifBlank { "0.0" }
    }

    /** Numeric, per-segment comparison — "2.10" > "2.9", unlike plain string comparison. */
    fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val l = local.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val maxLen = maxOf(r.size, l.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}
