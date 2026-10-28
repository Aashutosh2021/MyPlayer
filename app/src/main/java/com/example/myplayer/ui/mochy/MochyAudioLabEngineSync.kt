package com.example.myplayer.ui.mochy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext

/**
 * Keeps the real audio engine in sync with the Audio Lab settings for the whole lifetime of the UI.
 *
 * Before: the sync lived inside MochyAudioFxScreen, so effects only reached the engine while that one
 * screen was on screen, and everything reset to "off" after an app restart.
 * Now: placed once in MainScreen. It (1) restores the fine-tune sliders, (2) pushes the saved settings
 * into the DSP right at app start, and (3) re-pushes + saves on every change from ANY Audio Lab screen.
 */
@Composable
fun MochyAudioLabEngineSync(mochyState: MochyUiStateHolder) {
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(mochyState) {
        MochyAudioFxDspBridge.loadFineTune(appContext)
        snapshotFlow { mochyState.audioLabSignature() }.collect {
            MochyAudioFxDspBridge.syncFromUiState(mochyState)
            mochyState.saveAudioLabToPrefs()
        }
    }
}
