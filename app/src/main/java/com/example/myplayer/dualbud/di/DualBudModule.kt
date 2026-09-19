package com.example.myplayer.dualbud.di

import com.example.myplayer.dualbud.audio.AndroidAudioTrackOutput
import com.example.myplayer.dualbud.audio.AudioOutputDevice
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
