package com.hampushallkvist.biometunes.environment

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EnvironmentalTrackerTest {
    @Test
    fun `outside and subterranean use separate enter and exit thresholds`() {
        val outsideTracker = EnvironmentalTracker()
        assertEquals(EnvironmentMode.OUTSIDE, outsideTracker.update(scores(0.8, 0.1), bothAllowed, null, thresholds, 0.0, 0.5).mode)
        assertEquals(EnvironmentMode.OUTSIDE, outsideTracker.update(scores(0.5, 0.1), bothAllowed, null, thresholds, 0.0, 0.5).mode)
        assertEquals(EnvironmentMode.SHELTERED, outsideTracker.update(scores(0.4, 0.1), bothAllowed, null, thresholds, 0.0, 0.5).mode)

        val subterraneanTracker = EnvironmentalTracker()
        assertEquals(EnvironmentMode.SUBTERRANEAN, subterraneanTracker.update(scores(0.1, 0.8), bothAllowed, null, thresholds, 0.0, 0.5).mode)
        assertEquals(EnvironmentMode.SUBTERRANEAN, subterraneanTracker.update(scores(0.3, 0.6), bothAllowed, null, thresholds, 0.0, 0.5).mode)
        assertEquals(EnvironmentMode.SHELTERED, subterraneanTracker.update(scores(0.3, 0.5), bothAllowed, null, thresholds, 0.0, 0.5).mode)
    }

    @Test
    fun `current hard constraints immediately remove impossible modes`() {
        val tracker = EnvironmentalTracker()
        tracker.update(scores(0.9, 0.1), bothAllowed, null, thresholds, 0.0, 0.5)

        val result = tracker.update(
            scores(0.9, 0.1),
            EnvironmentConstraints(outsideAllowed = false, subterraneanAllowed = false),
            null,
            thresholds,
            2.0,
            0.5,
        )

        assertEquals(EnvironmentMode.SHELTERED, result.mode)
    }

    @Test
    fun `smoothing uses elapsed time and zero duration disables it`() {
        val tracker = EnvironmentalTracker()
        tracker.update(scores(0.0, 0.0), bothAllowed, null, thresholds, 2.0, 0.5)

        val smoothed = tracker.update(scores(1.0, 1.0), bothAllowed, null, thresholds, 2.0, 2.0)
        val expectedAlpha = 1.0 - exp(-1.0)
        assertEquals(expectedAlpha, smoothed.smoothedScores.exposure, absoluteTolerance = 0.000_001)
        assertEquals(expectedAlpha, smoothed.smoothedScores.depth, absoluteTolerance = 0.000_001)

        val unsmoothed = tracker.update(scores(0.2, 0.3), bothAllowed, null, thresholds, 0.0, 0.5)
        assertEquals(scores(0.2, 0.3), unsmoothed.smoothedScores)
    }

    @Test
    fun `decisive signals obey constraints and reset clears history`() {
        val tracker = EnvironmentalTracker()
        val forbiddenDecisive = tracker.update(
            scores(0.8, 0.8),
            EnvironmentConstraints(outsideAllowed = true, subterraneanAllowed = false),
            EnvironmentMode.SUBTERRANEAN,
            thresholds,
            0.0,
            0.5,
        )
        assertNotEquals(EnvironmentMode.SUBTERRANEAN, forbiddenDecisive.mode)

        tracker.reset()
        val afterReset = tracker.update(scores(0.5, 0.1), bothAllowed, null, thresholds, 2.0, 0.5)
        assertEquals(EnvironmentMode.SHELTERED, afterReset.mode)
        assertEquals(scores(0.5, 0.1), afterReset.smoothedScores)
    }

    private companion object {
        val thresholds = EnvironmentalThresholds(0.65, 0.45, 0.70, 0.55, 0.25, 0.40)
        val bothAllowed = EnvironmentConstraints(outsideAllowed = true, subterraneanAllowed = true)

        fun scores(exposure: Double, depth: Double) = EnvironmentalScores(exposure, depth)
    }
}
