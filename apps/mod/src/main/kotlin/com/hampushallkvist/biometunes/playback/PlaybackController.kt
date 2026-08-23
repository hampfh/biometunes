package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId

/**
 * Mixes the live tracks toward the desired one.
 *
 * Every live track is a [Voice] holding its own fade progress. Each tick the desired voice moves one
 * step toward full gain and every other voice moves one step toward silence, so a voice is only ever
 * disposed once it is inaudible. Progress is never reset or reassigned mid-fade: interrupting a fade,
 * losing a handle, or changing the fade duration only changes where a voice is heading, never its
 * current gain.
 */
class PlaybackController(private val adapter: AudioAdapter) {
    private val voices = mutableListOf<Voice>()
    private var failedStart: TrackId? = null
    private var failedStartCooldown = 0

    /**
     * @param advanceFade false while the game is paused. Minecraft keeps ticking the client but stops
     * pushing tickable sound volumes to the audio device, so advancing here would apply the whole
     * skipped fade in one step on resume.
     */
    fun update(
        desired: TrackDefinition,
        options: PlaybackOptions,
        advanceFade: Boolean = true,
    ) {
        val normalized = options.normalized()
        val endedProgress = pruneEndedVoices()
        val started = ensureDesiredVoice(desired, normalized, endedProgress)
        if (voices.isEmpty()) {
            adapter.setVanillaMusicSuppressed(false)
            return
        }
        if (advanceFade && !started) advanceFade(desired, normalized.crossfadeTicks)
        voices.forEach { voice ->
            adapter.setGain(voice.handle, equalPowerGain(voice.progress) * normalized.volume)
        }
        adapter.setVanillaMusicSuppressed(true)
    }

    fun stop() {
        voices.forEach { voice -> adapter.stop(voice.handle) }
        voices.clear()
        failedStart = null
        failedStartCooldown = 0
        adapter.setVanillaMusicSuppressed(false)
    }

    /** Drops voices the audio engine no longer owns, keeping their progress for an immediate restart. */
    private fun pruneEndedVoices(): Map<TrackId, Double> {
        val endedProgress = mutableMapOf<TrackId, Double>()
        voices.removeAll { voice ->
            if (adapter.isPlaying(voice.handle)) return@removeAll false
            adapter.stop(voice.handle)
            endedProgress[voice.track.id] = voice.progress
            true
        }
        return endedProgress
    }

    private fun ensureDesiredVoice(
        desired: TrackDefinition,
        options: PlaybackOptions,
        endedProgress: Map<TrackId, Double>,
    ): Boolean {
        if (voices.any { voice -> voice.track.id == desired.id }) {
            failedStart = null
            return false
        }
        if (desired.id == failedStart && failedStartCooldown > 0) {
            failedStartCooldown--
            return false
        }

        val immediate = options.crossfadeTicks == 0 || voices.isEmpty()
        val progress = if (immediate) 1.0 else endedProgress[desired.id] ?: 0.0
        disposeQuietestWhileFull()
        val handle = adapter.start(desired, equalPowerGain(progress) * options.volume)
        if (handle == null) {
            // Keep whatever is already playing rather than trading it for silence, and back off so a
            // repeatedly unavailable track cannot start a sound per tick.
            failedStart = desired.id
            failedStartCooldown = FAILED_START_COOLDOWN_TICKS
            return false
        }

        failedStart = null
        if (immediate) {
            voices.forEach { voice -> adapter.stop(voice.handle) }
            voices.clear()
        }
        voices += Voice(desired, handle, progress)
        return true
    }

    /** Frees a slot before a start so live handles never exceed [MAX_VOICES]. */
    private fun disposeQuietestWhileFull() {
        while (voices.size >= MAX_VOICES) {
            val quietest = voices.minBy { voice -> voice.progress }
            adapter.stop(quietest.handle)
            voices -= quietest
        }
    }

    private fun advanceFade(desired: TrackDefinition, crossfadeTicks: Int) {
        // Without a live desired voice the loudest one is the target, so a failed start recovers by
        // fading the previous track back up instead of stranding it mid-fade.
        val target = voices.firstOrNull { voice -> voice.track.id == desired.id }
            ?: voices.maxBy { voice -> voice.progress }
        val step = if (crossfadeTicks <= 0) 1.0 else 1.0 / crossfadeTicks
        voices.forEach { voice ->
            val delta = if (voice === target) step else -step
            voice.progress = (voice.progress + delta).coerceIn(0.0, 1.0)
        }
        voices.removeAll { voice ->
            if (voice === target || voice.progress > SILENT_PROGRESS) return@removeAll false
            adapter.stop(voice.handle)
            true
        }
    }

    private fun PlaybackOptions.normalized() = PlaybackOptions(
        volume = volume.coerceIn(0f, 1f),
        crossfadeTicks = crossfadeTicks.coerceIn(0, 300),
    )

    private companion object {
        /** Bounds concurrent streaming sound channels; Minecraft's pool can be as small as two. */
        const val MAX_VOICES = 3
        const val FAILED_START_COOLDOWN_TICKS = 20
        const val SILENT_PROGRESS = 1e-6
    }
}

private class Voice(
    val track: TrackDefinition,
    val handle: AudioHandle,
    var progress: Double,
)
