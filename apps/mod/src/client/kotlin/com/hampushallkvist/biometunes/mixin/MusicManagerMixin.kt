package com.hampushallkvist.biometunes.mixin

import com.hampushallkvist.biometunes.client.VanillaMusicGate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.WinScreen
import net.minecraft.client.sounds.MusicManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MusicManager::class)
abstract class MusicManagerMixin {
    @Inject(method = ["tick"], at = [At("HEAD")], cancellable = true)
    private fun skipVanillaMusic(callback: CallbackInfo) {
        val client = Minecraft.getInstance()
        if (
            VanillaMusicGate.shouldSuppress(
                hasLevel = client.level != null,
                hasPlayer = client.player != null,
                isEndCredits = client.gui.screen() is WinScreen,
            )
        ) {
            callback.cancel()
        }
    }
}
