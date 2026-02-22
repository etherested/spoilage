package etherested.spoilage.logic;

import org.slf4j.LoggerFactory;
import etherested.spoilage.config.SpoilageConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// manages per-container spoilage rate multipliers;
// parses config format "namespace:container;multiplier" and provides lookup
public class ContainerSpoilageRates {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerSpoilageRates.class);
    private static final Map<ResourceLocation, Float> containerRates = new HashMap<>();
    private static boolean initialized = false;
    private static long lastConfigCheck = 0;
    private static final long CONFIG_CHECK_INTERVAL = 100; // Check every 100 ticks

    // initializes or refreshes the container rates from config;
    // call this periodically or when config might have changed
    public static void refresh() {
        containerRates.clear();

        List<? extends String> rates = SpoilageConfig.getContainerSpoilageRates();
        for (String entry : rates) {
            parseEntry(entry);
        }

        initialized = true;
        LOGGER.debug("Loaded {} container spoilage rates", containerRates.size());
    }

    // parses a single config entry in format "namespace:container;multiplier"
    private static void parseEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return;
        }

        int semicolonIndex = entry.lastIndexOf(';');
        if (semicolonIndex == -1 || semicolonIndex == entry.length() - 1) {
            LOGGER.warn("Invalid container spoilage rate format (missing semicolon or multiplier): {}", entry);
            return;
        }

        String blockId = entry.substring(0, semicolonIndex).trim();
        String multiplierStr = entry.substring(semicolonIndex + 1).trim();

        try {
            float multiplier = Float.parseFloat(multiplierStr);
            if (multiplier < 0.0f || multiplier > 10.0f) {
                LOGGER.warn("Container spoilage rate multiplier out of range (0.0-10.0): {} for {}", multiplier, blockId);
                multiplier = Math.max(0.0f, Math.min(10.0f, multiplier));
            }

            ResourceLocation location = ResourceLocation.tryParse(blockId);
            if (location == null) {
                LOGGER.warn("Invalid block ID in container spoilage rates: {}", blockId);
                return;
            }

            containerRates.put(location, multiplier);
            LOGGER.debug("Registered container spoilage rate: {} = {}", location, multiplier);

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid multiplier value in container spoilage rates: {} for {}", multiplierStr, blockId);
        }
    }

    // gets the spoilage rate multiplier for a container at the given position;
    // returns 1.0 if no specific rate is configured
    // @param blockEntity the block entity to check
    // @return the spoilage multiplier (lower = slower spoilage)
    public static float getMultiplier(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return 1.0f;
        }

        ensureInitialized();

        Block block = blockEntity.getBlockState().getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        return containerRates.getOrDefault(blockId, 1.0f);
    }

    // gets the spoilage rate multiplier for a block by its ID;
    // returns 1.0 if no specific rate is configured
    // @param blockId the block's resource location
    // @return the spoilage multiplier (lower = slower spoilage)
    public static float getMultiplier(ResourceLocation blockId) {
        if (blockId == null) {
            return 1.0f;
        }

        ensureInitialized();

        return containerRates.getOrDefault(blockId, 1.0f);
    }

    // checks if a container has a custom spoilage rate defined
    public static boolean hasCustomRate(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }

        ensureInitialized();

        Block block = blockEntity.getBlockState().getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        return containerRates.containsKey(blockId);
    }

    // ensures the rates are initialized, refreshing from config if needed
    private static void ensureInitialized() {
        if (!initialized) {
            refresh();
        }
    }

    // checks and potentially refreshes config (call periodically from tick handlers)
    public static void checkForConfigRefresh(long worldTime) {
        if (worldTime - lastConfigCheck > CONFIG_CHECK_INTERVAL) {
            lastConfigCheck = worldTime;
            // reparse in case config was hot-reloaded
            refresh();
        }
    }

    // clears the cache and marks as uninitialized
    public static void invalidate() {
        containerRates.clear();
        initialized = false;
    }
}
