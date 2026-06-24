package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a song that can be played regardless of source.
 */
sealed class PlayableSong {
    abstract val id: String
    abstract val title: String
    abstract val artist: String
    abstract val durationMs: Long
    abstract val thumbnailUrl: String?

    data class Local(val entity: SongEntity) : PlayableSong() {
        override val id = entity.id
        override val title = entity.title
        override val artist = entity.artist
        override val durationMs = entity.duration
        override val thumbnailUrl = entity.albumArt
    }

    data class Downloaded(val entity: DownloadedSongEntity) : PlayableSong() {
        override val id = entity.id
        override val title = entity.title
        override val artist = entity.artist
        override val durationMs = entity.durationMs
        override val thumbnailUrl = entity.thumbnailUrl
    }

    data class Online(
        override val id: String,
        override val title: String,
        override val artist: String,
        override val durationMs: Long,
        override val thumbnailUrl: String?
    ) : PlayableSong()
}

@Singleton
class HybridLibraryRepository @Inject constructor(
    private val songDao: SongDao,
    private val downloadedSongDao: DownloadedSongDao
) {
    /**
     * Returns a combined flow of local device songs and downloaded online songs.
     * Deduplicated by videoId — a downloaded song takes precedence over a local song
     * if they somehow share the same ID (which they wouldn't normally).
     */
    fun getHybridLibrary(): Flow<List<PlayableSong>> {
        return combine(
            songDao.getAllSongs(),
            downloadedSongDao.getAllDownloads()
        ) { localSongs, downloadedSongs ->
            val list = mutableListOf<PlayableSong>()

            // Prioritize downloaded songs so they appear as SAVED
            val downloadedIds = downloadedSongs.map { it.id }.toSet()
            
            localSongs
                .filter { it.id !in downloadedIds }
                .mapTo(list) { PlayableSong.Local(it) }

            downloadedSongs
                .mapTo(list) { PlayableSong.Downloaded(it) }

            list.sortedBy { it.title }
        }
    }

    /**
     * Search across both local and downloaded songs.
     */
    fun searchHybridLibrary(query: String): Flow<List<PlayableSong>> {
        return combine(
            songDao.searchSongs(query),
            downloadedSongDao.searchDownloads(query)
        ) { localSongs, downloadedSongs ->
            val list = mutableListOf<PlayableSong>()
            localSongs.mapTo(list) { PlayableSong.Local(it) }
            downloadedSongs.mapTo(list) { PlayableSong.Downloaded(it) }
            list.sortedBy { it.title }
        }
    }
}
