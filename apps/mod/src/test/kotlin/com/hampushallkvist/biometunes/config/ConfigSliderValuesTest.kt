package com.hampushallkvist.biometunes.config

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigSliderValuesTest {
    @Test
    fun `volume slider rounds to whole percentage points`() {
        assertEquals(0f, ConfigSliderValues.volume(0.0))
        assertEquals(0.35f, ConfigSliderValues.volume(0.346))
        assertEquals(1f, ConfigSliderValues.volume(1.0))
    }

    @Test
    fun `crossfade slider rounds to half-second steps`() {
        assertEquals(0f, ConfigSliderValues.crossfadeSeconds(0.0))
        assertEquals(7.5f, ConfigSliderValues.crossfadeSeconds(0.5))
        assertEquals(15f, ConfigSliderValues.crossfadeSeconds(0.99))
    }
}
