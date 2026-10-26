cmake_minimum_required(VERSION 3.22.1)

project("myplayer_native_modules")

# ── Security compiler flags ───────────────────────────────────────────────────
set(SECURITY_FLAGS
    "-fstack-protector-strong"
    "-D_FORTIFY_SOURCE=2"
    "-O2"
    "-fvisibility=hidden"
    "-fPIC"
)

# ── Target 1: Security & Tamper Detection (myplayer_security) ─────────────────
add_library(
    myplayer_security
    SHARED
    native-lib.cpp
)

target_compile_options(myplayer_security PRIVATE ${SECURITY_FLAGS})

target_link_options(myplayer_security PRIVATE
    "-Wl,--exclude-libs,ALL"
    "-Wl,-z,noexecstack"
    "-Wl,-z,relro"
    "-Wl,-z,now"
    "-Wl,-z,max-page-size=16384"
)

find_library(log-lib log)
target_link_libraries(myplayer_security ${log-lib})

# ── Target 2: LastWave 64-Bit DSP Audio Engine (lastwave_audio_engine) ─────────
add_library(
    lastwave_audio_engine
    SHARED
    AudioEngine.cpp
    DspProcessor.cpp
    ParametricEq.cpp
)

target_compile_options(lastwave_audio_engine PRIVATE
    "-std=c++17"
    "-O3"
    "-Wall"
    "-fPIC"
)
# Note: "-ffast-math" intentionally ABSENT — the DSP engine relies on
# std::isfinite() guards and anti-denormal checks that ffast-math optimizes away.

# 16KB page alignment for Android 15/16 (from LastWave commit f7afa9b)
target_link_options(lastwave_audio_engine PRIVATE
    "-Wl,-z,max-page-size=16384"
)

target_link_libraries(
    lastwave_audio_engine
    android
    log
    m
)
