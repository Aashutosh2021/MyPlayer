package com.myplayer.app.aria.providers

import com.myplayer.app.aria.model.AriaGeneralResponse
import com.myplayer.app.aria.model.AriaStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerProvider @Inject constructor() {
    fun applyPreset(presetName: String, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        return AriaGeneralResponse(AriaStatus.NOT_AVAILABLE, commandOrdinal, transactionId, "Equalizer DSP is currently stubbed/unavailable.")
    }

    fun applyCustomBands(bands: FloatArray, commandOrdinal: Int, transactionId: String?): AriaGeneralResponse {
        return AriaGeneralResponse(AriaStatus.NOT_AVAILABLE, commandOrdinal, transactionId, "Equalizer DSP is currently stubbed/unavailable.")
    }
}
