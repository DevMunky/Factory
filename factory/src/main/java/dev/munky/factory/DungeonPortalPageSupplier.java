package dev.munky.factory;

import com.hypixel.hytale.builtin.portals.components.PortalDeviceConfig;
import com.hypixel.hytale.builtin.portals.utils.BlockTypeUtils;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class DungeonPortalPageSupplier implements CustomPageSupplier {
      @Override
      public CustomUIPage tryCreate(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> store,
            PlayerRef playerRef,
            InteractionContext context
      ) {
            BlockPosition targetBlock = context.getTargetBlock();
            if (targetBlock == null) return null;
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return null;
            
            World world = store.getExternalData().getWorld();
            
            int x = targetBlock.x;
            int y = targetBlock.y;
            int z = targetBlock.z;
            
            var blockType = world.getBlockType(x, y, z);
            if (blockType == null) return null;
            var blockRef = BlockModule.getBlockEntity(world, x, y, z);
            if (blockRef == null) {
                  playerRef.sendMessage(
                        Message.translation("server.portals.device.blockEntityMisconfigured")
                  );
                  return null;
            }
            
            var chunkStore = world.getChunkStore();
            var existingDevice = chunkStore.getStore().getComponent(blockRef, DungeonPortal.getComponentType());
            if (existingDevice == null) {
                  // Not a proper portal.
                  return null;
            }
            
            for (String blockStateKey : existingDevice.getConfig().getBlockStates()) {
                  if (BlockTypeUtils.getBlockForState(blockType, blockStateKey) == null) {
                        playerRef.sendMessage(
                              Message.translation("server.portals.device.blockStateMisconfigured")
                                    .param("state", blockStateKey)
                        );
                        return null;
                  }
            }
            
            if (existingDevice.getPortalType() == null) {
                  playerRef.sendMessage(Message.raw("non-initialized portal: portalKey=" + existingDevice.getPortalKey()));
                  // Portal has not been summoned yet.
                  return null;
            }
            
            // gets set in interaction
//            chunkStore.getStore().putComponent(
//                  blockRef,
//                  DungeonPortal.getComponentType(),
//                  new DungeonPortal(config, blockType.getId())
//            );
            
            return new DungeonPortalStartPage(playerRef, blockRef);
      }
      
      public static final BuilderCodec<DungeonPortalPageSupplier> CODEC
            = BuilderCodec.builder(DungeonPortalPageSupplier.class, DungeonPortalPageSupplier::new).build();
}