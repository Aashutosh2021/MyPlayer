# Configuration Map - MyPlayer

This map catalogs the build configurations, compiler flags, permissions, and obfuscation parameters used by the **MyPlayer** application.

---

## 1. Gradle Build Configurations

### App-Level Build Script: `app/build.gradle.kts`
* **Purpose**: Compiles application modules, NDK libraries, and bundles assets.
* **Key Configuration Parameters**:
  * `compileSdk` / `targetSdk`: `36`
  * `minSdk`: `28` (Android 9.0 Pie)
  * `applicationId`: `"com.example.myplayer"`
  * `ndk.abiFilters`: `arm64-v8a`, `armeabi-v7a`, `x86_64`
  * `externalNativeBuild.cmake`: Path to `CMakeLists.txt`, version `3.22.1`, compile flags `-std=c++17`, `-fstack-protector-strong`, `-O2`.
  * `buildTypes.release`: Enables `isMinifyEnabled = true` (R8/ProGuard) and `isShrinkResources = true` (resources shrinking), disables debugging.
* **Modified Where**: In `app/build.gradle.kts`.

### Version Catalog: `gradle/libs.versions.toml`
* **Purpose**: Provides a single source of truth for library dependencies and versions.
* **Key Version Anchors**:
  * Kotlin: `2.0.21`
  * Compose: Mapped via Compose BOM.
  * Media3: `1.4.1` (or latest stable, backing ExoPlayer and MediaSession).
  * Room: `2.6.1`
  * Hilt: `2.51.1`
* **Modified Where**: In `gradle/libs.versions.toml`.

---

## 2. Android Manifest: `app/src/main/AndroidManifest.xml`

* **Purpose**: Declares package metadata, system permissions, components, and service types.
* **Required System Permissions**:
  * `android.permission.INTERNET` - Required for YouTube Music searching and streaming.
  * `android.permission.READ_MEDIA_AUDIO` - Read local device music (Android 13+).
  * `android.permission.READ_EXTERNAL_STORAGE` - Read local device music (Android 12 and below).
  * `android.permission.FOREGROUND_SERVICE` - Allow background execution.
  * `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Specifically required by Media3 `MusicService` to run playback foreground services on Android 14+.
  * `android.permission.FOREGROUND_SERVICE_DATA_SYNC` - Specifically required by `DownloadWorker` foreground notifications during file downloads.
* **Component Declarations**:
  * `MyPlayerApplication` as `<application>` name.
  * `MainActivity` as launch target with `android.intent.action.MAIN` intent filters.
  * `MusicService` registered as foreground service type `mediaPlayback`.
  * WorkManager foreground services mapped to `dataSync`.

---

## 3. R8 / ProGuard Obfuscation: `app/proguard-rules.pro`

* **Purpose**: Code shrinking, optimization, and obfuscation rules applied to release builds.
* **Critical Protection Rules**:
  * **Keep JNI Bindings**: Prevents renaming of external C++ methods, preserving linkages to the NDK:
    ```proguard
    -keepclasseswithmembernames,includedescriptorclasses class * {
        native <methods>;
    }
    ```
  * **Keep Security Bridge**: Preserves native bridge names explicitly to avoid JNI Link Errors:
    ```proguard
    -keep class com.example.myplayer.security.SecurityNativeBridge { *; }
    ```
  * **Keep Hilt & Room**: Preserves generated classes, database managers, and Dagger entry points to prevent reflection failures.

---

## 4. Native CMake Configurations: `app/src/main/cpp/CMakeLists.txt`

* **Purpose**: Directs CMake build rules to compile NDK C++ libraries.
* **Configurations**:
  * Declares library output name: `myplayer_security`.
  * Compiles `native-lib.cpp` into a shared library: `add_library(myplayer_security SHARED native-lib.cpp)`.
  * Link target dependencies: Links the security library to the Android logging framework: `target_link_libraries(myplayer_security log)`.

---

## 5. Local Properties: `local.properties`

* **Purpose**: Caches machine-specific SDK pathways and private keys.
* **Key Configuration Parameters**:
  * `sdk.dir`: Absolute path to local Android SDK folder.
  * `play.integrity.projectNumber`: Linked Google Cloud Console Project Number used to issue Play Integrity checks. Translated via Gradle build script to BuildConfig field `PLAY_INTEGRITY_PROJECT_NUMBER` during compilation.
* **Modified Where**: In `local.properties` (never committed to version control).
