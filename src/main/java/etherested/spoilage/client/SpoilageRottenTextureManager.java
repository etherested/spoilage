package etherested.spoilage.client;

import org.slf4j.LoggerFactory;
import etherested.spoilage.Spoilage;
import etherested.spoilage.client.data.SpoilageAssetItemData;
import etherested.spoilage.client.data.SpoilageAssetRegistry;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageTextureStage;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * unified manager for spoilage texture models;
 * handles texture stages with smooth blending transitions;
 * supports explicit model definitions via resource packs;
 * models from any namespace (spoilage, Minecraft, other mods)
 */
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpoilageRottenTextureManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilageRottenTextureManager.class);

    // models explicitly registered for loading
    private static final Set<ResourceLocation> MODELS_TO_REGISTER = new HashSet<>();

    // cache of loaded baked models (model location -> baked model)
    private static final Map<ResourceLocation, BakedModel> bakedModels = new HashMap<>();

    // items confirmed to have no spoilage texture (negative cache)
    private static final Set<ResourceLocation> noTextureItems = new HashSet<>();

    static {
        // register built-in spoilage models
        // models in subfolders need explicit registration to be loaded
        registerBuiltInModels();
    }

    /**
     * registers built-in spoilage models from the mod's assets;
     * these use the subfolder pattern: item/<stage>/<item>
     */
    private static void registerBuiltInModels() {
        // stale item textures
        MODELS_TO_REGISTER.add(ResourceLocation.fromNamespaceAndPath("minecraft", "item/stale/apple"));
        MODELS_TO_REGISTER.add(ResourceLocation.fromNamespaceAndPath("minecraft", "item/stale/potato"));
        // rotten item textures
        MODELS_TO_REGISTER.add(ResourceLocation.fromNamespaceAndPath("minecraft", "item/rotten/apple"));
        MODELS_TO_REGISTER.add(ResourceLocation.fromNamespaceAndPath("minecraft", "item/rotten/potato"));

        // block textures - cake with all slice states (bites 0-6)
        // full cake (bites=0) uses "cake", sliced states use "cake_slice1" through "cake_slice6"
        MODELS_TO_REGISTER.add(ResourceLocation.fromNamespaceAndPath("minecraft", "block/rotten/cake"));
        for (int i = 1; i <= 6; i++) {
            MODELS_TO_REGISTER.add(ResourceLocation.fromNamespaceAndPath("minecraft", "block/rotten/cake_slice" + i));
        }
    }

    /** registers additional models needed during model loading */
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation model : MODELS_TO_REGISTER) {
            event.register(ModelResourceLocation.standalone(model));
            LOGGER.debug("Registered spoilage model: {}", model);
        }
    }

    /** caches explicitly registered models after baking is complete */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        bakedModels.clear();
        noTextureItems.clear();

        // cache explicitly registered models
        for (ResourceLocation modelLoc : MODELS_TO_REGISTER) {
            ModelResourceLocation mrl = ModelResourceLocation.standalone(modelLoc);
            BakedModel model = event.getModels().get(mrl);
            if (model != null) {
                bakedModels.put(modelLoc, model);
                LOGGER.debug("Cached spoilage model: {}", modelLoc);
            } else {
                LOGGER.warn("Failed to find registered spoilage model: {}", modelLoc);
            }
        }

        LOGGER.info("Spoilage texture manager initialized: {} explicit models", MODELS_TO_REGISTER.size());
    }

    /** checks if an item has any spoilage textures available */
    public static boolean hasSpoilageTextures(ItemStack stack) {
        if (!SpoilageConfig.useTextureBlending()) {
            return false;
        }

        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (noTextureItems.contains(itemId)) {
            return false;
        }

        // check client-side asset definitions
        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(itemId);
        if (assetData != null && assetData.hasSpoilageTextures()) {
            return true;
        }

        noTextureItems.add(itemId);
        return false;
    }

    /**
     * gets the stale texture stage data for an item;
     * checks flexible key names - any key containing "stale" and "item"
     */
    @Nullable
    public static SpoilageTextureStage getStaleTextureData(ItemStack stack) {
        if (!SpoilageConfig.useTextureBlending()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // check client-side asset definitions
        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(itemId);
        if (assetData != null) {
            if (assetData.hasStaleTexture()) {
                return assetData.getStaleItemTexture().orElse(null);
            }
            // check flexible keys - any key containing "stale" and "item" (or just "stale" without "block")
            SpoilageTextureStage flexibleStage = findItemStageByPattern(assetData, "stale", 0.3f);
            if (flexibleStage != null) {
                return flexibleStage;
            }
        }

        return null;
    }

    /**
     * gets the rotten texture stage data for an item;
     * checks flexible key names - any key containing "rotten" and "item"
     */
    @Nullable
    public static SpoilageTextureStage getRottenTextureData(ItemStack stack) {
        if (!SpoilageConfig.useTextureBlending()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // check client-side asset definitions
        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(itemId);
        if (assetData != null) {
            if (assetData.hasRottenTexture()) {
                return assetData.getRottenItemTexture().orElse(null);
            }
            // check flexible keys - any key containing "rotten" and "item" (or just "rotten" without "block")
            SpoilageTextureStage flexibleStage = findItemStageByPattern(assetData, "rotten", 0.7f);
            if (flexibleStage != null) {
                return flexibleStage;
            }
        }

        return null;
    }

    /**
     * finds an item texture stage by pattern matching the key name or threshold;
     * supports flexible key names like "my_rotten_effect" or custom stage names;
     * excludes keys containing "block"
     */
    @Nullable
    private static SpoilageTextureStage findItemStageByPattern(SpoilageAssetItemData assetData, String pattern, float minThreshold) {
        Map<String, SpoilageTextureStage> allStages = assetData.getAllTextureStages();

        // first, look for keys containing the pattern (case-insensitive) but NOT containing "block"
        for (Map.Entry<String, SpoilageTextureStage> entry : allStages.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains(pattern.toLowerCase()) && !key.contains("block")) {
                return entry.getValue();
            }
        }

        // then, look for any stage with matching threshold range (excluding block keys)
        for (Map.Entry<String, SpoilageTextureStage> entry : allStages.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (!key.contains("block") && entry.getValue().startThreshold() >= minThreshold) {
                return entry.getValue();
            }
        }

        return null;
    }

    /** gets the baked model for a texture stage */
    @Nullable
    public static BakedModel getModel(ResourceLocation modelLocation) {
        return bakedModels.get(modelLocation);
    }

    /** gets the stale model for an item stack */
    @Nullable
    public static BakedModel getStaleModel(ItemStack stack) {
        SpoilageTextureStage data = getStaleTextureData(stack);
        return data != null ? getModel(data.model()) : null;
    }

    /** gets the rotten model for an item stack */
    @Nullable
    public static BakedModel getRottenModel(ItemStack stack) {
        SpoilageTextureStage data = getRottenTextureData(stack);
        return data != null ? getModel(data.model()) : null;
    }

    /** registers a model to be loaded */
    public static void registerModel(ResourceLocation modelLocation) {
        MODELS_TO_REGISTER.add(modelLocation);
    }

    /** gets the rotten texture data for a block */
    @Nullable
    public static SpoilageTextureStage getBlockRottenTextureData(ResourceLocation blockId) {
        if (!SpoilageConfig.useTextureBlending()) {
            return null;
        }

        // check client-side asset definitions
        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(blockId);
        if (assetData != null) {
            if (assetData.hasRottenBlockTexture()) {
                return assetData.rottenBlockTexture().orElse(null);
            }
            // check flexible keys - any key containing "rotten" or with threshold >= 0.7
            SpoilageTextureStage flexibleStage = findBlockStageByPattern(assetData, "rotten", 0.7f);
            if (flexibleStage != null) {
                return flexibleStage;
            }
        }

        return null;
    }

    /** gets the stale texture data for a block */
    @Nullable
    public static SpoilageTextureStage getBlockStaleTextureData(ResourceLocation blockId) {
        if (!SpoilageConfig.useTextureBlending()) {
            return null;
        }

        // check client-side asset definitions
        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(blockId);
        if (assetData != null) {
            if (assetData.hasStaleBlockTexture()) {
                return assetData.staleBlockTexture().orElse(null);
            }
            // check flexible keys - any key containing "stale" or with threshold in 0.3-0.6 range
            SpoilageTextureStage flexibleStage = findBlockStageByPattern(assetData, "stale", 0.3f);
            if (flexibleStage != null) {
                return flexibleStage;
            }
        }

        return null;
    }

    /**
     * finds a texture stage by pattern matching the key name or threshold;
     * supports flexible key names like "my_rotten_effect" or custom stage names
     */
    @Nullable
    private static SpoilageTextureStage findBlockStageByPattern(SpoilageAssetItemData assetData, String pattern, float minThreshold) {
        Map<String, SpoilageTextureStage> allStages = assetData.getAllTextureStages();

        // first, look for keys containing the pattern (case-insensitive)
        for (Map.Entry<String, SpoilageTextureStage> entry : allStages.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains(pattern.toLowerCase()) && key.contains("block")) {
                return entry.getValue();
            }
        }

        // then, look for any stage with matching threshold range
        for (SpoilageTextureStage stage : allStages.values()) {
            if (stage.startThreshold() >= minThreshold) {
                return stage;
            }
        }

        return null;
    }

    /**
     * gets all texture stages for a block (supports flexible key names);
     * returns stages sorted by start threshold
     */
    public static List<SpoilageTextureStage> getAllBlockTextureStages(ResourceLocation blockId) {
        List<SpoilageTextureStage> stages = new ArrayList<>();

        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(blockId);
        if (assetData != null) {
            stages.addAll(assetData.getAllTextureStages().values());
        }

        // sort by start threshold
        stages.sort((a, b) -> Float.compare(a.startThreshold(), b.startThreshold()));
        return stages;
    }

    /** checks if a block has any spoilage textures available */
    public static boolean hasBlockSpoilageTextures(ResourceLocation blockId) {
        if (!SpoilageConfig.useTextureBlending()) {
            return false;
        }

        // check client-side asset definitions
        SpoilageAssetItemData assetData = SpoilageAssetRegistry.getData(blockId);
        return assetData != null && assetData.hasBlockSpoilageTextures();
    }

    /** clears all caches */
    public static void clearCaches() {
        bakedModels.clear();
        noTextureItems.clear();
        SpoilageAssetRegistry.clear();
    }

    /**
     * gets the baked model for a texture stage based on block state;
     * if the texture stage has state-aware models, looks up the appropriate model for the state;
     * otherwise returns the default model
     * @param data the texture stage data
     * @param state the block state
     * @return the baked model for this state, or null if not found
     */
    @Nullable
    public static BakedModel getModelForState(SpoilageTextureStage data, BlockState state) {
        if (data == null) {
            return null;
        }
        ResourceLocation modelLoc = data.getModelForState(state);
        return getModel(modelLoc);
    }
}
