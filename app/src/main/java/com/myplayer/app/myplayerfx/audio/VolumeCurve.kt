package com.myplayer.app.myplayerfx.audio

import kotlin.math.pow

/** how myplayer's own volume maps onto gain: linear crowds everything audible into the top of the slider, a power curve spreads the quiet end out; limit caps the loudest the app plays */
data class VolumeCurve(
    val enabled: Boolean = false,
    val exponent: Float = 2f,
    val limit: Float = 1f,
) {
    fun gain(volume: Float): Float {
        val v = volume.coerceIn(0f, 1f)
        if (!enabled) return v
        return limit.coerceIn(0.05f, 1f) * v.pow(exponent.coerceIn(1f, 5f))
    }
}
