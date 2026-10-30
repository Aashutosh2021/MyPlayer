package com.myplayer.app.data.artist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ArtistInfo(val name: String, val imageUrl: String?)

/** Real artists (name + photo) from Deezer's public search API, so typed names are valid and have a picture. */
@Singleton
class ArtistSearchRepository @Inject constructor(private val client: OkHttpClient) {

    private val photoCache = ConcurrentHashMap<String, String>()
    private val noPhoto = ConcurrentHashMap.newKeySet<String>()

    /** null = network/parse error (offline), empty = no such artist. */
    suspend fun search(query: String, limit: Int = 12): List<ArtistInfo>? = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        try {
            val url = "https://api.deezer.com/search/artist".toHttpUrl().newBuilder()
                .addQueryParameter("q", q)
                .addQueryParameter("limit", limit.toString())
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val data = JSONObject(resp.body?.string() ?: return@use null).optJSONArray("data") ?: return@use emptyList()
                (0 until data.length()).mapNotNull { i ->
                    val o = data.getJSONObject(i)
                    val name = o.optString("name").trim()
                    if (name.isEmpty()) return@mapNotNull null
                    // Deezer's "no picture" placeholder has an empty hash in the URL
                    val pic = o.optString("picture_big").ifBlank { o.optString("picture_medium") }
                        .takeIf { it.isNotBlank() && !it.contains("/artist//") }
                    ArtistInfo(name, pic).also { info -> pic?.let { photoCache[key(name)] = it } }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Photo for an artist name (best match), cached. */
    suspend fun photoFor(name: String): String? {
        val k = key(name)
        photoCache[k]?.let { return it }
        if (k in noPhoto) return null
        val hits = search(name, 5) ?: return null
        val best = hits.firstOrNull { key(it.name) == k } ?: hits.firstOrNull()
        val url = best?.imageUrl
        if (url != null) photoCache[k] = url else noPhoto += k
        return url
    }

    private fun key(name: String) = name.lowercase().filter { it.isLetterOrDigit() }
}
