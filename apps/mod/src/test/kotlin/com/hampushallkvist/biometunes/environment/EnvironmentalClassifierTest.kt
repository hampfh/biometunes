package com.hampushallkvist.biometunes.environment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvironmentalClassifierTest {
    @Test
    fun `scores environmental evidence as configured weighted means`() {
        val evidence = EnvironmentalEvidence(
            centerSkyExposed = true,
            skyExposedSamples = 3,
            skySampleCount = 6,
            medianSurfaceDepth = 24.0,
            enclosure = 0.25,
            skyLight = 6,
            blockLight = 3,
        )

        val scores = EnvironmentalClassifier.score(evidence, sampling, weights)

        assertEquals(0.525, scores.exposure, absoluteTolerance = 0.000_001)
        assertEquals(0.6925 / 1.45, scores.depth, absoluteTolerance = 0.000_001)
    }

    @Test
    fun `darkness is weak supporting depth evidence and cannot make a shallow room subterranean`() {
        val shallowDarkRoom = EnvironmentalEvidence(
            centerSkyExposed = false,
            skyExposedSamples = 0,
            skySampleCount = 9,
            medianSurfaceDepth = 0.0,
            enclosure = 0.0,
            skyLight = 0,
            blockLight = 0,
        )

        val scores = EnvironmentalClassifier.score(shallowDarkRoom, sampling, weights)

        assertEquals(0.20 / 1.45, scores.depth, absoluteTolerance = 0.000_001)
        assertTrue(scores.depth < thresholds.subterraneanEnterDepth)
    }

    @Test
    fun `hard constraints derive directly from current sky observations`() {
        val roofed = evidence(centerSkyExposed = false, skyExposedSamples = 0)
        val nearbyOpening = evidence(centerSkyExposed = false, skyExposedSamples = 1)

        assertEquals(EnvironmentConstraints(outsideAllowed = false, subterraneanAllowed = true), EnvironmentalRules.constraints(roofed))
        assertEquals(EnvironmentConstraints(outsideAllowed = false, subterraneanAllowed = false), EnvironmentalRules.constraints(nearbyOpening))
        assertEquals(null, EnvironmentalRules.decisiveMode(roofed, EnvironmentalRules.constraints(roofed)))
    }

    private fun evidence(centerSkyExposed: Boolean, skyExposedSamples: Int) = EnvironmentalEvidence(
        centerSkyExposed = centerSkyExposed,
        skyExposedSamples = skyExposedSamples,
        skySampleCount = 9,
        medianSurfaceDepth = 0.0,
        enclosure = 0.0,
        skyLight = 0,
        blockLight = 0,
    )

    private companion object {
        val sampling = EnvironmentalSampling(nearbyRadius = 6, enclosureRadius = 2, surfaceDepthScale = 48)
        val weights = EnvironmentalWeights(1.0, 0.25, 0.25, 1.0, 0.25, 0.15, 0.05)
        val thresholds = EnvironmentalThresholds(0.65, 0.45, 0.70, 0.55, 0.25, 0.40)
    }
}
