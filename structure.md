# Project Structure

`	ext
+--- .claude
|    +--- SYSTEM_PROMPT.md
|    +--- UNIVERSAL_AI_BRAIN_PROMPT.md
|    +--- settings.json
|    \--- settings.local.json
+--- .gemini
|    +--- SYSTEM_PROMPT.md
|    +--- UNIVERSAL_AI_BRAIN_PROMPT.md
|    +--- settings.json
|    \--- skills
|         +--- find-skills
|         |    +--- .skillfish.json
|         |    +--- SKILL.md
|         |    \--- source.json
|         +--- gsap-core
|         |    \--- SKILL.md
|         +--- gsap-frameworks
|         |    \--- SKILL.md
|         +--- gsap-performance
|         |    \--- SKILL.md
|         +--- gsap-plugins
|         |    \--- SKILL.md
|         +--- gsap-react
|         |    \--- SKILL.md
|         +--- gsap-scrolltrigger
|         |    \--- SKILL.md
|         +--- gsap-timeline
|         |    \--- SKILL.md
|         +--- gsap-utils
|         |    \--- SKILL.md
|         +--- higgsfield-generate
|         |    +--- SKILL.md
|         |    \--- references
|         |         +--- marketing-ad-references.md
|         |         +--- marketing-avatars.md
|         |         +--- marketing-brand-kits.md
|         |         +--- marketing-dtc-ads.md
|         |         +--- marketing-modes.md
|         |         +--- marketing-products.md
|         |         +--- marketing-setup-items.md
|         |         +--- media-inputs.md
|         |         +--- model-catalog.md
|         |         +--- prompt-engineering.md
|         |         +--- troubleshooting.md
|         |         \--- workflows.md
|         +--- higgsfield-marketplace-cards
|         |    \--- SKILL.md
|         +--- higgsfield-product-photoshoot
|         |    \--- SKILL.md
|         +--- higgsfield-soul-id
|         |    +--- SKILL.md
|         |    \--- references
|         |         +--- photo-guide.md
|         |         \--- troubleshooting.md
|         +--- higgsfield-websites
|         |    +--- SKILL.md
|         |    \--- references
|         |         +--- app-cover.md
|         |         +--- app-flow.md
|         |         +--- app-layouts.md
|         |         +--- app-quickstart.md
|         |         +--- asset-system.md
|         |         +--- auth.md
|         |         +--- containers.md
|         |         +--- design-recipe.md
|         |         +--- design-taste-frontend.md
|         |         +--- fnf-react.md
|         |         +--- fnf-sdk.md
|         |         +--- image-to-code.md
|         |         +--- quanta-design.md
|         |         +--- reference-boards.md
|         |         +--- review-rubric.md
|         |         +--- runtime-and-infra.md
|         |         +--- security.md
|         |         +--- seo.md
|         |         +--- website-flow.md
|         |         +--- wow-catalog.md
|         |         \--- wow-maker.md
|         +--- motion-dev-animations
|         |    +--- ACTIVATION_TEST.md
|         |    +--- CONTRIBUTING.md
|         |    +--- DEPLOYMENT_STATUS.md
|         |    +--- LICENSE
|         |    +--- README.md
|         |    +--- RESEARCH_SYNTHESIS.md
|         |    +--- SKILL.md
|         |    +--- SKILL.md.backup
|         |    +--- VALIDATION_V3.md
|         |    +--- examples
|         |    |    +--- card-hover.md
|         |    |    +--- example-config.json
|         |    |    +--- hero-fade-up.md
|         |    |    +--- magnetic-button.md
|         |    |    +--- parallax-layers.md
|         |    |    \--- scroll-reveal.md
|         |    +--- reference
|         |    |    +--- api-reference.md
|         |    |    \--- spring-physics.md
|         |    +--- schema
|         |    |    \--- motion-config.schema.json
|         |    +--- scripts
|         |    |    \--- validate_motion_config.py
|         |    \--- templates
|         |         +--- component-library.tsx
|         |         \--- nextjs-page.tsx
|         +--- seedance-prompt-en
|         |    \--- SKILL.md
|         \--- skills-lock.json
+--- .gitignore
+--- .vscode
|    \--- settings.json
+--- Agent_BOOT.md
+--- CLAUDE.md
+--- CRASH_REPORT.md
+--- DATA_LAYER_CONSOLIDATION_REPORT.md
+--- DATA_MODEL_NORMALIZATION_REPORT.md
+--- DOWNLOAD_SYSTEM_REPORT.md
+--- MUSICCONTROLLER_REFACTOR_REPORT.md
+--- PERFORMANCE_RECOVERY_REPORT.md
+--- PERFORMANCE_REPORT.md
+--- PLAYBACK_INTEGRATION_REPORT.md
+--- PLAYBACK_PIPELINE_REPORT.md
+--- PLAYBACK_RECOVERY_REPORT.md
+--- PROMPT.md
+--- QA_REPORT.md
+--- README.md
+--- RECOMMENDATION_DECOUPLING_REPORT.md
+--- RELEASE_READINESS.md
+--- Recovery.md
+--- app
|    +--- .gitignore
|    +--- build.gradle.kts
|    +--- lint-baseline.xml
|    +--- proguard-rules.pro
|    \--- src
|         +--- androidTest
|         |    \--- java
|         |         \--- com
|         |              \--- example
|         |                   \--- myplayer
|         |                        \--- ExampleInstrumentedTest.kt
|         +--- main
|         |    +--- AndroidManifest.xml
|         |    +--- cpp
|         |    |    +--- CMakeLists.txt
|         |    |    \--- native-lib.cpp
|         |    +--- ic_launcher-playstore.png
|         |    +--- java
|         |    |    \--- com
|         |    |         \--- example
|         |    |              \--- myplayer
|         |    |                   +--- MainActivity.kt
|         |    |                   +--- MyPlayerApplication.kt
|         |    |                   +--- aria
|         |    |                   |    +--- cache
|         |    |                   |    |    \--- AriaMemoryCache.kt
|         |    |                   |    +--- dispatcher
|         |    |                   |    |    \--- CommandDispatcher.kt
|         |    |                   |    +--- event
|         |    |                   |    |    \--- AriaEventPublisher.kt
|         |    |                   |    +--- model
|         |    |                   |    |    +--- AriaCommand.kt
|         |    |                   |    |    +--- AriaResponse.kt
|         |    |                   |    |    +--- AriaStatus.kt
|         |    |                   |    |    +--- QueueInfo.kt
|         |    |                   |    |    +--- Responses.kt
|         |    |                   |    |    \--- SongInfo.kt
|         |    |                   |    +--- performance
|         |    |                   |    |    \--- AriaPerformanceMonitor.kt
|         |    |                   |    +--- providers
|         |    |                   |    |    +--- DownloadProvider.kt
|         |    |                   |    |    +--- EqualizerProvider.kt
|         |    |                   |    |    +--- HistoryProvider.kt
|         |    |                   |    |    +--- LyricsProvider.kt
|         |    |                   |    |    +--- MetadataProvider.kt
|         |    |                   |    |    +--- PlaybackController.kt
|         |    |                   |    |    +--- PlaylistProvider.kt
|         |    |                   |    |    +--- QueueProvider.kt
|         |    |                   |    |    +--- RecommendationProvider.kt
|         |    |                   |    |    \--- SearchProvider.kt
|         |    |                   |    +--- recovery
|         |    |                   |    |    \--- AriaStateRecoveryManager.kt
|         |    |                   |    +--- security
|         |    |                   |    |    +--- AriaPermission.kt
|         |    |                   |    |    +--- AriaSecurityConfig.kt
|         |    |                   |    |    \--- CallerVerifier.kt
|         |    |                   |    \--- service
|         |    |                   |         \--- AriaService.kt
|         |    |                   +--- data
|         |    |                   |    +--- backup
|         |    |                   |    |    \--- BackupRestoreManager.kt
|         |    |                   |    +--- download
|         |    |                   |    |    \--- DownloadWorker.kt
|         |    |                   |    +--- local
|         |    |                   |    |    +--- AppDatabase.kt
|         |    |                   |    |    +--- dao
|         |    |                   |    |    |    +--- CachedLyricsDao.kt
|         |    |                   |    |    |    +--- DownloadedSongDao.kt
|         |    |                   |    |    |    +--- FavoriteDao.kt
|         |    |                   |    |    |    +--- FolderDao.kt
|         |    |                   |    |    |    +--- PlaylistDao.kt
|         |    |                   |    |    |    +--- RecentHistoryDao.kt
|         |    |                   |    |    |    +--- RecentSearchDao.kt
|         |    |                   |    |    |    \--- SongDao.kt
|         |    |                   |    |    +--- datastore
|         |    |                   |    |    |    \--- SettingsDataStore.kt
|         |    |                   |    |    +--- entity
|         |    |                   |    |    |    +--- CachedLyricsEntity.kt
|         |    |                   |    |    |    +--- DownloadedSongEntity.kt
|         |    |                   |    |    |    +--- FavoriteEntity.kt
|         |    |                   |    |    |    +--- FolderEntity.kt
|         |    |                   |    |    |    +--- PlaylistEntity.kt
|         |    |                   |    |    |    +--- PlaylistSongCrossReference.kt
|         |    |                   |    |    |    +--- RecentHistoryEntity.kt
|         |    |                   |    |    |    +--- RecentSearchEntity.kt
|         |    |                   |    |    |    \--- SongEntity.kt
|         |    |                   |    |    \--- prefs
|         |    |                   |    |         \--- PreferencesManager.kt
|         |    |                   |    +--- lyrics
|         |    |                   |    |    \--- LyricsRepository.kt
|         |    |                   |    +--- model
|         |    |                   |    |    +--- MusicItem.kt
|         |    |                   |    |    \--- MusicItemMapper.kt
|         |    |                   |    +--- online
|         |    |                   |    |    +--- InnertubeApi.kt
|         |    |                   |    |    +--- NewPipeDownloader.kt
|         |    |                   |    |    \--- model
|         |    |                   |    |         \--- OnlineSong.kt
|         |    |                   |    +--- recommendation
|         |    |                   |    |    +--- RecommendationCoordinator.kt
|         |    |                   |    |    +--- RecommendationManager.kt
|         |    |                   |    |    +--- RecommendationModule.kt
|         |    |                   |    |    +--- RecommendationRepository.kt
|         |    |                   |    |    +--- api
|         |    |                   |    |    |    +--- RecommendationApi.kt
|         |    |                   |    |    |    \--- RecommendationSource.kt
|         |    |                   |    |    +--- cache
|         |    |                   |    |    |    \--- RecommendationCache.kt
|         |    |                   |    |    +--- engine
|         |    |                   |    |    |    +--- RecommendationEngine.kt
|         |    |                   |    |    |    \--- RecommendationValidator.kt
|         |    |                   |    |    +--- logging
|         |    |                   |    |    |    +--- RecommendationLogger.kt
|         |    |                   |    |    |    \--- RecommendationMetrics.kt
|         |    |                   |    |    +--- mapper
|         |    |                   |    |    |    \--- RecommendationMapper.kt
|         |    |                   |    |    +--- model
|         |    |                   |    |    |    +--- RecommendationResult.kt
|         |    |                   |    |    |    +--- RecommendationSeed.kt
|         |    |                   |    |    |    \--- RecommendationSong.kt
|         |    |                   |    |    +--- playback
|         |    |                   |    |    |    \--- RecommendationPlaybackRepository.kt
|         |    |                   |    |    +--- queue
|         |    |                   |    |    |    +--- QueueConstants.kt
|         |    |                   |    |    |    +--- QueueMetrics.kt
|         |    |                   |    |    |    +--- RecentPlaybackWindow.kt
|         |    |                   |    |    |    +--- RecommendationFilter.kt
|         |    |                   |    |    |    +--- RecommendationHistory.kt
|         |    |                   |    |    |    +--- RecommendationQueue.kt
|         |    |                   |    |    |    +--- RecommendationQueueHealth.kt
|         |    |                   |    |    |    +--- RecommendationQueueManager.kt
|         |    |                   |    |    |    +--- RecommendationQueuePolicy.kt
|         |    |                   |    |    |    \--- RecommendationSession.kt
|         |    |                   |    |    +--- strategy
|         |    |                   |    |    |    \--- RecommendationStrategy.kt
|         |    |                   |    |    \--- utils
|         |    |                   |    |         \--- RecommendationConstants.kt
|         |    |                   |    \--- repository
|         |    |                   |         +--- DownloadRepository.kt
|         |    |                   |         +--- FavoriteRepository.kt
|         |    |                   |         +--- HybridLibraryRepository.kt
|         |    |                   |         +--- MediaScanner.kt
|         |    |                   |         +--- MusicRepository.kt
|         |    |                   |         +--- OnlineSearchRepository.kt
|         |    |                   |         +--- PlaylistRepository.kt
|         |    |                   |         +--- RecentHistoryRepository.kt
|         |    |                   |         \--- SongRepository.kt
|         |    |                   +--- di
|         |    |                   |    +--- AriaModule.kt
|         |    |                   |    +--- CacheModule.kt
|         |    |                   |    +--- DatabaseModule.kt
|         |    |                   |    +--- NetworkModule.kt
|         |    |                   |    \--- PlayerModule.kt
|         |    |                   +--- playback
|         |    |                   |    +--- MediaItemFactory.kt
|         |    |                   |    +--- MusicController.kt
|         |    |                   |    +--- MusicService.kt
|         |    |                   |    +--- PlayRequest.kt
|         |    |                   |    +--- PlaybackCompletionGuard.kt
|         |    |                   |    +--- PlaybackErrorHandler.kt
|         |    |                   |    +--- PlaybackEvent.kt
|         |    |                   |    +--- PlaybackRouter.kt
|         |    |                   |    +--- PlaybackSourceResolver.kt
|         |    |                   |    +--- PlaybackStateManager.kt
|         |    |                   |    \--- SleepTimerManager.kt
|         |    |                   +--- security
|         |    |                   |    +--- AntiDebugManager.kt
|         |    |                   |    +--- EmulatorDetectionManager.kt
|         |    |                   |    +--- FridaDetectionManager.kt
|         |    |                   |    +--- HookDetectionManager.kt
|         |    |                   |    +--- IntegrityManager.kt
|         |    |                   |    +--- NetworkSecurityManager.kt
|         |    |                   |    +--- RootDetectionManager.kt
|         |    |                   |    +--- SecurityManager.kt
|         |    |                   |    +--- SecurityNativeBridge.kt
|         |    |                   |    +--- SignatureVerifier.kt
|         |    |                   |    +--- StringEncryptionManager.kt
|         |    |                   |    \--- TamperDetectionManager.kt
|         |    |                   +--- ui
|         |    |                   |    +--- common
|         |    |                   |    |    \--- AlbumArtImage.kt
|         |    |                   |    +--- components
|         |    |                   |    |    +--- FloatingNavBar.kt
|         |    |                   |    |    +--- GlassComponents.kt
|         |    |                   |    |    \--- recommendation
|         |    |                   |    |         +--- RecommendationQueuePreview.kt
|         |    |                   |    |         +--- RecommendationReasonChip.kt
|         |    |                   |    |         +--- RecommendationSection.kt
|         |    |                   |    |         +--- RecommendationStates.kt
|         |    |                   |    |         \--- RecommendedSongCard.kt
|         |    |                   |    +--- navigation
|         |    |                   |    |    \--- Screen.kt
|         |    |                   |    +--- screens
|         |    |                   |    |    +--- downloads
|         |    |                   |    |    |    +--- DownloadsScreen.kt
|         |    |                   |    |    |    \--- DownloadsViewModel.kt
|         |    |                   |    |    +--- home
|         |    |                   |    |    |    +--- HomeScreen.kt
|         |    |                   |    |    |    \--- HomeViewModel.kt
|         |    |                   |    |    +--- library
|         |    |                   |    |    |    +--- LibraryScreen.kt
|         |    |                   |    |    |    +--- LibraryViewModel.kt
|         |    |                   |    |    |    +--- PlaylistDetailScreen.kt
|         |    |                   |    |    |    \--- PlaylistDetailViewModel.kt
|         |    |                   |    |    +--- main
|         |    |                   |    |    |    +--- MainScreen.kt
|         |    |                   |    |    |    +--- MainViewModel.kt
|         |    |                   |    |    |    \--- MiniPlayer.kt
|         |    |                   |    |    +--- nowplaying
|         |    |                   |    |    |    +--- LyricsTab.kt
|         |    |                   |    |    |    +--- LyricsViewModel.kt
|         |    |                   |    |    |    \--- NowPlayingScreen.kt
|         |    |                   |    |    +--- recommendation
|         |    |                   |    |    |    \--- RecommendationViewModel.kt
|         |    |                   |    |    +--- search
|         |    |                   |    |    |    +--- OnlineSearchScreen.kt
|         |    |                   |    |    |    +--- OnlineSearchViewModel.kt
|         |    |                   |    |    |    +--- SearchScreen.kt
|         |    |                   |    |    |    \--- SearchViewModel.kt
|         |    |                   |    |    \--- settings
|         |    |                   |    |         +--- SettingsScreen.kt
|         |    |                   |    |         \--- SettingsViewModel.kt
|         |    |                   |    \--- theme
|         |    |                   |         +--- Color.kt
|         |    |                   |         +--- Modifiers.kt
|         |    |                   |         +--- Theme.kt
|         |    |                   |         \--- Type.kt
|         |    |                   \--- util
|         |    |                        \--- AudioAlbumArtFetcher.kt
|         |    \--- res
|         |         +--- drawable
|         |         |    +--- ic_launcher_background.xml
|         |         |    \--- ic_launcher_foreground.xml
|         |         +--- mipmap-anydpi
|         |         +--- mipmap-anydpi-v26
|         |         |    +--- ic_launcher.xml
|         |         |    \--- ic_launcher_round.xml
|         |         +--- mipmap-hdpi
|         |         |    +--- ic_launcher.webp
|         |         |    +--- ic_launcher_foreground.webp
|         |         |    \--- ic_launcher_round.webp
|         |         +--- mipmap-mdpi
|         |         |    +--- ic_launcher.webp
|         |         |    +--- ic_launcher_foreground.webp
|         |         |    \--- ic_launcher_round.webp
|         |         +--- mipmap-xhdpi
|         |         |    +--- ic_launcher.webp
|         |         |    +--- ic_launcher_foreground.webp
|         |         |    \--- ic_launcher_round.webp
|         |         +--- mipmap-xxhdpi
|         |         |    +--- ic_launcher.webp
|         |         |    +--- ic_launcher_foreground.webp
|         |         |    \--- ic_launcher_round.webp
|         |         +--- mipmap-xxxhdpi
|         |         |    +--- ic_launcher.webp
|         |         |    +--- ic_launcher_foreground.webp
|         |         |    \--- ic_launcher_round.webp
|         |         +--- values
|         |         |    +--- colors.xml
|         |         |    +--- strings.xml
|         |         |    \--- themes.xml
|         |         \--- xml
|         |              +--- backup_rules.xml
|         |              +--- data_extraction_rules.xml
|         |              \--- network_security_config.xml
|         \--- test
|              \--- java
|                   +--- android
|                   |    \--- util
|                   |         \--- Log.java
|                   \--- com
|                        \--- example
|                             \--- myplayer
|                                  +--- ExampleUnitTest.kt
|                                  +--- aria
|                                  |    \--- AriaSdkTests.kt
|                                  \--- data
|                                       \--- recommendation
|                                            \--- queue
|                                                 \--- RecommendationQueueManagerTest.kt
+--- app_logs.txt
+--- brain
|    +--- 00_INDEX.md
|    +--- 01_PROJECT_MEMORY.md
|    +--- 02_ARCHITECTURE.md
|    +--- 03_FILE_INDEX.md
|    +--- 04_CLASS_INDEX.md
|    +--- 05_METHOD_INDEX.md
|    +--- 06_DEPENDENCY_GRAPH.md
|    +--- 07_EVENT_GRAPH.md
|    +--- 08_DATA_FLOW.md
|    +--- 09_API_MAP.md
|    +--- 10_DATABASE_MAP.md
|    +--- 11_FEATURE_MAP.md
|    +--- 12_UI_MAP.md
|    +--- 13_CONFIGURATION_MAP.md
|    +--- 14_GLOBAL_VARIABLES.md
|    +--- 15_EXTERNAL_SERVICES.md
|    +--- 16_CALL_GRAPH.md
|    +--- 17_PROJECT_RULES.md
|    +--- 18_CURRENT_STATE.md
|    +--- 19_ROADMAP.md
|    +--- 20_AI_CONTEXT.md
|    +--- 21_AGENT_BOOT.md
|    +--- 22_IMPACT_MAP.md
|    +--- 23_FEATURE_DEPENDENCY_MATRIX.md
|    +--- 24_REFACTOR_AUDIT.md
|    +--- CHANGELOG.md
|    +--- CONTRIBUTING.md
|    +--- DECISIONS.md
|    +--- DESIGN.md
|    +--- FEATURES.md
|    +--- FEATURE_DEPENDENCY_MATRIX.md
|    +--- INSTALLATION.md
|    +--- Impact_MAP.md
|    +--- KNOWN_ISSUES.md
|    +--- LICENSE_NOTICE.md
|    +--- PERFORMANCE.md
|    +--- PRD.md
|    +--- PROJECT_STRUCTURE.md
|    +--- PROMPT.md
|    +--- ROADMAP.md
|    +--- SECURITY.md
|    +--- TODO.md
|    +--- TRD.md
|    \--- VERSION.json
+--- build.gradle.kts
+--- docs
|    +--- architecture.md
|    +--- aria_sdk_architecture.md
|    +--- lyrics.md
|    +--- overview.md
|    +--- playback.md
|    +--- recommendation.md
|    +--- settings.md
|    \--- ui.md
+--- gradle
|    +--- gradle-daemon-jvm.properties
|    +--- libs.versions.toml
|    \--- wrapper
|         +--- gradle-wrapper.jar
|         \--- gradle-wrapper.properties
+--- gradle.properties
+--- gradlew
+--- gradlew.bat
+--- local.properties
+--- my_tree_clean.txt
+--- report-2.md
+--- report.md
+--- settings.gradle.kts
\--- structure.md`
