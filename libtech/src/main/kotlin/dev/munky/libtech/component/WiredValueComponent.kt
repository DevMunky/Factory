package dev.munky.libtech.component

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.component.WiredValueComponent.Connection

abstract class WiredValueComponent<COMPONENT, CONNECTION> : Component<ChunkStore>
        where COMPONENT : WiredValueComponent<COMPONENT, CONNECTION>, CONNECTION : Connection<COMPONENT, CONNECTION>
{
    open var value: Double = .0

    abstract var capacity: Double
    abstract var connections: MutableSet<CONNECTION>

    interface Connection<COMPONENT, CONNECTION>
            where COMPONENT : WiredValueComponent<COMPONENT, CONNECTION>, CONNECTION : Connection<COMPONENT, CONNECTION>
    {
        var position: Vector3i

        fun getBlockRef(store: Store<ChunkStore>) : Ref<ChunkStore>? {
            val x = position.x
            val y = position.y
            val z = position.z

            val cx = ChunkUtil.chunkCoordinate(x)
            val cy = ChunkUtil.indexSection(y)
            val cz = ChunkUtil.chunkCoordinate(z)

            val sectionRef = store.externalData.getChunkSectionReference(cx, cy, cz) ?: return null
            val blockChunk = sectionRef.store.getComponent(sectionRef, BlockComponentChunk.getComponentType()) ?: return null


            // Look in hytale code for this
            return blockChunk.getEntityReference(ChunkUtil.indexBlock(x, y, z))
        }
    }

}