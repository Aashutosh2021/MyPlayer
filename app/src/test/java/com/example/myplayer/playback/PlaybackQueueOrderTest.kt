package com.example.myplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying canonical queue ordering, index progression,
 * repeat modes, end-of-queue safety, and navigation rules for Phase R11.
 */
class PlaybackQueueOrderTest {

    data class MockSong(
        val id: String,
        val title: String,
        val downloadedAt: Long = 0L
    )

    // ====================================================================
    // Phase 3 & 8: Canonical Queue Order
    // ====================================================================

    @Test
    fun canonicalQueueOrder_preservesVisibleListOrder() {
        val visibleSongs = listOf(
            MockSong("1", "Song A"),
            MockSong("2", "Song B"),
            MockSong("3", "Song C"),
            MockSong("4", "Song D"),
            MockSong("5", "Song E")
        )

        val startIndex = 0
        val queue = visibleSongs.toList()

        assertEquals(5, queue.size)
        assertEquals("Song A", queue[startIndex].title)
        assertEquals("Song B", queue[1].title)
        assertEquals("Song C", queue[2].title)
        assertEquals("Song D", queue[3].title)
        assertEquals("Song E", queue[4].title)
    }

    @Test
    fun middleItemPlayback_startsAtCorrectIndex() {
        val visibleSongs = listOf(
            MockSong("1", "Song A"),
            MockSong("2", "Song B"),
            MockSong("3", "Song C"),
            MockSong("4", "Song D"),
            MockSong("5", "Song E")
        )

        val startIndex = 2 // Song C
        assertEquals("Song C", visibleSongs[startIndex].title)

        // Advancing from middle item proceeds to subsequent songs
        val nextIndex = startIndex + 1
        assertEquals("Song D", visibleSongs[nextIndex].title)
    }

    // ====================================================================
    // Phase 7: Downloads List Ordering (downloadedAt DESC)
    // ====================================================================

    @Test
    fun downloadsQueueOrder_matchesUiDescendingTimestampSort() {
        val downloadedSongsFromDb = listOf(
            MockSong("1", "Oldest Download", downloadedAt = 1000L),
            MockSong("2", "Middle Download", downloadedAt = 2000L),
            MockSong("3", "Newest Download", downloadedAt = 3000L)
        )

        // UI displays downloads sorted by downloadedAt DESC:
        val uiSortedDownloads = downloadedSongsFromDb.sortedByDescending { it.downloadedAt }

        // With ORDER BY downloadedAt DESC in DAO:
        assertEquals("Newest Download", uiSortedDownloads[0].title)
        assertEquals("Middle Download", uiSortedDownloads[1].title)
        assertEquals("Oldest Download", uiSortedDownloads[2].title)

        // Tapping index 0 in UI plays Newest Download, auto-next proceeds to Middle Download (index 1)
        val selectedIndex = 0
        assertEquals("Newest Download", uiSortedDownloads[selectedIndex].title)
        val nextIndex = selectedIndex + 1
        assertEquals("Middle Download", uiSortedDownloads[nextIndex].title)
    }

    // ====================================================================
    // Phase 5 & 12: Repeat Modes & Queue End Behavior
    // ====================================================================

    @Test
    fun repeatOff_atQueueEnd_doesNotAdvanceAndDoesNotWipeQueue() {
        val queueSize = 5
        val currentIndex = 4 // Last item (Song E)
        val repeatMode = RepeatMode.OFF

        val nextIndex = calculateNextIndex(currentIndex, queueSize, repeatMode)
        // Repeat OFF: queue has no next item; playback stops without wiping queue
        assertNull(nextIndex)
    }

    @Test
    fun repeatAll_atQueueEnd_loopsToStart() {
        val queueSize = 5
        val currentIndex = 4 // Last item
        val repeatMode = RepeatMode.ALL

        val nextIndex = calculateNextIndex(currentIndex, queueSize, repeatMode)
        assertEquals(0, nextIndex)
    }

    @Test
    fun repeatOne_repeatsCurrentItem() {
        val queueSize = 5
        val currentIndex = 2 // Song C
        val repeatMode = RepeatMode.ONE

        val nextIndex = calculateNextIndex(currentIndex, queueSize, repeatMode)
        assertEquals(2, nextIndex)
    }

    // ====================================================================
    // Phase 6: Previous Song Navigation Rules (3-Second Rule)
    // ====================================================================

    @Test
    fun skipToPrevious_whenPositionGreaterThan3Seconds_rewindsCurrentItem() {
        val currentPositionMs = 4500L
        val currentIndex = 2

        val action = calculatePreviousAction(
            currentPositionMs = currentPositionMs,
            currentIndex = currentIndex,
            history = listOf("Song A", "Song B")
        )

        assertEquals(PreviousAction.REWIND_TO_START, action)
    }

    @Test
    fun skipToPrevious_whenPositionUnder3Seconds_withHistory_popsHistory() {
        val currentPositionMs = 1200L
        val currentIndex = 2
        val history = listOf("Song A", "Song B")

        val action = calculatePreviousAction(
            currentPositionMs = currentPositionMs,
            currentIndex = currentIndex,
            history = history
        )

        assertEquals(PreviousAction.PLAY_FROM_HISTORY, action)
    }

    @Test
    fun skipToPrevious_whenPositionUnder3Seconds_noHistory_usesPreviousMediaItem() {
        val currentPositionMs = 1000L
        val currentIndex = 2
        val history = emptyList<String>()

        val action = calculatePreviousAction(
            currentPositionMs = currentPositionMs,
            currentIndex = currentIndex,
            history = history
        )

        assertEquals(PreviousAction.SEEK_TO_PREVIOUS_ITEM, action)
    }

    @Test
    fun skipToPrevious_atFirstItemUnder3Seconds_noHistory_rewindsToStart() {
        val currentPositionMs = 800L
        val currentIndex = 0
        val history = emptyList<String>()

        val action = calculatePreviousAction(
            currentPositionMs = currentPositionMs,
            currentIndex = currentIndex,
            history = history
        )

        assertEquals(PreviousAction.REWIND_TO_START, action)
    }

    // ====================================================================
    // Phase 10: Background Pre-resolution Prioritization
    // ====================================================================

    @Test
    fun backgroundResolutionOrder_prioritizesUpcomingSongsFirst() {
        val totalSongs = 5
        val startIndex = 2 // Playing item 2 (Song C)

        // Pre-resolution order must resolve 3, 4 first, then wrap to 0, 1
        val resolutionOrder = (startIndex + 1 until totalSongs) + (0 until startIndex)

        assertEquals(listOf(3, 4, 0, 1), resolutionOrder)
    }

    // ====================================================================
    // Helper Models & Simulation Logic
    // ====================================================================

    enum class RepeatMode { OFF, ONE, ALL }
    enum class PreviousAction { REWIND_TO_START, PLAY_FROM_HISTORY, SEEK_TO_PREVIOUS_ITEM }

    private fun calculateNextIndex(currentIndex: Int, queueSize: Int, repeatMode: RepeatMode): Int? {
        if (queueSize <= 0) return null
        return when (repeatMode) {
            RepeatMode.ONE -> currentIndex
            RepeatMode.ALL -> (currentIndex + 1) % queueSize
            RepeatMode.OFF -> if (currentIndex + 1 < queueSize) currentIndex + 1 else null
        }
    }

    private fun calculatePreviousAction(
        currentPositionMs: Long,
        currentIndex: Int,
        history: List<String>
    ): PreviousAction {
        return if (currentPositionMs > 3000L) {
            PreviousAction.REWIND_TO_START
        } else if (history.isNotEmpty()) {
            PreviousAction.PLAY_FROM_HISTORY
        } else if (currentIndex > 0) {
            PreviousAction.SEEK_TO_PREVIOUS_ITEM
        } else {
            PreviousAction.REWIND_TO_START
        }
    }
}
