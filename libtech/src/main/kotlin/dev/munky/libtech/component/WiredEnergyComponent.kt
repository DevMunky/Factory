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
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.libtech.LibTech
import dev.munky.libtech.component.WiredEnergyComponent.EnergyConnection
import dev.munky.libtech.util.codec


data class WiredEnergyComponent(
    override var connections: MutableSet<EnergyConnection> = HashSet(),
) : WiredValueComponent<WiredEnergyComponent, EnergyConnection>() {
    override var capacity: Double = Double.MAX_VALUE

    override fun clone(): Component<ChunkStore> = copy()

    data class EnergyConnection(
        override var position: Vector3i = Vector3i.ZERO
    ) : Connection<WiredEnergyComponent, EnergyConnection> {
        companion object {
            val CODEC = codec(EnergyConnection::class, ::EnergyConnection) {
                field(EnergyConnection::position, Vector3i.CODEC) {
                    validate(Validators.nonNull())
                }
            }

            init {
                CodecStore.STATIC.putCodec(CodecKey("EnergyConnectionCodec"), CODEC)
            }
        }
    }

    companion object {
        val CODEC: BuilderCodec<WiredEnergyComponent> = codec(WiredEnergyComponent::class, ::WiredEnergyComponent) {
            field(WiredEnergyComponent::connections, SetCodec(StoredCodec(CodecKey("EnergyConnectionCodec")), ::mutableSetOf, false))
            field(WiredEnergyComponent::value, Codec.DOUBLE)
        }

        fun getComponentType() = LibTech.get().wiredEnergyComponentType
    }
}