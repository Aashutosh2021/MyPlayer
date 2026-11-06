package com.myplayer.app.di

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
import com.myplayer.app.playback.dsp.LastWaveDspAudioProcessor
import com.myplayer.app.playback.dsp.MasterAudioDspProcessor
import com.myplayer.app.playback.dsp.SpatialAudioProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.math.min

/**
 * Spotify-style 9-Second Rolling Chunk DataSource:
 * 1. Requests audio in 160 KB (~9 seconds of Opus/AAC audio) HTTP Range chunks.
 * 2. Parses total stream length from HTTP `Content-Range: bytes start-end/total` so
 *    ExoPlayer knows the exact file boundary even when `dataSpec.length == LENGTH_UNSET`.
 * 3. Retries transient chunk failures up to 4 times with backoff on slow 2G/3G networks.
 * 4. NEVER returns false EOF (`RESULT_END_OF_INPUT`) on network drops — throws `IOException`
 *    so ExoPlayer cleanly pauses to rebuffer and resumes from the exact byte!
 */
@OptIn(UnstableApi::class)
class NineSecondChunkDataSource(
    private val upstream: DataSource,
    private val chunkSizeBytes: Long = 160 * 1024L // ~9 seconds of audio per chunk
) : DataSource {
    private var currentDataSpec: DataSpec? = null
    private var currentPosition: Long = 0L
    private var totalStreamEndPosition: Long = C.LENGTH_UNSET.toLong()
    private var bytesRemainingInChunk: Long = 0L
    private var isChunkedStream: Boolean = false
    private var isChunkOpen: Boolean = false

    companion object {
        private const val TAG = "NineSecChunkDS"
        private const val MAX_CHUNK_RETRIES = 4
        private val CONTENT_RANGE_REGEX = Regex("""bytes\s+\d+-\d+/(\d+)""", RegexOption.IGNORE_CASE)
    }

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec
        currentPosition = dataSpec.position
        totalStreamEndPosition = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.position + dataSpec.length
        } else {
            C.LENGTH_UNSET.toLong()
        }

        val scheme = dataSpec.uri.scheme ?: ""
        val host = dataSpec.uri.host ?: ""
        isChunkedStream = (scheme == "http" || scheme == "https") &&
            (host.contains("googlevideo.com") ||
                dataSpec.length == C.LENGTH_UNSET.toLong() ||
                dataSpec.length > chunkSizeBytes)

        if (!isChunkedStream) {
            isChunkOpen = true
            return upstream.open(dataSpec)
        }

        openNextNineSecondChunkWithRetry(isInitialOpen = true)

        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (totalStreamEndPosition != C.LENGTH_UNSET.toLong()) {
            (totalStreamEndPosition - dataSpec.position).coerceAtLeast(0L)
        } else {
            C.LENGTH_UNSET.toLong()
        }
    }

    private fun openNextNineSecondChunkWithRetry(isInitialOpen: Boolean): Boolean {
        val baseSpec = currentDataSpec ?: return false

        if (totalStreamEndPosition != C.LENGTH_UNSET.toLong() && currentPosition >= totalStreamEndPosition) {
            return false // True end of audio stream reached
        }

        val remainingInStream = if (totalStreamEndPosition != C.LENGTH_UNSET.toLong()) {
            totalStreamEndPosition - currentPosition
        } else {
            C.LENGTH_UNSET.toLong()
        }

        val targetChunkLength = if (remainingInStream != C.LENGTH_UNSET.toLong()) {
            min(chunkSizeBytes, remainingInStream)
        } else {
            chunkSizeBytes
        }

        if (targetChunkLength <= 0L) return false

        var lastException: Exception? = null

        for (attempt in 1..MAX_CHUNK_RETRIES) {
            try {
                if (isChunkOpen) {
                    try { upstream.close() } catch (_: Exception) {}
                    isChunkOpen = false
                }

                val chunkSpec = baseSpec.buildUpon()
                    .setPosition(currentPosition)
                    .setLength(targetChunkLength)
                    .build()

                val openedBytes = upstream.open(chunkSpec)
                isChunkOpen = true

                // Extract total stream size from Content-Range header (e.g., "bytes 0-163839/3845120")
                if (totalStreamEndPosition == C.LENGTH_UNSET.toLong()) {
                    val headers = upstream.responseHeaders
                    val contentRange = headers["Content-Range"]?.firstOrNull()
                        ?: headers["content-range"]?.firstOrNull()
                    if (!contentRange.isNullOrBlank()) {
                        val totalSize = CONTENT_RANGE_REGEX.find(contentRange)
                            ?.groupValues?.getOrNull(1)?.toLongOrNull()
                        if (totalSize != null && totalSize > 0L) {
                            totalStreamEndPosition = if (baseSpec.length != C.LENGTH_UNSET.toLong()) {
                                min(totalSize, baseSpec.position + baseSpec.length)
                            } else {
                                totalSize
                            }
                        }
                    }
                }

                val actualRemaining = if (totalStreamEndPosition != C.LENGTH_UNSET.toLong()) {
                    (totalStreamEndPosition - currentPosition).coerceAtLeast(0L)
                } else {
                    targetChunkLength
                }

                bytesRemainingInChunk = when {
                    openedBytes != C.LENGTH_UNSET.toLong() -> min(openedBytes, actualRemaining)
                    else -> min(targetChunkLength, actualRemaining)
                }

                return bytesRemainingInChunk > 0L
            } catch (e: androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                // HTTP 416 Range Not Satisfiable means we reached exact EOF
                if (e.responseCode == 416) {
                    totalStreamEndPosition = currentPosition
                    bytesRemainingInChunk = 0L
                    return false
                }
                lastException = e
                android.util.Log.w(TAG, "Chunk HTTP ${e.responseCode} at pos=$currentPosition (attempt $attempt/$MAX_CHUNK_RETRIES)")
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w(TAG, "Slow network chunk retry $attempt/$MAX_CHUNK_RETRIES at pos=$currentPosition: ${e.message}")
            }

            if (attempt < MAX_CHUNK_RETRIES) {
                try {
                    Thread.sleep(350L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        // Throw IOException instead of returning false so ExoPlayer triggers rebuffering/retry
        // rather than falsely ending the song at 9 seconds!
        throw java.io.IOException(
            "Unable to load 9s audio chunk at byte $currentPosition (initial=$isInitialOpen)",
            lastException
        )
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!isChunkedStream) {
            return upstream.read(buffer, offset, length)
        }
        if (length == 0) return 0

        if (bytesRemainingInChunk <= 0L) {
            if (isChunkOpen) {
                try { upstream.close() } catch (_: Exception) {}
                isChunkOpen = false
            }
            if (totalStreamEndPosition != C.LENGTH_UNSET.toLong() && currentPosition >= totalStreamEndPosition) {
                return C.RESULT_END_OF_INPUT
            }
            val opened = openNextNineSecondChunkWithRetry(isInitialOpen = false)
            if (!opened || bytesRemainingInChunk <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
        }

        val bytesToRead = min(length.toLong(), bytesRemainingInChunk).toInt()
        val bytesRead = try {
            upstream.read(buffer, offset, bytesToRead)
        } catch (e: java.io.IOException) {
            // Mid-chunk socket drop on slow network: close current chunk and retry seamlessly
            android.util.Log.w(TAG, "Mid-chunk read interrupted at $currentPosition, re-opening chunk: ${e.message}")
            if (isChunkOpen) {
                try { upstream.close() } catch (_: Exception) {}
                isChunkOpen = false
            }
            val reopened = openNextNineSecondChunkWithRetry(isInitialOpen = false)
            if (!reopened || bytesRemainingInChunk <= 0L) return C.RESULT_END_OF_INPUT
            upstream.read(buffer, offset, min(length.toLong(), bytesRemainingInChunk).toInt())
        }

        if (bytesRead == C.RESULT_END_OF_INPUT) {
            if (isChunkOpen) {
                try { upstream.close() } catch (_: Exception) {}
                isChunkOpen = false
            }
            bytesRemainingInChunk = 0L
            if (totalStreamEndPosition != C.LENGTH_UNSET.toLong() && currentPosition < totalStreamEndPosition) {
                val opened = openNextNineSecondChunkWithRetry(isInitialOpen = false)
                if (opened && bytesRemainingInChunk > 0L) {
                    return read(buffer, offset, length)
                }
            }
            return C.RESULT_END_OF_INPUT
        }

        currentPosition += bytesRead
        bytesRemainingInChunk -= bytesRead
        return bytesRead
    }

    override fun getUri(): Uri? = upstream.uri ?: currentDataSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        currentDataSpec = null
        bytesRemainingInChunk = 0L
        isChunkedStream = false
        if (isChunkOpen) {
            isChunkOpen = false
            try { upstream.close() } catch (_: Exception) {}
        }
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
            .setConnectTimeoutMs(30_000) // 30s timeout for slow 2G/3G networks
            .setReadTimeoutMs(30_000)
            .setKeepPostFor302Redirects(true)
            .setUserAgent("com.google.android.youtube/19.29.37 (Linux; U; Android 14) gzip")

        // Wrap HTTP DataSource in 9-Second Chunked Range DataSource
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
        masterAudioDspProcessor: MasterAudioDspProcessor,
        spatialAudioProcessor: SpatialAudioProcessor,
        audioFx: com.myplayer.app.myplayerfx.AudioFxService
    ): ExoPlayer {
        // MyPlayer Audio Lab chain (needs 16-bit PCM, so float output is off): spatial/surround/mono, reverb, loudness
        val replayGain = com.myplayer.app.myplayerfx.audio.ReplayGainTags()
        com.myplayer.app.ui.myplayer.MyPlayerAudioFxDspBridge.attach(audioFx)
        val myplayerFx = arrayOf<androidx.media3.common.audio.AudioProcessor>(
            com.myplayer.app.myplayerfx.audio.SpatialAudioProcessor(audioFx),
            com.myplayer.app.myplayerfx.audio.ReverbAudioProcessor(audioFx),
            com.myplayer.app.myplayerfx.audio.LoudnessAudioProcessor(audioFx),
        )
        val prefs = context.getSharedPreferences("playback_preferences", Context.MODE_PRIVATE)
        val initialRepeat = prefs.getInt("playback_repeat_mode", Player.REPEAT_MODE_OFF)
        val initialShuffle = prefs.getBoolean("playback_shuffle_mode", false)

        // Spotify-Style 9-Second Progressive Chunk LoadControl:
        // - Starts playback after just 500ms of audio arrives (~8 KB)
        // - Loads 9s chunks progressively up to a 27s (3-chunk) rolling window
        // - Prevents minBuffer == maxBuffer thrashing on slow connections
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 9_000,                      // Trigger next 9s chunk fetch when buffer < 9s
                /* maxBufferMs = */ 27_000,                     // Buffer up to 3x 9-second chunks ahead
                /* bufferForPlaybackMs = */ 500,                // Ultra-fast start after 0.5s
                /* bufferForPlaybackAfterRebufferMs = */ 1_500  // Smooth 1.5s resume after weak network stall
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(18_000, false)                       // Keep last 18s (2 chunks) in RAM for instant seek-back
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(/* minLoadableRetryCount = */ 6))

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
                val sink = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(false)
                    .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                    .setAudioProcessors(arrayOf(spatialAudioProcessor, masterAudioDspProcessor, *myplayerFx))
                    .build()
                return com.myplayer.app.myplayerfx.audio.MyPlayerVolumeSink(sink, audioFx, replayGain)
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
                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        replayGain.update(tracks)
                    }
                })
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
