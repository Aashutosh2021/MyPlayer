package com.example.myplayer.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    private const val MAX_CACHE_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB

    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "audio_streams")
        return SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
        )
    }
}
