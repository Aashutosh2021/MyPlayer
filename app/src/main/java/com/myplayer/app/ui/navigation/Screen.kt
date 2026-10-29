package com.myplayer.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Trending : Screen("trending")
    object NowPlaying : Screen("now_playing")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object DualBud : Screen("dual_bud")
    object SyncPlay : Screen("sync_play")
    object AudioPlayback : Screen("audio_playback")
    object MochyAudioLab : Screen("mochy_audio_lab")
    object MochyEqualizer : Screen("mochy_equalizer")
    object MochyAppearance : Screen("mochy_appearance")
}


