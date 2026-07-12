# Crash & Exception Safety Audit Report (Phase R9)

This audit documents exception handling, safety bounds, nullability protection, and resource leaks across **MyPlayer V2** following structural refactorings.

---

## 1. Safety Bounds & Crash Check Results

We searched the codebase for common Kotlin/Android exception triggers to ensure maximum safety.

### 1.1 Double-Bang Operator (`!!`) Safety Check
All double-bang usages were audited to confirm zero risk of `NullPointerException`:
- **`NowPlayingScreen.kt:182`** (`title!!`): Executed only when `hasSong` is `true`. `hasSong` is defined as `!title.isNullOrBlank()`, guaranteeing `title` is non-null. **SAFE**.
- **`SettingsScreen.kt:109`** (`showDialogText!!`): Executed inside the composable row when `showDialogText` is not null. **SAFE**.
- **`DownloadWorker.kt:193`** (`customFolderUriString!!`): Executed when `hasCustomFolder` is `true`, which is evaluated via `!customFolderUriString.isNullOrEmpty()`. **SAFE**.
- **`LyricsRepository.kt:339`** (`targetAlbum!!`, `candidateAlbum!!`): Smart cast wrapper checks `!targetAlbum.isNullOrBlank()` first. **SAFE**.

### 1.2 Checked Exception (`throw`) Safety Check
All occurrences of `throw` statements are wrapped inside try-catch scopes or handled safely:
- **`OnlineSearchRepository`** (`throw IOException(...)` in Backup): Caught in the calling viewmodel to show the user a ClayCard dialog containing the failure text instead of crashing the process. **SAFE**.
- **`InnertubeApi.kt`** (`throw YouTubeStreamBlockedException`): Propagated to the `PlaybackSourceResolver`. Resolved as a user-facing string error ("Failed to load audio stream. Please check connection.") via the `PlaybackErrorHandler`, stopping playback gracefully without application crash. **SAFE**.
- **`DownloadWorker.kt`** (`throw IOException("Stopped by user")`): Captured inside `doWork()`'s outer try-catch. Changes the WorkManager worker state to `Result.failure()` and cleans up temp disk chunks. **SAFE**.

---

## 2. Thread Safety & ANR Prevention

1. **Strict Thread Segregation**:
   - Media3 player commands (`play`, `pause`, `seek`, `setMediaItems`) are strictly bound to `Dispatchers.Main` inside `MusicController`.
   - File IO operations (`DownloadWorker` chunk buffers, backup writing, and scanned directory traversals) are executed on `Dispatchers.IO`.
   - SQLite queries (DAO operations) are run on Room's managed thread pools or called inside `withContext(Dispatchers.IO)`.
   - Heavy similarity matches (Levenshtein distances) are run on `Dispatchers.Default` to prevent frame skips in Jetpack Compose layout threads.

2. **Coroutine Job Leak Prevention**:
   - `MusicController` coroutine scope uses `SupervisorJob()`. When the controller lifecycle is destroyed, all child updater jobs (position tracker, event collectors) are cancelled.
   - `PlaybackRouter` implements active resolution job tracking (`activePlayJob`). A new play request immediately calls `activePlayJob?.cancel()`, terminating any ongoing queue resolution thread loops.

---

## 3. Database Transaction & Foreign Key Safety

- All playlist updates and favorites insertions are executed in Room within transactional scopes.
- Foreign key dependencies (`songs` table constraints on playlists) are satisfied using automated stub mappings (`toSongEntity()`) in `MusicItemMapper.kt` prior to database writes.
