# Database Map - MyPlayer

This map catalogs the schema definitions, table columns, indices, foreign keys, and SQL queries that form the persistent data layer of **MyPlayer**.

---

## 1. Database Specifications

* **Technology**: SQLite / Android Room Persistence Library.
* **Schema Version**: `2` (Updated from version 1).
* **Database File Name**: `myplayer_database` (SQLite format).
* **Schema Exporting**: Disabled (`exportSchema = false`).

---

## 2. Table Schemas & Entities

### Table: `songs`
Stores metadata for discovered local music files and cached online track references.
* **Columns**:
  * `id` (`TEXT`, Primary Key): File URI string or online URL identifier.
  * `title` (`TEXT`): Track name.
  * `artist` (`TEXT`): Performing artist.
  * `album` (`TEXT`): Album name.
  * `duration` (`INTEGER`): Length in milliseconds.
  * `path` (`TEXT`): File path or source URI.
  * `albumArt` (`TEXT`): Album artwork file path or URI.
  * `dateAdded` (`INTEGER`): Epoch timestamp.
  * `playCount` (`INTEGER`, Default `0`): Play counter.
* **DAO Reference**: `SongDao`

### Table: `downloaded_songs` (Added in v2)
Stores metadata for audio tracks downloaded from YouTube Music.
* **Columns**:
  * `id` (`TEXT`, Primary Key): YouTube video ID.
  * `title` (`TEXT`): Song title.
  * `artist` (`TEXT`): Performing artist.
  * `thumbnailUrl` (`TEXT`): Remote thumbnail URL.
  * `durationMs` (`INTEGER`): Duration in milliseconds.
  * `localPath` (`TEXT`): Full disk path to the downloaded `.m4a` file.
  * `fileSizeBytes` (`INTEGER`): Downloaded file size in bytes.
  * `downloadedAt` (`INTEGER`): Download completion timestamp.
* **DAO Reference**: `DownloadedSongDao`

### Table: `playlists`
Stores user-created custom playlists.
* **Columns**:
  * `id` (`INTEGER`, Primary Key, Autoincrement): Unique playlist identifier.
  * `name` (`TEXT`): Playlist name.
  * `createdAt` (`INTEGER`): Creation epoch timestamp.
* **DAO Reference**: `PlaylistDao`

### Table: `playlist_song_cross_ref`
Maps the Many-to-Many relationship between playlists and songs.
* **Columns**:
  * `playlistId` (`INTEGER`, Primary Key, Foreign Key): Links to `playlists.id`.
  * `songId` (`TEXT`, Primary Key, Foreign Key): Links to `songs.id`.
  * `position` (`INTEGER`): Ordering index of the song in the playlist.
* **Foreign Key Constraints**:
  * `playlistId` references `playlists.id` on delete CASCADE.
  * `songId` references `songs.id` on delete CASCADE.
* **DAO Reference**: `PlaylistDao`

### Table: `favorites`
Caches the list of songs marked as favorites by the user.
* **Columns**:
  * `songId` (`TEXT`, Primary Key, Foreign Key): Links to `songs.id`.
  * `addedAt` (`INTEGER`): Timestamp when added.
* **Foreign Key Constraints**:
  * `songId` references `songs.id` on delete CASCADE.
* **DAO Reference**: `FavoriteDao`

### Table: `folders`
Maintains the list of directories scanned for local audio.
* **Columns**:
  * `uri` (`TEXT`, Primary Key): Directory URI string (Storage Access Framework).
  * `name` (`TEXT`): Name of folder.
* **DAO Reference**: `FolderDao`

### Table: `recent_history`
Tracks the chronological listening log.
* **Columns**:
  * `songId` (`TEXT`, Primary Key, Foreign Key): Links to `songs.id`.
  * `playedAt` (`INTEGER`): Played timestamp.
* **Foreign Key Constraints**:
  * `songId` references `songs.id` on delete CASCADE.
* **DAO Reference**: `RecentHistoryDao`

### Table: `recent_searches` (Added in v2)
Stores search query strings typed by the user.
* **Columns**:
  * `query` (`TEXT`, Primary Key): The query string.
  * `timestamp` (`INTEGER`): The epoch timestamp.
* **DAO Reference**: `RecentSearchDao`

---

## 3. Database Migrations

### Migration `MIGRATION_1_2`
Executes SQL scripts to transition the SQLite database from version 1 to version 2, creating download cache and search history tables:
```sql
CREATE TABLE IF NOT EXISTS downloaded_songs (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    thumbnailUrl TEXT NOT NULL,
    durationMs INTEGER NOT NULL,
    localPath TEXT NOT NULL,
    fileSizeBytes INTEGER NOT NULL DEFAULT 0,
    downloadedAt INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS recent_searches (
    query TEXT NOT NULL PRIMARY KEY,
    timestamp INTEGER NOT NULL
);
```

---

## 4. Key DAO SQL Queries

* **Library Listing (`SongDao.kt`)**:
  ```sql
  SELECT * FROM songs ORDER BY title ASC
  ```
* **Folder Deletion Cascade (`SongDao.kt`)**:
  ```sql
  DELETE FROM songs WHERE path LIKE :folderUri || '%'
  ```
* **Most Played Tracks (`SongDao.kt`)**:
  ```sql
  SELECT * FROM songs ORDER BY playCount DESC LIMIT 50
  ```
* **Search Local Library (`SongDao.kt`)**:
  ```sql
  SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'
  ```
* **Playlist Song Retrieval (`PlaylistDao.kt`)**:
  ```sql
  SELECT s.* FROM songs s 
  INNER JOIN playlist_song_cross_ref ref ON s.id = ref.songId 
  WHERE ref.playlistId = :playlistId 
  ORDER BY ref.position ASC
  ```
* **Downloaded Checks (`DownloadedSongDao.kt`)**:
  ```sql
  SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE id = :id)
  ```
* **Recent Searches Trimming (`RecentSearchDao.kt`)**:
  Trims search history to maintain a capped size, retaining only the 20 most recent entries:
  ```sql
  DELETE FROM recent_searches WHERE query NOT IN (
      SELECT query FROM recent_searches ORDER BY timestamp DESC LIMIT 20
  )
  ```
