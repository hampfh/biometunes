package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.selection.BossEncounter
import com.hampushallkvist.biometunes.selection.PlayerContext
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss

class MinecraftContextSampler(private val client: Minecraft) {
    fun sample(): PlayerContext? {
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

        return PlayerContext(
            biomeId = biomeId,
            biomeTags = biomeTags,
            dimensionId = level.dimension().identifier().toString(),
            boss = boss,
        )
    }
}
