package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundAvailabilityCacheTest {
    @Test
    fun `registered empty sound is reportable once and then skipped`() {
        // Catches retrying and re-logging a registered event whose missing OGG leaves total weight zero.
        val cache = SoundAvailabilityCache()
        val track = TrackId("forest")
        var resolutions = 0

        assertEquals(
            SoundStartDecision.ReportUnavailable(UnavailableSoundReason.ZERO_PLAYABLE_WEIGHT),
            cache.start<String>(
                trackId = track,
                resolvePlayableWeight = {
                    resolutions++
                    0
                },
                attemptPlayback = { error("zero-weight sound must not reach playback") },
            ),
        )
        assertEquals(
            SoundStartDecision.SkipUnavailable,
            cache.start<String>(
                trackId = track,
                resolvePlayableWeight = {
                    resolutions++
                    0
                },
                attemptPlayback = { error("cached sound must not reach playback") },
            ),
        )
        assertEquals(1, resolutions)
    }

    @Test
    fun `generic not-started failure leaves a playable sound retryable`() {
        // Catches caching SoundEngine NOT_STARTED even though channel allocation may recover next tick.
        val cache = SoundAvailabilityCache()
        val track = TrackId("forest")
        var playbackAttempts = 0

        val firstAttempt = cache.start<String>(
            trackId = track,
            resolvePlayableWeight = { 1 },
            attemptPlayback = {
                playbackAttempts++
                null
            },
        )
        val retryAfterTransientFailure = cache.start(
            trackId = track,
            resolvePlayableWeight = { 1 },
            attemptPlayback = {
                playbackAttempts++
                "started-handle"
            },
        )

        assertEquals(SoundStartDecision.NotStarted, firstAttempt)
        assertEquals(SoundStartDecision.Started("started-handle"), retryAfterTransientFailure)
        assertEquals(2, playbackAttempts)
    }

    @Test
    fun `resource reload clears deterministic unavailability`() {
        // Catches a cache that survives F3+T after a resource pack restores the missing OGG.
        val cache = SoundAvailabilityCache()
        val track = TrackId("forest")
        cache.start<String>(track, resolvePlayableWeight = { 0 }, attemptPlayback = { null })

        cache.clear()

        assertEquals(
            SoundStartDecision.Started("started-handle"),
            cache.start(
                trackId = track,
                resolvePlayableWeight = { 1 },
                attemptPlayback = { "started-handle" },
            ),
        )
    }

    @Test
    fun `missing registry event is deterministic and reportable once`() {
        // Catches retaining the old per-tick warning behavior for an unknown sound event.
        val cache = SoundAvailabilityCache()
        val track = TrackId("unknown")

        assertEquals(
            SoundStartDecision.ReportUnavailable(UnavailableSoundReason.MISSING_EVENT),
            cache.start<String>(track, resolvePlayableWeight = { null }, attemptPlayback = { null }),
        )
        assertEquals(
            SoundStartDecision.SkipUnavailable,
            cache.start<String>(track, resolvePlayableWeight = { null }, attemptPlayback = { null }),
        )
    }
}
