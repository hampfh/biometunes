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
        // Catches a mutation that drops, rewrites, or fails to resolve any of the 66 vanilla biome IDs.
        val expectedBiomes = javaClass.getResourceAsStream("/vanilla-26.2-biomes.txt")!!
            .bufferedReader().readLines().filter(String::isNotBlank)

        assertEquals(66, expectedBiomes.size)
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

            assertEquals(SelectionSource.EXACT_BIOME, resolved.source, biomeId)
            assertEquals(catalog.biomes.getValue(biomeId), resolved.track.id, biomeId)
            assertEquals("track:${catalog.biomes.getValue(biomeId).value}", resolved.notificationKey, biomeId)
        }
    }

    private fun loadCatalog(): TrackCatalog {
        val repoRoot = Path.of(System.getProperty("biometunes.repoRoot"))
        val catalogPath = repoRoot.resolve(
            "apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json",
        )
        return TrackCatalogParser.parse(Files.readString(catalogPath)).getOrThrow()
    }
}
