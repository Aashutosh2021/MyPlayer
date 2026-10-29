package com.myplayer.app.dualbud.di

import com.myplayer.app.dualbud.audio.AndroidAudioTrackOutput
import com.myplayer.app.dualbud.audio.AudioOutputDevice
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DualBudModule {

    @Provides
    @Singleton
    fun provideAudioOutputDevice(): AudioOutputDevice {
        return AndroidAudioTrackOutput()
    }
}
