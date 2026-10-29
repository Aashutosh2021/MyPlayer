package com.myplayer.app.mochyfx.audio

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import com.myplayer.app.mochyfx.AudioFxService
import java.nio.ByteBuffer

/** Mochy's volume stage: the exponential volume curve and the song's ReplayGain shape every volume the player sets. */
@UnstableApi
class MochyVolumeSink(
    delegate: AudioSink,
    private val audioFx: AudioFxService,
    private val replayGain: ReplayGainTags,
) : ForwardingAudioSink(delegate) {

    private var requestedVolume = 1f
    private var appliedCurve: VolumeCurve? = null
    private var appliedReplayGain = 1f

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        // curve or ReplayGain changed: the player won't resend its unchanged volume
        if (audioFx.volumeCurve !== appliedCurve || replayGainFactor() != appliedReplayGain) applyVolume()
        return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }

    override fun setVolume(volume: Float) {
        requestedVolume = volume
        applyVolume()
    }

    private fun applyVolume() {
        val curve = audioFx.volumeCurve
        appliedCurve = curve
        appliedReplayGain = replayGainFactor()
        super.setVolume(curve.gain(requestedVolume) * appliedReplayGain)
    }

    private fun replayGainFactor() = replayGain.linear(audioFx.replayGain, audioFx.normalize)
}
