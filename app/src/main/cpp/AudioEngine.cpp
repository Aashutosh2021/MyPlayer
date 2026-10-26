#include <jni.h>
#include <cmath>
#include <algorithm>
#include <cstring>
#include <cstdint>
#include <limits>

#ifndef M_PI
#define M_PI 3.1415926535897932384626433832795
#endif

namespace {

constexpr double kPi = 3.1415926535897932384626433832795;
constexpr float kOutputCeiling = 0.944060876F; // -0.5 dBFS
constexpr float kSoftKneeThreshold = 0.841395141F; // -1.5 dBFS
constexpr float kClarityStereoWidth = 1.22F;
constexpr float kClarityMakeupGain = 1.04F;
constexpr float kAirExciterAmount = 0.22F;
constexpr float kLimiterEngageThreshold = 1.25F; // Preserves transient dynamics without squash

inline double safeFrequency(double sampleRate, double freq) noexcept {
    return std::clamp(freq, 10.0, sampleRate * 0.45);
}

struct Biquad {
    double b0 = 1.0, b1 = 0.0, b2 = 0.0, a1 = 0.0, a2 = 0.0;
    double z1[2] = {0.0, 0.0};
    double z2[2] = {0.0, 0.0};

    inline void clear() noexcept {
        z1[0] = z1[1] = z2[0] = z2[1] = 0.0;
    }

    inline float tick(float in, int ch) noexcept {
        double val = static_cast<double>(in);
        double out = b0 * val + z1[ch];
        z1[ch] = b1 * val - a1 * out + z2[ch];
        z2[ch] = b2 * val - a2 * out;

        if (std::abs(z1[ch]) < 1.0e-20) z1[ch] = 0.0;
        if (std::abs(z2[ch]) < 1.0e-20) z2[ch] = 0.0;
        if (!std::isfinite(out) || !std::isfinite(z1[ch]) || !std::isfinite(z2[ch])) {
            z1[ch] = z2[ch] = 0.0;
            return 0.0f;
        }
        return static_cast<float>(out);
    }

    void setPeaking(double sampleRate, double freq, double q, double gainDb) noexcept {
        if (std::abs(gainDb) < 0.005 || freq >= sampleRate * 0.45) {
            b0 = 1.0; b1 = 0.0; b2 = 0.0; a1 = 0.0; a2 = 0.0;
            return;
        }
        double omega = 2.0 * kPi * safeFrequency(sampleRate, freq) / sampleRate;
        double cosine = std::cos(omega);
        double alpha = std::sin(omega) / (2.0 * std::max(q, 0.01));
        double amplitude = std::pow(10.0, gainDb / 40.0);
        double a0 = 1.0 + alpha / amplitude;

        b0 = (1.0 + alpha * amplitude) / a0;
        b1 = (-2.0 * cosine) / a0;
        b2 = (1.0 - alpha * amplitude) / a0;
        a1 = (-2.0 * cosine) / a0;
        a2 = (1.0 - alpha / amplitude) / a0;
    }

    void setHighPass(double sampleRate, double freq, double q) noexcept {
        double omega = 2.0 * kPi * safeFrequency(sampleRate, freq) / sampleRate;
        double cosine = std::cos(omega);
        double alpha = std::sin(omega) / (2.0 * std::max(q, 0.01));
        double a0 = 1.0 + alpha;

        b0 = ((1.0 + cosine) * 0.5) / a0;
        b1 = (-(1.0 + cosine)) / a0;
        b2 = ((1.0 + cosine) * 0.5) / a0;
        a1 = (-2.0 * cosine) / a0;
        a2 = (1.0 - alpha) / a0;
    }

    void setHighShelf(double sampleRate, double freq, double slope, double gainDb) noexcept {
        if (std::abs(gainDb) < 0.005 || freq >= sampleRate * 0.45) {
            b0 = 1.0; b1 = 0.0; b2 = 0.0; a1 = 0.0; a2 = 0.0;
            return;
        }
        double omega = 2.0 * kPi * safeFrequency(sampleRate, freq) / sampleRate;
        double cosine = std::cos(omega);
        double sine = std::sin(omega);
        double amplitude = std::pow(10.0, gainDb / 40.0);
        double safeSlope = std::clamp(slope, 0.1, 1.0);
        double alpha = (sine * 0.5) * std::sqrt((amplitude + 1.0 / amplitude) * (1.0 / safeSlope - 1.0) + 2.0);
        double beta = 2.0 * std::sqrt(amplitude) * alpha;
        double a0 = (amplitude + 1.0) - (amplitude - 1.0) * cosine + beta;

        b0 = (amplitude * ((amplitude + 1.0) + (amplitude - 1.0) * cosine + beta)) / a0;
        b1 = (-2.0 * amplitude * ((amplitude - 1.0) + (amplitude + 1.0) * cosine)) / a0;
        b2 = (amplitude * ((amplitude + 1.0) + (amplitude - 1.0) * cosine - beta)) / a0;
        a1 = (2.0 * ((amplitude - 1.0) - (amplitude + 1.0) * cosine)) / a0;
        a2 = ((amplitude + 1.0) - (amplitude - 1.0) * cosine - beta) / a0;
    }

    void setLowShelf(double sampleRate, double freq, double slope, double gainDb) noexcept {
        if (std::abs(gainDb) < 0.005 || freq >= sampleRate * 0.45) {
            b0 = 1.0; b1 = 0.0; b2 = 0.0; a1 = 0.0; a2 = 0.0;
            return;
        }
        double omega = 2.0 * kPi * safeFrequency(sampleRate, freq) / sampleRate;
        double cosine = std::cos(omega);
        double sine = std::sin(omega);
        double amplitude = std::pow(10.0, gainDb / 40.0);
        double safeSlope = std::clamp(slope, 0.1, 1.0);
        double alpha = (sine * 0.5) * std::sqrt((amplitude + 1.0 / amplitude) * (1.0 / safeSlope - 1.0) + 2.0);
        double beta = 2.0 * std::sqrt(amplitude) * alpha;
        double a0 = (amplitude + 1.0) + (amplitude - 1.0) * cosine + beta;

        b0 = (amplitude * ((amplitude + 1.0) - (amplitude - 1.0) * cosine + beta)) / a0;
        b1 = (2.0 * amplitude * ((amplitude - 1.0) - (amplitude + 1.0) * cosine)) / a0;
        b2 = (amplitude * ((amplitude + 1.0) - (amplitude - 1.0) * cosine - beta)) / a0;
        a1 = (-2.0 * ((amplitude - 1.0) + (amplitude + 1.0) * cosine)) / a0;
        a2 = ((amplitude + 1.0) + (amplitude - 1.0) * cosine - beta) / a0;
    }
};

static const double ISO_15_FREQS[15] = {
    25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0,
    1000.0, 1600.0, 2500.0, 4000.0, 6300.0, 10000.0, 16000.0
};

static Biquad eqBands[15];
// Studio Master Clarity 8-Stage Chain
static Biquad subBassHighPass;   // 24 Hz
static Biquad bassFoundation;    // 72 Hz
static Biquad lowMidSeparation;  // 280 Hz
static Biquad boxinessControl;   // 750 Hz
static Biquad presenceDetail;    // 3400 Hz
static Biquad airDetail;         // 10500 Hz
static Biquad airExciterFilter;  // 6000 Hz
static Biquad monoBassFilter;    // 130 Hz

static double gSampleRate = 48000.0;
static bool gBitPerfect = false;
static bool gEqEnabled = true;
static bool gStudioClarity = false;
static float gPreampLinear = 1.0f;
static float gLimiterGain = 1.0f;

inline float softSaturate(float x) noexcept {
    const float absX = std::abs(x);
    if (absX <= kSoftKneeThreshold) return x;
    constexpr float range = kOutputCeiling - kSoftKneeThreshold;
    const float excess = absX - kSoftKneeThreshold;
    const float ratio = std::tanh(excess / range);
    const float saturated = kSoftKneeThreshold + range * ratio;
    return x >= 0.0f ? saturated : -saturated;
}

inline void processFrame(float &left, float &right, bool isStereo) noexcept {
    if (gBitPerfect) return;

    // Apply Preamp
    left *= gPreampLinear;
    right *= gPreampLinear;

    // Stage 1: 15-Band ISO Equalizer
    if (gEqEnabled) {
        for (int b = 0; b < 15; ++b) {
            left = eqBands[b].tick(left, 0);
            if (isStereo) right = eqBands[b].tick(right, 1);
        }
        if (!isStereo) right = left;
    }

    const float dryL = left;
    const float dryR = right;

    // Stage 2: Studio Master Clarity Multi-Stage Acoustic Chain
    if (gStudioClarity) {
        float wetL = subBassHighPass.tick(left, 0);
        float wetR = isStereo ? subBassHighPass.tick(right, 1) : wetL;

        wetL = bassFoundation.tick(wetL, 0);
        wetR = isStereo ? bassFoundation.tick(wetR, 1) : wetL;

        wetL = lowMidSeparation.tick(wetL, 0);
        wetR = isStereo ? lowMidSeparation.tick(wetR, 1) : wetL;

        wetL = boxinessControl.tick(wetL, 0);
        wetR = isStereo ? boxinessControl.tick(wetR, 1) : wetL;

        wetL = presenceDetail.tick(wetL, 0);
        wetR = isStereo ? presenceDetail.tick(wetR, 1) : wetL;

        wetL = airDetail.tick(wetL, 0);
        wetR = isStereo ? airDetail.tick(wetR, 1) : wetL;

        // Harmonic Air Exciter (cubic tape sheen on >6 kHz)
        float exciterInL = airExciterFilter.tick(dryL, 0);
        float exciterInR = isStereo ? airExciterFilter.tick(dryR, 1) : exciterInL;
        float excitedL = exciterInL - (exciterInL * exciterInL * exciterInL * 0.25f);
        float excitedR = exciterInR - (exciterInR * exciterInR * exciterInR * 0.25f);
        wetL += excitedL * kAirExciterAmount;
        wetR += excitedR * kAirExciterAmount;

        // Mid-Side Mono-Bass Anti-Blur & Soundstage Expansion
        if (isStereo) {
            float mid = (wetL + wetR) * 0.5f;
            float rawSide = (wetL - wetR) * 0.5f;
            float sideHigh = monoBassFilter.tick(rawSide, 0); // 130 Hz highpass on side
            float wideSide = sideHigh * kClarityStereoWidth;
            wetL = mid + wideSide;
            wetR = mid - wideSide;
        }
        wetL *= kClarityMakeupGain;
        wetR *= kClarityMakeupGain;

        left = wetL;
        right = wetR;
    }

    // Stage 3: Analog Soft-Knee Saturation + Punchy Limiter
    left = softSaturate(left);
    right = softSaturate(right);

    float peak = std::max(std::abs(left), std::abs(right));
    float reqLimiter = peak > kLimiterEngageThreshold ? kLimiterEngageThreshold / peak : 1.0f;
    if (reqLimiter < gLimiterGain) {
        gLimiterGain = reqLimiter;
    } else {
        gLimiterGain += (1.0f - gLimiterGain) * 0.002f;
        if (1.0f - gLimiterGain < 0.0001f) gLimiterGain = 1.0f;
    }
    left *= gLimiterGain;
    right *= gLimiterGain;
}

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_example_myplayer_playback_dsp_MasterAudioDspProcessor_nativeConfigure(
        JNIEnv *env, jobject /*thiz*/, jint sampleRate, jboolean bitPerfect,
        jboolean eqEnabled, jfloatArray eqGains,
        jboolean studioClarity, jint clarityPresetOrd, jfloat loudnessGainDb) {

    gSampleRate = (sampleRate > 0) ? static_cast<double>(sampleRate) : 48000.0;
    gBitPerfect = bitPerfect;
    gEqEnabled = eqEnabled;
    gStudioClarity = studioClarity;

    // 1. Configure 15-Band ISO Equalizer
    if (eqGains != nullptr) {
        jfloat *gains = env->GetFloatArrayElements(eqGains, nullptr);
        if (gains != nullptr) {
            for (int i = 0; i < 15; ++i) {
                double g = gEqEnabled ? static_cast<double>(gains[i]) : 0.0;
                eqBands[i].setPeaking(gSampleRate, ISO_15_FREQS[i], 1.41421356, g);
            }
            env->ReleaseFloatArrayElements(eqGains, gains, JNI_ABORT);
        }
    }

    // 2. Configure LastWave Studio Master Clarity Chain
    // Trims: 0: SubHP, 1: BassFoundation, 2: Anti-Mud, 3: Boxiness, 4: Presence, 5: AirShelf
    double bassGain = 3.2, mudGain = -3.0, boxGain = -1.4, presenceGain = 3.8, airGain = 4.8;
    if (clarityPresetOrd == 1) { // SPEAKER
        bassGain -= 1.5; airGain -= 0.5;
    } else if (clarityPresetOrd == 2) { // HEADPHONE
        bassGain -= 0.5; presenceGain -= 1.0;
    } else if (clarityPresetOrd == 3) { // DAC
        airGain -= 1.5;
    }

    subBassHighPass.setHighPass(gSampleRate, 24.0, 0.7071);
    bassFoundation.setPeaking(gSampleRate, 72.0, 0.80, bassGain);
    lowMidSeparation.setPeaking(gSampleRate, 280.0, 0.90, mudGain);
    boxinessControl.setPeaking(gSampleRate, 750.0, 0.85, boxGain);
    presenceDetail.setPeaking(gSampleRate, 3400.0, 0.85, presenceGain);
    airDetail.setHighShelf(gSampleRate, 10500.0, 0.85, airGain);
    airExciterFilter.setHighPass(gSampleRate, 6000.0, 0.7071);
    monoBassFilter.setHighPass(gSampleRate, 130.0, 0.7071);

    // Preamp Linear Gain: +1.0 dB pre-limiter boost as in LastWave kEqualizerPreLimiterBoostDb
    float totalGainDb = static_cast<float>(loudnessGainDb) + (gEqEnabled ? 1.0f : 0.0f);
    gPreampLinear = std::pow(10.0f, totalGainDb / 20.0f);
}

// Float32 Direct Buffer Processing
extern "C" JNIEXPORT void JNICALL
Java_com_example_myplayer_playback_dsp_MasterAudioDspProcessor_nativeProcessPcmFloat(
        JNIEnv *env, jobject /*thiz*/, jobject directBuffer, jint byteOffset, jint frameCount, jint channelCount) {

    if (gBitPerfect || directBuffer == nullptr || frameCount <= 0) return;

    auto *base = static_cast<uint8_t *>(env->GetDirectBufferAddress(directBuffer));
    if (base == nullptr) return;

    auto *samples = reinterpret_cast<float *>(base + byteOffset);
    bool isStereo = (channelCount == 2);

    for (int i = 0; i < frameCount; ++i) {
        if (isStereo) {
            processFrame(samples[i * 2], samples[i * 2 + 1], true);
        } else {
            float dummy = samples[i];
            processFrame(samples[i], dummy, false);
        }
    }
}

// Int16 Direct Buffer Processing
extern "C" JNIEXPORT void JNICALL
Java_com_example_myplayer_playback_dsp_MasterAudioDspProcessor_nativeProcessPcm16(
        JNIEnv *env, jobject /*thiz*/, jobject directBuffer, jint byteOffset, jint frameCount, jint channelCount) {

    if (gBitPerfect || directBuffer == nullptr || frameCount <= 0) return;

    auto *base = static_cast<uint8_t *>(env->GetDirectBufferAddress(directBuffer));
    if (base == nullptr) return;

    auto *samples = reinterpret_cast<int16_t *>(base + byteOffset);
    bool isStereo = (channelCount == 2);

    for (int i = 0; i < frameCount; ++i) {
        if (isStereo) {
            float L = static_cast<float>(samples[i * 2]) / 32768.0f;
            float R = static_cast<float>(samples[i * 2 + 1]) / 32768.0f;
            processFrame(L, R, true);
            samples[i * 2] = static_cast<int16_t>(std::clamp(lrintf(L * 32767.0f), -32768L, 32767L));
            samples[i * 2 + 1] = static_cast<int16_t>(std::clamp(lrintf(R * 32767.0f), -32768L, 32767L));
        } else {
            float mono = static_cast<float>(samples[i]) / 32768.0f;
            float dummy = mono;
            processFrame(mono, dummy, false);
            samples[i] = static_cast<int16_t>(std::clamp(lrintf(mono * 32767.0f), -32768L, 32767L));
        }
    }
}

// Fallback for ByteArray Int16
extern "C" JNIEXPORT void JNICALL
Java_com_example_myplayer_playback_dsp_MasterAudioDspProcessor_nativeProcessPcm16Array(
        JNIEnv *env, jobject /*thiz*/, jbyteArray pcmBytes, jint length, jint channelCount) {

    if (gBitPerfect || pcmBytes == nullptr || length <= 0) return;

    jbyte *raw = env->GetByteArrayElements(pcmBytes, nullptr);
    if (raw == nullptr) return;

    auto *samples = reinterpret_cast<int16_t *>(raw);
    bool isStereo = (channelCount == 2);
    int frameCount = length / (isStereo ? 4 : 2);

    for (int i = 0; i < frameCount; ++i) {
        if (isStereo) {
            float L = static_cast<float>(samples[i * 2]) / 32768.0f;
            float R = static_cast<float>(samples[i * 2 + 1]) / 32768.0f;
            processFrame(L, R, true);
            samples[i * 2] = static_cast<int16_t>(std::clamp(lrintf(L * 32767.0f), -32768L, 32767L));
            samples[i * 2 + 1] = static_cast<int16_t>(std::clamp(lrintf(R * 32767.0f), -32768L, 32767L));
        } else {
            float mono = static_cast<float>(samples[i]) / 32768.0f;
            float dummy = mono;
            processFrame(mono, dummy, false);
            samples[i] = static_cast<int16_t>(std::clamp(lrintf(mono * 32767.0f), -32768L, 32767L));
        }
    }

    env->ReleaseByteArrayElements(pcmBytes, raw, 0);
}
