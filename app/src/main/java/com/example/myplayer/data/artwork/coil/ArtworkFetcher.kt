package com.example.myplayer.data.artwork.coil

import android.net.Uri
import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.example.myplayer.data.artwork.ArtworkRepository
import com.example.myplayer.data.artwork.model.ArtworkModel
import javax.inject.Provider

/**
 * Custom Coil Fetcher that seamlessly resolves [ArtworkModel] requests using [ArtworkRepository].
 */
class ArtworkFetcher(
    private val data: ArtworkModel,
    private val options: Options,
    private val imageLoader: ImageLoader,
    private val artworkRepository: ArtworkRepository
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val result = artworkRepository.getArtwork(
            title = data.title,
            artist = data.artist,
            album = data.album,
            localArtworkUri = data.localUri
        )

        val targetUrl = result?.url ?: data.localUri
        if (targetUrl.isNullOrBlank()) return null

        val uri = Uri.parse(targetUrl)
        val (delegateFetcher) = imageLoader.components.newFetcher(uri, options, imageLoader) ?: return null
        return delegateFetcher.fetch()
    }

    class Factory(
        private val artworkRepositoryProvider: Provider<ArtworkRepository>
    ) : Fetcher.Factory<ArtworkModel> {
        override fun create(data: ArtworkModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return ArtworkFetcher(
                data = data,
                options = options,
                imageLoader = imageLoader,
                artworkRepository = artworkRepositoryProvider.get()
            )
        }
    }
}
