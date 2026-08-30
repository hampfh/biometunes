package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.ReloadableTrackCatalog
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.config.ConfigStore
import com.hampushallkvist.biometunes.playback.PlaybackController
import com.hampushallkvist.biometunes.selection.PlayerContext
import com.hampushallkvist.biometunes.selection.TrackResolver
import com.hampushallkvist.biometunes.ui.BiomeTunesConfigScreen
import com.hampushallkvist.biometunes.ui.EnvironmentalDebugHud
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.WinScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import org.slf4j.LoggerFactory

object BiomeTunesClient : ClientModInitializer {
    internal val logger = LoggerFactory.getLogger("BiomeTunes")

    private lateinit var currentConfig: BiomeTunesConfig
    private lateinit var configStore: ConfigStore
    private lateinit var audioAdapter: MinecraftAudioAdapter
    private lateinit var director: MusicDirector
    private lateinit var sampler: MinecraftContextSampler
    private lateinit var catalogReloadListener: TrackCatalogReloadListener

    private val playbackLifecycle = ClientPlaybackLifecycle(CONTEXT_SAMPLE_INTERVAL_TICKS)
    private var cachedContext: PlayerContext? = null

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
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("biometunes", "environmental_debug_hud"),
            EnvironmentalDebugHud(
                config = { currentConfig },
                context = { cachedContext },
                audio = { audioAdapter.treatmentDiagnostics },
            ),
        )
        catalogReloadListener = TrackCatalogReloadListener(
            state = reloadableCatalog,
            onReload = audioAdapter::clearUnavailableSounds,
            onAccepted = {
                director.stop()
                audioAdapter.resetAudioTreatment()
                cachedContext = null
                sampler.resetEnvironmentalClassification()
                playbackLifecycle.forceFreshSample()
            },
        )

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
            .registerReloadListener(catalogReloadListener)
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            director.stop()
            audioAdapter.resetAudioTreatment()
            cachedContext = null
            sampler.resetEnvironmentalClassification()
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            director.stop()
            audioAdapter.resetAudioTreatment()
        }
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
        val lifecycle = playbackLifecycle.tick(
            levelIdentity = level,
            dimensionIdentity = level?.dimension(),
            playerIdentity = client.player,
            isEndCredits = client.gui.screen() is WinScreen,
        )
        if (lifecycle.stopPlayback) {
            director.stop()
            audioAdapter.resetAudioTreatment()
        }
        if (lifecycle.clearContext) {
            cachedContext = null
            sampler.resetEnvironmentalClassification()
        }
        if (!lifecycle.activeGameplay) return

        val catalog = catalogReloadListener.current
        if (catalog == null) {
            director.stop()
            return
        }
        if (lifecycle.sampleContext) cachedContext = sampler.sample(catalog)

        director.tick(cachedContext, catalog, currentConfig, client.isPaused())?.let { notice ->
            client.player?.sendOverlayMessage(
                Component.translatable(notice.translationKey, notice.argument),
            )
        }
    }

    private const val CONTEXT_SAMPLE_INTERVAL_TICKS = 10
}
