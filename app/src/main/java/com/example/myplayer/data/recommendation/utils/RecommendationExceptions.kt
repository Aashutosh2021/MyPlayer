package com.example.myplayer.data.recommendation.utils

open class RecommendationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class FetchRecommendationException(message: String, cause: Throwable? = null) : RecommendationException(message, cause)

class ValidationRecommendationException(message: String, cause: Throwable? = null) : RecommendationException(message, cause)

class MappingRecommendationException(message: String, cause: Throwable? = null) : RecommendationException(message, cause)
