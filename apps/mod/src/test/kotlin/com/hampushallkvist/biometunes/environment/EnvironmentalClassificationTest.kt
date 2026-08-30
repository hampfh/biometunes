package com.hampushallkvist.biometunes.environment

import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.catalog.TrackPool
import com.hampushallkvist.biometunes.catalog.WeightedTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EnvironmentalClassificationTest {
    @Test
    fun `boss no-profile and native underground biome bypass sampling`() {
        val classification = EnvironmentalClassification()
        val source = CountingSource()

        assertReason(
            NotClassifiedReason.BOSS_ENCOUNTER,
            classification.classify(input(profile = profile, boss = true), source),
        )
        assertReason(
            NotClassifiedReason.NO_PROFILE,
            classification.classify(input(profile = null), source),
        )
        assertReason(
            NotClassifiedReason.NATIVE_UNDERGROUND_BIOME,
            classification.classify(input(profile = profile, biomeId = "minecraft:deep_dark"), source),
        )
        assertEquals(0, source.observationCalls)
    }

    @Test
    fun `insufficient evidence retains a prior classification but cannot invent one`() {
        val classification = EnvironmentalClassification()
        val undergroundSource = CountingSource(
            surfaceY = 128,
            blocksMotion = true,
        )
        val unavailableSource = CountingSource(loaded = false)

        val initial = assertIs<EnvironmentalClassificationResult.Classified>(
            classification.classify(input(profile = profile), undergroundSource),
        )
        assertEquals(EnvironmentMode.SUBTERRANEAN, initial.mode)

        val retained = assertIs<EnvironmentalClassificationResult.Classified>(
            classification.classify(input(profile = profile), unavailableSource),
        )
        assertEquals(initial.mode, retained.mode)
        assertEquals(DecisionBasis.RETAINED_INSUFFICIENT_EVIDENCE, retained.diagnostics.decisionBasis)

        classification.reset()
        assertReason(
            NotClassifiedReason.INSUFFICIENT_EVIDENCE,
            classification.classify(input(profile = profile), unavailableSource),
        )
    }

    @Test
    fun `sheltered intensity derives from smoothed exposure`() {
        val result = assertIs<EnvironmentalClassificationResult.Classified>(
            EnvironmentalClassification().classify(
                input(profile = profile),
                CountingSource(surfaceY = 65),
            ),
        )

        assertEquals(EnvironmentMode.SHELTERED, result.mode)
        assertEquals(
            (1.0 - result.diagnostics.smoothedScores.exposure).toFloat(),
            result.shelterIntensity,
        )
        assertTrue(result.shelterIntensity in 0.0f..1.0f)
    }

    private fun assertReason(
        expected: NotClassifiedReason,
        result: EnvironmentalClassificationResult,
    ) {
        assertEquals(expected, assertIs<EnvironmentalClassificationResult.NotClassified>(result).reason)
    }

    private class CountingSource(
        private val loaded: Boolean = true,
        private val surfaceY: Int = 64,
        private val blocksMotion: Boolean = false,
    ) : EnvironmentalObservationSource {
        var observationCalls = 0

        override fun isLoaded(point: BlockPoint): Boolean {
            observationCalls++
            return loaded
        }

        override fun skyLight(point: BlockPoint): Int {
            observationCalls++
            return 0
        }

        override fun blockLight(point: BlockPoint): Int {
            observationCalls++
            return 0
        }

        override fun noLeavesSurfaceY(x: Int, z: Int): Int {
            observationCalls++
            return surfaceY
        }

        override fun blocksMotion(point: BlockPoint): Boolean {
            observationCalls++
            return blocksMotion
        }
    }

    private companion object {
        val profile = EnvironmentalProfile(
            nativeUndergroundBiomes = setOf("minecraft:deep_dark"),
            subterraneanTracks = TrackPool(listOf(WeightedTrack(TrackId("underground"), 9))),
            sampling = EnvironmentalSampling(6, 2, 48),
            weights = EnvironmentalWeights(1.0, 0.25, 0.25, 1.0, 0.25, 0.15, 0.05),
            thresholds = EnvironmentalThresholds(0.65, 0.45, 0.70, 0.55, 0.25, 0.40),
            smoothingSeconds = 2.0,
            shelteredAudio = ShelteredAudioSettings(0.85f, 0.35f),
        )

        fun input(
            profile: EnvironmentalProfile?,
            boss: Boolean = false,
            biomeId: String = "minecraft:plains",
        ) = EnvironmentalClassificationInput(
            dimensionId = "minecraft:overworld",
            biomeId = biomeId,
            bossEncounterActive = boss,
            profile = profile,
            origin = BlockPoint(0, 64, 0),
            elapsedSeconds = 0.5,
        )
    }
}
