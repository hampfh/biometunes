package com.hampushallkvist.biometunes.environment

data class EnvironmentalScores(
    val exposure: Double,
    val depth: Double,
)

object EnvironmentalClassifier {
    fun score(
        evidence: EnvironmentalEvidence,
        sampling: EnvironmentalSampling,
        weights: EnvironmentalWeights,
    ): EnvironmentalScores {
        val nearbySkyExposure = evidence.skyExposedSamples.toDouble() / evidence.skySampleCount
        val skyLight = evidence.skyLight.coerceIn(0, 15) / 15.0
        val blockLight = evidence.blockLight.coerceIn(0, 15) / 15.0
        val enclosure = evidence.enclosure.coerceIn(0.0, 1.0)

        val exposure = weightedMean(
            nearbySkyExposure.coerceIn(0.0, 1.0) to weights.nearbySkyExposure,
            skyLight to weights.skyLight,
            1.0 - enclosure to weights.localOpenness,
        )
        val depth = weightedMean(
            (evidence.medianSurfaceDepth / sampling.surfaceDepthScale).coerceIn(0.0, 1.0) to
                weights.surfaceDepth,
            enclosure to weights.localEnclosure,
            1.0 - skyLight to weights.skyDarkness,
            1.0 - blockLight to weights.blockDarkness,
        )
        return EnvironmentalScores(
            exposure = exposure.coerceIn(0.0, 1.0),
            depth = depth.coerceIn(0.0, 1.0),
        )
    }

    private fun weightedMean(vararg values: Pair<Double, Double>): Double {
        val totalWeight = values.sumOf(Pair<Double, Double>::second)
        require(totalWeight > 0.0) { "Environmental score weights must have a positive total" }
        return values.sumOf { (value, weight) -> value * weight } / totalWeight
    }
}
