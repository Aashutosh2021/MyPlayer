#include <jni.h>
#include <cmath>
#include <algorithm>
#include <cstdint>
#include <vector>
#include <atomic>

#include "DspProcessor.h"
#include "PcmConverter.h"

namespace {

static lastwave::audio::DspProcessor gDsp;
static std::atomic<float> gLoudnessGainLinear{1.0f};

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_myplayer_app_playback_dsp_MasterAudioDspProcessor_nativeConfigure(
        JNIEnv *env,
        jobject /*thiz*/,
        jint sampleRate,
        jboolean bitPerfect,
        jboolean eqEnabled,
        jfloatArray eqGains,
        jboolean studioClarity,
        jint clarityPresetOrd,
        jfloat loudnessGainDb) {

    const double sr = sampleRate > 0 ? static_cast<double>(sampleRate) : 48000.0;
    gDsp.configure(sr);
    gDsp.setBitPerfect(bitPerfect == JNI_TRUE);

    if (eqGains != nullptr && env->GetArrayLength(eqGains) == static_cast<jsize>(lastwave::audio::DspProcessor::kEqualizerBandCount)) {
        jfloat *gains = env->GetFloatArrayElements(eqGains, nullptr);
        if (gains != nullptr) {
            gDsp.setEqualizer(eqEnabled == JNI_TRUE, gains, lastwave::audio::DspProcessor::kEqualizerBandCount);
            env->ReleaseFloatArrayElements(eqGains, gains, JNI_ABORT);
        }
    } else {
        gDsp.setEqualizer(eqEnabled == JNI_TRUE, nullptr, 0);
    }

    gDsp.setStudioMasterClarity(studioClarity == JNI_TRUE);
    gDsp.setClarityPreset(clarityPresetOrd);
    gDsp.setPeakProtectionEnabled(true);

    if (std::abs(loudnessGainDb) > 0.001f) {
        gLoudnessGainLinear.store(std::pow(10.0f, loudnessGainDb / 20.0f), std::memory_order_relaxed);
    } else {
        gLoudnessGainLinear.store(1.0f, std::memory_order_relaxed);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_myplayer_app_playback_dsp_MasterAudioDspProcessor_nativeProcessPcmFloat(
        JNIEnv *env,
        jobject /*thiz*/,
        jobject directBuffer,
        jint byteOffset,
        jint frameCount,
        jint channelCount) {

    if (directBuffer == nullptr || frameCount <= 0 || (channelCount != 1 && channelCount != 2)) {
        return;
    }

    auto *base = static_cast<uint8_t *>(env->GetDirectBufferAddress(directBuffer));
    if (base == nullptr) return;

    auto *samples = reinterpret_cast<float *>(base + byteOffset);

    // Apply loudness gain if non-unity
    const float loudness = gLoudnessGainLinear.load(std::memory_order_relaxed);
    if (std::abs(loudness - 1.0f) > 0.0001f) {
        const size_t totalSamples = static_cast<size_t>(frameCount) * static_cast<size_t>(channelCount);
        for (size_t i = 0; i < totalSamples; ++i) {
            samples[i] *= loudness;
        }
    }

    // Process with the real LastWave DspProcessor!
    gDsp.process(samples, frameCount, channelCount);
}

extern "C" JNIEXPORT void JNICALL
Java_com_myplayer_app_playback_dsp_MasterAudioDspProcessor_nativeProcessPcm16(
        JNIEnv *env,
        jobject /*thiz*/,
        jobject directBuffer,
        jint byteOffset,
        jint frameCount,
        jint channelCount) {

    if (directBuffer == nullptr || frameCount <= 0 || (channelCount != 1 && channelCount != 2)) {
        return;
    }

    auto *base = static_cast<uint8_t *>(env->GetDirectBufferAddress(directBuffer));
    if (base == nullptr) return;

    auto *samples = reinterpret_cast<int16_t *>(base + byteOffset);
    const size_t totalSamples = static_cast<size_t>(frameCount) * static_cast<size_t>(channelCount);

    thread_local std::vector<float> scratch;
    if (scratch.size() < totalSamples) {
        scratch.resize(totalSamples);
    }

    // Convert Int16 -> Float32
    if (!lastwave::audio::PcmConverter::toFloat(
            samples, totalSamples, lastwave::audio::PcmFormat::Int16, scratch.data())) {
        return;
    }

    // Apply loudness gain if non-unity
    const float loudness = gLoudnessGainLinear.load(std::memory_order_relaxed);
    if (std::abs(loudness - 1.0f) > 0.0001f) {
        for (size_t i = 0; i < totalSamples; ++i) {
            scratch[i] *= loudness;
        }
    }

    // Process with the real LastWave DspProcessor!
    gDsp.process(scratch.data(), frameCount, channelCount);

    // Convert Float32 -> Int16
    for (size_t i = 0; i < totalSamples; ++i) {
        const float f = scratch[i];
        samples[i] = static_cast<int16_t>(std::clamp(std::lrintf(f * 32767.0f), -32768L, 32767L));
    }
}
