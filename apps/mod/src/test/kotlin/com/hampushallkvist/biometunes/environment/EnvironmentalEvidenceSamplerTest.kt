package com.hampushallkvist.biometunes.environment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvironmentalEvidenceSamplerTest {
    @Test
    fun `sampling work remains fixed when every point is loaded`() {
        val origin = BlockPoint(100, 64, -20)
        val source = CountingObservationSource(
            surfaceY = { _, _ -> 74 },
            blocksMotionAt = { true },
        )

        val evidence = assertNotNull(EnvironmentalEvidenceSampler.sample(origin, settings, source))

        assertEquals(10, source.skyLightCalls.size, "nine sky samples plus the player light read")
        assertEquals(2, source.skyLightCalls.count { it == origin })
        assertEquals(9, source.surfaceCalls.size)
        assertEquals(26, source.blocksMotionCalls.size)
        assertEquals(listOf(origin), source.blockLightCalls)
        assertEquals(9, evidence.skySampleCount)
        assertEquals(10.0, evidence.medianSurfaceDepth)
        assertEquals(1.0, evidence.enclosure)
    }

    @Test
    fun `maximum skylight and the no-leaves surface each expose sky`() {
        val origin = BlockPoint(0, 64, 0)
        val glassLike = CountingObservationSource(
            skyLightAt = { 15 },
            surfaceY = { _, _ -> 80 },
            blocksMotionAt = { true },
        )
        val leafLike = CountingObservationSource(
            skyLightAt = { 7 },
            surfaceY = { _, _ -> 63 },
        )

        val glassEvidence = assertNotNull(EnvironmentalEvidenceSampler.sample(origin, settings, glassLike))
        val leafEvidence = assertNotNull(EnvironmentalEvidenceSampler.sample(origin, settings, leafLike))

        assertTrue(glassEvidence.centerSkyExposed)
        assertEquals(1.0, glassEvidence.enclosure, "motion-blocking glass remains enclosure evidence")
        assertTrue(leafEvidence.centerSkyExposed)
    }

    @Test
    fun `unloaded sky positions are omitted and five valid positions are required`() {
        val origin = BlockPoint(0, 64, 0)
        val skyPoints = skyPoints(origin, settings.nearbyRadius)
        val fiveLoaded = CountingObservationSource(loaded = { it in skyPoints.take(5) })
        val fourLoaded = CountingObservationSource(loaded = { it in skyPoints.take(4) })

        assertNotNull(EnvironmentalEvidenceSampler.sample(origin, settings, fiveLoaded))
        assertEquals(6, fiveLoaded.skyLightCalls.size, "five sky samples plus the player light read")
        assertEquals(5, fiveLoaded.surfaceCalls.size)

        assertNull(EnvironmentalEvidenceSampler.sample(origin, settings, fourLoaded))
        assertEquals(4, fourLoaded.skyLightCalls.size)
        assertEquals(4, fourLoaded.surfaceCalls.size)
        assertTrue(fourLoaded.blocksMotionCalls.isEmpty())
        assertTrue(fourLoaded.blockLightCalls.isEmpty())
    }

    @Test
    fun `unloaded enclosure positions are omitted from its denominator`() {
        val origin = BlockPoint(0, 64, 0)
        val skyPoints = skyPoints(origin, settings.nearbyRadius).toSet()
        val soleEnclosurePoint = BlockPoint(2, 66, 2)
        val source = CountingObservationSource(
            loaded = { it in skyPoints || it == soleEnclosurePoint },
            blocksMotionAt = { it == soleEnclosurePoint },
        )

        val evidence = assertNotNull(EnvironmentalEvidenceSampler.sample(origin, settings, source))

        assertEquals(listOf(soleEnclosurePoint), source.blocksMotionCalls)
        assertEquals(1.0, evidence.enclosure)
    }

    private class CountingObservationSource(
        private val loaded: (BlockPoint) -> Boolean = { true },
        private val skyLightAt: (BlockPoint) -> Int = { 0 },
        private val blockLightAt: (BlockPoint) -> Int = { 0 },
        private val surfaceY: (Int, Int) -> Int = { _, _ -> 64 },
        private val blocksMotionAt: (BlockPoint) -> Boolean = { false },
    ) : EnvironmentalObservationSource {
        val skyLightCalls = mutableListOf<BlockPoint>()
        val blockLightCalls = mutableListOf<BlockPoint>()
        val surfaceCalls = mutableListOf<Pair<Int, Int>>()
        val blocksMotionCalls = mutableListOf<BlockPoint>()

        override fun isLoaded(point: BlockPoint) = loaded(point)

        override fun skyLight(point: BlockPoint): Int {
            skyLightCalls += point
            return skyLightAt(point)
        }

        override fun blockLight(point: BlockPoint): Int {
            blockLightCalls += point
            return blockLightAt(point)
        }

        override fun noLeavesSurfaceY(x: Int, z: Int): Int {
            surfaceCalls += x to z
            return surfaceY(x, z)
        }

        override fun blocksMotion(point: BlockPoint): Boolean {
            blocksMotionCalls += point
            return blocksMotionAt(point)
        }
    }

    private companion object {
        val settings = EnvironmentalSampling(nearbyRadius = 6, enclosureRadius = 2, surfaceDepthScale = 48)

        fun skyPoints(origin: BlockPoint, radius: Int) = buildList {
            add(origin)
            for (z in listOf(-radius, 0, radius)) {
                for (x in listOf(-radius, 0, radius)) {
                    if (x != 0 || z != 0) add(BlockPoint(origin.x + x, origin.y, origin.z + z))
                }
            }
        }
    }
}
