package com.example.myplayer.sync

import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.sync.model.SyncTrack
import com.example.myplayer.sync.track.SyncTrackMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncTrackMatcherTest {

    private val localSongs = mutableListOf<SongEntity>()
    private val downloadedSongs = mutableListOf<DownloadedSongEntity>()

    private val fakeSongDao = object : SongDao {
        override fun getAllSongs(): Flow<List<SongEntity>> = emptyFlow()
        override fun getTrendingSongs(): Flow<List<SongEntity>> = emptyFlow()
        override fun getSongById(id: String): SongEntity? = localSongs.find { it.id == id }
        override fun insertSongs(songs: List<SongEntity>) { localSongs.addAll(songs) }
        override fun deleteSongsByFolder(folderUri: String) {}
        override fun getRecentlyAddedSongs(): Flow<List<SongEntity>> = emptyFlow()
        override fun getMostPlayedSongs(): Flow<List<SongEntity>> = emptyFlow()
        override fun incrementPlayCount(id: String) {}
        override fun updateSongPath(id: String, path: String) {}
        override fun searchSongs(query: String): Flow<List<SongEntity>> = emptyFlow()
        override suspend fun getAllSongsSync(): List<SongEntity> = localSongs.toList()
        override suspend fun getSongByVideoId(videoId: String): SongEntity? = localSongs.find { it.videoId == videoId }
    }

    private val fakeDownloadedSongDao = object : DownloadedSongDao {
        override fun getAllDownloads(): Flow<List<DownloadedSongEntity>> = emptyFlow()
        override fun getAllDownloadsSync(): List<DownloadedSongEntity> = downloadedSongs.toList()
        override fun insertDownloads(downloads: List<DownloadedSongEntity>) { downloadedSongs.addAll(downloads) }
        override suspend fun getById(id: String): DownloadedSongEntity? = downloadedSongs.find { it.id == id }
        override suspend fun existsById(id: String): Boolean = downloadedSongs.any { it.id == id }
        override suspend fun insert(song: DownloadedSongEntity) { downloadedSongs.add(song) }
        override suspend fun deleteById(id: String) { downloadedSongs.removeIf { it.id == id } }
        override fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>> = emptyFlow()
    }

    private lateinit var matcher: SyncTrackMatcher

    @Before
    fun setUp() {
        localSongs.clear()
        downloadedSongs.clear()
        matcher = SyncTrackMatcher(fakeSongDao, fakeDownloadedSongDao)
    }

    @Test
    fun testMatchByVideoId() = runBlocking {
        localSongs.add(
            SongEntity(
                id = "local-1",
                title = "Starboy",
                artist = "The Weeknd",
                album = "Starboy",
                duration = 230000L,
                path = "/storage/song1.mp3",
                albumArt = null,
                dateAdded = 100L,
                videoId = "vid-123"
            )
        )

        val target = SyncTrack(
            title = "Starboy (Official)",
            artist = "Weeknd",
            album = "Starboy",
            durationMs = 230000L,
            videoId = "vid-123"
        )

        val match = matcher.findMatchingSong(target)
        assertNotNull(match)
        assertTrue(match is PlayableSong.Local)
        assertEquals("local-1", (match as PlayableSong.Local).entity.id)
    }

    @Test
    fun testMatchByExactTitleAndArtistWithinDuration() = runBlocking {
        localSongs.add(
            SongEntity(
                id = "local-2",
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                duration = 200500L,
                path = "/music/blinding.mp3",
                albumArt = null,
                dateAdded = 200L
            )
        )

        val target = SyncTrack(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 200000L // 500ms difference within 4000ms tolerance
        )

        val match = matcher.findMatchingSong(target)
        assertNotNull(match)
        assertTrue(match is PlayableSong.Local)
        assertEquals("Blinding Lights", (match as PlayableSong.Local).entity.title)
    }

    @Test
    fun testMatchFailsWhenSongNotPresent() = runBlocking {
        localSongs.add(
            SongEntity(
                id = "local-3",
                title = "Different Song",
                artist = "Artist A",
                album = "Album",
                duration = 180000L,
                path = "/music/diff.mp3",
                albumArt = null,
                dateAdded = 300L
            )
        )

        val target = SyncTrack(
            title = "Nonexistent Song",
            artist = "Artist B",
            album = "None",
            durationMs = 210000L
        )

        val match = matcher.findMatchingSong(target)
        assertNull(match)
    }

    @Test
    fun testMatchFailsWhenDurationDiffersTooMuch() = runBlocking {
        localSongs.add(
            SongEntity(
                id = "local-4",
                title = "Hello",
                artist = "Adele",
                album = "25",
                duration = 295000L,
                path = "/music/hello.mp3",
                albumArt = null,
                dateAdded = 400L
            )
        )

        // Same title and artist, but completely different duration (e.g. preview or live cut)
        val target = SyncTrack(
            title = "Hello",
            artist = "Adele",
            album = "25",
            durationMs = 120000L // 175s difference
        )

        val match = matcher.findMatchingSong(target)
        assertNull(match)
    }
}
