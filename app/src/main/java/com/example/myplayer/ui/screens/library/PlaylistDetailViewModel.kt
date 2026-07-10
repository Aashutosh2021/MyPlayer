package com.example.myplayer.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.repository.HybridLibraryRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val hybridLibraryRepository: HybridLibraryRepository
) : ViewModel() {

    private val _playlistId = MutableStateFlow(-1L)

    val playlist: StateFlow<PlaylistEntity?> = _playlistId.flatMapLatest { id ->
        repository.getAllPlaylists().map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<PlayableSong>> = _playlistId.flatMapLatest { id ->
        repository.getSongsInPlaylist(id).map { entities ->
            entities.map { PlayableSong.Local(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All songs available in the user's library (local + downloaded) to pick from */
    val libraryForPicker: StateFlow<List<PlayableSong>> =
        hybridLibraryRepository.getHybridLibrary()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPlaylistId(id: Long) {
        _playlistId.value = id
    }

    fun removeSong(songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(_playlistId.value, songId)
        }
    }

    fun addSong(song: PlayableSong) {
        viewModelScope.launch {
            val currentCount = songs.value.size
            repository.addSongToPlaylist(_playlistId.value, song, currentCount)
        }
    }

    /** Add multiple songs at once */
    fun addSongs(songList: List<PlayableSong>) {
        viewModelScope.launch {
            val currentCount = songs.value.size
            songList.forEachIndexed { i, song ->
                repository.addSongToPlaylist(_playlistId.value, song, currentCount + i)
            }
        }
    }

    /** Save reordered list of song IDs */
    fun saveReorder(newOrderIds: List<String>) {
        viewModelScope.launch {
            repository.reorderSongsInPlaylist(_playlistId.value, newOrderIds)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: PlayableSong) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song, 0)
        }
    }
}
