package com.hampushallkvist.biometunes.config

import com.hampushallkvist.biometunes.playback.PlaybackOptions
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigStoreTest {
    @Test
    fun `defaults match the approved player experience`() {
        // Catches a mutation that changes an approved player-facing default.
        assertEquals(
            BiomeTunesConfig(
                enabled = true,
                volume = 1f,
                crossfadeSeconds = 5f,
                biomeNotifications = false,
                bossMusic = true,
                bossNotifications = false,
            ),
            BiomeTunesConfig(),
        )
    }

    @Test
    fun `normalization clamps public ranges and derives playback options`() {
        // Catches missing or incorrect player-safe bounds before options reach playback.
        val normalized = BiomeTunesConfig(volume = 4f, crossfadeSeconds = -2f).normalized()

        assertEquals(1f, normalized.volume)
        assertEquals(0f, normalized.crossfadeSeconds)
        assertEquals(PlaybackOptions(volume = 1f, crossfadeTicks = 0), normalized.playbackOptions)

        val upperFade = BiomeTunesConfig(crossfadeSeconds = 20f).normalized()
        assertEquals(15f, upperFade.crossfadeSeconds)
        assertEquals(PlaybackOptions(volume = 1f, crossfadeTicks = 300), upperFade.playbackOptions)
    }

    @Test
    fun `save and load round trip normalized player settings`() = withConfigPath { path ->
        // Catches persistence that omits settings, writes unnormalized values, or reads a different codec shape.
        val settings = BiomeTunesConfig(
            enabled = false,
            volume = 0.35f,
            crossfadeSeconds = 7.5f,
            biomeNotifications = true,
            bossMusic = false,
            bossNotifications = true,
        )
        val store = ConfigStore(path) { _, _ -> }

        assertTrue(store.save(settings).isSuccess)

        assertEquals(settings, store.load())
        val saved = Files.readString(path)
        assertTrue(saved.contains("\"crossfadeSeconds\": 7.5"))
        assertTrue(saved.contains("\n"))
    }

    @Test
    fun `load ignores unknown JSON fields for forward compatibility`() = withConfigPath { path ->
        // Catches a decoder that rejects files written by a newer version of the mod.
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            """
            {
              "enabled": false,
              "volume": 0.4,
              "crossfadeSeconds": 2.5,
              "biomeNotifications": true,
              "bossMusic": false,
              "bossNotifications": true,
              "futureSetting": "new-version-value"
            }
            """.trimIndent(),
        )

        assertEquals(
            BiomeTunesConfig(
                enabled = false,
                volume = 0.4f,
                crossfadeSeconds = 2.5f,
                biomeNotifications = true,
                bossMusic = false,
                bossNotifications = true,
            ),
            ConfigStore(path) { _, _ -> }.load(),
        )
    }

    @Test
    fun `malformed JSON returns defaults without deleting or changing the user file`() = withConfigPath { path ->
        // Catches error recovery that overwrites or removes a malformed file the user may need to repair.
        Files.createDirectories(path.parent)
        val malformed = "{ \"volume\": 0.5,"
        Files.writeString(path, malformed)
        val warnings = mutableListOf<String>()

        assertEquals(BiomeTunesConfig(), ConfigStore(path) { message, _ -> warnings += message }.load())

        assertTrue(Files.exists(path))
        assertContentEquals(malformed.encodeToByteArray(), Files.readAllBytes(path))
        assertEquals(1, warnings.size)
    }

    @Test
    fun `save atomically replaces existing config bytes without leaving a temporary file`() = withConfigPath { path ->
        // Catches replacement that preserves stale file content or leaves an interrupted-write temporary sibling.
        Files.createDirectories(path.parent)
        val original = "{\n  \"enabled\": true\n}\n".encodeToByteArray()
        Files.write(path, original)
        val replacement = BiomeTunesConfig(enabled = false, volume = 0.25f)
        val store = ConfigStore(path) { _, _ -> }

        assertTrue(store.save(replacement).isSuccess)

        assertTrue(Files.exists(path))
        assertFalse(Files.readAllBytes(path).contentEquals(original))
        assertEquals(replacement, store.load())
        assertFalse(Files.exists(path.resolveSibling("${path.fileName}.tmp")))
    }

    @Test
    fun `save failure stays in the result and removes its temporary sibling`() = withConfigPath { path ->
        // Catches a failed final replacement that escapes the Result boundary or leaves partial config output behind.
        Files.createDirectories(path)
        val warnings = mutableListOf<String>()

        val result = ConfigStore(path) { message, _ -> warnings += message }.save(BiomeTunesConfig())

        assertTrue(result.isFailure)
        assertEquals(1, warnings.size)
        assertTrue(Files.isDirectory(path))
        assertFalse(Files.exists(path.resolveSibling("${path.fileName}.tmp")))
    }

    private fun withConfigPath(block: (Path) -> Unit) {
        val directory = createTempDirectory("biometunes-config-store-test")
        try {
            block(directory.resolve("config").resolve("biometunes.json"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
