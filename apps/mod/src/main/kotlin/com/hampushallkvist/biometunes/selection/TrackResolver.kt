package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId

enum class SelectionSource {
    BOSS,
    EXACT_BIOME,
    BIOME_TAG,
    DIMENSION,
    FALLBACK,
}

data class ResolvedTrack(
    val track: TrackDefinition,
    val source: SelectionSource,
    val notificationKey: String,
)

class TrackResolver {
    fun resolve(
        context: PlayerContext,
        catalog: TrackCatalog,
        bossMusicEnabled: Boolean,
    ): ResolvedTrack {
        if (bossMusicEnabled) {
            context.boss?.let { boss ->
                catalog.bosses[boss.catalogKey]?.let { id ->
                    return resolved(catalog, id, SelectionSource.BOSS, "boss:${boss.catalogKey}")
                }
            }
        }

        catalog.biomes[context.biomeId]?.let { id ->
            return resolved(catalog, id, SelectionSource.EXACT_BIOME, "track:${id.value}")
        }
        catalog.biomeTags.firstOrNull { (tag, _) -> tag in context.biomeTags }?.second?.let { id ->
            return resolved(catalog, id, SelectionSource.BIOME_TAG, "track:${id.value}")
        }
        catalog.dimensions[context.dimensionId]?.let { id ->
            return resolved(catalog, id, SelectionSource.DIMENSION, "track:${id.value}")
        }
        return resolved(catalog, catalog.fallback, SelectionSource.FALLBACK, "track:${catalog.fallback.value}")
    }

    private fun resolved(
        catalog: TrackCatalog,
        id: TrackId,
        source: SelectionSource,
        notificationKey: String,
    ) = ResolvedTrack(catalog.tracks.getValue(id), source, notificationKey)
}
