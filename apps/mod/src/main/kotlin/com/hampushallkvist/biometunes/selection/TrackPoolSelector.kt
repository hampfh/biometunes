package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.catalog.TrackPool
import com.hampushallkvist.biometunes.catalog.TrackPoolEntry
import com.hampushallkvist.biometunes.catalog.WeightedSilence
import com.hampushallkvist.biometunes.catalog.WeightedTrack
import kotlin.random.Random

fun interface BoundedRandom {
    fun nextLong(bound: Long): Long
}

sealed interface SelectedAudio

data class SelectedTrack(val track: TrackDefinition) : SelectedAudio

data class SelectedSilence(val durationSeconds: Int) : SelectedAudio

class TrackPoolSelector(
    private val random: BoundedRandom = BoundedRandom { bound -> Random.Default.nextLong(bound) },
) {
    fun select(
        pool: TrackPool,
        globalSilence: WeightedSilence,
        tracks: Map<TrackId, TrackDefinition>,
    ): SelectedAudio {
        val entries = if (pool.entries.any { it is WeightedSilence }) {
            pool.entries
        } else {
            pool.entries + globalSilence
        }
        val roll = random.nextLong(entries.sumOf { it.weight.toLong() })
        var threshold = 0L
        val selected = entries.first { entry ->
            threshold += entry.weight
            roll < threshold
        }
        return selected.resolve(tracks)
    }

    private fun TrackPoolEntry.resolve(tracks: Map<TrackId, TrackDefinition>): SelectedAudio =
        when (this) {
            is WeightedTrack -> SelectedTrack(tracks.getValue(track))
            is WeightedSilence -> {
                val minimum = durationSeconds.min.toLong()
                val rangeSize = durationSeconds.max.toLong() - minimum + 1L
                SelectedSilence((minimum + random.nextLong(rangeSize)).toInt())
            }
        }
}
