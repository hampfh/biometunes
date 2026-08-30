package com.hampushallkvist.biometunes.client

import com.hampushallkvist.biometunes.catalog.TrackCatalog
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult
import com.hampushallkvist.biometunes.playback.AudioTreatment
import com.hampushallkvist.biometunes.playback.PlaybackController
import com.hampushallkvist.biometunes.playback.PlaybackOptions
import com.hampushallkvist.biometunes.playback.PlaybackUpdateResult
import com.hampushallkvist.biometunes.playback.ShelteredTreatmentEnvelope
import com.hampushallkvist.biometunes.selection.PlayerContext
import com.hampushallkvist.biometunes.selection.ResolvedTrack
import com.hampushallkvist.biometunes.selection.SelectedSilence
import com.hampushallkvist.biometunes.selection.SelectedTrack
import com.hampushallkvist.biometunes.selection.SelectionSource
import com.hampushallkvist.biometunes.selection.TrackResolver

data class PlayerNotice(val translationKey: String, val argument: String)

class MusicDirector(
    private val resolver: TrackResolver,
    private val playback: PlaybackController,
    private val treatmentEnvelope: ShelteredTreatmentEnvelope = ShelteredTreatmentEnvelope(),
) {
    private var cachedResolution: CachedResolution? = null
    private var lastNotificationState: NotificationState? = null
    private var silenceTicksRemaining = 0L

    fun tick(
        context: PlayerContext?,
        catalog: TrackCatalog,
        config: BiomeTunesConfig,
        paused: Boolean = false,
    ): PlayerNotice? {
        if (context == null || !config.enabled) {
            stop()
            return null
        }

        var resolved = resolve(context, catalog, config.bossMusic)
        if (resolved.selection is SelectedSilence && silenceTicksRemaining <= 0L) {
            cachedResolution = null
            resolved = resolve(context, catalog, config.bossMusic)
        }
        val playbackOptions = config.playbackOptions.copy(
            treatment = treatment(context, catalog, paused),
        )
        var playbackResult = updatePlayback(resolved, playbackOptions, paused)
        if (playbackResult == PlaybackUpdateResult.TRACK_ENDED) {
            cachedResolution = null
            resolved = resolve(context, catalog, config.bossMusic)
            playbackResult = updatePlayback(resolved, playbackOptions, paused)
        }
        if (
            resolved.selection is SelectedSilence &&
            playbackResult == PlaybackUpdateResult.SILENT &&
            !paused &&
            silenceTicksRemaining > 0L
        ) {
            silenceTicksRemaining--
        }

        val selectedTrack = (resolved.selection as? SelectedTrack)?.track ?: run {
            lastNotificationState = null
            return null
        }

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
        return PlayerNotice(translationKey, selectedTrack.title)
    }

    fun stop() {
        playback.stop()
        cachedResolution = null
        lastNotificationState = null
        silenceTicksRemaining = 0L
        treatmentEnvelope.reset()
    }

    private fun updatePlayback(
        resolved: ResolvedTrack,
        options: PlaybackOptions,
        paused: Boolean,
    ): PlaybackUpdateResult = when (val selection = resolved.selection) {
        is SelectedTrack -> playback.update(
            selection.track,
            options,
            advanceFade = !paused,
            restartEnded = false,
        )
        is SelectedSilence -> playback.updateSilence(
            options,
            advanceFade = !paused,
        )
    }

    private fun treatment(
        context: PlayerContext,
        catalog: TrackCatalog,
        paused: Boolean,
    ): AudioTreatment {
        val profile = catalog.environmentalProfiles[context.dimensionId] ?: run {
            treatmentEnvelope.reset()
            return AudioTreatment.NONE
        }
        val targetIntensity =
            (context.environmentalClassification as? EnvironmentalClassificationResult.Classified)
                ?.shelterIntensity
                ?: 0.0f
        return treatmentEnvelope.update(
            targetIntensity = targetIntensity,
            settings = profile.shelteredAudio,
            smoothingSeconds = profile.smoothingSeconds,
            advance = !paused,
        )
    }

    private fun resolve(
        context: PlayerContext,
        catalog: TrackCatalog,
        bossMusicEnabled: Boolean,
    ): ResolvedTrack {
        val resolvedPool = resolver.resolvePool(context, catalog, bossMusicEnabled)
        cachedResolution?.let { cached ->
            if (
                cached.catalog === catalog &&
                cached.poolKey == resolvedPool.key
            ) {
                return cached.resolved
            }
        }
        return resolver.select(resolvedPool, catalog).also { resolved ->
            cachedResolution = CachedResolution(catalog, resolvedPool.key, resolved)
            silenceTicksRemaining = when (val selection = resolved.selection) {
                is SelectedSilence -> selection.durationSeconds.toLong() * TICKS_PER_SECOND
                is SelectedTrack -> 0L
            }
        }
    }

    private companion object {
        const val TICKS_PER_SECOND = 20L
    }
}

private data class NotificationState(
    val key: String,
    val enabled: Boolean,
)

private data class CachedResolution(
    val catalog: TrackCatalog,
    val poolKey: String,
    val resolved: ResolvedTrack,
)
