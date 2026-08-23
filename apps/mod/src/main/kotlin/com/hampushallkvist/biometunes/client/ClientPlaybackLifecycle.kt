package com.hampushallkvist.biometunes.client

object GameplayActivity {
    fun isActive(hasLevel: Boolean, hasPlayer: Boolean, isEndCredits: Boolean): Boolean =
        hasLevel && hasPlayer && !isEndCredits
}

data class ClientPlaybackDecision(
    val activeGameplay: Boolean,
    val stopPlayback: Boolean,
    val clearContext: Boolean,
    val sampleContext: Boolean,
)

class ClientPlaybackLifecycle(private val sampleIntervalTicks: Int) {
    private var observedLevel: Any? = null
    private var observedDimension: Any? = null
    private var observedPlayer: Any? = null
    private var ticksSinceSample = 0
    private var forceResample = true

    fun tick(
        levelIdentity: Any?,
        dimensionIdentity: Any?,
        playerIdentity: Any?,
        isEndCredits: Boolean,
    ): ClientPlaybackDecision {
        val activeGameplay = GameplayActivity.isActive(
            hasLevel = levelIdentity != null,
            hasPlayer = playerIdentity != null,
            isEndCredits = isEndCredits,
        )
        if (!activeGameplay) {
            resetIdentities()
            return ClientPlaybackDecision(
                activeGameplay = false,
                stopPlayback = true,
                clearContext = true,
                sampleContext = false,
            )
        }

        val identityChanged =
            levelIdentity !== observedLevel ||
                dimensionIdentity != observedDimension ||
                playerIdentity !== observedPlayer
        if (identityChanged) {
            observedLevel = levelIdentity
            observedDimension = dimensionIdentity
            observedPlayer = playerIdentity
            ticksSinceSample = 0
            forceResample = true
        }

        val sampleContext = if (forceResample) {
            true
        } else {
            ++ticksSinceSample >= sampleIntervalTicks
        }
        if (sampleContext) {
            ticksSinceSample = 0
            forceResample = false
        }

        return ClientPlaybackDecision(
            activeGameplay = true,
            stopPlayback = identityChanged,
            clearContext = identityChanged,
            sampleContext = sampleContext,
        )
    }

    fun forceFreshSample() {
        ticksSinceSample = 0
        forceResample = true
    }

    private fun resetIdentities() {
        observedLevel = null
        observedDimension = null
        observedPlayer = null
        ticksSinceSample = 0
        forceResample = true
    }
}
