package com.hampushallkvist.biometunes.client

object VanillaMusicGate {
    var suppressed: Boolean = false

    fun shouldSuppress(hasLevel: Boolean, hasPlayer: Boolean, isEndCredits: Boolean): Boolean =
        suppressed && GameplayActivity.isActive(hasLevel, hasPlayer, isEndCredits)
}
