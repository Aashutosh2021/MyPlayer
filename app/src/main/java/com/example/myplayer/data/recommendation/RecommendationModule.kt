package com.example.myplayer.data.recommendation

import com.example.myplayer.data.recommendation.api.RecommendationSource
import com.example.myplayer.data.recommendation.api.YouTubeRecommendationSource
import com.example.myplayer.data.recommendation.strategy.DefaultRankingStrategy
import com.example.myplayer.data.recommendation.strategy.RecommendationStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecommendationModule {

    @Binds
    @Singleton
    abstract fun bindRecommendationSource(
        youTubeRecommendationSource: YouTubeRecommendationSource
    ): RecommendationSource

    @Binds
    @Singleton
    abstract fun bindRecommendationStrategy(
        smartRankingStrategy: com.example.myplayer.data.recommendation.strategy.SmartRankingStrategy
    ): RecommendationStrategy
    
    @Binds
    @Singleton
    abstract fun bindRecommendationQueuePolicy(
        defaultQueuePolicy: com.example.myplayer.data.recommendation.queue.DefaultQueuePolicy
    ): com.example.myplayer.data.recommendation.queue.RecommendationQueuePolicy
}
