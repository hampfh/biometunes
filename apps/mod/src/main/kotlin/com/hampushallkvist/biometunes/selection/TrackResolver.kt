package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackPool
import com.hampushallkvist.biometunes.environment.EnvironmentMode
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult

enum class SelectionSource {
    BOSS,
    SUBTERRANEAN,
    EXACT_BIOME,
    BIOME_TAG,
    DIMENSION,
    FALLBACK,
}

data class ResolvedTrack(
    val selection: SelectedAudio,
    val source: SelectionSource,
    val notificationKey: String,
)

data class ResolvedPool(
    val pool: TrackPool,
    val source: SelectionSource,
    val key: String,
    val notificationKey: String? = null,
)

class TrackResolver(
    private val selector: TrackPoolSelector = TrackPoolSelector(),
) {
    fun resolve(
        context: PlayerContext,
        catalog: TrackCatalog,
        bossMusicEnabled: Boolean,
    ): ResolvedTrack = select(resolvePool(context, catalog, bossMusicEnabled), catalog)

    fun resolvePool(
        context: PlayerContext,
        catalog: TrackCatalog,
        bossMusicEnabled: Boolean,
    ): ResolvedPool {
        if (bossMusicEnabled) {
            context.boss?.let { boss ->
                catalog.bosses[boss.catalogKey]?.let { pool ->
                    val notificationKey = "boss:${boss.catalogKey}"
                    return ResolvedPool(
                        pool,
                        SelectionSource.BOSS,
                        "biome:${context.biomeId}|$notificationKey",
                        notificationKey,
                    )
                }
            }
        }

        val classification = context.environmentalClassification
        if (classification is EnvironmentalClassificationResult.Classified &&
            classification.mode == EnvironmentMode.SUBTERRANEAN) {
            catalog.environmentalProfiles[context.dimensionId]?.let { profile ->
                return ResolvedPool(
                    pool = profile.subterraneanTracks,
                    source = SelectionSource.SUBTERRANEAN,
                    key = "environmental-profile:${context.dimensionId}|subterranean",
                )
            }
        }

        catalog.biomes[context.biomeId]?.let { pool ->
            return ResolvedPool(pool, SelectionSource.EXACT_BIOME, "biome:${context.biomeId}")
        }
        catalog.biomeTags.firstOrNull { (tag, _) -> tag in context.biomeTags }?.let { (tag, pool) ->
            return ResolvedPool(
                pool,
                SelectionSource.BIOME_TAG,
                "biome:${context.biomeId}|tag:$tag",
            )
        }
        catalog.dimensions[context.dimensionId]?.let { pool ->
            return ResolvedPool(
                pool,
                SelectionSource.DIMENSION,
                "biome:${context.biomeId}|dimension:${context.dimensionId}",
            )
        }
        return ResolvedPool(
            catalog.fallback,
            SelectionSource.FALLBACK,
            "biome:${context.biomeId}|fallback",
        )
    }

    fun select(
        resolvedPool: ResolvedPool,
        catalog: TrackCatalog,
    ): ResolvedTrack {
        val selection = selector.select(resolvedPool.pool, catalog.globalSilence, catalog.tracks)
        val notificationKey = resolvedPool.notificationKey ?: when (selection) {
            is SelectedTrack -> "track:${selection.track.id.value}"
            is SelectedSilence -> "silence"
        }
        return ResolvedTrack(
            selection,
            resolvedPool.source,
            notificationKey,
        )
    }
}
