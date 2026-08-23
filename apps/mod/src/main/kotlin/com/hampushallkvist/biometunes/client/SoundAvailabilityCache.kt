package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackId

enum class UnavailableSoundReason {
    MISSING_EVENT,
    ZERO_PLAYABLE_WEIGHT,
}

sealed interface SoundStartDecision<out T> {
    data class Started<T>(val handle: T) : SoundStartDecision<T>
    data object NotStarted : SoundStartDecision<Nothing>
    data object SkipUnavailable : SoundStartDecision<Nothing>
    data class ReportUnavailable(val reason: UnavailableSoundReason) : SoundStartDecision<Nothing>
}

class SoundAvailabilityCache {
    private val unavailable = mutableSetOf<TrackId>()

    fun <T : Any> start(
        trackId: TrackId,
        resolvePlayableWeight: () -> Int?,
        attemptPlayback: () -> T?,
    ): SoundStartDecision<T> {
        if (trackId in unavailable) return SoundStartDecision.SkipUnavailable
        val playableWeight = resolvePlayableWeight()
        val reason = when {
            playableWeight == null -> UnavailableSoundReason.MISSING_EVENT
            playableWeight <= 0 -> UnavailableSoundReason.ZERO_PLAYABLE_WEIGHT
            else -> return attemptPlayback()?.let { handle -> SoundStartDecision.Started(handle) }
                ?: SoundStartDecision.NotStarted
        }
        unavailable += trackId
        return SoundStartDecision.ReportUnavailable(reason)
    }

    fun clear() = unavailable.clear()
}
