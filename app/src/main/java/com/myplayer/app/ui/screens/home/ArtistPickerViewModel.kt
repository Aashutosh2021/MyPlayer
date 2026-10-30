package com.myplayer.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myplayer.app.data.artist.ArtistInfo
import com.myplayer.app.data.artist.ArtistSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

sealed interface ArtistSearchState {
    data object Idle : ArtistSearchState
    data object Loading : ArtistSearchState
    data class Found(val artists: List<ArtistInfo>) : ArtistSearchState
    data object NotFound : ArtistSearchState
    data object Offline : ArtistSearchState
}

@HiltViewModel
class ArtistPickerViewModel @Inject constructor(
    private val repository: ArtistSearchRepository
) : ViewModel() {

    private val _results = MutableStateFlow<ArtistSearchState>(ArtistSearchState.Idle)
    val results: StateFlow<ArtistSearchState> = _results.asStateFlow()

    private val _photos = MutableStateFlow<Map<String, String>>(emptyMap())
    val photos: StateFlow<Map<String, String>> = _photos.asStateFlow()

    private var searchJob: Job? = null
    private val requested = HashSet<String>()
    private val gate = Semaphore(4)

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _results.value = ArtistSearchState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _results.value = ArtistSearchState.Loading
            val found = repository.search(q)
            _results.value = when {
                found == null -> ArtistSearchState.Offline
                found.isEmpty() -> ArtistSearchState.NotFound
                else -> {
                    _photos.update { old -> old + found.mapNotNull { a -> a.imageUrl?.let { a.name to it } } }
                    ArtistSearchState.Found(found)
                }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _results.value = ArtistSearchState.Idle
    }

    /** Fills in photos for the artist grid; each name is looked up once. */
    fun loadPhotos(names: Collection<String>) {
        names.filter { it !in _photos.value && requested.add(it) }.forEach { name ->
            viewModelScope.launch {
                gate.withPermit { repository.photoFor(name) }?.let { url -> _photos.update { it + (name to url) } }
            }
        }
    }
}
