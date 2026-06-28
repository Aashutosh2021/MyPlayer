# External Services & Libraries - MyPlayer

This document lists the third-party frameworks and libraries integrated into the **MyPlayer** application, documenting their purpose and codebase location.

---

## 1. Media & Audio Frameworks

### Google Media3 (ExoPlayer & MediaSession)
* **Purpose**: Core audio engine. Media3 replaces legacy MediaCompat helper APIs, standardizing audio focus, lock screen control notifications, background play limits, and codec formats.
* **Used Where**:
  * [MusicService.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicService.kt) - Houses the player instance.
  * [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt) - Connects to the active service.
  * [PlayerModule.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/di/PlayerModule.kt) - Injects player configurations.

### TeamNewPipe: NewPipeExtractor (v0.26.3)
* **Purpose**: YouTube HTML and JSON parser. Bypasses standard bot detection and resolves video keys to raw audio stream links without requiring official API credentials.
* **Used Where**:
  * [InnertubeApi.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/InnertubeApi.kt) - Resolves stream URLs.
  * [NewPipeDownloader.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/NewPipeDownloader.kt) - Handles HTTP queries.

---

## 2. Core Jetpack Libraries

### Room Persistence Library
* **Purpose**: Object-relational mapping (ORM) layer over SQLite.
* **Used Where**:
  * [AppDatabase.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/AppDatabase.kt) - Database configuration.
  * Package `com.example.myplayer.data.local.dao` - Queries.
  * Package `com.example.myplayer.data.local.entity` - Tables.

### WorkManager
* **Purpose**: Manages background downloading tasks. WorkManager ensures file downloads finish even if the user exits the application.
* **Used Where**:
  * [DownloadWorker.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/download/DownloadWorker.kt) - Download execution.
  * [DownloadRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/DownloadRepository.kt) - Triggers workers.

### Paging 3
* **Purpose**: Pages online search queries in lists to save memory.
* **Used Where**:
  * [OnlineSearchRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/OnlineSearchRepository.kt) - Exposes paginated flows.
  * [SearchScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/search/SearchScreen.kt) - Renders paginated results.

---

## 3. Dependency Injection & Image Loading

### Dagger Hilt
* **Purpose**: Dependency Injection (DI) system. Automated dependency injection simplifies testing, avoids boilerplate, and enforces clean scoping.
* **Used Where**:
  * Throughout the entire codebase (annotated with `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`, `@Module`, `@InstallIn`).

### Coil (Coil-Compose)
* **Purpose**: Image loading library. Handles image caching, placeholder vectors, crossfades, and loads album artwork Uris.
* **Used Where**:
  * `AlbumArtImage.kt` - Artwork loader.
  * [MyPlayerApplication.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/MyPlayerApplication.kt) - Injects custom album art fetchers.

---

## 4. Networking & Serialization

### Square OkHttp
* **Purpose**: HTTP client. Connects to network endpoints, handles connection pooling, and parses chunked range requests.
* **Used Where**:
  * [NetworkModule.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/di/NetworkModule.kt) - Decrypts and injects clients.
  * [InnertubeApi.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/InnertubeApi.kt) - Queries YTM.
  * [DownloadWorker.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/download/DownloadWorker.kt) - Downloads files.

### Kotlinx Serialization
* **Purpose**: JSON parsing library.
* **Used Where**:
  * Custom serialization tasks in the data layer.

---

## 5. Security & Device Integrity

### Google Play Integrity API
* **Purpose**: Validates that the application is running in a trusted Google-certified environment.
* **Used Where**:
  * [IntegrityManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/IntegrityManager.kt) - Issues integrity check tokens.
  * [SecurityManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/SecurityManager.kt) - Triggers validation checks.

### Android Crypto Library (`androidx.security:security-crypto`)
* **Purpose**: Provides encrypted storage layers (like EncryptedSharedPreferences) backed by Android Keystore.
* **Used Where**:
  * [StringEncryptionManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/StringEncryptionManager.kt) - Decrypts constant strings and encrypts dynamic tokens using AES-GCM.
