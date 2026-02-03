package dev.munky.factory

import com.hypixel.hytale.builtin.instances.InstancesPlugin
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.asset.type.gameplay.respawn.WorldSpawnPoint
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.io.ServerManager
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.munky.factory.block.component.MachineComponent
import dev.munky.factory.block.interaction.MachineInteraction
import dev.munky.factory.system.MachineSystem
import dev.munky.factory.tool.interaction.WireToolInteraction
import java.util.UUID

class FactoryMod(init: JavaPluginInit) : JavaPlugin(init) {
    var dungeonInstanceWorld: World? = null

    lateinit var dungeonPortalComponentType: ComponentType<ChunkStore, DungeonPortal>
        private set

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
        entityStoreRegistry.shutdown()
        eventRegistry.shutdown()
        logger.atInfo().log("Factory shut down.")
    }

    fun registerEvents() {
        eventRegistry.register(PlayerConnectEvent::class.java) {
            val addr = ServerManager.get().nonLoopbackAddress ?: ServerManager.get().publicAddress ?: ServerManager.get().localOrPublicAddress
            logger.atInfo().log("Registering events for $addr")
            if (addr != null && addr.port == 5521) {
                if (dungeonInstanceWorld == null) {
                    dungeonInstanceWorld = InstanceUtil.spawnInstance("DungeonInstance", null).join()
                }
                it.world = dungeonInstanceWorld
            }
        }

    }

    fun registerInteractions() {
        logger.atInfo().log("interactions registering.")
        getCodecRegistry(Interaction.CODEC).register("Machine", MachineInteraction::class.java, MachineInteraction.CODEC)
        getCodecRegistry(Interaction.CODEC).register("WireTool", WireToolInteraction::class.java, WireToolInteraction.CODEC)
        getCodecRegistry(Interaction.CODEC).register("SummonDungeonPortal", DungeonPortalSummonInteraction::class.java, DungeonPortalSummonInteraction.CODEC)
        getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("DungeonPortal", DungeonPortalPageSupplier::class.java, DungeonPortalPageSupplier.CODEC)
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
        dungeonPortalComponentType = chunkStoreRegistry.registerComponent(
            DungeonPortal::class.java,
            "DungeonPortal",
            DungeonPortal.CODEC,
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