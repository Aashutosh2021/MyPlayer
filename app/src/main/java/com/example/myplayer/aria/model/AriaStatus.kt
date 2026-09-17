package com.example.myplayer.aria.model

/**
 * Exhaustive set of status codes returned by every AriaResponse.
 * Clients MUST handle all cases — do not use else-branch in when expressions.
 */
enum class AriaStatus {
    /** Command executed successfully. */
    SUCCESS,

    /** Command failed due to an internal error. Check AriaResponse.errorMessage. */
    FAILURE,

    /** No song is currently playing or loaded. */
    NOTHING_PLAYING,

    /** Caller is not signed by a trusted certificate. */
    PERMISSION_DENIED,

    /** The requested item (song, playlist, etc.) could not be found. */
    NOT_FOUND,

    /** An identical item already exists (e.g. duplicate playlist name). */
    ALREADY_EXISTS,

    /** The operation has been accepted and queued for async execution. */
    QUEUED,

    /** The feature is recognised but not available in this build (e.g. live lyrics). */
    NOT_AVAILABLE,

    /** The requested operation was rejected because a conflicting one is in progress. */
    BUSY,

    /** The parameter bundle was malformed or missing required keys. */
    BAD_REQUEST,
}
