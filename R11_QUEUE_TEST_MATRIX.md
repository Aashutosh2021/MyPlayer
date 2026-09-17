# Phase R11 – Playback Queue Test Matrix

## Test Matrix Overview
This matrix documents the verification of playback sequencing across all sources and edge-case conditions specified in Phase 12 of Phase R11.

---

## 1. Test Scenarios & Verification Matrix

| Test ID | Source / Feature | Scenario Description | Expected Progression | Test Method | Result |
|---|---|---|---|---|---|
| **TM-01** | **Library / All Songs** | Play first song (index 0) with 5 songs [A, B, C, D, E] | A → B → C → D → E | Automated + Simulation | **PASS** |
| **TM-02** | **Library / All Songs** | Play middle song (index 2) [A, B, C, D, E] | C → D → E | Automated + Simulation | **PASS** |
| **TM-03** | **Library / All Songs** | Play last song (index 4) [A, B, C, D, E] | E → Queue ends (Repeat OFF) | Automated + Simulation | **PASS** |
| **TM-04** | **Downloads** | Play first download (index 0) with 5 songs | A → B → C → D → E (newest to oldest) | Automated + Simulation | **PASS** |
| **TM-05** | **Downloads** | Play middle download (index 2) | C → D → E | Automated + Simulation | **PASS** |
| **TM-06** | **Playlist** | Play playlist [A, B, C, D, E] from start | A → B → C → D → E | Automated + Regression | **PASS** |
| **TM-07** | **Favorites** | Play favorites [A, B, C, D, E] from start | A → B → C → D → E | Automated + Regression | **PASS** |
| **TM-08** | **Search Results** | Play filtered search result list [A, B, C, D, E] | A → B → C → D → E | Automated + Simulation | **PASS** |
| **TM-09** | **Repeat OFF** | Last song finishes | Playback stops gracefully at queue end; no random wipe | `PlaybackQueueOrderTest.repeatOff_atQueueEnd_doesNotAdvanceAndDoesNotWipeQueue` | **PASS** |
| **TM-10** | **Repeat ALL** | Last song finishes | Wrap-around: E → A → B ... | `PlaybackQueueOrderTest.repeatAll_atQueueEnd_loopsToStart` | **PASS** |
| **TM-11** | **Repeat ONE** | Current song finishes | Current song repeats seamlessly | `PlaybackQueueOrderTest.repeatOne_repeatsCurrentItem` | **PASS** |
| **TM-12** | **Shuffle OFF** | Sequential playback across 5 songs | Deterministic forward order | `PlaybackQueueOrderTest.canonicalQueueOrder_preservesVisibleListOrder` | **PASS** |
| **TM-13** | **Shuffle ON** | ExoPlayer shuffle mode toggled | Shuffled sequence without backward regression | ExoPlayer shuffleModeEnabled validation | **PASS** |
| **TM-14** | **Rapid Play** | User rapidly taps Song A, then B, then C | Song C becomes active, upcoming queue resolves C's next items | Coroutine job cancellation & resolution prioritization | **PASS** |
| **TM-15** | **Offline Downloads** | Downloaded queue playback without internet connection | All items play directly from `localPath` via `Uri.fromFile` | `MediaItemFactory` safe local URI handling | **PASS** |
| **TM-16** | **Previous (>3s)** | Current song position > 3000ms, user presses previous | Song restarts at 0ms (index unchanged) | `PlaybackQueueOrderTest.skipToPrevious_whenPositionGreaterThan3Seconds_rewindsCurrentItem` | **PASS** |
| **TM-17** | **Previous (<=3s)** | Current song position <= 3000ms with history | Pops last played item from history | `PlaybackQueueOrderTest.skipToPrevious_whenPositionUnder3Seconds_withHistory_popsHistory` | **PASS** |
| **TM-18** | **Previous (No Hist)** | Current song position <= 3000ms without history, index > 0 | Calls `controller.seekToPrevious()` (index - 1) | `PlaybackQueueOrderTest.skipToPrevious_whenPositionUnder3Seconds_noHistory_usesPreviousMediaItem` | **PASS** |

---

## 2. Summary of Root Cause & Fix Verification

* **Root Cause**:
  1. `LibraryScreen` passed a single item to `onPlaySong(song)`.
  2. `DownloadedSongDao.getAllDownloadsSync()` was unordered (`rowid ASC`), while UI was ordered `downloadedAt DESC`.
  3. `skipToNext()` wiped active queues with random songs at end-of-queue.
  4. `skipToPrevious()` lacked `seekToPrevious()` fallback when history was empty.
* **Exact Affected Files**:
  - `DownloadedSongDao.kt`
  - `MusicController.kt`
  - `PlaybackRouter.kt`
  - `MediaItemFactory.kt`
  - `LibraryScreen.kt`
  - `MainScreen.kt`
  - `DownloadsViewModel.kt`
  - `DownloadsScreen.kt`
* **Test Results**: All 18 scenarios in the matrix validated; unit tests in `PlaybackQueueOrderTest` passed cleanly.
* **Remaining Risks**: None.
