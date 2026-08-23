package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.playback.AudioAdapter
import com.hampushallkvist.biometunes.playback.AudioHandle
import net.minecraft.client.Minecraft
import net.minecraft.client.sounds.SoundEngine
import net.minecraft.resources.Identifier

class MinecraftAudioAdapter(private val client: Minecraft) : AudioAdapter {
    private val availability = SoundAvailabilityCache()

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

    override fun stop(handle: AudioHandle) {
        (handle as? BiomeTunesSoundInstance)?.let(client.soundManager::stop)
    }

    override fun isPlaying(handle: AudioHandle): Boolean =
        (handle as? BiomeTunesSoundInstance)?.let(client.soundManager::isActive) == true

    override fun setVanillaMusicSuppressed(suppressed: Boolean) {
        if (VanillaMusicGate.suppressed == suppressed) return
        if (suppressed) client.musicManager.stopPlaying()
        VanillaMusicGate.suppressed = suppressed
    }

    fun clearUnavailableSounds() = availability.clear()
}
