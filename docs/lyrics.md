# Lyrics Integration

MyPlayer features a robust, real-time synced lyrics engine powered by the open-source LRCLIB database.

## Architecture

1. **`LyricsRepository`:** Handles network calls to `https://lrclib.net/api`. It first attempts to fetch `.lrc` synced lyrics. If unavailable, it falls back to plain text lyrics.
2. **`LyricsViewModel`:** Scoped to the `NowPlayingScreen`. It automatically triggers a fetch whenever `MusicController.currentSong` changes.
3. **`LyricsTab`:** The UI component responsible for displaying the lyrics and auto-scrolling to the active line.

## Query Optimization
YouTube video titles often contain heavy noise (e.g., "Song Name (Official Music Video) [4K]"). LRCLIB is highly sensitive to string matching. 
The `LyricsRepository` uses pre-compiled `Regex` constants to strip brackets, parentheses, and common noise keywords ("official", "audio", "lyrical") before querying the API. This drastically improves the hit rate.

## Scrolling Performance
Synced lyrics update the UI position every 500ms based on the playback clock. 

Initially, this caused severe UI stutter because the active line index was recalculated inside a `remember` block, firing a scroll animation 120 times per minute.

**The Fix:** The index calculation is wrapped in `derivedStateOf`. This ensures that the Compose Recomposition engine only triggers the `LaunchedEffect(activeIndex)` when the user actually transitions to a *new line* of lyrics, completely eliminating main-thread backpressure.
