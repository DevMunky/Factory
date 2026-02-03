package dev.munky.libtech.component

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.set.SetCodec
import com.hypixel.hytale.codec.store.CodecKey
import com.hypixel.hytale.codec.store.CodecStore
import com.hypixel.hytale.codec.store.StoredCodec
import com.hypixel.hytale.codec.validation.Validators
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.LibTech
import dev.munky.libtech.component.WiredEnergyComponent.EnergyConnection
import dev.munky.libtech.util.codec
import java.util.concurrent.ConcurrentHashMap


class WiredEnergyComponent(
    override var connectedBlocks: MutableSet<EnergyConnection> = ConcurrentHashMap.newKeySet(),
    override var value: Double = 0.0,
    override var capacity: Double = Double.MAX_VALUE
) : WiredValueComponent<WiredEnergyComponent, EnergyConnection>() {

    override fun addConnection(connection: EnergyConnection) {
        connectedBlocks.add(connection)
    }

    override fun removeConnection(connection: EnergyConnection) {
        connectedBlocks.remove(connection)
    }

    override fun clone(): Component<ChunkStore> = WiredEnergyComponent(
        connectedBlocks.toMutableSet(),
        value, capacity
    )

    override fun toString() = "WiredEnergyComponent(connectedBlocks=$connectedBlocks, value=$value, capacity=$capacity)"

    data class EnergyConnection(
        override var start: Vector3i = Vector3i.ZERO,
        override var end: Vector3i = Vector3i.ZERO
    ) : Connection<WiredEnergyComponent, EnergyConnection> {
        companion object {
            val CODEC = codec(EnergyConnection::class, ::EnergyConnection) {
                field(EnergyConnection::start, Vector3i.CODEC) {
                    validate(Validators.nonNull())
                }
                field(EnergyConnection::end, Vector3i.CODEC) {
                    validate(Validators.nonNull())
                }
            }
        }
    }

    companion object {
        val CODEC: BuilderCodec<WiredEnergyComponent> = codec(WiredEnergyComponent::class, ::WiredEnergyComponent) {
            field(WiredEnergyComponent::connectedBlocks, SetCodec(EnergyConnection.CODEC, { ConcurrentHashMap.newKeySet() }, false))
            field(WiredEnergyComponent::value, Codec.DOUBLE)
        }

        fun getComponentType() = LibTech.get().wiredEnergyComponentType
    }
}