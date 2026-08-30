package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult
import com.hampushallkvist.biometunes.environment.NotClassifiedReason

enum class BossEncounter(val catalogKey: String) {
    ENDER_DRAGON("ender_dragon"),
    WITHER("wither"),
}

data class PlayerContext(
    val biomeId: String,
    val biomeTags: Set<String>,
    val dimensionId: String,
    val boss: BossEncounter?,
    val environmentalClassification: EnvironmentalClassificationResult =
        EnvironmentalClassificationResult.NotClassified(NotClassifiedReason.NO_PROFILE),
)
