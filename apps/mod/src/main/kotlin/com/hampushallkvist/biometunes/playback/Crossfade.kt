package com.hampushallkvist.biometunes.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class FadeGains(val outgoing: Float, val incoming: Float)

fun equalPowerGains(progress: Double): FadeGains {
    val clampedProgress = progress.coerceIn(0.0, 1.0)
    return FadeGains(
        outgoing = cos(clampedProgress * PI / 2.0).toFloat(),
        incoming = sin(clampedProgress * PI / 2.0).toFloat(),
    )
}
