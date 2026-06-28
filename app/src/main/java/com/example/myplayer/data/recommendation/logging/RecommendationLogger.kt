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

    fun logEvent(eventName: String, params: Map<String, Any>) {
        if (BuildConfig.DEBUG) {
            val formattedParams = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
            Log.d(TAG, "[RECOMMENDATION] [EVENT: $eventName] $formattedParams")
        }
    }
}
