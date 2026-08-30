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
    fun `settings and action bar text has the complete English translation contract`() {
        // Catches missing or mistyped keys that would surface raw translation IDs in the client UI.
        val translations = Json.parseToJsonElement(
            Files.readString(soundpack.resolve("assets/biometunes/lang/en_us.json")),
        ).jsonObject
        val expected = mapOf(
            "screen.biometunes.title" to "BiomeTunes Settings",
            "screen.biometunes.save" to "Save",
            "screen.biometunes.cancel" to "Cancel",
            "options.biometunes.enabled" to "Enabled",
            "options.biometunes.volume" to "Volume: %s%%",
            "options.biometunes.crossfade" to "Crossfade: %s seconds",
            "options.biometunes.biome_notifications" to "Biome notifications",
            "options.biometunes.boss_music" to "Boss music",
            "options.biometunes.boss_notifications" to "Boss notifications",
            "options.biometunes.environmental_debug_hud" to "Environmental Debug HUD",
            "message.biometunes.biome" to "Now playing: %s",
            "message.biometunes.boss" to "Boss music: %s",
        )

        assertEquals(
            expected,
            expected.keys.associateWith { key -> translations[key]?.jsonPrimitive?.content },
        )
    }

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
            "forest", "frozen_ocean", "jungle", "mountains", "mountains_2", "nether_wastes",
            "ocean", "plains", "savanna", "snowy", "snowy_ocean", "swamp", "taiga", "the_end",
            "underground", "warm_ocean",
        )
        val tracks = catalogJson.getValue("tracks").jsonArray.map { it.jsonObject }
        val trackIds = tracks.map { it.getValue("id").jsonPrimitive.content }.toSet()
        val expectedSoundEventKeys = expectedTrackIds.map { "music.$it" }.toSet()
        val oggTrackIds = Files.list(soundpack.resolve("assets/biometunes/sounds/music")).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "ogg" }
                .map { it.nameWithoutExtension }
                .toList()
                .toSet()
        }

        // Catches a duplicate or otherwise extra catalog track entry.
        assertEquals(21, tracks.size, "catalog must contain exactly 21 track entries")
        assertEquals(tracks.size, trackIds.size, "catalog track IDs must be unique")
        assertEquals(expectedTrackIds, trackIds)
        // Catches any non-music or otherwise extra sound-event key in sounds.json.
        assertEquals(expectedSoundEventKeys, soundsJson.keys)
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

    @Test
    fun `bundled environmental classification is configured only for the overworld`() {
        val profiles = readCatalog().getValue("environmental_profiles").jsonObject

        assertEquals(setOf("minecraft:overworld"), profiles.keys)
        assertEquals(
            setOf(
                "minecraft:deep_dark",
                "minecraft:dripstone_caves",
                "minecraft:lush_caves",
                "minecraft:sulfur_caves",
            ),
            profiles.getValue("minecraft:overworld").jsonObject
                .getValue("native_underground_biomes").jsonArray
                .map { it.jsonPrimitive.content }
                .toSet(),
        )
    }

    private fun readCatalog() = Json.parseToJsonElement(
        Files.readString(catalog.also {
            assertTrue(Files.isRegularFile(it), "catalog must exist at $it")
        }),
    ).jsonObject
}
