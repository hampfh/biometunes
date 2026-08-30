package com.hampushallkvist.biometunes.environment

enum class EnvironmentMode {
    OUTSIDE,
    SHELTERED,
    SUBTERRANEAN,
}

data class EnvironmentConstraints(
    val outsideAllowed: Boolean,
    val subterraneanAllowed: Boolean,
)

object EnvironmentalRules {
    fun constraints(evidence: EnvironmentalEvidence) = EnvironmentConstraints(
        outsideAllowed = evidence.centerSkyExposed,
        subterraneanAllowed = evidence.skyExposedSamples == 0,
    )

    @Suppress("UNUSED_PARAMETER")
    fun decisiveMode(
        evidence: EnvironmentalEvidence,
        constraints: EnvironmentConstraints,
    ): EnvironmentMode? = null
}
