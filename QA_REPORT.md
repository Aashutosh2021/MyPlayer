# QA & Verification Matrix Report (Phase R9)

This QA report logs the end-to-end verification results of all core user features, offline capabilities, and lifecycle states in **MyPlayer V2** following recovery integrations.

---

## 1. Verification Matrix

| Component | Feature / Scenario | Input / Action | Result | Status |
|---|---|---|---|---|
| **Playback** | Scanned Local Song | Play scanned device audio file | Path verified; played locally | **PASS** |
| | Downloaded Song | Play downloaded `.m4a` YouTube track | Parsed local URI; played from storage | **PASS** |
| | Online Streaming | Search & Play online song | YouTube stream URL resolved dynamically | **PASS** |
| | Custom Playlists | Play queue of local/downloaded mix | PlaybackRouter resolved indices sequentially | **PASS** |
| | Favorites | Add to Favorites & Play | Correctly mapped, played via router | **PASS** |
| **Downloads** | Expedited Download | Enqueue song download | Scheduled immediately via WorkManager | **PASS** |
| | Download Cancellation | Cancel active download task | Worker terminated, tmp files cleaned up | **PASS** |
| | Duplicate Prevention | Re-download already cached song | Guard checked database, skipped task | **PASS** |
| **Recommendation**| Autoplay Generation | End of current song list | Emitted AutoplayRequested; enqueued next | **PASS** |
| | Cache Lookup | Seek same seed recommendations | Instant cached list lookup, no api call | **PASS** |
| **Lyrics** | Synced LRC Lyrics | Play song with LRC lyrics | Real-time scroll synced to positionMs | **PASS** |
| | Fuzzy Search | Play song with spelling drift | API searched & calculated confidence | **PASS** |
| **Settings** | Sleep Timer | Set sleep timer duration | Paused playback cleanly on countdown | **PASS** |
| | Backup & Restore | Run backup to JSON | JSON exported/imported database state | **PASS** |
| | Shuffle & Repeat | Toggle Player Mode | ExoPlayer modes updated immediately | **PASS** |
| **Notification** | Media Control Bar | Play/Pause/Skip from system drawer | Emitted session updates to drawer controls | **PASS** |

---

## 2. Offline Mode Scenarios

### Scenario 1: Play Downloaded Song Offline
- **Action**: Disable Mobile Data & Wi-Fi, select downloaded song.
- **Verification**: `PlaybackSourceResolver` checked `downloaded_songs` table, retrieved `localPath`, validated file existence, and bypassed stream extraction.
- **Status**: **PASS** (Zero network dependency).

### Scenario 3: Recommendation Offline Behavior
- **Action**: Enable autoplay recommendations while offline.
- **Verification**: Autoplay requested recommendation seed; repository checked network status, found no connection, and gracefully aborted recommendation generation, logging an offline info event.
- **Status**: **PASS** (No crash/ANR; playback stopped at queue end).

---

## 3. Lifecycle & System Events

* **Home Button / Background Playback**: Played music, pressed Home button. ExoPlayer switched to background foreground service execution. Notification remained active. **PASS**.
* **System Process Restoration**: App swiped away in Recents. Foreground service cleared notification, released ExoPlayer, and terminated clean. **PASS**.
* **Configuration / Screen Rotation**: Rotated screen 90 degrees during active playback. UI state preserved via ViewModel flow states, music continued without interruptions. **PASS**.
