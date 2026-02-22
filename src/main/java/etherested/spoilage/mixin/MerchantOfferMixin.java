package etherested.spoilage.mixin;

import etherested.spoilage.FreshnessLevel;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mixin to make villagers only accept fresh items in trades;
// items must be FRESH (80-100% freshness) to be used in merchant trades
@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    // intercepts trade satisfaction check to reject non-fresh items
    @Inject(method = "satisfiedBy", at = @At("HEAD"), cancellable = true)
    private void spoilage$checkFreshness(ItemStack stackA, ItemStack stackB, CallbackInfoReturnable<Boolean> cir) {
        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.doVillagersIgnoreSpoiled()) {
            return;
        }

        // get world time - try client level first, then server
        long worldTime = 0;
        if (Minecraft.getInstance().level != null) {
            worldTime = Minecraft.getInstance().level.getGameTime();
        } else {
            // can't get world time, skip check
            return;
        }

        // check first trade slot
        if (!stackA.isEmpty() && SpoilageCalculator.isSpoilable(stackA)) {
            float spoilage = SpoilageCalculator.getSpoilagePercent(stackA, worldTime);
            FreshnessLevel level = FreshnessLevel.fromSpoilage(spoilage);
            // only accept FRESH items (80-100% freshness)
            if (level != FreshnessLevel.FRESH) {
                cir.setReturnValue(false);
                return;
            }
        }

        // check second trade slot
        if (!stackB.isEmpty() && SpoilageCalculator.isSpoilable(stackB)) {
            float spoilage = SpoilageCalculator.getSpoilagePercent(stackB, worldTime);
            FreshnessLevel level = FreshnessLevel.fromSpoilage(spoilage);
            // only accept FRESH items (80-100% freshness)
            if (level != FreshnessLevel.FRESH) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
