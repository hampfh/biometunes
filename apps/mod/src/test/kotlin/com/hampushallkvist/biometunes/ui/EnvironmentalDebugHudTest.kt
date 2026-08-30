package com.hampushallkvist.biometunes.ui

import com.hampushallkvist.biometunes.client.AudioFilterStatus
import com.hampushallkvist.biometunes.client.AudioTreatmentDiagnostics
import com.hampushallkvist.biometunes.environment.DecisionBasis
import com.hampushallkvist.biometunes.environment.EnvironmentConstraints
import com.hampushallkvist.biometunes.environment.EnvironmentMode
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult
import com.hampushallkvist.biometunes.environment.EnvironmentalDiagnostics
import com.hampushallkvist.biometunes.environment.EnvironmentalEvidence
import com.hampushallkvist.biometunes.environment.EnvironmentalScores
import com.hampushallkvist.biometunes.environment.NotClassifiedReason
import com.hampushallkvist.biometunes.selection.PlayerContext
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentalDebugHudTest {
    @Test
    fun `classified lines expose the complete cached diagnostic snapshot`() {
        val context = context(
            EnvironmentalClassificationResult.Classified(
                mode = EnvironmentMode.SHELTERED,
                shelterIntensity = 0.7f,
                diagnostics = EnvironmentalDiagnostics(
                    profileDimensionId = "minecraft:overworld",
                    evidence = EnvironmentalEvidence(
                        centerSkyExposed = false,
                        skyExposedSamples = 2,
                        skySampleCount = 9,
                        medianSurfaceDepth = 12.0,
                        enclosure = 0.75,
                        skyLight = 3,
                        blockLight = 7,
                    ),
                    rawScores = EnvironmentalScores(0.2, 0.4),
                    smoothedScores = EnvironmentalScores(0.3, 0.5),
                    constraints = EnvironmentConstraints(outsideAllowed = false, subterraneanAllowed = true),
                    decisionBasis = DecisionBasis.WEIGHTED_SCORES,
                ),
            ),
        )
        val audio = AudioTreatmentDiagnostics(
            status = AudioFilterStatus.EFX_ACTIVE,
            requestedHighFrequencyGain = 0.45f,
            attachedVoiceCount = 2,
            effectiveGainMultiplier = 0.9f,
        )

        assertEquals(
            listOf(
                "Environmental Classification",
                "Dimension: minecraft:overworld | Profile: minecraft:overworld",
                "Status: Classified | Mode: SHELTERED | Basis: WEIGHTED_SCORES",
                "Constraints: Outside=false | Subterranean=true",
                "Raw: exposure=0.200 depth=0.400",
                "Smoothed: exposure=0.300 depth=0.500",
                "Sky exposed: 2/9 | Median depth: 12.0 | Enclosure: 0.750",
                "Light: sky=3 block=7 | Shelter: 0.700",
                "Treatment: gain=0.900 high-frequency=0.450 | EFX_ACTIVE | Voices=2",
            ),
            EnvironmentalDebugLines.format(context, audio),
        )
    }

    @Test
    fun `not-classified lines use every canonical reason`() {
        NotClassifiedReason.entries.forEach { reason ->
            assertEquals(
                listOf(
                    "Environmental Classification",
                    "Dimension: minecraft:overworld",
                    "Status: NotClassified | Reason: ${reason.name}",
                ),
                EnvironmentalDebugLines.format(
                    context(EnvironmentalClassificationResult.NotClassified(reason)),
                    AudioTreatmentDiagnostics(AudioFilterStatus.INACTIVE, 1.0f, 0),
                ),
            )
        }
    }

    private fun context(result: EnvironmentalClassificationResult) = PlayerContext(
        biomeId = "minecraft:plains",
        biomeTags = emptySet(),
        dimensionId = "minecraft:overworld",
        boss = null,
        environmentalClassification = result,
    )
}
