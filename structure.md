# Project Structure

```text
│   .gitignore
│   api-map.md
│   architecture.md
│   build.gradle.kts
│   CLAUDE.md
│   code.html
│   codebase_memory.md
│database-map.md
│   dependency-graph.md
│   DESIGN.md
│   gradle.properties
│   gradlew
│   gradlew.bat
│   local.properties
│   log.txt
│   memory.md
│   NowPlayingScreen.html
│   NowPlayingScreen.png
│   PlaylistDetailScreen_backup.kt
│   PRD.md
│   routes.md
│   settings.gradle.kts
│   structure.md
│   TRD.md
│
├───.gemini
│       settings.json
│
├───.gradle
│   │   file-system.probe
│   │
│   ├───8.9
│   │   │   gc.properties
│   │   │
│   │   ├───checksums
│   │   │       checksums.lock
│   │   │       md5-checksums.bin
│   │   │       sha1-checksums.bin
│   │   │
│   │   ├───dependencies-accessors
│   │   │   │   gc.properties
│   │   │   │
│   │   │   ├───04df97747e0363ca3b676a82ef4fbe8ccf2da125
│   │   │   │   │   metadata.bin
│   │   │   │   │
│   │   │   │   ├───classes
│   │   │   │   │   └───org
│   │   │   │   │       └───gradle
│   │   │   │   │           └───accessors
│   │   │   │   │               └───dm
│   │   │   │   │                       LibrariesForLibs$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibs$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibs.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock.class
│   │   │   │   │
│   │   │   │   └───sources
│   │   │   │       └───org
│   │   │   │           └───gradle
│   │   │   │               └───accessors
│   │   │   │                   └───dm
│   │   │   │                           LibrariesForLibs.java
│   │   │   │                           LibrariesForLibsInPluginsBlock.java
│   │   │   │
│   │   │   ├───3fe847779d195476cd5be80d8647724a62c7ed0c
│   │   │   │   │   metadata.bin
│   │   │   │   │
│   │   │   │   ├───classes
│   │   │   │   │   └───org
│   │   │   │   │       └───gradle
│   │   │   │   │           └───accessors
│   │   │   │   │               └───dm
│   │   │   │   │                       LibrariesForLibs$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeMaterialLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibs$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibs.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock.class
│   │   │   │   │
│   │   │   │   └───sources
│   │   │   │       └───org
│   │   │   │           └───gradle
│   │   │   │               └───accessors
│   │   │   │                   └───dm
│   │   │   │                           LibrariesForLibs.java
│   │   │   │                           LibrariesForLibsInPluginsBlock.java
│   │   │   │
│   │   │   ├───870317b936614ca3358586062138428b203be2c0
│   │   │   │   │   metadata.bin
│   │   │   │   │
│   │   │   │   ├───classes
│   │   │   │   │   └───org
│   │   │   │   │       └───gradle
│   │   │   │   │           └───accessors
│   │   │   │   │               └───dm
│   │   │   │   │                       LibrariesForLibs$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibs$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibs.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock.class
│   │   │   │   │
│   │   │   │   └───sources
│   │   │   │       └───org
│   │   │   │           └───gradle
│   │   │   │               └───accessors
│   │   │   │                   └───dm
│   │   │   │                           LibrariesForLibs.java
│   │   │   │                           LibrariesForLibsInPluginsBlock.java
│   │   │   │
│   │   │   ├───c4b41ac81b93b6a42c2a64ee0faefc53dd0d86a0
│   │   │   │   │   metadata.bin
│   │   │   │   │
│   │   │   │   ├───classes
│   │   │   │   │   └───org
│   │   │   │   │       └───gradle
│   │   │   │   │           └───accessors
│   │   │   │   │               └───dm
│   │   │   │   │                       LibrariesForLibs$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeMaterialLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxDatastoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibs$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibs.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxDatastoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock.class
│   │   │   │   │
│   │   │   │   └───sources
│   │   │   │       └───org
│   │   │   │           └───gradle
│   │   │   │               └───accessors
│   │   │   │                   └───dm
│   │   │   │                           LibrariesForLibs.java
│   │   │   │                           LibrariesForLibsInPluginsBlock.java
│   │   │   │
│   │   │   ├───dc84c7ec9233b4f8756aaed4d53a6710d61b5202
│   │   │   │   │   metadata.bin
│   │   │   │   │
│   │   │   │   ├───classes
│   │   │   │   │   └───org
│   │   │   │   │       └───gradle
│   │   │   │   │           └───accessors
│   │   │   │   │               └───dm
│   │   │   │   │                       LibrariesForLibs$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeMaterialLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibs$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibs$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibs$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibs.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxActivityLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiTestLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxCoreLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxEspressoLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxHiltNavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$BundleAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$CoilLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$HiltPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$KotlinPluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$Media3LibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$NavigationLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$PluginAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$RoomLibraryAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock$VersionAccessors.class
│   │   │   │   │                       LibrariesForLibsInPluginsBlock.class
│   │   │   │   │
│   │   │   │   └───sources
│   │   │   │       └───org
│   │   │   │           └───gradle
│   │   │   │               └───accessors
│   │   │   │                   └───dm
│   │   │   │                           LibrariesForLibs.java
│   │   │   │                           LibrariesForLibsInPluginsBlock.java
│   │   │   │
│   │   │   └───e8b9367504372b35b136f9a5a9766f802522bc97
│   │   │       │   metadata.bin
│   │   │       │
│   │   │       ├───classes
│   │   │       │   └───org
│   │   │       │       └───gradle
│   │   │       │           └───accessors
│   │   │       │               └───dm
│   │   │       │                       LibrariesForLibs$AndroidPluginAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxActivityLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxComposeLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxComposeMaterialLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxComposeUiLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxComposeUiTestLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxCoreLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxDatastoreLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxEspressoLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxHiltLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxHiltNavigationLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxLifecycleLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$BundleAccessors.class
│   │   │       │                       LibrariesForLibs$CoilLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$HiltLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$HiltPluginAccessors.class
│   │   │       │                       LibrariesForLibs$KotlinPluginAccessors.class
│   │   │       │                       LibrariesForLibs$KotlinxLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$KotlinxSerializationLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$Media3DatasourceLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$Media3ExoplayerLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$Media3LibraryAccessors.class
│   │   │       │                       LibrariesForLibs$NavigationLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$OkhttpLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$OkhttpLoggingLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$PagingLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$PagingRuntimeLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$PluginAccessors.class
│   │   │       │                       LibrariesForLibs$RoomLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$VersionAccessors.class
│   │   │       │                       LibrariesForLibs$WorkLibraryAccessors.class
│   │   │       │                       LibrariesForLibs$WorkRuntimeLibraryAccessors.class
│   │   │       │                       LibrariesForLibs.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidPluginAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxActivityLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxComposeLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialIconsLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxComposeMaterialLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiTestLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxComposeUiToolingLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxCoreLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxDatastoreLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxEspressoLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxHiltLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxHiltNavigationLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$AndroidxLifecycleRuntimeLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$BundleAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$CoilLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$HiltLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$HiltPluginAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$KotlinPluginAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$KotlinxLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$KotlinxSerializationLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$Media3DatasourceLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$Media3ExoplayerLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$Media3LibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$NavigationLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$OkhttpLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$OkhttpLoggingLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$PagingLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$PagingRuntimeLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$PluginAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$RoomLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$VersionAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$WorkLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock$WorkRuntimeLibraryAccessors.class
│   │   │       │                       LibrariesForLibsInPluginsBlock.class
│   │   │       │
│   │   │       └───sources
│   │   │           └───org
│   │   │               └───gradle
│   │   │                   └───accessors
│   │   │                       └───dm
│   │   │                               LibrariesForLibs.java
│   │   │                               LibrariesForLibsInPluginsBlock.java
│   │   │
│   │   ├───expanded
│   │   ├───fileChanges
│   │   │       last-build.bin
│   │   │
│   │   ├───fileHashes
│   │   │       fileHashes.bin
│   │   │       fileHashes.lock
│   │   │       resourceHashesCache.bin
│   │   │
│   │   └───vcsMetadata
│   ├───9.3.1
│   │   │   gc.properties
│   │   │
│   │   ├───checksums
│   │   │       checksums.lock
│   │   │       md5-checksums.bin
│   │   │       sha1-checksums.bin
│   │   │
│   │   ├───executionHistory
│   │   │       executionHistory.bin
│   │   │       executionHistory.lock
│   │   │
│   │   ├───expanded
│   │   │       expanded.lock
│   │   │
│   │   ├───fileChanges
│   │   │       last-build.bin
│   │   │
│   │   ├───fileHashes
│   │   │       fileHashes.bin
│   │   │       fileHashes.lock
│   │   │       resourceHashesCache.bin
│   │   │
│   │   └───vcsMetadata
│   ├───buildOutputCleanup
│   │       buildOutputCleanup.lock
│   │       cache.properties
│   │       outputFiles.bin
│   │
│   ├───kotlin
│   │   └───errors
│   └───vcs-1
│           gc.properties
│
├───.idea
│   │   .gitignore
│   │   AndroidProjectSystem.xml
│   │   assetWizardSettings.xml
│   │   compiler.xml
│   │   deploymentTargetSelector.xml
│   │   gradle.xml
│   │   misc.xml
│   │   runConfigurations.xml
│   │   workspace.xml
│   │
│   ├───caches
│   │       deviceStreaming.xml
│   │
│   └───inspectionProfiles
│           Project_Default.xml
│
├───.kotlin
│   ├───errors
│   │       errors-1781685773106.log
│   │       errors-1781685805269.log
│   │
│   └───sessions
├───.vscode
│       settings.json
│
├───app
│   │   .gitignore
│   │   build.gradle.kts
│   │   proguard-rules.pro
│   │
│   ├───build
│   │   ├───generated
│   │   │   ├───ap_generated_sources
│   │   │   │   └───debug
│   │   │   │       └───out
│   │   │   ├───hilt
│   │   │   │   ├───component_sources
│   │   │   │   │   └───debug
│   │   │   │   │       ├───com
│   │   │   │   │       │   └───example
│   │   │   │   │       │       └───myplayer
│   │   │   │   │       │               DaggerMyPlayerApplication_HiltComponents_SingletonC.java
│   │   │   │   │       │               Hilt_MyPlayerApplication.java
│   │   │   │   │       │               MyPlayerApplication_GeneratedInjector.java
│   │   │   │   │       │               MyPlayerApplication_HiltComponents.java
│   │   │   │   │       │
│   │   │   │   │       ├───dagger
│   │   │   │   │       │   └───hilt
│   │   │   │   │       │       └───internal
│   │   │   │   │       │           └───aggregatedroot
│   │   │   │   │       │               └───codegen
│   │   │   │   │       │                       _com_example_myplayer_MyPlayerApplication.java
│   │   │   │   │       │
│   │   │   │   │       └───hilt_aggregated_deps
│   │   │   │   │               _com_example_myplayer_MyPlayerApplication_GeneratedInjector.java
│   │   │   │   │
│   │   │   │   └───component_trees
│   │   │   │       └───debug
│   │   │   │           ├───com
│   │   │   │           │   └───example
│   │   │   │           │       └───myplayer
│   │   │   │           │               MyPlayerApplication_ComponentTreeDeps.java
│   │   │   │           │
│   │   │   │           └───dagger
│   │   │   │               └───hilt
│   │   │   │                   └───internal
│   │   │   │                       └───processedrootsentinel
│   │   │   │                           └───codegen
│   │   │   │                                   _com_example_myplayer_MyPlayerApplication.java
│   │   │   │
│   │   │   ├───ksp
│   │   │   │   └───debug
│   │   │   │       ├───java
│   │   │   │       │   ├───com
│   │   │   │       │   │   └───example
│   │   │   │       │   │       └───myplayer
│   │   │   │       │   │           │   Hilt_MainActivity.java
│   │   │   │       │   │           │   MainActivity_GeneratedInjector.java
│   │   │   │       │   │           │   MyPlayerApplication_GeneratedInjector.java
│   │   │   │       │   │           │   MyPlayerApplication_MembersInjector.java
│   │   │   │       │   │           │
│   │   │   │       │   │           ├───data
│   │   │   │       │   │           │   ├───download
│   │   │   │       │   │           │   │       DownloadWorker_AssistedFactory.java
│   │   │   │       │   │           │   │       DownloadWorker_AssistedFactory_Impl.java
│   │   │   │       │   │           │   │       DownloadWorker_Factory.java
│   │   │   │       │   │           │   │       DownloadWorker_HiltModule.java
│   │   │   │       │   │           │   │
│   │   │   │       │   │           │   ├───local
│   │   │   │       │   │           │   │   ├───datastore
│   │   │   │       │   │           │   │   │       SettingsDataStore_Factory.java
│   │   │   │       │   │           │   │   │
│   │   │   │       │   │           │   │   └───prefs
│   │   │   │       │   │           │   │           PreferencesManager_Factory.java
│   │   │   │       │   │           │   │
│   │   │   │       │   │           │   ├───online
│   │   │   │       │   │           │   │       InnertubeApi_Factory.java
│   │   │   │       │   │           │   │
│   │   │   │       │   │           │   └───repository
│   │   │   │       │   │           │           DownloadRepository_Factory.java
│   │   │   │       │   │           │           HybridLibraryRepository_Factory.java
│   │   │   │       │   │           │           MediaScanner_Factory.java
│   │   │   │       │   │           │           MusicRepository_Factory.java
│   │   │   │       │   │           │           OnlineSearchRepository_Factory.java
│   │   │   │       │   │           │
│   │   │   │       │   │           ├───di
│   │   │   │       │   │           │       CacheModule_ProvideSimpleCacheFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideAppDatabaseFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideDownloadedSongDaoFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideFavoriteDaoFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideFolderDaoFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvidePlaylistDaoFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideRecentHistoryDaoFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideRecentSearchDaoFactory.java
│   │   │   │       │   │           │       DatabaseModule_ProvideSongDaoFactory.java
│   │   │   │       │   │           │       NetworkModule_ProvideOkHttpClientFactory.java
│   │   │   │       │   │           │       PlayerModule_ProvideAudioAttributesFactory.java
│   │   │   │       │   │           │       PlayerModule_ProvideCacheDataSourceFactoryFactory.java
│   │   │   │       │   │           │       PlayerModule_ProvideExoPlayerFactory.java
│   │   │   │       │   │           │
│   │   │   │       │   │           ├───playback
│   │   │   │       │   │           │       Hilt_MusicService.java
│   │   │   │       │   │           │       MusicController_Factory.java
│   │   │   │       │   │           │       MusicService_GeneratedInjector.java
│   │   │   │       │   │           │       MusicService_MembersInjector.java
│   │   │   │       │   │           │
│   │   │   │       │   │           └───ui
│   │   │   │       │   │               └───screens
│   │   │   │       │   │                   ├───downloads
│   │   │   │       │   │                   │       DownloadsViewModel_Factory.java
│   │   │   │       │   │                   │       DownloadsViewModel_HiltModules.java
│   │   │   │       │   │                   │       DownloadsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       DownloadsViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       DownloadsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │
│   │   │   │       │   │                   ├───home
│   │   │   │       │   │                   │       HomeViewModel_Factory.java
│   │   │   │       │   │                   │       HomeViewModel_HiltModules.java
│   │   │   │       │   │                   │       HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       HomeViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │
│   │   │   │       │   │                   ├───library
│   │   │   │       │   │                   │       LibraryViewModel_Factory.java
│   │   │   │       │   │                   │       LibraryViewModel_HiltModules.java
│   │   │   │       │   │                   │       LibraryViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       LibraryViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       LibraryViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │       PlaylistDetailViewModel_Factory.java
│   │   │   │       │   │                   │       PlaylistDetailViewModel_HiltModules.java
│   │   │   │       │   │                   │       PlaylistDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       PlaylistDetailViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       PlaylistDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │
│   │   │   │       │   │                   ├───main
│   │   │   │       │   │                   │       MainViewModel_Factory.java
│   │   │   │       │   │                   │       MainViewModel_HiltModules.java
│   │   │   │       │   │                   │       MainViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       MainViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       MainViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │
│   │   │   │       │   │                   ├───search
│   │   │   │       │   │                   │       OnlineSearchViewModel_Factory.java
│   │   │   │       │   │                   │       OnlineSearchViewModel_HiltModules.java
│   │   │   │       │   │                   │       OnlineSearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       OnlineSearchViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       OnlineSearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │       SearchViewModel_Factory.java
│   │   │   │       │   │                   │       SearchViewModel_HiltModules.java
│   │   │   │       │   │                   │       SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                   │       SearchViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                   │       SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │                   │
│   │   │   │       │   │                   └───settings
│   │   │   │       │   │                           SettingsViewModel_Factory.java
│   │   │   │       │   │                           SettingsViewModel_HiltModules.java
│   │   │   │       │   │                           SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.java
│   │   │   │       │   │                           SettingsViewModel_HiltModules_KeyModule_ProvideFactory.java
│   │   │   │       │   │                           SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.java
│   │   │   │       │   │
│   │   │   │       │   ├───dagger
│   │   │   │       │   │   └───hilt
│   │   │   │       │   │       └───internal
│   │   │   │       │   │           └───aggregatedroot
│   │   │   │       │   │               └───codegen
│   │   │   │       │   │                       _com_example_myplayer_MyPlayerApplication.java
│   │   │   │       │   │
│   │   │   │       │   └───hilt_aggregated_deps
│   │   │   │       │           _com_example_myplayer_data_download_DownloadWorker_HiltModule.java
│   │   │   │       │           _com_example_myplayer_di_CacheModule.java
│   │   │   │       │           _com_example_myplayer_di_DatabaseModule.java
│   │   │   │       │           _com_example_myplayer_di_NetworkModule.java
│   │   │   │       │           _com_example_myplayer_di_PlayerModule.java
│   │   │   │       │           _com_example_myplayer_MainActivity_GeneratedInjector.java
│   │   │   │       │           _com_example_myplayer_MyPlayerApplication_GeneratedInjector.java
│   │   │   │       │           _com_example_myplayer_playback_MusicService_GeneratedInjector.java
│   │   │   │       │           _com_example_myplayer_ui_screens_downloads_DownloadsViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_downloads_DownloadsViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_home_HomeViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_home_HomeViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_library_LibraryViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_library_LibraryViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_library_PlaylistDetailViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_library_PlaylistDetailViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_main_MainViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_main_MainViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_search_OnlineSearchViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_search_OnlineSearchViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_search_SearchViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_search_SearchViewModel_HiltModules_KeyModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_settings_SettingsViewModel_HiltModules_BindsModule.java
│   │   │   │       │           _com_example_myplayer_ui_screens_settings_SettingsViewModel_HiltModules_KeyModule.java
│   │   │   │       │
│   │   │   │       ├───kotlin
│   │   │   │       │   └───com
│   │   │   │       │       └───example
│   │   │   │       │           └───myplayer
│   │   │   │       │               └───data
│   │   │   │       │                   └───local
│   │   │   │       │                       │   AppDatabase_Impl.kt
│   │   │   │       │                       │
│   │   │   │       │                       └───dao
│   │   │   │       │                               DownloadedSongDao_Impl.kt
│   │   │   │       │                               FavoriteDao_Impl.kt
│   │   │   │       │                               FolderDao_Impl.kt
│   │   │   │       │                               PlaylistDao_Impl.kt
│   │   │   │       │                               RecentHistoryDao_Impl.kt
│   │   │   │       │                               RecentSearchDao_Impl.kt
│   │   │   │       │                               SongDao_Impl.kt
│   │   │   │       │
│   │   │   │       └───resources
│   │   │   │           └───META-INF
│   │   │   │               └───proguard
│   │   │   │                       com_example_myplayer_ui_screens_downloads_DownloadsViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_downloads_DownloadsViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_home_HomeViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_home_HomeViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_library_LibraryViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_library_LibraryViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_library_PlaylistDetailViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_library_PlaylistDetailViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_main_MainViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_main_MainViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_search_OnlineSearchViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_search_OnlineSearchViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_search_SearchViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_search_SearchViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_settings_SettingsViewModel_HiltModules_BindsModule_LazyClassKeys.pro
│   │   │   │                       com_example_myplayer_ui_screens_settings_SettingsViewModel_HiltModules_KeyModule_LazyClassKeys.pro
│   │   │   │
│   │   │   ├───res
│   │   │   │   └───pngs
│   │   │   │       └───debug
│   │   │   └───updated_navigation_xml
│   │   │       └───debug
│   │   ├───intermediates
│   │   │   ├───aar_metadata_check
│   │   │   │   └───debug
│   │   │   │       └───checkDebugAarMetadata
│   │   │   ├───android_res_source_set_path_map
│   │   │   │   └───debug
│   │   │   │       └───mapDebugSourceSetPaths
│   │   │   │               file-map.txt
│   │   │   │
│   │   │   ├───annotation_processor_list
│   │   │   │   └───debug
│   │   │   │       └───javaPreCompileDebug
│   │   │   │               annotationProcessors.json
│   │   │   │
│   │   │   ├───apk
│   │   │   │   └───debug
│   │   │   │           app-debug.apk
│   │   │   │           output-metadata.json
│   │   │   │
│   │   │   ├───apk_ide_redirect_file
│   │   │   │   └───debug
│   │   │   │       └───createDebugApkListingFileRedirect
│   │   │   │               redirect.txt
│   │   │   │
│   │   │   ├───app_metadata
│   │   │   │   └───debug
│   │   │   │       └───writeDebugAppMetadata
│   │   │   │               app-metadata.properties
│   │   │   │
│   │   │   ├───assets
│   │   │   │   └───debug
│   │   │   │       └───mergeDebugAssets
│   │   │   ├───built_in_kotlinc
│   │   │   │   └───debug
│   │   │   │       └───compileDebugKotlin
│   │   │   │           └───classes
│   │   │   │               ├───com
│   │   │   │               │   └───example
│   │   │   │               │       └───myplayer
│   │   │   │               │           │   ComposableSingletons$MainActivityKt.class
│   │   │   │               │           │   MainActivity.class
│   │   │   │               │           │   MyPlayerApplication.class
│   │   │   │               │           │
│   │   │   │               │           ├───data
│   │   │   │               │           │   ├───download
│   │   │   │               │           │   │       DownloadWorker$Companion.class
│   │   │   │               │           │   │       DownloadWorker$doWork$1.class
│   │   │   │               │           │   │       DownloadWorker$doWork$2.class
│   │   │   │               │           │   │       DownloadWorker$doWork$3.class
│   │   │   │               │           │   │       DownloadWorker$doWork$4.class
│   │   │   │               │           │   │       DownloadWorker$doWork$bytes$1.class
│   │   │   │               │           │   │       DownloadWorker.class
│   │   │   │               │           │   │
│   │   │   │               │           │   ├───local
│   │   │   │               │           │   │   │   AppDatabase$Companion$MIGRATION_1_2$1.class
│   │   │   │               │           │   │   │   AppDatabase$Companion.class
│   │   │   │               │           │   │   │   AppDatabase.class
│   │   │   │               │           │   │   │   AppDatabase_Impl$createOpenDelegate$_openDelegate$1.class
│   │   │   │               │           │   │   │   AppDatabase_Impl.class
│   │   │   │               │           │   │   │
│   │   │   │               │           │   │   ├───dao
│   │   │   │               │           │   │   │       DownloadedSongDao.class
│   │   │   │               │           │   │   │       DownloadedSongDao_Impl$1.class
│   │   │   │               │           │   │   │       DownloadedSongDao_Impl$Companion.class
│   │   │   │               │           │   │   │       DownloadedSongDao_Impl.class
│   │   │   │               │           │   │   │       FavoriteDao.class
│   │   │   │               │           │   │   │       FavoriteDao_Impl$1.class
│   │   │   │               │           │   │   │       FavoriteDao_Impl$Companion.class
│   │   │   │               │           │   │   │       FavoriteDao_Impl.class
│   │   │   │               │           │   │   │       FolderDao.class
│   │   │   │               │           │   │   │       FolderDao_Impl$1.class
│   │   │   │               │           │   │   │       FolderDao_Impl$2.class
│   │   │   │               │           │   │   │       FolderDao_Impl$Companion.class
│   │   │   │               │           │   │   │       FolderDao_Impl.class
│   │   │   │               │           │   │   │       PlaylistDao.class
│   │   │   │               │           │   │   │       PlaylistDao_Impl$1.class
│   │   │   │               │           │   │   │       PlaylistDao_Impl$2.class
│   │   │   │               │           │   │   │       PlaylistDao_Impl$3.class
│   │   │   │               │           │   │   │       PlaylistDao_Impl$Companion.class
│   │   │   │               │           │   │   │       PlaylistDao_Impl.class
│   │   │   │               │           │   │   │       RecentHistoryDao.class
│   │   │   │               │           │   │   │       RecentHistoryDao_Impl$1.class
│   │   │   │               │           │   │   │       RecentHistoryDao_Impl$Companion.class
│   │   │   │               │           │   │   │       RecentHistoryDao_Impl.class
│   │   │   │               │           │   │   │       RecentSearchDao.class
│   │   │   │               │           │   │   │       RecentSearchDao_Impl$1.class
│   │   │   │               │           │   │   │       RecentSearchDao_Impl$Companion.class
│   │   │   │               │           │   │   │       RecentSearchDao_Impl.class
│   │   │   │               │           │   │   │       SongDao.class
│   │   │   │               │           │   │   │       SongDao_Impl$1.class
│   │   │   │               │           │   │   │       SongDao_Impl$Companion.class
│   │   │   │               │           │   │   │       SongDao_Impl.class
│   │   │   │               │           │   │   │
│   │   │   │               │           │   │   ├───datastore
│   │   │   │               │           │   │   │       SettingsDataStore.class
│   │   │   │               │           │   │   │       SettingsDataStoreKt.class
│   │   │   │               │           │   │   │
│   │   │   │               │           │   │   ├───entity
│   │   │   │               │           │   │   │       DownloadedSongEntity.class
│   │   │   │               │           │   │   │       FavoriteEntity.class
│   │   │   │               │           │   │   │       FolderEntity.class
│   │   │   │               │           │   │   │       PlaylistEntity.class
│   │   │   │               │           │   │   │       PlaylistSongCrossReference.class
│   │   │   │               │           │   │   │       RecentHistoryEntity.class
│   │   │   │               │           │   │   │       RecentSearchEntity.class
│   │   │   │               │           │   │   │       SongEntity.class
│   │   │   │               │           │   │   │
│   │   │   │               │           │   │   └───prefs
│   │   │   │               │           │   │           PreferencesManager$Companion.class
│   │   │   │               │           │   │           PreferencesManager$setDownloadFolderUri$2.class
│   │   │   │               │           │   │           PreferencesManager$special$$inlined$map$1$2$1.class
│   │   │   │               │           │   │           PreferencesManager$special$$inlined$map$1$2.class
│   │   │   │               │           │   │           PreferencesManager$special$$inlined$map$1.class
│   │   │   │               │           │   │           PreferencesManager.class
│   │   │   │               │           │   │           PreferencesManagerKt.class
│   │   │   │               │           │   │
│   │   │   │               │           │   ├───online
│   │   │   │               │           │   │   │   InnertubeApi$Companion.class
│   │   │   │               │           │   │   │   InnertubeApi$getStreamUrl$2.class
│   │   │   │               │           │   │   │   InnertubeApi$search$2.class
│   │   │   │               │           │   │   │   InnertubeApi$SearchPage.class
│   │   │   │               │           │   │   │   InnertubeApi.class
│   │   │   │               │           │   │   │   NewPipeDownloader.class
│   │   │   │               │           │   │   │   YouTubeStreamBlockedException.class
│   │   │   │               │           │   │   │
│   │   │   │               │           │   │   └───model
│   │   │   │               │           │   │           OnlineSong.class
│   │   │   │               │           │   │
│   │   │   │               │           │   └───repository
│   │   │   │               │           │           DownloadRepository.class
│   │   │   │               │           │           HybridLibraryRepository$getHybridLibrary$1$invokeSuspend$$inlined$sortedBy$1.class
│   │   │   │               │           │           HybridLibraryRepository$getHybridLibrary$1.class
│   │   │   │               │           │           HybridLibraryRepository$searchHybridLibrary$1$invokeSuspend$$inlined$sortedBy$1.class
│   │   │   │               │           │           HybridLibraryRepository$searchHybridLibrary$1.class
│   │   │   │               │           │           HybridLibraryRepository.class
│   │   │   │               │           │           InnertubeSearchPagingSource$load$1.class
│   │   │   │               │           │           InnertubeSearchPagingSource.class
│   │   │   │               │           │           MediaScanner$scanAllFolders$2.class
│   │   │   │               │           │           MediaScanner$scanFolder$2.class
│   │   │   │               │           │           MediaScanner.class
│   │   │   │               │           │           MusicRepository$addFolder$2.class
│   │   │   │               │           │           MusicRepository$addRecentHistory$2.class
│   │   │   │               │           │           MusicRepository$addSongToPlaylist$2.class
│   │   │   │               │           │           MusicRepository$createPlaylist$2.class
│   │   │   │               │           │           MusicRepository$deletePlaylist$2.class
│   │   │   │               │           │           MusicRepository$incrementPlayCount$2.class
│   │   │   │               │           │           MusicRepository$removeFolder$2.class
│   │   │   │               │           │           MusicRepository$removeSongFromPlaylist$2.class
│   │   │   │               │           │           MusicRepository$renamePlaylist$2.class
│   │   │   │               │           │           MusicRepository$reorderSongsInPlaylist$2.class
│   │   │   │               │           │           MusicRepository$toggleFavorite$2.class
│   │   │   │               │           │           MusicRepository.class
│   │   │   │               │           │           OnlineSearchRepository$saveSearch$1.class
│   │   │   │               │           │           OnlineSearchRepository.class
│   │   │   │               │           │           PlayableSong$Downloaded.class
│   │   │   │               │           │           PlayableSong$Local.class
│   │   │   │               │           │           PlayableSong$Online.class
│   │   │   │               │           │           PlayableSong.class
│   │   │   │               │           │
│   │   │   │               │           ├───di
│   │   │   │               │           │       CacheModule.class
│   │   │   │               │           │       DatabaseModule.class
│   │   │   │               │           │       NetworkModule.class
│   │   │   │               │           │       PlayerModule.class
│   │   │   │               │           │
│   │   │   │               │           ├───playback
│   │   │   │               │           │       MusicController$setupController$1.class
│   │   │   │               │           │       MusicController$skipToNext$1.class
│   │   │   │               │           │       MusicController$startPositionUpdater$1.class
│   │   │   │               │           │       MusicController$startSleepTimer$1.class
│   │   │   │               │           │       MusicController.class
│   │   │   │               │           │       MusicService$onCreate$1.class
│   │   │   │               │           │       MusicService$onCreate$callback$1.class
│   │   │   │               │           │       MusicService.class
│   │   │   │               │           │
│   │   │   │               │           ├───ui
│   │   │   │               │           │   ├───common
│   │   │   │               │           │   │       AlbumArtImageKt$AlbumArtImage$bitmap$2$1$1.class
│   │   │   │               │           │   │       AlbumArtImageKt$AlbumArtImage$bitmap$2$1.class
│   │   │   │               │           │   │       AlbumArtImageKt.class
│   │   │   │               │           │   │
│   │   │   │               │           │   ├───components
│   │   │   │               │           │   │       FloatingNavBarKt.class
│   │   │   │               │           │   │       GlassComponentsKt.class
│   │   │   │               │           │   │       NavItem.class
│   │   │   │               │           │   │
│   │   │   │               │           │   ├───navigation
│   │   │   │               │           │   │       Screen$Downloads.class
│   │   │   │               │           │   │       Screen$Home.class
│   │   │   │               │           │   │       Screen$Library.class
│   │   │   │               │           │   │       Screen$NowPlaying.class
│   │   │   │               │           │   │       Screen$Search.class
│   │   │   │               │           │   │       Screen$Settings.class
│   │   │   │               │           │   │       Screen.class
│   │   │   │               │           │   │
│   │   │   │               │           │   ├───screens
│   │   │   │               │           │   │   ├───downloads
│   │   │   │               │           │   │   │       ComposableSingletons$DownloadsScreenKt.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen$2$1$2$1.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen$2$3$1$2$1$1.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen$2$3$1$2$2$1.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen_uFdPcIQ$lambda$33$lambda$32$lambda$31$$inlined$items$default$1.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen_uFdPcIQ$lambda$33$lambda$32$lambda$31$$inlined$items$default$2.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen_uFdPcIQ$lambda$33$lambda$32$lambda$31$$inlined$items$default$3.class
│   │   │   │               │           │   │   │       DownloadsScreenKt$DownloadsScreen_uFdPcIQ$lambda$33$lambda$32$lambda$31$$inlined$items$default$4.class
│   │   │   │               │           │   │   │       DownloadsScreenKt.class
│   │   │   │               │           │   │   │       DownloadsViewModel$deleteSong$1.class
│   │   │   │               │           │   │   │       DownloadsViewModel$filteredDownloads$1.class
│   │   │   │               │           │   │   │       DownloadsViewModel.class
│   │   │   │               │           │   │   │
│   │   │   │               │           │   │   ├───home
│   │   │   │               │           │   │   │       ComposableSingletons$HomeScreenKt.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen$1$1$2$1$1$1$1$1.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen$1$1$4$1$1.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen_uFdPcIQ$lambda$26$lambda$25$$inlined$itemsIndexed$default$1.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen_uFdPcIQ$lambda$26$lambda$25$$inlined$itemsIndexed$default$2.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen_uFdPcIQ$lambda$26$lambda$25$$inlined$itemsIndexed$default$3.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen_uFdPcIQ$lambda$26$lambda$25$lambda$15$lambda$14$lambda$13$$inlined$itemsIndexed$default$1.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen_uFdPcIQ$lambda$26$lambda$25$lambda$15$lambda$14$lambda$13$$inlined$itemsIndexed$default$2.class
│   │   │   │               │           │   │   │       HomeScreenKt$HomeScreen_uFdPcIQ$lambda$26$lambda$25$lambda$15$lambda$14$lambda$13$$inlined$itemsIndexed$default$3.class
│   │   │   │               │           │   │   │       HomeScreenKt.class
│   │   │   │               │           │   │   │       HomeViewModel$playSong$1.class
│   │   │   │               │           │   │   │       HomeViewModel$playSong$2.class
│   │   │   │               │           │   │   │       HomeViewModel$toggleFavorite$1.class
```
