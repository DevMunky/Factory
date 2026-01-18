package dev.munky.libtech.system

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.PlayerUtil
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.component.WiredEnergyComponent
import dev.munky.libtech.component.WiredValueComponent
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class WireSystems<C, CONN>(
    val componentType: ComponentType<ChunkStore, C>
) where C : WiredValueComponent<C, CONN>, CONN: WiredValueComponent.Connection<C, CONN> {
    inner class MoveWiredValues : EntityTickingSystem<ChunkStore>() {

        override fun getQuery(): Query<ChunkStore> = componentType

        override fun tick(
            dt: Float,
            index: Int,
            archetypeChunk: ArchetypeChunk<ChunkStore>,
            store: Store<ChunkStore>,
            commandBuffer: CommandBuffer<ChunkStore>
        ) {
            val blockRef = archetypeChunk.getReferenceTo(index)
            val block = store.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType()) ?: return
            val blockIndex = block.index
            val worldChunk = block.chunkRef.store.getComponent(block.chunkRef, WorldChunk.getComponentType()) ?: return

            val blockX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.x, ChunkUtil.xFromIndex(blockIndex))
            val blockY = ChunkUtil.yFromBlockInColumn(blockIndex)
            val blockZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.z, ChunkUtil.zFromIndex(blockIndex))

            val thisEnergy = store.getComponent(blockRef, componentType) ?: return

            val thisValue = thisEnergy.value
            val positions = thisEnergy.connections

            @Suppress("UNCHECKED_CAST")
            val connections = positions
                .associateWith { it.getStartBlock(store) }
                .filter { (connection, v) ->
                    if (v == null) {
                        thisEnergy.removeConnection(connection)
                        false
                    } else true
                } as Map<WiredEnergyComponent.EnergyConnection, Ref<ChunkStore>>

            if (connections.isEmpty()) {
                return
            }

            val connectionValue = connections.values.fold(.0) { acc, ref ->
                acc + (store.getComponent(ref, WiredEnergyComponent.getComponentType())?.value ?: .0)
            }
            val networkValue = thisValue + connectionValue
            val networkEquilibrium = (networkValue / (connections.size + 1))

            var excessValue = thisValue - networkEquilibrium
            if (excessValue <= .1) excessValue = .0
            if (excessValue <= 0) {
                return
            }

            var moved = .0
            for ((route, conRef) in connections) {
                val conEnergy = store.getComponent(conRef, WiredEnergyComponent.getComponentType()) ?: continue

                val oldValue = conEnergy.value
                val newValue = conEnergy.value + (excessValue / connections.size)
                val clampedValue = newValue.coerceAtMost(conEnergy.capacity)

                excessValue += newValue - clampedValue
                conEnergy.value = clampedValue
                moved += clampedValue - oldValue

                shootLaser(store, route.start, route.end, 500)
            }

            commandBuffer.run {
                thisEnergy.value = thisValue - moved
            }
        }

        fun shootLaser(store: Store<ChunkStore>, start: Vector3i, end: Vector3i, durationMs: Int) {
            val laserPacket = BuilderToolLaserPointer()
            laserPacket.playerNetworkId = 0
            laserPacket.startX = start.x.toFloat() + .5f
            laserPacket.startY = start.y.toFloat() + 1f
            laserPacket.startZ = start.z.toFloat() + .5f
            laserPacket.endX = end.x.toFloat() + .5f
            laserPacket.endY = end.y.toFloat() + 1f
            laserPacket.endZ = end.z.toFloat() + .5f
            laserPacket.color = ColorParseUtil.hexStringToRGBInt("#ff0000")
            laserPacket.durationMs = durationMs

            PlayerUtil.broadcastPacketToPlayers(store.externalData.world.entityStore.store, laserPacket)
        }
    }

    inner class OnBlockChange : RefSystem<ChunkStore>() {

        override fun getQuery(): Query<ChunkStore> = componentType

        override fun onEntityAdded(
            blockRef: Ref<ChunkStore>,
            reason: AddReason,
            store: Store<ChunkStore>,
            commandBuffer: CommandBuffer<ChunkStore>
        ) {
            val energyComponent = store.getComponent(blockRef, WiredEnergyComponent.getComponentType()) ?: return
            energyComponent.connections.forEach { connection ->
                val connectionBlockRef = connection.getStartBlock(store)
                val connectionEnergyRef = connectionBlockRef?.let { store.getComponent(it, WiredEnergyComponent.getComponentType()) }
                if (connectionEnergyRef == null) commandBuffer.run {
                    energyComponent.removeConnection(connection)
                    println("cleaned up stale wire connection at $connection.")
                }
            }
        }

        override fun onEntityRemove(
            blockRef: Ref<ChunkStore>,
            reason: RemoveReason,
            store: Store<ChunkStore>,
            commandBuffer: CommandBuffer<ChunkStore>
        ) {
            val energyComponent = store.getComponent(blockRef, WiredEnergyComponent.getComponentType()) ?: return

            for (connection in energyComponent.connections) {
                val startRef = connection.getStartBlock(store)
                val endRef = connection.getEndBlock(store)
                if (startRef != null) {
                    val connectionEnergyRef = store.getComponent(startRef, WiredEnergyComponent.getComponentType()) ?: continue
                    commandBuffer.run {
                        println("fixed ${connection.start} : ${connectionEnergyRef.connections}")
                        connectionEnergyRef.removeConnection(connection)
                    }
                }
                if (endRef != null) {
                    val connectionEnergyRef = store.getComponent(endRef, WiredEnergyComponent.getComponentType()) ?: continue
                    commandBuffer.run {
                        println("fixed ${connection.end} : ${connectionEnergyRef.connections}")
                        connectionEnergyRef.removeConnection(connection)
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
    }
}