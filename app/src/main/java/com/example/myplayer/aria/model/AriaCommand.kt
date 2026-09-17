package com.example.myplayer.aria.model

/**
 * Every command ARIA clients can send to AriaService.
 *
 * The [ordinal] is used as the Message.what integer over the Messenger transport.
 * DO NOT reorder or remove entries — only append new entries at the end.
 * Removing/reordering would break binary compatibility with compiled client apps.
 *
 * Each entry documents:
 *  - Required Bundle keys (keys prefixed with ARIA_KEY_*)
 *  - The AriaStatus values it may return
 */
enum class AriaCommand {

    // ── Playback ──────────────────────────────────────────────────────────────

    /**
     * Play a song by its ID. Works for local (SongEntity.id) and downloaded (videoId) songs.
     * Required keys: ARIA_KEY_SONG_ID (String)
     * Optional keys: ARIA_KEY_SOURCE (String: "local"|"downloaded"|"online")
     * Returns: SUCCESS | NOT_FOUND | FAILURE
     */
    PLAY,

    /**
     * Pause current playback.
     * Returns: SUCCESS | NOTHING_PLAYING
     */
    PAUSE,

    /**
     * Resume paused playback.
     * Returns: SUCCESS | NOTHING_PLAYING
     */
    RESUME,

    /**
     * Stop playback and clear current item.
     * Returns: SUCCESS
     */
    STOP,

    /**
     * Skip to next song in queue.
     * Returns: SUCCESS | NOTHING_PLAYING
     */
    NEXT,

    /**
     * Go to previous song.
     * Returns: SUCCESS | NOTHING_PLAYING
     */
    PREVIOUS,

    /**
     * Seek to a position in the current song.
     * Required keys: ARIA_KEY_POSITION_MS (Long)
     * Returns: SUCCESS | NOTHING_PLAYING | BAD_REQUEST
     */
    SEEK,

    /**
     * Enable or disable shuffle mode.
     * Required keys: ARIA_KEY_ENABLED (Boolean)
     * Returns: SUCCESS
     */
    SHUFFLE,

    /**
     * Set repeat mode.
     * Required keys: ARIA_KEY_REPEAT_MODE (Int: 0=off, 1=all, 2=one)
     * Returns: SUCCESS
     */
    REPEAT,

    /**
     * Set playback speed.
     * Required keys: ARIA_KEY_SPEED (Float, 0.25–2.0)
     * Returns: SUCCESS | BAD_REQUEST
     */
    PLAYBACK_SPEED,

    /**
     * Set volume (app-level, 0.0–1.0).
     * Required keys: ARIA_KEY_VOLUME (Float)
     * Returns: SUCCESS | BAD_REQUEST
     */
    VOLUME,

    // ── Playlist ──────────────────────────────────────────────────────────────

    /**
     * Play an entire playlist starting at a given index.
     * Required keys: ARIA_KEY_PLAYLIST_ID (Long)
     * Optional keys: ARIA_KEY_START_INDEX (Int, default 0)
     * Returns: SUCCESS | NOT_FOUND | FAILURE
     */
    PLAY_PLAYLIST,

    /**
     * Create a new playlist.
     * Required keys: ARIA_KEY_PLAYLIST_NAME (String)
     * Returns: SUCCESS (payload contains ARIA_KEY_PLAYLIST_ID) | ALREADY_EXISTS | FAILURE
     */
    CREATE_PLAYLIST,

    /**
     * Rename an existing playlist.
     * Required keys: ARIA_KEY_PLAYLIST_ID (Long), ARIA_KEY_PLAYLIST_NAME (String)
     * Returns: SUCCESS | NOT_FOUND | FAILURE
     */
    RENAME_PLAYLIST,

    /**
     * Delete a playlist permanently.
     * Required keys: ARIA_KEY_PLAYLIST_ID (Long)
     * Returns: SUCCESS | NOT_FOUND | FAILURE
     */
    DELETE_PLAYLIST,

    // ── Queue ─────────────────────────────────────────────────────────────────

    /**
     * Add a song to the end of the current queue.
     * Required keys: ARIA_KEY_SONG_ID (String)
     * Returns: SUCCESS | NOT_FOUND | NOTHING_PLAYING
     */
    QUEUE_SONG,

    /**
     * Get the current playback queue as a serialised QueueInfo.
     * Returns: SUCCESS (payload contains ARIA_KEY_QUEUE) | NOTHING_PLAYING
     */
    GET_QUEUE,

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Search for songs (local + online).
     * Required keys: ARIA_KEY_QUERY (String)
     * Optional keys: ARIA_KEY_SOURCE (String: "local"|"online"|"all", default "all")
     * Returns: SUCCESS (payload contains ARIA_KEY_RESULTS) | FAILURE
     */
    SEARCH_SONG,

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Start downloading a song from YouTube Music.
     * Required keys: ARIA_KEY_SONG_ID (String, videoId), ARIA_KEY_TITLE (String), ARIA_KEY_ARTIST (String)
     * Returns: QUEUED | ALREADY_EXISTS | FAILURE
     */
    DOWNLOAD_SONG,

    /**
     * Delete a downloaded song from local storage and the database.
     * Required keys: ARIA_KEY_SONG_ID (String, videoId)
     * Returns: SUCCESS | NOT_FOUND | FAILURE
     */
    DELETE_DOWNLOAD,

    // ── Favorites ─────────────────────────────────────────────────────────────

    /**
     * Like (add to favorites) a song.
     * Required keys: ARIA_KEY_SONG_ID (String)
     * Returns: SUCCESS | NOT_FOUND | ALREADY_EXISTS
     */
    LIKE_SONG,

    /**
     * Dislike (remove from favorites) a song.
     * Required keys: ARIA_KEY_SONG_ID (String)
     * Returns: SUCCESS | NOT_FOUND
     */
    DISLIKE_SONG,

    /**
     * Favorite an artist.
     * Required keys: ARIA_KEY_ARTIST (String)
     * Returns: SUCCESS | NOT_FOUND | NOT_AVAILABLE
     */
    FAVORITE_ARTIST,

    /**
     * Favorite an album.
     * Required keys: ARIA_KEY_TITLE (String)
     * Returns: SUCCESS | NOT_FOUND | NOT_AVAILABLE
     */
    FAVORITE_ALBUM,

    // ── Metadata ──────────────────────────────────────────────────────────────

    /**
     * Get full metadata for the currently playing song.
     * Returns: SUCCESS (payload contains ARIA_KEY_SONG_INFO) | NOTHING_PLAYING
     */
    GET_CURRENT_SONG,

    /**
     * Get lyrics for the currently playing song (from cache).
     * Returns: SUCCESS (payload contains ARIA_KEY_LYRICS) | NOTHING_PLAYING | NOT_AVAILABLE
     */
    GET_LYRICS,

    /**
     * Get personalised recommendations based on play history.
     * Returns: SUCCESS (payload contains ARIA_KEY_RESULTS) | FAILURE
     */
    GET_RECOMMENDATIONS,

    /**
     * Get recently played songs.
     * Optional keys: ARIA_KEY_LIMIT (Int, default 20)
     * Returns: SUCCESS (payload contains ARIA_KEY_RESULTS)
     */
    GET_LISTENING_HISTORY,

    // ── Timers & Advanced ─────────────────────────────────────────────────────

    /**
     * Start or cancel a sleep timer.
     * Required keys: ARIA_KEY_MINUTES (Int, 0 = cancel)
     * Returns: SUCCESS
     */
    SLEEP_TIMER,

    /**
     * Apply an equalizer preset or custom bands.
     * Required keys: ARIA_KEY_EQ_PRESET (String) OR ARIA_KEY_EQ_BANDS (FloatArray)
     * Returns: SUCCESS | NOT_AVAILABLE | BAD_REQUEST
     */
    EQUALIZER,

    /**
     * Toggle offline mode (disable all network requests).
     * Required keys: ARIA_KEY_ENABLED (Boolean)
     * Returns: SUCCESS
     */
    OFFLINE_MODE,

    /**
     * Enable or disable crossfade between tracks.
     * Required keys: ARIA_KEY_ENABLED (Boolean)
     * Optional keys: ARIA_KEY_DURATION_MS (Int, crossfade duration)
     * Returns: SUCCESS | NOT_AVAILABLE
     */
    CROSSFADE,

    /**
     * Cancel an active transaction by transaction ID.
     * Required keys: ARIA_KEY_TRANSACTION_ID (String)
     */
    CANCEL,

    /**
     * Start a real-time progress update stream (position, duration, state) for the active track.
     * Required keys: ARIA_KEY_TRANSACTION_ID (String)
     */
    START_PLAYBACK_PROGRESS_STREAM,

    /**
     * Stop a playback progress stream.
     * Required keys: ARIA_KEY_TRANSACTION_ID (String)
     */
    STOP_PLAYBACK_PROGRESS_STREAM,

    /**
     * Register a callback Messenger for all event broadcasts.
     */
    REGISTER_EVENT_CALLBACK,

    /**
     * Unregister an event callback Messenger.
     */
    UNREGISTER_EVENT_CALLBACK,

    /**
     * Get system, playback, and queue health statistics.
     */
    GET_HEALTH,
}

// ── Bundle key constants ──────────────────────────────────────────────────────
// All keys used in command and response Bundles are centralised here.

const val ARIA_KEY_SONG_ID        = "aria.song_id"
const val ARIA_KEY_SONG_INFO      = "aria.song_info"
const val ARIA_KEY_QUEUE          = "aria.queue"
const val ARIA_KEY_RESULTS        = "aria.results"
const val ARIA_KEY_LYRICS         = "aria.lyrics"
const val ARIA_KEY_QUERY          = "aria.query"
const val ARIA_KEY_SOURCE         = "aria.source"
const val ARIA_KEY_POSITION_MS    = "aria.position_ms"
const val ARIA_KEY_ENABLED        = "aria.enabled"
const val ARIA_KEY_REPEAT_MODE    = "aria.repeat_mode"
const val ARIA_KEY_SPEED          = "aria.speed"
const val ARIA_KEY_VOLUME         = "aria.volume"
const val ARIA_KEY_PLAYLIST_ID    = "aria.playlist_id"
const val ARIA_KEY_PLAYLIST_NAME  = "aria.playlist_name"
const val ARIA_KEY_START_INDEX    = "aria.start_index"
const val ARIA_KEY_TITLE          = "aria.title"
const val ARIA_KEY_ARTIST         = "aria.artist"
const val ARIA_KEY_LIMIT          = "aria.limit"
const val ARIA_KEY_MINUTES        = "aria.minutes"
const val ARIA_KEY_EQ_PRESET      = "aria.eq_preset"
const val ARIA_KEY_EQ_BANDS       = "aria.eq_bands"
const val ARIA_KEY_DURATION_MS    = "aria.duration_ms"
const val ARIA_KEY_THUMBNAIL_URL  = "aria.thumbnail_url"
const val ARIA_KEY_TRANSACTION_ID = "aria.transaction_id"
