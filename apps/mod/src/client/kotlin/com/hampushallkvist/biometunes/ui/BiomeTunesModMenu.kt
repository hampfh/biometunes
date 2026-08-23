package com.hampushallkvist.biometunes.ui

import com.hampushallkvist.biometunes.client.BiomeTunesClient
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

class BiomeTunesModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory<Screen> { parent ->
        BiomeTunesClient.createConfigScreen(parent)
    }
}
