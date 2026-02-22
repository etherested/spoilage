package etherested.spoilage.mixin;

import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerClickMixin {
    @Shadow public abstract ItemStack getCarried();
    @Shadow public abstract void setCarried(ItemStack stack);

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void spoilage$onSlotClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) return;
        if (clickType != ClickType.PICKUP || button != 0) return;

        AbstractContainerMenu self = (AbstractContainerMenu)(Object)this;
        ItemStack carried = getCarried();

        if (slotId < 0 || slotId >= self.slots.size() || carried.isEmpty()) return;

        Slot slot = self.slots.get(slotId);
        ItemStack slotStack = slot.getItem();

        if (slotStack.isEmpty()) return;

        // both must be spoilable and same item type (but may have different spoilage)
        if (!SpoilageCalculator.isSpoilable(carried) || !SpoilageCalculator.isSpoilable(slotStack)) return;
        if (!ItemStack.isSameItem(carried, slotStack)) return;

        // skip if they would stack normally (same components)
        if (ItemStack.isSameItemSameComponents(carried, slotStack)) return;

        long worldTime = player.level().getGameTime();

        // prevent merging if either item is fully rotten (0% freshness)
        float carriedSpoilage = SpoilageCalculator.getSpoilagePercent(carried, worldTime);
        float slotSpoilage = SpoilageCalculator.getSpoilagePercent(slotStack, worldTime);
        if (carriedSpoilage >= 1.0f || slotSpoilage >= 1.0f) {
            return;
        }

        int maxStack = slotStack.getMaxStackSize();
        int totalCount = carried.getCount() + slotStack.getCount();

        // calculate merged spoilage data
        SpoilageData merged = SpoilageCalculator.mergeStacks(slotStack, carried, worldTime);

        if (totalCount <= maxStack) {
            // full merge
            slotStack.setCount(totalCount);
            slotStack.set(ModDataComponents.spoilageData(), merged);
            setCarried(ItemStack.EMPTY);
        } else {
            // partial merge - only the items that actually combine get merged spoilage
            // the slot gets merged spoilage for all items that end up there
            slotStack.setCount(maxStack);
            slotStack.set(ModDataComponents.spoilageData(), merged);

            // the carried items that DON'T merge keep their original spoilage
            carried.setCount(totalCount - maxStack);
            // don't modify carried's spoilage - it keeps its original value
        }

        slot.setChanged();
        ci.cancel();
    }
}
