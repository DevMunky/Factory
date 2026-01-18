package dev.munky.factory.block.interaction

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.data.unknown.UnknownComponents
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.munky.factory.FactoryMod
import dev.munky.factory.block.component.MachineComponent
import dev.munky.libtech.component.WiredEnergyComponent
import dev.munky.libtech.util.codec

class MachineInteraction : SimpleBlockInteraction() {
    override fun interactWithBlock(
        world: World,
        commandBuffer: CommandBuffer<EntityStore?>,
        type: InteractionType,
        context: InteractionContext,
        itemInHand: ItemStack?,
        pos: Vector3i,
        cooldownHandler: CooldownHandler
    ) {
        val ref = context.entity
        val player = commandBuffer.getComponent<Player>(ref, Player.getComponentType()) ?: return

        val chunkReference = world.chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(pos.x, pos.z)) ?: return
        val chunkStore = chunkReference.getStore()

        val blocksComponent = chunkStore.getComponent(chunkReference, BlockComponentChunk.getComponentType()) ?: return
        val blockIndex = ChunkUtil.indexBlockInColumn(pos.x, pos.y, pos.z)
        val blockRef = blocksComponent.getEntityReference(blockIndex) ?: return

        val machine = chunkStore.getComponent(blockRef, MachineComponent.getComponentType())
        val energy = chunkStore.getComponent(blockRef, WiredEnergyComponent.getComponentType())
        player.sendMessage(Message.raw("machine=(${machine.hashCode()})$machine\nenergy=(${energy.hashCode()})$energy"))
    }

    override fun simulateInteractWithBlock(
        type: InteractionType,
        context: InteractionContext,
        itemInHand: ItemStack?,
        world: World,
        pos: Vector3i
    ) {

    }

    companion object {
        val CODEC = codec(MachineInteraction::class, ::MachineInteraction) {}
    }
}