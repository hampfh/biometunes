package com.hampushallkvist.biometunes.client

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAlLowPassFilterTest {
    @Test
    fun `one lazy filter is shared and parameter writes are deduplicated`() {
        val backend = FakeLowPassBackend()
        val filter = OpenAlLowPassFilter(backend) { throw it }

        filter.apply(source = 10, highFrequencyGain = 0.4f)
        filter.apply(source = 11, highFrequencyGain = 0.4f)
        filter.apply(source = 10, highFrequencyGain = 0.4f)

        assertEquals(1, backend.supportChecks)
        assertEquals(listOf(1), backend.created)
        assertEquals(listOf(1 to 0.4f), backend.parameterWrites)
        assertEquals(listOf(10 to 1, 11 to 1), backend.attachments)
        assertEquals(AudioTreatmentDiagnostics(AudioFilterStatus.EFX_ACTIVE, 0.4f, 2), filter.diagnostics)
    }

    @Test
    fun `changed filter values are reapplied once to every attached source`() {
        val backend = FakeLowPassBackend()
        val filter = OpenAlLowPassFilter(backend) { throw it }

        filter.apply(source = 10, highFrequencyGain = 0.8f)
        filter.apply(source = 11, highFrequencyGain = 0.8f)
        filter.apply(source = 10, highFrequencyGain = 0.2f)
        filter.apply(source = 11, highFrequencyGain = 0.2f)
        filter.apply(source = 10, highFrequencyGain = 0.2f)

        assertEquals(listOf(1 to 0.8f, 1 to 0.2f), backend.parameterWrites)
        assertEquals(
            listOf(10 to 1, 11 to 1, 10 to 1, 11 to 1),
            backend.attachments,
        )
        assertEquals(AudioTreatmentDiagnostics(AudioFilterStatus.EFX_ACTIVE, 0.2f, 2), filter.diagnostics)
    }

    @Test
    fun `gain one detaches a source and reset disposes state for a new attempt`() {
        val backend = FakeLowPassBackend()
        val filter = OpenAlLowPassFilter(backend) { throw it }
        filter.apply(10, 0.3f)
        filter.apply(10, 1.0f)

        assertEquals(listOf(10), backend.detached)
        assertEquals(AudioTreatmentDiagnostics(AudioFilterStatus.INACTIVE, 1.0f, 0), filter.diagnostics)

        filter.reset(intArrayOf(10, 11))
        filter.apply(12, 0.2f)

        assertEquals(listOf(1), backend.deleted)
        assertEquals(2, backend.supportChecks)
        assertEquals(listOf(1, 2), backend.created)
    }

    @Test
    fun `unsupported and failed backends permanently fall back until reset`() {
        val unsupportedBackend = FakeLowPassBackend(isSupported = false)
        val unsupported = OpenAlLowPassFilter(unsupportedBackend) { throw it }
        unsupported.apply(10, 0.5f)
        unsupported.apply(11, 0.4f)
        assertEquals(1, unsupportedBackend.supportChecks)
        assertEquals(AudioFilterStatus.GAIN_ONLY_UNSUPPORTED, unsupported.diagnostics.status)

        val failures = mutableListOf<Throwable>()
        val failingBackend = FakeLowPassBackend(failOnAttachInitially = true)
        val failed = OpenAlLowPassFilter(failingBackend, failures::add)
        failed.apply(10, 0.5f)
        failed.apply(11, 0.4f)
        assertEquals(1, failures.size)
        assertEquals(1, failingBackend.attachments.size)
        assertEquals(AudioFilterStatus.GAIN_ONLY_ERROR, failed.diagnostics.status)

        failed.reset(intArrayOf())
        failingBackend.failOnAttach = false
        failed.apply(12, 0.2f)
        assertEquals(AudioFilterStatus.EFX_ACTIVE, failed.diagnostics.status)
    }

    @Test
    fun `a changed OpenAL context forgets the old filter without deleting it`() {
        val backend = FakeLowPassBackend()
        val filter = OpenAlLowPassFilter(backend) { throw it }
        filter.apply(10, 0.5f)

        backend.context = Any()
        filter.apply(11, 0.25f)

        assertEquals(listOf(1, 2), backend.created)
        assertEquals(emptyList(), backend.deleted)
        assertEquals(listOf(10 to 1, 11 to 2), backend.attachments)
        assertEquals(AudioTreatmentDiagnostics(AudioFilterStatus.EFX_ACTIVE, 0.25f, 1), filter.diagnostics)
    }

    private class FakeLowPassBackend(
        private var isSupported: Boolean = true,
        private var failOnAttachInitially: Boolean = false,
    ) : LowPassBackend {
        var context: Any = Any()
        var failOnAttach = failOnAttachInitially
        var supportChecks = 0
        val created = mutableListOf<Int>()
        val parameterWrites = mutableListOf<Pair<Int, Float>>()
        val attachments = mutableListOf<Pair<Int, Int>>()
        val detached = mutableListOf<Int>()
        val deleted = mutableListOf<Int>()

        override fun contextToken(): Any = context

        override fun supported(): Boolean {
            supportChecks++
            return isSupported
        }

        override fun create(): Int = (created.size + 1).also(created::add)

        override fun setHighFrequencyGain(filter: Int, value: Float) {
            parameterWrites += filter to value
        }

        override fun attach(source: Int, filter: Int) {
            attachments += source to filter
            if (failOnAttach) error("attach failed")
        }

        override fun detach(source: Int) {
            detached += source
        }

        override fun delete(filter: Int) {
            deleted += filter
        }
    }
}
