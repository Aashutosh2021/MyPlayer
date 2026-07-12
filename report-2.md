# MyPlayer V2 Foundation — Architecture Audit & Regression Investigation

This audit report details the comprehensive investigation, root cause analysis, and architectural analysis of the MyPlayer codebase, comparing the reference commit `57bdbff925c2420c9d95bc6f3f7c2489889b22fd` (Version 1.2v) with the current `HEAD` state.

---

## Phase 1 — Git Regression Audit

### 1. Files & Classes Modified
* **Playback Pipeline**:
  * MusicController.kt
  * AppDatabase.kt
  * DatabaseModule.kt
* **Download Pipeline**:
  * DownloadWorker.kt
  * DownloadedSongDao.kt
* **Lyrics Subsystem**:
  * LyricsRepository.kt
  * LyricsTab.kt
  * LyricsViewModel.kt
  * CachedLyricsDao.kt [NEW]
  * CachedLyricsEntity.kt [NEW]
* **Storage & Backup**:
  * BackupRestoreManager.kt [NEW]
  * SettingsScreen.kt
  * SettingsViewModel.kt
* **Compose UI & Performance**:
  * Modifiers.kt
  * NowPlayingScreen.kt
  * MainScreen.kt
  * MainViewModel.kt
  * PlaylistDetailViewModel.kt [NEW]

### 2. Classes Added / Removed
* **Added**:
  * `com.example.myplayer.data.backup.BackupRestoreManager`
  * `com.example.myplayer.data.local.dao.CachedLyricsDao`
  * `com.example.myplayer.data.local.entity.CachedLyricsEntity`
  * `com.example.myplayer.ui.screens.library.PlaylistDetailViewModel` (moved from screen file)
  * `com.example.myplayer.ui.screens.settings.BackupRestoreState`
  * `com.example.myplayer.ui.screens.nowplaying.LyricsMetadata`
* **Removed**:
  * None.

### 3. Dependency & Room Changes
* **Dependency Changes**:
  * `MusicController` is now tightly coupled with `InnertubeApi` to dynamically fetch streaming URLs for missing files.
  * `DownloadWorker` is now coupled with `LyricsRepository` for automatic lyric pre-caching.
* **Room Schema Changes**:
  * Incremented database version to `3`.
  * Added `cached_lyrics` table.
  * Created `MIGRATION_2_3` in `AppDatabase.kt`.

---

## Phase 2 — Playback Pipeline Audit

### Expected Playback Architecture
```
[User Click] ──► [MusicController / Router] ──► [Source Decision (Local/Remote)] ──► [Media3 / ExoPlayer]
```

### Current Playback Routing Implementation
* **Local Playback**: Playback requests for files on disk bypass resolution checks if the path begins with `http://` or `https://`.
* **Downloaded Playback**: Uses the same `playSongs` pipeline. However, `resolveLocalSongPathIfMissing()` is invoked first:
  1. Checks if `song.path` exists locally (via content resolver or `File.exists()`).
  2. If file missing: scans default `Music` folder for matching ID or title.
  3. If missing: checks connectivity. If online, resolves stream url from YouTube and plays over network. If offline, plays original path resulting in ExoPlayer error.
* **Online Playback**: Stream URL is resolved in the ViewModel and fed directly as `http/https` URI to Media3.
* **Autoplay Recommendation**: Fired by `MusicController.handlePlaybackEnded()`. Uses `RecommendationCoordinator` to fetch next recommended track, then calls `playOnlineSong()`.

---

## Phase 3 — Download Pipeline Audit

### Complete Pipeline Trace
```
[Search UI] ──► [Download Click] ──► [MainViewModel.downloadCurrentSong()] ──► [DownloadRepository] 
                                                                                   │
[App Library] ◄── [Room downloaded_songs] ◄── [DownloadWorker] ◄───────────────────┘
```

### Key Findings & Storage Details:
1. **Physical Storage**:
   * Standard: `/storage/emulated/0/Android/data/com.example.myplayer/files/Music/`
   * Custom SAF: `content://com.android.externalstorage.documents/tree/...`
2. **Room Database Persistence**:
   * Stores absolute file path or content URI in `DownloadedSongEntity.localPath`.
   * Stores YouTube video ID in `id`.
   * **Does NOT store** the transient `streamUrl`.
3. **Why Downloaded Songs Require Internet:**
   * When uninstalled or restored, absolute local file paths are wiped or modified. The restored Room DB still points to the old prefix.
   * If a file is missing on disk (or named incorrectly due to double-space title stripping conflicts), `resolveLocalSongPathIfMissing` evaluates to `false`.
   * If online, the controller fetches the remote YouTube stream and plays it over HTTP, consuming network data transparently. If offline, the resolution fails, resulting in `ENOENT` playback failure.

---

## Phase 4 — Comparison with Stable Commit (`57bdbff`)

| Component | Baseline Commit (`57bdbff`) | Current HEAD | Impact of Change |
| :--- | :--- | :--- | :--- |
| **Playback Routing** | Played `Uri.parse(song.path)` directly. | Runs `resolveLocalSongPathIfMissing` check. | Gated streaming fallbacks, causing network calls when files are missing. |
| **Download Worker** | No size verification. | Guard checks `fileSize <= 0` and collapses double-spaces. | Prevents writing corrupted phantom files, but doesn't fix old database paths. |
| **Internet Gate** | Absent. Playback failed offline cleanly. | Dynamic `isNetworkAvailable()` checks and streams. | Masked path invalidation errors by streaming over internet when online. |

---

## Phase 5 — UI Performance Audit

### Performance Bottlenecks Rank
1. **Synced Lyrics Swapping** (Layout Churn): Font resizing and `AnimatedContent` previously forced massive layout recalculations. (Optimized in current HEAD using GPU `graphicsLayer` scaling).
2. **Progress Ticks** (Recomposition storms): Progress slider updates previously recomposed the entire `NowPlayingScreen` every 500ms. (Optimized by extracting `SeekBarSection` and passing `State<Long>`).
3. **composed Modifiers**: Dynamic shadow painters inside Lazy Columns caused high allocation churn. (Optimized by moving to `claySurfaceNonComposed` helpers).

---

## Phase 6 — Architecture Audit

* **God Class**: `MusicController.kt` (480 lines) manages queue state, sleep timer, network stream resolution, playback controllers, and connectivity gates.
* **Feature Coupling**:
  * Media3 playback engine is tightly coupled with `InnertubeApi` network streaming.
  * Autoplay recommendations are directly handled inside `MusicController` end-of-playback events.
* **Single Source of Truth Violations**:
  * Playlist songs are converted and stored as standard `SongEntity` in the `songs` table. If the song is downloaded *after* adding to a playlist, the playlist entity's path remains `"online://${songId}"`, completely bypassing local offline playback.

---

## Phase 7 — Crash Investigation

* **Swallowed Exceptions**:
  * `resolveLocalSongPathIfMissing` swallows file descriptor errors in a generic `catch (e: Exception) { false }` block, hiding permission and layout faults.
* **Room Migration Risks**:
  * `MIGRATION_2_3` inserts the `cached_lyrics` table. A crash occurs if the database runs on version 3 without the migration code registered.
* **Coroutines Cancellation**:
  * Playback resolution coroutine uses `scope.launch` which is bound to `Dispatchers.Main`. Heavy file existence scans on `Dispatchers.IO` block or delay main execution thread if not properly isolated.

---

## Phase 8 — Refactor & Recovery Plan

To achieve stable, modular V2 architecture, the following phases are recommended:

### V2 Proposed Architecture Diagram
```
[UI Views] 
   │
   ▼
[MainViewModel / ScreenViewModels]
   │
   ▼
[PlaybackRouter / Coordinator]
   ├──► [DownloadedSongRepository] ──► [Room Local Downloads]
   ├──► [OnlineStreamRepository]   ──► [InnerTube API]
   └──► [MediaPlaybackManager]     ──► [Media3 / ExoPlayer]
```

### Action Items:
1. **Decouple MusicController**: Extract network-related stream resolution into a separate `PlaybackRouter` helper class.
2. **Synchronize Playlists**: Sync `SongEntity.path` when a song completes downloading so playlist entries pointing to `"online://${songId}"` are updated to the local download path.
3. **Directory Healing**: Improve fallback scan to automatically update the Room DB path when files are found in the Music directory.
