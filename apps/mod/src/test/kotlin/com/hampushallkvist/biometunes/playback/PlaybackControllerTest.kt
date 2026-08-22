package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackControllerTest {
    @Test
    fun `equal power gains retain full endpoint power and avoid a midpoint dip`() {
        // Catches mutations that swap the sine and cosine curves or use linear fade gains.
        val start = equalPowerGains(0.0)
        assertEquals(1f, start.outgoing, 0.0001f)
        assertEquals(0f, start.incoming, 0.0001f)

        val midpoint = equalPowerGains(0.5)
        assertEquals(0.7071f, midpoint.outgoing, 0.0002f)
        assertEquals(0.7071f, midpoint.incoming, 0.0002f)

        val end = equalPowerGains(1.0)
        assertEquals(0f, end.outgoing, 0.0001f)
        assertEquals(1f, end.incoming, 0.0001f)
    }

    @Test
    fun `idle to playing starts the desired track at normalized configured volume`() {
        // Catches a mutation that starts initial playback muted, unscaled, or above the allowed gain.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)

        controller.update(trackA, PlaybackOptions(volume = 1.4f, crossfadeTicks = 100))

        assertEquals(setOf(trackA.id), adapter.ownedTrackIds)
        assertEquals(1f, adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `playing update with the same TrackId retains its handle and applies the latest volume`() {
        // Catches object-identity comparison, unnecessary restart, and stale volume application.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        controller.update(trackA, PlaybackOptions(volume = 0.8f, crossfadeTicks = 100))

        controller.update(track("a"), PlaybackOptions(volume = 0.35f, crossfadeTicks = 100))

        assertEquals(listOf(trackA.id), adapter.starts)
        assertEquals(setOf(trackA.id), adapter.ownedTrackIds)
        assertEquals(0.35f, adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `zero duration switch disposes the old handle and starts the new track at full volume`() {
        // Catches a mutation that leaves the outgoing handle alive or starts an immediate switch muted.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        controller.update(trackA, PlaybackOptions(volume = 0.8f, crossfadeTicks = 100))

        controller.update(trackB, PlaybackOptions(volume = 0.6f, crossfadeTicks = 0))

        assertEquals(listOf(trackA.id, trackB.id), adapter.starts)
        assertEquals(listOf(trackA.id), adapter.stops)
        assertEquals(setOf(trackB.id), adapter.ownedTrackIds)
        assertEquals(0.6f, adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `ordinary 100 tick fade advances to equal power midpoint and completes on tick 100`() {
        // Catches linear gains, off-by-one progress, and failure to dispose the completed outgoing handle.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)

        repeat(50) { controller.update(trackB, options) }

        assertEquals(setOf(trackA.id, trackB.id), adapter.ownedTrackIds)
        assertEquals(0.7071f, adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(0.7071f, adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(adapter, expected = 2)

        repeat(50) { controller.update(trackB, options) }

        assertEquals(listOf(trackA.id), adapter.stops)
        assertEquals(setOf(trackB.id), adapter.ownedTrackIds)
        assertEquals(1f, adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `crossfade applies current user volume to both unscaled mix gains`() {
        // Catches a mutation that volume-scales only the incoming or outgoing side of a fade.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        controller.update(trackA, PlaybackOptions(volume = 1f, crossfadeTicks = 100))
        controller.update(trackB, PlaybackOptions(volume = 0.9f, crossfadeTicks = 100))

        repeat(50) {
            controller.update(trackB, PlaybackOptions(volume = 0.4f, crossfadeTicks = 100))
        }

        assertEquals(0.28284f, adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(0.28284f, adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(adapter, expected = 2)
    }

    @Test
    fun `third track interruption stops the quieter handle before retaining the louder origin`() {
        // Catches a mutation that retains the quieter side or leaks a third live handle on interruption.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)
        repeat(75) { controller.update(trackB, options) }
        val retainedGain = adapter.gainFor(trackB.id)

        controller.update(trackC, options)

        assertEquals(listOf(trackA.id, trackB.id, trackC.id), adapter.starts)
        assertEquals(listOf(trackA.id), adapter.stops)
        assertEquals(setOf(trackB.id, trackC.id), adapter.ownedTrackIds)
        assertEquals(retainedGain, adapter.gainFor(trackB.id), 0.0001f)
        assertEquals(0f, adapter.gainFor(trackC.id), 0.0001f)
        assertHandleBound(adapter, expected = 2)
    }

    @Test
    fun `retargeting the existing outgoing track preserves both gains and reverses toward it`() {
        // Catches mutations that restart an existing OGG, fail to swap roles, or reset retarget origins.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)
        repeat(25) { controller.update(trackB, options) }
        val aOrigin = adapter.gainFor(trackA.id)
        val bOrigin = adapter.gainFor(trackB.id)

        controller.update(track("a"), options)

        assertEquals(listOf(trackA.id, trackB.id), adapter.starts)
        assertTrue(adapter.stops.isEmpty())
        assertEquals(aOrigin, adapter.gainFor(trackA.id), 0.0001f)
        assertEquals(bOrigin, adapter.gainFor(trackB.id), 0.0001f)
        assertHandleBound(adapter, expected = 2)

        repeat(50) { controller.update(trackA, options) }

        assertEquals(0.9777f, adapter.gainFor(trackA.id), 0.0003f)
        assertEquals(0.2706f, adapter.gainFor(trackB.id), 0.0003f)
        assertHandleBound(adapter, expected = 2)

        repeat(50) { controller.update(trackA, options) }

        assertEquals(listOf(trackB.id), adapter.stops)
        assertEquals(setOf(trackA.id), adapter.ownedTrackIds)
        assertEquals(1f, adapter.gainFor(trackA.id), 0.0001f)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `ended desired instance is disposed and restarted immediately without a duration timer`() {
        // Catches a mutation that trusts a stale Playing handle or waits for track-duration metadata.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 0.7f, crossfadeTicks = 100)
        controller.update(trackA, options)
        adapter.end(trackA.id)

        controller.update(track("a"), options)

        assertEquals(listOf(trackA.id, trackA.id), adapter.starts)
        assertEquals(listOf(trackA.id), adapter.stops)
        assertEquals(setOf(trackA.id), adapter.ownedTrackIds)
        assertTrue(adapter.isTrackPlaying(trackA.id))
        assertEquals(0.7f, adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `failed initial start remains idle and explicitly releases vanilla music`() {
        // Catches a mutation that claims music ownership when no custom handle was created.
        val adapter = FakeAudioAdapter()
        adapter.setVanillaMusicSuppressed(true)
        adapter.rejectedTrackIds += trackA.id
        val controller = PlaybackController(adapter)

        controller.update(trackA, PlaybackOptions(volume = 0.8f, crossfadeTicks = 100))

        assertEquals(listOf(trackA.id), adapter.starts)
        assertTrue(adapter.stops.isEmpty())
        assertFalse(adapter.vanillaMusicSuppressed)
        assertEquals(false, adapter.suppressionChanges.last())
        assertHandleBound(adapter, expected = 0)
    }

    @Test
    fun `failed incoming start stops the retained outgoing handle and releases vanilla music`() {
        // Catches a mutation that remains Playing and retries transitions while claiming vanilla ownership.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 0.8f, crossfadeTicks = 100)
        controller.update(trackA, options)
        adapter.rejectedTrackIds += trackB.id

        controller.update(trackB, options)

        assertEquals(listOf(trackA.id, trackB.id), adapter.starts)
        assertEquals(listOf(trackA.id), adapter.stops)
        assertFalse(adapter.vanillaMusicSuppressed)
        assertEquals(false, adapter.suppressionChanges.last())
        assertHandleBound(adapter, expected = 0)
    }

    @Test
    fun `stop disposes both crossfade handles and releases vanilla music idempotently`() {
        // Catches a mutation that cleans up only one state-owned handle or leaves suppression raised.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)
        repeat(25) { controller.update(trackB, options) }

        controller.stop()
        controller.stop()

        assertEquals(listOf(trackA.id, trackB.id), adapter.stops)
        assertFalse(adapter.vanillaMusicSuppressed)
        assertEquals(false, adapter.suppressionChanges.last())
        assertHandleBound(adapter, expected = 0)
    }

    @Test
    fun `ordinary fade duration change immediately divides current elapsed ticks by the new duration`() {
        // Catches a mutation that captures the original duration or rescales elapsed ticks on config change.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val original = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, original)
        controller.update(trackB, original)
        repeat(24) { controller.update(trackB, original) }

        controller.update(trackB, PlaybackOptions(volume = 1f, crossfadeTicks = 50))

        assertEquals(0.7071f, adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(0.7071f, adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(adapter, expected = 2)
    }

    @Test
    fun `changing an active fade duration to zero completes it immediately`() {
        // Catches a mutation that divides by zero or waits another captured-duration tick to finish.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)
        repeat(25) { controller.update(trackB, options) }

        controller.update(trackB, PlaybackOptions(volume = 0.45f, crossfadeTicks = 0))

        assertEquals(listOf(trackA.id), adapter.stops)
        assertEquals(setOf(trackB.id), adapter.ownedTrackIds)
        assertEquals(0.45f, adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `zero duration third track interruption disposes both old handles before immediate playback`() {
        // Catches a mutation that applies zero duration only when the desired track is already in the fade.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)
        repeat(75) { controller.update(trackB, options) }

        controller.update(trackC, PlaybackOptions(volume = 0.5f, crossfadeTicks = 0))

        assertEquals(listOf(trackA.id, trackB.id, trackC.id), adapter.starts)
        assertEquals(setOf(trackA.id, trackB.id), adapter.stops.toSet())
        assertEquals(setOf(trackC.id), adapter.ownedTrackIds)
        assertEquals(0.5f, adapter.gainFor(trackC.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    @Test
    fun `ended desired incoming loop disposes both fade owners and restarts desired immediately`() {
        // Catches a mutation that advances gains on a dead incoming handle until the fade timer expires.
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)
        val options = PlaybackOptions(volume = 0.65f, crossfadeTicks = 100)
        controller.update(trackA, options)
        controller.update(trackB, options)
        repeat(25) { controller.update(trackB, options) }
        adapter.end(trackB.id)

        controller.update(trackB, options)

        assertEquals(listOf(trackA.id, trackB.id, trackB.id), adapter.starts)
        assertEquals(listOf(trackA.id, trackB.id), adapter.stops)
        assertEquals(setOf(trackB.id), adapter.ownedTrackIds)
        assertTrue(adapter.isTrackPlaying(trackB.id))
        assertEquals(0.65f, adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(adapter.vanillaMusicSuppressed)
        assertHandleBound(adapter, expected = 1)
    }

    private fun assertHandleBound(adapter: FakeAudioAdapter, expected: Int) {
        assertEquals(expected, adapter.ownedHandleCount)
        assertTrue(adapter.ownedHandleCount in 0..2)
        assertTrue(adapter.maximumOwnedHandleCount in 0..2)
    }

    private companion object {
        val trackA = track("a")
        val trackB = track("b")
        val trackC = track("c")

        fun track(id: String) = TrackDefinition(
            id = TrackId(id),
            soundEvent = "biometunes:music.$id",
            title = "Track $id",
            artist = "Artist $id",
        )
    }
}

private class FakeAudioAdapter : AudioAdapter {
    private data class FakeAudioHandle(val serial: Int) : AudioHandle

    private data class Instance(
        val track: TrackDefinition,
        var gain: Float,
        var playing: Boolean = true,
    )

    private var nextSerial = 0
    private val instances = linkedMapOf<FakeAudioHandle, Instance>()
    var maximumOwnedHandleCount = 0
        private set
    val rejectedTrackIds = mutableSetOf<TrackId>()
    val starts = mutableListOf<TrackId>()
    val gainChanges = mutableListOf<Pair<TrackId, Float>>()
    val stops = mutableListOf<TrackId>()
    val suppressionChanges = mutableListOf<Boolean>()

    var vanillaMusicSuppressed: Boolean = false
        private set

    val ownedTrackIds: Set<TrackId>
        get() = instances.values.mapTo(linkedSetOf()) { it.track.id }

    val ownedHandleCount: Int
        get() = instances.size

    override fun start(track: TrackDefinition, initialGain: Float): AudioHandle? {
        starts += track.id
        if (track.id in rejectedTrackIds) return null
        return FakeAudioHandle(nextSerial++).also { handle ->
            instances[handle] = Instance(track, initialGain)
            maximumOwnedHandleCount = maxOf(maximumOwnedHandleCount, instances.size)
        }
    }

    override fun setGain(handle: AudioHandle, gain: Float) {
        val instance = instances.getValue(handle as FakeAudioHandle)
        instance.gain = gain
        gainChanges += instance.track.id to gain
    }

    override fun stop(handle: AudioHandle) {
        val instance = instances.remove(handle as FakeAudioHandle) ?: return
        stops += instance.track.id
    }

    override fun isPlaying(handle: AudioHandle): Boolean =
        instances[handle as FakeAudioHandle]?.playing == true

    override fun setVanillaMusicSuppressed(suppressed: Boolean) {
        vanillaMusicSuppressed = suppressed
        suppressionChanges += suppressed
    }

    fun gainFor(trackId: TrackId): Float =
        instances.values.single { it.track.id == trackId }.gain

    fun end(trackId: TrackId) {
        instances.values.single { it.track.id == trackId }.playing = false
    }

    fun isTrackPlaying(trackId: TrackId): Boolean =
        instances.values.single { it.track.id == trackId }.playing
}
