package com.example.myplayer.aria.recovery

import android.content.Context
import android.util.Log
import com.example.myplayer.aria.cache.AriaMemoryCache
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AriaStateRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryCache: AriaMemoryCache
) {
    companion object {
        private const val PREFS_NAME = "aria_recovery_settings"
        private const val KEY_QUEUE_IDS = "recovery_queue_ids"
        private const val KEY_CURRENT_SONG_ID = "recovery_current_song_id"
        private const val KEY_POSITION_MS = "recovery_position_ms"
        private const val KEY_REPEAT_MODE = "recovery_repeat_mode"
        private const val KEY_SHUFFLE_MODE = "recovery_shuffle_mode"
        private const val TAG = "AriaStateRecovery"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveState(
        queueIds: List<String>,
        currentSongId: String?,
        positionMs: Long,
        repeatMode: Int,
        shuffleMode: Boolean
    ) {
        prefs.edit().apply {
            putString(KEY_QUEUE_IDS, JSONArray(queueIds).toString())
            putString(KEY_CURRENT_SONG_ID, currentSongId)
            putLong(KEY_POSITION_MS, positionMs)
            putInt(KEY_REPEAT_MODE, repeatMode)
            putBoolean(KEY_SHUFFLE_MODE, shuffleMode)
            apply()
        }
    }

    suspend fun restoreState(musicController: MusicController) = withContext(Dispatchers.Main) {
        try {
            val queueStr = prefs.getString(KEY_QUEUE_IDS, null) ?: return@withContext
            val currentSongId = prefs.getString(KEY_CURRENT_SONG_ID, null)
            val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
            val repeatMode = prefs.getInt(KEY_REPEAT_MODE, 0)
            val shuffleMode = prefs.getBoolean(KEY_SHUFFLE_MODE, false)

            val jsonArray = JSONArray(queueStr)
            if (jsonArray.length() == 0) return@withContext

            val songIds = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                songIds.add(jsonArray.getString(i))
            }

            Log.d(TAG, "Restoring playback state with ${songIds.size} songs. Target song: $currentSongId at position: ${positionMs}ms")

            // Reconstruct SongEntities from Memory Cache (instant, thread-safe, 0ms latency)
            val songsToRestore = mutableListOf<SongEntity>()
            for (id in songIds) {
                // Check downloaded cache first
                val downloaded = memoryCache.getDownloadedSong(id)
                if (downloaded != null) {
                    songsToRestore.add(
                        SongEntity(
                            id = downloaded.id,
                            title = downloaded.title,
                            artist = downloaded.artist,
                            album = downloaded.album.ifBlank { "Downloads" },
                            duration = downloaded.durationMs,
                            path = downloaded.localPath,
                            albumArt = downloaded.thumbnailUrl,
                            dateAdded = downloaded.downloadedAt,
                            videoId = downloaded.id
                        )
                    )
                    continue
                }
                
                // Fallback to local songs cache
                val local = memoryCache.getLocalSong(id)
                if (local != null) {
                    songsToRestore.add(local)
                }
            }

            if (songsToRestore.isNotEmpty()) {
                val startIndex = songIds.indexOf(currentSongId).coerceAtLeast(0)
                
                // Build the queue in MusicController and immediately pause/seek to avoid blasting audio
                musicController.playSongs(songsToRestore, startIndex)
                
                val controller = musicController.getMediaController()
                if (controller != null) {
                    controller.pause()
                    controller.seekTo(startIndex, positionMs)
                    controller.repeatMode = repeatMode
                    controller.shuffleModeEnabled = shuffleMode
                    Log.i(TAG, "Successfully restored queue state from memory cache.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore state", e)
        }
    }
}
