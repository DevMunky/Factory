package dev.munky.libtech.system

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.PlayerUtil
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.component.WiredEnergyComponent
import dev.munky.libtech.component.WiredValueComponent
import java.util.concurrent.TimeUnit

class WireSystems<C>(
    val componentType: ComponentType<ChunkStore, C>
) where C : WiredValueComponent<C, *> {
    inner class MoveWiredValues : DelayedEntitySystem<ChunkStore>(1f) {

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
            val blockX = ChunkUtil.xFromIndex(blockIndex)
            val blockY = ChunkUtil.yFromIndex(blockIndex)
            val blockZ = ChunkUtil.zFromIndex(blockIndex)

            val thisComponent = store.getComponent(blockRef, componentType) ?: return

            val thisValue = thisComponent.value
            val positions = thisComponent.connections

            @Suppress("UNCHECKED_CAST")
            val connections = positions
                .associateWith { it.getBlockRef(store) }
                .filter { (_, v) -> v != null }
                    as Map<Vector3i, Ref<ChunkStore>>

            if (connections.isEmpty()) {
                LOGGER.atInfo().atMostEvery(1, TimeUnit.MINUTES).log("$blockRef No connections to move from $blockX $blockY $blockZ")
                return
            }

            val connectionValue = connections.values.fold(.0) { acc, connection ->
                acc + (store.getComponent(connection, WiredEnergyComponent.getComponentType())?.value ?: .0)
            }
            val totalValue = thisValue + connectionValue
            val distributed = totalValue / connections.size
            var valueToMove = thisValue - distributed

            if (valueToMove <= 0) {
                LOGGER.atInfo().atMostEvery(1, TimeUnit.MINUTES).log("$blockRef Not enough energy to move from $blockX $blockY $blockZ")
                return
            }

            var moved = .0
            for ((position, conRef) in connections) {
                val conEnergy = store.getComponent(conRef, WiredEnergyComponent.getComponentType()) ?: continue
                val oldValue = conEnergy.value
                val newValue = conEnergy.value + (valueToMove / connections.size)
                val clampedValue = newValue.coerceAtMost(conEnergy.capacity)

                valueToMove += newValue - clampedValue
                conEnergy.value = clampedValue
                moved += clampedValue - oldValue

                shootLaser(store, blockX, blockY, blockZ, position, 500)
                LOGGER.atInfo().atMostEvery(1, TimeUnit.MINUTES).log("$blockRef Moved $moved from $blockX $blockY $blockZ to $position")
            }
            thisComponent.value -= moved
        }

        fun shootLaser(store: Store<ChunkStore>, startX: Int, startY: Int, startZ:Int, end: Vector3i, durationMs: Int) {
            val laserPacket = BuilderToolLaserPointer()
            laserPacket.playerNetworkId = 0
            laserPacket.startX = startX.toFloat()
            laserPacket.startY = startY.toFloat()
            laserPacket.startZ = startZ.toFloat()
            laserPacket.endX = end.x.toFloat()
            laserPacket.endY = end.y.toFloat()
            laserPacket.endZ = end.z.toFloat()
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
            println("new wire")
        }

        override fun onEntityRemove(
            blockRef: Ref<ChunkStore>,
            reason: RemoveReason,
            store: Store<ChunkStore>,
            commandBuffer: CommandBuffer<ChunkStore>
        ) {

        }
    }

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
    }
}