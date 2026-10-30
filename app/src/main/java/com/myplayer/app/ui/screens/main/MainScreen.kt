package com.myplayer.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.myplayer.app.ui.components.WhatsNewDialog
import com.myplayer.app.ui.components.WhatsNewRepository
import androidx.navigation.compose.rememberNavController
import com.myplayer.app.ui.components.FloatingNavBar
import com.myplayer.app.ui.navigation.Screen
import com.myplayer.app.ui.screens.downloads.DownloadsScreen
import com.myplayer.app.ui.screens.home.HomeScreen
import com.myplayer.app.ui.screens.library.LibraryScreen
import com.myplayer.app.ui.screens.library.PlaylistDetailScreen
import com.myplayer.app.ui.screens.nowplaying.NowPlayingScreen
import com.myplayer.app.ui.screens.search.SearchScreen
import com.myplayer.app.ui.screens.settings.SettingsScreen
import com.myplayer.app.ui.theme.CloudBlueBackground
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myplayer.app.ui.myplayer.*

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val homeViewModel: com.myplayer.app.ui.screens.home.HomeViewModel = hiltViewModel()
    val myplayerState = rememberMyPlayerUiStateHolder()
    // Audio Lab -> real DSP for the whole UI lifetime (restores saved settings at app start).
    MyPlayerAudioLabEngineSync(myplayerState)
    var showStylePicker by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentOnlineSong by viewModel.currentOnlineSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    // NOTE: currentPosition is intentionally NOT collected here at the top of MainScreen!
    // Keeping currentPosition isolated prevents 500ms whole-screen recomposition passes
    // (Scaffold, NavHost, HomeScreen, FloatingNavBar).

    // Surface one-shot playback errors (e.g. a downloaded file that is no longer on disk).
    val context = LocalContext.current
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    LaunchedEffect(playbackError) {
        playbackError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearPlaybackError()
        }
    }

    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    LaunchedEffect(downloadError) {
        downloadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearDownloadError()
        }
    }

    val hasAnySong = currentSong != null || currentOnlineSong != null
    val displayTitle = currentSong?.title ?: currentOnlineSong?.title
    val displayArtist = currentSong?.artist ?: currentOnlineSong?.artist
    val displayArt = currentSong?.albumArt ?: currentOnlineSong?.thumbnailUrl

    val isOnNowPlaying = currentRoute == Screen.NowPlaying.route

    // Routes where the nav bar should be hidden
    val hideNavRoutes = setOf(Screen.NowPlaying.route, Screen.DualBud.route, Screen.SyncPlay.route, Screen.MyPlayerAudioLab.route, Screen.MyPlayerEqualizer.route, Screen.MyPlayerAppearance.route, "playlist_detail/{playlistId}")
    val showNav = currentRoute != null && !hideNavRoutes.any { currentRoute.startsWith(it.split("{")[0]) }

    if (!myplayerState.onboardingDone) {
        MyPlayerOnboardingFlow(
            myplayerState = myplayerState,
            spotifyManager = homeViewModel.spotifyManager,
            onComplete = { username, selectedArtists ->
                myplayerState.completeOnboarding(username, selectedArtists)
                homeViewModel.setUserName(username)
                homeViewModel.saveUserTastePreferences(
                    setOf("Hindi", "Punjabi"),
                    setOf("Bollywood", "Pop"),
                    selectedArtists
                )
                // What's New greets the user right after onboarding, never during it.
                if (WhatsNewRepository.shouldShow(context)) showWhatsNew = true
            }
        )
        return
    }

    if (showWhatsNew) {
        WhatsNewDialog(
            onDismiss = {
                showWhatsNew = false
                WhatsNewRepository.markSeen(context)
            }
        )
    }

    if (showStylePicker) {
        Dialog(
            onDismissRequest = { showStylePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MyPlayerPlayerStylesPickerScreen(
                myplayerState = myplayerState,
                previewTitle = displayTitle ?: "Khat",
                previewArtist = displayArtist ?: "Navjot Ahuja",
                previewArtUri = displayArt,
                onBack = { showStylePicker = false }
            )
        }
    }

    // Content draws behind the transparent miniplayer + navbar; screens get the bar height as list padding.
    val density = androidx.compose.ui.platform.LocalDensity.current
    var barHeight by remember { mutableStateOf(0.dp) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNav) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { barHeight = with(density) { it.size.height.toDp() } }
                        .background(androidx.compose.ui.graphics.Color.Transparent),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Mini player above nav bar
                    AnimatedVisibility(
                        visible = hasAnySong,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + scaleIn(initialScale = 0.88f) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        // Memoize synthetic SongEntity: only rebuild when the online song reference
                        // changes, not on every isPlaying / any-state recomposition.
                        val miniSong = currentSong?.let {
                            if (it.albumArt.isNullOrBlank() && !displayArt.isNullOrBlank()) {
                                it.copy(albumArt = displayArt)
                            } else {
                                it
                            }
                        } ?: remember(currentOnlineSong, displayArt) {
                            com.myplayer.app.data.local.entity.SongEntity(
                                id = currentOnlineSong?.videoId ?: "",
                                title = displayTitle ?: "",
                                artist = displayArtist ?: "",
                                album = "",
                                duration = 0,
                                path = displayArt ?: "",
                                albumArt = displayArt,
                                dateAdded = 0
                            )
                        }
                        MiniPlayer(
                            song = miniSong,
                            isPlaying = isPlaying,
                            onPlayPauseClick = remember(viewModel) { { viewModel.playPause() } },
                            onNextClick = remember(viewModel) { { viewModel.skipToNext() } },
                            onPreviousClick = remember(viewModel) { { viewModel.skipToPrevious() } },
                            mainViewModel = viewModel,
                            onPlayerClick = remember(navController) {
                                {
                                    navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                                }
                            }
                        )
                    }

                    FloatingNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (route == currentRoute) return@FloatingNavBar
                            // Home is the start destination: pop everything above it (Settings and its sub-screens included).
                            if (route == Screen.Home.route && navController.popBackStack(Screen.Home.route, inclusive = false)) {
                                return@FloatingNavBar
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(180))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(150)
                ) + fadeOut(tween(150))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + scaleIn(initialScale = 0.94f) + fadeIn(tween(180))
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut(tween(150))
            },
            modifier = Modifier
                .fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    },
                    bottomPadding = barHeight,
                    // Reuse the instance created above: a second hiltViewModel() here would run
                    // refreshAiRecommendations / trending / library sync a second time at startup.
                    viewModel = homeViewModel
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = barHeight
                )
            }
            composable(Screen.Trending.route) {
                com.myplayer.app.ui.screens.trending.TrendingScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = barHeight
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = barHeight
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId") { launchSingleTop = true }
                    },
                    onPlaySong = { songs, index ->
                        viewModel.playPlaylist(songs, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    },
                    myplayerState = myplayerState,
                    onOpenCustomizeStyles = { showStylePicker = true },
                    onOpenAudioLab = { navController.navigate(Screen.MyPlayerAudioLab.route) { launchSingleTop = true } },
                    onOpenAppearance = { navController.navigate(Screen.MyPlayerAppearance.route) { launchSingleTop = true } },
                    bottomPadding = barHeight
                )
            }
            composable("playlist_detail/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: -1L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { songs: List<com.myplayer.app.data.repository.PlayableSong>, index: Int ->
                        viewModel.playPlaylist(songs, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    }
                )
            }
            composable(
                route = Screen.NowPlaying.route,
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + scaleIn(initialScale = 0.90f) + fadeIn()
                },
                exitTransition = {
                    slideOutVertically(targetOffsetY = { it }) + fadeOut()
                },
                popExitTransition = {
                    slideOutVertically(targetOffsetY = { it }) +
                        scaleOut(targetScale = 0.90f) +
                        fadeOut()
                }
            ) {
                val sleepTimerRemaining by viewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
                val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
                val isShuffleOn by viewModel.isShuffleOn.collectAsStateWithLifecycle()
                val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
                val liveDuration by viewModel.currentDuration.collectAsStateWithLifecycle()
                val durationMs = if (liveDuration > 0L) {
                    liveDuration
                } else {
                    currentSong?.duration?.takeIf { it > 0L }
                        ?: currentOnlineSong?.durationMs?.takeIf { it > 0L }
                        ?: 0L
                }
                val canDownload by viewModel.isCurrentSongOnline.collectAsStateWithLifecycle()
                val isDownloaded by viewModel.isCurrentSongDownloaded.collectAsStateWithLifecycle()
                val isDownloading by viewModel.isCurrentSongDownloading.collectAsStateWithLifecycle()
                val currentAudioQuality by viewModel.currentAudioQuality.collectAsStateWithLifecycle()
                val positionState = viewModel.currentPosition.collectAsStateWithLifecycle()
                val queue by viewModel.playbackQueue.collectAsStateWithLifecycle()
                NowPlayingScreen(
                    myplayerState = myplayerState,
                    canDownload = canDownload,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    onDownloadClick = { viewModel.downloadCurrentSong() },
                    onRemoveDownloadClick = { viewModel.removeCurrentSongDownload() },
                    audioQuality = currentAudioQuality,
                    title = displayTitle,
                    artist = displayArtist,
                    artUri = currentSong?.albumArt ?: currentOnlineSong?.thumbnailUrl,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    positionState = positionState,
                    onPlayPauseClick = { viewModel.playPause() },
                    onNextClick = { viewModel.skipToNext() },
                    onPreviousClick = { viewModel.skipToPrevious() },
                    onSeek = { viewModel.seekTo(it) },
                    onBackClick = { navController.popBackStack() },
                    isFavorite = isFavorite,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    sleepTimerRemaining = sleepTimerRemaining,
                    onStartSleepTimer = { viewModel.startSleepTimer(it) },
                    onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                    isShuffleOn = isShuffleOn,
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    repeatMode = repeatMode,
                    onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    queue = queue,
                    onPlayQueueIndex = { viewModel.playQueueIndex(it) },
                    onOpenCustomizeStyles = { showStylePicker = true }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToDualBud = { navController.navigate(Screen.DualBud.route) { launchSingleTop = true } },
                    onNavigateToSyncPlay = { navController.navigate(Screen.SyncPlay.route) { launchSingleTop = true } },
                    onNavigateToAudioPlayback = { navController.navigate(Screen.AudioPlayback.route) { launchSingleTop = true } },
                    myplayerState = myplayerState,
                    onNavigateToPlayerStyles = { showStylePicker = true },
                    onNavigateToMyPlayerAppearance = { navController.navigate(Screen.MyPlayerAppearance.route) { launchSingleTop = true } },
                    onNavigateToMyPlayerAudioLab = { navController.navigate(Screen.MyPlayerAudioLab.route) { launchSingleTop = true } },
                    bottomPadding = barHeight
                )
            }
            composable(Screen.MyPlayerAudioLab.route) {
                MyPlayerAudioLabHubScreen(
                    myplayerState = myplayerState,
                    onOpenEqualizer = { navController.navigate(Screen.MyPlayerEqualizer.route) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MyPlayerEqualizer.route) {
                MyPlayerEqualizerScreen(
                    myplayerState = myplayerState,
                    onApplyGainsToEngine = { enabled, gains, preamp -> MyPlayerAudioFxDspBridge.setEq(enabled, gains, preamp) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MyPlayerAppearance.route) {
                MyPlayerAppearanceSettingsScreen(
                    myplayerState = myplayerState,
                    onOpenStylesPicker = { showStylePicker = true },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DualBud.route) {
                com.myplayer.app.dualbud.ui.DualBudScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SyncPlay.route) {
                com.myplayer.app.sync.ui.SyncPlayScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.AudioPlayback.route) {
                val settingsVm: com.myplayer.app.ui.screens.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                com.myplayer.app.ui.settings.AudioAndPlaybackScreen(
                    store = settingsVm.audioPlaybackStore,
                    featureManagers = settingsVm.audioFeatureManagers,
                    onBack = { navController.popBackStack() },
                    myplayerState = myplayerState
                )
            }
        }
    }
}


