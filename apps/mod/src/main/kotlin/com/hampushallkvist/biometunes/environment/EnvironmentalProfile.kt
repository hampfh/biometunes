package com.hampushallkvist.biometunes.environment

import com.hampushallkvist.biometunes.catalog.TrackPool

data class EnvironmentalProfile(
    val nativeUndergroundBiomes: Set<String>,
    val subterraneanTracks: TrackPool,
    val sampling: EnvironmentalSampling,
    val weights: EnvironmentalWeights,
    val thresholds: EnvironmentalThresholds,
    val smoothingSeconds: Double,
    val shelteredAudio: ShelteredAudioSettings,
)

data class EnvironmentalSampling(
    val nearbyRadius: Int,
    val enclosureRadius: Int,
    val surfaceDepthScale: Int,
)

data class EnvironmentalWeights(
    val nearbySkyExposure: Double,
    val skyLight: Double,
    val localOpenness: Double,
    val surfaceDepth: Double,
    val localEnclosure: Double,
    val skyDarkness: Double,
    val blockDarkness: Double,
)

data class EnvironmentalThresholds(
    val outsideEnterExposure: Double,
    val outsideExitExposure: Double,
    val subterraneanEnterDepth: Double,
    val subterraneanExitDepth: Double,
    val subterraneanEnterMaxExposure: Double,
    val subterraneanExitMaxExposure: Double,
)

data class ShelteredAudioSettings(
    val maximumGainMultiplier: Float,
    val maximumHighFrequencyGain: Float,
)
