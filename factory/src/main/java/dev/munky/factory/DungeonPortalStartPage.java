package dev.munky.factory;

import com.hypixel.hytale.builtin.portals.PortalsPlugin;
import com.hypixel.hytale.builtin.portals.integrations.PortalGameplayConfig;
import com.hypixel.hytale.builtin.portals.resources.PortalWorld;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.portalworld.PortalType;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * UI to start a dungeon with party members.
 */
public class DungeonPortalStartPage
      extends InteractiveCustomUIPage<DungeonPortalStartPage.Data> {
      
      private final Ref<ChunkStore> blockRef;
      
      public DungeonPortalStartPage(
            PlayerRef playerRef,
            Ref<ChunkStore> blockRef
      ) {
            super(
                  playerRef,
                  CustomPageLifetime.CanDismissOrCloseThroughInteraction,
                  Data.CODEC
            );
            this.blockRef = blockRef;
      }
      
      @Override
      public void build(
            @NotNull Ref<EntityStore> ref,
            @NotNull UICommandBuilder commandBuilder,
            @NotNull UIEventBuilder eventBuilder,
            @NotNull Store<EntityStore> store
      ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            
            Result result = computeState(player, store);
            
            if (result instanceof Result.Error.InvalidBlock) {
                  return;
            }
            
            if (result instanceof Result.Success success) {
                  commandBuilder.append("Pages/DungeonPortalStart.ui");
                  
                  PortalType portalType = success.portalDevice().getPortalType();
                  var portalDesc = portalType.getDescription();
                  String[] objectives = portalDesc.getObjectivesKeys();
                  String[] tips = portalDesc.getWisdomKeys();
                  
                  commandBuilder.set("#Artwork.Background", "Pages/Portals/" + portalDesc.getSplashImageFilename());
                  commandBuilder.set("#Title0.TextSpans", portalDesc.getDisplayName());
                  commandBuilder.set("#FlavorLabel.TextSpans", portalDesc.getFlavorText());
                  
                  updateCustomPills(commandBuilder, portalType);
                  
                  commandBuilder.set("#Objectives.Visible", objectives.length > 0);
                  commandBuilder.set("#Tips.Visible", tips.length > 0);
                  
                  updateBulletList(commandBuilder, "#ObjectivesList", objectives);
                  updateBulletList(commandBuilder, "#TipsList", tips);
                  
                  PortalGameplayConfig gameplayConfig = portalType.getGameplayConfig().getPluginConfig().get(PortalGameplayConfig.class);
                  
                  long totalMinutes = TimeUnit.SECONDS.toMinutes(success.portalDevice().getPortalKey().getTimeLimitSeconds());
                  
                  eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#SummonButton",
                        EventData.of("Action", "SummonActivated"),
                        false
                  );
                  
                  eventBuilder.addEventBinding(
                        CustomUIEventBindingType.MouseEntered,
                        "#SummonButton",
                        EventData.of("Action", "SummonMouseEntered"),
                        false
                  );
                  
                  eventBuilder.addEventBinding(
                        CustomUIEventBindingType.MouseExited,
                        "#SummonButton",
                        EventData.of("Action", "SummonMouseExited"),
                        false
                  );
                  
                  if (!portalType.isVoidInvasionEnabled()) {
                        commandBuilder.set(
                              "#ExplorationTimeText.TextSpans",
                              Message.translation("server.customUI.portalDevice.durationMins").param("time", totalMinutes)
                        );
                        return;
                  }
                  
                  assert gameplayConfig != null;
                  assert gameplayConfig.getVoidEvent() != null;
                  
                  long breachMinutes = TimeUnit.SECONDS.toMinutes(gameplayConfig.getVoidEvent().getDurationSeconds());
                  long exploreTime = totalMinutes - breachMinutes;
                  
                  commandBuilder.set(
                        "#ExplorationTimeText.TextSpans",
                        Message.translation("server.customUI.portalDevice.minutesToExplore").param("time", exploreTime)
                  );
                  
                  commandBuilder.set("#BreachTimeBullet.Visible", true);
                  
                  commandBuilder.set(
                        "#BreachTimeText.TextSpans",
                        Message.translation("server.customUI.portalDevice.minutesVoidInvasion").param("time", breachMinutes)
                  );
                  return;
            }
            
            // Error page
            commandBuilder.append("Pages/DungeonPortalError.ui");
            
            if (result instanceof Result.Error.PortalInsidePortal) {
                  commandBuilder.set(
                        "#UsageErrorLabel.Text",
                        Message.translation("server.customUI.portalDevice.portalInsidePortal")
                  );
            } else if (result instanceof Result.Error.MaxActivePortals) {
                  commandBuilder.set(
                        "#UsageErrorLabel.Text",
                        Message.translation("server.customUI.portalDevice.maxFragments").param("max", 4)
                  );
            } else if (result instanceof Result.Error.NotAPortalKey
                             || result instanceof Result.Error.NothingOffered) {
                  
                  commandBuilder.set(
                        "#UsageErrorTitle.Text",
                        Message.translation("server.customUI.portalDevice.needPortalKey")
                  );
                  commandBuilder.set(
                        "#UsageErrorLabel.Text",
                        Message.translation("server.customUI.portalDevice.nothingHeld")
                  );
            } else if (result instanceof Result.Error.InstanceKeyNotFound(String instanceId)) {
                  commandBuilder.set(
                        "#UsageErrorLabel.Text",
                        "The instance id '" + instanceId + "' does not exist, this is a developer error with the portaltype."
                  );
            } else if (result instanceof Result.Error.PortalTypeNotFound(String portalTypeId)) {
                  commandBuilder.set(
                        "#UsageErrorLabel.Text",
                        "The portaltype id '" + portalTypeId + "' does not exist, this is a developer error with the portal key."
                  );
            } else {
                  commandBuilder.set(
                        "#UsageErrorLabel.Text",
                        Message.translation("server.customUI.portalDevice.unknownError").param("state", result.toString())
                  );
            }
      }
      
      @Override
      public void handleDataEvent(
            @NotNull Ref<EntityStore> playerEcsRef,
            @NotNull Store<EntityStore> store,
            @NotNull Data data
      ) {
            Player playerComponent = store.getComponent(playerEcsRef, Player.getComponentType());
            if (playerComponent == null) return;
            
            Result result = computeState(playerComponent, store);
            if (!(result instanceof Result.Success(
                  WorldChunk worldChunk, BlockStateInfo blockState, DungeonPortal portalDevice
            ))) return;
            
            switch (data.action) {
                  case "SummonMouseEntered" -> {
                        UICommandBuilder builder = new UICommandBuilder();
                        builder.set("#Vignette.Visible", true);
                        sendUpdate(builder, null, false);
                        return;
                  }
                  case "SummonMouseExited" -> {
                        UICommandBuilder builder = new UICommandBuilder();
                        builder.set("#Vignette.Visible", false);
                        sendUpdate(builder, null, false);
                        return;
                  }
            }
            
            playerComponent.getPageManager().setPage(playerEcsRef, store, Page.None);
            
            World world = store.getExternalData().getWorld();
            
            // Move players to server, default port is 5520
            portalDevice.fetchDestinationState().thenAcceptAsync((state) -> {
                  if (!playerEcsRef.isValid()) {
                        playerComponent.sendMessage(Message.raw("Player ref invalid."));
                        return;
                  }
                  switch (state) {
                        case DungeonPortal.DestinationResult.Invalid _ -> {
                              playerComponent.sendMessage(DESTINATION_INVALID);
                        }
                        case DungeonPortal.DestinationResult.Valid valid -> {
                              var targetHost = valid.hostname();
                              var targetPort = valid.port();
                              
                              // This should actually move the player to the destination
                              HytaleLogger.getLogger().atInfo().log("Sent " + playerRef.getUsername() + " to " + targetHost + ":" + targetPort);
                              playerRef.referToServer(targetHost, targetPort);
//
//                              Transform playerPos = store.ensureAndGetComponent(playerEcsRef, TransformComponent.getComponentType()).getTransform();
//                              store.addComponent(playerEcsRef, Teleport.getComponentType(), new Teleport(playerPos.getPosition().add(0d, 10d, 0d), playerPos.getRotation()));
//
//                              UUIDComponent uuidComponent = store.getComponent(playerEcsRef, UUIDComponent.getComponentType());
//                              assert uuidComponent != null;
//                              UUID playerUuid = uuidComponent.getUuid();
//
//                              int x = ChunkUtil.xFromBlockInColumn(blockState.getIndex());
//                              int y = ChunkUtil.yFromBlockInColumn(blockState.getIndex());
//                              int z = ChunkUtil.zFromBlockInColumn(blockState.getIndex());
                        }
                  }
            }, world);
      }
      
      private Result computeState(Player player, ComponentAccessor<EntityStore> accessor) {
            if (!blockRef.isValid()) return Result.Error.InvalidBlock.INSTANCE;
            
            if (PortalsPlugin.getInstance().countActiveFragments() >= 4) {
                  return Result.Error.MaxActivePortals.INSTANCE;
            }
            
            Store<ChunkStore> chunkStore = blockRef.getStore();
            
            BlockStateInfo blockInfo = chunkStore.getComponent(blockRef, BlockStateInfo.getComponentType());
            DungeonPortal device = chunkStore.getComponent(blockRef, DungeonPortal.getComponentType());
            
            if (blockInfo == null || device == null) {
                  return Result.Error.InvalidBlock.INSTANCE;
            }
            
//            if (device.getDestinationWorld() == null) {
//                  return Result.Error.InvalidBlock.INSTANCE;
//            }
            
            WorldChunk chunk = chunkStore.getComponent(blockInfo.getChunkRef(), WorldChunk.getComponentType());
            if (chunk == null) {
                  return Result.Error.InvalidBlock.INSTANCE;
            }
            
            PortalWorld inside = accessor.getResource(PortalWorld.getResourceType());
            if (inside.exists()) {
                  return Result.Error.PortalInsidePortal.INSTANCE;
            }
            
            return new Result.Success(
                  chunk, blockInfo, device
            );
      }
      
      
      public static final class Data {
            public String action;
            
            public static final BuilderCodec<Data> CODEC
                  = BuilderCodec.builder(Data.class, Data::new)
                          .append(
                                new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v,
                                d -> d.action
                          ).add()
                          .build();
      }
      
      public static void decrementItemInHand(Inventory inventory, int amount) {
            if (!inventory.usingToolsItem()) {
                  short slot = inventory.getActiveHotbarSlot();
                  if (slot != -1) {
                        ItemStack stack =
                              inventory.getHotbar().getItemStack(slot);
                        if (stack != null) {
                              inventory.getHotbar().removeItemStackFromSlot(
                                    slot, stack, amount, false, true
                              );
                        }
                  }
            }
      }
      
      public static void updateBulletList(
            UICommandBuilder builder,
            String selector,
            String[] keys
      ) {
            for (int i = 0; i < keys.length; i++) {
                  builder.append(selector, "Pages/Portals/BulletPoint.ui");
                  builder.set(
                        selector + "[" + i + "] #Label.TextSpans",
                        Message.translation(keys[i])
                  );
            }
      }
      
      public static void updateCustomPills(
            UICommandBuilder builder,
            PortalType portalType
      ) {
            var pills = portalType.getDescription().getPillTags();
            for (int i = 0; i < pills.size(); i++) {
                  var pill = pills.get(i);
                  builder.append("#Pills", "Pages/Portals/Pill.ui");
                  builder.set(
                        "#Pills[" + i + "].Background.Color",
                        ColorParseUtil.colorToHexString(pill.getColor())
                  );
                  builder.set(
                        "#Pills[" + i + "] #Label.TextSpans",
                        pill.getMessage()
                  );
            }
      }
      
      public sealed interface Result permits Result.Success, Result.Error {
            
            record Success(
                  WorldChunk worldChunk,
                  BlockStateInfo blockState,
                  DungeonPortal portalDevice
            ) implements Result {}
            
            sealed interface Error extends Result permits Error.InvalidBlock,
                                                                Error.InvalidDestination,
                                                                Error.OfferedIsNotHeld,
                                                                Error.NothingOffered,
                                                                Error.NotAPortalKey,
                                                                Error.PortalInsidePortal,
                                                                Error.MaxActivePortals,
                                                                Error.BotchedGameplayConfig,
                                                                Error.InstanceKeyNotFound,
                                                                Error.PortalTypeNotFound
            {
                  enum InvalidBlock implements Error { INSTANCE }
                  enum InvalidDestination implements Error { INSTANCE }
                  enum OfferedIsNotHeld implements Error { INSTANCE }
                  enum NothingOffered implements Error { INSTANCE }
                  enum NotAPortalKey implements Error { INSTANCE }
                  enum PortalInsidePortal implements Error { INSTANCE }
                  enum MaxActivePortals implements Error { INSTANCE }
                  enum BotchedGameplayConfig implements Error { INSTANCE }
                  
                  record InstanceKeyNotFound(String instanceId) implements Error {}
                  record PortalTypeNotFound(String portalTypeId) implements Error {}
            }
      }
      
      @NotNull
      public static final Message DESTINATION_INVALID = Message.raw("This portal's destination is invalid.");
}