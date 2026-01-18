package dev.munky.factory.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.factory.block.component.MachineComponent
import dev.munky.libtech.component.WiredEnergyComponent

class MachineSystem : EntityTickingSystem<ChunkStore>() {
    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<ChunkStore>,
        store: Store<ChunkStore>,
        commandBuffer: CommandBuffer<ChunkStore>
    ) {
        val block = archetypeChunk.getReferenceTo(index)
        val machine = store.getComponent(block, MachineComponent.getComponentType()) ?: return
        val energy = store.getComponent(block, WiredEnergyComponent.getComponentType()) ?: return
        machine.number = index
        if (energy.value >= 0 && index <= 1) {
            energy.value = 100.0
        }
    }

    override fun getQuery(): Query<ChunkStore> = Query.and(MachineComponent.getComponentType(), WiredEnergyComponent.getComponentType())

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
    }
}