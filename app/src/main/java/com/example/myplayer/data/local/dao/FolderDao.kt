package com.example.myplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>
    
    @Query("SELECT * FROM folders")
    fun getFoldersSync(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFolder(folder: FolderEntity): Unit

    @Delete
    fun deleteFolder(folder: FolderEntity): Unit
}
