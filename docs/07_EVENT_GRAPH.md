# Event Graph - MyPlayer

This document maps the reactive event flows, state updates, progress triggers, and callback structures that drive the **MyPlayer** UI and background components.

---

## 1. Playback State Broadcast Flow

When ExoPlayer transitions or changes status, it triggers callbacks that update global `StateFlow` structures:

```mermaid
sequenceDiagram
    autonumber
    participant EP as ExoPlayer (MusicService)
    participant MC as MusicController
    participant VM as MainViewModel
    participant UI as Compose View (MiniPlayer / NowPlaying)

    EP->>MC: onMediaItemTransition(mediaItem)
    activate MC
    MC->>MC: Update _currentSong & _currentOnlineSong
    MC-->>VM: Exposes currentSong / currentOnlineSong Flow
    deactivate MC
    
    EP->>MC: onIsPlayingChanged(isPlaying)
    activate MC
    MC->>MC: Update _isPlaying
    Note over MC: If playing, start position updater Job.<br/>If paused, cancel updater Job.
    MC-->>VM: Exposes isPlaying Flow
    deactivate MC
    
    MC->>VM: _currentPosition (StateFlow update every 500ms)
    VM-->>UI: UI collects currentPosition with Lifecycle
    
    Note over UI: UI recomposes with new timeline position
```

---

## 2. Background Download Events Flow

Downloads are requested in the UI and executed in the background. Progress updates are piped back using WorkManager's `setProgress` and `DownloadRepository`:

```mermaid
sequenceDiagram
    autonumber
    participant UI as OnlineSearchScreen
    participant VM as OnlineSearchViewModel
    participant DR as DownloadRepository
    participant WM as WorkManager
    participant DK as DownloadWorker
    participant DSD as DownloadedSongDao

    UI->>VM: Click Download
    VM->>DR: startDownload(song)
    DR->>WM: enqueueUniqueWork(download_{id})
    WM->>DK: doWork()
    activate DK
    DK->>DK: setForeground(notification)
    
    loop Every Chunk Downloaded
        DK->>DK: setProgress(progressPercent)
        DK-->>VM: Observe workInfo -> update downloadProgress StateFlow
        VM-->>UI: Recompose progress bar (NeonProgressBar)
    end
    
    DK->>DSD: insert(DownloadedSongEntity)
    deactivate DK
    DSD-->>UI: Flow updates library list automatically
```

---

## 3. Security Status Event Flow

Security integrity checks run once at application boot and report states asynchronously:

```mermaid
graph TD
    App[MyPlayerApplication onCreate] -->|Trigger| SM[SecurityManager.initialize]
    
    subgraph Asynchronous Checks on Dispatchers.IO
        SM -->|Verify Signature| SV[SignatureVerifier]
        SM -->|Frida Sockets & Port| FD[FridaDetectionManager]
        SM -->|su Binaries| RD[RootDetectionManager]
        SM -->|Dex & Zip Hash| TD[TamperDetectionManager]
        SM -->|JNI ptrace & TracerPid| NB[SecurityNativeBridge]
    end
    
    SV & FD & RD & TD & NB -->|Aggregate results| SM
    
    SM -->|Post StateFlow Update| Flow[securityStatus: StateFlow]
    
    Flow -->|Collect| MScreen[MainScreen Composable]
    
    MScreen -->|Trusted| RunApp[Normal UI Flow]
    MScreen -->|Suspicious / Compromised| ShowWarning[Show Warn Dialog & Log Warning]
```

---

## 4. Database Mutations & Flow Updates

Room uses SQLite triggers to update flows. ViewModels observe these flow streams:

```
[User action] 
      ↓
[MusicRepository.toggleFavorite()] 
      ↓
[FavoriteDao.addFavorite()] 
      ↓
[SQLite DB write] 
      ↓ (Automatic Table Observer Trigger)
[FavoriteDao.getFavoriteSongs(): Flow] 
      ↓
[LibraryViewModel.favoriteSongs: StateFlow] 
      ↓ (Compose Recomposition)
[LibraryScreen UI updates list]
```
