package com.example.myplayer.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.myplayer.data.local.dao.FolderDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.FolderEntity
import com.example.myplayer.data.local.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val folderDao: FolderDao
) {
    private val supportedExtensions = listOf("mp3", "m4a", "wav", "flac", "ogg")

    suspend fun scanAllFolders() = withContext(Dispatchers.IO) {
        val folders = folderDao.getFoldersSync()
        for (folder in folders) {
            scanFolder(folder)
        }
    }

    suspend fun scanFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        val folderUri = Uri.parse(folder.uri)
        val documentFile = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext
        
        if (!documentFile.exists() || !documentFile.isDirectory) {
            return@withContext
        }

        val songs = mutableListOf<SongEntity>()
        val retriever = MediaMetadataRetriever()

        // List files in the directory. 
        // Note: as per requirement, we do not scan sub-folders.
        val files = documentFile.listFiles()
        
        for (file in files) {
            if (file.isDirectory) continue

            val fileName = file.name ?: continue
            val extension = fileName.substringAfterLast('.', "").lowercase()

            // Skip unsupported extensions or explicitly ignored extensions like "aac"
            if (extension !in supportedExtensions || extension == "aac") {
                continue
            }

            try {
                retriever.setDataSource(context, file.uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: fileName.substringBeforeLast('.')
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L

                val albumArt = try {
                    // Just check if there is an embedded picture. We won't save the byte array to DB directly,
                    // we'll use Coil to load the art directly from the URI later, but we can flag if it has art.
                    // Actually, saving the URI as a string is the best way, and Coil handles reading the embedded art.
                    file.uri.toString()
                } catch (e: Exception) {
                    null
                }

                val song = SongEntity(
                    id = file.uri.toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    path = file.uri.toString(),
                    albumArt = albumArt, // URI string is used by Coil to load the art later
                    dateAdded = System.currentTimeMillis()
                )
                songs.add(song)

            } catch (e: Exception) {
                Log.e("MediaScanner", "Failed to extract metadata for \${file.uri}", e)
            }
        }
        
        try {
            retriever.release()
        } catch (e: Exception) {
            // Ignore release exceptions
        }

        // Replace existing songs for this folder or insert new ones
        if (songs.isNotEmpty()) {
            songDao.insertSongs(songs)
        }
    }
}
