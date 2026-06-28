package com.example.myplayer.data.recommendation.logging

import android.util.Log
import com.example.myplayer.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationLogger @Inject constructor() {
    private val TAG = "RecommendationEngine"

    fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[RECOMMENDATION] $message")
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "[RECOMMENDATION] $message", throwable)
        }
    }
}
