package com.example.aria.connector.sdk

import android.util.Log
import com.example.aria.connector.AppCommand
import com.example.aria.connector.AppResponse
import com.example.aria.connector.Capability

/**
 * Music-specific connector service for MyPlayer to extend.
 */
abstract class MusicConnectorService : AriaConnectorService() {

    private val tag = "MusicConnectorService"

    final override suspend fun handleCommand(command: AppCommand): AppResponse {
        if (command !is AppCommand.Music) {
            return AppResponse.Error(
                AppResponse.Error.ErrorCode.UNKNOWN,
                "This connector only handles Music commands."
            )
        }
        Log.d(tag, "▶ ${command.type}")
        return when (command) {
            is AppCommand.Music.Play              -> onPlay(command.query, command.queryType, command.mood)
            is AppCommand.Music.Pause             -> onPause()
            is AppCommand.Music.Resume            -> onResume()
            is AppCommand.Music.Next              -> onNext()
            is AppCommand.Music.Previous          -> onPrevious()
            is AppCommand.Music.Stop              -> onStop()
            is AppCommand.Music.Seek              -> onSeek(command.positionMs)
            is AppCommand.Music.SetVolume         -> onSetVolume(command.percent)
            is AppCommand.Music.SetRepeat         -> onSetRepeat(command.mode)
            is AppCommand.Music.SetShuffle        -> onSetShuffle(command.enabled)
            is AppCommand.Music.GetCurrentTrack   -> onGetCurrentTrack()
            is AppCommand.Music.GetQueue          -> onGetQueue()
            is AppCommand.Music.GetHistory        -> onGetHistory()
            is AppCommand.Music.Search            -> onSearch(command.query)
            is AppCommand.Music.AddToQueue        -> onAddToQueue(command.query)
            is AppCommand.Music.AddToFavorites    -> onAddToFavorites()
            is AppCommand.Music.CreatePlaylist    -> onCreatePlaylist(command.name)
            is AppCommand.Music.AddToPlaylist     -> onAddToPlaylist(command.playlistName, command.trackId)
            is AppCommand.Music.GetPlaylists      -> onGetPlaylists()
            is AppCommand.Music.Download          -> onDownload(command.trackId)
            is AppCommand.Music.GetLyrics         -> onGetLyrics()
            is AppCommand.Music.GetRecommendations -> onGetRecommendations(command.mood, command.context)
            is AppCommand.Music.SetSleepTimer     -> onSetSleepTimer(command.minutes)
            is AppCommand.Music.SetEqualizer      -> onSetEqualizer(command.preset)
        }
    }

    // ── Override these in MyPlayer ────────────────────────────────────────────

    open suspend fun onPlay(
        query: String,
        queryType: AppCommand.Music.Play.QueryType,
        mood: String?,
    ): AppResponse = AppResponse.NotSupported

    open suspend fun onPause(): AppResponse = AppResponse.NotSupported
    open suspend fun onResume(): AppResponse = AppResponse.NotSupported
    open suspend fun onNext(): AppResponse = AppResponse.NotSupported
    open suspend fun onPrevious(): AppResponse = AppResponse.NotSupported
    open suspend fun onStop(): AppResponse = AppResponse.NotSupported
    open suspend fun onSeek(positionMs: Long): AppResponse = AppResponse.NotSupported
    open suspend fun onSetVolume(percent: Int): AppResponse = AppResponse.NotSupported
    open suspend fun onSetRepeat(mode: AppCommand.Music.SetRepeat.RepeatMode): AppResponse = AppResponse.NotSupported
    open suspend fun onSetShuffle(enabled: Boolean): AppResponse = AppResponse.NotSupported
    open suspend fun onGetCurrentTrack(): AppResponse = AppResponse.NotSupported
    open suspend fun onGetQueue(): AppResponse = AppResponse.NotSupported
    open suspend fun onGetHistory(): AppResponse = AppResponse.NotSupported
    open suspend fun onSearch(query: String): AppResponse = AppResponse.NotSupported
    open suspend fun onAddToQueue(query: String): AppResponse = AppResponse.NotSupported
    open suspend fun onAddToFavorites(): AppResponse = AppResponse.NotSupported
    open suspend fun onCreatePlaylist(name: String): AppResponse = AppResponse.NotSupported
    open suspend fun onAddToPlaylist(playlistName: String, trackId: String): AppResponse = AppResponse.NotSupported
    open suspend fun onGetPlaylists(): AppResponse = AppResponse.NotSupported
    open suspend fun onDownload(trackId: String): AppResponse = AppResponse.NotSupported
    open suspend fun onGetLyrics(): AppResponse = AppResponse.NotSupported
    open suspend fun onGetRecommendations(mood: String?, context: String?): AppResponse = AppResponse.NotSupported
    open suspend fun onSetSleepTimer(minutes: Int): AppResponse = AppResponse.NotSupported
    open suspend fun onSetEqualizer(preset: String): AppResponse = AppResponse.NotSupported
}
