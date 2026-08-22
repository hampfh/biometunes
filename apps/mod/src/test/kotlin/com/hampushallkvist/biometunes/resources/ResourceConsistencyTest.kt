package com.hampushallkvist.biometunes.resources

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResourceConsistencyTest {
    private val root = Path.of(System.getProperty("biometunes.repoRoot"))
    private val soundpack = root.resolve("apps/soundpack")
    private val catalog = root.resolve(
        "apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json",
    )

    @Test
    fun `every catalog track has one canonical streamed sound event ogg subtitle and attribution`() {
        // This fails if a track loses its sound event, OGG, attribution, or translated subtitle.
        val catalogJson = readCatalog()
        val soundsJson = Json.parseToJsonElement(
            Files.readString(soundpack.resolve("assets/biometunes/sounds.json")),
        ).jsonObject
        val translations = Json.parseToJsonElement(
            Files.readString(soundpack.resolve("assets/biometunes/lang/en_us.json")),
        ).jsonObject
        val readme = Files.readString(soundpack.resolve("README.md"))

        val expectedTrackIds = setOf(
            "birch_forest", "dark_forest", "desert", "ender_dragon", "flower_forest",
            "forest", "jungle", "mountains", "ocean", "plains", "savanna", "snowy",
            "snowy_ocean", "the_end", "warm_ocean",
        )
        val tracks = catalogJson.getValue("tracks").jsonArray.map { it.jsonObject }
        val trackIds = tracks.map { it.getValue("id").jsonPrimitive.content }.toSet()
        val soundEventIds = soundsJson.keys
            .filter { it.startsWith("music.") }
            .map { it.removePrefix("music.") }
            .toSet()
        val oggTrackIds = Files.list(soundpack.resolve("assets/biometunes/sounds/music")).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "ogg" }
                .map { it.nameWithoutExtension }
                .toList()
                .toSet()
        }

        assertEquals(expectedTrackIds, trackIds)
        assertEquals(expectedTrackIds, soundEventIds)
        assertEquals(expectedTrackIds, oggTrackIds)

        tracks.forEach { track ->
            val id = track.getValue("id").jsonPrimitive.content
            val artist = track.getValue("artist").jsonPrimitive.content
            val subtitleKey = "subtitles.biometunes.music.$id"
            val sound = assertNotNull(soundsJson["music.$id"]).jsonObject

            assertEquals("biometunes:music.$id", track.getValue("sound_event").jsonPrimitive.content)
            assertTrue(track.getValue("title").jsonPrimitive.content.isNotBlank())
            assertTrue(artist.isNotBlank())
            assertTrue(readme.contains(artist), "README credits must retain $artist")
            assertEquals("music", sound.getValue("category").jsonPrimitive.content)
            assertEquals(subtitleKey, sound.getValue("subtitle").jsonPrimitive.content)
            assertTrue(translations.getValue(subtitleKey).jsonPrimitive.content.isNotBlank())

            val soundDefinitions = sound.getValue("sounds").jsonArray
            assertEquals(1, soundDefinitions.size)
            val soundDefinition = soundDefinitions.single().jsonObject
            assertEquals("biometunes:music/$id", soundDefinition.getValue("name").jsonPrimitive.content)
            val stream = soundDefinition.getValue("stream").jsonPrimitive
            assertFalse(stream.toString().startsWith('"'), "stream must be a JSON boolean")
            assertTrue(stream.boolean)
        }
    }

    @Test
    fun `catalog covers exactly the vanilla 26_2 biome inventory`() {
        // This fails when a supported vanilla biome is omitted or an unexpected biome is added.
        val expected = javaClass.getResourceAsStream("/vanilla-26.2-biomes.txt")!!
            .bufferedReader().readLines().filter(String::isNotBlank).toSet()
        val catalogJson = readCatalog()
        val actual = catalogJson.getValue("biomes").jsonObject.keys

        assertEquals(expected, actual)
    }

    private fun readCatalog() = Json.parseToJsonElement(
        Files.readString(catalog.also {
            assertTrue(Files.isRegularFile(it), "catalog must exist at $it")
        }),
    ).jsonObject
}
