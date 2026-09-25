package com.example.myplayer.sync.di

import com.example.myplayer.sync.discovery.NsdSyncDiscovery
import com.example.myplayer.sync.discovery.SyncDiscovery
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncDiscovery(
        impl: NsdSyncDiscovery
    ): SyncDiscovery
}
