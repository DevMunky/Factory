package dev.munky.factory

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.role.support.EntitySupport
import dev.munky.factory.block.interaction.MachineInteraction
import dev.munky.factory.block.component.MachineComponent
import dev.munky.factory.system.MachineSystem
import dev.munky.factory.tool.interaction.WireToolInteraction

class FactoryMod(init: JavaPluginInit) : JavaPlugin(init) {

    lateinit var machineComponentType: ComponentType<ChunkStore, MachineComponent>
        private set

    lateinit var wireToolInteractionDataComponentType: ComponentType<EntityStore, WireToolInteraction.WireToolDataComponent>
        private set

    override fun setup() {
        instance = this
        logger.atInfo().log("factory loading.")
        registerComponents()
        registerSystems()
        registerInteractions()
        registerEvents()
        logger.atInfo().log("factory loaded.")
    }

    override fun shutdown() {
        logger.atInfo().log("Factory shutting down.")
        chunkStoreRegistry.shutdown()
        logger.atInfo().log("Factory shut down.")
    }

    fun registerEvents() {

    }

    fun registerInteractions() {
        logger.atInfo().log("interactions registering.")
        getCodecRegistry(Interaction.CODEC).register("Machine", MachineInteraction::class.java, MachineInteraction.CODEC)
        getCodecRegistry(Interaction.CODEC).register("WireTool", WireToolInteraction::class.java, WireToolInteraction.CODEC)
    }

    fun registerSystems() {
        logger.atInfo().log("systems registering.")
        chunkStoreRegistry.registerSystem(MachineSystem())
    }

    fun registerComponents() {
        logger.atInfo().log("components registering.")
        machineComponentType = chunkStoreRegistry.registerComponent(
            MachineComponent::class.java,
            "Machine",
            MachineComponent.CODEC
        )
        wireToolInteractionDataComponentType = entityStoreRegistry.registerComponent(
            WireToolInteraction.WireToolDataComponent::class.java
        ) { throw UnsupportedOperationException() }
    }

    companion object {
        private lateinit var instance: FactoryMod

        fun get(): FactoryMod = instance
    }
}