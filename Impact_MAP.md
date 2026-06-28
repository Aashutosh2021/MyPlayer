# IMPACT_MAP.md

# Feature Impact Matrix

Purpose

Predict every component affected before changing code.

---

## Playback

MusicController

↓

PlayerService

↓

Notification

↓

Queue

↓

Now Playing

↓

Mini Player

↓

SeekBar

↓

Playback State

---

## Queue

QueueManager

↓

PlayerViewModel

↓

NowPlaying

↓

Playlist

↓

Notification

---

## Playlist

PlaylistRepository

↓

Database

↓

Playlist Screen

↓

Queue

---

## Recommendation

Recommendation Engine

↓

Recommendation Repository

↓

Search

↓

Home Screen

↓

Now Playing

↓

Related Songs

---

## Search

Search Repository

↓

API Layer

↓

Cache

↓

UI

↓

History

---

## Downloads

Download Repository

↓

Worker

↓

Notification

↓

Database

↓

Downloads Screen

---

## Lyrics

Lyrics Service

↓

Player

↓

Now Playing

↓

Cache

---

## Equalizer

DSP

↓

Audio Engine

↓

Player

↓

Settings

---

## Authentication

Auth Manager

↓

Database

↓

API

↓

Profile

---

## Settings

Preference Manager

↓

Theme

↓

Player

↓

Downloads

↓

Network

---

## Cross Device Sync

Database

↓

API

↓

Authentication

↓

Player State

↓

Queue

↓

Playlists

---

Before implementing any feature, identify every affected block above.
