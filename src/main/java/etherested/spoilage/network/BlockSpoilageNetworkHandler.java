package etherested.spoilage.network;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.ChunkSpoilageData;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * handles network synchronization of block spoilage data to clients;
 * syncs spoilage data when player starts watching a chunk
 * or during periodic updates for active spoilage changes
 */
@EventBusSubscriber(modid = Spoilage.MODID)
public class BlockSpoilageNetworkHandler {

    // how often to sync spoilage updates (in ticks)
    private static final int SYNC_INTERVAL = 100; // 5 seconds
    private static int tickCounter = 0;

    /** syncs block spoilage data when a player starts watching a chunk */
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        ChunkPos chunkPos = event.getPos();

        // get all spoilage entries in this chunk
        Map<BlockPos, Float> chunkSpoilage = getChunkSpoilageData(level, chunkPos);

        if (!chunkSpoilage.isEmpty()) {
            // send spoilage data to the player
            PacketDistributor.sendToPlayer(player, new BlockSpoilageSyncPacket(chunkSpoilage));
        }
    }

    /** periodic sync of active spoilage changes to nearby players */
    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        tickCounter++;
        if (tickCounter < SYNC_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // get all block spoilage entries
        ChunkSpoilageData data = ChunkSpoilageCapability.getData(serverLevel);
        if (data == null) {
            return;
        }

        Map<BlockPos, ChunkSpoilageData.BlockSpoilageEntry> allEntries = data.getAllEntries();
        if (allEntries.isEmpty()) {
            return;
        }

        long worldTime = serverLevel.getGameTime();

        // group updates by chunk for efficient packet sending
        Map<ChunkPos, Map<BlockPos, Float>> chunkUpdates = new HashMap<>();
        // collect entries to remove (to avoid ConcurrentModificationException)
        List<BlockPos> entriesToRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, ChunkSpoilageData.BlockSpoilageEntry> entry : allEntries.entrySet()) {
            BlockPos pos = entry.getKey();
            ChunkSpoilageData.BlockSpoilageEntry spoilageEntry = entry.getValue();
            ChunkSpoilageData.BlockType type = spoilageEntry.type();

            // sync recovering crops for visual tint feedback
            if (type == ChunkSpoilageData.BlockType.CROP) {
                if (SpoilageConfig.isStaleSeedGrowthPenaltyEnabled() && spoilageEntry.initialSpoilage() > 0) {
                    long recoveryPeriod = SpoilageConfig.getStaleSeedRecoveryTicks();
                    float recovering = spoilageEntry.getRecoveringSpoilage(worldTime, recoveryPeriod);
                    if (recovering > 0.05f) {
                        BlockState cropState = serverLevel.getBlockState(pos);
                        if (!cropState.isAir() && cropState.getBlock() instanceof CropBlock) {
                            ChunkPos chunkPos = new ChunkPos(pos);
                            chunkUpdates.computeIfAbsent(chunkPos, k -> new HashMap<>()).put(pos, recovering);
                        }
                    }
                }
                continue;
            }

            BlockState state = serverLevel.getBlockState(pos);

            // handle MATURE_CROP specially, sync rot progress for visual tint
            if (type == ChunkSpoilageData.BlockType.MATURE_CROP) {
                // verify it's still a crop block
                if (state.isAir() || !(state.getBlock() instanceof CropBlock)) {
                    entriesToRemove.add(pos);
                    continue;
                }

                // calculate rot progress for mature crops
                long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
                long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
                float rotProgress = spoilageEntry.getRotProgress(worldTime, freshPeriod, rotPeriod);

                // only sync if there's visible rot
                if (rotProgress > 0.05f) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    chunkUpdates.computeIfAbsent(chunkPos, k -> new HashMap<>()).put(pos, rotProgress);
                }
                continue;
            }

            // verify block is still present and spoilable (for BLOCK, CAKE types)
            if (state.isAir() || !SpoilageItemRegistry.isBlockSpoilable(state.getBlock())) {
                // block was removed, mark for cleanup
                entriesToRemove.add(pos);
                continue;
            }

            // calculate current spoilage for regular spoilable blocks
            float spoilage = calculateBlockSpoilage(serverLevel, pos, state.getBlock(), spoilageEntry, worldTime);

            ChunkPos chunkPos = new ChunkPos(pos);
            chunkUpdates.computeIfAbsent(chunkPos, k -> new HashMap<>()).put(pos, spoilage);
        }

        // remove stale entries after iteration and notify clients to clear cache
        for (BlockPos pos : entriesToRemove) {
            data.removeEntry(pos);
            ChunkPos staleChunk = new ChunkPos(pos);
            chunkUpdates.computeIfAbsent(staleChunk, k -> new HashMap<>()).put(pos, 0.0f);
        }

        // send updates to players watching each chunk
        for (Map.Entry<ChunkPos, Map<BlockPos, Float>> chunkEntry : chunkUpdates.entrySet()) {
            ChunkPos chunkPos = chunkEntry.getKey();
            Map<BlockPos, Float> spoilageData = chunkEntry.getValue();

            if (!spoilageData.isEmpty()) {
                BlockSpoilageSyncPacket packet = new BlockSpoilageSyncPacket(spoilageData);

                // send to all players watching this chunk
                for (ServerPlayer player : serverLevel.players()) {
                    // check if player is tracking this chunk (within view distance)
                    if (isPlayerWatchingChunk(player, chunkPos)) {
                        PacketDistributor.sendToPlayer(player, packet);
                    }
                }
            }
        }
    }

    /** clears client cache when player disconnects or changes dimension */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // client cache is cleared when disconnecting (handled by ClientPlayerNetworkEvent)
    }

    /**
     * immediately syncs a single block's spoilage to all players watching the chunk;
     * used for instant visual updates when placing spoiled blocks
     */
    public static void syncSingleBlock(ServerLevel level, BlockPos pos, float spoilage) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, Float> spoilageData = new HashMap<>();
        spoilageData.put(pos, spoilage);
        BlockSpoilageSyncPacket packet = new BlockSpoilageSyncPacket(spoilageData);

        for (ServerPlayer player : level.players()) {
            if (isPlayerWatchingChunk(player, chunkPos)) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }

    /** checks if a player is watching a specific chunk */
    private static boolean isPlayerWatchingChunk(ServerPlayer player, ChunkPos chunkPos) {
        // check if chunk is within player's view distance
        ChunkPos playerChunk = player.chunkPosition();
        int viewDistance = player.serverLevel().getServer().getPlayerList().getViewDistance();

        int dx = Math.abs(playerChunk.x - chunkPos.x);
        int dz = Math.abs(playerChunk.z - chunkPos.z);

        return dx <= viewDistance && dz <= viewDistance;
    }

    /** gets all spoilage data for blocks in a chunk */
    private static Map<BlockPos, Float> getChunkSpoilageData(ServerLevel level, ChunkPos chunkPos) {
        Map<BlockPos, Float> result = new HashMap<>();

        ChunkSpoilageData data = ChunkSpoilageCapability.getData(level);
        if (data == null) {
            return result;
        }

        long worldTime = level.getGameTime();
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        for (Map.Entry<BlockPos, ChunkSpoilageData.BlockSpoilageEntry> entry : data.getAllEntries().entrySet()) {
            BlockPos pos = entry.getKey();

            // check if position is in this chunk
            if (pos.getX() < minX || pos.getX() > maxX || pos.getZ() < minZ || pos.getZ() > maxZ) {
                continue;
            }

            ChunkSpoilageData.BlockSpoilageEntry spoilageEntry = entry.getValue();
            ChunkSpoilageData.BlockType type = spoilageEntry.type();

            // sync recovering crops for visual tint feedback
            if (type == ChunkSpoilageData.BlockType.CROP) {
                if (SpoilageConfig.isStaleSeedGrowthPenaltyEnabled() && spoilageEntry.initialSpoilage() > 0) {
                    long recoveryPeriod = SpoilageConfig.getStaleSeedRecoveryTicks();
                    float recovering = spoilageEntry.getRecoveringSpoilage(worldTime, recoveryPeriod);
                    if (recovering > 0.05f) {
                        BlockState cropState = level.getBlockState(pos);
                        if (!cropState.isAir() && cropState.getBlock() instanceof CropBlock) {
                            result.put(pos, recovering);
                        }
                    }
                }
                continue;
            }

            BlockState state = level.getBlockState(pos);

            // handle MATURE_CROP specially, sync rot progress for visual tint
            if (type == ChunkSpoilageData.BlockType.MATURE_CROP) {
                // verify it's still a crop block
                if (state.isAir() || !(state.getBlock() instanceof CropBlock)) {
                    continue;
                }

                // calculate rot progress for mature crops
                long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
                long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
                float rotProgress = spoilageEntry.getRotProgress(worldTime, freshPeriod, rotPeriod);

                // only sync if there's visible rot
                if (rotProgress > 0.05f) {
                    result.put(pos, rotProgress);
                }
                continue;
            }

            // verify block is still present and spoilable (for BLOCK, CAKE types)
            if (state.isAir() || !SpoilageItemRegistry.isBlockSpoilable(state.getBlock())) {
                continue;
            }

            float spoilage = calculateBlockSpoilage(level, pos, state.getBlock(), spoilageEntry, worldTime);
            result.put(pos, spoilage);
        }

        return result;
    }

    /** calculates the current spoilage percentage for a block */
    private static float calculateBlockSpoilage(ServerLevel level, BlockPos pos, Block block,
                                                 ChunkSpoilageData.BlockSpoilageEntry entry, long worldTime) {
        // try to get lifetime from the linked item
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation linkedItemId = SpoilageItemRegistry.getLinkedItem(blockId);

        long lifetime;
        if (linkedItemId != null) {
            ItemStack itemStack = new ItemStack(BuiltInRegistries.ITEM.get(linkedItemId));
            lifetime = SpoilageCalculator.getLifetime(itemStack);
        } else {
            // fallback to default lifetime
            lifetime = 24000L * 3; // 3 Minecraft days
        }

        if (lifetime <= 0) {
            lifetime = 24000L * 3;
        }

        return entry.getSpoilage(worldTime, lifetime);
    }
}
