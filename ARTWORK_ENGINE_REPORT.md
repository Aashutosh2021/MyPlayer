# Artwork Engine Architecture & Engineering Report

## Executive Summary
A high-performance, clean-room **Artwork Engine** was implemented for **MyPlayer**, enabling high-resolution album and song artwork resolution across multiple external and local providers. 

The engine implements a strict multi-tier fallback pipeline:
$$\text{Local / Embedded Art} \longrightarrow \text{Deezer} \longrightarrow \text{iTunes} \longrightarrow \text{YouTube Music} \longrightarrow \text{Placeholder}$$

All concepts were designed and written cleanly from first principles to integrate natively with MyPlayer's existing Dagger Hilt dependency graph, Coil `ImageLoader`, and Room database without any license contamination (no copyleft/GPL code) and without modifying the Room database schema.

---

## 1. Existing Artwork System Found
Prior to this implementation, MyPlayer handled artwork through disparate mechanisms:
1. **Local Media**: `AudioAlbumArtFetcher.kt` read embedded artwork via `contentResolver.loadThumbnail()` (Android Q+) and fallback `MediaMetadataRetriever.embeddedPicture`.
2. **Downloaded & Online Media**: Only YouTube standard/low-res thumbnails (`hqdefault.jpg` or `w226-h226` Google CDN thumbnails) were stored in `thumbnailUrl`.
3. **UI Display**: `AlbumArtImage.kt` received an optional `uri: String?` directly. If the URI was absent, corrupted, or low-resolution, no automatic lookup occurred, displaying an empty icon container.
4. **No Resolution Engine**: There was no unified service to search or retrieve studio-grade album covers (1000x1000 or 1400x1400) from music databases.

---

## 2. Architecture & Components Implemented

### A. Core Contracts & Data Models (`data/artwork/model` & `data/artwork/provider`)
- **`ArtworkResult`**: Holds the resolved high-res URL (`url`), the source provider name (`provider`), and nominal pixel dimensions (`width`, `height`).
- **`ArtworkModel`**: Request model encapsulating `title`, `artist`, optional `album`, and optional `localUri`. Enables Coil to intercept artwork requests directly.
- **`ArtworkProvider`**: Contract interface exposing `name`, `priority`, and `suspend fun fetchArtwork(title, artist, album): ArtworkResult?`.

### B. External Providers (`data/artwork/provider`)
1. **`DeezerArtworkProvider` (Priority 1)**:
   - Queries `https://api.deezer.com/search`.
   - Prioritizes `cover_xl` (1000x1000), falling back to `cover_big` (500x500).
   - Validates candidate title and artist matches with variant protection.
   - Built-in rate limit handling (HTTP 429 and JSON error quotas) with an automatic 60-second circuit-breaker backoff.
2. **`ITunesArtworkProvider` (Priority 2)**:
   - Queries `https://itunes.apple.com/search?term=...&entity=song&limit=5`.
   - Upgrades standard `artworkUrl100` (`.../100x100bb.jpg`) to `1000x1000bb.jpg` for master-quality artwork directly from Apple's CDN.
   - Validates candidate title and artist matches.
   - High availability worldwide with zero API keys required.
3. **`YouTubeMusicArtworkProvider` (Priority 3)**:
   - Queries YouTube Music via the injected `InnertubeApi`.
   - Upgrades thumbnail resolutions (replacing low-res dimension tags with `=w800-h800-l90-rj` or using high-quality video thumbnails).
   - Validates title and artist matches.

### C. Matching & Validation Engine (`data/artwork/matching/ArtworkMatcher`)
To eliminate incorrect covers (e.g. matching a remix, live version, or instrumental when playing studio audio):
- **Sanitization**: Strips noise descriptors (`[Official Video]`, `(Remastered 2011)`, `(Audio)`, `| 4K MV`), delimiter suffixes, and channel noise (`- Topic`, `VEVO`, `Official`).
- **Variant Protection**:
  - Rejects candidate remixes (`remix`, `club mix`, `extended mix`) unless the search query is explicitly a remix.
  - Rejects live recordings (`live at`, `acoustic`, `in concert`) unless the search query is explicitly live.
  - Rejects instrumental/karaoke versions when not requested.
- **Confidence Scoring**: Calculates normalized Levenshtein similarity across cleaned titles (60% weight) and artists (40% weight). Enforces a minimum confidence threshold ($\ge 0.68$).

### D. Multi-Tier Cache System (`data/artwork/cache/ArtworkCache`)
- **In-Memory LRU Cache**: Implemented using `LinkedHashMap<String, CachedArtworkEntry>` with LRU order (capacity 500 entries) for $<1\text{ms}$ retrieval.
- **Persistent Disk Cache**: Stores JSON metadata in `context.cacheDir/artwork_cache/{stable_key}.json`.
  - Preserves original URL, source provider, track name, artist, and timestamp.
  - Positive cache TTL: 14 days.
  - Auto-pruning: Deletes oldest entries if cache exceeds 2,000 files.
- **Negative Caching**: When all providers fail to find artwork for obscure or non-existent tracks, records a negative cache entry (`notFound = true`, 24-hour TTL) to prevent repeated network spam.
- **Stable Cache Keys**: Sanitized deterministic key generation: `"${cleanArtist}_${cleanTitle}"` (lowercase, alphanumeric with underscores).

### E. In-Flight Request Deduplication (`ArtworkRepository`)
Uses `ConcurrentHashMap<String, Deferred<ArtworkResult?>>`. When multiple UI components (e.g. Now Playing screen, MiniPlayer, and list items) request artwork for the same song concurrently, only a single network request is dispatched. All callers share the result.

### F. Coil Integration (`data/artwork/coil/ArtworkFetcher`)
- `ArtworkFetcher` implements Coil's `Fetcher` for `ArtworkModel`.
- Registered in `MyPlayerApplication.newImageLoader()`.
- Automatically invoked when passing `ArtworkModel` to `AsyncImage` or `ImageRequest`.
- Delegates downloading and disk byte caching to Coil.

### G. Batch Prefetching
- `ArtworkRepository.prefetchBatch(items)` utilizes a `Semaphore(4)` to bound concurrency, resolving artwork and warming Coil's cache in the background without congesting network bandwidth.

---

## 3. UI Enhancements
1. **`AlbumArtImage.kt`**:
   - Enhanced with optional `title: String?`, `artist: String?`, and `album: String?`.
   - Seamlessly converts metadata to `ArtworkModel`, resolving artwork automatically.
2. **`NowPlayingScreen.kt`**:
   - Passes `title` and `artist` into `AlbumArtImage`, enabling high-res 1000x1000 artwork display during playback.
3. **`MiniPlayer.kt`**:
   - Passes `song.title` and `song.artist` into `AlbumArtImage`.
4. **`HomeScreen.kt`**:
   - Passes `song.title` and `song.artist` into `AlbumArtImage` across all song lists and trending items.
5. **`DownloadsScreen.kt` & `OnlineSearchScreen.kt`**:
   - Wraps list items with `ArtworkModel` for automatic high-res artwork resolution.

---

## 4. Verification & Build Results
- **Unit Tests**: Executed 19 dedicated unit tests across matching, caching, and repository fallback. **All 19 tests passed (100% success)**.
- **Full Project Unit Tests**: All existing test suites across lyrics, recommendations, queue order, and Aria engine executed and passed in 3 seconds.
- **Debug APK Build**: `./gradlew assembleDebug` completed successfully with 0 errors.

---

## 5. Known Limitations
- **Deezer Regional Licensing**: Deezer's public API returns empty results in certain geographic regions (e.g. India) due to streaming catalog licensing. Our multi-provider fallback mitigates this completely by automatically falling through to iTunes and YouTube Music.
- **Extremely Obscure Local Audio**: Audio files lacking both ID3 tags and standard title/artist metadata cannot be matched online and fall back cleanly to local embedded art or the default placeholder icon.
