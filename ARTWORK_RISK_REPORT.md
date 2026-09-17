# Artwork Engine Risk Assessment & Mitigation Report

## Overview
This document outlines the risk assessment, potential failure modes, performance considerations, and architectural mitigations incorporated into MyPlayer's dedicated **Artwork Engine**.

---

## 1. Risk Matrix

| Risk Factor | Probability | Impact | Severity | Mitigation Strategy |
|---|---|---|---|---|
| **External API Rate Limiting (Deezer)** | Medium | Low | **Low** | Built-in circuit-breaker: HTTP 429 or quota exceeded triggers automatic 60-second backoff. Execution immediately falls through to iTunes. |
| **Regional Geoblocking (Deezer)** | High | Low | **Low** | In regions where Deezer streaming is unlicensed (e.g. India), Deezer returns empty results. The multi-provider pipeline immediately resolves artwork from iTunes or YouTube Music without failing. |
| **Network Latency / Slow Response** | Medium | Medium | **Low** | Each provider has a strict 5-second call timeout. Resolution runs strictly on `Dispatchers.IO` and never blocks the UI or main thread. Initial local art/thumbnail is emitted immediately. |
| **Duplicate Concurrent Network Requests** | High | Medium | **Mitigated** | `inFlightRequests` deduplication map (`ConcurrentHashMap<String, Deferred<ArtworkResult?>>`) ensures that multiple UI components requesting the same song share a single network call. |
| **Displaying Wrong Covers for Remixes/Live** | Medium | High | **Mitigated** | Strict variant protection in `ArtworkMatcher`: Candidate covers containing `remix`, `live`, or `instrumental` are rejected if the query is a studio track. |
| **Excessive Disk Storage Consumption** | Low | Low | **Mitigated** | Persistent cache stores lightweight JSON metadata (~200 bytes per track). Automatic disk pruning runs when file count exceeds 2,000 files, removing oldest 25%. Image byte caching is managed by Coil's bounded disk cache. |
| **Repeated Queries for Non-Existent Tracks** | Medium | Medium | **Mitigated** | Negative caching: If all providers fail to resolve artwork for a track, a sentinel record (`notFound = true`) is cached for 24 hours to prevent repeated provider queries. |
| **Room Database Schema Corruption** | None | Critical | **Zero Risk** | Clean-room architectural design uses a dedicated disk-backed cache directory (`context.cacheDir/artwork_cache/`) rather than altering the Room SQLite schema. Zero migrations required. |

---

## 2. Performance & Threading Safeguards
- **Zero Main-Thread Blocking**: All network operations, JSON parsing, disk reads, and file writes run inside `withContext(Dispatchers.IO)`.
- **Instant UI Rendering**: `AlbumArtImage` renders the initial embedded art, local URI, or cached artwork immediately while high-res online updates resolve asynchronously in the background.
- **Batch Prefetch Throttling**: Batch prefetching for lists and playlists is throttled using a `Semaphore(4)` to prevent network congestion or memory spikes.

---

## 3. License & Compliance Verification
- **100% Clean-Room**: No code was copied or ported from GPLv3/copyleft repositories.
- **No API Secrets**: Uses public search endpoints from Deezer, Apple iTunes, and YouTube Music web clients with zero hardcoded API keys or secrets.
- **Android Guidelines**: Strictly follows Android Architecture Guidelines, Modern Android Development (MAD), and Jetpack Compose best practices.
