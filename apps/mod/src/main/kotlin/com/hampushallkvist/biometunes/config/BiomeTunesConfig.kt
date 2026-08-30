package com.hampushallkvist.biometunes.config

import com.hampushallkvist.biometunes.playback.PlaybackOptions
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class BiomeTunesConfig(
    val enabled: Boolean = true,
    val volume: Float = 1f,
    val crossfadeSeconds: Float = 5f,
    val biomeNotifications: Boolean = false,
    val bossMusic: Boolean = true,
    val bossNotifications: Boolean = false,
    val environmentalDebugHud: Boolean = false,
) {
    fun normalized() = copy(
        volume = volume.coerceIn(0f, 1f),
        crossfadeSeconds = crossfadeSeconds.coerceIn(0f, 15f),
    )

    val playbackOptions: PlaybackOptions
        get() = PlaybackOptions(volume, (crossfadeSeconds * 20f).roundToInt())
}
