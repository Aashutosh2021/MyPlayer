# Recommendation Subsystem

This document provides technical details on the Autoplay Recommendation subsystem beyond the high-level `RECOMMENDATION_ENGINE.md`.

## InnerTube API

The `RecommendationSource` interfaces with YouTube's private InnerTube API using Retrofit.
Because this is not a public API, requests must be meticulously constructed to mimic the official YouTube Music client.

- **Endpoint:** `POST https://music.youtube.com/youtubei/v1/next`
- **Payload:** Requires a specific JSON structure including `context.client` (identifying as Android/Web), and the `videoId` of the seed song.
- **Parsing:** The response is a deeply nested, dynamic JSON tree. We use Gson to parse it into an intermediate tree, then manually traverse to extract `musicResponsiveListItemRenderer` nodes which contain the visually/musically related tracks.

## State Management

The `RecommendationCacheRepository` prevents the autoplay engine from looping.

- **`historyIds`:** A `Set<String>` storing the `videoId` of every track played during the current session.
- **`rejectedIds`:** A `Set<String>` storing tracks the user manually removed from the "Up Next" queue.
- **Filtering:** When the `RecommendationPlaybackRepository` returns new recommendations, the Cache layer intercepts them and filters out any track present in `historyIds` or `rejectedIds`.

## Preloading & Minimum Threshold

The `RecommendationCoordinator` monitors the size of the `queueState`. If the queue size drops below a threshold (e.g., 5 songs), it triggers a background network fetch. This ensures that the user never hits the end of the queue without new songs already buffered and ready to play.

## Playback Completion Guard

ExoPlayer's `Player.Listener.onPlaybackStateChanged` can fire `Player.STATE_ENDED` multiple times in succession due to internal player resets, audio focus loss/gain right at the end of a track, or buffer under-runs.

If the `MusicController` naively responded to every `STATE_ENDED` by calling `playOnlineSong(recommendationCoordinator.getNextAutoplaySong())`, a double-fire would instantly skip the first recommended song. 

The `PlaybackCompletionGuard` acts as a debounce mechanism. It tracks the `mediaId` of the last completed track and ignores subsequent `STATE_ENDED` events for that same `mediaId` within a 1-second window.
