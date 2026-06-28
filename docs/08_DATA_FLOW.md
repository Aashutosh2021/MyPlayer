# Data Flow - MyPlayer

This document outlines how data moves through the **MyPlayer** application, mapping the transition from raw remote API payloads to database entities, memory states, and UI displays.

---

## 1. Remote API to UI Search Flow

This section details how search data flows from YouTube Music to the `SearchScreen`.

```
[Search Query Input] (User types "Classical")
         ↓ (500ms Debounce in OnlineSearchViewModel)
[OnlineSearchRepository.search(query)]
         ↓ (PagingConfig pageSize=20)
[InnertubeSearchPagingSource.load()]
         ↓ (OkHttp POST /search?key=API_KEY)
[InnertubeApi.search()]
         ↓ (XOR Decrypted Keys via StringEncryptionManager)
[Raw HTTP JSON Response]
         ↓ (JSONObject Manual Parsing)
[List<OnlineSong> + Continuation Token]
         ↓ (Flow<PagingData<OnlineSong>>)
[collectedAsStateWithLifecycle()]
         ↓
[SearchScreen Composable Grid]
```

---

## 2. Stream Resolution Flow

This pathway describes the data transformation required to play an online track.

```
[OnlineSong] (Contains: videoId = "dQw4w9WgXcQ")
         ↓
[InnertubeApi.getStreamUrl(videoId)]
         ↓ (ServiceList.YouTube.getStreamExtractor)
[NewPipeExtractor: audioStreams]
         ↓ (maxByOrNull { it.getBitrate() })
[AudioStream Object] (Contains: codec, bitrate, content link)
         ↓ (url = stream.getContent())
[Direct Media Link] (e.g. "https://rr4---sn-hp57knz.googlevideo.com/videoplayback?...")
         ↓
[MusicController.playOnlineSong(playableSong)]
         ↓
[MediaItem] (Injects URL into ExoPlayer)
         ↓
[Audio Track Output]
```

---

## 3. Background Download Flow

This pathway maps the flow of downloaded audio data from YouTube to local device storage and database cache:

```
[User clicks download] (OnlineSong metadata)
         ↓
[DownloadRepository.startDownload()]
         ↓ (workDataOf(videoId, title, streamUrl, etc.))
[WorkManager Database Queue]
         ↓ (DownloadWorker.doWork() on Dispatchers.IO)
[OkHttp chunked GET Request] (User-Agent bypass headers)
         ↓
[Chunk Stream Buffer] (8192 bytes buffer size)
         ↓ (contentResolver.openOutputStream(tmpDoc.uri))
[Temporary .tmp file] (e.g. "SongTitle.tmp")
         ↓ (Rename file to .m4a once final chunk matches size)
[Final .m4a File] (Saved in Custom SAF Folder or External Music Directory)
         ↓ (DownloadedSongEntity metadata creation)
[sqlite: downloaded_songs Table] (Record inserted via DownloadedSongDao)
         ↓
[UI Flow updates list]
```

---

## 4. Local Disk Media Scanning Flow

This pathway describes the process of scanning and importing local audio files into the library database:

```
[User adds directory] (Folder Uri selected via Storage Access Framework)
         ↓
[sqlite: folders Table] (FolderEntity written via FolderDao)
         ↓
[MediaScanner.scanFolder(folder)]
         ↓ (documentFile.listFiles() flat scan, no recursion)
[File Handles] (mp3, m4a, wav, flac, ogg formats)
         ↓ (MediaMetadataRetriever.setDataSource)
[Metadata Tags Extraction] (Title, Artist, Album, Duration, AlbumArt Uri)
         ↓
[SongEntity mappings] (id = fileUri.toString())
         ↓ (SongDao.insertSongs(List<SongEntity>))
[sqlite: songs Table] (Batch insert / replace)
         ↓
[Flow updates library views]
```

---

## 5. Playback Statistics Flow

Tracking user listening history and most-played metrics:

```
[ExoPlayer.play()] 
         ↓ (onMediaItemTransition triggers after index change)
[MusicController updates current state]
         ↓ (Launch Coroutine)
[MusicRepository.addRecentHistory(songId)]
         ↓
[sqlite: recent_history Table] (Insert record with current timestamp)
         ↓
[MusicRepository.incrementPlayCount(songId)]
         ↓
[sqlite: songs Table] (UPDATE songs SET playCount = playCount + 1 WHERE id = :songId)
         ↓
[Flow updates Home dashboard (Trending / Most Played)]
```
