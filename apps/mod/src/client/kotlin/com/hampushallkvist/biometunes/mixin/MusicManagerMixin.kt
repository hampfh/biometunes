package com.hampushallkvist.biometunes.mixin

import com.hampushallkvist.biometunes.client.VanillaMusicGate
import net.minecraft.client.sounds.MusicManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MusicManager::class)
abstract class MusicManagerMixin {
    @Inject(method = ["tick"], at = [At("HEAD")], cancellable = true)
    private fun skipVanillaMusic(callback: CallbackInfo) {
        if (VanillaMusicGate.suppressed) callback.cancel()
    }
}
