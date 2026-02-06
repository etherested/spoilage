package etherested.spoilage.logic;

import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilableItemData;
import etherested.spoilage.data.SpoilageGroupData;
import etherested.spoilage.data.SpoilageGroupRegistry;
import etherested.spoilage.data.SpoilageItemRegistry;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** core calculation logic for spoilage mechanics */
public class SpoilageCalculator {

    /**
     * gets the initialized spoilage data from a stack, or null if not present/initialized;
     * this is a helper to consolidate the common null/initialized check pattern
     */
    @Nullable
    public static SpoilageData getInitializedData(ItemStack stack) {
        SpoilageData data = stack.get(ModDataComponents.SPOILAGE_DATA.get());
        if (data == null || !data.isInitialized()) {
            return null;
        }
        return data;
    }

    /** gets the spoilage percentage (0.0 = fresh, 1.0 = rotten) */
    public static float getSpoilagePercent(ItemStack stack, long worldTime) {
        if (!isSpoilable(stack)) {
            return 0.0f;
        }

        SpoilageData data = getInitializedData(stack);
        if (data == null) {
            return 0.0f;
        }

        long lifetime = getLifetime(stack);
        if (lifetime <= 0) {
            return 0.0f;
        }

        long remaining = getRemainingTicks(stack, worldTime);
        if (remaining <= 0) {
            return 1.0f;
        }

        return 1.0f - ((float) remaining / lifetime);
    }

    /** gets the remaining ticks until fully spoiled */
    public static long getRemainingTicks(ItemStack stack, long worldTime) {
        if (!isSpoilable(stack)) {
            return Long.MAX_VALUE;
        }

        SpoilageData data = getInitializedData(stack);
        if (data == null) {
            return Long.MAX_VALUE;
        }

        if (data.isPaused()) {
            return data.remainingLifetime();
        }

        long lifetime = getLifetime(stack);
        long elapsed = (long) ((worldTime - data.creationTime()) * data.preservationMultiplier() * SpoilageConfig.getGlobalSpeedMultiplier());
        // subtract y-level savings from effective elapsed time
        long effectiveElapsed = elapsed - data.yLevelSavedTicks();
        return Math.max(0, lifetime - effectiveElapsed);
    }

    /**
     * gets the remaining ticks for display purposes (tooltips);
     * calculates prospective savings to prevent timer rollback between processing ticks;
     * shows real wall-clock time until spoiled at current preservation rate
     */
    public static long getRemainingTicksForDisplay(ItemStack stack, long worldTime) {
        if (!isSpoilable(stack)) {
            return Long.MAX_VALUE;
        }

        SpoilageData data = getInitializedData(stack);
        if (data == null) {
            return Long.MAX_VALUE;
        }

        if (data.isPaused()) {
            return data.remainingLifetime();
        }

        long lifetime = getLifetime(stack);
        long elapsed = (long) ((worldTime - data.creationTime()) * data.preservationMultiplier() * SpoilageConfig.getGlobalSpeedMultiplier());

        // check if item is currently in a y-level container (recent processing)
        float yMultiplier = data.currentContainerYMultiplier();
        long ticksSinceProcess = worldTime - data.lastYLevelProcessTick();
        int checkInterval = SpoilageConfig.getCheckIntervalTicks();

        // only consider in container if recently processed (within 2x check interval)
        boolean inYLevelContainer = yMultiplier < 1.0f
                && data.lastYLevelProcessTick() > 0
                && ticksSinceProcess < checkInterval * 2;

        long totalSavings = data.yLevelSavedTicks();

        if (inYLevelContainer) {
            // calculate prospective savings since last processing
            // this prevents timer rollback between periodic updates
            long prospectiveSavings = (long) (ticksSinceProcess * (1.0f - yMultiplier));
            totalSavings += prospectiveSavings;
        }

        // calculate remaining lifetime ticks
        long effectiveElapsed = elapsed - totalSavings;
        long remainingTicks = Math.max(0, lifetime - effectiveElapsed);

        if (inYLevelContainer) {
            // show real wall-clock time until spoiled at current preservation rate
            remainingTicks = (long) (remainingTicks / yMultiplier);
        }

        return remainingTicks;
    }

    /** gets the total lifetime for an item based on its spoilage group */
    public static long getLifetime(ItemStack stack) {
        SpoilableItemData itemData = getSpoilableData(stack);
        if (itemData == null) {
            return 0;
        }

        SpoilageGroupData groupData = SpoilageGroupRegistry.getGroup(itemData.spoilageGroup());
        if (groupData == null) {
            return SpoilageGroupData.DEFAULT_LIFETIME;
        }

        return itemData.getLifetime(groupData);
    }

    /** checks if an item is spoilable via datapack configuration */
    public static boolean isSpoilable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return getSpoilableData(stack) != null;
    }

    /** gets the spoilable data for an item from the spoilage registry */
    @Nullable
    public static SpoilableItemData getSpoilableData(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return SpoilageItemRegistry.getData(stack.getItem());
    }

    /** gets the spoilage group data for an item */
    @Nullable
    public static SpoilageGroupData getGroupData(ItemStack stack) {
        SpoilableItemData itemData = getSpoilableData(stack);
        if (itemData == null) {
            return null;
        }
        return SpoilageGroupRegistry.getGroup(itemData.spoilageGroup());
    }

    /** calculates weighted average spoilage when combining stacks or crafting */
    public static float calculateWeightedAverageSpoilage(ItemStack[] ingredients, long worldTime) {
        float totalWeight = 0;
        float totalSpoilage = 0;

        for (ItemStack stack : ingredients) {
            if (!stack.isEmpty() && isSpoilable(stack)) {
                int count = stack.getCount();
                float spoilage = getSpoilagePercent(stack, worldTime);
                totalWeight += count;
                totalSpoilage += spoilage * count;
            }
        }

        if (totalWeight <= 0) {
            return 0.0f;
        }

        return totalSpoilage / totalWeight;
    }

    /** initializes spoilage data on an item stack */
    public static void initializeSpoilage(ItemStack stack, long worldTime) {
        if (!isSpoilable(stack)) {
            return;
        }

        SpoilageData existing = stack.get(ModDataComponents.SPOILAGE_DATA.get());
        if (existing != null && existing.isInitialized()) {
            return;
        }

        stack.set(ModDataComponents.SPOILAGE_DATA.get(), SpoilageData.DEFAULT.initialize(worldTime));
    }

    /** initializes spoilage with a specific starting percentage (for loot tables) */
    public static void initializeSpoilageWithPercent(ItemStack stack, long worldTime, float spoilagePercent) {
        if (!isSpoilable(stack)) {
            return;
        }

        long lifetime = getLifetime(stack);
        long elapsed = (long) (lifetime * spoilagePercent);
        long adjustedCreation = worldTime - elapsed;

        stack.set(ModDataComponents.SPOILAGE_DATA.get(), new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f));
    }

    /**
     * gets the current preservation rate for display purposes;
     * returns 1.0 if no preservation, < 1.0 if preserved (e.g. 0.5 = 50% slower)
     */
    public static float getPreservationRateForDisplay(ItemStack stack) {
        SpoilageData data = getInitializedData(stack);
        if (data != null && data.preservationMultiplier() != 1.0f) {
            return data.preservationMultiplier();
        }
        return 1.0f;
    }

    /** creates spoilage data from weighted average (for crafting results) */
    public static void applyCraftedSpoilage(ItemStack result, ItemStack[] ingredients, long worldTime) {
        if (!isSpoilable(result)) {
            return;
        }

        float avgSpoilage = calculateWeightedAverageSpoilage(ingredients, worldTime);
        initializeSpoilageWithPercent(result, worldTime, avgSpoilage);
    }

    /** calculates weighted average spoilage when merging two stacks */
    public static SpoilageData mergeStacks(ItemStack existing, ItemStack incoming, long worldTime) {
        if (!isSpoilable(existing) || !isSpoilable(incoming)) {
            return SpoilageData.DEFAULT;
        }

        float existingSpoilage = getSpoilagePercent(existing, worldTime);
        float incomingSpoilage = getSpoilagePercent(incoming, worldTime);

        int existingCount = existing.getCount();
        int incomingCount = incoming.getCount();
        int totalCount = existingCount + incomingCount;

        float weightedSpoilage = (existingSpoilage * existingCount + incomingSpoilage * incomingCount) / totalCount;

        long lifetime = getLifetime(existing);
        long elapsed = (long) (lifetime * weightedSpoilage);
        long adjustedCreation = worldTime - elapsed;

        SpoilageData existingData = existing.get(ModDataComponents.SPOILAGE_DATA.get());
        float preservationMult = existingData != null ? existingData.preservationMultiplier() : 1.0f;

        return new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, preservationMult, 0L, 0L, 1.0f, 1.0f);
    }
}
