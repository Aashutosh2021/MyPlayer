# Artwork Engine Changed & Created Files Inventory

## Summary of Changes
Total files created or modified for the dedicated clean-room Artwork Engine: **14 files** (10 created, 4 modified existing app code, well within the 20 file threshold).

---

## 1. Newly Created Source Files

| File Path | Description |
|---|---|
| `app/src/main/java/com/example/myplayer/data/artwork/model/ArtworkResult.kt` | Domain model for resolved artwork URL, provider name, and pixel dimensions. |
| `app/src/main/java/com/example/myplayer/data/artwork/model/ArtworkModel.kt` | Request metadata model passed to Coil and ArtworkRepository. |
| `app/src/main/java/com/example/myplayer/data/artwork/provider/ArtworkProvider.kt` | Contract interface for artwork provider implementations. |
| `app/src/main/java/com/example/myplayer/data/artwork/matching/ArtworkMatcher.kt` | Metadata sanitization, variant protection (remix/live/instrumental), and Levenshtein confidence scoring. |
| `app/src/main/java/com/example/myplayer/data/artwork/cache/ArtworkCache.kt` | In-memory LRU cache and persistent JSON disk cache with negative caching and TTL management. |
| `app/src/main/java/com/example/myplayer/data/artwork/provider/DeezerArtworkProvider.kt` | Provider implementation querying Deezer search API and selecting `cover_xl`/`cover_big`. |
| `app/src/main/java/com/example/myplayer/data/artwork/provider/ITunesArtworkProvider.kt` | Provider implementation querying iTunes Search API and upgrading resolution to 1000x1000. |
| `app/src/main/java/com/example/myplayer/data/artwork/provider/YouTubeMusicArtworkProvider.kt` | Provider implementation querying YouTube Music via InnertubeApi and upgrading thumbnail resolutions. |
| `app/src/main/java/com/example/myplayer/data/artwork/ArtworkRepository.kt` | Primary orchestrator managing the multi-provider fallback pipeline, deduplication, and prefetching. |
| `app/src/main/java/com/example/myplayer/data/artwork/coil/ArtworkFetcher.kt` | Custom Coil `Fetcher` intercepting `ArtworkModel` to load resolved artwork images. |

---

## 2. Modified Existing Files

| File Path | Nature of Modification |
|---|---|
| `app/src/main/java/com/example/myplayer/MyPlayerApplication.kt` | Injected `Provider<ArtworkRepository>` and registered `ArtworkFetcher.Factory` in `newImageLoader()`. |
| `app/src/main/java/com/example/myplayer/ui/common/AlbumArtImage.kt` | Added optional `title`, `artist`, and `album` parameters; integrated `ArtworkModel` with Coil. |
| `app/src/main/java/com/example/myplayer/ui/screens/nowplaying/NowPlayingScreen.kt` | Passed `title` and `artist` into `AlbumArtImage` for high-resolution artwork rendering. |
| `app/src/main/java/com/example/myplayer/ui/screens/main/MiniPlayer.kt` | Passed `song.title` and `song.artist` into `AlbumArtImage`. |
| `app/src/main/java/com/example/myplayer/ui/screens/home/HomeScreen.kt` | Passed `song.title` and `song.artist` into `AlbumArtImage` for recently played and trending song items. |
| `app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsScreen.kt` | Wrapped downloaded songs with `ArtworkModel` for automatic high-res artwork resolution. |
| `app/src/main/java/com/example/myplayer/ui/screens/search/OnlineSearchScreen.kt` | Wrapped search results with `ArtworkModel` for automatic high-res artwork resolution. |
| `app/build.gradle.kts` | Added `testImplementation("org.json:json:20240303")` for JVM unit test JSON parsing. |

---

## 3. Newly Created Unit Test Files

| File Path | Test Count | Description |
|---|---|---|
| `app/src/test/java/com/example/myplayer/data/artwork/ArtworkMatcherTest.kt` | 8 tests | Verifies artist/title sanitization, exact match acceptance, variant protection (remix, live, instrumental rejection), wrong song rejection, and stable cache key generation. |
| `app/src/test/java/com/example/myplayer/data/artwork/ArtworkCacheTest.kt` | 3 tests | Verifies put/get operations, disk persistence, negative cache recording, and clear operations. |
| `app/src/test/java/com/example/myplayer/data/artwork/ArtworkRepositoryTest.kt` | 8 tests | Verifies provider fallback order (Deezer -> iTunes -> YouTube Music -> Local/Placeholder), exception handling, cache hits, concurrent deduplication, and offline behavior. |
