package com.example.myplayer.sync

import org.junit.Assert.*
import org.junit.Test

class SyncMessageCodecTest {

    @Test
    fun testJoinRequestEncodeDecode() {
        val original = SyncMessage.JoinRequest(
            sessionId = "sess-1",
            senderId = "dev-1",
            sequence = 1L,
            timestamp = 1000L,
            displayName = "Pixel 8"
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.JoinRequest)
        val msg = decoded as SyncMessage.JoinRequest
        assertEquals("sess-1", msg.sessionId)
        assertEquals("dev-1", msg.senderId)
        assertEquals(1L, msg.sequence)
        assertEquals(1000L, msg.timestamp)
        assertEquals("Pixel 8", msg.displayName)
    }

    @Test
    fun testJoinAcceptedEncodeDecode() {
        val original = SyncMessage.JoinAccepted(
            sessionId = "sess-1",
            senderId = "dev-master",
            sequence = 2L,
            timestamp = 2000L
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.JoinAccepted)
        val msg = decoded as SyncMessage.JoinAccepted
        assertEquals("sess-1", msg.sessionId)
        assertEquals("dev-master", msg.senderId)
    }

    @Test
    fun testTrackPrepareEncodeDecode() {
        val original = SyncMessage.TrackPrepare(
            sessionId = "sess-1",
            senderId = "dev-master",
            sequence = 3L,
            timestamp = 3000L,
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            durationMs = 213000L,
            startPositionMs = 15000L
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.TrackPrepare)
        val msg = decoded as SyncMessage.TrackPrepare
        assertEquals("dQw4w9WgXcQ", msg.videoId)
        assertEquals("Never Gonna Give You Up", msg.title)
        assertEquals("Rick Astley", msg.artist)
        assertEquals(213000L, msg.durationMs)
        assertEquals(15000L, msg.startPositionMs)
        assertNull(msg.albumArt)
    }

    @Test
    fun testTrackPrepareWithAlbumArtEncodeDecode() {
        val original = SyncMessage.TrackPrepare(
            sessionId = "sess-1",
            senderId = "dev-master",
            sequence = 3L,
            timestamp = 3000L,
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            durationMs = 213000L,
            startPositionMs = 15000L,
            albumArt = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.TrackPrepare)
        val msg = decoded as SyncMessage.TrackPrepare
        assertEquals("dQw4w9WgXcQ", msg.videoId)
        assertEquals("Never Gonna Give You Up", msg.title)
        assertEquals("Rick Astley", msg.artist)
        assertEquals(213000L, msg.durationMs)
        assertEquals(15000L, msg.startPositionMs)
        assertEquals("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", msg.albumArt)
    }

    @Test
    fun testPlayAtEncodeDecode() {
        val original = SyncMessage.PlayAt(
            sessionId = "sess-1",
            senderId = "dev-master",
            sequence = 4L,
            timestamp = 4000L,
            startDelayMs = 2500L,
            positionMs = 15000L
        )
        val encoded = SyncMessageCodec.encode(original)
        val decoded = SyncMessageCodec.decode(encoded)

        assertTrue(decoded is SyncMessage.PlayAt)
        val msg = decoded as SyncMessage.PlayAt
        assertEquals(2500L, msg.startDelayMs)
        assertEquals(15000L, msg.positionMs)
    }

    @Test
    fun testPauseAndSeekEncodeDecode() {
        val pause = SyncMessage.Pause(
            sessionId = "sess-1",
            senderId = "dev-master",
            sequence = 5L,
            timestamp = 5000L,
            positionMs = 20000L
        )
        val decodedPause = SyncMessageCodec.decode(SyncMessageCodec.encode(pause))
        assertTrue(decodedPause is SyncMessage.Pause)
        assertEquals(20000L, (decodedPause as SyncMessage.Pause).positionMs)

        val seek = SyncMessage.Seek(
            sessionId = "sess-1",
            senderId = "dev-master",
            sequence = 6L,
            timestamp = 6000L,
            positionMs = 45000L
        )
        val decodedSeek = SyncMessageCodec.decode(SyncMessageCodec.encode(seek))
        assertTrue(decodedSeek is SyncMessage.Seek)
        assertEquals(45000L, (decodedSeek as SyncMessage.Seek).positionMs)
    }

    @Test
    fun testMalformedDecodeReturnsNull() {
        assertNull(SyncMessageCodec.decode("not a valid json"))
        assertNull(SyncMessageCodec.decode("{}"))
        assertNull(SyncMessageCodec.decode("""{"sessionId":"s","type":"UNKNOWN"}"""))
    }
}
