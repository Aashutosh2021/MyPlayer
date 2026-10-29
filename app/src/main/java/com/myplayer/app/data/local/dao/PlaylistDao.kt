package com.myplayer.app.data.local.dao

import androidx.room.*
import com.myplayer.app.data.local.entity.PlaylistEntity
import com.myplayer.app.data.local.entity.PlaylistSongCrossReference
import com.myplayer.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    fun deletePlaylist(playlist: PlaylistEntity): Unit

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    fun renamePlaylist(playlistId: Long, newName: String): Unit

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSongToPlaylist(crossRef: PlaylistSongCrossReference): Unit

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    fun removeSongFromPlaylist(playlistId: Long, songId: String): Unit

    @Query("SELECT songs.* FROM songs \n        INNER JOIN playlist_songs ON songs.id = playlist_songs.songId \n        WHERE playlist_songs.playlistId = :playlistId \n        ORDER BY playlist_songs.position ASC\n    ")
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: String, position: Int)

    @Query("SELECT * FROM playlist_songs")
    fun getAllPlaylistSongsSync(): List<PlaylistSongCrossReference>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylistSongs(songs: List<PlaylistSongCrossReference>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylists(playlists: List<PlaylistEntity>)
}
