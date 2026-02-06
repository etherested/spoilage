package etherested.spoilage.mixin;

import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** mixin to apply spoilage transfer when taking crafted items */
@Mixin(ResultSlot.class)
public abstract class ResultSlotOnTakeMixin {

    @Shadow @Final private CraftingContainer craftSlots;

    /** apply spoilage to the crafted result when it's taken */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void spoilage$applySpoilageOnTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        long worldTime = player.level().getGameTime();

        // collect spoilable ingredients and apply weighted average
        int ingredientCount = 0;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack ingredient = craftSlots.getItem(i);
            if (!ingredient.isEmpty() && SpoilageCalculator.isSpoilable(ingredient)) {
                ingredientCount++;
            }
        }

        if (ingredientCount > 0) {
            ItemStack[] ingredients = new ItemStack[ingredientCount];
            int idx = 0;
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack ingredient = craftSlots.getItem(i);
                if (!ingredient.isEmpty() && SpoilageCalculator.isSpoilable(ingredient)) {
                    ingredients[idx++] = ingredient;
                }
            }
            SpoilageCalculator.applyCraftedSpoilage(stack, ingredients, worldTime);
        }
    }
}
