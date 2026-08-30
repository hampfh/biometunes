package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.environment.ShelteredAudioSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ShelteredTreatmentEnvelopeTest {
    @Test
    fun `treatment interpolates from unchanged audio to configured maxima`() {
        val envelope = ShelteredTreatmentEnvelope()

        assertEquals(AudioTreatment.NONE, envelope.update(0.0f, settings, 0.0, advance = true))
        assertTreatment(0.35f, 0.0f, 0.12f, envelope.update(1.0f, settings, 0.0, advance = true))
        assertTreatment(0.675f, 0.5f, 0.06f, envelope.update(0.5f, settings, 0.0, advance = true))
    }

    @Test
    fun `active ticks approach the target at a bounded rate and paused ticks hold`() {
        val envelope = ShelteredTreatmentEnvelope()

        val first = envelope.update(1.0f, settings, smoothingSeconds = 2.0, advance = true)
        val paused = envelope.update(1.0f, settings, smoothingSeconds = 2.0, advance = false)
        val second = envelope.update(1.0f, settings, smoothingSeconds = 2.0, advance = true)

        assertTreatment(0.98375f, 0.975f, 0.003f, first)
        assertEquals(first, paused)
        assertTreatment(0.9675f, 0.95f, 0.006f, second)
    }

    @Test
    fun `reset returns subsequent treatment to unchanged audio`() {
        val envelope = ShelteredTreatmentEnvelope()
        envelope.update(1.0f, settings, smoothingSeconds = 0.0, advance = true)

        envelope.reset()

        assertEquals(AudioTreatment.NONE, envelope.update(0.0f, settings, 2.0, advance = false))
    }

    private fun assertTreatment(
        expectedGain: Float,
        expectedHighFrequency: Float,
        expectedReverbSend: Float,
        actual: AudioTreatment,
    ) {
        assertEquals(expectedGain, actual.gainMultiplier, absoluteTolerance = 0.000_01f)
        assertEquals(expectedHighFrequency, actual.highFrequencyGain, absoluteTolerance = 0.000_01f)
        assertEquals(expectedReverbSend, actual.reverbSend, absoluteTolerance = 0.000_01f)
    }

    private companion object {
        val settings = ShelteredAudioSettings(
            maximumGainMultiplier = 0.35f,
            maximumHighFrequencyGain = 0.0f,
            maximumReverbSend = 0.12f,
        )
    }
}
