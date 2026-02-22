package etherested.spoilage.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// client-side cache for block spoilage data;
// stores spoilage percentages received from the server for rendering purposes
public class BlockSpoilageClientCache {

    // map of block position to spoilage percentage (0.0 to 1.0)
    private static final Map<BlockPos, Float> CACHE = new ConcurrentHashMap<>();

    // gets the spoilage percentage for a block position
    // @param pos the block position
    // @return the spoilage percentage (0.0 to 1.0), or 0.0 if not cached
    public static float getSpoilage(BlockPos pos) {
        return CACHE.getOrDefault(pos, 0.0f);
    }

    // checks if a block position has cached spoilage data
    // @param pos the block position
    // @return true if spoilage data exists for this position
    public static boolean hasSpoilage(BlockPos pos) {
        return CACHE.containsKey(pos);
    }

    // updates the spoilage value for a block position
    // @param pos the block position
    // @param spoilage the spoilage percentage (0.0 to 1.0)
    public static void updateSpoilage(BlockPos pos, float spoilage) {
        if (spoilage <= 0.0f) {
            CACHE.remove(pos);
        } else {
            CACHE.put(pos.immutable(), spoilage);
        }
    }

    // removes spoilage data for a block position
    // @param pos The block position
    public static void removeSpoilage(BlockPos pos) {
        CACHE.remove(pos);
    }

    // clears all cached spoilage data for a chunk
    // @param chunkPos The chunk position
    public static void clearChunk(ChunkPos chunkPos) {
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        CACHE.keySet().removeIf(pos ->
                pos.getX() >= minX && pos.getX() <= maxX &&
                        pos.getZ() >= minZ && pos.getZ() <= maxZ
        );
    }

    // clears all cached spoilage data;
    // called when disconnecting from server or changing dimensions
    public static void clearAll() {
        CACHE.clear();
    }

    // gets all cached block positions with spoilage data;
    // used by the block renderer to find blocks that need visual effects
    // @return unmodifiable view of the cache
    public static Map<BlockPos, Float> getAll() {
        return Map.copyOf(CACHE);
    }

    // gets the number of cached entries
    // @return The cache size
    public static int size() {
        return CACHE.size();
    }
}
