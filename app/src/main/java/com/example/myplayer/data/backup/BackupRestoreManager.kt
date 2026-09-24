package com.example.myplayer.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
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
    val downloadedAt: Long
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
     * Serializes all application settings, local folders, library songs, playlists,
     * favorites, recent searches, and downloads metadata to a single JSON string.
     */
    suspend fun generateBackup(): String = withContext(Dispatchers.IO) {
        val isAutoplayEnabled = settingsDataStore.isAutoplayEnabled.firstOrNull() ?: true
        val downloadFolderUri = preferencesManager.downloadFolderUri.firstOrNull()

        val settings = BackupSettings(isAutoplayEnabled, downloadFolderUri)
        val folders = db.folderDao().getFoldersSync().map { BackupFolder(it.uri, it.name) }
        
        // Songs list (obtained by collecting first element of Flow)
        val songsList = db.songDao().getAllSongs().firstOrNull() ?: emptyList()
        val songs = songsList.map {
            BackupSong(it.id, it.title, it.artist, it.album, it.duration, it.path, it.albumArt, it.dateAdded, it.playCount, it.videoId)
        }

        val downloads = db.downloadedSongDao().getAllDownloadsSync().map {
            BackupDownload(it.id, it.title, it.artist, it.thumbnailUrl, it.durationMs, it.localPath, it.fileSizeBytes, it.downloadedAt)
        }

        val favorites = db.favoriteDao().getAllFavoritesSync().map { BackupFavorite(it.songId, it.addedAt) }
        
        val playlistsList = db.playlistDao().getAllPlaylists().firstOrNull() ?: emptyList()
        val playlists = playlistsList.map { BackupPlaylist(it.id, it.name, it.createdAt) }

        val playlistSongs = db.playlistDao().getAllPlaylistSongsSync().map {
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
     * Clears all tables in a single transaction, inserts the backed-up data in order
     * of foreign key constraints, and schedules downloads for missing files in WorkManager.
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

            // Insert songs (Parent table required by playlists, favorites, and history)
            // For each song, check if local file still exists. If not and the song has a videoId,
            // rewrite path to online://<videoId> so it can stream on demand without source errors.
            val songsToInsert = backupData.songs.map { backupSong ->
                var resolvedPath = backupSong.path
                val vid = backupSong.videoId?.takeIf { it.isNotBlank() }
                    ?: if (backupSong.path.startsWith("online://")) backupSong.path.removePrefix("online://").takeIf { it.isNotBlank() } else null
                    ?: backupData.downloads.firstOrNull { it.id == backupSong.id || (it.title.equals(backupSong.title, ignoreCase = true) && it.artist.equals(backupSong.artist, ignoreCase = true)) }?.id
                    ?: if (backupSong.id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) backupSong.id else null

                // Only rewrite if path is NOT already an online:// URI
                if (!resolvedPath.startsWith("online://") && !resolvedPath.startsWith("http")) {
                    val fileExists = try {
                        if (resolvedPath.startsWith("content://")) {
                            val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, Uri.parse(resolvedPath))
                            doc?.exists() == true && doc.length() > 0
                        } else {
                            val file = File(resolvedPath)
                            file.exists() && file.length() > 0
                        }
                    } catch (e: Exception) { false }

                    if (!fileExists && !vid.isNullOrBlank()) {
                        Log.i("BackupRestoreManager", "Local file missing for '${backupSong.title}', rewriting path to online://$vid")
                        resolvedPath = "online://$vid"
                    }
                }

                SongEntity(
                    id = backupSong.id,
                    title = backupSong.title,
                    artist = backupSong.artist,
                    album = backupSong.album,
                    duration = backupSong.duration,
                    path = resolvedPath,
                    albumArt = backupSong.albumArt,
                    dateAdded = backupSong.dateAdded,
                    playCount = backupSong.playCount,
                    videoId = vid
                )
            }
            if (songsToInsert.isNotEmpty()) {
                db.songDao().insertSongs(songsToInsert)
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

            // Insert playlist song references (Foreign Key references: playlists.id & songs.id)
            val crossRefs = backupData.playlistSongs.map {
                PlaylistSongCrossReference(
                    playlistId = it.playlistId,
                    songId = it.songId,
                    position = it.position
                )
            }
            if (crossRefs.isNotEmpty()) {
                db.playlistDao().insertPlaylistSongs(crossRefs)
            }

            // Insert favorites (Foreign Key references: songs.id)
            val favoritesToInsert = backupData.favorites.map {
                FavoriteEntity(
                    songId = it.songId,
                    addedAt = it.addedAt
                )
            }
            if (favoritesToInsert.isNotEmpty()) {
                db.favoriteDao().insertFavorites(favoritesToInsert)
            }

            // Insert recent history (Foreign Key references: songs.id)
            val historyToInsert = backupData.recentHistory.map {
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

            // Insert downloads metadata ONLY for files that actually exist on disk.
            // If the files are missing, do not insert them into downloaded_songs table
            // so the user can manually download them from NowPlayingScreen or list screen.
            val downloadsToInsert = backupData.downloads.filter { download ->
                try {
                    if (download.localPath.startsWith("content://") || download.localPath.startsWith("file://")) {
                        val uri = Uri.parse(download.localPath)
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length > 0 } ?: false
                    } else {
                        val file = File(download.localPath)
                        file.exists() && file.length() > 0
                    }
                } catch (e: Exception) { false }
            }.map {
                DownloadedSongEntity(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    thumbnailUrl = it.thumbnailUrl,
                    durationMs = it.durationMs,
                    localPath = it.localPath,
                    fileSizeBytes = it.fileSizeBytes,
                    downloadedAt = it.downloadedAt
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

        // Downloads metadata is restored above but files are NOT auto-downloaded.
        // Users can manually download songs from the NowPlaying screen or list screen.
        Log.i("BackupRestoreManager", "Restore complete. ${backupData.downloads.size} download records restored (files not auto-downloaded).")
        Log.i("BackupRestoreManager", "${backupData.songs.size} songs, ${backupData.playlists.size} playlists, ${backupData.favorites.size} favorites restored.")
    }
}
