# API Map - MyPlayer

Since MyPlayer does not have a custom backend, its "API Layer" consists of interactions with the YouTube Music Innertube API and the NewPipeExtractor library.

## 1. YouTube Music Innertube API
**Base URL**: `https://music.youtube.com/youtubei/v1`
**Auth**: Public Web Client Credentials (No API Key needed for basic search).

### Endpoints
| Method | Endpoint | Purpose | Input | Output |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/search` | Search for songs/artists | `query`, `continuationToken` | `SearchPage` (List of `OnlineSong`, `continuationToken`) |

### Client Contexts
The API requires specific context JSONs to masquerade as different clients:
- **WEB_REMIX**: Used for searching songs to match the web interface behavior.
- **ANDROID_VR**: Used during stream resolution to fetch direct audio URLs.

---

## 2. NewPipeExtractor Integration
NewPipeExtractor is used as a middleware to bypass YouTube's bot detection and resolve actual stream URLs.

### Key Flow: Stream Resolution
`InnertubeApi.getStreamUrl(videoId)` $\rightarrow$ `NewPipe.getStreamExtractor(url)` $\rightarrow$ `fetchPage()` $\rightarrow$ `audioStreams.maxBy { bitrate }` $\rightarrow$ `getContent()`

**Resolved Output**: A direct `.m4a` or `.webm` URL that can be passed directly to ExoPlayer.

---

## 3. Local API (Data Store)
The app uses `Preferences DataStore` for lightweight settings.
- **Key**: `DOWNLOAD_FOLDER_URI`
- **Purpose**: Stores the user-selected folder for downloading songs via Storage Access Framework (SAF).