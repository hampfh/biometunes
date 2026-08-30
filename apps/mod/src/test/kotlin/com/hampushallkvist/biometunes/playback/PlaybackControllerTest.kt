package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackControllerTest {
    @Test
    fun `unchanged treatment applies gain low pass and reverb send to every live voice`() {
        val session = Session()
        val treated = PlaybackOptions(
            volume = 0.5f,
            crossfadeTicks = 100,
            treatment = AudioTreatment(gainMultiplier = 0.8f, highFrequencyGain = 0.4f, reverbSend = 0.12f),
        )
        session.tick(trackA, treated)
        session.tick(trackB, treated)
        repeat(50) { session.tick(trackB, treated) }

        assertEquals(equalPowerGain(0.5) * 0.5f * 0.8f, session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(equalPowerGain(0.5) * 0.5f * 0.8f, session.adapter.gainFor(trackB.id), 0.0002f)
        assertEquals(0.4f, session.adapter.lowPassFor(trackA.id))
        assertEquals(0.4f, session.adapter.lowPassFor(trackB.id))
        assertEquals(0.12f, session.adapter.reverbFor(trackA.id))
        assertEquals(0.12f, session.adapter.reverbFor(trackB.id))
    }

    @Test
    fun `silence fades retain current treatment until voices stop`() {
        val session = Session()
        val treated = PlaybackOptions(
            volume = 0.6f,
            crossfadeTicks = 100,
            treatment = AudioTreatment(gainMultiplier = 0.75f, highFrequencyGain = 0.2f, reverbSend = 0.1f),
        )
        session.tick(trackA, treated)

        session.controller.updateSilence(PlaybackOptions(volume = 0.6f, crossfadeTicks = 100))

        assertEquals(equalPowerGain(0.99) * 0.6f * 0.75f, session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(0.2f, session.adapter.lowPassFor(trackA.id))
        assertEquals(0.1f, session.adapter.reverbFor(trackA.id))
    }

    @Test
    fun `equal power gain reaches both endpoints and holds constant power across a fade`() {
        // Catches mutations that invert the curve or use linear fade gains.
        assertEquals(0f, equalPowerGain(0.0), 0.0001f)
        assertEquals(0.7071f, equalPowerGain(0.5), 0.0002f)
        assertEquals(1f, equalPowerGain(1.0), 0.0001f)

        for (step in 0..10) {
            val progress = step / 10.0
            val outgoing = equalPowerGain(1.0 - progress)
            val incoming = equalPowerGain(progress)
            assertEquals(1f, outgoing * outgoing + incoming * incoming, 0.0002f)
        }
    }

    @Test
    fun `idle to playing starts the desired track at normalized configured volume`() {
        // Catches a mutation that starts initial playback muted, unscaled, or above the allowed gain.
        val session = Session()

        session.tick(trackA, PlaybackOptions(volume = 1.4f, crossfadeTicks = 100))

        assertEquals(setOf(trackA.id), session.adapter.ownedTrackIds)
        assertEquals(1f, session.adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `playing update with the same TrackId retains its handle and applies the latest volume`() {
        // Catches object-identity comparison, unnecessary restart, and stale volume application.
        val session = Session()
        session.tick(trackA, PlaybackOptions(volume = 0.8f, crossfadeTicks = 100))

        session.tick(track("a"), PlaybackOptions(volume = 0.35f, crossfadeTicks = 100))

        assertEquals(listOf(trackA.id), session.adapter.starts)
        assertEquals(setOf(trackA.id), session.adapter.ownedTrackIds)
        assertEquals(0.35f, session.adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `zero duration switch disposes the old handle and starts the new track at full volume`() {
        // Catches a mutation that leaves the outgoing handle alive or starts an immediate switch muted.
        val session = Session()
        session.tick(trackA, PlaybackOptions(volume = 0.8f, crossfadeTicks = 100))

        session.tick(trackB, PlaybackOptions(volume = 0.6f, crossfadeTicks = 0))

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.starts)
        assertEquals(listOf(trackA.id), session.adapter.stops)
        assertEquals(setOf(trackB.id), session.adapter.ownedTrackIds)
        assertEquals(0.6f, session.adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `ordinary 100 tick fade advances to equal power midpoint and completes on tick 100`() {
        // Catches linear gains, off-by-one progress, and failure to dispose the completed outgoing handle.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)

        repeat(50) { session.tick(trackB, options) }

        assertEquals(setOf(trackA.id, trackB.id), session.adapter.ownedTrackIds)
        assertEquals(0.7071f, session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(0.7071f, session.adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(session.adapter, expected = 2)

        repeat(50) { session.tick(trackB, options) }

        assertEquals(listOf(trackA.id), session.adapter.stops)
        assertEquals(setOf(trackB.id), session.adapter.ownedTrackIds)
        assertEquals(1f, session.adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
        session.assertNoAudibleJump()
    }

    @Test
    fun `crossfade applies current user volume to both unscaled mix gains`() {
        // Catches a mutation that volume-scales only the incoming or outgoing side of a fade.
        val session = Session()
        session.tick(trackA, PlaybackOptions(volume = 1f, crossfadeTicks = 100))
        session.tick(trackB, PlaybackOptions(volume = 0.9f, crossfadeTicks = 100))

        repeat(50) {
            session.tick(trackB, PlaybackOptions(volume = 0.4f, crossfadeTicks = 100))
        }

        assertEquals(0.28284f, session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(0.28284f, session.adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(session.adapter, expected = 2)
    }

    @Test
    fun `third track interruption fades the interrupted pair out instead of cutting one`() {
        // Catches a regression to stopping the quieter fade owner while it is still audible.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(75) { session.tick(trackB, options) }

        session.tick(trackC, options)

        assertEquals(listOf(trackA.id, trackB.id, trackC.id), session.adapter.starts)
        assertTrue(session.adapter.stops.isEmpty())
        assertEquals(setOf(trackA.id, trackB.id, trackC.id), session.adapter.ownedTrackIds)
        assertEquals(equalPowerGain(0.25), session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(equalPowerGain(0.75), session.adapter.gainFor(trackB.id), 0.0002f)
        assertEquals(0f, session.adapter.gainFor(trackC.id), 0.0001f)
        assertHandleBound(session.adapter, expected = 3)

        repeat(25) { session.tick(trackC, options) }

        assertEquals(listOf(trackA.id), session.adapter.stops)
        assertEquals(setOf(trackB.id, trackC.id), session.adapter.ownedTrackIds)

        repeat(75) { session.tick(trackC, options) }

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.stops)
        assertEquals(setOf(trackC.id), session.adapter.ownedTrackIds)
        assertEquals(1f, session.adapter.gainFor(trackC.id), 0.0001f)
        session.assertNoAudibleJump()
    }

    @Test
    fun `a fourth track disposes only the quietest voice to bound live handles`() {
        // Catches an unbounded voice list that can exhaust Minecraft's streaming channel pool.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        repeat(30) { session.tick(trackB, options) }
        repeat(10) { session.tick(trackC, options) }

        session.tick(trackD, options)

        assertEquals(setOf(trackA.id, trackB.id, trackD.id), session.adapter.ownedTrackIds)
        assertEquals(listOf(trackC.id), session.adapter.stops)
        assertHandleBound(session.adapter, expected = 3)
    }

    @Test
    fun `retargeting the existing outgoing track reverses the fade from its current gains`() {
        // Catches mutations that restart an existing OGG or reset fade progress when roles reverse.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(25) { session.tick(trackB, options) }

        session.tick(track("a"), options)

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.starts)
        assertTrue(session.adapter.stops.isEmpty())
        assertEquals(equalPowerGain(0.76), session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(equalPowerGain(0.24), session.adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(session.adapter, expected = 2)

        repeat(24) { session.tick(trackA, options) }

        assertEquals(listOf(trackB.id), session.adapter.stops)
        assertEquals(setOf(trackA.id), session.adapter.ownedTrackIds)
        assertEquals(1f, session.adapter.gainFor(trackA.id), 0.0001f)
        assertHandleBound(session.adapter, expected = 1)
        session.assertNoAudibleJump()
    }

    @Test
    fun `alternating border biomes converge instead of leaving both tracks audible`() {
        // Catches fade progress that restarts on every reversal and never resolves at a biome border.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        var desired = trackA
        repeat(200) { tick ->
            if (tick % 10 == 0) desired = if (desired === trackA) trackB else trackA
            session.tick(desired, options)
        }

        assertTrue(
            session.largestSecondaryGain < 0.3f,
            "a second track reached ${session.largestSecondaryGain} while flickering",
        )
        assertTrue(session.adapter.gainFor(trackA.id) > 0.9f)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `ended desired instance is disposed and restarted immediately without a duration timer`() {
        // Catches a mutation that trusts a stale playing handle or waits for track-duration metadata.
        val session = Session()
        val options = PlaybackOptions(volume = 0.7f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.adapter.end(trackA.id)

        session.tick(track("a"), options)

        assertEquals(listOf(trackA.id, trackA.id), session.adapter.starts)
        assertEquals(listOf(trackA.id), session.adapter.stops)
        assertEquals(setOf(trackA.id), session.adapter.ownedTrackIds)
        assertTrue(session.adapter.isTrackPlaying(trackA.id))
        assertEquals(0.7f, session.adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `ended outgoing leaves the incoming fading in rather than jumping it to full volume`() {
        // Catches a regression that promotes the surviving fade owner straight to full gain.
        val session = Session()
        val options = PlaybackOptions(volume = 0.6f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(25) { session.tick(trackB, options) }
        session.adapter.end(trackA.id)

        session.tick(track("b"), options)

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.starts)
        assertEquals(listOf(trackA.id), session.adapter.stops)
        assertEquals(setOf(trackB.id), session.adapter.ownedTrackIds)
        assertEquals(equalPowerGain(0.26) * 0.6f, session.adapter.gainFor(trackB.id), 0.0002f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)

        repeat(74) { session.tick(trackB, options) }

        assertEquals(0.6f, session.adapter.gainFor(trackB.id), 0.0001f)
        session.assertNoAudibleJump()
    }

    @Test
    fun `ended outgoing after reversal leaves the desired track fading rather than jumping`() {
        // Catches a promotion jump that only shows up once fade roles have reversed.
        val session = Session()
        val options = PlaybackOptions(volume = 0.8f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(25) { session.tick(trackB, options) }
        session.tick(track("a"), options)
        repeat(10) { session.tick(trackA, options) }
        session.adapter.end(trackB.id)

        session.tick(trackA, PlaybackOptions(volume = 0.55f, crossfadeTicks = 100))

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.starts)
        assertEquals(listOf(trackB.id), session.adapter.stops)
        assertEquals(setOf(trackA.id), session.adapter.ownedTrackIds)
        assertTrue(session.adapter.isTrackPlaying(trackA.id))
        assertEquals(equalPowerGain(0.87) * 0.55f, session.adapter.gainFor(trackA.id), 0.0002f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `ended incoming restarts at its own fade position without disturbing the outgoing`() {
        // Catches a mutation that tears down a healthy outgoing track when the incoming loop dies.
        val session = Session()
        val options = PlaybackOptions(volume = 0.65f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(25) { session.tick(trackB, options) }
        session.adapter.end(trackB.id)

        session.tick(trackB, options)

        assertEquals(listOf(trackA.id, trackB.id, trackB.id), session.adapter.starts)
        assertEquals(listOf(trackB.id), session.adapter.stops)
        assertEquals(setOf(trackA.id, trackB.id), session.adapter.ownedTrackIds)
        assertTrue(session.adapter.isTrackPlaying(trackB.id))
        assertEquals(equalPowerGain(0.75) * 0.65f, session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(equalPowerGain(0.25) * 0.65f, session.adapter.gainFor(trackB.id), 0.0002f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 2)
        session.assertNoAudibleJump()
    }

    @Test
    fun `failed initial start remains idle and explicitly releases vanilla music`() {
        // Catches a mutation that claims music ownership when no custom handle was created.
        val session = Session()
        session.adapter.setVanillaMusicSuppressed(true)
        session.adapter.rejectedTrackIds += trackA.id

        session.tick(trackA, PlaybackOptions(volume = 0.8f, crossfadeTicks = 100))

        assertEquals(listOf(trackA.id), session.adapter.starts)
        assertTrue(session.adapter.stops.isEmpty())
        assertFalse(session.adapter.vanillaMusicSuppressed)
        assertEquals(false, session.adapter.suppressionChanges.last())
        assertHandleBound(session.adapter, expected = 0)
    }

    @Test
    fun `failed incoming start keeps the current track playing and backs off before retrying`() {
        // Catches a regression that trades live music for silence when a track cannot be started.
        val session = Session()
        val options = PlaybackOptions(volume = 0.8f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.adapter.rejectedTrackIds += trackB.id

        session.tick(trackB, options)

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.starts)
        assertTrue(session.adapter.stops.isEmpty())
        assertEquals(setOf(trackA.id), session.adapter.ownedTrackIds)
        assertEquals(0.8f, session.adapter.gainFor(trackA.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)

        repeat(20) { session.tick(trackB, options) }

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.starts)

        session.adapter.rejectedTrackIds -= trackB.id
        session.tick(trackB, options)

        assertEquals(listOf(trackA.id, trackB.id, trackB.id), session.adapter.starts)
        assertEquals(setOf(trackA.id, trackB.id), session.adapter.ownedTrackIds)
        assertEquals(0f, session.adapter.gainFor(trackB.id), 0.0001f)
        session.assertNoAudibleJump()
    }

    @Test
    fun `paused ticks hold every gain and resume the fade one step at a time`() {
        // Catches advancing a fade Minecraft cannot hear, which lands as one step on resume.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(10) { session.tick(trackB, options) }
        val pausedOutgoing = session.adapter.gainFor(trackA.id)
        val pausedIncoming = session.adapter.gainFor(trackB.id)

        repeat(60) { session.tick(trackB, options, advanceFade = false) }

        assertEquals(pausedOutgoing, session.adapter.gainFor(trackA.id), 0.0001f)
        assertEquals(pausedIncoming, session.adapter.gainFor(trackB.id), 0.0001f)
        assertHandleBound(session.adapter, expected = 2)

        session.tick(trackB, options)

        assertEquals(equalPowerGain(0.89), session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(equalPowerGain(0.11), session.adapter.gainFor(trackB.id), 0.0002f)
        session.assertNoAudibleJump()
    }

    @Test
    fun `stop disposes both crossfade handles and releases vanilla music idempotently`() {
        // Catches a mutation that cleans up only one state-owned handle or leaves suppression raised.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(25) { session.tick(trackB, options) }

        session.controller.stop()
        session.controller.stop()

        assertEquals(listOf(trackA.id, trackB.id), session.adapter.stops)
        assertFalse(session.adapter.vanillaMusicSuppressed)
        assertEquals(false, session.adapter.suppressionChanges.last())
        assertHandleBound(session.adapter, expected = 0)
    }

    @Test
    fun `fade duration change alters the remaining rate without moving current gains`() {
        // Catches rescaling elapsed progress against a new duration, which steps the gains mid-fade.
        val session = Session()
        val original = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, original)
        session.tick(trackB, original)
        repeat(24) { session.tick(trackB, original) }

        session.tick(trackB, PlaybackOptions(volume = 1f, crossfadeTicks = 50))

        assertEquals(equalPowerGain(0.74), session.adapter.gainFor(trackA.id), 0.0002f)
        assertEquals(equalPowerGain(0.26), session.adapter.gainFor(trackB.id), 0.0002f)
        assertHandleBound(session.adapter, expected = 2)
        // A halved duration doubles the step, so allow one step of the shorter fade.
        session.assertNoAudibleJump(maxStepGain = 2 * ONE_STEP_GAIN)
    }

    @Test
    fun `changing an active fade duration to zero completes it immediately`() {
        // Catches a mutation that divides by zero or waits another captured-duration tick to finish.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(25) { session.tick(trackB, options) }

        session.tick(trackB, PlaybackOptions(volume = 0.45f, crossfadeTicks = 0))

        assertEquals(listOf(trackA.id), session.adapter.stops)
        assertEquals(setOf(trackB.id), session.adapter.ownedTrackIds)
        assertEquals(0.45f, session.adapter.gainFor(trackB.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    @Test
    fun `zero duration third track interruption disposes both old handles before immediate playback`() {
        // Catches a mutation that applies zero duration only when the desired track is already in the fade.
        val session = Session()
        val options = PlaybackOptions(volume = 1f, crossfadeTicks = 100)
        session.tick(trackA, options)
        session.tick(trackB, options)
        repeat(75) { session.tick(trackB, options) }

        session.tick(trackC, PlaybackOptions(volume = 0.5f, crossfadeTicks = 0))

        assertEquals(listOf(trackA.id, trackB.id, trackC.id), session.adapter.starts)
        assertEquals(setOf(trackA.id, trackB.id), session.adapter.stops.toSet())
        assertEquals(setOf(trackC.id), session.adapter.ownedTrackIds)
        assertEquals(0.5f, session.adapter.gainFor(trackC.id), 0.0001f)
        assertTrue(session.adapter.vanillaMusicSuppressed)
        assertHandleBound(session.adapter, expected = 1)
    }

    private fun assertHandleBound(adapter: FakeAudioAdapter, expected: Int) {
        assertEquals(expected, adapter.ownedHandleCount)
        assertTrue(adapter.ownedHandleCount in 0..MAX_LIVE_HANDLES)
        assertTrue(adapter.maximumOwnedHandleCount in 0..MAX_LIVE_HANDLES)
    }

    private companion object {
        const val MAX_LIVE_HANDLES = 3

        /** One tick of a 100 tick fade moves a gain by at most pi/2 * 0.01. */
        const val ONE_STEP_GAIN = 0.016f

        val trackA = track("a")
        val trackB = track("b")
        val trackC = track("c")
        val trackD = track("d")

        fun track(id: String) = TrackDefinition(
            id = TrackId(id),
            soundEvent = "biometunes:music.$id",
            title = "Track $id",
            artist = "Artist $id",
        )
    }

    /** Drives the controller a tick at a time and watches the gains it applies for discontinuities. */
    private class Session {
        val adapter = FakeAudioAdapter()
        val controller = PlaybackController(adapter)

        private var previousGains: Map<TrackId, Float> = emptyMap()
        private var largestJump = 0f
        private var jumpDescription = ""

        /** Loudest gain reached by anything other than the loudest track, across every tick so far. */
        var largestSecondaryGain = 0f
            private set

        fun tick(
            desired: TrackDefinition,
            options: PlaybackOptions,
            advanceFade: Boolean = true,
        ) {
            controller.update(desired, options, advanceFade)
            val gains = adapter.gains()
            // Only tracks that were already live can jump; starting one is an intentional step, and
            // a track the audio engine ended is already silent before the controller disposes it.
            for ((id, before) in previousGains) {
                if (id in adapter.endedTrackIds) continue
                val after = gains[id] ?: 0f
                if (abs(after - before) > largestJump) {
                    largestJump = abs(after - before)
                    jumpDescription = "${id.value} moved $before -> $after"
                }
            }
            largestSecondaryGain = maxOf(
                largestSecondaryGain,
                gains.values.sortedDescending().drop(1).firstOrNull() ?: 0f,
            )
            previousGains = gains
        }

        fun assertNoAudibleJump(maxStepGain: Float = ONE_STEP_GAIN) {
            assertTrue(largestJump <= maxStepGain, "gain jumped more than one fade step: $jumpDescription")
        }
    }
}

private class FakeAudioAdapter : AudioAdapter {
    private data class FakeAudioHandle(val serial: Int) : AudioHandle

    private data class Instance(
        val track: TrackDefinition,
        var gain: Float,
        var highFrequencyGain: Float = 1.0f,
        var reverbSend: Float = 0.0f,
        var playing: Boolean = true,
    )

    private var nextSerial = 0
    private val instances = linkedMapOf<FakeAudioHandle, Instance>()
    var maximumOwnedHandleCount = 0
        private set
    val rejectedTrackIds = mutableSetOf<TrackId>()
    val endedTrackIds = mutableSetOf<TrackId>()
    val starts = mutableListOf<TrackId>()
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
    }

    override fun setLowPass(handle: AudioHandle, highFrequencyGain: Float) {
        instances.getValue(handle as FakeAudioHandle).highFrequencyGain = highFrequencyGain
    }

    override fun setReverb(handle: AudioHandle, send: Float) {
        instances.getValue(handle as FakeAudioHandle).reverbSend = send
    }

    override fun stop(handle: AudioHandle) {
        val instance = requireNotNull(instances.remove(handle as FakeAudioHandle)) {
            "attempted to stop an unknown or already-disposed handle $handle"
        }
        stops += instance.track.id
    }

    override fun isPlaying(handle: AudioHandle): Boolean =
        instances[handle as FakeAudioHandle]?.playing == true

    override fun setVanillaMusicSuppressed(suppressed: Boolean) {
        if (vanillaMusicSuppressed == suppressed) return
        vanillaMusicSuppressed = suppressed
        suppressionChanges += suppressed
    }

    fun gainFor(trackId: TrackId): Float =
        instances.values.single { it.track.id == trackId }.gain

    fun lowPassFor(trackId: TrackId): Float =
        instances.values.single { it.track.id == trackId }.highFrequencyGain

    fun reverbFor(trackId: TrackId): Float =
        instances.values.single { it.track.id == trackId }.reverbSend

    fun gains(): Map<TrackId, Float> = instances.values.associate { it.track.id to it.gain }

    fun end(trackId: TrackId) {
        instances.values.single { it.track.id == trackId }.playing = false
        endedTrackIds += trackId
    }

    fun isTrackPlaying(trackId: TrackId): Boolean =
        instances.values.single { it.track.id == trackId }.playing
}
