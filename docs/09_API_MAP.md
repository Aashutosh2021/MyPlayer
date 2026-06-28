# API Map - MyPlayer

This map catalogs the remote web interfaces, payload structures, stream extraction APIs, and local key stores used by the **MyPlayer** application.

---

## 1. Remote API: YouTube Music Innertube

MyPlayer utilizes YouTube's private/public **Innertube API** (the core API backing YouTube, YouTube Music, and Android/TV clients).

### General Specifications
* **Base URL**: `https://music.youtube.com/youtubei/v1` (Decrypted at runtime via `StringEncryptionManager`).
* **Authentication**: None required. Uses public client parameters and credentials.
* **Format**: JSON over HTTP POST.

---

### Endpoint: Search Music
* **Path**: `/search?key={API_KEY}`
* **Method**: `POST`
* **Headers**:
  * `User-Agent`: `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36`
  * `Content-Type`: `application/json`
  * `Origin`: `https://music.youtube.com`
  * `Referer`: `https://music.youtube.com/`
* **Client Context**: Uses the `WEB_REMIX` client name and version parameters.

#### Request JSON Payload Structure (First Page)
```json
{
  "context": {
    "client": {
      "clientName": "WEB_REMIX",
      "clientVersion": "1.20241111.01.00",
      "hl": "en",
      "gl": "US"
    }
  },
  "query": "classical music",
  "params": "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
}
```
> [!NOTE]
> The `params` filter string `"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"` filters search results to **Songs Only**, suppressing standard video uploads and playlists.

#### Request JSON Payload Structure (Pagination Pages)
```json
{
  "context": {
    "client": {
      "clientName": "WEB_REMIX",
      "clientVersion": "1.20241111.01.00",
      "hl": "en",
      "gl": "US"
    }
  },
  "continuation": "y4GtsS45g..."
}
```

#### Response Parsing Map
The manual parser `parseSearchResponse()` in `InnertubeApi.kt` extracts data from the nested JSON tree:
1. **Continuation Token**:
   * Path (First Page): `contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.continuations[0].nextContinuationData.continuation`
   * Path (Continuation Page): `continuationContents.musicShelfContinuation.continuations[0].nextContinuationData.continuation`
2. **Music Shelf Content Nodes**:
   * Path: `contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.contents` -> Filter for `musicShelfRenderer.contents` array.
3. **Item Parsing (`parseMusicItem()`)**:
   * **Video ID**: `overlay.musicItemThumbnailOverlayRenderer.content.musicPlayButtonRenderer.playNavigationEndpoint.watchEndpoint.videoId`
   * **Title**: `flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text.runs[0].text`
   * **Artist**: `flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text.runs[0].text`
   * **Duration**: Scans all elements in the second flex column runs list for a colon (`:`) separator.
   * **Thumbnail URL**: `thumbnail.musicThumbnailRenderer.thumbnail.thumbnails[last].url` -> Resizes `w60-h60` to `w226-h226` for high resolution.

---

## 2. Stream URL Extraction API: NewPipeExtractor

To resolve raw audio file streams, MyPlayer integrates the open-source **NewPipeExtractor** Java library.

### Key Resolution Flow
1. Construct YouTube watch Uri: `"https://www.youtube.com/watch?v=" + videoId`.
2. Retrieve Stream Extractor: `ServiceList.YouTube.getStreamExtractor(url)`.
3. Fetch extractor page structure: `extractor.fetchPage()`.
4. Parse audio streams: `extractor.audioStreams`.
5. Identify highest quality track: `audioStreams.maxByOrNull { it.getBitrate() }`.
6. Extract streaming link: `bestStream.getContent()`.

### Bypassing Bot Detection
YouTube enforces bot-detection (e.g. signature decryption, poToken). NewPipeExtractor provides an internal decrypter layer (`NewPipeDownloader.kt`) which handles standard client parameters:
* Client Context: Masquerades as a Android VR client (`ANDROID_VR`, clientVersion `"1.65.10"`) during stream URL negotiation to fetch direct deciphered streaming URLs.

---

## 3. Local API: Settings DataStore

Lightweight app settings are persisted using Preferences DataStore.

* **Database File**: `myplayer_settings.preferences_pb`
* **Key Name**: `download_folder_uri`
* **Type**: `String`
* **Purpose**: Stores the Storage Access Framework (SAF) folder URI representing the directory chosen by the user for music downloads.
