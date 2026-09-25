package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.lastfm.LastFmTrackMatcher
import com.example.myplayer.data.recommendation.lastfm.LastFmTrackMetadata
import com.example.myplayer.security.StringEncryptionManager
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LastFmTrackMatcherTest {

    private lateinit var matcher: LastFmTrackMatcher

    @Before
    fun setup() {
        // InnertubeApi dummy instance for testing findBestMatch scoring logic
        val dummyApi = InnertubeApi(OkHttpClient(), StringEncryptionManager())
        matcher = LastFmTrackMatcher(dummyApi)
    }

    @Test
    fun testFindBestMatch_ExactTitleAndArtist_MatchesSuccessfully() {
        val target = LastFmTrackMetadata(name = "Blinding Lights", artist = "The Weeknd", durationSeconds = 200L)
        val candidates = listOf(
            OnlineSong(videoId = "vid1", title = "Blinding Lights", artist = "The Weeknd", thumbnailUrl = "", durationMs = 200000L),
            OnlineSong(videoId = "vid2", title = "Something Else", artist = "Different Artist", thumbnailUrl = "", durationMs = 150000L)
        )

        val match = matcher.findBestMatch(target, candidates)
        assertNotNull(match)
        assertEquals("vid1", match?.videoId)
    }

    @Test
    fun testFindBestMatch_MismatchedArtist_RejectsMatch() {
        val target = LastFmTrackMetadata(name = "As It Was", artist = "Harry Styles", durationSeconds = 167L)
        val candidates = listOf(
            OnlineSong(videoId = "vid_wrong", title = "Totally Unrelated", artist = "Unknown Band", thumbnailUrl = "", durationMs = 300000L)
        )

        val match = matcher.findBestMatch(target, candidates)
        assertNull("Must reject candidate with no correlation", match)
    }

    @Test
    fun testFindBestMatch_EmptyCandidates_ReturnsNull() {
        val target = LastFmTrackMetadata(name = "Bad Guy", artist = "Billie Eilish")
        val match = matcher.findBestMatch(target, emptyList())
        assertNull(match)
    }
}
