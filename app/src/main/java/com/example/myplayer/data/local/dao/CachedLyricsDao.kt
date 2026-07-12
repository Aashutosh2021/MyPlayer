package com.example.myplayer.data.local.dao

import androidx.room.*
import com.example.myplayer.data.local.entity.CachedLyricsEntity

@Dao
interface CachedLyricsDao {
    @Query("SELECT * FROM cached_lyrics WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSong(songId: String): CachedLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: CachedLyricsEntity)

    @Query("DELETE FROM cached_lyrics WHERE songId = :songId")
    suspend fun deleteLyricsForSong(songId: String)

    @Query("SELECT * FROM cached_lyrics")
    fun getAllCachedLyricsSync(): List<CachedLyricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCachedLyrics(lyrics: List<CachedLyricsEntity>)
}
