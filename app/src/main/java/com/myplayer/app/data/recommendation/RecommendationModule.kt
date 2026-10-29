package com.myplayer.app.data.recommendation

import com.myplayer.app.data.recommendation.api.RecommendationSource
import com.myplayer.app.data.recommendation.api.YouTubeRecommendationSource
import com.myplayer.app.data.recommendation.strategy.DefaultRankingStrategy
import com.myplayer.app.data.recommendation.strategy.RecommendationStrategy
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
        router: com.myplayer.app.data.recommendation.api.RecommendationSourceRouter
    ): RecommendationSource

    @Binds
    @Singleton
    abstract fun bindRecommendationStrategy(
        smartRankingStrategy: com.myplayer.app.data.recommendation.strategy.SmartRankingStrategy
    ): RecommendationStrategy
    
    @Binds
    @Singleton
    abstract fun bindRecommendationQueuePolicy(
        defaultQueuePolicy: com.myplayer.app.data.recommendation.queue.DefaultQueuePolicy
    ): com.myplayer.app.data.recommendation.queue.RecommendationQueuePolicy
}
