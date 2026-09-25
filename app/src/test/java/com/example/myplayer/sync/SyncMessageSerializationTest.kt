package com.example.myplayer.sync

import com.example.myplayer.sync.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SyncMessageSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun testHelloSerialization() {
        val original: SyncMessage = SyncMessage.Hello(
            sequence = 1,
            senderDeviceId = "device-123",
            timestamp = 1000L,
            deviceName = "Pixel 8",
            role = "MASTER"
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<SyncMessage>(serialized)

        assertTrue(deserialized is SyncMessage.Hello)
        val hello = deserialized as SyncMessage.Hello
        assertEquals("device-123", hello.senderDeviceId)
        assertEquals("Pixel 8", hello.deviceName)
        assertEquals("MASTER", hello.role)
    }

    @Test
    fun testTrackPrepareAndReadySerialization() {
        val prepare: SyncMessage = SyncMessage.TrackPrepare(
            sequence = 2,
            senderDeviceId = "master-1",
            timestamp = 2000L,
            track = SyncTrack(
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                durationMs = 200000L,
                videoId = "4NRXx6U8ABQ"
            ),
            playRequestId = "req-99",
            positionMs = 1500L
        )
        val prepStr = json.encodeToString(prepare)
        val prepMsg = json.decodeFromString<SyncMessage>(prepStr) as SyncMessage.TrackPrepare
        assertEquals("Blinding Lights", prepMsg.track.title)
        assertEquals("4NRXx6U8ABQ", prepMsg.track.videoId)
        assertEquals(1500L, prepMsg.positionMs)

        val ready: SyncMessage = SyncMessage.TrackReady(
            sequence = 3,
            senderDeviceId = "slave-1",
            timestamp = 2500L,
            playRequestId = "req-99",
            isAvailable = true,
            matchedTitle = "Blinding Lights"
        )
        val readyStr = json.encodeToString(ready)
        val readyMsg = json.decodeFromString<SyncMessage>(readyStr) as SyncMessage.TrackReady
        assertTrue(readyMsg.isAvailable)
        assertEquals("Blinding Lights", readyMsg.matchedTitle)
    }

    @Test
    fun testPlayAtAndSyncStateSerialization() {
        val playAt: SyncMessage = SyncMessage.PlayAt(
            sequence = 4,
            senderDeviceId = "master-1",
            timestamp = 3000L,
            playRequestId = "req-99",
            positionMs = 0L,
            masterTargetTimeMs = 4200L
        )
        val playStr = json.encodeToString(playAt)
        val playMsg = json.decodeFromString<SyncMessage>(playStr) as SyncMessage.PlayAt
        assertEquals(4200L, playMsg.masterTargetTimeMs)

        val syncState: SyncMessage = SyncMessage.SyncState(
            sequence = 5,
            senderDeviceId = "master-1",
            timestamp = 5000L,
            track = SyncTrack("Starboy", "The Weeknd", "Starboy", 230000L),
            positionMs = 12000L,
            isPlaying = true,
            playbackSpeed = 1.0f,
            masterTimestamp = 5000L
        )
        val stateStr = json.encodeToString(syncState)
        val stateMsg = json.decodeFromString<SyncMessage>(stateStr) as SyncMessage.SyncState
        assertEquals(12000L, stateMsg.positionMs)
        assertTrue(stateMsg.isPlaying)
        assertEquals("Starboy", stateMsg.track?.title)
    }

    @Test
    fun testJoinAcceptedWithSessionRoster() {
        val session = SyncSession(
            sessionId = "MYPLAYER-1234",
            sessionName = "Aashu Room",
            masterDeviceId = "master-dev",
            masterDeviceName = "Pixel 8 Pro",
            port = 48950,
            devices = listOf(
                SyncDevice("master-dev", "Pixel 8 Pro", "MASTER", isReady = true),
                SyncDevice("slave-dev", "Galaxy S24", "SLAVE", isReady = true, currentTrackTitle = "Starboy")
            )
        )
        val accepted: SyncMessage = SyncMessage.JoinAccepted(
            sequence = 6,
            senderDeviceId = "master-dev",
            timestamp = 6000L,
            session = session
        )
        val encoded = json.encodeToString(accepted)
        val decoded = json.decodeFromString<SyncMessage>(encoded) as SyncMessage.JoinAccepted
        assertEquals("MYPLAYER-1234", decoded.session.sessionId)
        assertEquals(2, decoded.session.devices.size)
        assertEquals("Galaxy S24", decoded.session.devices[1].deviceName)
    }
}
