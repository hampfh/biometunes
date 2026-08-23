package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.playback.PlaybackController
import com.hampushallkvist.biometunes.selection.PlayerContext
import com.hampushallkvist.biometunes.selection.ResolvedTrack
import com.hampushallkvist.biometunes.selection.SelectionSource
import com.hampushallkvist.biometunes.selection.TrackResolver

data class PlayerNotice(val translationKey: String, val argument: String)

class MusicDirector(
    private val resolver: TrackResolver,
    private val playback: PlaybackController,
) {
    private var cachedResolution: CachedResolution? = null
    private var lastNotificationState: NotificationState? = null

    fun tick(
        context: PlayerContext?,
        catalog: TrackCatalog,
        config: BiomeTunesConfig,
    ): PlayerNotice? {
        if (context == null || !config.enabled) {
            stop()
            return null
        }

        val resolved = resolve(context, catalog, config.bossMusic)
        playback.update(resolved.track, config.playbackOptions)

        val shouldNotify = when (resolved.source) {
            SelectionSource.BOSS -> config.bossNotifications
            else -> config.biomeNotifications
        }
        val notificationState = NotificationState(resolved.notificationKey, shouldNotify)
        if (notificationState == lastNotificationState) return null
        lastNotificationState = notificationState
        if (!shouldNotify) return null

        val translationKey = when (resolved.source) {
            SelectionSource.BOSS -> "message.biometunes.boss"
            else -> "message.biometunes.biome"
        }
        return PlayerNotice(translationKey, resolved.track.title)
    }

    fun stop() {
        playback.stop()
        cachedResolution = null
        lastNotificationState = null
    }

    private fun resolve(
        context: PlayerContext,
        catalog: TrackCatalog,
        bossMusicEnabled: Boolean,
    ): ResolvedTrack {
        cachedResolution?.let { cached ->
            if (
                cached.context == context &&
                cached.catalog === catalog &&
                cached.bossMusicEnabled == bossMusicEnabled
            ) {
                return cached.resolved
            }
        }
        return resolver.resolve(context, catalog, bossMusicEnabled).also { resolved ->
            cachedResolution = CachedResolution(context, catalog, bossMusicEnabled, resolved)
        }
    }
}

private data class NotificationState(
    val key: String,
    val enabled: Boolean,
)

private data class CachedResolution(
    val context: PlayerContext,
    val catalog: TrackCatalog,
    val bossMusicEnabled: Boolean,
    val resolved: ResolvedTrack,
)
