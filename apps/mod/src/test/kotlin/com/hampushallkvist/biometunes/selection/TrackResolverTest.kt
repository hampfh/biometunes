package com.hampushallkvist.biometunes.selection

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackCatalogParser
import com.hampushallkvist.biometunes.catalog.TrackId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackResolverTest {
    private val resolver = TrackResolver()
    private val catalog = loadCatalog()

    @Test
    fun `boss music takes precedence over exact biome and uses boss notification family`() {
        // Catches a mutation that checks biome mappings before enabled boss mappings.
        val resolved = resolver.resolve(
            context = PlayerContext(
                biomeId = "minecraft:plains",
                biomeTags = setOf("minecraft:is_overworld"),
                dimensionId = "minecraft:overworld",
                boss = BossEncounter.ENDER_DRAGON,
            ),
            catalog = catalog,
            bossMusicEnabled = true,
        )

        assertEquals(TrackId("ender_dragon"), resolved.track.id)
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

            assertEquals(TrackId(trackId), resolved.track.id)
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

        assertEquals(TrackId("plains"), resolved.track.id)
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

        assertEquals(TrackId("ocean"), resolved.track.id)
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

        assertEquals(TrackId("dark_forest"), resolved.track.id)
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

        assertEquals(TrackId("plains"), resolved.track.id)
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

        assertEquals(TrackId("plains"), resolved.track.id)
        assertEquals(SelectionSource.EXACT_BIOME, resolved.source)
        assertEquals("track:plains", resolved.notificationKey)
    }

    @Test
    fun `real catalog resolves every vanilla biome identifier to its exact configured track`() {
        // Catches a mutation that rewrites a catalog mapping and changes resolver output in tandem.
        val expectedBiomes = javaClass.getResourceAsStream("/vanilla-26.2-biomes.txt")!!
            .bufferedReader().readLines().filter(String::isNotBlank)

        assertEquals(66, expectedBiomes.size)
        assertEquals(expectedBiomeTracks.keys, expectedBiomes.toSet())
        expectedBiomes.forEach { biomeId ->
            val resolved = resolver.resolve(
                context = PlayerContext(
                    biomeId = biomeId,
                    biomeTags = emptySet(),
                    dimensionId = "minecraft:unknown_dimension",
                    boss = null,
                ),
                catalog = catalog,
                bossMusicEnabled = false,
            )

            val expectedTrack = expectedBiomeTracks.getValue(biomeId)
            assertEquals(SelectionSource.EXACT_BIOME, resolved.source, biomeId)
            assertEquals(expectedTrack, resolved.track.id, biomeId)
            assertEquals("track:${expectedTrack.value}", resolved.notificationKey, biomeId)
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
        "minecraft:deep_dark" to TrackId("dark_forest"),
        "minecraft:deep_frozen_ocean" to TrackId("snowy_ocean"),
        "minecraft:deep_lukewarm_ocean" to TrackId("warm_ocean"),
        "minecraft:deep_ocean" to TrackId("ocean"),
        "minecraft:desert" to TrackId("desert"),
        "minecraft:dripstone_caves" to TrackId("mountains"),
        "minecraft:end_barrens" to TrackId("the_end"),
        "minecraft:end_highlands" to TrackId("the_end"),
        "minecraft:end_midlands" to TrackId("the_end"),
        "minecraft:eroded_badlands" to TrackId("desert"),
        "minecraft:flower_forest" to TrackId("flower_forest"),
        "minecraft:forest" to TrackId("forest"),
        "minecraft:frozen_ocean" to TrackId("snowy_ocean"),
        "minecraft:frozen_peaks" to TrackId("snowy"),
        "minecraft:frozen_river" to TrackId("snowy"),
        "minecraft:grove" to TrackId("snowy"),
        "minecraft:ice_spikes" to TrackId("snowy"),
        "minecraft:jagged_peaks" to TrackId("mountains"),
        "minecraft:jungle" to TrackId("jungle"),
        "minecraft:lukewarm_ocean" to TrackId("warm_ocean"),
        "minecraft:lush_caves" to TrackId("jungle"),
        "minecraft:mangrove_swamp" to TrackId("forest"),
        "minecraft:meadow" to TrackId("plains"),
        "minecraft:mushroom_fields" to TrackId("flower_forest"),
        "minecraft:nether_wastes" to TrackId("dark_forest"),
        "minecraft:ocean" to TrackId("ocean"),
        "minecraft:old_growth_birch_forest" to TrackId("birch_forest"),
        "minecraft:old_growth_pine_taiga" to TrackId("forest"),
        "minecraft:old_growth_spruce_taiga" to TrackId("forest"),
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
        "minecraft:sulfur_caves" to TrackId("mountains"),
        "minecraft:sunflower_plains" to TrackId("plains"),
        "minecraft:swamp" to TrackId("forest"),
        "minecraft:taiga" to TrackId("forest"),
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

    private fun loadCatalog(): TrackCatalog {
        val repoRoot = Path.of(System.getProperty("biometunes.repoRoot"))
        val catalogPath = repoRoot.resolve(
            "apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json",
        )
        return TrackCatalogParser.parse(Files.readString(catalogPath)).getOrThrow()
    }
}
