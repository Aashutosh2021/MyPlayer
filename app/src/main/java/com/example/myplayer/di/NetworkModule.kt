package com.example.myplayer.di

import com.example.myplayer.BuildConfig
import com.example.myplayer.security.NetworkSecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * NetworkModule — Phase 13/14 Network Hardening
 *
 * Changes from baseline:
 *  1. HttpLoggingInterceptor is ONLY added in debug builds.
 *     In release builds, logging is completely disabled to prevent
 *     request/response data leakage via Logcat.
 *  2. Certificate pinning (via NetworkSecurityManager) is applied in release.
 *  3. Timeouts are more conservative (30s — unchanged, appropriate).
 *
 * THREAT MITIGATED:
 *  - Network traffic inspection via Logcat in production
 *  - MITM attacks against YouTube Music API (via certificate pinning)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkSecurityManager(): NetworkSecurityManager {
        return NetworkSecurityManager()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        networkSecurityManager: NetworkSecurityManager
    ): OkHttpClient {
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 20
        }
        val connectionPool = okhttp3.ConnectionPool(16, 5, TimeUnit.MINUTES)

        val builder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        // Phase 13: Only log network traffic in debug builds.
        // In release, logging is COMPLETELY disabled.
        // WHY: HttpLoggingInterceptor.Level.BODY prints full request/response bodies
        //      to Logcat — any app with READ_LOGS permission can intercept this.
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        val client = builder.build()

        // Phase 13: Apply certificate pinning in release builds.
        // In debug builds, pinning is disabled to allow proxy tools (Burp, Charles).
        // The network_security_config.xml provides OS-level cleartext blocking in all builds.
        return networkSecurityManager.applyPinning(client, enabled = !BuildConfig.DEBUG)
    }
}
