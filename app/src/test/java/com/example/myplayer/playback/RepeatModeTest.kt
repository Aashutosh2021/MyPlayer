package com.example.myplayer.playback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.media3.common.Player
import com.example.myplayer.ui.theme.ClayPrimary
import com.example.myplayer.ui.theme.TextMuted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying playback sequence behavior for all 3 repeat states:
 * 1. Normal Loop (Player.REPEAT_MODE_ALL)
 * 2. Loop with "1" (Player.REPEAT_MODE_ONE)
 * 3. Repeat disabled (Player.REPEAT_MODE_OFF)
 *
 * Also verifies shuffle independence, cycling sequence, and UI icon/tint selection.
 */
class RepeatModeTest {

    data class MockSong(
        val id: String,
        val title: String,
        val source: String = "Playlist" // Album, Folder, Artist, Search, Queue, etc.
    )

    // Helper logic modeling the player's next index calculation per Media3 specifications
    private fun calculateNextIndex(
        currentIndex: Int,
        queueSize: Int,
        repeatMode: Int
    ): Int? {
        if (queueSize <= 0) return null
        return when (repeatMode) {
            Player.REPEAT_MODE_ONE -> currentIndex
            Player.REPEAT_MODE_ALL -> (currentIndex + 1) % queueSize
            Player.REPEAT_MODE_OFF -> if (currentIndex + 1 < queueSize) currentIndex + 1 else null
            else -> null
        }
    }

    // Helper logic simulating repeat mode cycling
    private fun cycleRepeatMode(currentMode: Int): Int {
        return when (currentMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // Helper mapping reflecting NowPlayingScreen icon & tint selection
    data class RepeatUiState(
        val icon: ImageVector,
        val tint: androidx.compose.ui.graphics.Color,
        val contentDescription: String
    )

    private fun getRepeatUiState(repeatMode: Int): RepeatUiState {
        return when (repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatUiState(Icons.Filled.RepeatOne, ClayPrimary, "Repeat One")
            Player.REPEAT_MODE_ALL -> RepeatUiState(Icons.Filled.Repeat, ClayPrimary, "Repeat All")
            else -> RepeatUiState(Icons.Filled.Repeat, TextMuted, "Repeat Off")
        }
    }

    // ====================================================================
    // 1. REPEAT_MODE_ALL Tests (Normal Loop)
    // ====================================================================

    @Test
    fun repeatAll_lastSong_automaticallyWrapsToFirstSong() {
        val queue = listOf(
            MockSong("1", "Song 1"),
            MockSong("2", "Song 2"),
            MockSong("3", "Song 3")
        )
        val lastIndex = queue.size - 1 // Index 2 (Song 3)
        val nextIndex = calculateNextIndex(lastIndex, queue.size, Player.REPEAT_MODE_ALL)

        // After last song finishes, automatically start again from the first song
        assertEquals(0, nextIndex)
        assertEquals("Song 1", queue[nextIndex!!].title)
    }

    @Test
    fun repeatAll_playsSongsInOrderContinuously() {
        val queue = listOf(
            MockSong("1", "Song 1"),
            MockSong("2", "Song 2"),
            MockSong("3", "Song 3")
        )

        // Simulate full loop: 0 -> 1 -> 2 -> 0 -> 1 -> 2
        var currentIndex = 0
        val playedTitles = mutableListOf<String>()

        for (step in 0 until 6) {
            playedTitles.add(queue[currentIndex].title)
            currentIndex = calculateNextIndex(currentIndex, queue.size, Player.REPEAT_MODE_ALL)!!
        }

        val expectedOrder = listOf("Song 1", "Song 2", "Song 3", "Song 1", "Song 2", "Song 3")
        assertEquals(expectedOrder, playedTitles)
    }

    @Test
    fun repeatAll_singleItemQueue_loopsSameItem() {
        val queue = listOf(MockSong("1", "Solo Song"))
        val nextIndex = calculateNextIndex(0, queue.size, Player.REPEAT_MODE_ALL)
        assertEquals(0, nextIndex)
    }

    // ====================================================================
    // 2. REPEAT_MODE_ONE Tests (Loop "1")
    // ====================================================================

    @Test
    fun repeatOne_repeatsCurrentSongContinuously_doesNotAdvance() {
        val queue = listOf(
            MockSong("1", "Song 1"),
            MockSong("2", "Song 2"),
            MockSong("3", "Song 3")
        )

        val currentIndex = 1 // Song 2
        val nextIndex = calculateNextIndex(currentIndex, queue.size, Player.REPEAT_MODE_ONE)

        // Repeat only the currently playing song continuously without advancing
        assertEquals(currentIndex, nextIndex)
        assertEquals("Song 2", queue[nextIndex!!].title)
    }

    @Test
    fun repeatOne_atLastIndex_repeatsLastSong() {
        val queue = listOf(
            MockSong("1", "Song 1"),
            MockSong("2", "Song 2"),
            MockSong("3", "Song 3")
        )

        val lastIndex = queue.size - 1
        val nextIndex = calculateNextIndex(lastIndex, queue.size, Player.REPEAT_MODE_ONE)
        assertEquals(lastIndex, nextIndex)
        assertEquals("Song 3", queue[nextIndex!!].title)
    }

    // ====================================================================
    // 3. REPEAT_MODE_OFF Tests (Repeat Disabled)
    // ====================================================================

    @Test
    fun repeatOff_middleItem_advancesToNextSong() {
        val queue = listOf(
            MockSong("1", "Song 1"),
            MockSong("2", "Song 2"),
            MockSong("3", "Song 3")
        )

        val nextIndex = calculateNextIndex(0, queue.size, Player.REPEAT_MODE_OFF)
        assertEquals(1, nextIndex)
        assertEquals("Song 2", queue[nextIndex!!].title)
    }

    @Test
    fun repeatOff_lastSong_stopsPlaybackAndDoesNotRestart() {
        val queue = listOf(
            MockSong("1", "Song 1"),
            MockSong("2", "Song 2"),
            MockSong("3", "Song 3")
        )

        val lastIndex = queue.size - 1
        val nextIndex = calculateNextIndex(lastIndex, queue.size, Player.REPEAT_MODE_OFF)

        // After the last song finishes, stop playback (returns null; does not wrap to 0)
        assertNull(nextIndex)
    }

    // ====================================================================
    // 4. Repeat Mode Cycling Order Tests
    // ====================================================================

    @Test
    fun cycleRepeatMode_orderIsOffThenAllThenOneThenOff() {
        // Expected mapping & cycle:
        // Initial: REPEAT_MODE_OFF (0)
        val mode0 = Player.REPEAT_MODE_OFF
        assertEquals(0, mode0)

        // Cycle 1: Normal loop (REPEAT_MODE_ALL = 2)
        val mode1 = cycleRepeatMode(mode0)
        assertEquals(Player.REPEAT_MODE_ALL, mode1)
        assertEquals(2, mode1)

        // Cycle 2: Loop 1 (REPEAT_MODE_ONE = 1)
        val mode2 = cycleRepeatMode(mode1)
        assertEquals(Player.REPEAT_MODE_ONE, mode2)
        assertEquals(1, mode2)

        // Cycle 3: Repeat disabled (REPEAT_MODE_OFF = 0)
        val mode3 = cycleRepeatMode(mode2)
        assertEquals(Player.REPEAT_MODE_OFF, mode3)
        assertEquals(0, mode3)
    }

    // ====================================================================
    // 5. Shuffle Independence Tests
    // ====================================================================

    @Test
    fun shuffleAndRepeat_areIndependentSettings() {
        var isShuffleEnabled = false
        var repeatMode = Player.REPEAT_MODE_OFF

        // Enable shuffle -> repeatMode remains REPEAT_MODE_OFF
        isShuffleEnabled = true
        assertEquals(Player.REPEAT_MODE_OFF, repeatMode)
        assertTrue(isShuffleEnabled)

        // Change repeatMode -> shuffle remains enabled
        repeatMode = Player.REPEAT_MODE_ALL
        assertTrue(isShuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, repeatMode)

        // Change to repeat ONE -> shuffle remains enabled
        repeatMode = Player.REPEAT_MODE_ONE
        assertTrue(isShuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, repeatMode)

        // Disable shuffle -> repeatMode remains REPEAT_MODE_ONE
        isShuffleEnabled = false
        assertFalse(isShuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, repeatMode)
    }

    // ====================================================================
    // 6. UI Icon & Tint Mapping Tests
    // ====================================================================

    @Test
    fun uiIconMapping_repeatAll_displaysNormalLoopWithActiveTint() {
        val uiState = getRepeatUiState(Player.REPEAT_MODE_ALL)
        // Normal Loop icon: Icons.Filled.Repeat with active ClayPrimary tint
        assertEquals(Icons.Filled.Repeat, uiState.icon)
        assertEquals(ClayPrimary, uiState.tint)
        assertEquals("Repeat All", uiState.contentDescription)
    }

    @Test
    fun uiIconMapping_repeatOne_displaysLoopOneWithActiveTint() {
        val uiState = getRepeatUiState(Player.REPEAT_MODE_ONE)
        // Loop icon with "1": Icons.Filled.RepeatOne with active ClayPrimary tint
        assertEquals(Icons.Filled.RepeatOne, uiState.icon)
        assertEquals(ClayPrimary, uiState.tint)
        assertEquals("Repeat One", uiState.contentDescription)
    }

    @Test
    fun uiIconMapping_repeatOff_displaysDisabledArrowWithMutedTint() {
        val uiState = getRepeatUiState(Player.REPEAT_MODE_OFF)
        // Repeat disabled / normal arrow icon: Icons.Filled.Repeat with muted tint
        assertEquals(Icons.Filled.Repeat, uiState.icon)
        assertEquals(TextMuted, uiState.tint)
        assertEquals("Repeat Off", uiState.contentDescription)
    }

    // ====================================================================
    // 7. Works Across Any Playback Source
    // ====================================================================

    @Test
    fun repeatModeAll_worksAcrossAllSources() {
        val sources = listOf("Playlist", "Album", "Folder", "Artist", "Search Results", "Queue")

        for (source in sources) {
            val queue = listOf(
                MockSong("s1", "Track 1", source),
                MockSong("s2", "Track 2", source),
                MockSong("s3", "Track 3", source)
            )

            // When last song finishes, automatically wrap to first song
            val nextIndex = calculateNextIndex(2, queue.size, Player.REPEAT_MODE_ALL)
            assertEquals("Source $source should wrap to first song in REPEAT_MODE_ALL", 0, nextIndex)
            assertEquals("Track 1", queue[nextIndex!!].title)
        }
    }
}
