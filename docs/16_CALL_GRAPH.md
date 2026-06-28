# Call Graph - MyPlayer

This call graph maps the invocation hierarchies of critical operations within the **MyPlayer** codebase.

---

## 1. Playback Operations Call Graph

Describes what happens when a user clicks play on a song card.

### Flow A: Local Song Playback
```
LibraryScreen.kt (Click item card)
  └─► LibraryViewModel.playSong(playableSong)
        └─► MusicController.playSong(playableSong)
              └─► MusicController.playSongs(listOf(songEntity), 0)
                    ├─► MediaController.setMediaItems(mediaItems, index, C.TIME_UNSET)
                    ├─► MediaController.prepare()
                    ├─► MediaController.play()
                    ├─► MusicController.startPositionUpdater()
                    │     └─► Launches positionUpdateJob (polls position every 500ms)
                    └─► ExoPlayer (inside MusicService) starts audio output
```

### Flow B: Online Streaming Playback
```
SearchScreen.kt (Click play button on search result item)
  └─► OnlineSearchViewModel.streamSong(song)
        ├─► OnlineSearchViewModel._isLoadingStream.value = videoId
        ├─► InnertubeApi.getStreamUrl(videoId)
        │     └─► ServiceList.YouTube.getStreamExtractor(url)
        │           ├─► extractor.fetchPage()
        │           └─► extractor.audioStreams.maxByOrNull { it.getBitrate() }
        ├─► MusicController.playOnlineSong(songWithStreamUrl)
        │     └─► MediaController.setMediaItem(mediaItem) + prepare() + play()
        └─► OnlineSearchViewModel._isLoadingStream.value = null
```

---

## 2. Paged Search Query Call Graph

Maps how search operations traverse the repository and API layer:

```
SearchScreen.kt (User types query)
  └─► SearchScreen: onQueryChange(text)
        └─► OnlineSearchViewModel.onQueryChange(text)
              ├─► debounces input query for 500ms
              └─► OnlineSearchViewModel.searchResults (Flow pipeline)
                    └─► OnlineSearchRepository.search(query)
                          └─► Pager(pagingSourceFactory = { InnertubeSearchPagingSource })
                                └─► InnertubeSearchPagingSource.load(params)
                                      └─► InnertubeApi.search(query, continuationToken)
                                            ├─► StringEncryptionManager.baseUrl & ytmApiKey
                                            └─► OkHttpClient.newCall(request).execute()
                                                  └─► parseSearchResponse(body)
```

---

## 3. Background Download Call Graph

Maps the full execution pipeline when a track is downloaded:

```
SearchScreen.kt (Click download icon)
  └─► OnlineSearchViewModel.downloadSong(song)
        ├─► DownloadRepository.isDownloaded(song.videoId) (Check if exists)
        ├─► InnertubeApi.getStreamUrl(song.videoId) (Resolve direct link)
        ├─► DownloadRepository.startDownload(songWithStreamUrl)
        │     └─► WorkManager.enqueueUniqueWork("download_{id}", KEEP, request)
        │
    [System Schedules Worker Execution]
        │
        └─► DownloadWorker.doWork()
              ├─► DownloadWorker.setForeground(getForegroundInfo()) (Notification setup)
              ├─► PreferencesManager.downloadFolderUri.firstOrNull() (Read download path)
              ├─► DownloadWorker.downloadToStream() (HTTP Range GET requests in chunks)
              │     └─► OkHttpClient.newCall(req).execute() (Streams chunk bytes)
              ├─► FileOutputStream.write(buffer) (Saves temporary file)
              ├─► DownloadWorker.setProgress(workDataOf(progress)) (Posts progress)
              ├─► tempFile.renameTo(destFile) (Rename to .m4a once complete)
              └─► DownloadedSongDao.insert(DownloadedSongEntity) (Writes metadata)
```

---

## 4. Boot Security Verification Call Graph

Maps the security scan pipeline executed at startup:

```
MyPlayerApplication.onCreate()
  └─► SecurityManager.initialize()
        ├─► IntegrityManager.warmup()
        └─► SecurityManager.runAllChecks() (Launches on Dispatchers.IO)
              ├─► SignatureVerifier.verify()
              │     └─► SignatureVerifier.getSigningCertificateSHA256()
              ├─► RootDetectionManager.check()
              ├─► FridaDetectionManager.check()
              ├─► EmulatorDetectionManager.check()
              ├─► HookDetectionManager.check()
              ├─► SecurityNativeBridge.runAllNativeChecks()
              │     └─► native-lib.cpp: Java_..._nativeRunAllChecks()
              │           ├─► nativeCheckFridaPort()
              │           ├─► nativeCheckProcMapsForFrida()
              │           ├─► nativeCheckTracerPid()
              │           ├─► nativeCheckSuBinary()
              │           └─► nativeCheckPtrace()
              ├─► AntiDebugManager.check()
              ├─► TamperDetectionManager.check()
              └─► SecurityManager._securityStatus.value = status
```
