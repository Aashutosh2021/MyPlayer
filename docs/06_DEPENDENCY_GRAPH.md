# Dependency Graph - MyPlayer

This document visualizes the class, package, database, and dependency injection relationships within the **MyPlayer** codebase.

---

## 1. Class-Level Structural Dependency Graph

This diagram shows how major classes depend on one another. Arrows denote "depends on" or "references" direction.

```mermaid
graph TD
    App[MyPlayerApplication] --> SM[SecurityManager]
    App --> AA[AudioAlbumArtFetcher]
    
    MainActivity --> Theme[Theme.kt]
    MainActivity --> MScreen[MainScreen]
    MScreen --> FloatingNavBar
    MScreen --> MiniPlayer
    MScreen --> Nav[Screen Navigation Router]
    
    Nav --> HomeScreen
    Nav --> SearchScreen
    Nav --> LibraryScreen
    Nav --> DownloadsScreen
    Nav --> NowPlayingScreen
    Nav --> PlaylistDetailScreen
    
    HomeScreen --> HVM[HomeViewModel]
    SearchScreen --> SVM[SearchViewModel]
    SearchScreen --> OSVM[OnlineSearchViewModel]
    LibraryScreen --> LVM[LibraryViewModel]
    DownloadsScreen --> DVM[DownloadsViewModel]
    NowPlayingScreen --> MVM[MainViewModel]
    PlaylistDetailScreen --> LVM
    
    HVM --> MRepo[MusicRepository]
    LVM --> MRepo
    LVM --> HLibRepo[HybridLibraryRepository]
    DVM --> DRepo[DownloadRepository]
    OSVM --> OSRepo[OnlineSearchRepository]
    OSVM --> DRepo
    OSVM --> API[InnertubeApi]
    MVM --> MC[MusicController]
    MVM --> MRepo
    
    MC --> MRepo
    MC --> MServ[MusicService]
    MServ --> Exo[ExoPlayer Engine]
    
    OSRepo --> API
    OSRepo --> RSD[RecentSearchDao]
    
    HLibRepo --> SD[SongDao]
    HLibRepo --> DSD[DownloadedSongDao]
    
    DRepo --> DSD
    DRepo --> DW[DownloadWorker]
    DW --> PM[PreferencesManager]
    DW --> DSD
    
    MRepo --> SD
    MRepo --> PLD[PlaylistDao]
    MRepo --> FD[FavoriteDao]
    MRepo --> FLD[FolderDao]
    MRepo --> RHD[RecentHistoryDao]
    MRepo --> Scanner[MediaScanner]
    
    Scanner --> SD
    Scanner --> FLD
    
    SM --> SV[SignatureVerifier]
    SM --> RD[RootDetectionManager]
    SM --> Frida[FridaDetectionManager]
    SM --> ED[EmulatorDetectionManager]
    SM --> HD[HookDetectionManager]
    SM --> AD[AntiDebugManager]
    SM --> TD[TamperDetectionManager]
    SM --> NB[SecurityNativeBridge]
    SM --> IM[IntegrityManager]
    
    API --> OkHttp[OkHttpClient]
    API --> SEM[StringEncryptionManager]
```

---

## 2. Dependency Injection Injections (Hilt)

Hilt manages the instantiation of core application singletons and databases. The graph below maps how modules supply types to classes.

```mermaid
graph LR
    subgraph DatabaseModule
        DB[AppDatabase] --> SD[SongDao]
        DB --> PLD[PlaylistDao]
        DB --> FD[FavoriteDao]
        DB --> FLD[FolderDao]
        DB --> RHD[RecentHistoryDao]
        DB --> DSD[DownloadedSongDao]
        DB --> RSD[RecentSearchDao]
    end

    subgraph NetworkModule
        Ok[OkHttpClient] --> API[InnertubeApi]
    end

    subgraph PlayerModule
        Exo[ExoPlayer] --> MC[MusicController]
    end

    SD & PLD & FD & FLD & RHD --> MRepo[MusicRepository]
    DSD --> HLibRepo[HybridLibraryRepository]
    DSD --> DRepo[DownloadRepository]
    RSD & API --> OSRepo[OnlineSearchRepository]
    
    MRepo & HLibRepo & DRepo & OSRepo & MC --> ViewModels
```

---

## 3. SQLite Schema Relations (Room Database)

This graph displays the foreign keys and cross-references of the local Room Database tables.

```mermaid
erDiagram
    songs {
        TEXT id PK
        TEXT title
        TEXT artist
        TEXT album
        INTEGER duration
        TEXT path
        TEXT albumArt
        INTEGER dateAdded
        INTEGER playCount
    }
    downloaded_songs {
        TEXT id PK
        TEXT title
        TEXT artist
        TEXT thumbnailUrl
        INTEGER durationMs
        TEXT localPath
        INTEGER fileSizeBytes
        INTEGER downloadedAt
    }
    playlists {
        INTEGER id PK
        TEXT name
        INTEGER createdAt
    }
    playlist_song_cross_ref {
        INTEGER playlistId FK
        TEXT songId FK
        INTEGER position
    }
    favorites {
        TEXT songId PK
        INTEGER addedAt
    }
    folders {
        TEXT uri PK
        TEXT name
    }
    recent_history {
        TEXT songId PK
        INTEGER playedAt
    }
    recent_searches {
        TEXT query PK
        INTEGER timestamp
    }

    playlists ||--o{ playlist_song_cross_ref : contains
    songs ||--o{ playlist_song_cross_ref : mapped
    songs ||--o| favorites : marked
    songs ||--o{ recent_history : played
    folders ||--o{ songs : contains
```
