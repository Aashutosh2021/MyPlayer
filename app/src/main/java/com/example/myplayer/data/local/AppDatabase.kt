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
        RecentSearchEntity::class
    ],
    version = 2,
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
    }
}
