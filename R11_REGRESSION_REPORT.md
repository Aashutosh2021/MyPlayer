# Phase R11 – Regression Analysis & Protection Report

## 1. Overview
This report evaluates potential regression risks introduced by Phase R11 fixes and details the automated test suite added to prevent recurrence of queue sequencing bugs.

---

## 2. Root Cause, Affected Files & Functions
* **Root Causes**:
  - Library single-item list dispatch to `musicController.playSongs()`.
  - Downloads database unordered query (`rowid ASC`) conflicting with UI descending timestamp order (`downloadedAt DESC`).
  - Destruction of user playback queues by `skipToNext()` fallback when hitting the end of queue.
  - Inability to seek previous song when starting playback in middle of queue due to empty history stack.
  - Linear pre-resolution in `PlaybackRouter` resolving backwards before forwards.
  - Missing file scheme parsing for absolute paths in `MediaItemFactory`.
* **Exact Affected Files & Functions**:
  - [DownloadedSongDao.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/DownloadedSongDao.kt): `getAllDownloadsSync()`
  - [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt): `playDownloadedSongs()`, `skipToPrevious()`, `skipToNext()`, `updateCurrentSong()`
  - [PlaybackRouter.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/PlaybackRouter.kt): `playQueue()`
  - [MediaItemFactory.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MediaItemFactory.kt): `createMediaItem()`
  - [LibraryScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/library/LibraryScreen.kt): `onPlaySong` callback in `LazyColumn`
  - [MainScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/main/MainScreen.kt): `Screen.Library.route`
  - [DownloadsViewModel.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsViewModel.kt): `playSong()`
  - [DownloadsScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsScreen.kt): `LazyColumn` item click

---

## 3. Regression Risk Assessment & Safeguards

| Feature Area | Potential Risk | Safety Mechanism in Place | Status |
|---|---|---|---|
| **ExoPlayer Queue Integrity** | Calling `setMediaItems` with many songs might consume excessive memory | Media3 `MediaItem` instances contain lightweight metadata; audio stream sources are resolved on demand | **SAFE** |
| **Downloads Search Filter** | Clicking a filtered list item could play items outside the filter | `DownloadsScreen` passes the currently displayed `downloads` list, preserving filtering | **SAFE** |
| **Previous Navigation** | Previous button might skip to wrong song if history is populated | Prioritizes 3-second rewind first, then history pop, then `controller.seekToPrevious()` fallback | **SAFE** |
| **End-of-Queue Next** | Tapping next at last song could crash or freeze | Guarded: stops cleanly if repeat mode is OFF, loops if ALL, repeats if ONE | **SAFE** |
| **Offline Playback** | Local downloads might fail if path lacks `file://` scheme | `MediaItemFactory` explicitly formats absolute paths using `Uri.fromFile(File(path))` | **SAFE** |

---

## 4. Automated Regression Tests Executed

The test suite [PlaybackQueueOrderTest.kt](file:///e:/MyPlayer/app/src/test/java/com/example/myplayer/playback/PlaybackQueueOrderTest.kt) was executed via:
```bash
./gradlew testDebugUnitTest --tests com.example.myplayer.playback.PlaybackQueueOrderTest
```

### Test Case Results:
1. `canonicalQueueOrder_preservesVisibleListOrder`: **PASSED**
2. `middleItemPlayback_startsAtCorrectIndex`: **PASSED**
3. `downloadsQueueOrder_matchesUiDescendingTimestampSort`: **PASSED**
4. `repeatOff_atQueueEnd_doesNotAdvanceAndDoesNotWipeQueue`: **PASSED**
5. `repeatAll_atQueueEnd_loopsToStart`: **PASSED**
6. `repeatOne_repeatsCurrentItem`: **PASSED**
7. `skipToPrevious_whenPositionGreaterThan3Seconds_rewindsCurrentItem`: **PASSED**
8. `skipToPrevious_whenPositionUnder3Seconds_withHistory_popsHistory`: **PASSED**
9. `skipToPrevious_whenPositionUnder3Seconds_noHistory_usesPreviousMediaItem`: **PASSED**
10. `skipToPrevious_atFirstItemUnder3Seconds_noHistory_rewindsToStart`: **PASSED**
11. `backgroundResolutionOrder_prioritizesUpcomingSongsFirst`: **PASSED**

---

## 5. Remaining Risks
There are no known remaining regression risks. The fix is strictly localized to the sequencing pipeline and UI dispatch callbacks without altering database schemas or introducing competing queue managers.
