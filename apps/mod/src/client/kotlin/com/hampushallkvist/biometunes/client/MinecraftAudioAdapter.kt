package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.mixin.ChannelAccessor
import com.hampushallkvist.biometunes.mixin.SoundEngineAccessor
import com.hampushallkvist.biometunes.mixin.SoundManagerAccessor
import com.hampushallkvist.biometunes.playback.AudioAdapter
import com.hampushallkvist.biometunes.playback.AudioHandle
import com.hampushallkvist.biometunes.playback.AudioTreatment
import net.minecraft.client.Minecraft
import net.minecraft.client.sounds.SoundEngine
import net.minecraft.resources.Identifier

class MinecraftAudioAdapter(private val client: Minecraft) : AudioAdapter {
    private val availability = SoundAvailabilityCache()
    private val lowPass = OpenAlLowPassFilter(
        backend = LwjglLowPassBackend(),
        onFailure = { error ->
            BiomeTunesClient.logger.warn(
                "OpenAL EFX low-pass failed; sheltered playback will use gain reduction only",
                error,
            )
        },
    )
    @Volatile
    private var effectiveGainMultiplier = 1.0f

    val treatmentDiagnostics: AudioTreatmentDiagnostics
        get() = lowPass.diagnostics.copy(effectiveGainMultiplier = effectiveGainMultiplier)

    override fun start(track: TrackDefinition, initialGain: Float): AudioHandle? {
        val location = Identifier.parse(track.soundEvent)
        return when (
            val decision = availability.start(
                trackId = track.id,
                resolvePlayableWeight = { client.soundManager.getSoundEvent(location)?.weight },
                attemptPlayback = {
                    val instance = BiomeTunesSoundInstance(location, initialGain)
                    when (client.soundManager.play(instance)) {
                        SoundEngine.PlayResult.STARTED,
                        SoundEngine.PlayResult.STARTED_SILENTLY -> instance
                        SoundEngine.PlayResult.NOT_STARTED -> null
                    }
                },
            )
        ) {
            is SoundStartDecision.Started -> decision.handle
            SoundStartDecision.NotStarted,
            SoundStartDecision.SkipUnavailable -> null
            is SoundStartDecision.ReportUnavailable -> {
                when (decision.reason) {
                    UnavailableSoundReason.MISSING_EVENT -> BiomeTunesClient.logger.warn(
                        "Missing sound event {} for track {}; unavailable until resource reload",
                        location,
                        track.id.value,
                    )
                    UnavailableSoundReason.ZERO_PLAYABLE_WEIGHT -> BiomeTunesClient.logger.warn(
                        "Sound event {} for track {} has no playable sounds; unavailable until resource reload",
                        location,
                        track.id.value,
                    )
                }
                null
            }
        }
    }

    override fun setGain(handle: AudioHandle, gain: Float) {
        (handle as? BiomeTunesSoundInstance)?.setGain(gain)
    }

    override fun setLowPass(handle: AudioHandle, highFrequencyGain: Float) {
        val instance = handle as? BiomeTunesSoundInstance ?: return
        instance.setRequestedHighFrequencyGain(highFrequencyGain)
        val channelHandle = soundEngine().biometunesInstanceToChannel()[instance] ?: return
        channelHandle.execute { channel ->
            lowPass.apply(
                source = (channel as ChannelAccessor).biometunesSource(),
                highFrequencyGain = instance.requestedHighFrequencyGain,
            )
        }
    }

    override fun setTreatment(treatment: AudioTreatment) {
        effectiveGainMultiplier = treatment.gainMultiplier
    }

    override fun stop(handle: AudioHandle) {
        (handle as? BiomeTunesSoundInstance)?.let { instance ->
            setLowPass(instance, 1.0f)
            client.soundManager.stop(instance)
        }
    }

    override fun isPlaying(handle: AudioHandle): Boolean =
        (handle as? BiomeTunesSoundInstance)?.let(client.soundManager::isActive) == true

    override fun setVanillaMusicSuppressed(suppressed: Boolean) {
        if (VanillaMusicGate.suppressed == suppressed) return
        if (suppressed) client.musicManager.stopPlaying()
        VanillaMusicGate.suppressed = suppressed
    }

    fun clearUnavailableSounds() = availability.clear()

    fun resetAudioTreatment() {
        effectiveGainMultiplier = 1.0f
        val engine = soundEngine()
        engine.biometunesInstanceToChannel()
            .filterKeys { it is BiomeTunesSoundInstance }
            .values
            .forEach { handle ->
                handle.execute { channel ->
                    lowPass.apply((channel as ChannelAccessor).biometunesSource(), 1.0f)
                }
            }
        engine.biometunesChannelAccess().executeOnChannels {
            lowPass.reset(intArrayOf())
        }
    }

    private fun soundEngine(): SoundEngineAccessor =
        (client.soundManager as SoundManagerAccessor).biometunesSoundEngine() as SoundEngineAccessor
}
