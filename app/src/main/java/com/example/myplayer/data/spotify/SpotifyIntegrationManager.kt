package com.example.myplayer.data.spotify

import android.content.Context
import android.util.Log
import com.example.myplayer.data.local.dao.FavoriteDao
import com.example.myplayer.data.local.dao.PlaylistDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.FavoriteEntity
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.local.entity.PlaylistSongCrossReference
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.recommendation.ai.OnDeviceMusicAiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val weight: Float,
    val artworkUrl: String? = null
)

data class SpotifyPlaylistSummary(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val totalTracks: Int
)

data class SpotifyFullSyncResult(
    val likedSongsCount: Int,
    val playlistsCount: Int,
    val totalTracksSynced: Int
)

@Singleton
class SpotifyIntegrationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val aiModel: OnDeviceMusicAiModel,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao
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
        private const val HASH_SEARCH_DESKTOP = "4801118d4a100f756e833d33984436a3899cff359c532f8fd3aaf174b60b3b49"
        private const val HASH_ADD_TO_LIBRARY = "7c5a69420e2bfae3da5cc4e14cbc8bb3f6090f80afc00ffc179177f19be3f33d"
        private const val HASH_ARTIST_OVERVIEW = "5b9e64f43843fa3a9b6a98543600299b0a2cbbbccfdcdcef2402eb9c1017ca4c"
        private const val HASH_LIBRARY_V3 = "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3"
        private const val HASH_FETCH_PLAYLIST = "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763"

        const val USER_AGENT_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    private val prefs by lazy {
        context.getSharedPreferences("spotify_integration_prefs", Context.MODE_PRIVATE)
    }
    private val refreshMutex = Mutex()

    // Fire-and-forget scope for the heavyweight full-library sync so the login flow
    // and taste-sync callers are never blocked for minutes by playlist fetching.
    // Settings' manual "Sync Library" button still awaits the sync directly.
    private val librarySyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Serializes concurrent full-library syncs (login background sync + manual sync)
    // so Room inserts and playlist cross-refs can't interleave.
    private val fullSyncMutex = Mutex()

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

            // 1. Tier 1 (Preferred): Fetch GraphQL Liked Songs (resistant to 429 rate-limiting!)
            try {
                val likedTracks = fetchGraphQlLikedSongs(token, limit = 60)
                likedTracks.forEachIndexed { index, seed ->
                    val posFactor = 1.0f - (index.toFloat() / likedTracks.size.coerceAtLeast(1)) * 0.5f
                    seeds.add(seed.copy(weight = 1.5f * posFactor))
                }
            } catch (e: Exception) {
                Log.w(TAG, "GraphQL likedSongs failed", e)
            }

            delay(150L)

            // 2. Tier 2: Fetch REST Top Tracks with delays
            for ((range, baseWeight) in listOf("short_term" to 1.8f, "medium_term" to 1.3f)) {
                try {
                    val topTracks = fetchRestTopTracks(token, timeRange = range, limit = 40)
                    topTracks.forEachIndexed { index, seed ->
                        val posFactor = 1.0f - (index.toFloat() / topTracks.size.coerceAtLeast(1)) * 0.5f
                        seeds.add(seed.copy(weight = baseWeight * posFactor))
                    }
                    delay(150L)
                } catch (e: Exception) {
                    Log.w(TAG, "REST topTracks($range) failed, continuing with available seeds", e)
                }
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

            // Also synchronize full Spotify library and playlists into MyPlayer Room DB.
            // Fire-and-forget: awaiting this (all playlists + tracks fetches with retry
            // pauses) inside the login chain made the dialog look eternally "stuck on
            // loading". The login completes, the dialog dismisses, and the library
            // sync continues in the background.
            librarySyncScope.launch {
                // Brief settle delay: Spotify just served the login + taste-sync burst;
                // firing the full-library burst instantly increases 429 rate-limiting.
                delay(3_000L)
                runCatching { syncAllSpotifyLibraryAndPlaylists() }
                    .onFailure { Log.w(TAG, "Full Spotify library sync failed", it) }
            }

            deduplicated
        } finally {
            _isSyncing.value = false
        }
    }

    data class SpotifyResolvedMatch(
        val trackId: String,
        val trackUri: String,
        val title: String,
        val artistId: String,
        val artistName: String
    )

    /**
     * 2-WAY BRIDGE:
     * 1. Finds the song currently playing in MyPlayer on Spotify (searchDesktop)
     * 2. If syncToLibrary = true (e.g. listened >30s or Liked), pushes it to your Spotify Library (addToLibrary)
     * 3. Fetches Spotify's live recommendations (Artist Top Tracks + Related Artists' Top Tracks)
     *    and trains MyPlayer's On-Device AI!
     */
    suspend fun reportPlayedSongAndFetchSpotifyRecommendations(
        title: String,
        artist: String,
        syncToSpotifyLibrary: Boolean = true
    ): List<SpotifyTrackSeed> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext emptyList()
        try {
            // 1. Resolve MyPlayer song on Spotify
            val match = searchTrackOnSpotify(token, "$title $artist") ?: return@withContext emptyList()
            Log.d(TAG, "Matched on Spotify: '${match.title}' by ${match.artistName} (track=${match.trackId}, artist=${match.artistId})")

            // 2. Push song to Spotify Library so Spotify's taste algorithm updates!
            if (syncToSpotifyLibrary && match.trackUri.isNotBlank()) {
                pushTrackToSpotifyLibrary(token, match.trackUri)
            }

            // 3. Pull Spotify's live recommendations (Top Tracks + Related Artists from queryArtistOverview)
            val spotifyRecos = fetchSpotifyArtistAndRelatedRecommendations(token, match.artistId)

            // 4. Feed Spotify's new recommendations into MyPlayer's On-Device AI!
            spotifyRecos.forEachIndexed { idx, seed ->
                val weight = (1.5f - (idx * 0.03f)).coerceAtLeast(0.5f)
                aiModel.learnFromInteraction(
                    videoId = seed.id,
                    title = seed.title,
                    artist = seed.artist,
                    durationMs = seed.durationMs,
                    reward = weight
                )
            }

            spotifyRecos
        } catch (e: Exception) {
            Log.w(TAG, "2-Way Spotify recommendation bridge failed", e)
            emptyList()
        }
    }

    /**
     * Step 1: Search MyPlayer's song on Spotify via GraphQL `searchDesktop`
     */
    private suspend fun searchTrackOnSpotify(token: String, query: String): SpotifyResolvedMatch? {
        val vars = JSONObject().apply {
            put("searchTerm", query.trim())
            put("offset", 0)
            put("limit", 5)
            put("numberOfTopResults", 3)
            put("includeAudiobooks", false)
            put("includeArtistHasConcertsField", false)
            put("includePreReleases", false)
            put("includeLocalConcertsField", false)
            put("includeAuthors", false)
        }

        val res = graphqlPost(token, "searchDesktop", HASH_SEARCH_DESKTOP, vars)
        val items = res.optJSONObject("data")
            ?.optJSONObject("searchV2")
            ?.optJSONObject("tracksV2")
            ?.optJSONArray("items") ?: return null

        if (items.length() == 0) return null
        val firstItem = items.getJSONObject(0).optJSONObject("item")?.optJSONObject("data") ?: return null

        val trackUri = firstItem.optString("uri")
        val trackId = trackUri.substringAfterLast(":")
        val trackName = firstItem.optString("name")

        val firstArtist = firstItem.optJSONObject("artists")?.optJSONArray("items")?.optJSONObject(0)
        val artistUri = firstArtist?.optString("uri") ?: ""
        val artistId = artistUri.substringAfterLast(":")
        val artistName = firstArtist?.optJSONObject("profile")?.optString("name") ?: ""

        if (trackId.isBlank() || artistId.isBlank()) return null
        return SpotifyResolvedMatch(trackId, trackUri, trackName, artistId, artistName)
    }

    /**
     * Step 2: Send played/liked song to Spotify (`addToLibrary` GraphQL mutation)
     * so Spotify updates your personal taste profile!
     */
    private suspend fun pushTrackToSpotifyLibrary(token: String, trackUri: String) {
        try {
            val vars = JSONObject().apply {
                put("libraryItemUris", JSONArray().put(trackUri))
            }
            graphqlPost(token, "addToLibrary", HASH_ADD_TO_LIBRARY, vars)
            Log.d(TAG, "Synced track $trackUri to Spotify Library!")
        } catch (e: Exception) {
            Log.w(TAG, "pushTrackToSpotifyLibrary failed for $trackUri", e)
        }
    }

    /**
     * Step 3: Fetch Spotify's Recommended Tracks + Related Artists via `queryArtistOverview`
     */
    private suspend fun fetchSpotifyArtistAndRelatedRecommendations(
        token: String,
        artistId: String
    ): List<SpotifyTrackSeed> {
        val out = mutableListOf<SpotifyTrackSeed>()
        val relatedArtistIds = mutableListOf<String>()

        fun parseOverviewTracks(artistObj: JSONObject, collectRelatedIds: Boolean) {
            val topTracks = artistObj.optJSONObject("discography")
                ?.optJSONObject("topTracks")
                ?.optJSONArray("items")

            if (topTracks != null) {
                for (i in 0 until topTracks.length()) {
                    val trackObj = topTracks.getJSONObject(i).optJSONObject("track") ?: continue
                    val id = trackObj.optString("id").ifBlank { trackObj.optString("uri").substringAfterLast(":") }
                    val name = trackObj.optString("name")
                    val dur = trackObj.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L
                    val artistItems = trackObj.optJSONObject("artists")?.optJSONArray("items")
                    val artistNames = mutableListOf<String>()
                    if (artistItems != null) {
                        for (j in 0 until artistItems.length()) {
                            val aName = artistItems.getJSONObject(j).optJSONObject("profile")?.optString("name")
                            if (!aName.isNullOrBlank()) artistNames.add(aName)
                        }
                    }
                    if (name.isNotBlank() && artistNames.isNotEmpty()) {
                        out.add(SpotifyTrackSeed(id, name, artistNames.joinToString(", "), dur, 1.3f))
                    }
                }
            }

            if (collectRelatedIds) {
                val relatedItems = artistObj.optJSONObject("relatedContent")
                    ?.optJSONObject("relatedArtists")
                    ?.optJSONArray("items")
                if (relatedItems != null) {
                    for (i in 0 until relatedItems.length().coerceAtMost(4)) {
                        val relUri = relatedItems.getJSONObject(i).optString("uri")
                        val relId = relUri.substringAfterLast(":")
                        if (relId.isNotBlank()) relatedArtistIds.add(relId)
                    }
                }
            }
        }

        // 1. Fetch main artist's top tracks + related artist IDs from Spotify
        val mainVars = JSONObject().apply {
            put("uri", "spotify:artist:$artistId")
            put("locale", "")
        }
        val mainRes = graphqlPost(token, "queryArtistOverview", HASH_ARTIST_OVERVIEW, mainVars)
        val mainArtistUnion = mainRes.optJSONObject("data")?.optJSONObject("artistUnion")
        if (mainArtistUnion != null) {
            parseOverviewTracks(mainArtistUnion, collectRelatedIds = true)
        }

        // 2. Fetch top tracks from Spotify's top 3 Related Artists ("Fans Also Like") with small delay
        for (relId in relatedArtistIds.take(3)) {
            try {
                delay(150L)
                val relVars = JSONObject().apply {
                    put("uri", "spotify:artist:$relId")
                    put("locale", "")
                }
                val relRes = graphqlPost(token, "queryArtistOverview", HASH_ARTIST_OVERVIEW, relVars)
                val relUnion = relRes.optJSONObject("data")?.optJSONObject("artistUnion")
                if (relUnion != null) {
                    parseOverviewTracks(relUnion, collectRelatedIds = false)
                }
            } catch (_: Exception) {}
        }

        return out.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
    }

    // ── Meld's TOTP + sp_dc Authentication Engine ────────────────────────

    private suspend fun fetchAccessTokenViaTotp(spDc: String, spKey: String): Pair<String, Long> {
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

    private suspend fun fetchNuanceSecret(): Pair<String, Int> {
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

    private suspend fun fetchSpotifyServerTime(): Long {
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

    private suspend fun fetchUserProfile(token: String): String? {
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

    private suspend fun fetchRestTopTracks(token: String, timeRange: String, limit: Int): List<SpotifyTrackSeed> {
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
            val albumArt = t.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
            if (name.isNotBlank() && artistNames.isNotEmpty()) {
                list.add(SpotifyTrackSeed(id, name, artistNames.joinToString(", "), dur, 1.0f, albumArt))
            }
        }
        return list
    }

    private suspend fun fetchGraphQlLikedSongs(token: String, limit: Int): List<SpotifyTrackSeed> {
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
            val albumArt = data.optJSONObject("albumOfTrack")?.optJSONObject("coverArt")
                ?.optJSONArray("sources")?.optJSONObject(0)?.optString("url")
            if (name.isNotBlank() && artists.isNotEmpty()) {
                out.add(SpotifyTrackSeed(id, name, artists.joinToString(", "), dur, 1.0f, albumArt))
            }
        }
        return out
    }

    suspend fun fetchUserPlaylists(token: String): List<SpotifyPlaylistSummary> {
        val out = mutableListOf<SpotifyPlaylistSummary>()
        // 1. Prefer GraphQL libraryV3 (much less prone to 429 rate limiting than REST!)
        try {
            val vars = JSONObject().apply {
                put("filters", JSONArray().put("Playlists"))
                put("order", JSONObject.NULL)
                put("textFilter", "")
                put("features", JSONArray().put("LIKED_SONGS"))
                put("limit", 50)
                put("offset", 0)
                put("flatten", true)
                put("expandedFolders", JSONArray())
                put("folderUri", JSONObject.NULL)
                put("includeFoldersWhenFlattening", false)
            }
            val gqlRes = graphqlPost(token, "libraryV3", HASH_LIBRARY_V3, vars)
            val items = gqlRes.optJSONObject("data")
                ?.optJSONObject("me")
                ?.optJSONObject("libraryV3")
                ?.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val itemWrapper = items.getJSONObject(i).optJSONObject("item") ?: continue
                    val data = itemWrapper.optJSONObject("data") ?: continue
                    val uri = itemWrapper.optString("_uri").ifBlank { data.optString("uri") }
                    val id = uri.substringAfterLast(":")
                    val name = data.optString("name")
                    val desc = data.optString("description")
                    val img = data.optJSONObject("images")?.optJSONArray("items")
                        ?.optJSONObject(0)?.optJSONArray("sources")?.optJSONObject(0)?.optString("url")
                    val total = data.optJSONObject("content")?.optInt("totalCount", 0) ?: 0
                    if (id.isNotBlank() && name.isNotBlank()) {
                        out.add(SpotifyPlaylistSummary(id, name, desc, img, total))
                    }
                }
            }
            if (out.isNotEmpty()) {
                return out
            }
        } catch (ex: Exception) {
            Log.w(TAG, "GraphQL libraryV3 failed, falling back to REST me/playlists", ex)
        }

        // 2. Fallback to REST me/playlists if GraphQL returned empty or failed
        try {
            val res = httpGet(
                "$REST_BASE_URL/me/playlists?limit=50&offset=0",
                mapOf(
                    "Authorization" to "Bearer $token",
                    "app-platform" to "WebPlayer",
                    "Origin" to "https://open.spotify.com",
                    "Referer" to "https://open.spotify.com/"
                )
            )
            val items = JSONObject(res).optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val desc = item.optString("description")
                    val imgUrl = item.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                    val total = item.optJSONObject("tracks")?.optInt("total", 0) ?: 0
                    if (id.isNotBlank() && name.isNotBlank()) {
                        out.add(SpotifyPlaylistSummary(id, name, desc, imgUrl, total))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Both GraphQL and REST failed for user playlists", e)
        }
        return out
    }

    suspend fun fetchPlaylistTracks(token: String, playlistId: String): List<SpotifyTrackSeed> {
        val out = mutableListOf<SpotifyTrackSeed>()
        // 1. Prefer GraphQL fetchPlaylist (persisted query, resistant to 429)
        try {
            val vars = JSONObject().apply {
                put("uri", "spotify:playlist:$playlistId")
                put("offset", 0)
                put("limit", 100)
                put("enableWatchFeedEntrypoint", false)
            }
            val gqlRes = graphqlPost(token, "fetchPlaylist", HASH_FETCH_PLAYLIST, vars)
            val items = gqlRes.optJSONObject("data")
                ?.optJSONObject("playlistV2")
                ?.optJSONObject("content")
                ?.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val elem = items.getJSONObject(i)
                    val itemWrapper = elem.optJSONObject("itemV2") ?: continue
                    val data = itemWrapper.optJSONObject("data") ?: continue
                    val uri = itemWrapper.optString("_uri").ifBlank { data.optString("uri") }
                    val id = uri.substringAfterLast(":")
                    val name = data.optString("name")
                    val dur = data.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L
                    val artistItems = data.optJSONObject("artists")?.optJSONArray("items")
                    val artists = mutableListOf<String>()
                    if (artistItems != null) {
                        for (j in 0 until artistItems.length()) {
                            val a = artistItems.getJSONObject(j).optJSONObject("profile")?.optString("name")
                            if (!a.isNullOrBlank()) artists.add(a)
                        }
                    }
                    val albumArt = data.optJSONObject("albumOfTrack")?.optJSONObject("coverArt")
                        ?.optJSONArray("sources")?.optJSONObject(0)?.optString("url")
                    if (id.isNotBlank() && name.isNotBlank() && artists.isNotEmpty()) {
                        out.add(SpotifyTrackSeed(id, name, artists.joinToString(", "), dur, 1.0f, albumArt))
                    }
                }
            }
            if (out.isNotEmpty()) {
                return out
            }
        } catch (ex: Exception) {
            Log.w(TAG, "GraphQL fetchPlaylist failed for $playlistId, falling back to REST", ex)
        }

        // 2. Fallback to REST playlists/$id/tracks if GraphQL returned empty or failed
        try {
            val res = httpGet(
                "$REST_BASE_URL/playlists/$playlistId/tracks?limit=100&offset=0",
                mapOf(
                    "Authorization" to "Bearer $token",
                    "app-platform" to "WebPlayer",
                    "Origin" to "https://open.spotify.com",
                    "Referer" to "https://open.spotify.com/"
                )
            )
            val items = JSONObject(res).optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val trackObj = items.getJSONObject(i).optJSONObject("track") ?: continue
                    val id = trackObj.optString("id")
                    val name = trackObj.optString("name")
                    val dur = trackObj.optLong("duration_ms", 0L)
                    val artistsArr = trackObj.optJSONArray("artists")
                    val artistNames = mutableListOf<String>()
                    if (artistsArr != null) {
                        for (j in 0 until artistsArr.length()) {
                            val a = artistsArr.getJSONObject(j).optString("name")
                            if (a.isNotBlank()) artistNames.add(a)
                        }
                    }
                    val albumArt = trackObj.optJSONObject("album")?.optJSONArray("images")
                        ?.optJSONObject(0)?.optString("url")
                    if (id.isNotBlank() && name.isNotBlank() && artistNames.isNotEmpty()) {
                        out.add(SpotifyTrackSeed(id, name, artistNames.joinToString(", "), dur, 1.0f, albumArt))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Both GraphQL and REST failed for playlist $playlistId", e)
        }
        return out
    }

    /**
     * Syncs all user Spotify Liked Songs and Playlists into MyPlayer Room DB
     * (SongEntity, PlaylistEntity, PlaylistSongCrossReference, FavoriteEntity).
     */
    suspend fun syncAllSpotifyLibraryAndPlaylists(): Result<SpotifyFullSyncResult> = fullSyncMutex.withLock {
        withContext(Dispatchers.IO) {
        _isSyncing.value = true
        runCatching {
            val token = getValidAccessToken() ?: throw IllegalStateException("Not logged in to Spotify")
            var totalTracks = 0
            val allCollectedSeeds = mutableListOf<SpotifyTrackSeed>()

            // 1. Sync Liked Songs into MyPlayer Favorites & Library
            val likedTracks = fetchGraphQlLikedSongs(token, limit = 100)
            if (likedTracks.isNotEmpty()) {
                val songEntities = likedTracks.map { seed ->
                    val songId = "spotify:${seed.id}"
                    SongEntity(
                        id = songId,
                        title = seed.title,
                        artist = seed.artist,
                        album = "Spotify Liked Songs",
                        duration = seed.durationMs,
                        path = "online://${seed.id}",
                        albumArt = seed.artworkUrl,
                        dateAdded = System.currentTimeMillis()
                    )
                }
                songDao.insertSongs(songEntities)
                favoriteDao.insertFavorites(songEntities.map { FavoriteEntity(songId = it.id, addedAt = System.currentTimeMillis()) })
                allCollectedSeeds.addAll(likedTracks)
                totalTracks += likedTracks.size
                Log.d(TAG, "Synced ${likedTracks.size} Liked Songs to MyPlayer Favorites")
            }

            // Small delay between library liked songs and playlists fetch to avoid rate burst
            delay(150L)

            // 2. Sync User's Spotify Playlists into MyPlayer Playlists
            val playlists = fetchUserPlaylists(token)
            var playlistsSynced = 0
            for (pl in playlists) {
                // Sequential delay (150ms) between playlist fetches to prevent 429 rate limiting
                delay(150L)
                try {
                    val tracks = fetchPlaylistTracks(token, pl.id)
                    if (tracks.isEmpty()) continue

                    val existingPl = playlistDao.getPlaylistByName(pl.name)
                    val plId = existingPl?.id ?: playlistDao.insertPlaylist(PlaylistEntity(name = pl.name))

                    val trackEntities = mutableListOf<SongEntity>()
                    val crossRefs = mutableListOf<PlaylistSongCrossReference>()

                    tracks.forEachIndexed { pos, seed ->
                        val songId = "spotify:${seed.id}"
                        trackEntities.add(
                            SongEntity(
                                id = songId,
                                title = seed.title,
                                artist = seed.artist,
                                album = pl.name,
                                duration = seed.durationMs,
                                path = "online://${seed.id}",
                                albumArt = seed.artworkUrl ?: pl.imageUrl,
                                dateAdded = System.currentTimeMillis()
                            )
                        )
                        crossRefs.add(PlaylistSongCrossReference(playlistId = plId, songId = songId, position = pos))
                    }

                    songDao.insertSongs(trackEntities)
                    playlistDao.insertPlaylistSongs(crossRefs)
                    allCollectedSeeds.addAll(tracks)
                    totalTracks += tracks.size
                    playlistsSynced++
                    Log.d(TAG, "Synced playlist '${pl.name}' (${tracks.size} tracks)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync playlist '${pl.name}'", e)
                }
            }

            // 3. Train On-Device AI with all synced songs
            allCollectedSeeds.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
                .take(100)
                .forEach { track ->
                    aiModel.learnFromInteraction(
                        videoId = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationMs = track.durationMs,
                        reward = 1.8f
                    )
                }

            SpotifyFullSyncResult(
                likedSongsCount = likedTracks.size,
                playlistsCount = playlistsSynced,
                totalTracksSynced = totalTracks
            )
        }.also {
            _isSyncing.value = false
        }
        }
    }

    private suspend fun graphqlPost(
        token: String,
        operationName: String,
        sha256Hash: String,
        variables: JSONObject,
        maxRetries: Int = 3
    ): JSONObject {
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
        val postData = bodyObj.toString().toByteArray(Charsets.UTF_8)
        var attempt = 0

        while (true) {
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
                conn.outputStream.use { it.write(postData) }

                val code = conn.responseCode
                if (code == 429) {
                    val retryAfterSec = conn.getHeaderField("Retry-After")?.toLongOrNull()
                    // Cap at 15s so a huge Retry-After header can't freeze the login
                    // flow for minutes; default exponential backoff otherwise.
                    val delayMs = if (retryAfterSec != null && retryAfterSec > 0) {
                        (retryAfterSec * 1000L).coerceAtMost(15_000L)
                    } else {
                        ((attempt + 1) * 2000L)
                    }
                    Log.w(TAG, "GQL $operationName HTTP 429 Too Many Requests. Retry-After: ${retryAfterSec}s. Pausing ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)...")
                    if (++attempt <= maxRetries) {
                        delay(delayMs)
                        continue
                    }
                    throw IllegalStateException("GQL $operationName HTTP 429 rate limit exceeded after $maxRetries retries")
                }

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
    }

    private suspend fun httpGet(
        urlString: String,
        headers: Map<String, String>,
        maxRetries: Int = 3
    ): String {
        var attempt = 0

        while (true) {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("User-Agent", USER_AGENT_DESKTOP)
                conn.setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

                val code = conn.responseCode
                if (code == 429) {
                    val retryAfterSec = conn.getHeaderField("Retry-After")?.toLongOrNull()
                    // Cap at 15s so a huge Retry-After header can't freeze the login
                    // flow for minutes; default exponential backoff otherwise.
                    val delayMs = if (retryAfterSec != null && retryAfterSec > 0) {
                        (retryAfterSec * 1000L).coerceAtMost(15_000L)
                    } else {
                        ((attempt + 1) * 2000L)
                    }
                    Log.w(TAG, "HTTP GET $urlString 429 Too Many Requests. Retry-After: ${retryAfterSec}s. Pausing ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)...")
                    if (++attempt <= maxRetries) {
                        delay(delayMs)
                        continue
                    }
                    throw IllegalStateException("HTTP GET 429 rate limit exceeded after $maxRetries retries for $urlString")
                }

                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw IllegalStateException("HTTP $code: $err")
                }
                return conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun saveAccessToken(token: String, expiryMs: Long) {
        prefs.edit()
            .putString("access_token", token)
            .putLong("token_expiry_ms", expiryMs)
            .apply()
    }
}
