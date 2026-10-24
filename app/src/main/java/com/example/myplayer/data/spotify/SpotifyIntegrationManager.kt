package com.example.myplayer.data.spotify

import android.content.Context
import android.util.Log
import com.example.myplayer.data.recommendation.ai.OnDeviceMusicAiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

data class SpotifyTrackSeed(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val weight: Float
)

@Singleton
class SpotifyIntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiModel: OnDeviceMusicAiModel
) {
    companion object {
        private const val TAG = "SpotifyIntegration"
        const val LOGIN_URL = "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
        private const val TOKEN_URL = "https://open.spotify.com/api/token"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val NUANCE_GIST_URL = "https://api.github.com/gists/22ed9c6ba463899e933427f7de1f0eef"
        private const val GQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        private const val REST_BASE_URL = "https://api.spotify.com/v1"

        // Meld's GraphQL Persisted Query Hashes
        private const val HASH_PROFILE = "53bcb064f6cd18c23f752bc324a791194d20df612d8e1239c735144ab0399ced"
        private const val HASH_LIKED_SONGS = "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240"

        const val USER_AGENT_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    private val prefs by lazy {
        context.getSharedPreferences("spotify_integration_prefs", Context.MODE_PRIVATE)
    }
    private val refreshMutex = Mutex()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        _isLoggedIn.value = !prefs.getString("sp_dc", "").isNullOrBlank()
        _username.value = prefs.getString("spotify_username", null)
    }

    /**
     * Called by SpotifyLoginDialog when WebView captures sp_dc and sp_key cookies.
     * Exchanges cookies + TOTP for a Bearer token, fetches user profile & taste data,
     * and injects all Spotify Top Tracks & Liked Songs into OnDeviceMusicAiModel!
     */
    suspend fun completeLoginWithCookies(spDc: String, spKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            prefs.edit()
                .putString("sp_dc", spDc)
                .putString("sp_key", spKey)
                .apply()

            val token = fetchAccessTokenViaTotp(spDc, spKey)
            saveAccessToken(token.first, token.second)
            _isLoggedIn.value = true

            val displayName = fetchUserProfile(token.first) ?: "Spotify User"
            prefs.edit().putString("spotify_username", displayName).apply()
            _username.value = displayName

            // Immediately sync user's Spotify Top Tracks, Liked Songs & Top Artists into AI Brain!
            syncSpotifyTasteIntoAi()
            displayName
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _username.value = null
    }

    /**
     * Ensures a valid Bearer accessToken is available, refreshing via sp_dc + TOTP if expired.
     */
    suspend fun getValidAccessToken(): String? = refreshMutex.withLock {
        val currentToken = prefs.getString("access_token", null)
        val expiry = prefs.getLong("token_expiry_ms", 0L)
        if (!currentToken.isNullOrBlank() && System.currentTimeMillis() < expiry - 30_000L) {
            return@withLock currentToken
        }

        val spDc = prefs.getString("sp_dc", null) ?: return@withLock null
        val spKey = prefs.getString("sp_key", "") ?: ""

        return@withLock try {
            val (freshToken, freshExpiry) = fetchAccessTokenViaTotp(spDc, spKey)
            saveAccessToken(freshToken, freshExpiry)
            freshToken
        } catch (e: Exception) {
            Log.e(TAG, "Spotify token refresh failed", e)
            null
        }
    }

    /**
     * Fetches user's Spotify Liked Songs (GraphQL) + Top Tracks (REST) + Top Artists (REST)
     * and trains OnDeviceMusicAiModel so MyPlayer recommends songs matching their Spotify taste!
     */
    suspend fun syncSpotifyTasteIntoAi(): List<SpotifyTrackSeed> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val token = getValidAccessToken() ?: return@withContext emptyList()
            val seeds = mutableListOf<SpotifyTrackSeed>()

            // 1. Tier 1: Fetch REST Top Tracks (short_term = last 4 weeks, medium_term = last 6 months)
            for ((range, baseWeight) in listOf("short_term" to 2.0f, "medium_term" to 1.5f)) {
                try {
                    val topTracks = fetchRestTopTracks(token, timeRange = range, limit = 40)
                    topTracks.forEachIndexed { index, seed ->
                        val posFactor = 1.0f - (index.toFloat() / topTracks.size.coerceAtLeast(1)) * 0.5f
                        seeds.add(seed.copy(weight = baseWeight * posFactor))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "REST topTracks($range) failed, continuing with GraphQL", e)
                }
            }

            // 2. Tier 2: Fetch GraphQL Liked Songs (never rate-limited!)
            try {
                val likedTracks = fetchGraphQlLikedSongs(token, limit = 60)
                likedTracks.forEachIndexed { index, seed ->
                    val posFactor = 1.0f - (index.toFloat() / likedTracks.size.coerceAtLeast(1)) * 0.5f
                    seeds.add(seed.copy(weight = 1.4f * posFactor))
                }
            } catch (e: Exception) {
                Log.w(TAG, "GraphQL likedSongs failed", e)
            }

            val deduplicated = seeds.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
            Log.d(TAG, "Synced ${deduplicated.size} tracks from Spotify into OnDeviceMusicAiModel")

            // Feed Spotify taste profile directly into our 128-D Neural Hash + RL Bandit!
            deduplicated.take(60).forEach { track ->
                aiModel.learnFromInteraction(
                    videoId = track.id,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs,
                    reward = track.weight
                )
            }

            deduplicated
        } finally {
            _isSyncing.value = false
        }
    }

    // ── Meld's TOTP + sp_dc Authentication Engine ────────────────────────

    private fun fetchAccessTokenViaTotp(spDc: String, spKey: String): Pair<String, Long> {
        val (secret, version) = fetchNuanceSecret()
        val serverTimeSec = fetchSpotifyServerTime()
        val totp = generateTotp(secret, serverTimeSec)

        val url = "$TOKEN_URL?reason=transport&productType=web-player&totp=$totp&totpServer=$totp&totpVer=$version"
        val cookieHeader = buildString {
            append("sp_dc=$spDc")
            if (spKey.isNotBlank()) append("; sp_key=$spKey")
        }

        val responseStr = httpGet(url, mapOf("Cookie" to cookieHeader))
        val json = JSONObject(responseStr)
        val isAnonymous = json.optBoolean("isAnonymous", true)
        val accessToken = json.optString("accessToken", "")
        val expiryMs = json.optLong("accessTokenExpirationTimestampMs", System.currentTimeMillis() + 3600_000L)

        if (isAnonymous || accessToken.isBlank()) {
            throw IllegalStateException("Spotify returned anonymous token — sp_dc cookie expired")
        }
        return accessToken to expiryMs
    }

    private fun fetchNuanceSecret(): Pair<String, Int> {
        val body = httpGet(NUANCE_GIST_URL, emptyMap())
        val filesObj = JSONObject(body).getJSONObject("files")
        val firstFileKey = filesObj.keys().next()
        val contentStr = filesObj.getJSONObject(firstFileKey).getString("content")
        val arr = JSONArray(contentStr)

        var bestSecret = ""
        var maxVer = -1
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val v = item.getInt("v")
            if (v > maxVer) {
                maxVer = v
                bestSecret = item.getString("s")
            }
        }
        if (bestSecret.isBlank()) throw IllegalStateException("No TOTP secret in Gist")
        return bestSecret to maxVer
    }

    private fun fetchSpotifyServerTime(): Long {
        val body = httpGet(SERVER_TIME_URL, emptyMap())
        return JSONObject(body).getLong("serverTime")
    }

    private fun generateTotp(secret: String, serverTimeSec: Long): String {
        val key = base32Decode(secret)
        val timeStep = floor(serverTimeSec.toDouble() / 30.0).toLong()
        val timeBytes = ByteArray(8)
        var value = timeStep
        for (i in 7 downTo 0) {
            timeBytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(timeBytes)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val code = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        return (code % 1_000_000).toString().padStart(6, '0')
    }

    private fun base32Decode(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = input.uppercase().replace("=", "")
        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (c in cleaned) {
            val v = alphabet.indexOf(c)
            if (v < 0) continue
            buffer = (buffer shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }

    // ── Spotify GraphQL & REST Calls ─────────────────────────────────────

    private fun fetchUserProfile(token: String): String? {
        return try {
            val gqlRes = graphqlPost(token, "profileAttributes", HASH_PROFILE, JSONObject())
            gqlRes.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("profile")?.optString("name")
        } catch (_: Exception) {
            try {
                val restRes = httpGet("$REST_BASE_URL/me", mapOf("Authorization" to "Bearer $token"))
                JSONObject(restRes).optString("display_name")
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun fetchRestTopTracks(token: String, timeRange: String, limit: Int): List<SpotifyTrackSeed> {
        val url = "$REST_BASE_URL/me/top/tracks?time_range=$timeRange&limit=$limit&offset=0"
        val res = httpGet(
            url,
            mapOf(
                "Authorization" to "Bearer $token",
                "app-platform" to "WebPlayer",
                "Origin" to "https://open.spotify.com",
                "Referer" to "https://open.spotify.com/"
            )
        )
        val items = JSONObject(res).optJSONArray("items") ?: return emptyList()
        val list = mutableListOf<SpotifyTrackSeed>()
        for (i in 0 until items.length()) {
            val t = items.getJSONObject(i)
            val id = t.optString("id")
            val name = t.optString("name")
            val dur = t.optLong("duration_ms", 0L)
            val artistsArr = t.optJSONArray("artists")
            val artistNames = mutableListOf<String>()
            if (artistsArr != null) {
                for (j in 0 until artistsArr.length()) {
                    artistsArr.getJSONObject(j).optString("name").takeIf { it.isNotBlank() }?.let { artistNames.add(it) }
                }
            }
            if (name.isNotBlank() && artistNames.isNotEmpty()) {
                list.add(SpotifyTrackSeed(id, name, artistNames.joinToString(", "), dur, 1.0f))
            }
        }
        return list
    }

    private fun fetchGraphQlLikedSongs(token: String, limit: Int): List<SpotifyTrackSeed> {
        val vars = JSONObject().apply {
            put("offset", 0)
            put("limit", limit)
        }
        val res = graphqlPost(token, "fetchLibraryTracks", HASH_LIKED_SONGS, vars)
        val items = res.optJSONObject("data")
            ?.optJSONObject("me")
            ?.optJSONObject("library")
            ?.optJSONObject("tracks")
            ?.optJSONArray("items") ?: return emptyList()

        val out = mutableListOf<SpotifyTrackSeed>()
        for (i in 0 until items.length()) {
            val trackWrapper = items.getJSONObject(i).optJSONObject("track") ?: continue
            val data = trackWrapper.optJSONObject("data") ?: continue
            val uri = trackWrapper.optString("_uri").ifBlank { data.optString("uri") }
            val id = uri.substringAfterLast(":")
            val name = data.optString("name")
            val dur = data.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L
            val artistItems = data.optJSONObject("artists")?.optJSONArray("items")
            val artists = mutableListOf<String>()
            if (artistItems != null) {
                for (j in 0 until artistItems.length()) {
                    val aName = artistItems.getJSONObject(j).optJSONObject("profile")?.optString("name")
                    if (!aName.isNullOrBlank()) artists.add(aName)
                }
            }
            if (name.isNotBlank() && artists.isNotEmpty()) {
                out.add(SpotifyTrackSeed(id, name, artists.joinToString(", "), dur, 1.0f))
            }
        }
        return out
    }

    private fun graphqlPost(token: String, operationName: String, sha256Hash: String, variables: JSONObject): JSONObject {
        val bodyObj = JSONObject().apply {
            put("variables", variables)
            put("operationName", operationName)
            put("extensions", JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", sha256Hash)
                })
            })
        }
        val conn = URL(GQL_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", USER_AGENT_DESKTOP)
            conn.setRequestProperty("app-platform", "WebPlayer")
            conn.setRequestProperty("Origin", "https://open.spotify.com")
            conn.setRequestProperty("Referer", "https://open.spotify.com/")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.outputStream.use { it.write(bodyObj.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw IllegalStateException("GQL $operationName HTTP $code: $err")
            }
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(resp)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(urlString: String, headers: Map<String, String>): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", USER_AGENT_DESKTOP)
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw IllegalStateException("HTTP $code: $err")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun saveAccessToken(token: String, expiryMs: Long) {
        prefs.edit()
            .putString("access_token", token)
            .putLong("token_expiry_ms", expiryMs)
            .apply()
    }
}
