package com.hampushallkvist.biometunes.environment

data class BlockPoint(
    val x: Int,
    val y: Int,
    val z: Int,
)

interface EnvironmentalObservationSource {
    fun isLoaded(point: BlockPoint): Boolean

    fun skyLight(point: BlockPoint): Int

    fun blockLight(point: BlockPoint): Int

    fun noLeavesSurfaceY(x: Int, z: Int): Int

    fun blocksMotion(point: BlockPoint): Boolean
}

data class EnvironmentalEvidence(
    val centerSkyExposed: Boolean,
    val skyExposedSamples: Int,
    val skySampleCount: Int,
    val medianSurfaceDepth: Double,
    val enclosure: Double,
    val skyLight: Int,
    val blockLight: Int,
)
