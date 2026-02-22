package etherested.spoilage.mixin;

import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mixin to prevent crafting with fully rotten ingredients,
// we mixin to Slot and check if it's a ResultSlot since mayPickup is defined in Slot
@Mixin(Slot.class)
public abstract class ResultSlotMixin {

    // prevents taking the crafting result if any spoilable ingredient is fully rotten
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void spoilage$preventRottenCraft(Player player, CallbackInfoReturnable<Boolean> cir) {
        // only apply to ResultSlot (crafting results)
        if (!((Object)this instanceof ResultSlot resultSlot)) {
            return;
        }

        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        // access craftSlots via reflection or accessor
        CraftingContainer craftSlots = ((ResultSlotAccessor) resultSlot).getCraftSlots();
        if (craftSlots == null) {
            return;
        }

        long worldTime = player.level().getGameTime();

        // check all ingredients in the crafting grid
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack ingredient = craftSlots.getItem(i);
            if (!ingredient.isEmpty() && SpoilageCalculator.isSpoilable(ingredient)) {
                float spoilage = SpoilageCalculator.getSpoilagePercent(ingredient, worldTime);
                if (spoilage >= 1.0f) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}
