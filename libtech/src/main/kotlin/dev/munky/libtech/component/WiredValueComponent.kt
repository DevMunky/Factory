package dev.munky.libtech.component

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.modules.block.BlockModule
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.component.WiredEnergyComponent.EnergyConnection
import dev.munky.libtech.component.WiredValueComponent.Connection

abstract class WiredValueComponent<COMPONENT, CONNECTION> : Component<ChunkStore>
        where COMPONENT : WiredValueComponent<COMPONENT, CONNECTION>, CONNECTION : Connection<COMPONENT, CONNECTION>
{
    abstract var value: Double

    abstract var capacity: Double
    protected abstract var connectedBlocks: MutableSet<CONNECTION>
    val connections get() = connectedBlocks.toSet()

    abstract fun addConnection(connection: CONNECTION)
    abstract fun removeConnection(connection: CONNECTION)

    interface Connection<COMPONENT, CONNECTION>
            where COMPONENT : WiredValueComponent<COMPONENT, CONNECTION>, CONNECTION : Connection<COMPONENT, CONNECTION>
    {
        var start: Vector3i
        var end: Vector3i

        fun getStartBlock(store: Store<ChunkStore>) : Ref<ChunkStore>? {
            val x = start.x
            val y = start.y
            val z = start.z

            return BlockModule.getBlockEntity(store.externalData.world, x, y, z)
        }

        fun getEndBlock(store: Store<ChunkStore>) : Ref<ChunkStore>? {
            val x = end.x
            val y = end.y
            val z = end.z

            return BlockModule.getBlockEntity(store.externalData.world, x, y, z)
        }
    }

}