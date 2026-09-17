package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Sealed hierarchy of every command ARIA can issue to an ecosystem app.
 *
 * ARIA knows nothing about MyPlayer. It only constructs AppCommands.
 *
 * For backward compatibility with older legacy apps, this class implements [toJson]
 * and the companion implements [fromJson].
 */
@Parcelize
sealed class AppCommand : Parcelable {
    abstract val type: String
    abstract fun toJson(): String

    @Parcelize
    sealed class Music : AppCommand() {

        @Parcelize
        data class Play(
            val query: String,
            val queryType: QueryType = QueryType.ANY,
            val mood: String? = null,
        ) : Music() {
            override val type get() = "MUSIC_PLAY"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject()
                    .put("query", query)
                    .put("queryType", queryType.name)
                    .apply { if (mood != null) put("mood", mood) })
                .toString()

            enum class QueryType { ANY, SONG, ARTIST, ALBUM, PLAYLIST }
        }

        @Parcelize
        data object Pause : Music() {
            override val type get() = "MUSIC_PAUSE"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data object Resume : Music() {
            override val type get() = "MUSIC_RESUME"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data object Next : Music() {
            override val type get() = "MUSIC_NEXT"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data object Previous : Music() {
            override val type get() = "MUSIC_PREVIOUS"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data object Stop : Music() {
            override val type get() = "MUSIC_STOP"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data class Seek(val positionMs: Long) : Music() {
            override val type get() = "MUSIC_SEEK"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("positionMs", positionMs))
                .toString()
        }

        @Parcelize
        data class SetVolume(val percent: Int) : Music() {
            override val type get() = "MUSIC_SET_VOLUME"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("percent", percent.coerceIn(0, 100)))
                .toString()
        }

        @Parcelize
        data class SetRepeat(val mode: RepeatMode) : Music() {
            override val type get() = "MUSIC_SET_REPEAT"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("mode", mode.name))
                .toString()

            enum class RepeatMode { OFF, ONE, ALL }
        }

        @Parcelize
        data class SetShuffle(val enabled: Boolean) : Music() {
            override val type get() = "MUSIC_SET_SHUFFLE"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("enabled", enabled))
                .toString()
        }

        @Parcelize
        data object GetCurrentTrack : Music() {
            override val type get() = "MUSIC_GET_CURRENT_TRACK"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data object GetQueue : Music() {
            override val type get() = "MUSIC_GET_QUEUE"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data object GetHistory : Music() {
            override val type get() = "MUSIC_GET_HISTORY"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data class Search(val query: String) : Music() {
            override val type get() = "MUSIC_SEARCH"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("query", query))
                .toString()
        }

        @Parcelize
        data class AddToQueue(val query: String) : Music() {
            override val type get() = "MUSIC_ADD_TO_QUEUE"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("query", query))
                .toString()
        }

        @Parcelize
        data object AddToFavorites : Music() {
            override val type get() = "MUSIC_FAVORITE"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data class CreatePlaylist(val name: String) : Music() {
            override val type get() = "MUSIC_CREATE_PLAYLIST"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("name", name))
                .toString()
        }

        @Parcelize
        data class AddToPlaylist(val playlistName: String, val trackId: String = "") : Music() {
            override val type get() = "MUSIC_ADD_TO_PLAYLIST"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject()
                    .put("playlistName", playlistName)
                    .put("trackId", trackId))
                .toString()
        }

        @Parcelize
        data object GetPlaylists : Music() {
            override val type get() = "MUSIC_GET_PLAYLISTS"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data class Download(val trackId: String = "") : Music() {
            override val type get() = "MUSIC_DOWNLOAD"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("trackId", trackId))
                .toString()
        }

        @Parcelize
        data object GetLyrics : Music() {
            override val type get() = "MUSIC_GET_LYRICS"
            override fun toJson(): String = simple(type)
        }

        @Parcelize
        data class GetRecommendations(val mood: String? = null, val context: String? = null) : Music() {
            override val type get() = "MUSIC_GET_RECOMMENDATIONS"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject()
                    .putOpt("mood", mood)
                    .putOpt("context", context))
                .toString()
        }

        @Parcelize
        data class SetSleepTimer(val minutes: Int) : Music() {
            override val type get() = "MUSIC_SLEEP_TIMER"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("minutes", minutes))
                .toString()
        }

        @Parcelize
        data class SetEqualizer(val preset: String) : Music() {
            override val type get() = "MUSIC_SET_EQUALIZER"
            override fun toJson(): String = JSONObject()
                .put("type", type)
                .put("payload", JSONObject().put("preset", preset))
                .toString()
        }
    }

    companion object {
        private fun simple(type: String): String =
            JSONObject().put("type", type).put("payload", JSONObject()).toString()

        /**
         * Deserialize from JSON — used for backward compatibility with legacy apps.
         */
        fun fromJson(json: String): AppCommand? {
            return try {
                val obj = JSONObject(json)
                val type = obj.getString("type")
                val payload = obj.optJSONObject("payload") ?: JSONObject()
                when (type) {
                    "MUSIC_PLAY"              -> Music.Play(
                        query     = payload.getString("query"),
                        queryType = runCatching {
                            Music.Play.QueryType.valueOf(payload.optString("queryType", "ANY"))
                        }.getOrDefault(Music.Play.QueryType.ANY),
                        mood      = payload.optString("mood").takeIf { it.isNotEmpty() },
                    )
                    "MUSIC_PAUSE"             -> Music.Pause
                    "MUSIC_RESUME"            -> Music.Resume
                    "MUSIC_NEXT"              -> Music.Next
                    "MUSIC_PREVIOUS"          -> Music.Previous
                    "MUSIC_STOP"              -> Music.Stop
                    "MUSIC_SEEK"              -> Music.Seek(payload.getLong("positionMs"))
                    "MUSIC_SET_VOLUME"        -> Music.SetVolume(payload.getInt("percent"))
                    "MUSIC_SET_REPEAT"        -> Music.SetRepeat(
                        Music.SetRepeat.RepeatMode.valueOf(payload.getString("mode"))
                    )
                    "MUSIC_SET_SHUFFLE"       -> Music.SetShuffle(payload.getBoolean("enabled"))
                    "MUSIC_GET_CURRENT_TRACK" -> Music.GetCurrentTrack
                    "MUSIC_GET_QUEUE"         -> Music.GetQueue
                    "MUSIC_GET_HISTORY"       -> Music.GetHistory
                    "MUSIC_SEARCH"            -> Music.Search(payload.getString("query"))
                    "MUSIC_ADD_TO_QUEUE"      -> Music.AddToQueue(payload.getString("query"))
                    "MUSIC_FAVORITE"          -> Music.AddToFavorites
                    "MUSIC_CREATE_PLAYLIST"   -> Music.CreatePlaylist(payload.getString("name"))
                    "MUSIC_ADD_TO_PLAYLIST"   -> Music.AddToPlaylist(
                        playlistName = payload.getString("playlistName"),
                        trackId      = payload.optString("trackId"),
                    )
                    "MUSIC_GET_PLAYLISTS"     -> Music.GetPlaylists
                    "MUSIC_DOWNLOAD"          -> Music.Download(payload.optString("trackId"))
                    "MUSIC_GET_LYRICS"        -> Music.GetLyrics
                    "MUSIC_GET_RECOMMENDATIONS" -> Music.GetRecommendations(
                        mood    = payload.optString("mood").takeIf { it.isNotEmpty() },
                        context = payload.optString("context").takeIf { it.isNotEmpty() },
                    )
                    "MUSIC_SLEEP_TIMER"       -> Music.SetSleepTimer(payload.getInt("minutes"))
                    "MUSIC_SET_EQUALIZER"     -> Music.SetEqualizer(payload.getString("preset"))
                    else                      -> null
                }
            } catch (_: Exception) { null }
        }
    }
}
