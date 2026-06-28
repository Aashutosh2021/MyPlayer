# Current State & Technical Debt - MyPlayer

This document outlines the current feature completion status, in-progress tasks, pending roadmaps, technical debt, and known vulnerabilities in **MyPlayer**.

---

## 1. Feature Completion Status

### ✅ Completed
* **Local Library Management**: Discovers and indexes local audio formats (MP3, M4A, WAV, FLAC, OGG). Directories are mapped using Storage Access Framework (SAF) folder picker prompts.
* **YouTube Music Search**: Fetches online search queries using client masquerading context headers (`WEB_REMIX`). Manual JSON response parsers filter results to songs.
* **Online Streaming**: Decrypts stream signatures and extracts direct media links using `NewPipeExtractor` and `NewPipeDownloader`.
* **Background Downloads**: Worker downloading via `WorkManager` with expedited job status. Downloads tracks in chunks using range requests, saving files to custom directories.
* **Decoupled Playback Engine**: Singleton `MusicController` wrapper and foreground promoted `MusicService` hosting ExoPlayer.
* **Claymorphic UI Theme**: Implemented custom `.claySurface()` convex and concave visual modifiers, cards, sliders, progress bars, and navigation islands.
* **Security Validation Pipeline**: Executes parallel startup checks for rooted devices, emulator markers, Frida injection signatures, debugger attachments, and classes.dex tampering.
* **String Encryption**: Compile-time XOR obfuscation tables and Android Keystore AES-GCM managers.
* **Sleep Timer**: Triggers automatic player pausing via coroutine countdown timers.

### 🔄 In Progress
* **Google Play Integrity Verification**: Startup warmup calls in `IntegrityManager` work, but the backend requires linking the Google Cloud Console project number in `local.properties` to issue validation tokens.

### ❌ Pending / Planned
* **DSP Audio Effects Framework**: The folders `playback/dsp` and `ui/screens/dsp` are currently empty. Audio processors (Equalizer, Bass Boost, Loudness) and their respective UI screens are not implemented.
* **Advanced Player Additions**: Lyrics integration, dynamic color theming based on current track album art, and cross-device playlist sync.

---

## 2. Technical Debt

* **Hardcoded API Credentials**:
  * The public YouTube Music API key and Innertube base URLs are hardcoded as XOR obfuscated tables inside `StringEncryptionManager.kt`. They should be moved to secure remote configurations or injected during CI/CD builds.
* **Manual JSON Parsing**:
  * `InnertubeApi.kt` uses Android's primitive `JSONObject` and `JSONArray` APIs to manually traverse YouTube response trees. This results in verbose parsing methods (`findMusicShelfContents`, `parseMusicItem`). The codebase should migrate to the `kotlinx.serialization` JSON parser library.
* **Flat Folder Scanner**:
  * The `MediaScanner` indexes files on a flat level. If a folder contains sub-directories, they are ignored. The scanning loop should be refactored to support optional recursive scans.
* **Ignored Format Warnings**:
  * The scanner explicitly checks and skips "aac" files, which are technically supported by Android's media framework.

---

## 3. Known Vulnerabilities & Risks

* **YouTube API Schema Drift (High Risk)**:
  * Because the app queries the unofficial YouTube Music API, any change in YouTube's response JSON layout will break search parsing.
  * In particular, the duration parsing method `parseMusicItem()` checks text strings for a colon (`:`). If YouTube alters this format (e.g., returning seconds or using localized labels), duration indexing will break.
* **Bot-Detection Throttling (Medium Risk)**:
  * YouTube regularly updates bot-detection parameters. Although `NewPipeExtractor` updates mitigate this, streaming or downloading could fail if YouTube enforces stricter signature checks (such as mandatory PoToken parameters).
