package com.example.myplayer.aria.providers

import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.playback.MusicController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistProvider @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicController: MusicController,
    private val performanceMonitor: AriaPerformanceMonitor
) {
    suspend fun playPlaylist(playlistId: Long, startIndex: Int, commandOrdinal: Int, transactionId: String?): PlaylistResponse = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        val playlistSongs = withContext(Dispatchers.IO) {
            musicRepository.getSongsInPlaylistAsPlayable(playlistId).firstOrNull()
        }
        
        if (playlistSongs.isNullOrEmpty()) {
            return@withContext PlaylistResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId)
        }

        musicController.playPlaylist(playlistSongs, startIndex)
        performanceMonitor.recordLatency("playlist_ops", System.currentTimeMillis() - startTime)

        val songInfoList = playlistSongs.map { song ->
            val isOnline = song.id.startsWith("online://")
            SongInfo(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = if (isOnline) "YouTube Music" else "Downloads",
                durationMs = song.durationMs,
                path = if (isOnline) song.id else "",
                albumArtUrl = song.thumbnailUrl,
                isOnline = isOnline,
                playCount = 0
            )
        }

        return@withContext PlaylistResponse(
            status = AriaStatus.SUCCESS,
            commandOrdinal = commandOrdinal,
            transactionId = transactionId,
            playlistId = playlistId,
            songs = songInfoList
        )
    }

    suspend fun createPlaylist(name: String, commandOrdinal: Int, transactionId: String?): PlaylistResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (name.isBlank()) {
            return@withContext PlaylistResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId)
        }

        val all = musicRepository.getAllPlaylists().first()
        // Task 6: Request Validation (Playlist already exists)
        if (all.any { it.name.equals(name, ignoreCase = true) }) {
            return@withContext PlaylistResponse(AriaStatus.ALREADY_EXISTS, commandOrdinal, transactionId)
        }

        try {
            val playlistId = musicRepository.createPlaylist(name)
            performanceMonitor.recordLatency("playlist_ops", System.currentTimeMillis() - startTime)
            return@withContext PlaylistResponse(
                status = AriaStatus.SUCCESS,
                commandOrdinal = commandOrdinal,
                transactionId = transactionId,
                playlistId = playlistId,
                playlistName = name
            )
        } catch (e: Exception) {
            return@withContext PlaylistResponse(AriaStatus.FAILURE, commandOrdinal, transactionId)
        }
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (newName.isBlank()) return@withContext AriaGeneralResponse(AriaStatus.BAD_REQUEST, commandOrdinal, transactionId)

        val all = musicRepository.getAllPlaylists().first()
        val playlist = all.firstOrNull { it.id == playlistId } ?: return@withContext AriaGeneralResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId)

        // Task 6: Request Validation (Playlist name conflict)
        if (all.any { it.name.equals(newName, ignoreCase = true) && it.id != playlistId }) {
            return@withContext AriaGeneralResponse(AriaStatus.ALREADY_EXISTS, commandOrdinal, transactionId, "Another playlist with the name '$newName' already exists.")
        }

        try {
            musicRepository.renamePlaylist(playlistId, newName)
            performanceMonitor.recordLatency("playlist_ops", System.currentTimeMillis() - startTime)
            return@withContext AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        } catch (e: Exception) {
            return@withContext AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId)
        }
    }

    suspend fun deletePlaylist(playlistId: Long, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val all = musicRepository.getAllPlaylists().first()
        val playlist = all.firstOrNull { it.id == playlistId } ?: return@withContext AriaGeneralResponse(AriaStatus.NOT_FOUND, commandOrdinal, transactionId)

        try {
            musicRepository.deletePlaylist(playlist)
            performanceMonitor.recordLatency("playlist_ops", System.currentTimeMillis() - startTime)
            return@withContext AriaGeneralResponse(AriaStatus.SUCCESS, commandOrdinal, transactionId)
        } catch (e: Exception) {
            return@withContext AriaGeneralResponse(AriaStatus.FAILURE, commandOrdinal, transactionId)
        }
    }
}
