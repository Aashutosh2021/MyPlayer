package com.example.myplayer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.myplayer.data.local.dao.RecentSearchDao
import com.example.myplayer.data.local.entity.RecentSearchEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineSearchRepository @Inject constructor(
    private val innertubeApi: InnertubeApi,
    private val recentSearchDao: RecentSearchDao
) {
    fun search(query: String, broadSearch: Boolean = false): Flow<PagingData<OnlineSong>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { InnertubeSearchPagingSource(innertubeApi, query, broadSearch) }
        ).flow
    }

    fun getRecentSearches(): Flow<List<RecentSearchEntity>> = recentSearchDao.getRecentSearches()

    suspend fun saveSearch(query: String) {
        if (query.isBlank()) return
        recentSearchDao.insert(RecentSearchEntity(query = query.trim()))
        recentSearchDao.trimOldEntries()
    }

    suspend fun deleteSearch(query: String) = recentSearchDao.delete(query)

    suspend fun clearAllSearches() = recentSearchDao.clearAll()
}

class InnertubeSearchPagingSource(
    private val innertubeApi: InnertubeApi,
    private val query: String,
    private val broadSearch: Boolean = false
) : PagingSource<String, OnlineSong>() {

    private val seenVideoIds = mutableSetOf<String>()

    override fun getRefreshKey(state: PagingState<String, OnlineSong>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, OnlineSong> {
        return try {
            val key = params.key
            if (key == null) {
                // First page load:
                val (songs, nextKey) = if (broadSearch) {
                    val page = innertubeApi.search(query, continuationToken = null, filtered = false)
                    val distinct = page.songs.filter { seenVideoIds.add(it.videoId) }
                    distinct to page.continuationToken?.let { "broadened:$it" }
                } else {
                    val filteredPage = innertubeApi.search(query, continuationToken = null, filtered = true)
                    val filteredDistinct = filteredPage.songs.filter { seenVideoIds.add(it.videoId) }

                    // If filtered search returns fewer than 8 items, broaden search to surface older/alternate versions
                    if (filteredDistinct.size < 8) {
                        val broadenedPage = innertubeApi.search(query, continuationToken = null, filtered = false)
                        val broadenedDistinct = broadenedPage.songs.filter { seenVideoIds.add(it.videoId) }
                        val combined = filteredDistinct + broadenedDistinct
                        val next = broadenedPage.continuationToken?.let { "broadened:$it" }
                            ?: filteredPage.continuationToken?.let { "filtered:$it" }
                        combined to next
                    } else {
                        filteredDistinct to filteredPage.continuationToken?.let { "filtered:$it" }
                    }
                }

                // Prefetch first few visible search results so tapping starts immediately
                val topVisibleIds = songs.take(4).map { it.videoId }
                if (topVisibleIds.isNotEmpty()) {
                    innertubeApi.prefetchVisibleStreams(topVisibleIds)
                }

                LoadResult.Page(
                    data = songs,
                    prevKey = null,
                    nextKey = nextKey
                )
            } else {
                // Subsequent page loads:
                val isBroad = key.startsWith("broadened:") || broadSearch
                val cleanToken = key.removePrefix("broadened:").removePrefix("filtered:")
                val page = innertubeApi.search(query, continuationToken = cleanToken, filtered = !isBroad)
                val distinct = page.songs.filter { seenVideoIds.add(it.videoId) }
                val nextPrefix = if (isBroad) "broadened:" else "filtered:"

                LoadResult.Page(
                    data = distinct,
                    prevKey = null,
                    nextKey = page.continuationToken?.let { "$nextPrefix$it" }
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
