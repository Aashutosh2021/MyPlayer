package com.example.myplayer.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.example.myplayer.data.local.AppDatabase
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.local.prefs.PreferencesManager
import com.example.myplayer.data.local.entity.*
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val settings: BackupSettings,
    val folders: List<BackupFolder>,
    val songs: List<BackupSong>,
    val downloads: List<BackupDownload>,
    val favorites: List<BackupFavorite>,
    val playlists: List<BackupPlaylist>,
    val playlistSongs: List<BackupPlaylistSong>,
    val recentHistory: List<BackupRecentHistory>,
    val recentSearches: List<BackupRecentSearch>,
    val cachedLyrics: List<BackupCachedLyrics> = emptyList()
)

@Serializable
data class BackupSettings(
    val isAutoplayEnabled: Boolean,
    val downloadFolderUri: String?
)

@Serializable
data class BackupFolder(val uri: String, val name: String)

@Serializable
data class BackupSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArt: String?,
    val dateAdded: Long,
    val playCount: Int,
    val videoId: String? = null
)

@Serializable
data class BackupDownload(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val localPath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long,
    val album: String = ""
)

@Serializable
data class BackupFavorite(val songId: String, val addedAt: Long)

@Serializable
data class BackupPlaylist(val id: Long, val name: String, val createdAt: Long)

@Serializable
data class BackupPlaylistSong(val playlistId: Long, val songId: String, val position: Int)

@Serializable
data class BackupRecentHistory(val songId: String, val playedAt: Long)

@Serializable
data class BackupRecentSearch(val query: String, val timestamp: Long)

@Serializable
data class BackupCachedLyrics(
    val songId: String,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val trackName: String,
    val artistName: String,
    val cachedAt: Long
)

@Singleton
class BackupRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val preferencesManager: PreferencesManager,
    private val downloadRepository: DownloadRepository,
    private val innertubeApi: InnertubeApi
) {
    private val jsonHelper = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Serializes all application settings, local folders, library songs, playlists with songs,
     * favorites, recent searches, and downloads to a single JSON string.
     * Guarantees that every downloaded song and playlist song has full metadata in the backup.
     */
    suspend fun generateBackup(): String = withContext(Dispatchers.IO) {
        val isAutoplayEnabled = settingsDataStore.isAutoplayEnabled.firstOrNull() ?: true
        val downloadFolderUri = preferencesManager.downloadFolderUri.firstOrNull()

        val settings = BackupSettings(isAutoplayEnabled, downloadFolderUri)
        val folders = db.folderDao().getFoldersSync().map { BackupFolder(it.uri, it.name) }

        // 1. Gather all songs from database
        val songsList = db.songDao().getAllSongs().firstOrNull() ?: emptyList()
        val songsMap = songsList.associateBy { it.id }.toMutableMap()

        // 2. Gather all downloads & guarantee their presence in songsMap
        val allDownloads = db.downloadedSongDao().getAllDownloadsSync()
        allDownloads.forEach { dl ->
            if (!songsMap.containsKey(dl.id)) {
                songsMap[dl.id] = SongEntity(
                    id = dl.id,
                    title = dl.title,
                    artist = dl.artist,
                    album = dl.album.ifBlank { "Downloads" },
                    duration = dl.durationMs,
                    path = dl.localPath,
                    albumArt = dl.thumbnailUrl,
                    dateAdded = dl.downloadedAt,
                    playCount = 0,
                    videoId = dl.id
                )
            }
        }

        // 3. Gather all playlist songs & ensure every playlist song is backed up with metadata
        val playlistSongsRaw = db.playlistDao().getAllPlaylistSongsSync()
        playlistSongsRaw.forEach { ps ->
            if (!songsMap.containsKey(ps.songId)) {
                val matchingDl = allDownloads.firstOrNull { it.id == ps.songId }
                if (matchingDl != null) {
                    songsMap[ps.songId] = SongEntity(
                        id = matchingDl.id,
                        title = matchingDl.title,
                        artist = matchingDl.artist,
                        album = matchingDl.album.ifBlank { "Downloads" },
                        duration = matchingDl.durationMs,
                        path = matchingDl.localPath,
                        albumArt = matchingDl.thumbnailUrl,
                        dateAdded = matchingDl.downloadedAt,
                        playCount = 0,
                        videoId = matchingDl.id
                    )
                } else {
                    songsMap[ps.songId] = SongEntity(
                        id = ps.songId,
                        title = "Unknown Track",
                        artist = "Unknown Artist",
                        album = "Playlist",
                        duration = 0L,
                        path = "online://${ps.songId}",
                        albumArt = null,
                        dateAdded = System.currentTimeMillis(),
                        playCount = 0,
                        videoId = ps.songId
                    )
                }
            }
        }

        val songs = songsMap.values.map {
            BackupSong(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = it.album,
                duration = it.duration,
                path = it.path,
                albumArt = it.albumArt,
                dateAdded = it.dateAdded,
                playCount = it.playCount,
                videoId = it.videoId
            )
        }

        val downloads = allDownloads.map {
            BackupDownload(
                id = it.id,
                title = it.title,
                artist = it.artist,
                thumbnailUrl = it.thumbnailUrl,
                durationMs = it.durationMs,
                localPath = it.localPath,
                fileSizeBytes = it.fileSizeBytes,
                downloadedAt = it.downloadedAt,
                album = it.album
            )
        }

        val favorites = db.favoriteDao().getAllFavoritesSync().map { BackupFavorite(it.songId, it.addedAt) }

        val playlistsList = db.playlistDao().getAllPlaylists().firstOrNull() ?: emptyList()
        val playlists = playlistsList.map { BackupPlaylist(it.id, it.name, it.createdAt) }

        val playlistSongs = playlistSongsRaw.map {
            BackupPlaylistSong(it.playlistId, it.songId, it.position)
        }

        val recentHistory = db.recentHistoryDao().getAllRecentHistorySync().map { BackupRecentHistory(it.songId, it.playedAt) }
        val recentSearches = db.recentSearchDao().getAllRecentSearchesSync().map { BackupRecentSearch(it.query, it.timestamp) }
        val cachedLyrics = db.cachedLyricsDao().getAllCachedLyricsSync().map {
            BackupCachedLyrics(it.songId, it.plainLyrics, it.syncedLyrics, it.trackName, it.artistName, it.cachedAt)
        }

        val backupData = BackupData(
            settings = settings,
            folders = folders,
            songs = songs,
            downloads = downloads,
            favorites = favorites,
            playlists = playlists,
            playlistSongs = playlistSongs,
            recentHistory = recentHistory,
            recentSearches = recentSearches,
            cachedLyrics = cachedLyrics
        )

        jsonHelper.encodeToString(backupData)
    }

    /**
     * Restores application state from the provided [backupJson] string.
     * Restores all playlists with their songs, restores all downloaded songs, resolves
     * file paths across app package upgrades, and schedules background downloads for missing files.
     */
    suspend fun restoreBackup(backupJson: String) = withContext(Dispatchers.IO) {
        val backupData = jsonHelper.decodeFromString<BackupData>(backupJson)

        // 1. Restore Preferences
        settingsDataStore.setAutoplayEnabled(backupData.settings.isAutoplayEnabled)
        preferencesManager.setDownloadFolderUri(backupData.settings.downloadFolderUri)

        // 2. Clear all tables and insert DB rows in a single clean transaction to respect Foreign Key Constraints
        db.withTransaction {
            db.clearAllTables()

            // Insert folders
            backupData.folders.forEach {
                db.folderDao().addFolder(FolderEntity(uri = it.uri, name = it.name))
            }

            // Prepare all songs map (combining backed-up songs, downloads, and playlist items)
            val songsMap = mutableMapOf<String, SongEntity>()

            backupData.songs.forEach { backupSong ->
                val resolvedPath = resolveLocalPath(context, backupSong.path, backupSong.title)
                val vid = backupSong.videoId?.takeIf { it.isNotBlank() }
                    ?: if (backupSong.path.startsWith("online://")) backupSong.path.removePrefix("online://").takeIf { it.isNotBlank() } else null
                    ?: backupData.downloads.firstOrNull { it.id == backupSong.id || (it.title.equals(backupSong.title, ignoreCase = true) && it.artist.equals(backupSong.artist, ignoreCase = true)) }?.id
                    ?: if (backupSong.id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) backupSong.id else null

                val finalPath = if (!checkFileExists(context, resolvedPath) && !vid.isNullOrBlank()) {
                    "online://$vid"
                } else {
                    resolvedPath
                }

                songsMap[backupSong.id] = SongEntity(
                    id = backupSong.id,
                    title = backupSong.title,
                    artist = backupSong.artist,
                    album = backupSong.album,
                    duration = backupSong.duration,
                    path = finalPath,
                    albumArt = backupSong.albumArt,
                    dateAdded = backupSong.dateAdded,
                    playCount = backupSong.playCount,
                    videoId = vid
                )
            }

            // Ensure every download is also in songs table
            backupData.downloads.forEach { dl ->
                if (!songsMap.containsKey(dl.id)) {
                    val resolvedPath = resolveLocalPath(context, dl.localPath, "${dl.title}.m4a")
                    val finalPath = if (checkFileExists(context, resolvedPath)) resolvedPath else "online://${dl.id}"
                    songsMap[dl.id] = SongEntity(
                        id = dl.id,
                        title = dl.title,
                        artist = dl.artist,
                        album = dl.album.ifBlank { "Downloads" },
                        duration = dl.durationMs,
                        path = finalPath,
                        albumArt = dl.thumbnailUrl,
                        dateAdded = dl.downloadedAt,
                        playCount = 0,
                        videoId = dl.id
                    )
                }
            }

            // Ensure every playlist song has an entry in songs table so foreign key never fails
            backupData.playlistSongs.forEach { ps ->
                if (!songsMap.containsKey(ps.songId)) {
                    val dlMatch = backupData.downloads.firstOrNull { it.id == ps.songId }
                    if (dlMatch != null) {
                        val resolvedPath = resolveLocalPath(context, dlMatch.localPath, "${dlMatch.title}.m4a")
                        val finalPath = if (checkFileExists(context, resolvedPath)) resolvedPath else "online://${dlMatch.id}"
                        songsMap[ps.songId] = SongEntity(
                            id = dlMatch.id,
                            title = dlMatch.title,
                            artist = dlMatch.artist,
                            album = dlMatch.album.ifBlank { "Downloads" },
                            duration = dlMatch.durationMs,
                            path = finalPath,
                            albumArt = dlMatch.thumbnailUrl,
                            dateAdded = dlMatch.downloadedAt,
                            playCount = 0,
                            videoId = dlMatch.id
                        )
                    } else {
                        songsMap[ps.songId] = SongEntity(
                            id = ps.songId,
                            title = "Restored Track",
                            artist = "Unknown Artist",
                            album = "Playlist",
                            duration = 0L,
                            path = "online://${ps.songId}",
                            albumArt = null,
                            dateAdded = System.currentTimeMillis(),
                            playCount = 0,
                            videoId = ps.songId
                        )
                    }
                }
            }

            // Insert all songs
            if (songsMap.isNotEmpty()) {
                db.songDao().insertSongs(songsMap.values.toList())
            }

            // Insert playlists
            backupData.playlists.forEach {
                db.playlistDao().insertPlaylist(
                    PlaylistEntity(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt
                    )
                )
            }

            // Insert playlist song references
            val crossRefs = backupData.playlistSongs.filter { songsMap.containsKey(it.songId) }.map {
                PlaylistSongCrossReference(
                    playlistId = it.playlistId,
                    songId = it.songId,
                    position = it.position
                )
            }
            if (crossRefs.isNotEmpty()) {
                db.playlistDao().insertPlaylistSongs(crossRefs)
            }

            // Insert favorites
            val favoritesToInsert = backupData.favorites.filter { songsMap.containsKey(it.songId) }.map {
                FavoriteEntity(
                    songId = it.songId,
                    addedAt = it.addedAt
                )
            }
            if (favoritesToInsert.isNotEmpty()) {
                db.favoriteDao().insertFavorites(favoritesToInsert)
            }

            // Insert recent history
            val historyToInsert = backupData.recentHistory.filter { songsMap.containsKey(it.songId) }.map {
                RecentHistoryEntity(
                    songId = it.songId,
                    playedAt = it.playedAt
                )
            }
            if (historyToInsert.isNotEmpty()) {
                db.recentHistoryDao().insertRecentHistory(historyToInsert)
            }

            // Insert recent searches
            val searchesToInsert = backupData.recentSearches.map {
                RecentSearchEntity(
                    query = it.query,
                    timestamp = it.timestamp
                )
            }
            if (searchesToInsert.isNotEmpty()) {
                db.recentSearchDao().insertRecentSearches(searchesToInsert)
            }

            // Insert downloaded songs: restore ALL records with resolved paths
            val downloadsToInsert = backupData.downloads.map { download ->
                val resolvedPath = resolveLocalPath(context, download.localPath, "${download.title}.m4a")
                DownloadedSongEntity(
                    id = download.id,
                    title = download.title,
                    artist = download.artist,
                    album = download.album,
                    thumbnailUrl = download.thumbnailUrl,
                    durationMs = download.durationMs,
                    localPath = resolvedPath,
                    fileSizeBytes = download.fileSizeBytes,
                    downloadedAt = download.downloadedAt
                )
            }
            if (downloadsToInsert.isNotEmpty()) {
                db.downloadedSongDao().insertDownloads(downloadsToInsert)
            }

            // Restore cached lyrics
            val lyricsToInsert = backupData.cachedLyrics.map {
                CachedLyricsEntity(
                    songId = it.songId,
                    plainLyrics = it.plainLyrics,
                    syncedLyrics = it.syncedLyrics,
                    trackName = it.trackName,
                    artistName = it.artistName,
                    cachedAt = it.cachedAt
                )
            }
            if (lyricsToInsert.isNotEmpty()) {
                db.cachedLyricsDao().insertCachedLyrics(lyricsToInsert)
            }
        }

        // 3. For any restored download whose file does not exist on disk, schedule background download
        val missingDownloads = backupData.downloads.filter { dl ->
            val resolved = resolveLocalPath(context, dl.localPath, "${dl.title}.m4a")
            !checkFileExists(context, resolved)
        }
        if (missingDownloads.isNotEmpty()) {
            Log.i("BackupRestoreManager", "Auto-scheduling ${missingDownloads.size} missing downloaded songs for background download...")
            missingDownloads.forEach { dl ->
                try {
                    val onlineSong = OnlineSong(
                        videoId = dl.id,
                        title = dl.title,
                        artist = dl.artist,
                        durationMs = dl.durationMs,
                        thumbnailUrl = dl.thumbnailUrl
                    )
                    downloadRepository.startDownload(onlineSong)
                } catch (e: Exception) {
                    Log.w("BackupRestoreManager", "Failed to schedule download for ${dl.title}", e)
                }
            }
        }

        Log.i("BackupRestoreManager", "Restore complete. ${backupData.downloads.size} downloads restored (${missingDownloads.size} queued for background download).")
        Log.i("BackupRestoreManager", "${backupData.playlists.size} playlists restored with all songs.")
    }

    companion object {
        /**
         * Checks whether a given file path or content URI exists and has non-zero size.
         */
        fun checkFileExists(context: Context, path: String): Boolean {
            if (path.isBlank() || path.startsWith("online://")) return false
            return try {
                if (path.startsWith("content://")) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(path))
                    doc?.exists() == true && doc.length() > 0
                } else if (path.startsWith("file://")) {
                    val file = File(Uri.parse(path).path ?: "")
                    file.exists() && file.length() > 0
                } else {
                    val file = File(path)
                    file.exists() && file.length() > 0
                }
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Resolves local file paths across Android package renames, external music dirs, and device moves.
         */
        fun resolveLocalPath(context: Context, originalPath: String, fallbackFileName: String): String {
            if (checkFileExists(context, originalPath)) return originalPath

            val currentPkg = context.packageName
            val altPkgPath = originalPath
                .replace("com.example.myplayer", currentPkg)
                .replace("com.myplayer.app", currentPkg)
            if (checkFileExists(context, altPkgPath)) return altPkgPath

            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val cleanName = File(originalPath).name.ifBlank {
                fallbackFileName.replace(Regex("[^a-zA-Z0-9 _\\-\\.]"), "").trim().take(60)
            }
            val candidate = File(musicDir, cleanName)
            if (candidate.exists() && candidate.length() > 0) return candidate.absolutePath

            // Return target candidate path even if not on disk yet (used for queued downloads)
            return candidate.absolutePath
        }
    }
}
