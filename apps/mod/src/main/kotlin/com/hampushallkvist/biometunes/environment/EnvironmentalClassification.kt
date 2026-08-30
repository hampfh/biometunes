package com.hampushallkvist.biometunes.environment

enum class NotClassifiedReason {
    BOSS_ENCOUNTER,
    NO_PROFILE,
    NATIVE_UNDERGROUND_BIOME,
    INSUFFICIENT_EVIDENCE,
}

data class EnvironmentalClassificationInput(
    val dimensionId: String,
    val biomeId: String,
    val bossEncounterActive: Boolean,
    val profile: EnvironmentalProfile?,
    val origin: BlockPoint,
    val elapsedSeconds: Double,
)

data class EnvironmentalDiagnostics(
    val profileDimensionId: String,
    val evidence: EnvironmentalEvidence,
    val rawScores: EnvironmentalScores,
    val smoothedScores: EnvironmentalScores,
    val constraints: EnvironmentConstraints,
    val decisionBasis: DecisionBasis,
)

sealed interface EnvironmentalClassificationResult {
    data class NotClassified(
        val reason: NotClassifiedReason,
    ) : EnvironmentalClassificationResult

    data class Classified(
        val mode: EnvironmentMode,
        val shelterIntensity: Float,
        val diagnostics: EnvironmentalDiagnostics,
    ) : EnvironmentalClassificationResult
}

class EnvironmentalClassification(
    private val tracker: EnvironmentalTracker = EnvironmentalTracker(),
) {
    private var lastClassified: EnvironmentalClassificationResult.Classified? = null

    fun classify(
        input: EnvironmentalClassificationInput,
        source: EnvironmentalObservationSource,
    ): EnvironmentalClassificationResult {
        if (input.bossEncounterActive) {
            return EnvironmentalClassificationResult.NotClassified(NotClassifiedReason.BOSS_ENCOUNTER)
        }
        val profile = input.profile
            ?: return EnvironmentalClassificationResult.NotClassified(NotClassifiedReason.NO_PROFILE)
        if (input.biomeId in profile.nativeUndergroundBiomes) {
            return EnvironmentalClassificationResult.NotClassified(NotClassifiedReason.NATIVE_UNDERGROUND_BIOME)
        }

        val evidence = EnvironmentalEvidenceSampler.sample(input.origin, profile.sampling, source)
            ?: return retainedOrInsufficient()
        val rawScores = EnvironmentalClassifier.score(evidence, profile.sampling, profile.weights)
        val constraints = EnvironmentalRules.constraints(evidence)
        val tracked = tracker.update(
            raw = rawScores,
            constraints = constraints,
            decisiveMode = EnvironmentalRules.decisiveMode(evidence, constraints),
            thresholds = profile.thresholds,
            smoothingSeconds = profile.smoothingSeconds,
            elapsedSeconds = input.elapsedSeconds,
        )
        val diagnostics = EnvironmentalDiagnostics(
            profileDimensionId = input.dimensionId,
            evidence = evidence,
            rawScores = rawScores,
            smoothedScores = tracked.smoothedScores,
            constraints = constraints,
            decisionBasis = tracked.decisionBasis,
        )
        val result = EnvironmentalClassificationResult.Classified(
            mode = tracked.mode,
            shelterIntensity = if (tracked.mode == EnvironmentMode.SHELTERED) {
                (1.0 - tracked.smoothedScores.exposure).coerceIn(0.0, 1.0).toFloat()
            } else {
                0.0f
            },
            diagnostics = diagnostics,
        )
        lastClassified = result
        return result
    }

    fun reset() {
        tracker.reset()
        lastClassified = null
    }

    private fun retainedOrInsufficient(): EnvironmentalClassificationResult {
        val previous = lastClassified
            ?: return EnvironmentalClassificationResult.NotClassified(NotClassifiedReason.INSUFFICIENT_EVIDENCE)
        return previous.copy(
            diagnostics = previous.diagnostics.copy(
                decisionBasis = DecisionBasis.RETAINED_INSUFFICIENT_EVIDENCE,
            ),
        ).also { lastClassified = it }
    }
}
