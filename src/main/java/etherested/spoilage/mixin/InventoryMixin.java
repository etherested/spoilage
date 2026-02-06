package etherested.spoilage.mixin;

import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * mixin to auto-combine food items with same food in offhand;
 * uses weighted spoilage average when combining
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow @Final public Player player;

    @Shadow public abstract ItemStack getItem(int slot);

    @Shadow public abstract void setItem(int slot, ItemStack stack);

    /** intercepts item pickup to potentially merge with offhand */
    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void spoilage$autoMergeOffhand(ItemStack incoming, CallbackInfoReturnable<Boolean> cir) {
        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.isOffhandAutoCombineEnabled()) {
            return;
        }

        if (incoming.isEmpty() || !SpoilageCalculator.isSpoilable(incoming)) {
            return;
        }

        // don't combine inedible items (100% spoiled)
        long worldTime = player.level().getGameTime();
        float incomingSpoilage = SpoilageCalculator.getSpoilagePercent(incoming, worldTime);
        if (incomingSpoilage >= 1.0f) {
            return;
        }

        ItemStack offhand = getItem(Inventory.SLOT_OFFHAND);
        if (offhand.isEmpty()) {
            return;
        }

        // check if same item type and both spoilable
        if (!ItemStack.isSameItem(offhand, incoming)) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(offhand)) {
            return;
        }

        // don't combine with inedible items in offhand
        float offhandSpoilage = SpoilageCalculator.getSpoilagePercent(offhand, worldTime);
        if (offhandSpoilage >= 1.0f) {
            return;
        }

        // check stack size limits
        int maxStackSize = offhand.getMaxStackSize();
        int currentSize = offhand.getCount();
        int incomingSize = incoming.getCount();

        if (currentSize >= maxStackSize) {
            return; // Offhand is full
        }

        // calculate how many items we can merge
        int spaceAvailable = maxStackSize - currentSize;
        int toMerge = Math.min(spaceAvailable, incomingSize);

        if (toMerge <= 0) {
            return;
        }

        // calculate weighted average spoilage
        SpoilageData mergedData = calculateMergedSpoilage(offhand, incoming, toMerge, worldTime);

        // update offhand stack
        offhand.setCount(currentSize + toMerge);
        offhand.set(ModDataComponents.SPOILAGE_DATA.get(), mergedData);

        // update incoming stack
        if (toMerge >= incomingSize) {
            // all items merged, mark as empty to signal success
            incoming.setCount(0);
            cir.setReturnValue(true);
        } else {
            // partial merge, reduce incoming stack
            incoming.setCount(incomingSize - toMerge);
            // don't cancel - let vanilla handle the remaining items
        }
    }

    /** calculates merged spoilage data using weighted average */
    private SpoilageData calculateMergedSpoilage(ItemStack existing, ItemStack incoming, int incomingCount, long worldTime) {
        float existingSpoilage = SpoilageCalculator.getSpoilagePercent(existing, worldTime);
        float incomingSpoilage = SpoilageCalculator.getSpoilagePercent(incoming, worldTime);

        int existingCount = existing.getCount();
        int totalCount = existingCount + incomingCount;

        float weightedSpoilage = (existingSpoilage * existingCount + incomingSpoilage * incomingCount) / totalCount;

        long lifetime = SpoilageCalculator.getLifetime(existing);
        long elapsed = (long) (lifetime * weightedSpoilage);
        long adjustedCreation = worldTime - elapsed;

        SpoilageData existingData = existing.get(ModDataComponents.SPOILAGE_DATA.get());
        float preservationMult = existingData != null ? existingData.preservationMultiplier() : 1.0f;

        return new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, preservationMult, 0L, 0L, 1.0f, 1.0f);
    }
}
