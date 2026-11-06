package com.myplayer.app.data.local.dao

import androidx.room.*
import com.myplayer.app.data.local.entity.CachedLyricsEntity

@Dao
interface CachedLyricsDao {
    @Query("SELECT * FROM cached_lyrics WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSong(songId: String): CachedLyricsEntity?

    /**
     * Matches cached lyrics by exact songId OR by case-insensitive Track Name + Artist Name
     * so offline/downloaded songs always find their cached lyrics even if songId format differs!
     */
    @Query("""
        SELECT * FROM cached_lyrics 
        WHERE songId = :songId 
           OR (LOWER(TRIM(trackName)) = LOWER(TRIM(:trackName)) AND LOWER(TRIM(artistName)) = LOWER(TRIM(:artistName)))
           OR (LOWER(TRIM(trackName)) = LOWER(TRIM(:trackName)) AND :trackName != '')
        ORDER BY 
            CASE 
                WHEN songId = :songId THEN 0 
                WHEN LOWER(TRIM(artistName)) = LOWER(TRIM(:artistName)) THEN 1 
                ELSE 2 
            END
        LIMIT 1
    """)
    suspend fun getLyricsForSongOrMetadata(
        songId: String,
        trackName: String,
        artistName: String
    ): CachedLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: CachedLyricsEntity)

    @Query("DELETE FROM cached_lyrics WHERE songId = :songId")
    suspend fun deleteLyricsForSong(songId: String)

    @Query("SELECT * FROM cached_lyrics")
    fun getAllCachedLyricsSync(): List<CachedLyricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCachedLyrics(lyrics: List<CachedLyricsEntity>)
}
