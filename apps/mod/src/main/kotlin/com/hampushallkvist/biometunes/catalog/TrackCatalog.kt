package com.hampushallkvist.biometunes.catalog

@JvmInline
value class TrackId(val value: String)

data class TrackDefinition(
    val id: TrackId,
    val soundEvent: String,
    val title: String,
    val artist: String,
)

data class TrackCatalog(
    val tracks: Map<TrackId, TrackDefinition>,
    val bosses: Map<String, TrackId>,
    val biomes: Map<String, TrackId>,
    val biomeTags: List<Pair<String, TrackId>>,
    val dimensions: Map<String, TrackId>,
    val fallback: TrackId,
)
