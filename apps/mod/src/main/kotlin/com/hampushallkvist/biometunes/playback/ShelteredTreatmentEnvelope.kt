package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.environment.ShelteredAudioSettings
import kotlin.math.min

data class AudioTreatment(
    val gainMultiplier: Float,
    val highFrequencyGain: Float,
    val reverbSend: Float = 0.0f,
) {
    companion object {
        val NONE = AudioTreatment(gainMultiplier = 1.0f, highFrequencyGain = 1.0f, reverbSend = 0.0f)
    }
}

class ShelteredTreatmentEnvelope {
    private var intensity = 0.0f

    fun update(
        targetIntensity: Float,
        settings: ShelteredAudioSettings,
        smoothingSeconds: Double,
        advance: Boolean,
    ): AudioTreatment {
        val target = targetIntensity.coerceIn(0.0f, 1.0f)
        if (advance) {
            intensity = if (smoothingSeconds <= 0.0) {
                target
            } else {
                val maximumStep = (1.0 / (smoothingSeconds * TICKS_PER_SECOND)).toFloat()
                when {
                    target > intensity -> min(target, intensity + maximumStep)
                    target < intensity -> maxOf(target, intensity - maximumStep)
                    else -> intensity
                }
            }
        }

        val maximumGain = settings.maximumGainMultiplier.coerceIn(0.0f, 1.0f)
        val maximumHighFrequency = settings.maximumHighFrequencyGain.coerceIn(0.0f, 1.0f)
        val maximumReverbSend = settings.maximumReverbSend.coerceIn(0.0f, 1.0f)
        return AudioTreatment(
            gainMultiplier = lerp(1.0f, maximumGain, intensity),
            highFrequencyGain = lerp(1.0f, maximumHighFrequency, intensity),
            reverbSend = lerp(0.0f, maximumReverbSend, intensity),
        )
    }

    fun reset() {
        intensity = 0.0f
    }

    private fun lerp(start: Float, end: Float, progress: Float) = start + (end - start) * progress

    private companion object {
        const val TICKS_PER_SECOND = 20.0
    }
}
