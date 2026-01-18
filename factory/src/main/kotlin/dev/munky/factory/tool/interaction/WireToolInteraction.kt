package dev.munky.factory.tool.interaction

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.munky.factory.FactoryMod
import dev.munky.libtech.component.WiredEnergyComponent
import dev.munky.libtech.util.codec

class WireToolInteraction : SimpleBlockInteraction() {
    override fun interactWithBlock(
        world: World,
        commandBuffer: CommandBuffer<EntityStore>,
        type: InteractionType,
        context: InteractionContext,
        itemInHand: ItemStack?,
        pos: Vector3i,
        cooldownHandler: CooldownHandler
    ) {
        val refToPlayer = context.entity
        val playerRef = commandBuffer.getComponent(refToPlayer, PlayerRef.getComponentType()) ?: return

        val chunkReference = world.chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(pos.x, pos.z)) ?: return
        val chunkStore = chunkReference.getStore()

        val blocksComponent = chunkStore.getComponent(chunkReference, BlockComponentChunk.getComponentType()) ?: return
        val targetIndex = ChunkUtil.indexBlockInColumn(pos.x, pos.y, pos.z)
        val targetRef = blocksComponent.getEntityReference(targetIndex) ?: return

        val targetEnergy = chunkStore.getComponent(targetRef, WiredEnergyComponent.getComponentType())
        val toolData = commandBuffer.getComponent(refToPlayer, WireToolDataComponent.getComponentType())

        if (type == InteractionType.Primary && targetEnergy != null) {
            targetEnergy.value += 10
            return
        }

        when {
            toolData != null -> {
                commandBuffer.removeComponent(refToPlayer, WireToolDataComponent.getComponentType())
                val rootEnergy = toolData.wireComponent

                when (targetEnergy) {
                    null -> {
                        playerRef.sendMessage(Message.raw("This is not an energy block."))
                    }
                    else -> {
                        commandBuffer.run {
                            if (targetEnergy === rootEnergy) {
                                println("stupid wrench added two routes")
                                return@run
                            }
                            targetEnergy.addConnection(WiredEnergyComponent.EnergyConnection(start = toolData.position, end = pos))
                            rootEnergy.addConnection(WiredEnergyComponent.EnergyConnection(start = pos, end = toolData.position))
                        }
                        playerRef.sendMessage(Message.raw("wired energy block"))
                    }
                }
            }
            targetEnergy != null -> {
                commandBuffer.addComponent(refToPlayer, WireToolDataComponent.getComponentType(), WireToolDataComponent(
                    position = pos,
                    ref = targetRef,
                    wireComponent = targetEnergy
                ))
                playerRef.sendMessage(Message.raw("Interact with another wired block to connect them."))
            }
            else -> {
                // do nothing, as an energy block was not interacted with.
            }
        }
    }

    override fun simulateInteractWithBlock(
        type: InteractionType,
        context: InteractionContext,
        itemInHand: ItemStack?,
        world: World,
        pos: Vector3i
    ) {

    }

    data class WireToolDataComponent(
        val position: Vector3i,
        val ref: Ref<ChunkStore>,
        val wireComponent: WiredEnergyComponent
    ) : Component<EntityStore> {
        override fun clone(): Component<EntityStore> = copy()

        companion object {
            fun getComponentType() = FactoryMod.get().wireToolInteractionDataComponentType
        }
    }

    companion object {
        val CODEC = codec(WireToolInteraction::class, ::WireToolInteraction) {}
    }
}