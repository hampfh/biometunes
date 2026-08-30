package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.catalog.SilenceDurationRange
import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.catalog.TrackPool
import com.hampushallkvist.biometunes.catalog.WeightedSilence
import com.hampushallkvist.biometunes.catalog.WeightedTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackPoolSelectorTest {
    @Test
    fun `inline silence replaces global silence for both weight and duration`() {
        val observedBounds = mutableListOf<Long>()
        val rolls = ArrayDeque(listOf(4L, 5L))
        val selector = TrackPoolSelector(BoundedRandom { bound ->
            observedBounds += bound
            rolls.removeFirst()
        })
        val pool = TrackPool(
            listOf(
                WeightedTrack(forest.id, weight = 4),
                WeightedSilence(2, SilenceDurationRange(10, 20)),
            ),
        )

        val selected = selector.select(
            pool,
            globalSilence = WeightedSilence(100, SilenceDurationRange(30, 120)),
            tracks = mapOf(forest.id to forest),
        )

        assertEquals(SelectedSilence(durationSeconds = 15), selected)
        assertEquals(listOf(6L, 11L), observedBounds)
    }

    @Test
    fun `weighted boundaries select songs and global silence with inclusive duration`() {
        val pool = TrackPool(
            listOf(
                WeightedTrack(forest.id, weight = 2),
                WeightedTrack(desert.id, weight = 3),
            ),
        )
        val globalSilence = WeightedSilence(1, SilenceDurationRange(30, 120))
        val tracks = listOf(forest, desert).associateBy(TrackDefinition::id)

        assertEquals(SelectedTrack(forest), selector(0).select(pool, globalSilence, tracks))
        assertEquals(SelectedTrack(forest), selector(1).select(pool, globalSilence, tracks))
        assertEquals(SelectedTrack(desert), selector(2).select(pool, globalSilence, tracks))
        assertEquals(SelectedTrack(desert), selector(4).select(pool, globalSilence, tracks))
        assertEquals(
            SelectedSilence(durationSeconds = 120),
            selector(5, 90).select(pool, globalSilence, tracks),
        )
    }

    private fun selector(vararg values: Long): TrackPoolSelector {
        val remaining = ArrayDeque(values.toList())
        return TrackPoolSelector(BoundedRandom { bound ->
            remaining.removeFirst().also { require(it in 0 until bound) }
        })
    }

    private companion object {
        val forest = track("forest")
        val desert = track("desert")

        fun track(id: String) = TrackDefinition(
            id = TrackId(id),
            soundEvent = "biometunes:music.$id",
            title = id,
            artist = "Test Artist",
        )
    }
}
