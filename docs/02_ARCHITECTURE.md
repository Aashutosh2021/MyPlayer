# Architecture & System Design - MyPlayer

This document outlines the detailed system architecture, layer interfaces, component interactions, startup execution paths, and playback lifecycle transitions of the **MyPlayer** application.

---

## 1. High-Level Architecture Overview

MyPlayer is constructed as a **Model-View-ViewModel (MVVM)** application. The system decouples UI composition from business logic, data scanning, network endpoints, and background audio execution.

```mermaid
graph TD
    UI[Jetpack Compose UI] <--> VM[ViewModels]
    VM <--> REPO[Repository Layer]
    REPO <--> ROOM[Room Database]
    REPO <--> API[Innertube API]
    REPO <--> SAF[Storage Access Framework]
    
    MC[MusicController] <--> MS[MusicService Foreground Service]
    MS <--> EP[ExoPlayer Engine]
    EP --> AH[Audio Hardware]
    VM <--> MC
    
    App[Application onCreate] --> SM[SecurityManager]
    SM --> NB[SecurityNativeBridge]
    NB --> CPP[native-lib.cpp C++]
```

---

## 2. Component & Layer Architecture

### UI Layer (Composables & ViewModels)
* **MainScreen / MainViewModel**: Serves as the primary navigation host and mini-player controller. It coordinates global player state and reactive details.
* **NowPlayingScreen**: Renders full-screen details, metadata, timeline slider, and sleep timer overlay.
* **Sub-Screens**: `HomeScreen` (local dashboard), `SearchScreen` (online/local search), `LibraryScreen` (playlists/folders/favorites), and `DownloadsScreen` (sideloaded web audio).
* **Glass/Clay Components**: Custom buttons, progress bars, and navigation items defined in `GlassComponents.kt` utilizing `.claySurface()` modifiers.

### Domain/Repository Layer
* **MusicRepository**: Acts as the offline library gateway. It interacts with `SongDao`, `PlaylistDao`, `FavoriteDao`, `FolderDao`, `RecentHistoryDao`, and orchestrates local directory importing via `MediaScanner`.
* **OnlineSearchRepository**: Handles query dispatching. It couples the remote `InnertubeApi` with the local `RecentSearchDao` cache, presenting results via Jetpack Paging 3.
* **HybridLibraryRepository**: Merges offline songs (`SongEntity`) and online downloads (`DownloadedSongEntity`) into a unified, alphabetical stream of `PlayableSong` items.
* **DownloadRepository**: Drives background workers. It enqueues download tasks into `WorkManager` and tracks download progress states in-memory.

### Data Layer
* **AppDatabase**: The local Room Database containing 8 entity types.
* **InnertubeApi / NewPipeDownloader**: The remote HTTP client interface for YouTube Music query parsing.
* **NewPipeExtractor**: Evaluates YouTube watch links and decodes streaming signatures to extract direct `.m4a` and `.webm` links.

### Playback & Service Layer
* **MusicService**: Extends Media3's `MediaSessionService`. It runs in the foreground with an active notification, managing the `ExoPlayer` instance.
* **MusicController**: Acts as the central singleton manager. It wraps the asynchronous `MediaController` link to `MusicService` and exposes StateFlow flows (`isPlaying`, `currentSong`, `currentPosition`, etc.) to the UI.

---

## 3. Startup and Security Flow

At application launch, MyPlayer executes structural and security integrity verification before letting users stream or access files.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as MyPlayerApplication
    participant SM as SecurityManager
    participant NB as SecurityNativeBridge
    participant NDK as native-lib.cpp (C++)
    participant VM as MainViewModel
    participant UI as Compose UI

    User->>App: Launch App
    App->>SM: onCreate() -> initialize()
    activate SM
    SM->>NDK: warmup Play Integrity / launch runAllChecks() on Dispatchers.IO
    activate NDK
    SM->>NB: runAllNativeChecks()
    NB->>NDK: nativeRunAllChecks()
    Note over NDK: Runs Frida port, maps signatures,<br/>su binaries, and ptrace attach checks.
    NDK-->>NB: Returns Flags Bitmask
    SM->>SM: SignatureVerifier.verify() & TamperDetection.check()
    deactivate NDK
    SM-->>VM: Publish SecurityStatus (Trusted / Suspicious / Compromised)
    deactivate SM
    VM-->>UI: Collect with Lifecycle
    Note over UI: Soft fail approach: Log anomalies and continue.<br/>If Hard-Fail is desired, process terminates here.
```

---

## 4. Key Execution flows

### Online Search & Streaming Request Flow
1. User types in search bar -> debounced flow triggers query.
2. `SearchViewModel` calls `OnlineSearchRepository.search(query)`.
3. `InnertubeSearchPagingSource` executes search against `InnertubeApi` on `Dispatchers.IO`.
4. API executes POST `/search` using decrypted keys from `StringEncryptionManager`.
5. Paging 3 flows list of `OnlineSong` objects back to `SearchScreen`.
6. User clicks play -> `OnlineSearchViewModel.streamSong(song)` is executed.
7. `InnertubeApi.getStreamUrl(videoId)` invokes `NewPipeExtractor.getStreamExtractor()`.
8. Extractor connects, decodes signatures, and returns the direct `.m4a` URL.
9. `MusicController.playOnlineSong(song)` updates the player queue, loads the URL into ExoPlayer, and triggers `.play()`.

```mermaid
sequenceDiagram
    autonumber
    participant UI as SearchScreen
    participant VM as OnlineSearchViewModel
    participant MC as MusicController
    participant API as InnertubeApi
    participant NP as NewPipeExtractor
    participant EP as ExoPlayer (MusicService)

    UI->>VM: Click Song (stream)
    VM->>API: getStreamUrl(videoId)
    API->>NP: ServiceList.YouTube.getStreamExtractor(url)
    activate NP
    NP->>NP: fetchPage() & parse streams
    NP-->>API: List of AudioStreams
    deactivate NP
    API-->>VM: Direct .m4a URL
    VM->>MC: playOnlineSong(songWithUrl)
    MC->>EP: setMediaItem(streamUrl) + prepare() + play()
```

### Background Download Flow
1. User clicks download on an `OnlineSong`.
2. `OnlineSearchViewModel.downloadSong(song)` fetches the direct stream URL.
3. `DownloadRepository.startDownload()` enqueues an **Expedited Work Request** to `WorkManager` using `DownloadWorker`.
4. `DownloadWorker` transitions to a foreground task, displaying a persistent download progress notification.
5. Worker reads custom download directory Uri from `PreferencesManager` (DataStore).
6. Worker makes chunked OkHttp requests to the stream URL using an Android User-Agent header to bypass YouTube's content throttling.
7. Bytes are written to a temporary `.tmp` file.
8. Once download is complete, the file is renamed to `.m4a` inside the destination folder.
9. Worker inserts a new `DownloadedSongEntity` record into the local Room database.
10. `DownloadedSongDao` publishes an updated list flow, which refresh-updates the `DownloadsScreen` UI.

---

## 5. Playback Lifecycle

The audio playback lifecycle is managed by Media3 and Android OS rules:

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Preparing : setMediaItem() / prepare()
    Preparing --> Ready : onPlaybackStateChanged (STATE_READY)
    Ready --> Playing : play() / onIsPlayingChanged(true)
    Playing --> Paused : pause() / onIsPlayingChanged(false)
    Paused --> Playing : play()
    Playing --> Buffering : Network drop / STATE_BUFFERING
    Buffering --> Playing : Buffer filled
    Playing --> Ended : Song complete / STATE_ENDED
    Ended --> Idle : release() / stop()
    Ended --> Playing : repeat / playlist next
    Paused --> Idle
```

* **Foreground Service Promotion**: The `MusicService` transitions to a foreground service when ExoPlayer starts playing, ensuring playback survives background app lifecycle suspensions.
* **Task Swiped Away**: If the user swipes the app away from recent apps, `MusicService.onTaskRemoved()` stops the service if the player is currently paused or ended, avoiding battery leaks.
