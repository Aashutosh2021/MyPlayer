package com.example.myplayer.sync

import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SyncPlayBarrierTest {

    @Test
    fun testTrackDownloadProgressCodecRoundtrip() {
        val original = SyncMessage.TrackDownloadProgress(
            sessionId = "sess-123",
            senderId = "slave-1",
            sequence = 7L,
            timestamp = 7000L,
            progress = 65
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.TrackDownloadProgress)
        val msg = decoded as SyncMessage.TrackDownloadProgress
        assertEquals("sess-123", msg.sessionId)
        assertEquals("slave-1", msg.senderId)
        assertEquals(7L, msg.sequence)
        assertEquals(7000L, msg.timestamp)
        assertEquals(65, msg.progress)
    }

    @Test
    fun testTrackPrepareFailedCodecRoundtrip() {
        val original = SyncMessage.TrackPrepareFailed(
            sessionId = "sess-123",
            senderId = "slave-2",
            sequence = 8L,
            timestamp = 8000L,
            reason = "Network stream resolution failed"
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.TrackPrepareFailed)
        val msg = decoded as SyncMessage.TrackPrepareFailed
        assertEquals("sess-123", msg.sessionId)
        assertEquals("slave-2", msg.senderId)
        assertEquals(8L, msg.sequence)
        assertEquals("Network stream resolution failed", msg.reason)
    }

    @Test
    fun testReadyAckCodecRoundtrip() {
        val original = SyncMessage.Ready(
            sessionId = "sess-123",
            senderId = "slave-1",
            sequence = 9L,
            timestamp = 9000L,
            trackAvailable = true
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.Ready)
        val msg = decoded as SyncMessage.Ready
        assertEquals("sess-123", msg.sessionId)
        assertEquals(true, msg.trackAvailable)
    }

    @Test
    fun testKickCodecRoundtrip() {
        val original = SyncMessage.Kick(
            sessionId = "sess-123",
            senderId = "master-host",
            sequence = 10L,
            timestamp = 10000L,
            targetDeviceId = "slave-to-remove"
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.Kick)
        val msg = decoded as SyncMessage.Kick
        assertEquals("sess-123", msg.sessionId)
        assertEquals("master-host", msg.senderId)
        assertEquals("slave-to-remove", msg.targetDeviceId)
    }

    @Test
    fun testLocalTrackPreparationWithVerifiedFile() = runBlocking {
        // Create an actual temporary file to test real file verification
        val tempFile = File.createTempFile("test_sync_song", ".mp3").apply {
            writeBytes(ByteArray(1024) { 1 }) // non-empty
            deleteOnExit()
        }

        val localSongs = mutableListOf(
            SongEntity(
                id = "song-local-1",
                title = "Sunflower",
                artist = "Post Malone",
                album = "Spider-Man",
                duration = 158000L,
                path = tempFile.absolutePath,
                albumArt = null,
                dateAdded = 1000L,
                videoId = "sunflower_vid"
            )
        )

        val fakeSongDao = object : SongDao {
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

        val fakeDownloadedDao = object : DownloadedSongDao {
            override fun getAllDownloads(): Flow<List<DownloadedSongEntity>> = flowOf(emptyList())
            override fun getAllDownloadsSync(): List<DownloadedSongEntity> = emptyList()
            override fun insertDownloads(downloads: List<DownloadedSongEntity>) {}
            override suspend fun getById(id: String): DownloadedSongEntity? = null
            override suspend fun existsById(id: String): Boolean = false
            override suspend fun insert(song: DownloadedSongEntity) {}
            override suspend fun deleteById(id: String) {}
            override fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>> = flowOf(emptyList())
        }

        val matcher = SyncTrackMatcher(fakeSongDao, fakeDownloadedDao)

        // 1. Existing local file with valid disk size
        val refExisting = SyncTrackRef(
            videoId = "sunflower_vid",
            title = "Sunflower",
            artist = "Post Malone",
            durationMs = 158000L
        )
        val matchFound = matcher.findLocalFileMatch(refExisting)
        assertNotNull("Expected verified local file match", matchFound)
        assertEquals("song-local-1", matchFound?.id)

        // 2. Missing track on local disk
        val refMissing = SyncTrackRef(
            videoId = "missing_vid_456",
            title = "Unknown Song",
            artist = "Unknown Artist",
            durationMs = 200000L
        )
        val matchMissing = matcher.findLocalFileMatch(refMissing)
        assertNull("Missing track must return null to trigger download pipeline", matchMissing)
    }

    @Test
    fun testMultiDeviceBarrierStateLogic() {
        val connectedDevices = mutableMapOf<String, SyncDevice>()

        // 3 Slaves connect to Master
        connectedDevices["slave-A"] = SyncDevice("slave-A", "Pixel 8", status = DeviceSyncStatus.PREPARING)
        connectedDevices["slave-B"] = SyncDevice("slave-B", "Galaxy Tab", status = DeviceSyncStatus.PREPARING)
        connectedDevices["slave-C"] = SyncDevice("slave-C", "OnePlus", status = DeviceSyncStatus.PREPARING)

        fun isBarrierSatisfied(): Boolean {
            return connectedDevices.values.all { it.status == DeviceSyncStatus.READY }
        }

        // Initially no device is ready
        assertFalse(isBarrierSatisfied())

        // Slave A has local file and reports READY
        connectedDevices["slave-A"] = connectedDevices["slave-A"]!!.copy(status = DeviceSyncStatus.READY)
        assertFalse("Barrier must not start while Slave B and C are preparing", isBarrierSatisfied())

        // Slave B is downloading
        connectedDevices["slave-B"] = connectedDevices["slave-B"]!!.copy(
            status = DeviceSyncStatus.DOWNLOADING,
            downloadProgress = 60
        )
        assertFalse("Barrier must wait while Slave B is downloading", isBarrierSatisfied())

        // Slave C reports READY
        connectedDevices["slave-C"] = connectedDevices["slave-C"]!!.copy(status = DeviceSyncStatus.READY)
        assertFalse("Barrier must still wait for Slave B", isBarrierSatisfied())

        // Slave B completes download & prepares ExoPlayer -> reports READY
        connectedDevices["slave-B"] = connectedDevices["slave-B"]!!.copy(
            status = DeviceSyncStatus.READY,
            downloadProgress = 100
        )

        // Now ALL devices are READY!
        assertTrue("Barrier satisfied: All connected slaves are READY", isBarrierSatisfied())
    }
}
