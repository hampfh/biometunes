package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.ReloadableTrackCatalog
import com.hampushallkvist.biometunes.catalog.TrackCatalog
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager

class TrackCatalogReloadListener(
    private val state: ReloadableTrackCatalog,
    private val onAccepted: () -> Unit,
) : SimpleSynchronousResourceReloadListener {
    val current: TrackCatalog?
        get() = state.current

    override fun getFabricId(): Identifier =
        Identifier.fromNamespaceAndPath("biometunes", "tracks")

    override fun onResourceManagerReload(manager: ResourceManager) {
        val id = Identifier.fromNamespaceAndPath("biometunes", "biometunes/tracks.json")
        val loaded: Result<String> = runCatching {
            manager.getResourceOrThrow(id).openAsReader().use { it.readText() }
        }
        val parsed = loaded.fold(
            onSuccess = state::reload,
            onFailure = { Result.failure(it) },
        )
        parsed.onSuccess {
            onAccepted()
        }.onFailure { error ->
            BiomeTunesClient.logger.error(
                "Rejected BiomeTunes catalog {}; keeping prior catalog",
                id,
                error,
            )
        }
    }
}
