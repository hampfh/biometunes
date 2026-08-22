package com.hampushallkvist.biometunes.selection

enum class BossEncounter(val catalogKey: String) {
    ENDER_DRAGON("ender_dragon"),
    WITHER("wither"),
}

data class PlayerContext(
    val biomeId: String,
    val biomeTags: Set<String>,
    val dimensionId: String,
    val boss: BossEncounter?,
)
