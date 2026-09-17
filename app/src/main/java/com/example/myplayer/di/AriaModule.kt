package com.example.myplayer.di

import android.content.Context
import com.example.myplayer.aria.cache.AriaMemoryCache
import com.example.myplayer.aria.dispatcher.CommandDispatcher
import com.example.myplayer.aria.event.AriaEventPublisher
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.aria.providers.*
import com.example.myplayer.aria.recovery.AriaStateRecoveryManager
import com.example.myplayer.aria.security.CallerVerifier
import com.example.myplayer.data.local.dao.DownloadedSongDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.lyrics.LyricsRepository
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.playback.MusicController
import com.example.myplayer.playback.PlaybackEventBus
import com.example.myplayer.security.SignatureVerifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AriaModule {

    @Provides
    @Singleton
    fun provideCallerVerifier(
        @ApplicationContext context: Context,
        signatureVerifier: SignatureVerifier
    ): CallerVerifier {
        return CallerVerifier(context, signatureVerifier)
    }

    @Provides
    @Singleton
    fun provideAriaEventPublisher(): AriaEventPublisher {
        return AriaEventPublisher()
    }

    @Provides
    @Singleton
    fun provideAriaPerformanceMonitor(): AriaPerformanceMonitor {
        return AriaPerformanceMonitor()
    }

    @Provides
    @Singleton
    fun provideAriaMemoryCache(
        songDao: SongDao,
        downloadedSongDao: DownloadedSongDao
    ): AriaMemoryCache {
        return AriaMemoryCache(songDao, downloadedSongDao)
    }

    @Provides
    @Singleton
    fun provideAriaStateRecoveryManager(
        @ApplicationContext context: Context,
        memoryCache: AriaMemoryCache
    ): AriaStateRecoveryManager {
        return AriaStateRecoveryManager(context, memoryCache)
    }

    @Provides
    @Singleton
    fun providePlaybackController(
        @ApplicationContext context: Context,
        musicController: MusicController,
        memoryCache: AriaMemoryCache,
        performanceMonitor: AriaPerformanceMonitor
    ): PlaybackController {
        return PlaybackController(context, musicController, memoryCache, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideSearchProvider(
        memoryCache: AriaMemoryCache,
        innertubeApi: InnertubeApi,
        performanceMonitor: AriaPerformanceMonitor
    ): SearchProvider {
        return SearchProvider(memoryCache, innertubeApi, performanceMonitor)
    }

    @Provides
    @Singleton
    fun providePlaylistProvider(
        musicRepository: MusicRepository,
        musicController: MusicController,
        performanceMonitor: AriaPerformanceMonitor
    ): PlaylistProvider {
        return PlaylistProvider(musicRepository, musicController, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideQueueProvider(
        musicController: MusicController,
        memoryCache: AriaMemoryCache,
        performanceMonitor: AriaPerformanceMonitor
    ): QueueProvider {
        return QueueProvider(musicController, memoryCache, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideDownloadProvider(
        downloadRepository: DownloadRepository,
        innertubeApi: InnertubeApi,
        performanceMonitor: AriaPerformanceMonitor
    ): DownloadProvider {
        return DownloadProvider(downloadRepository, innertubeApi, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideMetadataProvider(
        musicController: MusicController,
        performanceMonitor: AriaPerformanceMonitor
    ): MetadataProvider {
        return MetadataProvider(musicController, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideHistoryProvider(
        musicRepository: MusicRepository,
        performanceMonitor: AriaPerformanceMonitor
    ): HistoryProvider {
        return HistoryProvider(musicRepository, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideRecommendationProvider(
        musicRepository: MusicRepository,
        performanceMonitor: AriaPerformanceMonitor
    ): RecommendationProvider {
        return RecommendationProvider(musicRepository, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideEqualizerProvider(): EqualizerProvider {
        return EqualizerProvider()
    }

    @Provides
    @Singleton
    fun provideLyricsProvider(
        musicController: MusicController,
        lyricsRepository: LyricsRepository,
        performanceMonitor: AriaPerformanceMonitor
    ): LyricsProvider {
        return LyricsProvider(musicController, lyricsRepository, performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideCommandDispatcher(
        @ApplicationContext context: Context,
        callerVerifier: CallerVerifier,
        eventPublisher: AriaEventPublisher,
        performanceMonitor: AriaPerformanceMonitor,
        musicController: MusicController,
        playbackController: PlaybackController,
        searchProvider: SearchProvider,
        playlistProvider: PlaylistProvider,
        queueProvider: QueueProvider,
        downloadProvider: DownloadProvider,
        metadataProvider: MetadataProvider,
        historyProvider: HistoryProvider,
        recommendationProvider: RecommendationProvider,
        equalizerProvider: EqualizerProvider,
        lyricsProvider: LyricsProvider
    ): CommandDispatcher {
        return CommandDispatcher(
            context,
            callerVerifier,
            eventPublisher,
            performanceMonitor,
            musicController,
            playbackController,
            searchProvider,
            playlistProvider,
            queueProvider,
            downloadProvider,
            metadataProvider,
            historyProvider,
            recommendationProvider,
            equalizerProvider,
            lyricsProvider
        )
    }
}
