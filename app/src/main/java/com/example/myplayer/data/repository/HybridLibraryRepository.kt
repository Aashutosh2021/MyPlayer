package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.model.MusicItem
import com.example.myplayer.data.model.toMusicItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a song that can be played regardless of source.
 * Kept for backwards-compatibility with existing ViewModels and UI screens.
 * New code should use [MusicItem] via [getHybridLibraryAsItems] instead.
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
    private val downloadedSongDao: DownloadedSongDao,
    private val songDao: SongDao? = null
) {
    /**
     * Returns a flow of downloaded online songs.
     * Pure local scanned files have been removed — only music saved from the downloaded section is kept.
     */
    fun getHybridLibrary(): Flow<List<PlayableSong>> {
        return downloadedSongDao.getAllDownloads().map { downloadedSongs ->
            downloadedSongs.map { PlayableSong.Downloaded(it) }.sortedBy { it.title }
        }
    }

    /**
     * Returns a flow using the canonical [MusicItem] model from downloaded songs.
     */
    fun getHybridLibraryAsItems(): Flow<List<MusicItem>> {
        return downloadedSongDao.getAllDownloads().map { downloadedSongs ->
            downloadedSongs.map { it.toMusicItem() }.sortedBy { it.title }
        }
    }

    /**
     * Search across downloaded songs.
     */
    fun searchHybridLibrary(query: String): Flow<List<PlayableSong>> {
        return downloadedSongDao.searchDownloads(query).map { downloadedSongs ->
            downloadedSongs.map { PlayableSong.Downloaded(it) }.sortedBy { it.title }
        }
    }

    /**
     * Search across downloaded songs, returning canonical [MusicItem].
     */
    fun searchHybridLibraryAsItems(query: String): Flow<List<MusicItem>> {
        return downloadedSongDao.searchDownloads(query).map { downloadedSongs ->
            downloadedSongs.map { it.toMusicItem() }.sortedBy { it.title }
        }
    }
}
