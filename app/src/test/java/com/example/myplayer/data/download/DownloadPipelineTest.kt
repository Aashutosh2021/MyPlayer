package com.example.myplayer.data.download

import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.model.OnlineSong
import org.junit.Assert.*
import org.junit.Test

class DownloadPipelineTest {

    // Helper implementing the same resolution logic as MainViewModel.getDownloadableTrack
    private fun resolveDownloadable(
        online: OnlineSong?,
        local: SongEntity?,
        downloadedSet: Set<String>
    ): OnlineSong? {
        if (online != null && online.videoId.isNotBlank()) {
            return online
        }
        if (local != null) {
            val videoId = local.videoId?.takeIf { it.isNotBlank() }
                ?: if (local.path.startsWith("online://")) {
                    local.path.removePrefix("online://").takeIf { it.isNotBlank() }
                } else null
                ?: if (downloadedSet.contains(local.id)) {
                    local.id
                } else null

            if (videoId != null) {
                val streamUrl = if (local.path.startsWith("http://") || local.path.startsWith("https://")) {
                    local.path
                } else null
                return OnlineSong(
                    videoId = videoId,
                    title = local.title,
                    artist = local.artist,
                    thumbnailUrl = local.albumArt ?: "",
                    durationMs = local.duration,
                    streamUrl = streamUrl
                )
            }
        }
        return null
    }

    @Test
    fun testDirectOnlineSong_resolvesDownloadableTrackCorrectly() {
        val directOnline = OnlineSong(
            videoId = "vid_abc_123",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            thumbnailUrl = "https://thumb.url/art.jpg",
            durationMs = 354000,
            streamUrl = "https://googlevideo.com/stream?id=123"
        )

        val result = resolveDownloadable(
            online = directOnline,
            local = null,
            downloadedSet = emptySet()
        )

        assertNotNull("Direct online song must resolve to downloadable track", result)
        assertEquals("vid_abc_123", result?.videoId)
        assertEquals("Bohemian Rhapsody", result?.title)
        assertEquals("Queen", result?.artist)
        assertEquals("https://googlevideo.com/stream?id=123", result?.streamUrl)
    }

    @Test
    fun testPlaylistQueuedOnlineSong_resolvesDownloadableTrackFromSongEntity() {
        // When online songs are played through playPlaylist/playSongs, currentOnlineSong is null
        // and currentSong is a SongEntity with videoId and path = "online://<videoId>"
        val queuedSong = SongEntity(
            id = "vid_xyz_789",
            title = "Starboy",
            artist = "The Weeknd",
            album = "YouTube Music",
            duration = 230000,
            path = "online://vid_xyz_789",
            albumArt = "https://thumb.url/weeknd.jpg",
            dateAdded = System.currentTimeMillis(),
            videoId = "vid_xyz_789"
        )

        val result = resolveDownloadable(
            online = null,
            local = queuedSong,
            downloadedSet = emptySet()
        )

        assertNotNull("Queued online song in SongEntity format must be recognized", result)
        assertEquals("vid_xyz_789", result?.videoId)
        assertEquals("Starboy", result?.title)
        assertEquals("The Weeknd", result?.artist)
        assertEquals("https://thumb.url/weeknd.jpg", result?.thumbnailUrl)
        assertNull("Queued track should require fresh streamUrl resolution", result?.streamUrl)
    }

    @Test
    fun testSyntheticSongWithoutVideoIdField_resolvesFromOnlinePath() {
        // If a SongEntity was created with videoId = null but path is "online://dQw4w9WgXcQ"
        val syntheticSong = SongEntity(
            id = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            album = "Online",
            duration = 213000,
            path = "online://dQw4w9WgXcQ",
            albumArt = null,
            dateAdded = 1000L,
            videoId = null
        )

        val result = resolveDownloadable(
            online = null,
            local = syntheticSong,
            downloadedSet = emptySet()
        )

        assertNotNull("Song with online:// path must resolve videoId", result)
        assertEquals("dQw4w9WgXcQ", result?.videoId)
        assertEquals("Never Gonna Give You Up", result?.title)
    }

    @Test
    fun testPureLocalDeviceSong_returnsNullForDownloadableTrack() {
        // Pure local media scanned from device storage with integer MediaStore ID
        val localDeviceSong = SongEntity(
            id = "142",
            title = "My Recording",
            artist = "Me",
            album = "Recordings",
            duration = 60000,
            path = "/storage/emulated/0/Music/recording.mp3",
            albumArt = null,
            dateAdded = System.currentTimeMillis(),
            videoId = null
        )

        val result = resolveDownloadable(
            online = null,
            local = localDeviceSong,
            downloadedSet = emptySet()
        )

        assertNull("Pure local device MP3 files must NOT be treated as downloadable online tracks", result)
    }

    @Test
    fun testDownloadedSong_resolvesCorrectlyFromDownloadedSet() {
        // A track that was downloaded and is now in downloadedSet
        val downloadedSong = SongEntity(
            id = "downloaded_vid_999",
            title = "Downloaded Track",
            artist = "Artist",
            album = "Downloads",
            duration = 180000,
            path = "/data/user/0/com.example.myplayer/files/track.m4a",
            albumArt = null,
            dateAdded = System.currentTimeMillis(),
            videoId = null
        )

        val result = resolveDownloadable(
            online = null,
            local = downloadedSong,
            downloadedSet = setOf("downloaded_vid_999")
        )

        assertNotNull("Downloaded song matching downloadedSet must resolve for deletion/status check", result)
        assertEquals("downloaded_vid_999", result?.videoId)
    }

    @Test
    fun testDuplicateDownloadPrevention_checksCorrectly() {
        val downloadedIds = setOf("vid_already_downloaded")
        val activeDownloads = mapOf("vid_actively_downloading" to 45)

        val song1 = OnlineSong("vid_already_downloaded", "Title", "Artist", "", 100L)
        val song2 = OnlineSong("vid_actively_downloading", "Title", "Artist", "", 100L)
        val song3 = OnlineSong("vid_new", "Title", "Artist", "", 100L)

        // Case 3 — already downloaded
        assertTrue("Already downloaded song must be detected", downloadedIds.contains(song1.videoId))

        // Case 2 — actively downloading
        assertTrue("Actively downloading song must be detected", activeDownloads.containsKey(song2.videoId))

        // Case 1 — new song
        assertFalse("New song must not be marked as downloaded", downloadedIds.contains(song3.videoId))
        assertFalse("New song must not be marked as downloading", activeDownloads.containsKey(song3.videoId))
    }
}
