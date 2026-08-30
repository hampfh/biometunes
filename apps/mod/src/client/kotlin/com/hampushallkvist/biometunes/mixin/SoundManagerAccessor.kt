package com.hampushallkvist.biometunes.mixin

import net.minecraft.client.sounds.SoundEngine
import net.minecraft.client.sounds.SoundManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SoundManager::class)
interface SoundManagerAccessor {
    @Accessor("soundEngine")
    fun biometunesSoundEngine(): SoundEngine
}
