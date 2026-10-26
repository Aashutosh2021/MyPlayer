package com.example.myplayer.di

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Format
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import android.os.Handler
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.example.myplayer.playback.dsp.LastWaveDspAudioProcessor
import com.example.myplayer.playback.dsp.MasterAudioDspProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.math.min

/**
 * Splits large audio stream requests into 160 KB (~9-second) HTTP Range chunks
 * just like Spotify/YouTube Music, bypassing CDN throttling and saving mobile data.
 * Restored from commit 957446e for ultra-fast instant playback start.
 */
@OptIn(UnstableApi::class)
class NineSecondChunkDataSource(
    private val upstream: DataSource,
    private val chunkSizeBytes: Long = 160 * 1024L // ~9 seconds of Opus audio per chunk
) : DataSource {
    private var currentDataSpec: DataSpec? = null
    private var currentPosition: Long = 0L
    private var bytesRemainingInStream: Long = C.LENGTH_UNSET.toLong()
    private var bytesRemainingInChunk: Long = 0L
    private var isChunkedStream: Boolean = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec
        currentPosition = dataSpec.position
        bytesRemainingInStream = dataSpec.length
        val host = dataSpec.uri.host ?: ""
        // Apply 9-second HTTP Range chunking to online audio streams
        isChunkedStream = (dataSpec.uri.scheme == "http" || dataSpec.uri.scheme == "https") &&
                (host.contains("googlevideo.com") || dataSpec.length == C.LENGTH_UNSET.toLong() || dataSpec.length > chunkSizeBytes)

        if (!isChunkedStream) {
            return upstream.open(dataSpec)
        }

        openNextNineSecondChunk()
        return dataSpec.length
    }

    private fun openNextNineSecondChunk(): Boolean {
        val baseSpec = currentDataSpec ?: return false
        val targetLength = if (bytesRemainingInStream != C.LENGTH_UNSET.toLong()) {
            min(chunkSizeBytes, bytesRemainingInStream)
        } else {
            chunkSizeBytes
        }
        if (targetLength <= 0L) return false

        val chunkSpec = baseSpec.buildUpon()
            .setPosition(currentPosition)
            .setLength(targetLength)
            .build()

        val opened = upstream.open(chunkSpec)
        bytesRemainingInChunk = if (opened != C.LENGTH_UNSET.toLong()) opened else targetLength
        return true
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!isChunkedStream) {
            return upstream.read(buffer, offset, length)
        }
        if (length == 0) return 0

        if (bytesRemainingInChunk == 0L) {
            upstream.close()
            if (bytesRemainingInStream != C.LENGTH_UNSET.toLong() && bytesRemainingInStream <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
            val opened = try {
                openNextNineSecondChunk()
            } catch (_: Exception) {
                false
            }
            if (!opened || bytesRemainingInChunk <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
        }

        val toRead = min(length.toLong(), bytesRemainingInChunk).toInt()
        val bytesRead = upstream.read(buffer, offset, toRead)
        if (bytesRead == C.RESULT_END_OF_INPUT) {
            return C.RESULT_END_OF_INPUT
        }

        currentPosition += bytesRead
        bytesRemainingInChunk -= bytesRead
        if (bytesRemainingInStream != C.LENGTH_UNSET.toLong()) {
            bytesRemainingInStream -= bytesRead
        }
        return bytesRead
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        currentDataSpec = null
        bytesRemainingInChunk = 0L
        isChunkedStream = false
        upstream.close()
    }

    class Factory(private val upstreamFactory: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return NineSecondChunkDataSource(upstreamFactory.createDataSource())
        }
    }
}

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        simpleCache: SimpleCache
    ): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(10_000)
            .setUserAgent("com.google.android.youtube/19.29.37 (Linux; U; Android 14) gzip")

        // Wrap HTTP DataSource in 9-Second Chunked Range DataSource (restored from 957446e)
        val chunkedHttpFactory = NineSecondChunkDataSource.Factory(httpDataSourceFactory)
        val upstreamFactory = DefaultDataSource.Factory(context, chunkedHttpFactory)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        cacheDataSourceFactory: CacheDataSource.Factory,
        lastWaveDspProcessor: LastWaveDspAudioProcessor,
        masterAudioDspProcessor: MasterAudioDspProcessor
    ): ExoPlayer {
        val prefs = context.getSharedPreferences("playback_preferences", Context.MODE_PRIVATE)
        val initialRepeat = prefs.getInt("playback_repeat_mode", Player.REPEAT_MODE_OFF)
        val initialShuffle = prefs.getBoolean("playback_shuffle_mode", false)

        // Spotify-style 9-Second Rolling Buffer LoadControl (from 957446e):
        // - Starts playing after just 0.5s of audio arrives!
        // - Buffers strictly 9 seconds ahead, pauses download, and fetches next 9s as you listen!
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 9_000,                     // Maintain 9s rolling buffer
                /* maxBufferMs = */ 9_000,                     // Never pre-download more than 9s ahead!
                /* bufferForPlaybackMs = */ 500,               // Instant start after 0.5s (~8 KB)!
                /* bufferForPlaybackAfterRebufferMs = */ 1_000 // Fast 1.0s recovery on weak network
            )
            .setTargetBufferBytes(256 * 1024)                 // Cap in-RAM buffer to 256 KB
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(9_000, false)                      // Keep last 9s in memory for instant rewind
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(/* minLoadableRetryCount = */ 5))

        // Deprioritize buggy vendor decoders (Samsung One UI / Exynos hardware decoders)
        // across audio MIME types to avoid misreported sample rates or digital boundary distortion.
        val accurateAudioMediaCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val decoderInfos = runCatching {
                MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder,
                )
            }.getOrDefault(emptyList())
            if (decoderInfos.isEmpty()) {
                emptyList()
            } else {
                decoderInfos.sortedBy { info ->
                    val lower = info.name.lowercase()
                    if (lower.contains("sec.") || lower.contains("exynos")) 1 else 0
                }
            }
        }

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: AudioSink,
                eventHandler: Handler,
                eventListener: AudioRendererEventListener,
                out: ArrayList<Renderer>,
            ) {
                // Hardware/platform Dolby Atmos & Spatial audio MediaCodec renderer:
                // Placed ahead of FFmpeg so Android's native Dolby Atmos HAL and Spatializer
                // decode E-AC-3 JOC with hardware dialogue normalization, full volume,
                // and system spatial virtualization. Standard codecs (FLAC, Opus, AAC, MP3, Vorbis)
                // return FORMAT_UNSUPPORTED_TYPE here so FFmpeg retains priority for them.
                val dolbySpatialRenderer = object : MediaCodecAudioRenderer(
                    context,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    audioSink,
                ) {
                    override fun supportsFormat(
                        mediaCodecSelector: MediaCodecSelector,
                        format: androidx.media3.common.Format,
                    ): Int {
                        val mime = format.sampleMimeType?.lowercase().orEmpty()
                        val isDolbyOrSpatial = mime.contains("eac3") || mime.contains("ec-3") ||
                            mime.contains("ac-3") || mime.contains("ac3") || mime.contains("ac4") ||
                            mime.contains("mha1") || mime.contains("mhm1")
                        if (!isDolbyOrSpatial) {
                            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE)
                        }
                        return super.supportsFormat(mediaCodecSelector, format)
                    }
                }
                out.add(dolbySpatialRenderer)

                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    audioSink,
                    eventHandler,
                    eventListener,
                    out,
                )
            }

            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true)
                    .setEnableAudioTrackPlaybackParams(false)
                    .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                    .setAudioProcessors(arrayOf(masterAudioDspProcessor))
                    .build()
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            setEnableAudioTrackPlaybackParams(false)
            setMediaCodecSelector(accurateAudioMediaCodecSelector)
        }

        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                repeatMode = initialRepeat
                shuffleModeEnabled = initialShuffle
                addAnalyticsListener(object : AnalyticsListener {
                    override fun onAudioInputFormatChanged(
                        eventTime: AnalyticsListener.EventTime,
                        format: Format,
                    ) {
                        masterAudioDspProcessor.setTrackFormat(format)
                    }
                })
            }
    }
}
