package com.example.myplayer.data.repository

import android.content.Context
import android.util.Log
import com.example.myplayer.data.local.dao.FolderDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.FolderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaScanner previously scanned local device storage for music files.
 * Per requirement, all local song scanning logic has been removed;
 * music is sourced from online search and saved exclusively in the downloaded section.
 */
@Singleton
class MediaScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val folderDao: FolderDao
) {
    suspend fun scanAllFolders() = withContext(Dispatchers.IO) {
        Log.d("MediaScanner", "Local folder scanning is disabled. Music is sourced exclusively online and via downloads.")
    }

    suspend fun scanFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        Log.d("MediaScanner", "Local folder scanning is disabled for ${folder.uri}. Music is sourced exclusively online and via downloads.")
    }
}
