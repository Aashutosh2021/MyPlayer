package com.example.myplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myplayer.data.local.dao.*
import com.example.myplayer.data.local.entity.*

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossReference::class,
        FavoriteEntity::class,
        FolderEntity::class,
        RecentHistoryEntity::class,
        DownloadedSongEntity::class,
        RecentSearchEntity::class,
        CachedLyricsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun folderDao(): FolderDao
    abstract fun recentHistoryDao(): RecentHistoryDao
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun cachedLyricsDao(): CachedLyricsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS downloaded_songs (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        localPath TEXT NOT NULL,
                        fileSizeBytes INTEGER NOT NULL DEFAULT 0,
                        downloadedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recent_searches (
                        query TEXT NOT NULL PRIMARY KEY,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_lyrics (
                        songId TEXT NOT NULL PRIMARY KEY,
                        plainLyrics TEXT,
                        syncedLyrics TEXT,
                        trackName TEXT NOT NULL,
                        artistName TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Phase R7 — Data Model Normalization.
         * Non-breaking additive migrations:
         * - songs.videoId: allows cross-referencing local songs with YouTube content.
         * - downloaded_songs.album: eliminates missing album metadata for downloaded tracks.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add nullable videoId to songs table (null for pure local files)
                db.execSQL("ALTER TABLE songs ADD COLUMN videoId TEXT")
                // Add album to downloaded_songs table (empty string default for existing rows)
                db.execSQL("ALTER TABLE downloaded_songs ADD COLUMN album TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
