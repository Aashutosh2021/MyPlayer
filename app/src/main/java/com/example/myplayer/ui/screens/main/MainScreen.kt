package com.example.myplayer.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myplayer.ui.components.FloatingNavBar
import com.example.myplayer.ui.navigation.Screen
import com.example.myplayer.ui.screens.downloads.DownloadsScreen
import com.example.myplayer.ui.screens.home.HomeScreen
import com.example.myplayer.ui.screens.library.LibraryScreen
import com.example.myplayer.ui.screens.library.PlaylistDetailScreen
import com.example.myplayer.ui.screens.nowplaying.NowPlayingScreen
import com.example.myplayer.ui.screens.search.SearchScreen
import com.example.myplayer.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentOnlineSong by viewModel.currentOnlineSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isShuffleOn by viewModel.isShuffleOn.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()

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
    val hideNavRoutes = setOf(Screen.NowPlaying.route, Screen.DualBud.route, "playlist_detail/{playlistId}")
    val showNav = currentRoute != null && !hideNavRoutes.any { currentRoute.startsWith(it.split("{")[0]) }

    val currentBottomPadding = if (hasAnySong) 190.dp else 120.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Content fills entire screen ─────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = currentBottomPadding
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = currentBottomPadding
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = currentBottomPadding
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId")
                    },
                    onPlaySong = { songs, index ->
                        viewModel.playPlaylist(songs, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = currentBottomPadding
                )
            }
            composable("playlist_detail/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: -1L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { songs: List<com.example.myplayer.data.repository.PlayableSong>, index: Int ->
                        viewModel.playPlaylist(songs, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.NowPlaying.route) {
                val durationMs = currentSong?.duration ?: viewModel.currentDuration.collectAsStateWithLifecycle().value
                val canDownload by viewModel.isCurrentSongOnline.collectAsStateWithLifecycle()
                val isDownloaded by viewModel.isCurrentSongDownloaded.collectAsStateWithLifecycle()
                val isDownloading by viewModel.isCurrentSongDownloading.collectAsStateWithLifecycle()
                val currentAudioQuality by viewModel.currentAudioQuality.collectAsStateWithLifecycle()
                val positionState = viewModel.currentPosition.collectAsStateWithLifecycle()
                NowPlayingScreen(
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
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDualBud = { navController.navigate(Screen.DualBud.route) { launchSingleTop = true } },
                    bottomPadding = currentBottomPadding
                )
            }
            composable(Screen.DualBud.route) {
                com.example.myplayer.dualbud.ui.DualBudScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // ── Floating overlay: mini player + nav bar ─────────────────────────
        if (showNav) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(10f),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Mini player above nav bar
                if (hasAnySong) {
                    // Memoize synthetic SongEntity: only rebuild when the online song reference
                // changes, not on every isPlaying / position / any-state recomposition.
                val miniSong = currentSong ?: remember(currentOnlineSong) {
                    com.example.myplayer.data.local.entity.SongEntity(
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
                        onPlayPauseClick = { viewModel.playPause() },
                        onNextClick = { viewModel.skipToNext() },
                        onPlayerClick = {
                            navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                        }
                    )
                }

                FloatingNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
