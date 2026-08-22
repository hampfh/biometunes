package com.hampushallkvist.biometunes.client

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientPlaybackLifecycleTest {
    @Test
    fun `gameplay requires a level and player outside the end credits`() {
        // Catches a mutation that treats WinScreen as gameplay because its level and player remain set.
        assertTrue(GameplayActivity.isActive(hasLevel = true, hasPlayer = true, isEndCredits = false))
        assertFalse(GameplayActivity.isActive(hasLevel = true, hasPlayer = true, isEndCredits = true))
        assertFalse(GameplayActivity.isActive(hasLevel = false, hasPlayer = true, isEndCredits = false))
        assertFalse(GameplayActivity.isActive(hasLevel = true, hasPlayer = false, isEndCredits = false))
    }

    @Test
    fun `credits stop playback and resuming the retained world samples immediately`() {
        // Catches retained session identities that suppress credits music or delay the post-credits sample.
        val lifecycle = ClientPlaybackLifecycle(sampleIntervalTicks = 10)
        val level = Any()
        val dimension = "minecraft:the_end"
        val player = Any()

        val initial = lifecycle.tick(level, dimension, player, isEndCredits = false)
        val credits = lifecycle.tick(level, dimension, player, isEndCredits = true)
        val resumed = lifecycle.tick(level, dimension, player, isEndCredits = false)

        assertTrue(initial.stopPlayback)
        assertTrue(initial.clearContext)
        assertTrue(initial.sampleContext)
        assertFalse(credits.activeGameplay)
        assertTrue(credits.stopPlayback)
        assertTrue(credits.clearContext)
        assertFalse(credits.sampleContext)
        assertTrue(resumed.activeGameplay)
        assertTrue(resumed.stopPlayback)
        assertTrue(resumed.clearContext)
        assertTrue(resumed.sampleContext)
    }

    @Test
    fun `same dimension player replacement stops and samples immediately`() {
        // Catches a mutation that keys lifecycle only by level and dimension across a respawn.
        val lifecycle = ClientPlaybackLifecycle(sampleIntervalTicks = 10)
        val level = Any()
        val firstPlayer = Any()
        lifecycle.tick(level, "minecraft:overworld", firstPlayer, isEndCredits = false)
        val unchanged = lifecycle.tick(level, "minecraft:overworld", firstPlayer, isEndCredits = false)

        val respawned = lifecycle.tick(level, "minecraft:overworld", Any(), isEndCredits = false)

        assertFalse(unchanged.stopPlayback)
        assertFalse(unchanged.clearContext)
        assertFalse(unchanged.sampleContext)
        assertTrue(respawned.stopPlayback)
        assertTrue(respawned.clearContext)
        assertTrue(respawned.sampleContext)
    }

    @Test
    fun `forced refresh samples on the next active tick without changing ownership`() {
        // Catches a resource-reload invalidation that waits for the ordinary sampling interval.
        val lifecycle = ClientPlaybackLifecycle(sampleIntervalTicks = 10)
        val level = Any()
        val player = Any()
        lifecycle.tick(level, "minecraft:overworld", player, isEndCredits = false)
        lifecycle.tick(level, "minecraft:overworld", player, isEndCredits = false)

        lifecycle.forceFreshSample()
        val refreshed = lifecycle.tick(level, "minecraft:overworld", player, isEndCredits = false)

        assertTrue(refreshed.activeGameplay)
        assertFalse(refreshed.stopPlayback)
        assertFalse(refreshed.clearContext)
        assertTrue(refreshed.sampleContext)
    }
}
