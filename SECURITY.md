# Security

MyPlayer handles both local file access and external network connections. This document outlines the security considerations and implementations.

## Network Security
- **HTTPS Only:** The application strictly communicates over HTTPS. All OkHttp and Retrofit clients are configured to reject plaintext HTTP connections.
- **No Third-Party Analytics:** There are no tracking SDKs (Firebase, Crashlytics, Mixpanel, etc.) embedded in this repository. 
- **LRCLIB Integration:** The lyrics API (lrclib.net) is an open-source, community-driven database that requires no API keys and does not track user identity.
- **InnerTube API:** YouTube queries are made anonymously. No user authentication (OAuth) is implemented, meaning playback history is not linked to any Google account.

## Local Storage Security
- **Scoped Storage:** MyPlayer targets SDK 34 (Android 14) and fully implements Scoped Storage guidelines. 
  - It uses `MediaStore` to scan for `.mp3` files rather than requesting legacy broad storage access.
  - Downloaded tracks are stored in the app's isolated private directory (`Context.filesDir`), ensuring other malicious applications cannot modify or access the cached audio.
- **Jetpack DataStore:** User settings (Autoplay toggle, etc.) are stored in Jetpack Preferences DataStore, which is private to the application sandbox.

## Permissions
The app requests the bare minimum permissions required for operation:
1. `INTERNET`: Required for YouTube streaming and lyrics.
2. `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required to comply with Android 14 restrictions for background audio playback.
3. `READ_MEDIA_AUDIO`: Required to index the user's local music library.
