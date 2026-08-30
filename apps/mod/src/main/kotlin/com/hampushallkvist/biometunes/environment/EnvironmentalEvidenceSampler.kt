package com.hampushallkvist.biometunes.environment

import kotlin.math.max

object EnvironmentalEvidenceSampler {
    private const val MINIMUM_SKY_SAMPLES = 5
    private const val MAX_LIGHT = 15

    fun sample(
        origin: BlockPoint,
        settings: EnvironmentalSampling,
        source: EnvironmentalObservationSource,
    ): EnvironmentalEvidence? {
        val skyObservations = skyPoints(origin, settings.nearbyRadius)
            .filter(source::isLoaded)
            .map { point ->
                val skyLight = source.skyLight(point).coerceIn(0, MAX_LIGHT)
                val surfaceY = source.noLeavesSurfaceY(point.x, point.z)
                SkyObservation(
                    point = point,
                    exposed = skyLight == MAX_LIGHT || point.y >= surfaceY,
                    surfaceDepth = max(0, surfaceY - point.y),
                )
            }

        if (skyObservations.size < MINIMUM_SKY_SAMPLES) return null
        val center = skyObservations.firstOrNull { it.point == origin } ?: return null

        val enclosureObservations = enclosurePoints(origin, settings.enclosureRadius)
            .filter(source::isLoaded)
            .map(source::blocksMotion)
        val enclosure = if (enclosureObservations.isEmpty()) {
            0.0
        } else {
            enclosureObservations.count { it }.toDouble() / enclosureObservations.size
        }

        return EnvironmentalEvidence(
            centerSkyExposed = center.exposed,
            skyExposedSamples = skyObservations.count(SkyObservation::exposed),
            skySampleCount = skyObservations.size,
            medianSurfaceDepth = median(skyObservations.map(SkyObservation::surfaceDepth)),
            enclosure = enclosure,
            skyLight = source.skyLight(origin).coerceIn(0, MAX_LIGHT),
            blockLight = source.blockLight(origin).coerceIn(0, MAX_LIGHT),
        )
    }

    private fun skyPoints(origin: BlockPoint, radius: Int): List<BlockPoint> = buildList {
        add(origin)
        for (z in listOf(-radius, 0, radius)) {
            for (x in listOf(-radius, 0, radius)) {
                if (x != 0 || z != 0) {
                    add(BlockPoint(origin.x + x, origin.y, origin.z + z))
                }
            }
        }
    }

    private fun enclosurePoints(origin: BlockPoint, radius: Int): List<BlockPoint> = buildList(26) {
        for (y in -1..1) {
            for (z in -1..1) {
                for (x in -1..1) {
                    if (x != 0 || y != 0 || z != 0) {
                        add(
                            BlockPoint(
                                x = origin.x + x * radius,
                                y = origin.y + y * radius,
                                z = origin.z + z * radius,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun median(values: List<Int>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle].toDouble()
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }

    private data class SkyObservation(
        val point: BlockPoint,
        val exposed: Boolean,
        val surfaceDepth: Int,
    )
}
