@file:Suppress("DEPRECATION", "removal")
package dev.munky.factory.block.component

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.codecs.set.SetCodec
import com.hypixel.hytale.codec.validation.Validators
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import dev.munky.factory.FactoryMod
import dev.munky.libtech.util.codec

class MachineComponent(
    var number: Int? = null,
    var id: String? = null,
    var items: MutableSet<ItemStack> = mutableSetOf()
): Component<ChunkStore> {
    override fun clone(): Component<ChunkStore> = MachineComponent(number, id, items.toMutableSet())

    override fun toString() = "MachineComponent(number=$number, id=$id, items=$items)"

    companion object {
        val CODEC = codec(MachineComponent::class, ::MachineComponent) {
            field(MachineComponent::number, Codec.INTEGER) {
                validate(Validators.nonNull())
                meta {
                    description = "The number of this machine"
                }
            }
            field(MachineComponent::id, Codec.STRING) {
                meta {
                    description = "The id of this machine"
                }
            }
            field(MachineComponent::items, SetCodec(ItemStack.CODEC, ::HashSet, false)) {
                meta {
                    description = "The items in this machine"
                }
            }
        }

        fun getComponentType() = FactoryMod.get().machineComponentType
    }
}