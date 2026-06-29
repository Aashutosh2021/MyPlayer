# Project Overview

MyPlayer is an Android application engineered to provide a seamless transition between local music library management and infinite online music discovery.

## The Goal
Most music players are either entirely local (like Poweramp) or entirely cloud-based (like Spotify/YouTube Music). MyPlayer bridges this gap. A user can start by playing a local MP3 file, and when the queue ends, the app seamlessly queries the YouTube InnerTube API to find similar tracks, buffering and streaming them automatically.

## Scale and Metrics
As of RC-2, the MyPlayer codebase consists of:
- **112 Kotlin source files**
- **~10,876 Lines of Code**
- **41 distinct packages**
- **10 Jetpack Compose Screens**
- **9 ViewModels**
- **7 Repositories**

## Core Pillars

1. **Performance:** The app targets 120fps on modern displays. This is achieved via strict MVVM Unidirectional Data Flow, single-pass Compose measurements, and rigid thread isolation (all IO is forced off the Main thread).
2. **Aesthetics:** The UI completely eschews standard Google Material 3 flat design in favor of a bespoke, tactile "Clay/Glassmorphism" UI, utilizing custom draw modifiers to generate rich inner and outer shadows without expensive Gaussian blurs.
3. **Stability:** The playback engine uses AndroidX Media3, robustly handling audio focus, headset unplug events, sleep timers, and background Service lifecycle constraints in Android 14.
