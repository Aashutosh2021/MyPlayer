package com.example.myplayer.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    val currentSong by viewModel.currentSong.collectAsState()
    val currentOnlineSong by viewModel.currentOnlineSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemainingSeconds.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    val hasAnySong = currentSong != null || currentOnlineSong != null
    val displayTitle = currentSong?.title ?: currentOnlineSong?.title
    val displayArtist = currentSong?.artist ?: currentOnlineSong?.artist
    val displayArt = currentSong?.albumArt ?: currentOnlineSong?.thumbnailUrl

    val isOnNowPlaying = currentRoute == Screen.NowPlaying.route

    // Routes where the nav bar should be hidden
    val hideNavRoutes = setOf(Screen.NowPlaying.route, "playlist_detail/{playlistId}", Screen.Settings.route)
    val showNav = currentRoute != null && !hideNavRoutes.any { currentRoute.startsWith(it.split("{")[0]) }

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
                    bottomPadding = if (hasAnySong) 160.dp else 100.dp
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = if (hasAnySong) 160.dp else 100.dp
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = if (hasAnySong) 160.dp else 100.dp
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId")
                    },
                    onPlaySong = { song ->
                        viewModel.playSong(song)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    bottomPadding = if (hasAnySong) 160.dp else 100.dp
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
                val durationMs = currentSong?.duration ?: viewModel.currentDuration.collectAsState().value
                val canDownload by viewModel.isCurrentSongOnline.collectAsState()
                val isDownloaded by viewModel.isCurrentSongDownloaded.collectAsState()
                val isDownloading by viewModel.isCurrentSongDownloading.collectAsState()
                NowPlayingScreen(
                    canDownload = canDownload,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    onDownloadClick = { viewModel.downloadCurrentSong() },
                    title = displayTitle,
                    artist = displayArtist,
                    artUri = currentSong?.albumArt ?: currentOnlineSong?.thumbnailUrl,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    currentPosition = viewModel.currentPosition.collectAsState().value,
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
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // ── Floating overlay: mini player + nav bar ─────────────────────────
        if (showNav) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Mini player above nav bar
                if (hasAnySong) {
                    val miniSong = currentSong
                        ?: com.example.myplayer.data.local.entity.SongEntity(
                            id = currentOnlineSong?.videoId ?: "",
                            title = displayTitle ?: "",
                            artist = displayArtist ?: "",
                            album = "",
                            duration = 0,
                            path = displayArt ?: "",
                            albumArt = displayArt,
                            dateAdded = 0
                        )
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
