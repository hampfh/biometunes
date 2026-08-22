package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.playback.AudioAdapter
import com.hampushallkvist.biometunes.playback.AudioHandle
import net.minecraft.client.Minecraft
import net.minecraft.client.sounds.SoundEngine
import net.minecraft.resources.Identifier

class MinecraftAudioAdapter(private val client: Minecraft) : AudioAdapter {
    private val unavailable = mutableSetOf<TrackId>()

    override fun start(track: TrackDefinition, initialGain: Float): AudioHandle? {
        if (track.id in unavailable) return null

        val location = Identifier.parse(track.soundEvent)
        if (client.soundManager.getSoundEvent(location) == null) {
            unavailable += track.id
            BiomeTunesClient.logger.warn(
                "Missing sound event {} for track {}",
                location,
                track.id.value,
            )
            return null
        }

        val instance = BiomeTunesSoundInstance(location, initialGain)
        return when (client.soundManager.play(instance)) {
            SoundEngine.PlayResult.STARTED,
            SoundEngine.PlayResult.STARTED_SILENTLY -> instance
            SoundEngine.PlayResult.NOT_STARTED -> null
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

    fun clearUnavailableSounds() = unavailable.clear()
}
