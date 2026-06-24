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
    fun search(query: String): Flow<PagingData<OnlineSong>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { InnertubeSearchPagingSource(innertubeApi, query) }
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
    private val query: String
) : PagingSource<String, OnlineSong>() {

    override fun getRefreshKey(state: PagingState<String, OnlineSong>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, OnlineSong> {
        return try {
            val page = innertubeApi.search(query, continuationToken = params.key)
            LoadResult.Page(
                data = page.songs,
                prevKey = null,
                nextKey = page.continuationToken
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
