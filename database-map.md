# Database Map - MyPlayer

The application uses **Room (SQLite)** for persistent local storage. All data access is performed via specialized DAOs (Data Access Objects).

## 1. Entity Inventory

| Table Name | Purpose | Key Fields | Relationships |
| :--- | :--- | :--- | :--- |
| `SongEntity` | Core metadata for local audio files | `id`, `title`, `artist`, `album`, `duration`, `path`, `albumArt` | Parent to `FavoriteEntity`, `RecentHistoryEntity` |
| `DownloadedSongEntity` | Metadata for songs downloaded from web | `id` (videoId), `title`, `artist`, `localPath`, `thumbnailUrl` | Independent (Bridge between Online $\rightarrow$ Local) |
| `PlaylistEntity` | Metadata for user-created playlists | `id`, `name`, `createdAt` | Parent to `PlaylistSongCrossReference` |
| `PlaylistSongCrossReference` | Mapping for M:N playlist-song relation | `playlistId`, `songId`, `position` | Child of `PlaylistEntity` & `SongEntity` |
| `FavoriteEntity` | Tracks songs marked as favorites | `songId`, `addedAt` | Child of `SongEntity` |
| `FolderEntity` | Maps local folders to the library | `id`, `name`, `uri` | Parent to `SongEntity` (Logical grouping) |
| `RecentHistoryEntity` | Tracks playback history | `songId`, `playedAt` | Child of `SongEntity` |
| `RecentSearchEntity` | Stores recent search queries | `query` (PK), `timestamp` | Independent |

## 2. Entity Relationship Diagram (ERD)

### Core Relationships
- **Playlists $\leftrightarrow$ Songs**: Many-to-Many via `PlaylistSongCrossReference`.
- **Songs $\rightarrow$ Favorites**: One-to-One (or Zero) via `FavoriteEntity`.
- **Songs $\rightarrow$ History**: One-to-Many via `RecentHistoryEntity`.
- **Folders $\rightarrow$ Songs**: One-to-Many (Logical mapping of songs by directory path).

### Data Flow Relationship
`OnlineSong (API)` $\rightarrow$ `DownloadWorker` $\rightarrow$ `DownloadedSongEntity (Room)` $\rightarrow$ `PlayableSong.Downloaded (Domain Model)`.

## 3. DAO Layer Responsibilities
- `SongDao`: Complex queries for searching, sorting by play count, and retrieving recently added songs.
- `PlaylistDao`: Manages playlist lifecycle and song ordering within playlists.
- `FavoriteDao`: Quick checks for "isFavorite" status and listing all favorites.
- `FolderDao`: Syncs local directory structures into the database.
- `DownloadedSongDao`: Manages the downloaded songs library and checks for existing downloads to avoid duplicates.
- `RecentSearchDao`: Maintains a limited-size list of recent queries (using `trimOldEntries`).