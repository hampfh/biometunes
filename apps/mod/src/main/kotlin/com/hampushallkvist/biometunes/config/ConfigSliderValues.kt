package com.hampushallkvist.biometunes.config

import kotlin.math.round

object ConfigSliderValues {
    fun volume(value: Double): Float = (round(value * 100.0) / 100.0).toFloat()

    fun crossfadeSeconds(value: Double): Float = (round(value * 30.0) / 2.0).toFloat()
}
