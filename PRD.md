# Project Requirements Document (PRD) - MyPlayer

## 1. Project Overview
MyPlayer is a comprehensive Android music player application designed to handle both local audio files and online music streaming via the YouTube Music (Innertube) API. The application emphasizes high-quality audio control through a built-in Digital Signal Processing (DSP) system and a seamless user experience for music discovery and playback.

## 2. Target Audience
- Music enthusiasts who want a high level of control over audio output (EQ, Bass, Loudness).
- Users who maintain a local music library but also want to stream and download music from the web.

## 3. Functional Requirements

### 3.1 Music Playback
- **Local Playback**: Scan and play audio files from the device storage.
- **Online Streaming**: Search and stream music using the YouTube Music API.
- **Playback Controls**: Standard play, pause, skip (next/previous), and seeking functionality.
- **Mini Player**: A persistent overlay allowing quick controls and access to the full player.
- **Sleep Timer**: Ability to set a countdown timer after which playback automatically stops.

### 3.2 Library Management
- **Automatic Scanning**: Scan device storage to populate the local library.
- **Playlist Management**: Create and manage playlists with an ordered list of songs.
- **Favorites**: Mark songs as favorites for quick access.
- **Recent History**: Track recently played songs.
- **Folder Organization**: Organise local music by folder structure.

### 3.3 Online Features
- **Online Search**: Search for songs, artists, or albums via the YouTube Music interface.
- **Music Downloading**: Download songs from the web to local storage via a background worker for offline playback.

### 3.4 User Interface & Experience
- **Modern Design**: Material 3 based UI using Jetpack Compose with a custom **Glassmorphism** aesthetic (glass-like backgrounds, neon accents, and soft glow effects).
- **Intuitive Navigation**: Floating navigation bar for quick switching between Home, Search, Library, and Downloads.
- **Now Playing Screen**: Full-screen view with album art, playback progress, and integrated playback controls, including a Sleep Timer dialog.

## 4. Non-Functional Requirements
- **Performance**: Low-latency audio playback and smooth UI transitions.
- **Stability**: Background playback must remain stable via a foreground service.
- **Efficiency**: Efficient database queries using Room and optimized network calls.
- **Reliability**: Background downloads must be handled by WorkManager to ensure completion even if the app is closed.
- **Security & Integrity**: The application must be hardened against reverse engineering, tampering, repackaging, and runtime hooking (Frida) to protect its core streaming logic. Sensitive strings must be encrypted and not present in plain text.