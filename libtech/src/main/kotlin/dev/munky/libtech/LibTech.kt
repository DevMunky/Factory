package dev.munky.libtech

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.component.WiredEnergyComponent
import dev.munky.libtech.system.WireSystems

class LibTech(init: JavaPluginInit) : JavaPlugin(init) {
    lateinit var wiredEnergyComponentType: ComponentType<ChunkStore, WiredEnergyComponent>
        private set

    override fun setup() {
        instance = this
        logger.atInfo().log("LibTech loading.")
        registerComponents()
        registerSystems()
        logger.atInfo().log("LibTech loaded.")
    }

    fun registerComponents() {
        logger.atInfo().log("components registering.")
        wiredEnergyComponentType = chunkStoreRegistry.registerComponent(
            WiredEnergyComponent::class.java,
            "WiredEnergy",
            WiredEnergyComponent.CODEC
        )
    }

    fun registerSystems() {
        logger.atInfo().log("systems registering.")
        val wireSystems = WireSystems(wiredEnergyComponentType)
        chunkStoreRegistry.registerSystem(wireSystems.MoveWiredValues())
        chunkStoreRegistry.registerSystem(wireSystems.OnBlockChange())
    }

    override fun shutdown() {
        // chunkStoreRegistry.shutdown()
        // entityStoreRegistry.shutdown()
        // eventRegistry.shutdown()
    }

    companion object {
        private lateinit var instance: LibTech
        fun get(): LibTech = instance
    }
}