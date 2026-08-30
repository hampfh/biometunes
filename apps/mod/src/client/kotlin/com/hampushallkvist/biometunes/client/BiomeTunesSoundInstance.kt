package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.playback.AudioHandle
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource

class BiomeTunesSoundInstance(
    soundEvent: Identifier,
    initialGain: Float,
) : AbstractTickableSoundInstance(
    SoundEvent.createVariableRangeEvent(soundEvent),
    SoundSource.MUSIC,
    RandomSource.create(),
), AudioHandle {
    private var gain = initialGain
    @Volatile
    var requestedHighFrequencyGain = 1.0f
        private set

    init {
        looping = true
        delay = 0
        attenuation = SoundInstance.Attenuation.NONE
        relative = true
    }

    fun setGain(value: Float) {
        gain = value.coerceIn(0f, 1f)
    }

    fun setRequestedHighFrequencyGain(value: Float) {
        requestedHighFrequencyGain = value.coerceIn(0.0f, 1.0f)
    }

    override fun getVolume(): Float = gain

    override fun tick() = Unit
}
