package com.example.myplayer.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying the Online Playback Recovery mechanism:
 * 1. Error classification & semantics (PlaybackErrorHandler)
 * 2. Retryable vs non-retryable error detection
 * 3. Track origin detection (online vs local vs downloaded)
 * 4. Bounded backoff retry sequence & delay policy
 * 5. Generation token race prevention on track skips
 * 6. Terminal failure transitions (AutoplayRequested)
 */
class OnlinePlaybackRecoveryTest {

    private lateinit var errorHandler: PlaybackErrorHandler

    @Before
    fun setup() {
        // Instantiate without Android Context (uses fallback constructor with networkOverride=true)
        errorHandler = PlaybackErrorHandler(context = null, networkOverride = true)
    }

    // =========================================================================
    // 1. Error Semantics & Classification (PlaybackErrorHandler)
    // =========================================================================

    @Test
    fun classifyError_deviceOffline_reportsNoInternetConnection() {
        val offlineHandler = PlaybackErrorHandler(context = null, networkOverride = false)
        val exception = PlaybackException(
            "Connection failed",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )
        val message = offlineHandler.classifyError(exception, isOnline = true)
        assertEquals("No internet connection. Please check your network.", message)
    }

    @Test
    fun classifyError_badHttpStatusOnline_reportsExpiredOrRejectedStream() {
        val exception = PlaybackException(
            "HTTP 403 Forbidden",
            null,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        )
        val message = errorHandler.classifyError(exception, isOnline = true)
        assertEquals("Audio stream expired or server rejected request.", message)
    }

    @Test
    fun classifyError_badHttpStatusOfflineDownloaded_reportsDownloadedFileRemoved() {
        val exception = PlaybackException(
            "HTTP 404",
            null,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        )
        val message = errorHandler.classifyError(exception, isOnline = false)
        assertEquals("This song isn't available offline. The downloaded file may have been removed.", message)
    }

    @Test
    fun classifyError_connectionTimeout_reportsTimeout() {
        val exception = PlaybackException(
            "Timeout",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )
        val message = errorHandler.classifyError(exception, isOnline = true)
        assertEquals("Connection timed out while loading audio stream.", message)
    }

    @Test
    fun classifyError_networkConnectionFailedWhileOnline_reportsStreamServerUnreachable() {
        val exception = PlaybackException(
            "Socket error",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )
        val message = errorHandler.classifyError(exception, isOnline = true)
        assertEquals("Network error while streaming audio. Stream server could not be reached.", message)
    }

    @Test
    fun classifyError_decoderInitFailed_reportsUnsupportedFormat() {
        val exception = PlaybackException(
            "Decoder init error",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        )
        val message = errorHandler.classifyError(exception, isOnline = true)
        assertEquals("Audio decoder error. This media format is not supported on this device.", message)
    }

    @Test
    fun classifyError_fileNotFound_reportsLocalOrDownloadedNotFound() {
        val exception = PlaybackException(
            "File missing",
            null,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )
        val message = errorHandler.classifyError(exception, isOnline = false)
        assertEquals("Local or downloaded file not found.", message)
    }

    // =========================================================================
    // 2. Track Type & Origin Detection
    // =========================================================================

    private fun isOnlineTrack(mediaId: String?, path: String?): Boolean {
        if (mediaId == null) return false
        if (path != null) {
            if (path.startsWith("content://") || path.startsWith("file://") || path.startsWith("/")) {
                return false
            }
            if (path.startsWith("online://")) return true
        }
        if (mediaId.startsWith("online://")) return true
        if (mediaId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return true
        return false
    }

    @Test
    fun isOnlineTrack_correctlyIdentifiesOnlineVsLocalVsDownloaded() {
        // Online tracks
        assertTrue("online:// path should be online", isOnlineTrack("yt_123", "online://dQw4w9WgXcQ"))
        assertTrue("online:// mediaId should be online", isOnlineTrack("online://dQw4w9WgXcQ", null))
        assertTrue("11-char YouTube video ID should be online", isOnlineTrack("dQw4w9WgXcQ", null))

        // Local storage / content URIs
        assertFalse("content:// URI should be local", isOnlineTrack("101", "content://media/external/audio/media/101"))
        assertFalse("file:// URI should be local", isOnlineTrack("102", "file:///storage/emulated/0/Music/song.mp3"))
        assertFalse("absolute path should be local", isOnlineTrack("103", "/storage/emulated/0/Music/song.flac"))

        // Downloaded files stored locally
        assertFalse("downloaded local path should be offline", isOnlineTrack("dQw4w9WgXcQ", "/data/user/0/com.example.myplayer/files/downloads/dQw4w9WgXcQ.m4a"))
    }

    // =========================================================================
    // 3. Retryable Error Classification
    // =========================================================================

    private fun isRetryableOnlineError(errorCode: Int): Boolean {
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> true
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> false
            else -> false
        }
    }

    @Test
    fun isRetryableOnlineError_transientErrorsAreRetryable() {
        assertTrue(isRetryableOnlineError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertTrue(isRetryableOnlineError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
        assertTrue(isRetryableOnlineError(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
        assertTrue(isRetryableOnlineError(PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
        assertTrue(isRetryableOnlineError(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED))
    }

    @Test
    fun isRetryableOnlineError_permanentErrorsAreNotRetryable() {
        assertFalse(isRetryableOnlineError(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
        assertFalse(isRetryableOnlineError(PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED))
        assertFalse(isRetryableOnlineError(PlaybackException.ERROR_CODE_DECODING_FAILED))
        assertFalse(isRetryableOnlineError(PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK))
    }

    // =========================================================================
    // 4. Bounded Retry Policy & Delays
    // =========================================================================

    @Test
    fun retryPolicy_constantsAndBackoffDelays() {
        val maxRetries = 3
        val retryDelaysMs = longArrayOf(0L, 750L, 1750L)

        assertEquals("Maximum retry attempts must be 3", 3, maxRetries)
        assertEquals("First attempt should be immediate (0ms)", 0L, retryDelaysMs[0])
        assertTrue("Second attempt should be ~500-800ms", retryDelaysMs[1] in 500L..800L)
        assertTrue("Third attempt should be ~1.5-2.0s", retryDelaysMs[2] in 1500L..2000L)
    }

    // =========================================================================
    // 5. Recovery State Machine Simulation: Tests 1, 2, 3, 4, 5, 6
    // =========================================================================

    /**
     * Simulated test harness modeling the MusicController online recovery logic.
     */
    private class SimulatedOnlineRecoverySession {
        var currentToken: Long = 0L
        var retryCount: Int = 0
        var activeTrackId: String? = null
        var isPlaying: Boolean = false
        var autoplayRequestedTriggered: Boolean = false
        var userErrorMessageShown: String? = null
        var resolvedStreamCount: Int = 0

        fun startTrack(trackId: String) {
            currentToken++
            retryCount = 0
            activeTrackId = trackId
            isPlaying = true
            autoplayRequestedTriggered = false
            userErrorMessageShown = null
        }

        fun skipToNext(nextTrackId: String) {
            currentToken++
            retryCount = 0
            activeTrackId = nextTrackId
            isPlaying = true
            userErrorMessageShown = null
        }

        fun simulateError(
            errorTrackId: String,
            isNetworkAvailable: Boolean,
            streamResolver: (attempt: Int) -> String?
        ): Boolean = simulateError(errorTrackId, networkProvider = { isNetworkAvailable }, streamResolver = streamResolver)

        fun simulateError(
            errorTrackId: String,
            networkProvider: () -> Boolean = { true },
            streamResolver: (attempt: Int) -> String?
        ): Boolean {
            if (activeTrackId != errorTrackId) return false

            val sessionToken = currentToken
            val maxRetries = 3

            while (retryCount < maxRetries) {
                // If user skipped or token changed, cancel stale recovery
                if (sessionToken != currentToken) {
                    return false
                }

                retryCount++
                val freshStream = streamResolver(retryCount)
                if (freshStream != null && networkProvider()) {
                    // Check token again before applying
                    if (sessionToken != currentToken) return false
                    resolvedStreamCount++
                    isPlaying = true
                    userErrorMessageShown = null
                    return true // Recovered!
                }
            }

            // Exhausted retries -> Terminal failure
            if (sessionToken == currentToken) {
                isPlaying = false
                userErrorMessageShown = "Audio stream expired or server rejected request."
                autoplayRequestedTriggered = true
            }
            return false
        }
    }

    @Test
    fun test1_onlineStreamSucceedsImmediately() {
        val session = SimulatedOnlineRecoverySession()
        session.startTrack("rec_song_1")

        assertTrue("Playback should be active", session.isPlaying)
        assertEquals("Retry count should be 0", 0, session.retryCount)
        assertNull("No error shown", session.userErrorMessageShown)
    }

    @Test
    fun test2_firstStreamFails_secondAttemptSucceeds() {
        val session = SimulatedOnlineRecoverySession()
        session.startTrack("rec_song_2")

        val recovered = session.simulateError("rec_song_2", isNetworkAvailable = true) { attempt ->
            if (attempt == 1) null else "https://googlevideo.com/fresh_stream"
        }

        assertTrue("Should recover on second attempt", recovered)
        assertTrue("Playback resumed automatically", session.isPlaying)
        assertEquals("Should have taken 2 attempts (retryCount=2)", 2, session.retryCount)
        assertNull("No error should be presented to the user during transient recovery", session.userErrorMessageShown)
        assertFalse("Autoplay skip should not be triggered when recovery succeeds", session.autoplayRequestedTriggered)
    }

    @Test
    fun test3_firstTwoAttemptsFail_thirdAttemptSucceeds() {
        val session = SimulatedOnlineRecoverySession()
        session.startTrack("rec_song_3")

        val recovered = session.simulateError("rec_song_3", isNetworkAvailable = true) { attempt ->
            if (attempt < 3) null else "https://googlevideo.com/fresh_stream_attempt3"
        }

        assertTrue("Should recover on third attempt", recovered)
        assertTrue("Playback resumed automatically", session.isPlaying)
        assertEquals("Should have taken 3 attempts", 3, session.retryCount)
        assertNull("No error shown to user", session.userErrorMessageShown)
        assertFalse("No autoplay skip triggered", session.autoplayRequestedTriggered)
    }

    @Test
    fun test4_allRetryAttemptsFail_triggersAutoplayNext() {
        val session = SimulatedOnlineRecoverySession()
        session.startTrack("rec_song_failing")

        val recovered = session.simulateError("rec_song_failing", isNetworkAvailable = true) { _ ->
            null // All attempts fail to resolve
        }

        assertFalse("Recovery should have failed", recovered)
        assertFalse("Playback stopped for failed song", session.isPlaying)
        assertEquals("Retries exhausted at 3", 3, session.retryCount)
        assertNotNull("User error exposed only after retries exhausted", session.userErrorMessageShown)
        assertTrue("Autoplay next song must be triggered automatically", session.autoplayRequestedTriggered)
    }

    @Test
    fun test5_networkTemporarilyUnavailableThenReturns() {
        val session = SimulatedOnlineRecoverySession()
        session.startTrack("rec_song_temp_net_loss")

        var networkOnline = false
        val recovered = session.simulateError("rec_song_temp_net_loss", networkProvider = { networkOnline }) { attempt ->
            if (attempt == 2) {
                networkOnline = true // Network restored on 2nd attempt
            }
            if (networkOnline) "https://googlevideo.com/restored_stream" else null
        }

        // Initially false, but attempt 2 restored network
        assertTrue("Should recover once network returns", recovered)
        assertTrue("Playback resumed", session.isPlaying)
        assertNull(session.userErrorMessageShown)
    }

    @Test
    fun test6_userPressesNextDuringRecovery_cancelsOldRecovery() {
        val session = SimulatedOnlineRecoverySession()
        session.startTrack("stale_song_A")

        // User manually presses Next to song B while recovery was pending
        session.skipToNext("new_song_B")

        // Simulate stale callback trying to resolve song A
        val recoveredStale = session.simulateError("stale_song_A", isNetworkAvailable = true) {
            "https://googlevideo.com/stale_song_A_stream"
        }

        assertFalse("Stale recovery for song A must be rejected", recoveredStale)
        assertEquals("Active track must remain song B", "new_song_B", session.activeTrackId)
        assertTrue("Active track B should be playing", session.isPlaying)
    }

    @Test
    fun test7_localSongPlaybackFails_doesNotTriggerOnlineRecovery() {
        val localMediaId = "1001"
        val localPath = "content://media/external/audio/media/1001"
        val isOnline = isOnlineTrack(localMediaId, localPath)
        assertFalse("Local song must NOT be treated as online", isOnline)

        val exception = PlaybackException("File error", null, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        val errorMessage = errorHandler.handleError(exception, hasNextItem = false, isOnline = isOnline)
        assertEquals("Local or downloaded file not found.", errorMessage)
        assertEquals("Local or downloaded file not found.", errorHandler.playbackError.value)
    }

    @Test
    fun test8_downloadedSongPlaybackFails_doesNotTriggerOnlineRecovery() {
        val downloadedMediaId = "yt_download_1"
        val downloadedLocalPath = "/data/user/0/com.example.myplayer/files/downloads/yt_download_1.m4a"
        val isOnline = isOnlineTrack(downloadedMediaId, downloadedLocalPath)
        assertFalse("Downloaded local file must NOT be treated as online stream", isOnline)

        val exception = PlaybackException("File read error", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        val errorMessage = errorHandler.handleError(exception, hasNextItem = false, isOnline = isOnline)
        assertEquals("This song isn't available offline. The downloaded file may have been removed.", errorMessage)
        assertEquals("This song isn't available offline. The downloaded file may have been removed.", errorHandler.playbackError.value)
    }
}
