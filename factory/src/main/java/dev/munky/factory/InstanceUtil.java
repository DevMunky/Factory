package dev.munky.factory;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.common.util.FormatUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import com.hypixel.hytale.sneakythrow.SneakyThrow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

public class InstanceUtil {
      /**
       * Spawns an instance with the given name, without a return point.
       */
      @NotNull
      public static CompletableFuture<World> spawnInstance(@NotNull String name, @Nullable String instancedWorldName) {
            Universe universe = Universe.get();
            Path path = universe.getPath();
            Path assetPath = InstancesPlugin.getInstanceAssetPath(name);
            LOGGER.atInfo().log("Spawning instance %s from %s", name, assetPath);
            UUID uuid = UUID.randomUUID();
            String worldKey = Objects.requireNonNullElse(instancedWorldName, "instance-" + InstancesPlugin.safeName(name) + "-" + uuid);
            
            Path worldPath = path.resolve("worlds").resolve(worldKey);
            return WorldConfig.load(assetPath.resolve("instance.bson")).thenApplyAsync(SneakyThrow.sneakyFunction((config) -> {
                  config.setUuid(uuid);
                  config.setDisplayName(WorldConfig.formatDisplayName(name));
                  config.markChanged();
                  long start = System.nanoTime();
                  LOGGER.at(Level.INFO).log("Copying instance files for %s to world %s", name, worldKey);
                  
                  try (Stream<Path> files = Files.walk(assetPath, FileUtil.DEFAULT_WALK_TREE_OPTIONS_ARRAY)) {
                        files.forEach(SneakyThrow.sneakyConsumer((filePath) -> {
                              Path rel = assetPath.relativize(filePath);
                              Path toPath = worldPath.resolve(rel.toString());
                              if (Files.isDirectory(filePath)) {
                                    Files.createDirectories(toPath);
                              } else if (Files.isRegularFile(filePath)) {
                                    Files.copy(filePath, toPath);
                              }
                        }));
                  }
                  
                  LOGGER.at(Level.INFO).log("Completed instance files for %s to world %s in %s", name, worldKey, FormatUtil.nanosToString(System.nanoTime() - start));
                  return config;
            })).thenCompose((config) -> universe.makeWorld(worldKey, worldPath, config));
      }
      
      private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
}