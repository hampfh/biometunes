package com.hampushallkvist.biometunes.playback

import kotlin.math.PI
import kotlin.math.sin

/**
 * Gain of a voice that is [progress] of the way into a fade, on an equal-power curve.
 *
 * Two voices whose progress sums to 1 keep constant total power, so a pair moving in opposite
 * directions at the same rate crossfades without a midpoint dip.
 */
fun equalPowerGain(progress: Double): Float =
    sin(progress.coerceIn(0.0, 1.0) * PI / 2.0).toFloat()
