package dev.munky.factory;

import com.hypixel.hytale.builtin.portals.components.PortalDeviceConfig;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.PortalKey;
import com.hypixel.hytale.server.core.asset.type.portalworld.PortalType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DungeonPortal implements Component<ChunkStore> {
      private PortalDeviceConfig config;
      private String baseBlockTypeKey;
      private PortalType portalType;
      private PortalKey portalKey;
      
      public DungeonPortal() {}
      
      public DungeonPortal(PortalDeviceConfig config, String baseBlockTypeKey) {
            this.config = config;
            this.baseBlockTypeKey = baseBlockTypeKey;
      }
      
      public PortalDeviceConfig getConfig() {
            return this.config;
      }
      
      public String getBaseBlockTypeKey() {
            return this.baseBlockTypeKey;
      }
      
      public BlockType getBaseBlockType() {
            return BlockType.getAssetMap().getAsset(this.baseBlockTypeKey);
      }
      
      public PortalType getPortalType() {
            return portalType;
      }
      
      public PortalKey getPortalKey() {
            return portalKey;
      }
      
      public void setPortalKey(PortalKey portalKey) {
            this.portalKey = portalKey;
            this.portalType = PortalType.getAssetMap().getAsset(portalKey.getPortalTypeId());
      }
      
      public CompletableFuture<DestinationResult> fetchDestinationState() {
            // TODO implement
            return CompletableFuture.completedFuture(new DestinationResult.Valid(
                  Set.of(),
                  120,
                  new Transform(0, 0, 0, 0, 0, 0),
                  "localhost",
                  5521
            ));
      }
      
      @Override
      public Component<ChunkStore> clone() {
            var clone =  new DungeonPortal(config, baseBlockTypeKey);
            clone.portalType = portalType;
            clone.portalKey = portalKey;
            return clone;
      }
      
      public static ComponentType<ChunkStore, DungeonPortal> getComponentType() {
            return FactoryMod.Companion.get().getDungeonPortalComponentType();
      }
      
      public static final BuilderCodec<DungeonPortal> CODEC
            = BuilderCodec.builder(DungeonPortal.class, DungeonPortal::new)
                    .appendInherited(
                          new KeyedCodec<>("Config", PortalDeviceConfig.CODEC),
                          (d, v) -> d.config = v,
                          d -> d.config,
                          (me, parent) -> me.config = parent.config
                    ).addValidator(Validators.nonNull()).add()
                    .append(
                          new KeyedCodec<>("BaseBlockType", Codec.STRING),
                          (d, v) -> d.baseBlockTypeKey = v,
                          d -> d.baseBlockTypeKey
                    ).addValidator(Validators.nonNull()).add()
                    .append(
                          new KeyedCodec<>("PortalKey", PortalKey.CODEC),
                          DungeonPortal::setPortalKey,
                          d -> d.portalKey
                    ).add()
                    .build();
      
      public sealed interface DestinationResult {
            record Invalid() implements DestinationResult {}
            record Valid(
                  Set<UUID> diedInWorld,
                  int timeLeftSeconds,
                  Transform spawnPoint,
                  String hostname,
                  int port
            ) implements DestinationResult {}
      }
}