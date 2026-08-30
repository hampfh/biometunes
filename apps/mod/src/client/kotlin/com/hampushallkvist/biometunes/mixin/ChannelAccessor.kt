package com.hampushallkvist.biometunes.mixin

import com.mojang.blaze3d.audio.Channel
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(Channel::class)
interface ChannelAccessor {
    @Accessor("source")
    fun biometunesSource(): Int
}
