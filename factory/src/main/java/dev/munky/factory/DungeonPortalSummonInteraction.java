package dev.munky.factory;

import com.hypixel.hytale.builtin.portals.components.PortalDeviceConfig;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.PortalKey;
import com.hypixel.hytale.server.core.asset.type.portalworld.PortalType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**+
 * Interaction to summon a portal, ready for players to start queuing into to.
 */
public class DungeonPortalSummonInteraction extends SimpleBlockInteraction {
      
      @Override
      protected void interactWithBlock(
            @NotNull World world,
            @NotNull CommandBuffer<EntityStore> commandBuffer,
            @NotNull InteractionType type,
            @NotNull InteractionContext context,
            @Nullable ItemStack itemInHand,
            @NotNull Vector3i targetBlock,
            @NotNull CooldownHandler cooldownHandler
      ) {
            Ref<EntityStore> playerEcsRef = context.getEntity();
            Player playerComponent = commandBuffer.getComponent(playerEcsRef, Player.getComponentType());
            
            if (playerComponent == null) {
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            int x = targetBlock.x;
            int y = targetBlock.y;
            int z = targetBlock.z;
            
            WorldChunk worldChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
            if (worldChunk == null) {
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            DungeonPortal portalDevice = BlockModule.get().getComponent(DungeonPortal.getComponentType(), world, x, y, z);
            if (portalDevice == null) {
                  playerComponent.sendMessage(Message.raw("Not a dungeon portal block."));
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            if (itemInHand == null || itemInHand.getItem().getPortalKey() == null) {
                  playerComponent.sendMessage(Message.raw("No portal key in hand."));
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            PortalKey portalKey = itemInHand.getItem().getPortalKey();
            PortalType portalType = PortalType.getAssetMap().getAsset(portalKey.getPortalTypeId());
            if (portalType == null) {
                  playerComponent.sendMessage(Message.raw("Portal type '" + portalKey.getPortalTypeId() + "' not found. Ensure portal key is valid."));
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            PortalDeviceConfig config = portalDevice.getConfig();
            if (config == null) {
                  playerComponent.sendMessage(Message.raw("Dungeon portal component is missing portal device config."));
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            BlockType blockType = worldChunk.getBlockType(x, y, z);
            assert blockType != null;
            if (blockType != portalDevice.getBaseBlockType()) {
                  playerComponent.sendMessage(Message.raw("Portal base block '" + portalDevice.getBaseBlockType().getId() + "' does not match actual block type '" + blockType.getId() + "'."));
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            if (!config.areBlockStatesValid(blockType)) {
                  playerComponent.sendMessage(Message.raw("Actual block type '" + blockType.getId() + "' is not a valid portal block type (not in config)."));
                  context.getState().state = InteractionState.Failed;
                  return;
            }
            
            BlockType spawning = blockType.getBlockForState(config.getSpawningState());
            BlockType on = blockType.getBlockForState(config.getOnState());
            BlockType off = blockType.getBlockForState(config.getOffState());
            
            assert on != null;
            assert off != null;
            assert spawning != null;
            
            //noinspection removal
            int rotation = worldChunk.getRotationIndex(x, y, z);
            
            // primed
            worldChunk.setBlock(x, y, z, BlockType.getAssetMap().getIndex(on.getId()), on, rotation, 0, 6);
            portalDevice.setPortalKey(portalKey);
            
            var blockRef = worldChunk.getBlockComponentEntity(x, y, z);
            assert blockRef != null;
            world.getChunkStore().getStore().putComponent(blockRef, DungeonPortal.getComponentType(), portalDevice);
            
            worldChunk.markNeedsSaving();
            
      }
      
      @Override
      protected void simulateInteractWithBlock(
            @NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext,
            @Nullable ItemStack itemStack,
            @NotNull World world, @NotNull Vector3i vector3i
      ) {}
      
      @NotNull
      public static final BuilderCodec<DungeonPortalSummonInteraction> CODEC = BuilderCodec.builder(DungeonPortalSummonInteraction.class, DungeonPortalSummonInteraction::new).build();
      @NotNull
      public static final Duration MINIMUM_TIME_IN_WORLD = Duration.ofMillis(3000L);
      
      
}