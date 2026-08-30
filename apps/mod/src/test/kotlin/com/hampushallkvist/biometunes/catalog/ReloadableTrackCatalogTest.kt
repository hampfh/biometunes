package com.hampushallkvist.biometunes.catalog

import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReloadableTrackCatalogTest {
    @Test
    fun `retains the last known good catalog when a reload fails`() {
        // Catches a mutation that clears or replaces current before validation completes.
        val reloadable = ReloadableTrackCatalog()
        val catalogA = reloadable.reload(catalog("plains")).getOrThrow()

        val invalidReload = reloadable.reload(catalog("plains", fallback = "missing"))

        assertTrue(invalidReload.isFailure)
        assertSame(catalogA, reloadable.current)
    }

    @Test
    fun `swaps current catalog after a successful reload`() {
        // Catches a mutation that validates a reload but leaves current pointed at the prior catalog.
        val reloadable = ReloadableTrackCatalog()
        reloadable.reload(catalog("plains")).getOrThrow()

        val catalogC = reloadable.reload(catalog("desert")).getOrThrow()

        assertSame(catalogC, reloadable.current)
    }

    private fun catalog(trackId: String, fallback: String = trackId): String = """
        {
          "tracks": [
            { "id": "$trackId", "sound_event": "biometunes:music.$trackId", "title": "${trackId.replaceFirstChar(Char::uppercase)}", "artist": "Abraham Frato" }
          ],
          "silence": {
            "weight": 1,
            "duration_seconds": { "min": 30, "max": 120 }
          },
          "bosses": {},
          "biomes": {},
          "biome_tags": [],
          "dimensions": {},
          "environmental_profiles": {},
          "fallback": [{ "track": "$fallback", "weight": 9 }]
        }
    """.trimIndent()
}
