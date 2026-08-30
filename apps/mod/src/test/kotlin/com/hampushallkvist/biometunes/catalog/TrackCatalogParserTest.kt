package com.hampushallkvist.biometunes.catalog

import com.hampushallkvist.biometunes.environment.EnvironmentalProfile
import com.hampushallkvist.biometunes.environment.EnvironmentalSampling
import com.hampushallkvist.biometunes.environment.EnvironmentalThresholds
import com.hampushallkvist.biometunes.environment.EnvironmentalWeights
import com.hampushallkvist.biometunes.environment.ShelteredAudioSettings
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackCatalogParserTest {
    @Test
    fun `parses a complete environmental profile`() {
        val catalog = TrackCatalogParser.parse(minimalCatalog).getOrThrow()

        assertEquals(
            EnvironmentalProfile(
                nativeUndergroundBiomes = setOf("minecraft:deep_dark"),
                subterraneanTracks = TrackPool(listOf(WeightedTrack(TrackId("plains"), weight = 9))),
                sampling = EnvironmentalSampling(nearbyRadius = 6, enclosureRadius = 2, surfaceDepthScale = 48),
                weights = EnvironmentalWeights(
                    nearbySkyExposure = 1.0,
                    skyLight = 0.25,
                    localOpenness = 0.25,
                    surfaceDepth = 1.0,
                    localEnclosure = 0.25,
                    skyDarkness = 0.15,
                    blockDarkness = 0.05,
                ),
                thresholds = EnvironmentalThresholds(
                    outsideEnterExposure = 0.65,
                    outsideExitExposure = 0.45,
                    subterraneanEnterDepth = 0.70,
                    subterraneanExitDepth = 0.55,
                    subterraneanEnterMaxExposure = 0.25,
                    subterraneanExitMaxExposure = 0.40,
                ),
                smoothingSeconds = 2.0,
                shelteredAudio = ShelteredAudioSettings(
                    maximumGainMultiplier = 0.85f,
                    maximumHighFrequencyGain = 0.35f,
                    maximumReverbSend = 0.12f,
                ),
            ),
            catalog.environmentalProfiles["minecraft:overworld"],
        )
    }

    @Test
    fun `requires environmental profiles but accepts an explicit empty object`() {
        val missing = minimalCatalog.replace(
            "  \"environmental_profiles\": $minimalEnvironmentalProfiles,\n",
            "",
        )

        assertTrue(TrackCatalogParser.parse(missing).isFailure)
        assertTrue(TrackCatalogParser.parse(minimalCatalogWithEmptyProfiles).isSuccess)
    }

    @Test
    fun `rejects invalid environmental profile references and bounds`() {
        val cases = listOf(
            Triple(
                "unknown native biome",
                minimalCatalog.replace(
                    "\"native_underground_biomes\":[\"minecraft:deep_dark\"]",
                    "\"native_underground_biomes\":[\"minecraft:missing\"]",
                ),
                "native_underground_biomes",
            ),
            Triple(
                "unknown subterranean track",
                minimalCatalog.replace(
                    "\"subterranean_tracks\":[{\"track\":\"plains\",\"weight\":9}]",
                    "\"subterranean_tracks\":[{\"track\":\"missing\",\"weight\":9}]",
                ),
                "subterranean_tracks",
            ),
            Triple("zero nearby radius", minimalCatalog.replace("\"nearby_radius\":6", "\"nearby_radius\":0"), "sampling.nearby_radius"),
            Triple("large nearby radius", minimalCatalog.replace("\"nearby_radius\":6", "\"nearby_radius\":17"), "sampling.nearby_radius"),
            Triple("zero enclosure radius", minimalCatalog.replace("\"enclosure_radius\":2", "\"enclosure_radius\":0"), "sampling.enclosure_radius"),
            Triple("large depth scale", minimalCatalog.replace("\"surface_depth_scale\":48", "\"surface_depth_scale\":257"), "sampling.surface_depth_scale"),
            Triple("negative weight", minimalCatalog.replace("\"nearby_sky_exposure\":1.0", "\"nearby_sky_exposure\":-1.0"), "weights.nearby_sky_exposure"),
            Triple("large weight", minimalCatalog.replace("\"sky_light\":0.25", "\"sky_light\":11.0"), "weights.sky_light"),
            Triple("reversed outside thresholds", minimalCatalog.replace("\"outside_enter_exposure\":0.65", "\"outside_enter_exposure\":0.40"), "thresholds.outside_enter_exposure"),
            Triple("reversed subterranean depth thresholds", minimalCatalog.replace("\"subterranean_enter_depth\":0.70", "\"subterranean_enter_depth\":0.50"), "thresholds.subterranean_enter_depth"),
            Triple("reversed subterranean exposure thresholds", minimalCatalog.replace("\"subterranean_enter_max_exposure\":0.25", "\"subterranean_enter_max_exposure\":0.50"), "thresholds.subterranean_enter_max_exposure"),
            Triple("large smoothing", minimalCatalog.replace("\"smoothing_seconds\":2.0", "\"smoothing_seconds\":11.0"), "smoothing_seconds"),
            Triple("large gain", minimalCatalog.replace("\"maximum_gain_multiplier\":0.85", "\"maximum_gain_multiplier\":1.1"), "sheltered_audio.maximum_gain_multiplier"),
            Triple("negative high-frequency gain", minimalCatalog.replace("\"maximum_high_frequency_gain\":0.35", "\"maximum_high_frequency_gain\":-0.1"), "sheltered_audio.maximum_high_frequency_gain"),
            Triple("large reverb send", minimalCatalog.replace("\"maximum_reverb_send\":0.12", "\"maximum_reverb_send\":1.1"), "sheltered_audio.maximum_reverb_send"),
        )

        cases.forEach { (name, json, field) ->
            val result = TrackCatalogParser.parse(json)
            assertTrue(result.isFailure, name)
            assertContains(
                result.exceptionOrNull()!!.message!!,
                "environmental_profiles['minecraft:overworld'].$field",
                message = name,
            )
        }
    }

    @Test
    fun `rejects zero-total environmental score weights`() {
        val zeroExposure = minimalCatalog
            .replace("\"nearby_sky_exposure\":1.0", "\"nearby_sky_exposure\":0.0")
            .replace("\"sky_light\":0.25", "\"sky_light\":0.0")
            .replace("\"local_openness\":0.25", "\"local_openness\":0.0")
        val zeroDepth = minimalCatalog
            .replace("\"surface_depth\":1.0", "\"surface_depth\":0.0")
            .replace("\"local_enclosure\":0.25", "\"local_enclosure\":0.0")
            .replace("\"sky_darkness\":0.15", "\"sky_darkness\":0.0")
            .replace("\"block_darkness\":0.05", "\"block_darkness\":0.0")

        assertContains(
            TrackCatalogParser.parse(zeroExposure).exceptionOrNull()!!.message!!,
            "environmental_profiles['minecraft:overworld'].weights exposure weights",
        )
        assertContains(
            TrackCatalogParser.parse(zeroDepth).exceptionOrNull()!!.message!!,
            "environmental_profiles['minecraft:overworld'].weights depth weights",
        )
    }

    @Test
    fun `parses weighted pools with global silence and an inline silence override`() {
        val result = TrackCatalogParser.parse(weightedCatalog)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        val catalog = result.getOrThrow()
        assertEquals(
            WeightedSilence(
                weight = 1,
                durationSeconds = SilenceDurationRange(min = 30, max = 120),
            ),
            catalog.globalSilence,
        )
        assertEquals(
            TrackPool(
                listOf(
                    WeightedTrack(TrackId("plains"), weight = 8),
                    WeightedTrack(TrackId("forest"), weight = 2),
                    WeightedSilence(
                        weight = 3,
                        durationSeconds = SilenceDurationRange(min = 15, max = 60),
                    ),
                ),
            ),
            catalog.biomes["minecraft:plains"],
        )
    }

    @Test
    fun `rejects the legacy string mapping format`() {
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                "\"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }]",
                "\"minecraft:plains\": \"plains\"",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "biomes")
    }

    @Test
    fun `rejects negative weights and reversed silence ranges`() {
        val negativeWeight = TrackCatalogParser.parse(
            weightedCatalog.replace(
                "{ \"track\": \"forest\", \"weight\": 2 }",
                "{ \"track\": \"forest\", \"weight\": -1 }",
            ),
        )
        val reversedRange = TrackCatalogParser.parse(
            weightedCatalog.replace(
                "{ \"min\": 15, \"max\": 60 }",
                "{ \"min\": 61, \"max\": 60 }",
            ),
        )

        assertTrue(negativeWeight.isFailure)
        assertContains(negativeWeight.exceptionOrNull()!!.message!!, "non-negative")
        assertTrue(reversedRange.isFailure)
        assertContains(reversedRange.exceptionOrNull()!!.message!!, "greater than or equal")
    }

    @Test
    fun `rejects duration metadata on songs and missing duration metadata on silence`() {
        val songDuration = TrackCatalogParser.parse(
            weightedCatalog.replace(
                "{ \"track\": \"forest\", \"weight\": 2 }",
                "{ \"track\": \"forest\", \"weight\": 2, \"duration_seconds\": { \"min\": 1, \"max\": 2 } }",
            ),
        )
        val missingSilenceDuration = TrackCatalogParser.parse(
            weightedCatalog.replace(
                "{ \"track\": \"silence\", \"weight\": 3, \"duration_seconds\": { \"min\": 15, \"max\": 60 } }",
                "{ \"track\": \"silence\", \"weight\": 3 }",
            ),
        )

        assertTrue(songDuration.isFailure)
        assertContains(songDuration.exceptionOrNull()!!.message!!, "only valid for silence")
        assertTrue(missingSilenceDuration.isFailure)
        assertContains(missingSilenceDuration.exceptionOrNull()!!.message!!, "required for silence")
    }

    @Test
    fun `rejects silence as a reserved track definition ID`() {
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace("\"id\": \"plains\"", "\"id\": \"silence\""),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "reserved")
    }

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
        val plainsPool = TrackPool(listOf(WeightedTrack(TrackId("plains"), weight = 9)))
        assertEquals(mapOf("ender_dragon" to plainsPool), catalog.bosses)
        assertEquals(
            mapOf(
                "minecraft:deep_dark" to plainsPool,
                "minecraft:plains" to plainsPool,
            ),
            catalog.biomes,
        )
        assertEquals(listOf("minecraft:is_overworld" to plainsPool), catalog.biomeTags)
        assertEquals(mapOf("minecraft:overworld" to plainsPool), catalog.dimensions)
        assertEquals(plainsPool, catalog.fallback)
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
                "\"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }]",
                "\"minecraft:plains\": [{ \"track\": \"missing\", \"weight\": 9 }]",
            ),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "missing")
    }

    @Test
    fun `rejects a catalog with no fallback`() {
        // Catches a mutation that defaults a missing fallback instead of rejecting an incomplete catalog.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(",\n  \"fallback\": [{ \"track\": \"plains\", \"weight\": 9 }]", ""),
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "fallback")
    }

    @Test
    fun `rejects fallbacks that reference unknown tracks`() {
        // Catches a mutation that skips cross-reference validation for the fallback track.
        val result = TrackCatalogParser.parse(
            minimalCatalog.replace(
                "\"fallback\": [{ \"track\": \"plains\", \"weight\": 9 }]",
                "\"fallback\": [{ \"track\": \"missing\", \"weight\": 9 }]",
            ),
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
                """    ,{ "tag": "minecraft:is_overworld", "tracks": [{ "track": "plains", "weight": 9 }] }
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
                "\"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }]",
                "\"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }], \"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }]",
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
                "\"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }]",
                "\"minecraft:plains\": [{ \"track\": \"plains\", \"weight\": 9 }], \"minecraft:\\u0070lains\": [{ \"track\": \"plains\", \"weight\": 9 }]",
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
        val minimalEnvironmentalProfiles = """
            {
              "minecraft:overworld": {
                "native_underground_biomes": ["minecraft:deep_dark"],
                "subterranean_tracks": [{ "track": "plains", "weight": 9 }],
                "sampling": {
                  "nearby_radius": 6,
                  "enclosure_radius": 2,
                  "surface_depth_scale": 48
                },
                "weights": {
                  "nearby_sky_exposure": 1.0,
                  "sky_light": 0.25,
                  "local_openness": 0.25,
                  "surface_depth": 1.0,
                  "local_enclosure": 0.25,
                  "sky_darkness": 0.15,
                  "block_darkness": 0.05
                },
                "thresholds": {
                  "outside_enter_exposure": 0.65,
                  "outside_exit_exposure": 0.45,
                  "subterranean_enter_depth": 0.70,
                  "subterranean_exit_depth": 0.55,
                  "subterranean_enter_max_exposure": 0.25,
                  "subterranean_exit_max_exposure": 0.40
                },
                "smoothing_seconds": 2.0,
                "sheltered_audio": {
                  "maximum_gain_multiplier": 0.85,
                  "maximum_high_frequency_gain": 0.35,
                  "maximum_reverb_send": 0.12
                }
              }
            }
        """.trimIndent().lineSequence().joinToString("") { it.trim() }.replace(" ", "")

        val weightedCatalog = """
            {
              "tracks": [
                { "id": "plains", "sound_event": "biometunes:music.plains", "title": "Plains", "artist": "Abraham Frato" },
                { "id": "forest", "sound_event": "biometunes:music.forest", "title": "Forest", "artist": "Abraham Frato" }
              ],
              "silence": {
                "weight": 1,
                "duration_seconds": { "min": 30, "max": 120 }
              },
              "bosses": {
                "ender_dragon": [{ "track": "plains", "weight": 9 }]
              },
              "biomes": {
                "minecraft:deep_dark": [{ "track": "plains", "weight": 9 }],
                "minecraft:plains": [
                  { "track": "plains", "weight": 8 },
                  { "track": "forest", "weight": 2 },
                  { "track": "silence", "weight": 3, "duration_seconds": { "min": 15, "max": 60 } }
                ]
              },
              "biome_tags": [
                { "tag": "minecraft:is_overworld", "tracks": [{ "track": "plains", "weight": 9 }] }
              ],
              "dimensions": {
                "minecraft:overworld": [{ "track": "plains", "weight": 9 }]
              },
              "environmental_profiles": $minimalEnvironmentalProfiles,
              "fallback": [{ "track": "plains", "weight": 9 }]
            }
        """.trimIndent()

        val minimalCatalog = """
            {
              "tracks": [
                { "id": "plains", "sound_event": "biometunes:music.plains", "title": "Plains", "artist": "Abraham Frato" }
              ],
              "silence": {
                "weight": 1,
                "duration_seconds": { "min": 30, "max": 120 }
              },
              "bosses": {
                "ender_dragon": [{ "track": "plains", "weight": 9 }]
              },
              "biomes": {
                "minecraft:deep_dark": [{ "track": "plains", "weight": 9 }],
                "minecraft:plains": [{ "track": "plains", "weight": 9 }]
              },
              "biome_tags": [
                { "tag": "minecraft:is_overworld", "tracks": [{ "track": "plains", "weight": 9 }] }
              ],
              "dimensions": {
                "minecraft:overworld": [{ "track": "plains", "weight": 9 }]
              },
              "environmental_profiles": $minimalEnvironmentalProfiles,
              "fallback": [{ "track": "plains", "weight": 9 }]
            }
        """.trimIndent()

        val minimalCatalogWithEmptyProfiles = minimalCatalog.replace(
            "  \"environmental_profiles\": $minimalEnvironmentalProfiles,",
            "  \"environmental_profiles\": {},",
        )
    }
}
