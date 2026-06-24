package com.example.myplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import javax.inject.Inject

import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.myplayer.util.AudioAlbumArtFetcher

@HiltAndroidApp
class MyPlayerApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // NewPipe is initialized inside InnertubeApi (injected singleton),
        // ensuring it receives the correct Hilt-provided OkHttpClient.
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AudioAlbumArtFetcher.Factory(this@MyPlayerApplication))
            }
            .build()
    }
}
