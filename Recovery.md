R1 → Playback Recovery
R2 → Download Recovery
R3 → UI Performance
R4 → MusicController Refactor
R5 → Playback Router
R6 → Recommendation Decouple
R7 → Playlist Refactor
R8 → Library Refactor
R9 → QA Sprint
R10 → Release Candidate




# Phase R1 – Playback Recovery

Objective:

Fix the playback regression where downloaded songs require an internet connection.

Reference Commit:
57bdbff925c2420c9d95bc6f3f7c2489889b22fd

Requirements

• Compare playback pipeline with reference commit.
• Remove incorrect routing.
• Downloaded songs must NEVER attempt network resolution.
• Playback must use local URI only.
• If local file missing:
    Show user-friendly error.
    Never fallback to streaming automatically.
• Online songs continue using stream resolver.
• Do not modify Recommendation.
• Do not modify Lyrics.
• Do not modify UI.

Testing

Verify:

✓ Download song
✓ Turn internet OFF
✓ Play downloaded song
✓ Play from Downloads
✓ Play from Library
✓ Play from Playlist
✓ Resume playback

Output:

PLAYBACK_RECOVERY_REPORT.md




# Phase R2 – Download Recovery

Requirements

Normalize Download database.

Room must store:

videoId

localUri

downloadStatus

fileSize

checksum

downloadTime

Remove dependency on absolute paths.

Do not scan folders during playback.

Do not resolve stream URLs.

Update playlists when download completes.

Verify:

Download

Delete

Re-download

Rename

Offline playback

Generate:

DOWNLOAD_RECOVERY_REPORT.md



# Phase R3 – Compose Performance Recovery

No new UI.

Only optimize.

Audit:

Home

Search

Library

Now Playing

Recommendations

Lyrics

Mini Player

Find:

Recomposition

StateFlow storms

Image loading

Heavy modifiers

Blur

Shadow

Animation

Nested Lazy layouts

Fix only verified issues.

Measure:

Frame Time

Janky Frames

Memory

CPU

Output report.



# Phase R4 – MusicController Decomposition

MusicController has become a God Class.

Split responsibilities.

Extract:

PlaybackRouter

PlaybackStateManager

PlaybackQueueManager

PlaybackSessionManager

PlaybackErrorHandler

PlaybackEvents

MusicController should only:

Play

Pause

Seek

Stop

Next

Previous

No network.

No downloads.

No recommendation.

No lyrics.

No storage.

Compile after every extraction.




# Phase R5 – Playback Router

Create:

PlaybackRouter

Responsibilities:

Receive PlayRequest

Determine source

Route to:

Local

Online

Downloaded

Recommendation

Return MediaItem only.

Never call ExoPlayer directly.

Testing:

Every play entry point.



# Phase R6 – Recommendation Decoupling

Recommendation must not depend on MusicController.

Use:

Playback Events

↓

Recommendation Queue

↓

Autoplay

Remove direct controller coupling.

Keep autoplay working.

Regression test.



# Phase R7 – Playlist Refactor

Current issue:

Playlist stores stale paths.

Instead:

Store only:

videoId

songId

Lookup playback source dynamically.

When download completes:

Playlist automatically plays local version.

Offline support mandatory.



# Phase R8 – Library Refactor

Separate:

Local Library

Downloaded Library

Online Search

Favorites

Playlists

Each independent.

No duplicated SongEntity.

Normalize models.



# Phase R9 – Full Regression Testing

Test:

Playback

Downloads

Offline

Search

Recommendations

Lyrics

Playlists

Favorites

Library

Settings

Rotation

Background playback

Notification

Lockscreen

Bluetooth

Headphones

No feature additions.

Generate QA report.



# Phase R10 – RC Build

Optimize:

APK

Resources

R8

Proguard

Baseline Profile

Startup

Generate:

Release Notes

Performance Report

APK Report

Architecture Report

Tag:

v2.0 RC1



Final order

R0 → Freeze
R1 → Playback
R2 → Downloads
R3 → UI Performance
R4 → MusicController
R5 → PlaybackRouter
R6 → Recommendation
R7 → Playlist
R8 → Library
R9 → QA
R10 → Release
