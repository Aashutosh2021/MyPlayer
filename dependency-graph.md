# Dependency Graph - MyPlayer

This document maps the critical dependencies and the flow of logic across the application to identify high-impact files and system bottlenecks.

## 1. Core System Flow (Critical Paths)

### Playback Chain (Highest Criticality)
`UI (NowPlaying/MiniPlayer)` $\rightarrow$ `MainViewModel` $\rightarrow$ `MusicController` (Singleton) $\rightarrow$ `MusicService` (MediaSessionService) $\rightarrow$ `ExoPlayer` $\rightarrow$ `Audio Hardware`
- **Impact**: Any bug here results in total audio failure.
- **Critical File**: `MusicController.kt`, `MusicService.kt`.

### Online Discovery Chain
`SearchScreen` $\rightarrow$ `SearchViewModel` $\rightarrow$ `OnlineSearchRepository` $\rightarrow$ `InnertubeSearchPagingSource` $\rightarrow$ `InnertubeApi` $\rightarrow$ `YouTube Music (Remote API)`
- **Impact**: Affects the ability to find new music.
- **Critical File**: `InnertubeApi.kt`.

### Download & Persistence Chain
`DownloadsScreen` $\rightarrow$ `DownloadsViewModel` $\rightarrow$ `DownloadRepository` $\rightarrow$ `WorkManager` $\rightarrow$ `DownloadWorker` $\rightarrow$ `InnertubeApi` (Stream Resolution) $\rightarrow$ `OkHttp` $\rightarrow$ `FileSystem` $\rightarrow$ `DownloadedSongDao` $\rightarrow$ `Room DB`
- **Impact**: Affects offline availability and storage management.
- **Critical File**: `DownloadWorker.kt`.

### Security Initialization Chain
`MyPlayerApplication` $\rightarrow$ `SecurityManager` $\rightarrow$ Various Detection Managers (Frida, Root, Hook, Emulator, etc.) $\rightarrow$ `SecurityNativeBridge` (JNI/C++)
- **Impact**: Dictates if the app is allowed to run. A failure here gracefully crashes the app.
- **Critical File**: `SecurityManager.kt`, `native-lib.cpp`.

---

## 2. Dependency Hierarchy

### Dependency Injection (Hilt)
- **Module $\rightarrow$ Implementation**:
    - `DatabaseModule` $\rightarrow$ `AppDatabase` $\rightarrow$ `DAOs`.
    - `NetworkModule` $\rightarrow$ `OkHttpClient` $\rightarrow$ `InnertubeApi`.
    - `PlayerModule` $\rightarrow$ `ExoPlayer` $\rightarrow$ `MusicController`.
- **Repository $\rightarrow$ Data Source**:
    - `MusicRepository` $\rightarrow$ `SongDao`, `PlaylistDao`, `FavoriteDao`, `FolderDao`.
    - `OnlineSearchRepository` $\rightarrow$ `InnertubeApi`, `RecentSearchDao`.
    - `HybridLibraryRepository` $\rightarrow$ `SongDao`, `DownloadedSongDao`.

---

## 3. High Impact Files
These files are "core" and should not be modified lightly as they affect multiple systems.

| File | Role | Risk of Modification |
| :--- | :--- | :--- |
| `InnertubeApi.kt` | API Protocol & Stream Resolver | High - Can break all online features. |
| `MusicController.kt` | State Bridge for Playback | High - Can cause UI/Playback desync. |
| `AppDatabase.kt` | Schema Definition | High - Requires migrations; affects all local data. |
| `SecurityManager.kt` | Security entry point | High - Mistakes can lead to false-positive crashes or vulnerable apps. |
| `StringEncryptionManager.kt`| Decrypts API Keys and URLs | High - Failing to XOR correctly breaks the entire network layer. |
| `MainScreen.kt` | Root Navigation & Layout | Medium - Affects app-wide navigation and overlay. |
| `DownloadWorker.kt` | File I/O & Stream Saving | Medium - Can cause storage leaks or corrupted files. |

## 4. Critical File Interdependence
- **UI $\rightarrow$ MusicController**: Every screen that interacts with playback depends on `MusicController`.
- **Repositories $\rightarrow$ DAOs**: All data access is gated through DAOs.
- **DownloadWorker $\rightarrow$ InnertubeApi**: The worker depends on the API's ability to resolve a stream URL before it can start downloading.