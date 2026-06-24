# System Architecture - MyPlayer

## 1. High-Level Layered Architecture
MyPlayer follows the **MVVM (Model-View-ViewModel)** pattern. The architecture is strictly layered to ensure a separation of concerns between the UI, business logic, and data sources.

### Layer Map
`UI Layer (Compose)` $\rightarrow$ `ViewModel Layer` $\rightarrow$ `Repository Layer` $\rightarrow$ `Data Source Layer`

#### UI Layer
- **Responsibility**: Rendering the state provided by ViewModels and capturing user interactions.
- **Key Components**: `MainScreen`, `HomeScreen`, `NowPlayingScreen`, `FloatingNavBar`, `MiniPlayer`.
- **Communication**: Observes `StateFlow` from ViewModels; calls ViewModel functions for actions.

#### ViewModel Layer
- **Responsibility**: Managing UI state, handling business logic for specific screens, and interacting with repositories.
- **Key Components**: `MainViewModel`, `HomeViewModel`, `SearchViewModel`, `LibraryViewModel`, `DownloadsViewModel`.
- **Communication**: Injected via Hilt; uses Coroutines for asynchronous operations.

#### Repository Layer
- **Responsibility**: Abstracting the data origin (Local DB vs. Online API) and providing a clean API to ViewModels.
- **Key Components**:
    - `MusicRepository`: Manages local songs, favorites, and playlists.
    - `OnlineSearchRepository`: Manages YouTube Music search and search history.
    - `HybridLibraryRepository`: Merges local and downloaded songs into a single `PlayableSong` stream.
    - `DownloadRepository`: Manages the lifecycle of song downloads.

#### Data Source Layer
- **Local**: Room Database (`AppDatabase`) and Preferences DataStore.
- **Remote**: YouTube Music Innertube API via `InnertubeApi`.
- **Resolution**: `NewPipeExtractor` used to resolve obfuscated stream URLs.

---

## 2. Playback Engine Architecture
The playback system is decoupled from the UI to ensure music continues playing when the app is in the background.

### Components
- **MusicService**: A `MediaSessionService` (Media3) that hosts the `ExoPlayer` instance. It is the actual engine that communicates with Android's audio system.
- **MusicController**: A Hilt-injected singleton that acts as a bridge. It wraps the `MediaController` and exposes a simplified `StateFlow` API for the UI.
- **ExoPlayer**: The low-level player handling different stream formats (DASH, HLS, Progressive).
- **DSP System**: A set of managers (`EqualizerManager`, `LoudnessManager`, etc.) that apply audio effects to the ExoPlayer's audio track.

### Playback Data Flow
`UI` $\rightarrow$ `MusicController.playSong(id)` $\rightarrow$ `MusicService` $\rightarrow$ `ExoPlayer` $\rightarrow$ `Audio Hardware`

---

## 3. Download & Sync Architecture
The download system is designed to be resilient, using a combination of a foreground service and background workers.

### Workflow
1. **Request**: `DownloadRepository` triggers a `WorkManager` request.
2. **Orchestration**: `DownloadWorker` is scheduled.
3. **Execution**:
    - `DownloadWorker` fetches the stream URL via `InnertubeApi`.
    - It streams the file via `OkHttp` to a temporary file.
    - Once complete, it renames the file to the final destination.
4. **Persistence**: The `DownloadedSongEntity` is inserted into the Room DB.
5. **Keep-Alive**: `DownloadService` is used as a foreground service to prevent the OS from killing the `DownloadWorker` during large file transfers.

---

## 4. Technology Dependency Graph
- **Hilt**: Manages the lifecycle and injection of all repositories, ViewModels, and the `MusicController`.
- **Room**: Provides a reactive data layer via `Flow`.
- **Paging 3**: Handles the efficient loading of large online search results.
- **Media3**: Standardizes media session and playback across Android versions.