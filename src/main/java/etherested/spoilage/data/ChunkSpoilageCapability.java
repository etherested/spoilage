package etherested.spoilage.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

// utility class for accessing chunk-level spoilage data;
// provides methods for storing and retrieving block spoilage data
public class ChunkSpoilageCapability {

    private static final String DATA_NAME = "spoilage_block_data";

    // gets the spoilage data for a level, creating it if it doesn't exist
    public static ChunkSpoilageData getData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ChunkSpoilageData.factory(),
                DATA_NAME
        );
    }

    // sets spoilage data for a block position
    public static void setBlockSpoilage(Level level, BlockPos pos, ChunkSpoilageData.BlockSpoilageEntry entry) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkSpoilageData data = getData(serverLevel);
        data.setEntry(pos, entry);
    }

    // gets spoilage data for a block position
    public static ChunkSpoilageData.BlockSpoilageEntry getBlockSpoilage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        ChunkSpoilageData data = getData(serverLevel);
        return data.getEntry(pos);
    }

    // removes spoilage data for a block position
    public static void removeBlockSpoilage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkSpoilageData data = getData(serverLevel);
        data.removeEntry(pos);
    }

    // checks if a block position has spoilage data
    public static boolean hasBlockSpoilage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        ChunkSpoilageData data = getData(serverLevel);
        return data.hasEntry(pos);
    }

    // creates a new entry for a placed spoilable block (starts fresh);
    // works with any block type (cakes, placed food, etc.)
    public static void setBlockPlaced(Level level, BlockPos pos) {
        setBlockPlacedWithSpoilage(level, pos, 0.0f);
    }

    // creates a new entry for a placed spoilable block with preserved spoilage from the item;
    // block spoilage continues after placement (not paused)
    public static void setBlockPlacedWithSpoilage(Level level, BlockPos pos, float initialSpoilage) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long worldTime = level.getGameTime();
        ChunkSpoilageData.BlockSpoilageEntry entry =
                ChunkSpoilageData.BlockSpoilageEntry.createWithSpoilage(worldTime, initialSpoilage, ChunkSpoilageData.BlockType.BLOCK);

        ChunkSpoilageData data = getData(serverLevel);
        data.setEntry(pos, entry);
    }

    // updates the spoilage value for a growing crop;
    // used when crop freshness increases due to growth
    public static void updateCropSpoilage(Level level, BlockPos pos, float newSpoilage) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkSpoilageData data = getData(serverLevel);
        ChunkSpoilageData.BlockSpoilageEntry existing = data.getEntry(pos);
        if (existing == null) {
            return;
        }

        // create updated entry with new spoilage value
        ChunkSpoilageData.BlockSpoilageEntry updated =
                new ChunkSpoilageData.BlockSpoilageEntry(
                        existing.creationTime(),
                        newSpoilage,
                        existing.isPaused(),
                        existing.type(),
                        existing.fullyGrownTime()
                );

        data.setEntry(pos, updated);
    }

    // gets the current spoilage percentage for a block
    public static float getBlockSpoilagePercent(Level level, BlockPos pos, long lifetime) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0.0f;
        }

        ChunkSpoilageData data = getData(serverLevel);
        ChunkSpoilageData.BlockSpoilageEntry entry = data.getEntry(pos);

        if (entry == null) {
            return 0.0f;
        }

        long worldTime = level.getGameTime();
        return entry.getSpoilage(worldTime, lifetime);
    }

    // creates a tracking entry for a newly planted crop;
    // stores seed spoilage which is used if harvested before maturity;
    // when crop reaches maturity it becomes 100% fresh and starts the rot cycle
    public static void setCropPlanted(Level level, BlockPos pos, float seedSpoilage) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long worldTime = level.getGameTime();
        ChunkSpoilageData.BlockSpoilageEntry entry =
                ChunkSpoilageData.BlockSpoilageEntry.createGrowingCrop(worldTime, seedSpoilage);

        ChunkSpoilageData data = getData(serverLevel);
        data.setEntry(pos, entry);
    }

    // marks a crop as fully grown, starting the fresh period timer;
    // changes the entry type to MATURE_CROP and sets fullyGrownTime
    public static void markCropFullyGrown(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long worldTime = level.getGameTime();
        ChunkSpoilageData data = getData(serverLevel);
        ChunkSpoilageData.BlockSpoilageEntry existing = data.getEntry(pos);

        ChunkSpoilageData.BlockSpoilageEntry entry;
        if (existing != null) {
            entry = existing.markFullyGrown(worldTime);
        } else {
            entry = ChunkSpoilageData.BlockSpoilageEntry.createMatureCrop(worldTime);
        }

        data.setEntry(pos, entry);
    }

    // gets the current rot progress for a fully grown crop;
    // returns rot progress from 0.0 (fresh) to 1.0 (fully rotten)
    public static float getCropRotProgress(Level level, BlockPos pos, long freshPeriod, long rotPeriod) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0.0f;
        }

        ChunkSpoilageData data = getData(serverLevel);
        ChunkSpoilageData.BlockSpoilageEntry entry = data.getEntry(pos);

        if (entry == null || !entry.isFullyGrown()) {
            return 0.0f;
        }

        long worldTime = level.getGameTime();
        return entry.getRotProgress(worldTime, freshPeriod, rotPeriod);
    }

    // resets the fully grown time for a mature crop (e.g. when bone meal is used);
    // this restarts the fresh period timer
    public static void resetCropFullyGrownTime(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkSpoilageData data = getData(serverLevel);
        ChunkSpoilageData.BlockSpoilageEntry existing = data.getEntry(pos);

        if (existing == null || !existing.isFullyGrown()) {
            return;
        }

        long worldTime = level.getGameTime();
        ChunkSpoilageData.BlockSpoilageEntry updated = existing.resetFullyGrownTime(worldTime);
        data.setEntry(pos, updated);
    }
}
