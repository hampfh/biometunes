package com.hampushallkvist.biometunes.client

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAlReverbSendTest {
    @Test
    fun `one lazy room reverb is shared and send gain writes are deduplicated`() {
        val backend = FakeReverbBackend()
        val reverb = OpenAlReverbSend(backend) { throw it }

        reverb.apply(source = 10, send = 0.12f)
        reverb.apply(source = 11, send = 0.12f)
        reverb.apply(source = 10, send = 0.12f)

        assertEquals(1, backend.supportChecks)
        assertEquals(listOf(ReverbResources(effect = 1, slot = 101)), backend.created)
        assertEquals(listOf(101 to 0.12f), backend.sendGainWrites)
        assertEquals(listOf(10 to 101, 11 to 101), backend.attachments)
        assertEquals(ReverbDiagnostics(ReverbStatus.EFX_ACTIVE, 0.12f, 2), reverb.diagnostics)
    }

    @Test
    fun `zero send detaches a source and reset deletes shared resources`() {
        val backend = FakeReverbBackend()
        val reverb = OpenAlReverbSend(backend) { throw it }
        reverb.apply(10, 0.1f)

        reverb.apply(10, 0.0f)

        assertEquals(listOf(10), backend.detached)
        assertEquals(ReverbDiagnostics(ReverbStatus.INACTIVE, 0.0f, 0), reverb.diagnostics)

        reverb.reset(intArrayOf(10, 11))

        assertEquals(listOf(ReverbResources(effect = 1, slot = 101)), backend.deleted)
    }

    @Test
    fun `unsupported and failed backends remain dry until reset`() {
        val unsupportedBackend = FakeReverbBackend(isSupported = false)
        val unsupported = OpenAlReverbSend(unsupportedBackend) { throw it }
        unsupported.apply(10, 0.1f)
        unsupported.apply(11, 0.12f)
        assertEquals(1, unsupportedBackend.supportChecks)
        assertEquals(ReverbStatus.UNSUPPORTED, unsupported.diagnostics.status)

        val failures = mutableListOf<Throwable>()
        val failingBackend = FakeReverbBackend(failOnAttachInitially = true)
        val failed = OpenAlReverbSend(failingBackend, failures::add)
        failed.apply(10, 0.1f)
        failed.apply(11, 0.12f)
        assertEquals(1, failures.size)
        assertEquals(1, failingBackend.attachments.size)
        assertEquals(ReverbStatus.ERROR, failed.diagnostics.status)

        failed.reset(intArrayOf())
        failingBackend.failOnAttach = false
        failed.apply(12, 0.08f)
        assertEquals(ReverbStatus.EFX_ACTIVE, failed.diagnostics.status)
    }

    @Test
    fun `a changed OpenAL context recreates reverb without deleting old context resources`() {
        val backend = FakeReverbBackend()
        val reverb = OpenAlReverbSend(backend) { throw it }
        reverb.apply(10, 0.1f)

        backend.context = Any()
        reverb.apply(11, 0.08f)

        assertEquals(
            listOf(ReverbResources(1, 101), ReverbResources(2, 102)),
            backend.created,
        )
        assertEquals(emptyList(), backend.deleted)
        assertEquals(listOf(10 to 101, 11 to 102), backend.attachments)
        assertEquals(ReverbDiagnostics(ReverbStatus.EFX_ACTIVE, 0.08f, 1), reverb.diagnostics)
    }

    private class FakeReverbBackend(
        private var isSupported: Boolean = true,
        private var failOnAttachInitially: Boolean = false,
    ) : ReverbBackend {
        var context: Any = Any()
        var failOnAttach = failOnAttachInitially
        var supportChecks = 0
        val created = mutableListOf<ReverbResources>()
        val sendGainWrites = mutableListOf<Pair<Int, Float>>()
        val attachments = mutableListOf<Pair<Int, Int>>()
        val detached = mutableListOf<Int>()
        val deleted = mutableListOf<ReverbResources>()

        override fun contextToken(): Any = context

        override fun supported(): Boolean {
            supportChecks++
            return isSupported
        }

        override fun create(): ReverbResources = ReverbResources(
            effect = created.size + 1,
            slot = created.size + 101,
        ).also(created::add)

        override fun setSendGain(slot: Int, value: Float) {
            sendGainWrites += slot to value
        }

        override fun attach(source: Int, slot: Int) {
            attachments += source to slot
            if (failOnAttach) error("attach failed")
        }

        override fun detach(source: Int) {
            detached += source
        }

        override fun delete(resources: ReverbResources) {
            deleted += resources
        }
    }
}
