package com.example.myplayer.sync

import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncTrackMatcherTest {

    private val localSongs = mutableListOf<SongEntity>()

    private val fakeSongDao = object : SongDao {
        override fun getAllSongs(): Flow<List<SongEntity>> = flowOf(localSongs)
        override fun getTrendingSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override fun getSongById(id: String): SongEntity? = localSongs.find { it.id == id }
        override fun insertSongs(songs: List<SongEntity>) { localSongs.addAll(songs) }
        override fun deleteSongsByFolder(folderUri: String) {}
        override fun getRecentlyAddedSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override fun getMostPlayedSongs(): Flow<List<SongEntity>> = flowOf(emptyList())
        override fun incrementPlayCount(id: String) {}
        override fun updateSongPath(id: String, path: String) {}
        override fun searchSongs(query: String): Flow<List<SongEntity>> = flowOf(emptyList())
        override suspend fun getAllSongsSync(): List<SongEntity> = localSongs.toList()
        override suspend fun getSongByVideoId(videoId: String): SongEntity? = localSongs.find { it.videoId == videoId }
    }

    private lateinit var matcher: SyncTrackMatcher

    @Before
    fun setUp() {
        localSongs.clear()
        matcher = SyncTrackMatcher(fakeSongDao)
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

        val target = SyncTrackRef(
            videoId = "vid-123",
            title = "Starboy (Official)",
            artist = "Weeknd",
            durationMs = 230000L
        )

        val match = matcher.findLocalMatch(target)
        assertNotNull(match)
        assertEquals("local-1", match?.id)
    }

    @Test
    fun testMatchByMetadataWithinTolerance() = runBlocking {
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

        val target = SyncTrackRef(
            videoId = null,
            title = "blinding lights",
            artist = "the weeknd",
            durationMs = 201500L // 1000ms difference, within 2000ms tolerance
        )

        val match = matcher.findLocalMatch(target)
        assertNotNull(match)
        assertEquals("local-2", match?.id)
    }

    @Test
    fun testNoMatchOutsideTolerance() = runBlocking {
        localSongs.add(
            SongEntity(
                id = "local-3",
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                duration = 200000L,
                path = "/music/blinding.mp3",
                albumArt = null,
                dateAdded = 200L
            )
        )

        val target = SyncTrackRef(
            videoId = null,
            title = "Blinding Lights",
            artist = "The Weeknd",
            durationMs = 210000L // 10000ms difference, exceeds tolerance
        )

        val match = matcher.findLocalMatch(target)
        assertNull(match)
    }
}
