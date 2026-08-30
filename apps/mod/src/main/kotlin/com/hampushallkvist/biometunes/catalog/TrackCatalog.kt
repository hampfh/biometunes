package com.hampushallkvist.biometunes.catalog

import com.hampushallkvist.biometunes.environment.EnvironmentalProfile

@JvmInline
value class TrackId(val value: String)

data class TrackDefinition(
    val id: TrackId,
    val soundEvent: String,
    val title: String,
    val artist: String,
)

data class SilenceDurationRange(
    val min: Int,
    val max: Int,
)

sealed interface TrackPoolEntry {
    val weight: Int
}

data class WeightedTrack(
    val track: TrackId,
    override val weight: Int,
) : TrackPoolEntry

data class WeightedSilence(
    override val weight: Int,
    val durationSeconds: SilenceDurationRange,
) : TrackPoolEntry

data class TrackPool(val entries: List<TrackPoolEntry>)

data class TrackCatalog(
    val tracks: Map<TrackId, TrackDefinition>,
    val globalSilence: WeightedSilence,
    val bosses: Map<String, TrackPool>,
    val biomes: Map<String, TrackPool>,
    val biomeTags: List<Pair<String, TrackPool>>,
    val dimensions: Map<String, TrackPool>,
    val environmentalProfiles: Map<String, EnvironmentalProfile>,
    val fallback: TrackPool,
)
