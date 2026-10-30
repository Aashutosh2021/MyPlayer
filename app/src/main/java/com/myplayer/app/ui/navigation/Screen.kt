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
    object MyPlayerAudioLab : Screen("myplayer_audio_lab")
    object MyPlayerEqualizer : Screen("myplayer_equalizer")
    object MyPlayerAppearance : Screen("myplayer_appearance")
}


