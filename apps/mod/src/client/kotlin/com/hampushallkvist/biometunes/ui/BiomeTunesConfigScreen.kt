package com.hampushallkvist.biometunes.ui

import com.hampushallkvist.biometunes.config.BiomeTunesConfig
import com.hampushallkvist.biometunes.config.ConfigSliderValues
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

class BiomeTunesConfigScreen(
    private val parent: Screen?,
    config: BiomeTunesConfig,
    private val onSave: (BiomeTunesConfig) -> Unit,
) : Screen(Component.translatable("screen.biometunes.title")) {
    private val layout = HeaderAndFooterLayout(this)
    private var workingConfig = config.copy()

    override fun init() {
        layout.removeChildren()
        layout.addTitleHeader(title, font)

        val controls = GridLayout().columnSpacing(COLUMN_SPACING).rowSpacing(ROW_SPACING)
        controls.addChild(enabledButton(), 0, 0)
        controls.addChild(VolumeSlider(), 0, 1)
        controls.addChild(CrossfadeSlider(), 1, 0)
        controls.addChild(biomeNotificationsButton(), 1, 1)
        controls.addChild(bossMusicButton(), 2, 0)
        controls.addChild(bossNotificationsButton(), 2, 1)
        controls.addChild(environmentalDebugHudButton(), 3, 0)
        layout.addToContents(controls)

        val actions = LinearLayout.horizontal().spacing(COLUMN_SPACING)
        actions.addChild(
            Button.builder(Component.translatable("screen.biometunes.save")) {
                onSave(workingConfig)
                onClose()
            }.width(CONTROL_WIDTH).build(),
        )
        actions.addChild(
            Button.builder(Component.translatable("screen.biometunes.cancel")) { onClose() }
                .width(CONTROL_WIDTH)
                .build(),
        )
        layout.addToFooter(actions)

        layout.visitWidgets(::addRenderableWidget)
        repositionElements()
    }

    override fun repositionElements() {
        layout.arrangeElements()
    }

    override fun onClose() {
        minecraft.gui.setScreen(parent)
    }

    private fun enabledButton() = CycleButton.onOffBuilder(workingConfig.enabled).create(
        Component.translatable("options.biometunes.enabled"),
    ) { _, enabled -> workingConfig = workingConfig.copy(enabled = enabled) }

    private fun biomeNotificationsButton() =
        CycleButton.onOffBuilder(workingConfig.biomeNotifications).create(
            Component.translatable("options.biometunes.biome_notifications"),
        ) { _, enabled -> workingConfig = workingConfig.copy(biomeNotifications = enabled) }

    private fun bossMusicButton() = CycleButton.onOffBuilder(workingConfig.bossMusic).create(
        Component.translatable("options.biometunes.boss_music"),
    ) { _, enabled -> workingConfig = workingConfig.copy(bossMusic = enabled) }

    private fun bossNotificationsButton() =
        CycleButton.onOffBuilder(workingConfig.bossNotifications).create(
            Component.translatable("options.biometunes.boss_notifications"),
        ) { _, enabled -> workingConfig = workingConfig.copy(bossNotifications = enabled) }

    private fun environmentalDebugHudButton() =
        CycleButton.onOffBuilder(workingConfig.environmentalDebugHud).create(
            Component.translatable("options.biometunes.environmental_debug_hud"),
        ) { _, enabled -> workingConfig = workingConfig.copy(environmentalDebugHud = enabled) }

    private abstract inner class SteppedSlider(
        initialValue: Double,
        private val steps: Int,
    ) : AbstractSliderButton(
        0, 0, CONTROL_WIDTH, CONTROL_HEIGHT, Component.empty(), initialValue,
    ) {
        override fun keyPressed(event: KeyEvent): Boolean {
            if (event.isSelection) {
                canChangeValue = !canChangeValue
                return true
            }
            if (!canChangeValue) return false

            val direction = when {
                event.isLeft -> -1
                event.isRight -> 1
                else -> return false
            }
            setValue(value + direction.toDouble() / steps)
            return true
        }
    }

    private inner class VolumeSlider : SteppedSlider(
        workingConfig.volume.toDouble(),
        VOLUME_STEPS,
    ) {
        init {
            updateMessage()
        }

        override fun updateMessage() {
            message = Component.translatable(
                "options.biometunes.volume",
                (workingConfig.volume * 100f).roundToInt(),
            )
        }

        override fun applyValue() {
            val volume = ConfigSliderValues.volume(value)
            workingConfig = workingConfig.copy(volume = volume)
            value = volume.toDouble()
        }
    }

    private inner class CrossfadeSlider : SteppedSlider(
        workingConfig.crossfadeSeconds.toDouble() / MAX_CROSSFADE_SECONDS,
        CROSSFADE_STEPS,
    ) {
        init {
            updateMessage()
        }

        override fun updateMessage() {
            message = Component.translatable(
                "options.biometunes.crossfade",
                workingConfig.crossfadeSeconds,
            )
        }

        override fun applyValue() {
            val crossfadeSeconds = ConfigSliderValues.crossfadeSeconds(value)
            workingConfig = workingConfig.copy(crossfadeSeconds = crossfadeSeconds)
            value = crossfadeSeconds.toDouble() / MAX_CROSSFADE_SECONDS
        }
    }

    private companion object {
        const val CONTROL_WIDTH = 150
        const val CONTROL_HEIGHT = 20
        const val COLUMN_SPACING = 8
        const val ROW_SPACING = 4
        const val MAX_CROSSFADE_SECONDS = 15.0
        const val VOLUME_STEPS = 100
        const val CROSSFADE_STEPS = 30
    }
}
