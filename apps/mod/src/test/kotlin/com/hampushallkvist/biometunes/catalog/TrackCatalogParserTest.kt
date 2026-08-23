package com.hampushallkvist.biometunes.catalog

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackCatalogParserTest {
    @Test
    fun `parses a valid minimal catalog into track definitions and mappings`() {
        // Catches a mutation that omits a catalog section or maps a raw field to the wrong domain field.
        val result = TrackCatalogParser.parse(minimalCatalog)

        assertTrue(result.isSuccess)
        val catalog = result.getOrThrow()
        assertEquals(
            TrackDefinition(
                id = TrackId("plains"),
                soundEvent = "biometunes:music.plains",
                title = "Plains",
                artist = "Abraham Frato",
            ),
            catalog.tracks[TrackId("plains")],
        )
        assertEquals(mapOf("ender_dragon" to TrackId("plains")), catalog.bosses)
        assertEquals(mapOf("minecraft:plains" to TrackId("plains")), catalog.biomes)
        assertEquals(listOf("minecraft:is_overworld" to TrackId("plains")), catalog.biomeTags)
        assertEquals(mapOf("minecraft:overworld" to TrackId("plains")), catalog.dimensions)
        assertEquals(TrackId("plains"), catalog.fallback)
    }

    @Test
    fun `rejects duplicate track IDs`() {
        // Catches a mutation that silently overwrites a duplicate track definition in the tracks map.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                "\n  ],",
                """,
                { "id": "plains", "sound_event": "biometunes:music.plains_two", "title": "Other Plains", "artist": "Other Artist" }
  ],
                """.trimIndent(),
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "plains")
    }

    @Test
    fun `rejects tracks with blank artists`() {
        // Catches a mutation that accepts whitespace-only artist credits.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace("\"artist\": \"Abraham Frato\"", "\"artist\": \"  \""),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "artist")
    }

    @Test
    fun `rejects biome mappings to unknown tracks`() {
        // Catches a mutation that skips cross-reference validation for biome mappings.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                "\"minecraft:plains\": \"plains\"",
                "\"minecraft:plains\": \"missing\"",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "missing")
    }

    @Test
    fun `rejects a catalog with no fallback`() {
        // Catches a mutation that defaults a missing fallback instead of rejecting an incomplete catalog.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(",\n  \"fallback\": \"plains\"", ""),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "fallback")
    }

    @Test
    fun `rejects fallbacks that reference unknown tracks`() {
        // Catches a mutation that skips cross-reference validation for the fallback track.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace("\"fallback\": \"plains\"", "\"fallback\": \"missing\""),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "missing")
    }

    @Test
    fun `rejects duplicate biome tags with the duplicated tag in the error`() {
        // Catches a mutation that accepts duplicate biome-tag keys or loses their identifier in the failure.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                """  ],
  "dimensions""",
                """    ,{ "tag": "minecraft:is_overworld", "track": "plains" }
  ],
  "dimensions""",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "minecraft:is_overworld")
    }

    @Test
    fun `rejects duplicate keys within biomes`() {
        // Catches a mutation that lets a JSON object overwrite a duplicate effective biome key.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                "\"minecraft:plains\": \"plains\"",
                "\"minecraft:plains\": \"plains\", \"minecraft:plains\": \"plains\"",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "minecraft:plains")
    }

    @Test
    fun `rejects repeated root biomes properties`() {
        // Catches a mutation that checks only an earlier biomes object while decoding a later one.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                """  "biome_tags": [""",
                """  "biomes": {
    "minecraft:desert": "plains"
  },
  "biome_tags": [""",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "biomes")
    }

    @Test
    fun `rejects escaped-equivalent biome keys`() {
        // Catches a mutation that compares raw JSON spelling instead of decoded biome-key identifiers.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                "\"minecraft:plains\": \"plains\"",
                "\"minecraft:plains\": \"plains\", \"minecraft:\\u0070lains\": \"plains\"",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "minecraft:plains")
    }

    @Test
    fun `rejects escaped-equivalent repeated root biomes properties`() {
        // Catches a mutation that misses a repeated root property when its JSON spelling is escaped.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                """  "biome_tags": [""",
                """  "b\u0069omes": {
    "minecraft:desert": "plains"
  },
  "biome_tags": [""",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "biomes")
    }

    @Test
    fun `parses valid nested JSON and escaped strings`() {
        // Catches a mutation that treats valid nested values or escaped strings as malformed while scanning keys.
        val result = TrackCatalogParser.parse(
            minimalCatalog
                .replace("\"title\": \"Plains\"", "\"title\": \"Plain\\u0073 \\\"Theme\\\"\"")
                .replace("\"artist\": \"Abraham Frato\"", "\"artist\": \"Abraham \\\\ Frato\""),
        )

        assertTrue(result.isSuccess)
        assertEquals("Plains \"Theme\"", result.getOrThrow().tracks[TrackId("plains")]!!.title)
    }

    private companion object {
        val minimalCatalog = """
            {
              "tracks": [
                { "id": "plains", "sound_event": "biometunes:music.plains", "title": "Plains", "artist": "Abraham Frato" }
              ],
              "bosses": {
                "ender_dragon": "plains"
              },
              "biomes": {
                "minecraft:plains": "plains"
              },
              "biome_tags": [
                { "tag": "minecraft:is_overworld", "track": "plains" }
              ],
              "dimensions": {
                "minecraft:overworld": "plains"
              },
              "fallback": "plains"
            }
        """.trimIndent()
    }
}
