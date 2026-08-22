package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.ReloadableTrackCatalog
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.config.ConfigStore
import com.hampushallkvist.biometunes.playback.PlaybackController
import com.hampushallkvist.biometunes.selection.PlayerContext
import com.hampushallkvist.biometunes.selection.TrackResolver
import com.hampushallkvist.biometunes.ui.BiomeTunesConfigScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.packs.PackType
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory

object BiomeTunesClient : ClientModInitializer {
    internal val logger = LoggerFactory.getLogger("BiomeTunes")

    private lateinit var currentConfig: BiomeTunesConfig
    private lateinit var configStore: ConfigStore
    private lateinit var audioAdapter: MinecraftAudioAdapter
    private lateinit var director: MusicDirector
    private lateinit var sampler: MinecraftContextSampler
    private lateinit var catalogReloadListener: TrackCatalogReloadListener

    private var observedLevel: ClientLevel? = null
    private var observedDimension: ResourceKey<Level>? = null
    private var cachedContext: PlayerContext? = null
    private var ticksSinceSample = 0

    @Volatile
    private var forceResample = true

    override fun onInitializeClient() {
        val client = Minecraft.getInstance()
        configStore = ConfigStore(
            FabricLoader.getInstance().configDir.resolve("biometunes.json"),
        ) { message, error -> logger.warn(message, error) }
        currentConfig = configStore.load()

        val reloadableCatalog = ReloadableTrackCatalog()
        audioAdapter = MinecraftAudioAdapter(client)
        director = MusicDirector(TrackResolver(), PlaybackController(audioAdapter))
        sampler = MinecraftContextSampler(client)
        catalogReloadListener = TrackCatalogReloadListener(reloadableCatalog) {
            audioAdapter.clearUnavailableSounds()
            director.stop()
            forceResample = true
        }

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
            .registerReloadListener(catalogReloadListener)
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> director.stop() }
        ClientLifecycleEvents.CLIENT_STOPPING.register { director.stop() }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("biometunes").executes {
                    Minecraft.getInstance().execute {
                        val minecraft = Minecraft.getInstance()
                        openConfigScreen(minecraft.gui.screen())
                    }
                    1
                },
            )
        }

        logger.info("BiomeTunes client initialized")
    }

    fun createConfigScreen(parent: Screen?): Screen =
        BiomeTunesConfigScreen(parent, currentConfig) { saved ->
            currentConfig = saved.normalized()
            configStore.save(currentConfig)
        }

    fun openConfigScreen(parent: Screen?) {
        Minecraft.getInstance().gui.setScreen(createConfigScreen(parent))
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        val dimension = level?.dimension()
        val identityChanged = level !== observedLevel || dimension !== observedDimension

        if (identityChanged) {
            director.stop()
            observedLevel = level
            observedDimension = dimension
            cachedContext = null
            ticksSinceSample = 0
            forceResample = true
        }

        if (level == null || client.player == null) {
            director.stop()
            cachedContext = null
            ticksSinceSample = 0
            forceResample = true
            return
        }

        if (forceResample) {
            sampleContext()
        } else {
            ticksSinceSample++
            if (ticksSinceSample >= CONTEXT_SAMPLE_INTERVAL_TICKS) sampleContext()
        }

        val catalog = catalogReloadListener.current
        if (catalog == null) {
            director.stop()
            return
        }

        director.tick(cachedContext, catalog, currentConfig)?.let { notice ->
            client.player?.sendOverlayMessage(
                Component.translatable(notice.translationKey, notice.argument),
            )
        }
    }

    private fun sampleContext() {
        cachedContext = sampler.sample()
        ticksSinceSample = 0
        forceResample = false
    }

    private const val CONTEXT_SAMPLE_INTERVAL_TICKS = 10
}
