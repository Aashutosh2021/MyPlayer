# Deep Architecture

This document expands on the root `ARCHITECTURE.md` file, detailing specific state propagation paths.

## StateFlow Propagation

The application relies entirely on Kotlin `StateFlow` for state management, completely eliminating callbacks or LiveData.

### Database to UI Pipeline
1. **Room DAO:** Returns a `Flow<List<SongEntity>>`. Room automatically emits a new list whenever the underlying SQLite table changes (e.g., when a song is downloaded or a playlist is updated).
2. **Repository:** The `MusicRepository` maps and exposes this `Flow`.
3. **ViewModel:** The `MainViewModel` or `LibraryViewModel` collects this Flow and exposes it as a `StateFlow` using `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)`. The `5000ms` timeout prevents the flow from restarting immediately during configuration changes (like screen rotations).
4. **Compose UI:** The Screen calls `viewModel.songs.collectAsStateWithLifecycle()`. This ensures the UI only observes the database when the Activity is in the `STARTED` state, saving battery when the app is in the background.

## Dependency Injection (Hilt)

Hilt is used to construct the dependency graph.

- **`@Singleton`:** Used for `MusicController`, `RecommendationCoordinator`, and all Repositories. These live for the lifetime of the application.
- **`@HiltViewModel`:** Used for UI controllers. They live for the lifetime of the navigation graph entry.
- **`AppModule.kt`:** Provides the Room Database instance, the Retrofit/OkHttp clients, the DataStore instance, and the Media3 `ExoPlayer` instance.

## Threading Policy

MyPlayer enforces strict Coroutine Dispatcher boundaries:
- **`Dispatchers.Main`:** UI rendering, ViewModel state updates, and all `MediaController` interactions.
- **`Dispatchers.IO`:** Room DB queries, DataStore reads/writes, OkHttp network calls, and file I/O (downloading audio).
- **`Dispatchers.Default`:** Heavy CPU calculations (e.g., parsing large JSON trees, complex Regex compilation).
