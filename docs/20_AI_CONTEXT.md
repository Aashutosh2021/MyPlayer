# AI Context Quick-Start - MyPlayer

This high-density quick-start document provides the core context required by AI models to understand, analyze, or add features to **MyPlayer** without scanning the entire codebase.

---

## 1. System DNA

MyPlayer is a local-first, streaming-capable Android music player built with Jetpack Compose, Hilt, Room, and Media3 (ExoPlayer).

### Architecture Map
* **UI**: MVVM pattern. ViewModels expose reactive state flows (`StateFlow`). Composables collect states using `collectAsStateWithLifecycle()`.
* **State Bridge**: `MusicController.kt` is a singleton acting as the sole gateway for playback commands (prepares queues, updates seekers, and operates sleep timers).
* **Player Core**: `MusicService.kt` is a foreground `MediaSessionService` that runs the underlying `ExoPlayer` instance.
* **Storage**: Room database (`AppDatabase`, schema version 2) acts as the offline source of truth. Lightweight configurations use DataStore.

---

## 2. API & Media Resolution

* **YouTube Music Search**: `InnertubeApi.kt` posts queries to YTM `/search` using public web credentials and JSON client parameters (`WEB_REMIX`). Manual JSON response parsers extract metadata into `OnlineSong` objects.
* **Direct Audio Resolution**: Bypasses bot detection by loading watch Uris into `NewPipeExtractor`. It extracts the highest-bitrate `.m4a` audio stream. Client-masquerading headers (`ANDROID_VR`) are loaded via `NewPipeDownloader.kt` to resolve direct streaming links.
* **Background Downloads**: Scheduled via `DownloadWorker` expedited tasks. The worker promotes itself to a foreground data-sync service and downloads audio streams in chunks using Range GET headers. Writes to disk (supporting Storage Access Framework folder Uris) and updates the database.

---

## 3. Security Hardening Layer

MyPlayer features a layered verification system to prevent tampering and dynamic reverse engineering:
* **NDK C++ Library (`libmyplayer_security.so` / `native-lib.cpp`)**:
  * Implements obfuscated strings decoded in native memory.
  * Performs TCP port scanning (port 27042) and `/proc/self/maps` signature parsing for Frida.
  * Performs `fopen` parsing for `su` root binaries.
  * Scans `/proc/self/status` for TracerPid debugger indicators and attempts self-tracing via `ptrace`.
* **Kotlin Orchestrator (`SecurityManager.kt`)**:
  * Runs security checks asynchronously at launch, posting status to a `SecurityStatus` flow.
  * Employs a *soft-fail* approach: anomalies are logged and reported, but the app continues to function.
* **Signature Verification (`SignatureVerifier.kt`)**:
  * Compares running APK certificate SHA-256 fingerprint against expected values. The expected fingerprint is split into multiple character arrays to prevent static string analysis.
* **Constant Encryption (`StringEncryptionManager.kt`)**:
  * Stores critical endpoints and keys as XOR-obfuscated byte arrays. Also includes AES-GCM encryption utilities backed by Android Keystore.

---

## 4. UI Style: Claymorphism

Instead of standard card or border layouts, UI containers use custom `.claySurface()` (convex) and `.clayConcave()` (sunken) modifiers from `Modifiers.kt` to implement a tactile clay visual language:
* **Convex Controls**: `ClayCard`, `ClayButton`, `ClayIconButton` (raise above cloud blue surfaces).
* **Concave Wells**: Search input fields, progress track wells (`ClayProgressBar`).

---

## 5. Development Constraints

> [!IMPORTANT]
> Keep the following rules in mind:
> * **Do NOT alter JNI method names** in `SecurityNativeBridge.kt` or `native-lib.cpp`, as this will cause JNI Linkage Errors.
> * **Do NOT hardcode plaintext keys** or URLs. Obfuscate them via XOR byte arrays first.
> * **Do NOT bypass or modify** `SignatureVerifier` check logic.
> * **Always route playback commands** through `MusicController`. Do not duplicate media controllers or ExoPlayer instances.
> * **Empty DSP packages**: Note that `playback/dsp` and `ui/screens/dsp` are currently empty placeholder structures.
