# MyPlayer V2 - Regression Investigation & Root Cause Analysis

This report documents the detailed investigation and root cause analysis (RCA) comparing the reference commit `57bdbff925c2420c9d95bc6f3f7c2489889b22fd` (Version 1.2v) with the current workspace `HEAD`.

---

## 1. Classifications of Modifications (Step 1)

All changes between the baseline and the current HEAD are grouped into the following functional areas:

### Playback & Media3
* **MusicController.kt**:
  * Modified `playSongs` to asynchronously pre-resolve queue URIs.
  * Added `resolveLocalSongPathIfMissing` method to dynamically check file availability and fallback to streaming when files are missing.
  * Added `onPlayerError` listener in ExoPlayer to handle source errors (`PlaybackException`) by skipping to the next song instead of freezing.
  * Injected `InnertubeApi` to fetch streaming URLs when local files are missing.
  * Added network connectivity gate `isNetworkAvailable()` using `ConnectivityManager`.

### Downloads
* **DownloadWorker.kt**:
  * Added size check validation (`fileSize <= 0L`) to reject corrupt/empty downloads before inserting into database.
  * Added title double-space collapsing logic `.replace(Regex(" {2,}"), " ")` to resolve naming conflicts.
  * Integrated automatic background lyrics caching on successful download using `LyricsRepository.fetchAndCacheLyrics`.

### Storage & Room
* **BackupRestoreManager.kt [NEW]**:
  * Added JSON-based serialization/deserialization for settings, folders, songs, favorites, playlists, and cached lyrics.
  * Handles database clearing/insertion in a single database transaction.
  * Automatically schedules expedited WorkManager downloads for missing local files when restoring a backup.
* **AppDatabase.kt**:
  * Incremented database version to `3`.
  * Added `MIGRATION_2_3` to create the `cached_lyrics` table.
* **DatabaseModule.kt**:
  * Registered `MIGRATION_2_3` and provided `CachedLyricsDao` dependency.
* **DownloadedSongDao.kt**:
  * Added `getAllDownloadsSync()` and `insertDownloads(List<DownloadedSongEntity>)` for backup operations.

### Lyrics Subsystem
* **LyricsRepository.kt**:
  * Refactored to query SQLite cache (`CachedLyricsDao`) before requesting the API.
  * Integrated a Levenshtein-based metadata similarity check (`calculateMetadataScore`) evaluating title, artist, album, and duration to ensure high-confidence matches.
* **LyricsTab.kt**:
  * Refactored `LyricsTab` to accept `positionMs: State<Long>` instead of raw values, preventing whole-screen recomposition.
  * Replaced `AnimatedContent` font-swapping with low-overhead GPU-accelerated `graphicsLayer` scaling.
* **LyricsViewModel.kt**:
  * Modified to collect current song details into a typed `LyricsMetadata` data class.

### ViewModels & Settings
* **MainViewModel.kt**:
  * Exposed `playbackError` StateFlow and `clearPlaybackError()` to notify users when disk files are missing.
* **SettingsViewModel.kt**:
  * Added `exportBackup(Uri)` and `importBackup(Uri)` triggers updating `BackupRestoreState`.

### Compose UI & Themes
* **Modifiers.kt**:
  * Refactored `claySurface` into a non-composed `claySurfaceNonComposed` counterpart to avoid recomposition overhead.
  * Added safety constraints (`.coerceAtLeast(1f)`) to avoid crashes when drawing blurs with radius <= 0.
* **NowPlayingScreen.kt**:
  * Extracted `SeekBarSection` to encapsulate progress updates and slider seeking, limiting recompositions to that specific area.
* **MainScreen.kt**:
  * Added LaunchedEffect displaying one-shot Toast notifications for playback errors.
  * Allowed Settings navigation in bottom navbar.

---

## 2. Download Regression Analysis (Step 2)

### Comparison of Download Flows

```
[Old Flow - Commit 57bdbff]
Download Button → DownloadWorker → Save to App DIRECTORY_MUSIC (.m4a) → Insert to downloaded_songs (localPath) → Play via localPath directly in ExoPlayer (Fully Offline)

[Current Flow - HEAD]
Download Button → DownloadWorker (validates size, collapses title spaces) → Save (.m4a) & Fetch Lyrics → Insert to downloaded_songs (localPath) → Play via playSongs() → resolveLocalSongPathIfMissing() → If missing, check network → Stream from YouTube
```

### Key Findings & Answers:
1. **Downloaded File Location**: Downloaded files are saved to `context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)` or a user-selected Storage Access Framework (SAF) folder URI.
2. **Local Path Persistence**: Room stores the absolute path (e.g. `/storage/emulated/0/Android/data/com.example.myplayer/files/Music/SongName.m4a`) or content URI (e.g. `content://com.android.externalstorage.documents/...`) in `DownloadedSongEntity.localPath`.
3. **Persisted Values in Room**:
   * `localPath`: Stores the absolute file path or content URI.
   * `youtubeId`: Saved in the `id` Primary Key column.
   * `streamUrl`: **Not stored** in the database (resolved dynamically).
4. **What broke after commit 57bdbff?**
   * **Path Invalidation on Restore**: On backup restore or app reinstall, the absolute path to external files or the SAF URI permissions become invalid. The database holds old paths, causing file checks to fail.
   * **Title Stripping Bugs**: The title-stripping sanitization of characters like `|` left double-spaces in the path string stored in Room while the actual file on disk had single spaces, causing immediate `ENOENT` (File Not Found) failures.

---

## 3. Playback Regression Analysis (Step 3)

### Playback Pipelines

```
[Old Pipeline - Commit 57bdbff]
Play Downloaded Song → playDownloadedSong() → SongEntity(path = localPath) → playSongs() → MediaItem.fromUri(path) → ExoPlayer (Instant play or direct error)

[Current Pipeline - HEAD]
Play Downloaded Song → playDownloadedSong() → SongEntity(path = localPath) → playSongs() → resolveLocalSongPathIfMissing()
                                                                                               │
                                            ┌──────────────────────────────────────────────────┴──────────────────────────────────────┐
                                     (File Exists)                                                                             (File Missing/Inaccessible)
                                            │                                                                                         │
                                     Play Local File                                                                          Check Internet
                                                                                                      ┌───────────────────────┴───────────────────────┐
                                                                                                   (Online)                                       (Offline)
                                                                                                      │                                               │
                                                                                             Get YouTube Stream URL                         Return Original Path
                                                                                             (Plays online over Wi-Fi/Data)                 (ExoPlayer ENOENT Failure)
```

### Why Downloaded Songs Resolve a Network Stream:
When `resolveLocalSongPathIfMissing` checks `song.path` and finds the file is missing or inaccessible (due to double-space title mismatch, missing SAF permissions, or absolute path changes after a restore), it does not fail immediately. Instead, if there is an active internet connection, it queries YouTube's InnerTube API to resolve the song's stream URL.
* **The User's Perspective**: The song plays fine when connected, but consumes internet data.
* **The Offline Failure**: When mobile data/Wi-Fi is turned off, this network stream resolution fails, and the app is forced to pass the original (invalid) local path to ExoPlayer, resulting in an immediate playback failure or crash.

---

## 4. Playback Entry Points & Call Graph (Step 4)

Here is the complete routing for playback initialization:

```
[UI Screen Interactions]
  │
  ├──► DownloadsScreen ───────► DownloadsViewModel.playSong() ────► MusicController.playDownloadedSong() ─┐
  │                                                                                                        │
  ├──► LibraryScreen ─────────► MainViewModel.playSong() ────────► MusicController.playSong() ────────────┼─► MusicController.playSongs() ─► resolveLocalSongPathIfMissing() ─► ExoPlayer
  │                                                                                                        │
  ├──► PlaylistDetailScreen ──► MainViewModel.playPlaylist() ────► MusicController.playPlaylist() ────────┘
  │
  └──► Autoplay / End of Song ──► handlePlaybackEnded() ─────────► MusicController.playOnlineSong() ────────► ExoPlayer
```

---

## 5. Storage Validation (Step 5)

* **File Existence**: The physical files do exist under the `Music` directory, but names can contain double spaces from legacy downloads (e.g. `"Baarish  Yaariyan.m4a"`).
* **Path Alignment**: If a playlist contains a song added while online, its `SongEntity.path` is stored as `"online://${songId}"`. Even if the song is downloaded later, the playlist row's path is never updated, forcing a network check during playback.
* **SAF Permissions**: Restoring a backup with custom SAF directories does not restore the directory read/write permission grants, making the files inaccessible even if they are physically present.

---

## 6. Recommendation Audit (Step 6)

* **Verification**: `RecommendationCoordinator` only acts as a seed listener on `onPlaybackStarted()`.
* **Accidental Coupling**: **None.** The recommendation engine does not intercept, modify, or reroute the playback URIs. The playback routing regression is entirely situated within the `resolveLocalSongPathIfMissing` method inside `MusicController.kt`.

---

## 7. UI Performance Audit (Step 7)

A comparison against the reference commit shows that the current HEAD contains **significant UI optimizations**:
1. **No Screen Recomposition on Ticks**: Extracted `SeekBarSection` and passed `State<Long>` to `NowPlayingScreen` and `LyricsTab`, isolating 500ms position updates to the smallest possible widgets.
2. **Smooth Lyrics Scrolling**: Synced lyrics scroll animations are debounced using `derivedStateOf` and `snapshotFlow` with binary search, resolving list jitter.
3. **No Churn scale transitions**: Uses GPU-accelerated `.graphicsLayer` scale changes rather than `AnimatedContent` size swaps to highlight active lyrics.
4. **Static UI Modifier Cache**: Replaced `composed` modifiers with `claySurfaceNonComposed` for cards and static buttons, reducing allocation churn.

---

## 8. Crash Investigation & Risk Report (Step 8)

| Risk Area | Code Location | Exception | Threat Level | Root Cause / Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **Playback** | `MusicController.kt` | `FileNotFoundException` / `ENOENT` | **HIGH** | Local file is missing, or permission is revoked, and device is offline. |
| **Database Restore** | `BackupRestoreManager.kt` | `IOException` | **MEDIUM** | Attempting to restore backup JSON offline throws network exceptions when resolving fresh streams. |
| **Media3 Engine** | `MusicService.kt` | `IllegalStateException` | **MEDIUM** | State transition triggers duplicate autoplay actions during buffer under-runs. |

---

## 9. Recommended Recovery & Architecture Plan (Step 9)

To ensure reliable, 100% offline playback of downloaded tracks, the following adjustments are recommended for the next phase:

### Phase 1: Local Path Normalization & DB Sync
* Update `PlaylistDetailViewModel` and other views to check if a song's `videoId` is present in the `downloaded_songs` table before choosing playback routing.
* If a song is marked as downloaded, always play its local file URI rather than its legacy `"online://"` path.

### Phase 2: Self-Healing File Resolution
* When checking if a file exists, if the stored absolute path is invalid (e.g. due to package reinstalls or backup restores), check the default `Music` directory for files matching the `videoId` to automatically repair the path in the database.

### Phase 3: Expedited Offline Errors
* If the file is missing and the device is offline, immediately show the user a Toast warning ("File missing. Please connect to download.") rather than letting ExoPlayer hang or try to resolve network endpoints.
