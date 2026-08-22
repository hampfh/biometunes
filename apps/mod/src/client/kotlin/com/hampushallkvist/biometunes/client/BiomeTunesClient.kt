package com.hampushallkvist.biometunes.client

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object BiomeTunesClient : ClientModInitializer {
    internal val logger = LoggerFactory.getLogger("BiomeTunes")

    override fun onInitializeClient() {
        logger.info("BiomeTunes client initialized")
    }
}
