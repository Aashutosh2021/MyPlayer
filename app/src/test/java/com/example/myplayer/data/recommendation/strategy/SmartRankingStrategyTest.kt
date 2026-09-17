package com.example.myplayer.data.recommendation.strategy

import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmartRankingStrategyTest {

    private lateinit var strategy: SmartRankingStrategy

    @Before
    fun setup() {
        strategy = SmartRankingStrategy()
    }

    @Test
    fun scoreAndRank_awardsSameArtistBonus() {
        val seed = RecommendationSeed(
            songId = "seed1",
            artist = "Radiohead",
            title = "Creep",
            genre = "alternative rock"
        )

        val candidates = listOf(
            RecommendationSong(
                videoId = "c1",
                title = "Karma Police",
                artist = "Radiohead",
                durationMs = 260_000L,
                thumbnailUrl = "",
                popularityScore = 40
            ),
            RecommendationSong(
                videoId = "c2",
                title = "Yellow",
                artist = "Coldplay",
                durationMs = 260_000L,
                thumbnailUrl = "",
                popularityScore = 40
            )
        )

        val ranked = strategy.scoreAndRank(seed, candidates)
        assertEquals("c1", ranked[0].videoId)
        assertTrue(ranked[0].recommendationScore > ranked[1].recommendationScore)
        assertTrue(ranked[0].reason.contains("Same Artist"))
    }

    @Test
    fun scoreAndRank_awardsGenreNeighborExpansion() {
        val seed = RecommendationSeed(
            songId = "seed1",
            artist = "Arctic Monkeys",
            title = "Do I Wanna Know",
            genre = "indie rock"
        )

        val neighborSong = RecommendationSong(
            videoId = "c_neighbor",
            title = "Song 1",
            artist = "Artist A",
            durationMs = 200_000L,
            thumbnailUrl = "",
            popularityScore = 0,
            metadata = mapOf("genre" to "alternative rock")
        )

        val unrelatedSong = RecommendationSong(
            videoId = "c_unrelated",
            title = "Song 2",
            artist = "Artist B",
            durationMs = 200_000L,
            thumbnailUrl = "",
            popularityScore = 0,
            metadata = mapOf("genre" to "classical")
        )

        val ranked = strategy.scoreAndRank(seed, listOf(unrelatedSong, neighborSong))
        assertEquals("c_neighbor", ranked[0].videoId)
        assertTrue(ranked[0].recommendationScore > ranked[1].recommendationScore)
        assertTrue(ranked[0].reason.contains("Genre Neighbor"))
    }

    @Test
    fun calculateGemScore_awardsPeakBonusForIndieSweetSpot() {
        assertEquals(9, strategy.calculateGemScore(30)) // Peak sweet spot
        assertEquals(7, strategy.calculateGemScore(15)) // Emerging indie
        assertEquals(5, strategy.calculateGemScore(65)) // Established
        assertEquals(-2, strategy.calculateGemScore(95)) // Mainstream saturation
    }

    @Test
    fun scoreAndRank_enforcesArtistDiversityCap() {
        val seed = RecommendationSeed(
            songId = "seed1",
            artist = "The Beatles",
            title = "Help"
        )

        // 4 songs by same artist + 1 by another artist
        val candidates = (1..4).map { i ->
            RecommendationSong(
                videoId = "beatles_$i",
                title = "Beatles Song $i",
                artist = "The Beatles",
                durationMs = 150_000L,
                thumbnailUrl = "",
                popularityScore = 40
            )
        } + listOf(
            RecommendationSong(
                videoId = "other_1",
                title = "Other Song",
                artist = "The Rolling Stones",
                durationMs = 150_000L,
                thumbnailUrl = "",
                popularityScore = 10
            )
        )

        val ranked = strategy.scoreAndRank(seed, candidates)
        
        // The top 2 songs can be The Beatles, but the 3rd should be diversified before deferred Beatles songs
        assertEquals("The Beatles", ranked[0].artist)
        assertEquals("The Beatles", ranked[1].artist)
        assertEquals("The Rolling Stones", ranked[2].artist)
    }
}
