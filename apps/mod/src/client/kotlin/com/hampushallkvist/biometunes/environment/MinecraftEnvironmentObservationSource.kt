package com.hampushallkvist.biometunes.environment

import net.minecraft.core.BlockPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.client.multiplayer.ClientLevel

class MinecraftEnvironmentObservationSource(
    private val level: ClientLevel,
) : EnvironmentalObservationSource {
    override fun isLoaded(point: BlockPoint): Boolean = level.hasChunkAt(point.x, point.z)

    override fun skyLight(point: BlockPoint): Int =
        level.getBrightness(LightLayer.SKY, point.toBlockPos())

    override fun blockLight(point: BlockPoint): Int =
        level.getBrightness(LightLayer.BLOCK, point.toBlockPos())

    override fun noLeavesSurfaceY(x: Int, z: Int): Int =
        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)

    override fun blocksMotion(point: BlockPoint): Boolean =
        level.getBlockState(point.toBlockPos()).blocksMotion()

    private fun BlockPoint.toBlockPos() = BlockPos(x, y, z)
}
