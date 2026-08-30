package com.hampushallkvist.biometunes.ui

import com.hampushallkvist.biometunes.client.AudioTreatmentDiagnostics
import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.environment.EnvironmentalClassificationResult
import com.hampushallkvist.biometunes.selection.PlayerContext
import java.util.Locale
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

object EnvironmentalDebugLines {
    fun format(
        context: PlayerContext,
        audio: AudioTreatmentDiagnostics,
    ): List<String> = when (val result = context.environmentalClassification) {
        is EnvironmentalClassificationResult.NotClassified -> listOf(
            "Environmental Classification",
            "Dimension: ${context.dimensionId}",
            "Status: NotClassified | Reason: ${result.reason.name}",
        )
        is EnvironmentalClassificationResult.Classified -> {
            val diagnostics = result.diagnostics
            val evidence = diagnostics.evidence
            listOf(
                "Environmental Classification",
                "Dimension: ${context.dimensionId} | Profile: ${diagnostics.profileDimensionId}",
                "Status: Classified | Mode: ${result.mode.name} | Basis: ${diagnostics.decisionBasis.name}",
                "Constraints: Outside=${diagnostics.constraints.outsideAllowed} | " +
                    "Subterranean=${diagnostics.constraints.subterraneanAllowed}",
                format(
                    "Raw: exposure=%.3f depth=%.3f",
                    diagnostics.rawScores.exposure,
                    diagnostics.rawScores.depth,
                ),
                format(
                    "Smoothed: exposure=%.3f depth=%.3f",
                    diagnostics.smoothedScores.exposure,
                    diagnostics.smoothedScores.depth,
                ),
                format(
                    "Sky exposed: %d/%d | Median depth: %.1f | Enclosure: %.3f",
                    evidence.skyExposedSamples,
                    evidence.skySampleCount,
                    evidence.medianSurfaceDepth,
                    evidence.enclosure,
                ),
                format(
                    "Light: sky=%d block=%d | Shelter: %.3f",
                    evidence.skyLight,
                    evidence.blockLight,
                    result.shelterIntensity,
                ),
                format(
                    "Treatment: gain=%.3f high-frequency=%.3f | %s | Voices=%d",
                    audio.effectiveGainMultiplier,
                    audio.requestedHighFrequencyGain,
                    audio.status.name,
                    audio.attachedVoiceCount,
                ),
            )
        }
    }

    private fun format(template: String, vararg values: Any): String =
        String.format(Locale.ROOT, template, *values)
}

class EnvironmentalDebugHud(
    private val config: () -> BiomeTunesConfig,
    private val context: () -> PlayerContext?,
    private val audio: () -> AudioTreatmentDiagnostics,
) : HudElement {
    override fun extractRenderState(graphics: GuiGraphicsExtractor, tickCounter: DeltaTracker) {
        if (!config().environmentalDebugHud) return
        val client = Minecraft.getInstance()
        if (client.level == null || client.player == null) return
        val lines = context()?.let { EnvironmentalDebugLines.format(it, audio()) } ?: return
        val width = lines.maxOf(client.font::width)
        graphics.fill(
            X - PADDING,
            Y - PADDING,
            X + width + PADDING,
            Y + lines.size * LINE_HEIGHT + PADDING,
            BACKGROUND_COLOR,
        )
        lines.forEachIndexed { index, line ->
            graphics.text(client.font, line, X, Y + index * LINE_HEIGHT, TEXT_COLOR, false)
        }
    }

    private companion object {
        const val X = 6
        const val Y = 6
        const val PADDING = 2
        const val LINE_HEIGHT = 9
        const val BACKGROUND_COLOR = 0x90000000.toInt()
        const val TEXT_COLOR = 0xFFFFFF
    }
}
