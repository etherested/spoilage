package etherested.spoilage.logic;

import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilableItemData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// processes spoilage periodically for player inventories and containers
public class SpoilageProcessor {

    // process spoilage for a player's inventory
    public static void processPlayerInventory(ServerPlayer player) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        Level level = player.level();
        long worldTime = level.getGameTime();
        Inventory inventory = player.getInventory();

        // count rotten slots for contamination penalty
        int rottenSlots = SpoilageCalculator.countRottenSlots(inventory, worldTime);
        float contaminationMultiplier = SpoilageCalculator.getContaminationMultiplier(rottenSlots);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !SpoilageCalculator.isSpoilable(stack)) {
                continue;
            }

            clearContainerYMultiplierIfNeeded(stack, worldTime);

            // apply contamination acceleration via negative savings
            if (contaminationMultiplier > 1.0f) {
                applyContaminationPenalty(stack, contaminationMultiplier, worldTime);
            }

            processStack(stack, worldTime, level);

            ItemStack replacement = checkRottenReplacement(stack, worldTime);
            if (replacement != null) {
                inventory.setItem(i, replacement);
            }
        }
    }

    // process spoilage for a container (inventory, chest, etc.)
    public static void processContainer(Container container, long worldTime, Level level) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !SpoilageCalculator.isSpoilable(stack)) {
                continue;
            }

            // reset Y-level multiplier if item has left a container
            // this fixes the timer display delay when moving items from containers to inventory
            clearContainerYMultiplierIfNeeded(stack, worldTime);
            processStack(stack, worldTime, level);

            // check for rotten replacement after processing
            ItemStack replacement = checkRottenReplacement(stack, worldTime);
            if (replacement != null) {
                container.setItem(i, replacement);
            }
        }
    }

    // clears the container Y-level multiplier if the item appears to have left a container;
    // this is detected by checking if the last Y-level processing was more than 2x the check interval ago
    private static void clearContainerYMultiplierIfNeeded(ItemStack stack, long worldTime) {
        SpoilageData data = SpoilageCalculator.getInitializedData(stack);
        if (data == null) {
            return;
        }

        // if multiplier is 1.0, no need to clear
        if (data.currentContainerYMultiplier() == 1.0f) {
            return;
        }

        // check if item hasn't been processed by ContainerSpoilageHandler recently
        long ticksSince = worldTime - data.lastYLevelProcessTick();
        if (ticksSince > SpoilageConfig.getCheckIntervalTicks() * 2) {
            // item has left the container - reset multiplier immediately
            stack.set(ModDataComponents.spoilageData(), data.clearContainerYMultiplier());
        }
    }

    // applies food contamination penalty as negative savings;
    // skips items already at rotten/inedible tier (80%+ spoilage)
    private static void applyContaminationPenalty(ItemStack stack, float rottenMultiplier, long worldTime) {
        SpoilageData data = SpoilageCalculator.getInitializedData(stack);
        if (data == null || data.isPaused()) return;

        // skip items already at rotten/inedible tier
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);
        if (spoilage >= 0.8f) return;

        // negative savings = checkInterval * (1.0 - multiplier) where multiplier > 1.0
        long negativeSavings = (long) (SpoilageConfig.getCheckIntervalTicks() * (1.0f - rottenMultiplier));

        stack.set(ModDataComponents.spoilageData(), new SpoilageData(
                data.creationTime(), data.remainingLifetime(), data.isPaused(),
                data.preservationMultiplier(),
                data.yLevelSavedTicks() + negativeSavings,
                data.lastYLevelProcessTick(),
                data.currentContainerYMultiplier(),
                data.biomeMultiplier()
        ));
    }

    // checks if a fully spoiled item should be replaced with another item
    // @param stack the item stack to check
    // @param worldTime current world time
    // @return the replacement ItemStack, or null if no replacement needed
    public static ItemStack checkRottenReplacement(ItemStack stack, long worldTime) {
        if (stack.isEmpty() || !SpoilageCalculator.isSpoilable(stack)) {
            return null;
        }

        // check if fully spoiled (100%)
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);
        if (spoilage < 1.0f) {
            return null;
        }

        // check if item has a rotten replacement defined
        SpoilableItemData itemData = SpoilageCalculator.getSpoilableData(stack);
        if (itemData == null || itemData.rottenReplacement().isEmpty()) {
            return null;
        }

        ResourceLocation replacementId = itemData.rottenReplacement().get();
        if (!BuiltInRegistries.ITEM.containsKey(replacementId)) {
            return null;
        }

        Item replacementItem = BuiltInRegistries.ITEM.get(replacementId);
        ItemStack replacement = new ItemStack(replacementItem, stack.getCount());

        // copy over NBT/components if the replacement is also spoilable, but start fresh
        // (don't copy spoilage data - the replacement starts in its natural state)
        return replacement;
    }

    // process a single item stack;
    // items stay at 100% spoilage when fully spoiled;
    // (no transformation - negative effects are applied when eaten instead)
    public static void processStack(ItemStack stack, long worldTime, Level level) {
        if (stack.isEmpty() || !SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        // initialize spoilage if not already done
        if (SpoilageCalculator.getInitializedData(stack) == null) {
            SpoilageCalculator.initializeSpoilage(stack, worldTime);
        }
    }

    // pause spoilage for items in a preservation container
    public static void pauseSpoilage(ItemStack stack, long worldTime) {
        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        SpoilageData data = SpoilageCalculator.getInitializedData(stack);
        if (data == null || data.isPaused()) {
            return;
        }

        long remaining = SpoilageCalculator.getRemainingTicks(stack, worldTime);
        stack.set(ModDataComponents.spoilageData(), data.pause(remaining));
    }

    // resume spoilage for items removed from preservation
    public static void resumeSpoilage(ItemStack stack, long worldTime) {
        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        SpoilageData data = stack.get(ModDataComponents.spoilageData());
        if (data == null || !data.isPaused()) {
            return;
        }

        long lifetime = SpoilageCalculator.getLifetime(stack);
        long remaining = data.remainingLifetime();
        long elapsed = lifetime - remaining;
        long newCreationTime = worldTime - (long)(elapsed / (data.preservationMultiplier() * SpoilageConfig.getGlobalSpeedMultiplier()));

        stack.set(ModDataComponents.spoilageData(), data.resume(newCreationTime));
    }

    // apply preservation multiplier to items in special containers
    public static void applyPreservationMultiplier(ItemStack stack, float multiplier, long worldTime) {
        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        SpoilageData data = stack.get(ModDataComponents.spoilageData());
        if (data == null) {
            SpoilageCalculator.initializeSpoilage(stack, worldTime);
            data = stack.get(ModDataComponents.spoilageData());
        }

        if (data != null && Math.abs(data.preservationMultiplier() - multiplier) > 0.001f) {
            // recalculate creation time based on current spoilage to account for multiplier change
            float currentSpoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);
            long lifetime = SpoilageCalculator.getLifetime(stack);
            long effectiveElapsed = (long)(lifetime * currentSpoilage);
            long newCreationTime = worldTime - (long)(effectiveElapsed / (multiplier * SpoilageConfig.getGlobalSpeedMultiplier()));

            stack.set(ModDataComponents.spoilageData(), new SpoilageData(
                    newCreationTime,
                    data.remainingLifetime(),
                    data.isPaused(),
                    multiplier,
                    data.yLevelSavedTicks(),
                    data.lastYLevelProcessTick(),
                    data.currentContainerYMultiplier(),
                    data.biomeMultiplier()
            ));
        }
    }

    // process all items for a player when they log in (resume paused spoilage)
    public static void onPlayerLogin(ServerPlayer player) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        long worldTime = player.level().getGameTime();
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && SpoilageCalculator.isSpoilable(stack)) {
                SpoilageData data = stack.get(ModDataComponents.spoilageData());
                if (data != null && data.isPaused()) {
                    resumeSpoilage(stack, worldTime);
                }
            }
        }
    }

    // process all items for a player when they log out (pause spoilage)
    public static void onPlayerLogout(ServerPlayer player) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        long worldTime = player.level().getGameTime();
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && SpoilageCalculator.isSpoilable(stack)) {
                pauseSpoilage(stack, worldTime);
            }
        }
    }
}
