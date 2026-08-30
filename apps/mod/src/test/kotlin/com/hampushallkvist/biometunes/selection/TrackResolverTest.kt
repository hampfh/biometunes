package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackCatalogParser
import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.catalog.TrackPool
import com.hampushallkvist.biometunes.catalog.WeightedTrack
import com.hampushallkvist.biometunes.environment.DecisionBasis
import com.hampushallkvist.biometunes.environment.EnvironmentConstraints
import com.hampushallkvist.biometunes.environment.EnvironmentMode
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult
import com.hampushallkvist.biometunes.environment.EnvironmentalDiagnostics
import com.hampushallkvist.biometunes.environment.EnvironmentalEvidence
import com.hampushallkvist.biometunes.environment.EnvironmentalScores
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackResolverTest {
    private val resolver = TrackResolver(TrackPoolSelector(BoundedRandom { 0 }))
    private val catalog = loadCatalog()

    @Test
    fun `subterranean classification selects the dimension profile pool`() {
        val resolved = resolver.resolvePool(
            context = PlayerContext(
                biomeId = "minecraft:plains",
                biomeTags = setOf("minecraft:is_overworld"),
                dimensionId = "minecraft:overworld",
                boss = null,
                environmentalClassification = classified(EnvironmentMode.SUBTERRANEAN),
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(SelectionSource.SUBTERRANEAN, resolved.source)
        assertEquals("environmental-profile:minecraft:overworld|subterranean", resolved.key)
        assertEquals(catalog.environmentalProfiles.getValue("minecraft:overworld").subterraneanTracks, resolved.pool)
    }

    @Test
    fun `outside sheltered and native underground bypass retain ordinary biome resolution`() {
        val classifications = listOf(
            classified(EnvironmentMode.OUTSIDE),
            classified(EnvironmentMode.SHELTERED),
            EnvironmentalClassificationResult.NotClassified(
                com.hampushallkvist.biometunes.environment.NotClassifiedReason.NATIVE_UNDERGROUND_BIOME,
            ),
        )

        classifications.forEach { classification ->
            val resolved = resolver.resolvePool(
                context = PlayerContext(
                    biomeId = "minecraft:deep_dark",
                    biomeTags = setOf("minecraft:is_overworld"),
                    dimensionId = "minecraft:overworld",
                    boss = null,
                    environmentalClassification = classification,
                ),
                catalog = catalog,
                bossMusicEnabled = true,
            )

            assertEquals(SelectionSource.EXACT_BIOME, resolved.source)
            assertEquals("biome:minecraft:deep_dark", resolved.key)
        }
    }

    @Test
    fun `weighted exact biome pool can select a later song`() {
        val weightedCatalog = catalog.copy(
            biomes = catalog.biomes + (
                "minecraft:plains" to TrackPool(
                    listOf(
                        WeightedTrack(TrackId("plains"), weight = 8),
                        WeightedTrack(TrackId("forest"), weight = 2),
                    ),
                )
            ),
        )
        val weightedResolver = TrackResolver(
            TrackPoolSelector(BoundedRandom { bound -> 8L.also { require(it < bound) } }),
        )

        val resolved = weightedResolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:plains",
                biomeTags = emptySet(),
                dimensionId = "minecraft:overworld",
                boss = null,
            ),
            catalog = weightedCatalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("forest"), resolved.trackId())
        assertEquals(SelectionSource.EXACT_BIOME, resolved.source)
        assertEquals("track:forest", resolved.notificationKey)
    }

    @Test
    fun `boss music takes precedence over exact biome and uses boss notification family`() {
        // Catches a mutation that checks biome mappings before enabled boss mappings.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:plains",
                biomeTags = setOf("minecraft:is_overworld"),
                dimensionId = "minecraft:overworld",
                boss = BossEncounter.ENDER_DRAGON,
                environmentalClassification = classified(EnvironmentMode.SUBTERRANEAN),
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("ender_dragon"), resolved.trackId())
        assertEquals(SelectionSource.BOSS, resolved.source)
        assertEquals("boss:ender_dragon", resolved.notificationKey)
    }

    @Test
    fun `ender dragon and wither both resolve to their configured boss track`() {
        // Catches a mutation that supports one boss enum value but drops the other.
        val expected = mapOf(
            BossEncounter.ENDER_DRAGON to "ender_dragon",
            BossEncounter.WITHER to "ender_dragon",
        )

        expected.forEach { (boss, trackId) ->
            val resolved = resolver.resolve(
                context = PlayerContext(
                    biomeId = "minecraft:plains",
                    biomeTags = emptySet(),
                    dimensionId = "minecraft:overworld",
                    boss = boss,
                ),
                catalog = catalog,
                bossMusicEnabled = true,
            )

            assertEquals(TrackId(trackId), resolved.trackId())
            assertEquals(SelectionSource.BOSS, resolved.source)
            assertEquals("boss:${boss.catalogKey}", resolved.notificationKey)
        }
    }

    @Test
    fun `exact biome takes precedence over matching biome tags`() {
        // Catches a mutation that scans tags before exact biome mappings.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:plains",
                biomeTags = setOf("minecraft:is_ocean", "minecraft:is_overworld"),
                dimensionId = "minecraft:the_nether",
                boss = null,
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("plains"), resolved.trackId())
        assertEquals(SelectionSource.EXACT_BIOME, resolved.source)
        assertEquals("track:plains", resolved.notificationKey)
    }

    @Test
    fun `first ordered matching biome tag takes precedence over dimension`() {
        // Catches a mutation that iterates player tags instead of the catalog's ordered tag list.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:unknown_biome",
                biomeTags = setOf("minecraft:is_nether", "minecraft:is_ocean"),
                dimensionId = "minecraft:overworld",
                boss = null,
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("ocean"), resolved.trackId())
        assertEquals(SelectionSource.BIOME_TAG, resolved.source)
        assertEquals("track:ocean", resolved.notificationKey)
    }

    @Test
    fun `dimension takes precedence over final fallback`() {
        // Catches a mutation that always returns the fallback when biome and tags miss.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:unknown_biome",
                biomeTags = emptySet(),
                dimensionId = "minecraft:the_nether",
                boss = null,
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("dark_forest"), resolved.trackId())
        assertEquals(SelectionSource.DIMENSION, resolved.source)
        assertEquals("track:dark_forest", resolved.notificationKey)
    }

    @Test
    fun `unknown biome and dimension use final fallback`() {
        // Catches a mutation that returns null or invents a track when every mapping misses.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:unknown_biome",
                biomeTags = emptySet(),
                dimensionId = "minecraft:unknown_dimension",
                boss = null,
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("plains"), resolved.trackId())
        assertEquals(SelectionSource.FALLBACK, resolved.source)
        assertEquals("track:plains", resolved.notificationKey)
    }

    @Test
    fun `disabled boss music skips boss and resolves exact biome`() {
        // Catches a mutation that ignores the bossMusicEnabled setting.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:plains",
                biomeTags = emptySet(),
                dimensionId = "minecraft:overworld",
                boss = BossEncounter.ENDER_DRAGON,
            ),
            catalog = catalog,
            bossMusicEnabled = false,
        )

        assertEquals(TrackId("plains"), resolved.trackId())
        assertEquals(SelectionSource.EXACT_BIOME, resolved.source)
        assertEquals("track:plains", resolved.notificationKey)
    }

    @Test
    fun `real catalog resolves every vanilla biome identifier to its exact configured track pool`() {
        // Catches a mutation that rewrites a catalog mapping and changes resolver output in tandem.
        val expectedBiomes = javaClass.getResourceAsStream("/vanilla-26.2-biomes.txt")!!
            .bufferedReader().readLines().filter(String::isNotBlank)

        assertEquals(66, expectedBiomes.size)
        assertEquals(expectedBiomeTracks.keys, expectedBiomes.toSet())
        expectedBiomes.forEach { biomeId ->
            val resolved = resolver.resolvePool(
                context = PlayerContext(
                    biomeId = biomeId,
                    biomeTags = emptySet(),
                    dimensionId = "minecraft:unknown_dimension",
                    boss = null,
                ),
                catalog = catalog,
                bossMusicEnabled = false,
            )

            val expectedTracks = expectedBiomeTracks.getValue(biomeId).let { primary ->
                if (primary == TrackId("mountains")) {
                    setOf(primary, TrackId("mountains_2"))
                } else {
                    setOf(primary)
                }
            }
            val actualTracks = resolved.pool.entries.filterIsInstance<WeightedTrack>()
            assertEquals(SelectionSource.EXACT_BIOME, resolved.source, biomeId)
            assertEquals(expectedTracks, actualTracks.map { it.track }.toSet(), biomeId)
            assertEquals(setOf(3), actualTracks.map { it.weight }.toSet(), biomeId)
        }
    }

    @Test
    fun `real catalog exposes dedicated mountain and taiga tag pools`() {
        val expected = mapOf(
            "minecraft:is_mountain" to setOf(TrackId("mountains"), TrackId("mountains_2")),
            "minecraft:is_taiga" to setOf(TrackId("taiga")),
        )

        expected.forEach { (tag, expectedTracks) ->
            val resolved = resolver.resolvePool(
                context = PlayerContext(
                    biomeId = "example:unmapped_biome",
                    biomeTags = setOf(tag),
                    dimensionId = "example:unmapped_dimension",
                    boss = null,
                ),
                catalog = catalog,
                bossMusicEnabled = false,
            )
            val actualTracks = resolved.pool.entries.filterIsInstance<WeightedTrack>()

            assertEquals(SelectionSource.BIOME_TAG, resolved.source, tag)
            assertEquals(expectedTracks, actualTracks.map { it.track }.toSet(), tag)
            assertEquals(setOf(3), actualTracks.map { it.weight }.toSet(), tag)
        }
    }

    private val expectedBiomeTracks = mapOf(
        "minecraft:badlands" to TrackId("desert"),
        "minecraft:bamboo_jungle" to TrackId("jungle"),
        "minecraft:basalt_deltas" to TrackId("dark_forest"),
        "minecraft:beach" to TrackId("ocean"),
        "minecraft:birch_forest" to TrackId("birch_forest"),
        "minecraft:cherry_grove" to TrackId("flower_forest"),
        "minecraft:cold_ocean" to TrackId("ocean"),
        "minecraft:crimson_forest" to TrackId("dark_forest"),
        "minecraft:dark_forest" to TrackId("dark_forest"),
        "minecraft:deep_cold_ocean" to TrackId("ocean"),
        "minecraft:deep_dark" to TrackId("underground"),
        "minecraft:deep_frozen_ocean" to TrackId("frozen_ocean"),
        "minecraft:deep_lukewarm_ocean" to TrackId("warm_ocean"),
        "minecraft:deep_ocean" to TrackId("ocean"),
        "minecraft:desert" to TrackId("desert"),
        "minecraft:dripstone_caves" to TrackId("underground"),
        "minecraft:end_barrens" to TrackId("the_end"),
        "minecraft:end_highlands" to TrackId("the_end"),
        "minecraft:end_midlands" to TrackId("the_end"),
        "minecraft:eroded_badlands" to TrackId("desert"),
        "minecraft:flower_forest" to TrackId("flower_forest"),
        "minecraft:forest" to TrackId("forest"),
        "minecraft:frozen_ocean" to TrackId("frozen_ocean"),
        "minecraft:frozen_peaks" to TrackId("snowy"),
        "minecraft:frozen_river" to TrackId("snowy"),
        "minecraft:grove" to TrackId("snowy"),
        "minecraft:ice_spikes" to TrackId("snowy"),
        "minecraft:jagged_peaks" to TrackId("mountains"),
        "minecraft:jungle" to TrackId("jungle"),
        "minecraft:lukewarm_ocean" to TrackId("warm_ocean"),
        "minecraft:lush_caves" to TrackId("underground"),
        "minecraft:mangrove_swamp" to TrackId("swamp"),
        "minecraft:meadow" to TrackId("plains"),
        "minecraft:mushroom_fields" to TrackId("flower_forest"),
        "minecraft:nether_wastes" to TrackId("nether_wastes"),
        "minecraft:ocean" to TrackId("ocean"),
        "minecraft:old_growth_birch_forest" to TrackId("birch_forest"),
        "minecraft:old_growth_pine_taiga" to TrackId("taiga"),
        "minecraft:old_growth_spruce_taiga" to TrackId("taiga"),
        "minecraft:pale_garden" to TrackId("dark_forest"),
        "minecraft:plains" to TrackId("plains"),
        "minecraft:river" to TrackId("ocean"),
        "minecraft:savanna" to TrackId("savanna"),
        "minecraft:savanna_plateau" to TrackId("savanna"),
        "minecraft:small_end_islands" to TrackId("the_end"),
        "minecraft:snowy_beach" to TrackId("snowy_ocean"),
        "minecraft:snowy_plains" to TrackId("snowy"),
        "minecraft:snowy_slopes" to TrackId("snowy"),
        "minecraft:snowy_taiga" to TrackId("snowy"),
        "minecraft:soul_sand_valley" to TrackId("dark_forest"),
        "minecraft:sparse_jungle" to TrackId("jungle"),
        "minecraft:stony_peaks" to TrackId("mountains"),
        "minecraft:stony_shore" to TrackId("mountains"),
        "minecraft:sulfur_caves" to TrackId("underground"),
        "minecraft:sunflower_plains" to TrackId("plains"),
        "minecraft:swamp" to TrackId("swamp"),
        "minecraft:taiga" to TrackId("taiga"),
        "minecraft:the_end" to TrackId("the_end"),
        "minecraft:the_void" to TrackId("the_end"),
        "minecraft:warm_ocean" to TrackId("warm_ocean"),
        "minecraft:warped_forest" to TrackId("dark_forest"),
        "minecraft:windswept_forest" to TrackId("forest"),
        "minecraft:windswept_gravelly_hills" to TrackId("mountains"),
        "minecraft:windswept_hills" to TrackId("mountains"),
        "minecraft:windswept_savanna" to TrackId("savanna"),
        "minecraft:wooded_badlands" to TrackId("desert"),
    )

    private fun ResolvedTrack.trackId(): TrackId = (selection as SelectedTrack).track.id

    private fun classified(mode: EnvironmentMode) = EnvironmentalClassificationResult.Classified(
        mode = mode,
        shelterIntensity = 0.0f,
        diagnostics = EnvironmentalDiagnostics(
            profileDimensionId = "minecraft:overworld",
            evidence = EnvironmentalEvidence(false, 0, 9, 48.0, 1.0, 0, 0),
            rawScores = EnvironmentalScores(0.0, 1.0),
            smoothedScores = EnvironmentalScores(0.0, 1.0),
            constraints = EnvironmentConstraints(false, true),
            decisionBasis = DecisionBasis.WEIGHTED_SCORES,
        ),
    )

    private fun loadCatalog(): TrackCatalog {
        val repoRoot = Path.of(System.getProperty("biometunes.repoRoot"))
        val catalogPath = repoRoot.resolve(
            "apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json",
        )
        return TrackCatalogParser.parse(Files.readString(catalogPath)).getOrThrow()
    }
}
