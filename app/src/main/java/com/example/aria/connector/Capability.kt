package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Enum of every capability an ecosystem app can declare.
 *
 * ARIA uses this to route commands — it never hardcodes which app supports what.
 * MyPlayer (or any other app) declares its subset at bind time.
 */
@Parcelize
enum class Capability(
    /** Human-readable label for logging and UI. */
    val label: String,
    /** Category this capability belongs to (used for routing in ConnectorRegistry). */
    val category: Category,
) : Parcelable {
    // ── Playback ───────────────────────────────────────────────────────────────
    PLAY("Play Music", Category.MUSIC),
    PAUSE("Pause Playback", Category.MUSIC),
    RESUME("Resume Playback", Category.MUSIC),
    NEXT("Next Track", Category.MUSIC),
    PREVIOUS("Previous Track", Category.MUSIC),
    SEEK("Seek Position", Category.MUSIC),
    STOP("Stop Playback", Category.MUSIC),

    // ── Queue / Library ────────────────────────────────────────────────────────
    GET_QUEUE("Get Queue", Category.MUSIC),
    ADD_TO_QUEUE("Add to Queue", Category.MUSIC),
    GET_CURRENT_TRACK("Get Current Track", Category.MUSIC),
    SEARCH("Search", Category.MUSIC),
    GET_HISTORY("Get Playback History", Category.MUSIC),

    // ── Library Management ─────────────────────────────────────────────────────
    FAVORITE("Favorite Track", Category.MUSIC),
    CREATE_PLAYLIST("Create Playlist", Category.MUSIC),
    ADD_TO_PLAYLIST("Add to Playlist", Category.MUSIC),
    GET_PLAYLISTS("Get Playlists", Category.MUSIC),

    // ── Audio ─────────────────────────────────────────────────────────────────
    SET_VOLUME("Set Volume", Category.MUSIC),
    SET_REPEAT("Set Repeat Mode", Category.MUSIC),
    SET_SHUFFLE("Set Shuffle", Category.MUSIC),
    SET_EQUALIZER("Set Equalizer", Category.MUSIC),

    // ── Content ───────────────────────────────────────────────────────────────
    GET_LYRICS("Get Lyrics", Category.MUSIC),
    GET_RECOMMENDATIONS("Get Recommendations", Category.MUSIC),
    DOWNLOAD("Download Track", Category.MUSIC),

    // ── Timers ────────────────────────────────────────────────────────────────
    SLEEP_TIMER("Sleep Timer", Category.MUSIC),

    // ── Future categories (reserved) ──────────────────────────────────────────
    VIDEO_PLAY("Play Video", Category.VIDEO),
    VIDEO_PAUSE("Pause Video", Category.VIDEO),
    SMART_HOME_CONTROL("Smart Home Control", Category.SMART_HOME),
    FITNESS_LOG("Log Fitness", Category.FITNESS),
    NOTES_CREATE("Create Note", Category.PRODUCTIVITY),
    NOTES_READ("Read Notes", Category.PRODUCTIVITY);

    enum class Category {
        MUSIC, VIDEO, SMART_HOME, FITNESS, PRODUCTIVITY, GENERAL
    }
}
