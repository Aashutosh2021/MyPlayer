package com.example.myplayer.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object NowPlaying : Screen("now_playing")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object DualBud : Screen("dual_bud")
}
