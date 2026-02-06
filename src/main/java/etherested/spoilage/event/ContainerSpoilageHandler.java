package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.ContainerSpoilageRates;
import etherested.spoilage.logic.SpoilageCalculator;
import etherested.spoilage.logic.SpoilageProcessor;
import etherested.spoilage.logic.preservation.PreservationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ContainerSpoilageHandler {

    // track containers processed this tick to prevent duplicate processing
    // when multiple players are near the same container
    private static final Set<BlockPos> processedThisTick = new HashSet<>();
    private static long lastTickTime = -1;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!SpoilageConfig.isEnabled()) return;

        long worldTime = level.getGameTime();
        if (worldTime % SpoilageConfig.getCheckIntervalTicks() != 0) return;

        // reset processed set when tick changes
        if (worldTime != lastTickTime) {
            processedThisTick.clear();
            lastTickTime = worldTime;
        }

        // periodically check for config changes
        ContainerSpoilageRates.checkForConfigRefresh(worldTime);

        // collect unique chunks to process first (avoids O(n²) when multiple players overlap)
        Set<ChunkPos> chunksToProcess = new HashSet<>();
        int viewDistance = level.getServer().getPlayerList().getViewDistance();

        for (ServerPlayer player : level.players()) {
            int chunkX = player.chunkPosition().x;
            int chunkZ = player.chunkPosition().z;

            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                    chunksToProcess.add(new ChunkPos(chunkX + dx, chunkZ + dz));
                }
            }
        }

        // process each unique chunk once
        for (ChunkPos chunkPos : chunksToProcess) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;

            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof Container container) {
                    BlockPos pos = be.getBlockPos();
                    // Skip if already processed this tick
                    if (processedThisTick.contains(pos)) continue;
                    processedThisTick.add(pos);

                    processContainerWithPreservation(container, be, pos, worldTime, level);
                }
            }
        }
    }

    private static void processContainerWithPreservation(Container container, BlockEntity blockEntity, BlockPos pos, long worldTime, Level level) {
        // get preservation info from the manager (includes Y-level, biome, and container factors)
        PreservationManager.PreservationInfo info = PreservationManager.getContainerPreservationInfo(level, pos, blockEntity);
        float combinedMultiplier = info.getCombinedMultiplier();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && SpoilageCalculator.isSpoilable(stack)) {
                // Apply preservation effects, storing both container multiplier and biome multiplier
                adjustSpoilageForPreservation(stack, info, worldTime);
                SpoilageProcessor.processStack(stack, worldTime, level);

                // Check for rotten replacement after processing
                ItemStack replacement = SpoilageProcessor.checkRottenReplacement(stack, worldTime);
                if (replacement != null) {
                    container.setItem(i, replacement);
                }
            }
        }
    }

    /**
     * adjusts item's preservation savings to simulate slower spoilage while in container;
     * uses cumulative savings approach instead of modifying creation time to prevent
     * rollback issues when multiple players are near the same container;
     * also stores the current preservation multipliers for smooth tooltip display
     * @param stack the item stack to adjust
     * @param info the preservation info containing all multipliers
     * @param worldTime current world time
     */
    private static void adjustSpoilageForPreservation(ItemStack stack, PreservationManager.PreservationInfo info, long worldTime) {
        SpoilageData data = SpoilageCalculator.getInitializedData(stack);
        if (data == null || data.isPaused()) {
            return;
        }

        float combinedMultiplier = info.getCombinedMultiplier();

        // skip if already processed this tick (item-level deduplication)
        if (data.lastYLevelProcessTick() == worldTime) {
            return;
        }

        long lastProcessTick = data.lastYLevelProcessTick();

        // first time processing - record the tick and multipliers, don't give savings yet
        // this prevents giving savings for time the item wasn't actually in the container
        if (lastProcessTick <= 0) {
            stack.set(ModDataComponents.SPOILAGE_DATA.get(),
                    data.withContainerPreservation(combinedMultiplier, info.biomeMultiplier(), worldTime));
            return;
        }

        // if combinedMultiplier is 1.0, no preservation effect - just update the multipliers for display
        if (combinedMultiplier >= 1.0f) {
            stack.set(ModDataComponents.SPOILAGE_DATA.get(),
                    data.withContainerPreservation(combinedMultiplier, info.biomeMultiplier(), worldTime));
            return;
        }

        // calculate actual elapsed time since last processing
        long elapsedSinceLastProcess = worldTime - lastProcessTick;

        // sanity check - don't give savings for unreasonably long periods
        // (e.g., if item was moved between containers or chunk was unloaded)
        int maxElapsed = SpoilageConfig.getCheckIntervalTicks() * 3;
        if (elapsedSinceLastProcess > maxElapsed) {
            elapsedSinceLastProcess = maxElapsed;
        }

        // calculate ticks saved this interval due to preservation
        // if combinedMultiplier is 0.5, items spoil at half speed, so we "give back" half the elapsed time
        long ticksSaved = (long) (elapsedSinceLastProcess * (1.0f - combinedMultiplier));

        // add savings to cumulative total and update multipliers for display
        SpoilageData updated = data.addYLevelSavings(ticksSaved, worldTime);
        stack.set(ModDataComponents.SPOILAGE_DATA.get(), new SpoilageData(
                updated.creationTime(),
                updated.remainingLifetime(),
                updated.isPaused(),
                updated.preservationMultiplier(),
                updated.yLevelSavedTicks(),
                updated.lastYLevelProcessTick(),
                combinedMultiplier,
                info.biomeMultiplier()
        ));
    }
}
