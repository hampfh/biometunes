package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackDefinition
import com.hampushallkvist.biometunes.catalog.TrackId
import com.hampushallkvist.biometunes.catalog.SilenceDurationRange
import com.hampushallkvist.biometunes.catalog.TrackPool
import com.hampushallkvist.biometunes.catalog.WeightedSilence
import com.hampushallkvist.biometunes.catalog.WeightedTrack
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult
import com.hampushallkvist.biometunes.environment.DecisionBasis
import com.hampushallkvist.biometunes.environment.EnvironmentConstraints
import com.hampushallkvist.biometunes.environment.EnvironmentMode
import com.hampushallkvist.biometunes.environment.EnvironmentalDiagnostics
import com.hampushallkvist.biometunes.environment.EnvironmentalEvidence
import com.hampushallkvist.biometunes.environment.EnvironmentalProfile
import com.hampushallkvist.biometunes.environment.EnvironmentalSampling
import com.hampushallkvist.biometunes.environment.EnvironmentalScores
import com.hampushallkvist.biometunes.environment.EnvironmentalThresholds
import com.hampushallkvist.biometunes.environment.EnvironmentalWeights
import com.hampushallkvist.biometunes.environment.NotClassifiedReason
import com.hampushallkvist.biometunes.environment.ShelteredAudioSettings
import com.hampushallkvist.biometunes.playback.AudioAdapter
import com.hampushallkvist.biometunes.playback.AudioHandle
import com.hampushallkvist.biometunes.playback.PlaybackController
import com.hampushallkvist.biometunes.selection.BossEncounter
import com.hampushallkvist.biometunes.selection.BoundedRandom
import com.hampushallkvist.biometunes.selection.PlayerContext
import com.hampushallkvist.biometunes.selection.TrackPoolSelector
import com.hampushallkvist.biometunes.selection.TrackResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicDirectorTest {
    @Test
    fun `outside to sheltered keeps the selected voice and changes only its treatment`() {
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)
        val profile = EnvironmentalProfile(
            nativeUndergroundBiomes = emptySet(),
            subterraneanTracks = pool(desert),
            sampling = EnvironmentalSampling(6, 2, 48),
            weights = EnvironmentalWeights(1.0, 0.25, 0.25, 1.0, 0.25, 0.15, 0.05),
            thresholds = EnvironmentalThresholds(0.65, 0.45, 0.70, 0.55, 0.25, 0.40),
            smoothingSeconds = 0.0,
            shelteredAudio = ShelteredAudioSettings(0.85f, 0.35f),
        )
        val environmentCatalog = catalog.copy(
            environmentalProfiles = mapOf("minecraft:overworld" to profile),
        )

        director.tick(environmentalContext(EnvironmentMode.OUTSIDE, 0.0f), environmentCatalog, config())
        director.tick(environmentalContext(EnvironmentMode.SHELTERED, 1.0f), environmentCatalog, config())

        assertEquals(listOf(forest.id), adapter.starts)
        assertEquals(0.85f, adapter.gainChanges.last().second, absoluteTolerance = 0.000_1f)
        assertEquals(forest.id, adapter.lowPassChanges.last().first)
        assertEquals(0.35f, adapter.lowPassChanges.last().second, absoluteTolerance = 0.000_1f)
    }

    @Test
    fun `changing biome rerolls during an unchanged boss encounter`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(0L, 1L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val bossCatalog = catalog.copy(
            bosses = mapOf(
                "ender_dragon" to TrackPool(
                    listOf(
                        WeightedTrack(forest.id, weight = 1),
                        WeightedTrack(desert.id, weight = 1),
                    ),
                ),
            ),
        )

        director.tick(dragonOverPlains, bossCatalog, config())
        director.tick(context("minecraft:desert", BossEncounter.ENDER_DRAGON), bossCatalog, config())

        assertEquals(listOf(forest.id, desert.id), adapter.starts)
    }

    @Test
    fun `paused ticks do not consume an active silence duration`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(0L, 0L, 1L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val silenceCatalog = catalog.copy(
            biomes = catalog.biomes + (
                "minecraft:plains" to TrackPool(
                    listOf(
                        WeightedSilence(1, SilenceDurationRange(1, 1)),
                        WeightedTrack(forest.id, weight = 1),
                    ),
                )
            ),
        )

        repeat(25) { director.tick(plains, silenceCatalog, config(), paused = true) }
        repeat(20) { director.tick(plains, silenceCatalog, config()) }

        assertTrue(adapter.starts.isEmpty())
        assertTrue(adapter.vanillaMusicSuppressed)

        director.tick(plains, silenceCatalog, config())

        assertEquals(listOf(forest.id), adapter.starts)
    }

    @Test
    fun `changing biome rerolls even when both biomes use the same weighted pool`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(0L, 1L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val sharedPool = TrackPool(
            listOf(
                WeightedTrack(forest.id, weight = 1),
                WeightedTrack(desert.id, weight = 1),
            ),
        )
        val sharedCatalog = catalog.copy(
            biomes = catalog.biomes + mapOf(
                "minecraft:plains" to sharedPool,
                "minecraft:grove" to sharedPool,
            ),
        )

        director.tick(plains, sharedCatalog, config())
        director.tick(grove, sharedCatalog, config())

        assertEquals(listOf(forest.id, desert.id), adapter.starts)
    }

    @Test
    fun `disabled boss changes do not reroll an unchanged biome pool`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(0L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val bossDisabled = config(bossMusic = false)

        director.tick(plains, catalog, bossDisabled)
        director.tick(dragonOverPlains, catalog, bossDisabled)

        assertEquals(listOf(forest.id), adapter.starts)
        assertEquals(setOf(forest.id), adapter.playingTrackIds)
    }

    @Test
    fun `silence duration starts only after the previous track has faded out`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(0L, 0L, 0L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val silenceCatalog = catalog.copy(
            biomes = catalog.biomes + (
                "minecraft:desert" to TrackPool(
                    listOf(WeightedSilence(1, SilenceDurationRange(1, 1))),
                )
            ),
        )

        director.tick(plains, silenceCatalog, config())
        repeat(21) { director.tick(context("minecraft:desert"), silenceCatalog, config()) }

        assertEquals(listOf(forest.id), adapter.starts)
        assertEquals(setOf(forest.id), adapter.playingTrackIds)
        assertTrue(adapter.vanillaMusicSuppressed)
    }

    @Test
    fun `inline silence suppresses vanilla music for its sampled duration then rerolls`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(1L, 0L, 0L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val silenceCatalog = catalog.copy(
            biomes = catalog.biomes + (
                "minecraft:plains" to TrackPool(
                    listOf(
                        WeightedTrack(forest.id, weight = 1),
                        WeightedSilence(1, SilenceDurationRange(1, 1)),
                    ),
                )
            ),
        )

        director.tick(plains, silenceCatalog, config())
        repeat(19) { director.tick(plains, silenceCatalog, config()) }

        assertTrue(adapter.vanillaMusicSuppressed)
        assertTrue(adapter.starts.isEmpty())

        director.tick(plains, silenceCatalog, config())

        assertEquals(listOf(forest.id), adapter.starts)
        assertEquals(setOf(forest.id), adapter.playingTrackIds)
    }

    @Test
    fun `finished track rerolls its biome pool before starting the next track`() {
        val adapter = DirectorAudioAdapter()
        val rolls = ArrayDeque(listOf(0L, 1L))
        val director = MusicDirector(
            TrackResolver(
                TrackPoolSelector(BoundedRandom { bound ->
                    rolls.removeFirst().also { require(it in 0 until bound) }
                }),
            ),
            PlaybackController(adapter),
        )
        val rerollCatalog = catalog.copy(
            biomes = catalog.biomes + (
                "minecraft:plains" to TrackPool(
                    listOf(
                        WeightedTrack(forest.id, weight = 1),
                        WeightedTrack(desert.id, weight = 1),
                    ),
                )
            ),
        )

        director.tick(plains, rerollCatalog, config())
        adapter.finish(forest.id)
        director.tick(plains, rerollCatalog, config())

        assertEquals(listOf(forest.id, desert.id), adapter.starts)
        assertEquals(setOf(desert.id), adapter.playingTrackIds)
    }

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
    fun `enabling biome notifications emits once for an unchanged biome and can be repeated after disabling`() {
        // Catches key-only de-duplication that consumes a biome notice while its family is disabled.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)

        val notices = listOf(
            director.tick(plains, catalog, config(biomeNotifications = false)),
            director.tick(plains, catalog, config(biomeNotifications = true)),
            director.tick(plains, catalog, config(biomeNotifications = true)),
            director.tick(plains, catalog, config(biomeNotifications = false)),
            director.tick(plains, catalog, config(biomeNotifications = true)),
        )

        assertEquals(
            listOf(
                null,
                PlayerNotice("message.biometunes.biome", "Forest Song"),
                null,
                null,
                PlayerNotice("message.biometunes.biome", "Forest Song"),
            ),
            notices,
        )
        assertEquals(listOf(forest.id), adapter.starts)
    }

    @Test
    fun `enabling boss notifications emits once for an unchanged boss and can be repeated after disabling`() {
        // Catches key-only de-duplication that consumes a boss notice while its family is disabled.
        val adapter = DirectorAudioAdapter()
        val director = director(adapter)

        val notices = listOf(
            director.tick(dragonOverPlains, catalog, config(bossNotifications = false)),
            director.tick(dragonOverPlains, catalog, config(bossNotifications = true)),
            director.tick(dragonOverPlains, catalog, config(bossNotifications = true)),
            director.tick(dragonOverPlains, catalog, config(bossNotifications = false)),
            director.tick(dragonOverPlains, catalog, config(bossNotifications = true)),
        )

        assertEquals(
            listOf(
                null,
                PlayerNotice("message.biometunes.boss", "Dragon Song"),
                null,
                null,
                PlayerNotice("message.biometunes.boss", "Dragon Song"),
            ),
            notices,
        )
        assertEquals(listOf(dragon.id), adapter.starts)
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
        val sharedBossCatalog = catalog.copy(bosses = mapOf("ender_dragon" to pool(forest)))
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
        MusicDirector(
            TrackResolver(TrackPoolSelector(BoundedRandom { 0 })),
            PlaybackController(adapter),
        )

    private fun config(
        enabled: Boolean = true,
        crossfadeSeconds: Float = 5f,
        biomeNotifications: Boolean = false,
        bossMusic: Boolean = true,
        bossNotifications: Boolean = false,
    ) = BiomeTunesConfig(
        enabled = enabled,
        crossfadeSeconds = crossfadeSeconds,
        biomeNotifications = biomeNotifications,
        bossMusic = bossMusic,
        bossNotifications = bossNotifications,
    )

    private companion object {
        val forest = track("forest", "Forest Song")
        val desert = track("desert", "Desert Song")
        val dragon = track("dragon", "Dragon Song")
        val catalog = TrackCatalog(
            tracks = listOf(forest, desert, dragon).associateBy(TrackDefinition::id),
            globalSilence = WeightedSilence(1, SilenceDurationRange(30, 120)),
            bosses = mapOf("ender_dragon" to pool(dragon)),
            biomes = mapOf(
                "minecraft:plains" to pool(forest),
                "minecraft:grove" to pool(forest),
                "minecraft:desert" to pool(desert),
            ),
            biomeTags = emptyList(),
            dimensions = mapOf("minecraft:the_nether" to pool(desert)),
            environmentalProfiles = emptyMap(),
            fallback = pool(forest),
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

        fun pool(track: TrackDefinition) = TrackPool(listOf(WeightedTrack(track.id, weight = 9)))

        fun context(biomeId: String, boss: BossEncounter? = null) = PlayerContext(
            biomeId = biomeId,
            biomeTags = emptySet(),
            dimensionId = "minecraft:overworld",
            boss = boss,
            environmentalClassification = EnvironmentalClassificationResult.NotClassified(
                NotClassifiedReason.NO_PROFILE,
            ),
        )

        fun environmentalContext(mode: EnvironmentMode, shelterIntensity: Float) = PlayerContext(
            biomeId = "minecraft:plains",
            biomeTags = emptySet(),
            dimensionId = "minecraft:overworld",
            boss = null,
            environmentalClassification = EnvironmentalClassificationResult.Classified(
                mode = mode,
                shelterIntensity = shelterIntensity,
                diagnostics = EnvironmentalDiagnostics(
                    profileDimensionId = "minecraft:overworld",
                    evidence = EnvironmentalEvidence(false, 0, 9, 1.0, 0.5, 0, 0),
                    rawScores = EnvironmentalScores(0.2, 0.4),
                    smoothedScores = EnvironmentalScores(0.2, 0.4),
                    constraints = EnvironmentConstraints(false, true),
                    decisionBasis = DecisionBasis.WEIGHTED_SCORES,
                ),
            ),
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
    val lowPassChanges = mutableListOf<Pair<TrackId, Float>>()
    val suppressionChanges = mutableListOf<Boolean>()

    var vanillaMusicSuppressed = false
        private set

    val playingTrackIds: Set<TrackId>
        get() = instances.values.filter(Instance::playing).mapTo(linkedSetOf()) { it.track.id }

    fun finish(trackId: TrackId) {
        instances.values.single { it.track.id == trackId && it.playing }.playing = false
    }

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

    override fun setLowPass(handle: AudioHandle, highFrequencyGain: Float) {
        val instance = instances.getValue(handle as Handle)
        lowPassChanges += instance.track.id to highFrequencyGain
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
