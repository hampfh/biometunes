package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.catalog.TrackDefinition

class PlaybackController(private val adapter: AudioAdapter) {
    private var state: PlaybackState = PlaybackState.Idle

    fun update(desired: TrackDefinition, options: PlaybackOptions) {
        val normalized = options.normalized()
        if (restartIfEnded(desired, normalized.volume)) return
        when (val currentState = state) {
            PlaybackState.Idle -> startPlaying(desired, normalized.volume)
            is PlaybackState.Playing -> updatePlaying(currentState, desired, normalized)
            is PlaybackState.Crossfading -> updateCrossfading(currentState, desired, normalized)
        }
    }

    fun stop() {
        when (val currentState = state) {
            PlaybackState.Idle -> Unit
            is PlaybackState.Playing -> stop(currentState.current)
            is PlaybackState.Crossfading -> {
                stop(currentState.outgoing)
                stop(currentState.incoming)
            }
        }
        state = PlaybackState.Idle
        adapter.setVanillaMusicSuppressed(false)
    }

    private fun startPlaying(desired: TrackDefinition, volume: Float) {
        val handle = adapter.start(desired, volume)
        if (handle == null) {
            state = PlaybackState.Idle
            adapter.setVanillaMusicSuppressed(false)
            return
        }
        state = PlaybackState.Playing(ActiveTrack(desired, handle, mixGain = 1f))
        adapter.setVanillaMusicSuppressed(true)
    }

    private fun restartIfEnded(desired: TrackDefinition, volume: Float): Boolean {
        when (val currentState = state) {
            PlaybackState.Idle -> return false
            is PlaybackState.Playing -> {
                if (adapter.isPlaying(currentState.current.handle)) return false
                stop(currentState.current)
            }
            is PlaybackState.Crossfading -> {
                val bothPlaying = adapter.isPlaying(currentState.outgoing.handle) &&
                    adapter.isPlaying(currentState.incoming.handle)
                if (bothPlaying) return false
                stop(currentState.outgoing)
                stop(currentState.incoming)
            }
        }
        state = PlaybackState.Idle
        startPlaying(desired, volume)
        return true
    }

    private fun updatePlaying(
        playing: PlaybackState.Playing,
        desired: TrackDefinition,
        options: PlaybackOptions,
    ) {
        if (playing.current.track.id == desired.id) {
            state = PlaybackState.Playing(applyGain(playing.current, mixGain = 1f, options.volume))
            return
        }
        if (options.crossfadeTicks == 0) {
            stop(playing.current)
            state = PlaybackState.Idle
            startPlaying(desired, options.volume)
            return
        }
        beginFade(playing.current, desired, options.volume)
    }

    private fun updateCrossfading(
        crossfading: PlaybackState.Crossfading,
        desired: TrackDefinition,
        options: PlaybackOptions,
    ) {
        if (
            crossfading.incoming.track.id != desired.id &&
            crossfading.outgoing.track.id != desired.id
        ) {
            interruptFade(crossfading, desired, options)
            return
        }
        if (crossfading.outgoing.track.id == desired.id) {
            retargetFade(crossfading, options)
            return
        }
        if (options.crossfadeTicks == 0) {
            completeFade(crossfading.outgoing, crossfading.incoming, options.volume)
            return
        }

        val elapsedTicks = crossfading.elapsedTicks + 1
        if (elapsedTicks >= options.crossfadeTicks) {
            completeFade(crossfading.outgoing, crossfading.incoming, options.volume)
            return
        }

        val gains = equalPowerGains(elapsedTicks.toDouble() / options.crossfadeTicks)
        val outgoingGain = crossfading.outgoingOrigin * gains.outgoing
        val incomingGain = crossfading.incomingOrigin +
            (1f - crossfading.incomingOrigin) * gains.incoming
        state = crossfading.copy(
            outgoing = applyGain(crossfading.outgoing, outgoingGain, options.volume),
            incoming = applyGain(crossfading.incoming, incomingGain, options.volume),
            elapsedTicks = elapsedTicks,
        )
    }

    private fun beginFade(outgoing: ActiveTrack, desired: TrackDefinition, volume: Float) {
        val adjustedOutgoing = applyGain(outgoing, outgoing.mixGain, volume)
        val incomingHandle = adapter.start(desired, initialGain = 0f)
        if (incomingHandle == null) {
            stop(adjustedOutgoing)
            state = PlaybackState.Idle
            adapter.setVanillaMusicSuppressed(false)
            return
        }
        state = PlaybackState.Crossfading(
            outgoing = adjustedOutgoing,
            incoming = ActiveTrack(desired, incomingHandle, mixGain = 0f),
            outgoingOrigin = adjustedOutgoing.mixGain,
        )
    }

    private fun interruptFade(
        crossfading: PlaybackState.Crossfading,
        desired: TrackDefinition,
        options: PlaybackOptions,
    ) {
        if (options.crossfadeTicks == 0) {
            stop(crossfading.outgoing)
            stop(crossfading.incoming)
            state = PlaybackState.Idle
            startPlaying(desired, options.volume)
            return
        }
        val (quieter, louder) = if (crossfading.outgoing.mixGain <= crossfading.incoming.mixGain) {
            crossfading.outgoing to crossfading.incoming
        } else {
            crossfading.incoming to crossfading.outgoing
        }
        stop(quieter)
        state = PlaybackState.Playing(louder)
        beginFade(louder, desired, options.volume)
    }

    private fun retargetFade(
        crossfading: PlaybackState.Crossfading,
        options: PlaybackOptions,
    ) {
        if (options.crossfadeTicks == 0) {
            completeFade(crossfading.incoming, crossfading.outgoing, options.volume)
            return
        }
        val outgoing = applyGain(
            crossfading.incoming,
            crossfading.incoming.mixGain,
            options.volume,
        )
        val incoming = applyGain(
            crossfading.outgoing,
            crossfading.outgoing.mixGain,
            options.volume,
        )
        state = PlaybackState.Crossfading(
            outgoing = outgoing,
            incoming = incoming,
            outgoingOrigin = outgoing.mixGain,
            incomingOrigin = incoming.mixGain,
        )
    }

    private fun completeFade(outgoing: ActiveTrack, incoming: ActiveTrack, volume: Float) {
        stop(outgoing)
        state = PlaybackState.Playing(applyGain(incoming, mixGain = 1f, volume))
    }

    private fun applyGain(active: ActiveTrack, mixGain: Float, volume: Float): ActiveTrack {
        adapter.setGain(active.handle, mixGain * volume)
        return active.copy(mixGain = mixGain)
    }

    private fun stop(active: ActiveTrack) {
        adapter.stop(active.handle)
    }

    private fun PlaybackOptions.normalized() = PlaybackOptions(
        volume = volume.coerceIn(0f, 1f),
        crossfadeTicks = crossfadeTicks.coerceIn(0, 300),
    )
}

private data class ActiveTrack(
    val track: TrackDefinition,
    val handle: AudioHandle,
    val mixGain: Float,
)

private sealed interface PlaybackState {
    data object Idle : PlaybackState

    data class Playing(val current: ActiveTrack) : PlaybackState

    data class Crossfading(
        val outgoing: ActiveTrack,
        val incoming: ActiveTrack,
        val outgoingOrigin: Float = 1f,
        val incomingOrigin: Float = 0f,
        val elapsedTicks: Int = 0,
    ) : PlaybackState
}
