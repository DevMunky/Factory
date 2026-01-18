package dev.munky.kotlin_plugin

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit

class KotlinPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    override fun setup() {
        logger.atInfo().log("Kotlin is present.")
    }
}