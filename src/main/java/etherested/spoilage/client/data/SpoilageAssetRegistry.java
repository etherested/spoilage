package etherested.spoilage.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.LoggerFactory;
import etherested.spoilage.Spoilage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

// client-side registry for spoilage asset data (textures);
// each item has its own file: assets/<namespace>/spoilage/<item_path>.json;
// for example:
//  - assets/minecraft/spoilage/potato.json
//  - assets/minecraft/spoilage/apple.json
//  - assets/mymod/spoilage/custom_food.json
// this allows resource packs to easily override item textures without affecting gameplay data
public class SpoilageAssetRegistry extends SimpleJsonResourceReloadListener
    //? if fabric {
    /*implements net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
    *///?}
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilageAssetRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "spoilage";

    // map from item registry ID to asset data
    private static final Map<ResourceLocation, SpoilageAssetItemData> ASSETS = new HashMap<>();

    public SpoilageAssetRegistry() {
        super(GSON, DIRECTORY);
    }

    //? if fabric {
    /*@Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(Spoilage.MODID, "spoilage_assets");
    }
    *///?}

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        ASSETS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            // skip group files (they're in spoilage/groups/ subdirectory)
            if (fileId.getPath().startsWith("groups/")) {
                continue;
            }

            // the file path becomes the item ID
            // e.g., file at assets/minecraft/spoilage/potato.json -> minecraft:potato
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), fileId.getPath());

            // verify the item exists
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                LOGGER.debug("Spoilage asset data for unknown item: {} (file: {})", itemId, fileId);
                continue;
            }

            try {
                // use fromJson for flexible key name support
                SpoilageAssetItemData data = SpoilageAssetItemData.fromJson(entry.getValue());

                // only store if there's actually texture data (item or block)
                if (data.hasSpoilageTextures() || data.hasTextureStages() || data.hasBlockSpoilageTextures() || !data.getAllTextureStages().isEmpty()) {
                    ASSETS.put(itemId, data);
                    LOGGER.debug("Loaded spoilage asset data for item: {} with {} texture stages", itemId, data.getAllTextureStages().size());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load spoilage asset data for {}: {}", itemId, e.getMessage());
            }
        }

        LOGGER.info("Loaded spoilage asset data for {} items", ASSETS.size());
    }

    // gets the spoilage asset data for an item by its registry ID
    @Nullable
    public static SpoilageAssetItemData getData(ResourceLocation itemId) {
        return ASSETS.get(itemId);
    }

    // gets the spoilage asset data for an item
    @Nullable
    public static SpoilageAssetItemData getData(Item item) {
        return getData(BuiltInRegistries.ITEM.getKey(item));
    }

    // checks if an item has spoilage asset data defined
    public static boolean hasAssetData(ResourceLocation itemId) {
        return ASSETS.containsKey(itemId);
    }

    // checks if an item has spoilage asset data defined
    public static boolean hasAssetData(Item item) {
        return hasAssetData(BuiltInRegistries.ITEM.getKey(item));
    }

    // gets all registered spoilage asset data
    public static Map<ResourceLocation, SpoilageAssetItemData> getAllData() {
        return Map.copyOf(ASSETS);
    }

    // clears all cached asset data
    public static void clear() {
        ASSETS.clear();
    }
}
