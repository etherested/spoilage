package etherested.spoilage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.LoggerFactory;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * registry for spoilable item and block data loaded from individual JSON files;
 * structure (unified):
 * - data/<namespace>/spoilage/<name>.json
 * - data/<namespace>/spoilage/groups/<group_name>.json
 * for example:
 * - data/minecraft/spoilage/potato.json
 * - data/minecraft/spoilage/apple.json
 * - data/minecraft/spoilage/cake.json (can define both item and block spoilage)
 * - data/mymod/spoilage/custom_food.json
 * each file can define spoilage for an item, a block, or both;
 * set "is_block": true to also register the data for the block form
 */
public class SpoilageItemRegistry extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilageItemRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "spoilage";

    // map from item registry ID to spoilage data
    private static final Map<ResourceLocation, SpoilableItemData> ITEMS = new HashMap<>();

    // map from block registry ID to spoilage data
    private static final Map<ResourceLocation, SpoilableItemData> BLOCKS = new HashMap<>();

    public SpoilageItemRegistry() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        ITEMS.clear();
        BLOCKS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();

            // skip group files (they're in spoilage/groups/ subdirectory)
            if (path.startsWith("groups/")) {
                continue;
            }

            try {
                SpoilableItemData data = SpoilableItemData.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(error -> new IllegalStateException("Failed to parse spoilage data: " + error));

                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), path);

                // register as item if it exists
                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    registerAsItem(id, data);
                }

                // register as block if is_block is true and block exists
                if (data.isBlock() && BuiltInRegistries.BLOCK.containsKey(id)) {
                    registerAsBlock(id, data);
                }
                // also auto-detect: if ID matches a block but not an item, register as block
                else if (!BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.BLOCK.containsKey(id)) {
                    registerAsBlock(id, data);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load spoilage data for {}: {}", fileId, e.getMessage());
            }
        }

        LOGGER.info("Loaded spoilage data for {} items and {} blocks", ITEMS.size(), BLOCKS.size());
    }

    private void registerAsItem(ResourceLocation itemId, SpoilableItemData data) {
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            LOGGER.warn("Spoilage data for unknown item: {}", itemId);
            return;
        }
        ITEMS.put(itemId, data);
        LOGGER.debug("Loaded spoilage data for item: {}", itemId);
    }

    private void registerAsBlock(ResourceLocation blockId, SpoilableItemData data) {
        if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
            LOGGER.warn("Spoilage data for unknown block: {}", blockId);
            return;
        }
        BLOCKS.put(blockId, data);
        LOGGER.debug("Loaded spoilage data for block: {}", blockId);
    }

    /** gets the spoilage data for an item by its registry ID */
    @Nullable
    public static SpoilableItemData getData(ResourceLocation itemId) {
        return ITEMS.get(itemId);
    }

    /** gets the spoilage data for an item */
    @Nullable
    public static SpoilableItemData getData(Item item) {
        return getData(BuiltInRegistries.ITEM.getKey(item));
    }

    /** checks if an item has spoilage data defined */
    public static boolean isSpoilable(ResourceLocation itemId) {
        return ITEMS.containsKey(itemId);
    }

    /** checks if an item has spoilage data defined */
    public static boolean isSpoilable(Item item) {
        return isSpoilable(BuiltInRegistries.ITEM.getKey(item));
    }

    /** gets all registered item spoilage data */
    public static Map<ResourceLocation, SpoilableItemData> getAllData() {
        return Map.copyOf(ITEMS);
    }

    /** gets the spoilage data for a block by its registry ID */
    @Nullable
    public static SpoilableItemData getBlockData(ResourceLocation blockId) {
        return BLOCKS.get(blockId);
    }

    /** gets the spoilage data for a block */
    @Nullable
    public static SpoilableItemData getBlockData(Block block) {
        return getBlockData(BuiltInRegistries.BLOCK.getKey(block));
    }

    /** checks if a block has spoilage data defined */
    public static boolean isBlockSpoilable(ResourceLocation blockId) {
        return BLOCKS.containsKey(blockId);
    }

    /** checks if a block has spoilage data defined */
    public static boolean isBlockSpoilable(Block block) {
        return isBlockSpoilable(BuiltInRegistries.BLOCK.getKey(block));
    }

    /** gets all registered block spoilage data */
    public static Map<ResourceLocation, SpoilableItemData> getAllBlockData() {
        return Map.copyOf(BLOCKS);
    }

    /**
     * gets the linked item for a block (used for block→item spoilage transfer);
     * checks if an item with the same registry ID exists and is spoilable
     * @param blockId the block registry ID
     * @return the item registry ID if a matching spoilable item exists, null otherwise
     */
    @Nullable
    public static ResourceLocation getLinkedItem(ResourceLocation blockId) {
        // check if item with same ID exists and is spoilable
        if (ITEMS.containsKey(blockId)) {
            return blockId;
        }
        return null;
    }

    /**
     * gets the linked item for a block
     * @param block the block
     * @return the item registry ID if a matching spoilable item exists, null otherwise
     */
    @Nullable
    public static ResourceLocation getLinkedItem(Block block) {
        return getLinkedItem(BuiltInRegistries.BLOCK.getKey(block));
    }
}
