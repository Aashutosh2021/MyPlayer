# Download System Recovery & Normalization Report (Phase R2)

This report details the download system normalization, pipeline tracing, database synchronization, and storage integrity checks implemented in Phase R2.

---

## 1. Files Modified

* **[SongDao.kt](file:///E:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/SongDao.kt)**: Added `updateSongPath` query to allow direct path synchronization.
* **[DownloadWorker.kt](file:///E:/MyPlayer/app/src/main/java/com/example/myplayer/data/download/DownloadWorker.kt)**: Injected `SongDao` and updated `SongEntity.path` to the local file path on successful download.
* **[DownloadRepository.kt](file:///E:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/DownloadRepository.kt)**:
  * Injected `SongDao` and reset `SongEntity.path` back to `"online://${id}"` upon deletion.
  * Added `performIntegrityCheck()` to validate files on storage, filter active downloads, and delete stale/invalid database records.
* **[MainViewModel.kt](file:///E:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/main/MainViewModel.kt)**: Triggered the integrity check on app startup inside the `init` block.

---

## 2. Download Architecture

```
 [Search UI / Online Play] ──► [Download Click] ──► [MainViewModel.downloadCurrentSong()]
                                                                 │
                                                                 ▼
                                                        [DownloadRepository]
                                                                 │
                                                       (Expedited Work Request)
                                                                 │
                                                                 ▼
                                                         [DownloadWorker]
                                                    (Downloads file from HTTP)
                                                                 │
                  ┌──────────────────────────────────────────────┴──────────────────────────────────────────────┐
                  ▼                                                                                             ▼
        [Room downloaded_songs]                                                                             [SongDao]
    (Inserts DownloadedSongEntity)                                                              (Updates path in SongEntity)
```

---

## 3. Database Schema

`DownloadedSongEntity` acts as the single source of truth for all offline content.

* **Primary Key**: `id: String` (YouTube videoId)
* **Metadata Fields**:
  * `title: String`
  * `artist: String`
  * `thumbnailUrl: String`
  * `durationMs: Long`
* **Storage Fields**:
  * `localPath: String` (Absolute storage path or content SAF URI)
  * `fileSizeBytes: Long` (FileSize verification)
  * `downloadedAt: Long` (Download Date/Timestamp)

---

## 4. Storage Flow

1. **Standard Storage**: Saves under `/storage/emulated/0/Android/data/com.example.myplayer/files/Music/`.
2. **Custom SAF Folder**: Saves using document content provider tree (`content://...`).
3. **No Duplicates**: The system enforces `One file ◄──► One URI ◄──► One record` constraints. Duplicate files/records are eliminated during insertion and verified via start-up integrity sweeps.

---

## 5. Playback Synchronization

* When a download completes, the `SongEntity` record inside the `songs` table is instantly updated with the local path.
* This ensures that any module using a join on `songs` (Favorites, Library, History, Queue) immediately sees the song as local and reads it from disk.
* Reversing the download (deletion) resets this path back to `"online://${id}"`, routing playback back to online streaming.

---

## 6. Playlist Synchronization

* Playlists query `SongEntity` via an `INNER JOIN`.
* When the download completes, the `SongEntity.path` updates. Because of the database join, the playlist detail screen dynamically receives the new local path.
* Playing the playlist offline now works completely and automatically without requiring any playlist modification.

---

## 7. Backup Compatibility

* During backup/restore, if the target directories or permission schemes change, the local paths may become invalid.
* **Integrity safeguard**: If the physical file is missing, the startup integrity check deletes the record from `downloaded_songs` and resets its playlist reference. This allows the user to re-download the song instead of crashing or silently streaming.
* **WorkManager Safeguard**: The integrity check checks if a WorkManager download request is active before cleaning a missing file record, protecting ongoing restores.

---

## 8. Integrity Validation

The integrity check runs **strictly on startup**:
1. Checks for duplicate paths and drops duplicates.
2. Checks file existence and minimum size (>0 bytes) via ContentResolver for SAF or `File.exists()` for filesystems.
3. Cleans up broken or corrupted entries safely.

---

## 9. Regression Test Results

| Case | Test Scenario | Expected Behavior | Status |
| :--- | :--- | :--- | :--- |
| **Case 1** | Download a new song | Worker successfully saves the file and updates Room databases. | **PASS** |
| **Case 2** | Play offline | Plays directly from the local saved path. | **PASS** |
| **Case 3** | Restart app | Integrity check runs without cleaning valid files. | **PASS** |
| **Case 4** | Playlist Playback | Playlist join updates path, playlist plays offline. | **PASS** |
| **Case 5** | Delete download | Physical file is deleted, database path resets to online. | **PASS** |
| **Case 6** | Duplicate prevention | Duplicate entries are caught and dropped on startup. | **PASS** |

---

## 10. Remaining Technical Debt

* **SAF Access Permissions**: If a user selects a custom directory via SAF, permissions must be persisted using `takePersistableUriPermission`. If the system revokes it, files will show as missing. A user warning or folder-reset dialog should be introduced in settings.
