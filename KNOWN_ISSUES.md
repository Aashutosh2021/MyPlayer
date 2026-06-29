# Known Issues & Limitations

This document tracks unresolved limitations and known issues in the current production build (RC-2). 

> **Note:** All previously tracked crash bugs, performance delays (the 2-3s touch lag), and scroll jank have been fully resolved in the Performance Investigation Sprint. 

## Unresolved Limitations

### 1. Offline Playlist Synchronization
Currently, downloading a track saves it locally, but the application does not feature background background differential syncing for entire playlists. Users must initiate downloads manually.

### 2. YouTube API Rate Limiting (InnerTube)
The Autoplay Recommendation Engine relies on reverse-engineered InnerTube API endpoints. Because this is unofficial, extremely heavy, continuous skipping may theoretically trigger YouTube's undocumented IP-based rate limiting, resulting in empty recommendation queues. The `RecommendationCoordinator` handles empty responses gracefully by pausing playback, but cannot bypass the block.

### 3. Missing Metadata on Niche Tracks
When searching for highly obscure tracks via YouTube, some tracks lack proper structured metadata (Artist/Album mapping). The system attempts to clean the titles using regex (removing "(Official Video)", "[4K]", etc.), but some tracks may still display messy titles.

### 4. LRCLIB Timestamp Skew
Synced lyrics are fetched from the open-source LRCLIB database. Occasionally, the LRC timestamps provided by the community for a specific track may be misaligned by a few seconds compared to the specific audio stream version fetched from YouTube.
