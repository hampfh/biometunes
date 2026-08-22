package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.playback.AudioAdapter
import com.hampushallkvist.biometunes.playback.AudioHandle
import com.hampushallkvist.biometunes.playback.PlaybackController
import com.hampushallkvist.biometunes.selection.BossEncounter
import com.hampushallkvist.biometunes.selection.PlayerContext
import com.hampushallkvist.biometunes.selection.TrackResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicDirectorTest {
    @Test
    fun `null context stops owned playback and releases vanilla music`() {
        // Catches a mutation that keeps the last biome playing while no player context is available.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        director.tick(plains, catalog, config())

        val notice = director.tick(null, catalog, config())

        assertNull(notice)
        assertEquals(listOf(forest.id), adapter.stops)
        assertTrue(adapter.playingTrackIds.isEmpty())
        assertFalse(adapter.vanillaMusicSuppressed)
    }

    @Test
    fun `disabled configuration stops owned playback and releases vanilla music`() {
        // Catches a mutation that resolves and retains music after the feature is disabled.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        director.tick(plains, catalog, config())

        val notice = director.tick(plains, catalog, config(enabled = false))

        assertNull(notice)
        assertEquals(listOf(forest.id), adapter.stops)
        assertTrue(adapter.playingTrackIds.isEmpty())
        assertFalse(adapter.vanillaMusicSuppressed)
    }

    @Test
    fun `biome notification is emitted once for a notification key`() {
        // Catches a mutation that emits an action-bar notice on every 20 Hz director tick.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        val notices = List(3) {
            director.tick(plains, catalog, config(biomeNotifications = true))
        }

        assertEquals(
            listOf(PlayerNotice("message.biometunes.biome", "Forest Song"), null, null),
            notices,
        )
        assertEquals(listOf(forest.id), adapter.starts)
        assertEquals(setOf(forest.id), adapter.playingTrackIds)
    }

    @Test
    fun `two biomes mapped to one track neither restart nor notify twice`() {
        // Catches biome-id notification keys and playback comparison by context rather than TrackId.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        val enabled = config(biomeNotifications = true)

        val first = director.tick(plains, catalog, enabled)
        val second = director.tick(grove, catalog, enabled)

        assertEquals(PlayerNotice("message.biometunes.biome", "Forest Song"), first)
        assertNull(second)
        assertEquals(listOf(forest.id), adapter.starts)
        assertEquals(setOf(forest.id), adapter.playingTrackIds)
    }

    @Test
    fun `boss and biome notification families follow their separate settings`() {
        // Catches use of the biome toggle/key for boss selections or the boss toggle/key for biome selections.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)

        val biomeNotice = director.tick(
            plains,
            catalog,
            config(biomeNotifications = true, bossNotifications = false),
        )
        val hiddenBossNotice = director.tick(
            dragonOverPlains,
            catalog,
            config(biomeNotifications = true, bossNotifications = false),
        )
        director.stop()
        val bossNotice = director.tick(
            dragonOverPlains,
            catalog,
            config(biomeNotifications = false, bossNotifications = true),
        )

        assertEquals(PlayerNotice("message.biometunes.biome", "Forest Song"), biomeNotice)
        assertNull(hiddenBossNotice)
        assertEquals(PlayerNotice("message.biometunes.boss", "Dragon Song"), bossNotice)
    }

    @Test
    fun `boss entry and exit change notice family even when both selections share one track`() {
        // Catches notification de-duplication by TrackId without preserving boss-versus-biome transitions.
        val adapter = DirectorAudioAdapter()
        val sharedBossCatalog = catalog.copy(bosses = mapOf("ender_dragon" to forest.id))
        val director = director(adapter)
        val enabled = config(biomeNotifications = true, bossNotifications = true)

        val notices = listOf(
            director.tick(plains, sharedBossCatalog, enabled),
            director.tick(dragonOverPlains, sharedBossCatalog, enabled),
            director.tick(plains, sharedBossCatalog, enabled),
        )

        assertEquals(
            listOf(
                PlayerNotice("message.biometunes.biome", "Forest Song"),
                PlayerNotice("message.biometunes.boss", "Forest Song"),
                PlayerNotice("message.biometunes.biome", "Forest Song"),
            ),
            notices,
        )
        assertEquals(listOf(forest.id), adapter.starts)
        assertTrue(adapter.stops.isEmpty())
    }

    @Test
    fun `leaving a boss returns to the biome track through playback`() {
        // Catches a mutation that latches the boss resolution after the encounter disappears.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        val immediate = config(crossfadeSeconds = 0f)

        director.tick(plains, catalog, immediate)
        director.tick(dragonOverPlains, catalog, immediate)
        director.tick(plains, catalog, immediate)

        assertEquals(listOf(forest.id, dragon.id, forest.id), adapter.starts)
        assertEquals(listOf(forest.id, dragon.id), adapter.stops)
        assertEquals(setOf(forest.id), adapter.playingTrackIds)
        assertTrue(adapter.vanillaMusicSuppressed)
    }

    @Test
    fun `stop resets notification state for a later playback session`() {
        // Catches a mutation that suppresses the first notice after disconnect or resource reload.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        val enabled = config(biomeNotifications = true)
        val first = director.tick(plains, catalog, enabled)

        director.stop()
        val afterStop = director.tick(plains, catalog, enabled)

        assertEquals(PlayerNotice("message.biometunes.biome", "Forest Song"), first)
        assertEquals(PlayerNotice("message.biometunes.biome", "Forest Song"), afterStop)
    }

    private fun director(adapter: DirectorAudioAdapter) =
        MusicDirector(TrackResolver(), PlaybackController(adapter))

    private fun config(
        enabled: Boolean = true,
        crossfadeSeconds: Float = 5f,
        biomeNotifications: Boolean = false,
        bossNotifications: Boolean = false,
    ) = BiomeTunesConfig(
        enabled = enabled,
        crossfadeSeconds = crossfadeSeconds,
        biomeNotifications = biomeNotifications,
        bossNotifications = bossNotifications,
    )

    private companion object {
        val forest = track("forest", "Forest Song")
        val desert = track("desert", "Desert Song")
        val dragon = track("dragon", "Dragon Song")
        val catalog = TrackCatalog(
            tracks = listOf(forest, desert, dragon).associateBy(TrackDefinition::id),
            bosses = mapOf("ender_dragon" to dragon.id),
            biomes = mapOf(
                "minecraft:plains" to forest.id,
                "minecraft:grove" to forest.id,
                "minecraft:desert" to desert.id,
            ),
            biomeTags = emptyList(),
            dimensions = mapOf("minecraft:the_nether" to desert.id),
            fallback = forest.id,
        )
        val plains = context("minecraft:plains")
        val grove = context("minecraft:grove")
        val dragonOverPlains = context("minecraft:plains", BossEncounter.ENDER_DRAGON)

        fun track(id: String, title: String) = TrackDefinition(
            id = TrackId(id),
            soundEvent = "biometunes:music.$id",
            title = title,
            artist = "Test Artist",
        )

        fun context(biomeId: String, boss: BossEncounter? = null) = PlayerContext(
            biomeId = biomeId,
            biomeTags = emptySet(),
            dimensionId = "minecraft:overworld",
            boss = boss,
        )
    }
}

private class DirectorAudioAdapter : AudioAdapter {
    private data class Handle(val serial: Int) : AudioHandle
    private data class Instance(
        val track: TrackDefinition,
        var gain: Float,
        var playing: Boolean = true,
    )

    private var nextSerial = 0
    private val instances = linkedMapOf<Handle, Instance>()
    val starts = mutableListOf<TrackId>()
    val stops = mutableListOf<TrackId>()
    val gainChanges = mutableListOf<Pair<TrackId, Float>>()
    val suppressionChanges = mutableListOf<Boolean>()

    var vanillaMusicSuppressed = false
        private set

    val playingTrackIds: Set<TrackId>
        get() = instances.values.filter(Instance::playing).mapTo(linkedSetOf()) { it.track.id }

    override fun start(track: TrackDefinition, initialGain: Float): AudioHandle =
        Handle(nextSerial++).also { handle ->
            starts += track.id
            instances[handle] = Instance(track, initialGain)
        }

    override fun setGain(handle: AudioHandle, gain: Float) {
        val instance = instances.getValue(handle as Handle)
        instance.gain = gain
        gainChanges += instance.track.id to gain
    }

    override fun stop(handle: AudioHandle) {
        val instance = requireNotNull(instances.remove(handle as Handle))
        stops += instance.track.id
    }

    override fun isPlaying(handle: AudioHandle): Boolean =
        instances[handle as Handle]?.playing == true

    override fun setVanillaMusicSuppressed(suppressed: Boolean) {
        vanillaMusicSuppressed = suppressed
        suppressionChanges += suppressed
    }
}
