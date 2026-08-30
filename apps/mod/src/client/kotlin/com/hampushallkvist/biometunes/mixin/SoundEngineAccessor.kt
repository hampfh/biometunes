package com.hampushallkvist.biometunes.mixin

import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.ChannelAccess
import net.minecraft.client.sounds.SoundEngine
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SoundEngine::class)
interface SoundEngineAccessor {
    @Accessor("instanceToChannel")
    fun biometunesInstanceToChannel(): Map<SoundInstance, ChannelAccess.ChannelHandle>

    @Accessor("channelAccess")
    fun biometunesChannelAccess(): ChannelAccess
}
