package com.example.myplayer.data.model

import com.example.myplayer.data.local.entity.DownloadedSongEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.repository.PlayableSong
import com.example.myplayer.playback.PlayRequest
import com.example.myplayer.playback.PlaybackSourceType

/**
 * Centralised mapper functions converting domain-specific song models → [MusicItem].
 *
 * Rules:
 * - Never duplicate mapping logic outside this file.
 * - Never embed PlayRequest construction outside this file.
 * - All callers must go through these extension functions.
 *
 * Introduced in Phase R7 — Data Model Normalization.
 */

// ── SongEntity → MusicItem ────────────────────────────────────────────────────

fun SongEntity.toMusicItem(
    downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    isFavorite: Boolean = false
): MusicItem = MusicItem(
    id = id,
    videoId = videoId,
    title = title,
    artist = artist,
    album = album,
    durationMs = duration,
    artworkUri = albumArt,
    playbackType = if (path.startsWith("online://")) PlaybackType.ONLINE else PlaybackType.LOCAL,
    localUri = if (path.startsWith("online://")) null else path,
    downloadStatus = downloadStatus,
    isFavorite = isFavorite
)

// ── DownloadedSongEntity → MusicItem ─────────────────────────────────────────

fun DownloadedSongEntity.toMusicItem(
    isFavorite: Boolean = false
): MusicItem = MusicItem(
    id = id,
    videoId = id,            // Downloaded songs use videoId as primary key
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = thumbnailUrl,
    playbackType = PlaybackType.DOWNLOADED,
    localUri = localPath,
    downloadStatus = DownloadStatus.DOWNLOADED,
    isFavorite = isFavorite
)

// ── OnlineSong → MusicItem ────────────────────────────────────────────────────

fun OnlineSong.toMusicItem(): MusicItem = MusicItem(
    id = videoId,
    videoId = videoId,
    title = title,
    artist = artist,
    album = "",
    durationMs = durationMs,
    artworkUri = thumbnailUrl,
    playbackType = PlaybackType.ONLINE,
    localUri = null,
    downloadStatus = DownloadStatus.NOT_DOWNLOADED
)

// ── RecommendationSong → MusicItem ────────────────────────────────────────────

fun RecommendationSong.toMusicItem(): MusicItem = MusicItem(
    id = videoId,
    videoId = videoId,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = thumbnailUrl,
    playbackType = PlaybackType.RECOMMENDATION,
    localUri = null,
    downloadStatus = DownloadStatus.NOT_DOWNLOADED,
    metadata = metadata + mapOf(
        "source" to source,
        "reason" to reason,
        "popularityScore" to popularityScore.toString(),
        "recommendationScore" to recommendationScore.toString()
    )
)

// ── PlayableSong → MusicItem ──────────────────────────────────────────────────

fun PlayableSong.toMusicItem(): MusicItem = when (this) {
    is PlayableSong.Local -> entity.toMusicItem()
    is PlayableSong.Downloaded -> entity.toMusicItem()
    is PlayableSong.Online -> MusicItem(
        id = id,
        videoId = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = durationMs,
        artworkUri = thumbnailUrl,
        playbackType = PlaybackType.ONLINE,
        localUri = null,
        downloadStatus = DownloadStatus.NOT_DOWNLOADED
    )
}

// ── MusicItem → PlayRequest ───────────────────────────────────────────────────

fun MusicItem.toPlayRequest(): PlayRequest {
    val sourceType = when (playbackType) {
        PlaybackType.LOCAL -> PlaybackSourceType.LOCAL
        PlaybackType.DOWNLOADED -> PlaybackSourceType.DOWNLOADED
        PlaybackType.ONLINE -> PlaybackSourceType.ONLINE
        PlaybackType.RECOMMENDATION -> PlaybackSourceType.RECOMMENDATION
    }
    return PlayRequest(
        songId = id,
        title = title,
        artist = artist,
        playbackSource = sourceType,
        localUri = localUri ?: "online://$id",
        albumArt = artworkUri,
        metadata = metadata
    )
}

// ── PlayableSong → SongEntity (Stubs for DB cross-referencing) ───────────────

fun PlayableSong.Online.toSongEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = "YouTube Music",
    duration = durationMs,
    path = "online://$id",
    albumArt = thumbnailUrl,
    dateAdded = System.currentTimeMillis(),
    videoId = id
)

fun PlayableSong.Downloaded.toSongEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = entity.album.ifBlank { "Downloads" },
    duration = durationMs,
    path = entity.localPath,
    albumArt = thumbnailUrl,
    dateAdded = System.currentTimeMillis(),
    videoId = id
)

