# Installation & Build Guide

This guide provides instructions for compiling and running MyPlayer from source.

## System Requirements

- **Android Studio:** Koala (2024.1.1) or newer.
- **Java Development Kit (JDK):** JDK 17 or higher.
- **Gradle:** Version 8.0+ (Handled automatically by the Gradle Wrapper).

## Android SDK Requirements

- **Minimum SDK:** API 24 (Android 7.0 Nougat)
- **Target SDK:** API 34 (Android 14)
- **Compile SDK:** API 34

## Build Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/MyPlayer.git
   cd MyPlayer
   ```

2. **Open in Android Studio**
   - Launch Android Studio.
   - Select `File -> Open` and select the `MyPlayer` root directory.
   - Wait for Gradle Sync to complete.

3. **Build the APK**
   From the command line (Terminal):
   ```bash
   # Build a debug APK
   ./gradlew assembleDebug

   # Run unit tests
   ./gradlew testDebugUnitTest
   ```
   Or simply use the Run button (▶) in Android Studio to deploy directly to your emulator or physical device.

## Required Device Permissions

Upon first launch, the app will request necessary permissions. 
If running on Android 13+ (API 33+), it will specifically ask for:
- `android.permission.READ_MEDIA_AUDIO`: To scan your local device for MP3 files.
- `android.permission.POST_NOTIFICATIONS`: To display the media playback foreground service notification.

In the `AndroidManifest.xml`, the following permissions are declared:
- `INTERNET`: For YouTube streaming and lyric fetching.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required to keep the audio engine running while the screen is off.
