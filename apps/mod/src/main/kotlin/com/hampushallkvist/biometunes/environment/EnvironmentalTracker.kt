package com.hampushallkvist.biometunes.environment

import kotlin.math.exp

enum class DecisionBasis {
    WEIGHTED_SCORES,
    DECISIVE_SIGNAL,
    RETAINED_INSUFFICIENT_EVIDENCE,
}

data class TrackedEnvironment(
    val mode: EnvironmentMode,
    val smoothedScores: EnvironmentalScores,
    val decisionBasis: DecisionBasis,
)

class EnvironmentalTracker {
    private var mode: EnvironmentMode? = null
    private var smoothedScores: EnvironmentalScores? = null

    fun update(
        raw: EnvironmentalScores,
        constraints: EnvironmentConstraints,
        decisiveMode: EnvironmentMode?,
        thresholds: EnvironmentalThresholds,
        smoothingSeconds: Double,
        elapsedSeconds: Double,
    ): TrackedEnvironment {
        require(smoothingSeconds >= 0.0) { "smoothingSeconds must be non-negative" }
        require(elapsedSeconds >= 0.0) { "elapsedSeconds must be non-negative" }

        if (decisiveMode != null && constraints.allows(decisiveMode)) {
            mode = decisiveMode
            smoothedScores = raw
            return TrackedEnvironment(decisiveMode, raw, DecisionBasis.DECISIVE_SIGNAL)
        }

        val smoothed = smooth(raw, smoothingSeconds, elapsedSeconds)
        val selected = selectMode(smoothed, constraints, thresholds)
        mode = selected
        smoothedScores = smoothed
        return TrackedEnvironment(selected, smoothed, DecisionBasis.WEIGHTED_SCORES)
    }

    fun reset() {
        mode = null
        smoothedScores = null
    }

    private fun smooth(
        raw: EnvironmentalScores,
        smoothingSeconds: Double,
        elapsedSeconds: Double,
    ): EnvironmentalScores {
        val previous = smoothedScores ?: return raw
        if (smoothingSeconds == 0.0) return raw
        val alpha = 1.0 - exp(-elapsedSeconds / smoothingSeconds)
        return EnvironmentalScores(
            exposure = previous.exposure + alpha * (raw.exposure - previous.exposure),
            depth = previous.depth + alpha * (raw.depth - previous.depth),
        )
    }

    private fun selectMode(
        scores: EnvironmentalScores,
        constraints: EnvironmentConstraints,
        thresholds: EnvironmentalThresholds,
    ): EnvironmentMode {
        if (mode == EnvironmentMode.OUTSIDE &&
            constraints.outsideAllowed &&
            scores.exposure >= thresholds.outsideExitExposure) {
            return EnvironmentMode.OUTSIDE
        }
        if (mode == EnvironmentMode.SUBTERRANEAN &&
            constraints.subterraneanAllowed &&
            scores.depth >= thresholds.subterraneanExitDepth &&
            scores.exposure <= thresholds.subterraneanExitMaxExposure) {
            return EnvironmentMode.SUBTERRANEAN
        }
        if (constraints.outsideAllowed && scores.exposure >= thresholds.outsideEnterExposure) {
            return EnvironmentMode.OUTSIDE
        }
        if (constraints.subterraneanAllowed &&
            scores.depth >= thresholds.subterraneanEnterDepth &&
            scores.exposure <= thresholds.subterraneanEnterMaxExposure) {
            return EnvironmentMode.SUBTERRANEAN
        }
        return EnvironmentMode.SHELTERED
    }

    private fun EnvironmentConstraints.allows(candidate: EnvironmentMode): Boolean = when (candidate) {
        EnvironmentMode.OUTSIDE -> outsideAllowed
        EnvironmentMode.SHELTERED -> true
        EnvironmentMode.SUBTERRANEAN -> subterraneanAllowed
    }
}
