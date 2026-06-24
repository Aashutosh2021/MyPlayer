package com.example.myplayer.di

import android.content.Context
import androidx.room.Room
import com.example.myplayer.data.local.AppDatabase
import com.example.myplayer.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "myplayer_db"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
    @Provides fun provideRecentHistoryDao(db: AppDatabase): RecentHistoryDao = db.recentHistoryDao()
    @Provides fun provideDownloadedSongDao(db: AppDatabase): DownloadedSongDao = db.downloadedSongDao()
    @Provides fun provideRecentSearchDao(db: AppDatabase): RecentSearchDao = db.recentSearchDao()
}
