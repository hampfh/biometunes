package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.environment.BlockPoint
import com.hampushallkvist.biometunes.environment.EnvironmentalClassification
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationInput
import com.hampushallkvist.biometunes.environment.MinecraftEnvironmentObservationSource
import com.hampushallkvist.biometunes.selection.BossEncounter
import com.hampushallkvist.biometunes.selection.PlayerContext
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss

class MinecraftContextSampler(
    private val client: Minecraft,
    private val environmentalClassification: EnvironmentalClassification = EnvironmentalClassification(),
) {
    fun sample(catalog: TrackCatalog): PlayerContext? {
        val level = client.level ?: return null
        val player = client.player ?: return null
        val biome = level.getBiome(player.blockPosition())
        val biomeId = biome.unwrapKey().orElse(null)?.identifier()?.toString() ?: return null
        val biomeTags = biome.tags().map { it.location().toString() }.toList().toSet()
        val entities = level.entitiesForRendering().filter { it.isAlive }
        val boss = when {
            entities.any { it is EnderDragon } -> BossEncounter.ENDER_DRAGON
            entities.any { it is WitherBoss } -> BossEncounter.WITHER
            else -> null
        }

        val dimensionId = level.dimension().identifier().toString()
        val classification = environmentalClassification.classify(
            input = EnvironmentalClassificationInput(
                dimensionId = dimensionId,
                biomeId = biomeId,
                bossEncounterActive = boss != null,
                profile = catalog.environmentalProfiles[dimensionId],
                origin = BlockPoint(
                    x = player.blockX,
                    y = kotlin.math.floor(player.eyeY).toInt(),
                    z = player.blockZ,
                ),
                elapsedSeconds = ENVIRONMENT_SAMPLE_INTERVAL_SECONDS,
            ),
            source = MinecraftEnvironmentObservationSource(level),
        )

        return PlayerContext(
            biomeId = biomeId,
            biomeTags = biomeTags,
            dimensionId = dimensionId,
            boss = boss,
            environmentalClassification = classification,
        )
    }

    fun resetEnvironmentalClassification() = environmentalClassification.reset()

    private companion object {
        const val ENVIRONMENT_SAMPLE_INTERVAL_SECONDS = 0.5
    }
}
