package etherested.spoilage.mixin;

import etherested.spoilage.FreshnessLevel;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * mixin to make villagers ignore spoiled food;
 * villagers will not pick up STALE or worse food
 * and will only accept FRESH (80-100%) items in trades
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {

    /** prevents villagers from wanting to pick up spoiled items */
    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void spoilage$rejectSpoiledItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.doVillagersIgnoreSpoiled()) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        // get villager's level to access world time
        Villager villager = (Villager) (Object) this;
        if (villager.level() == null) {
            return;
        }

        long worldTime = villager.level().getGameTime();
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);
        FreshnessLevel level = FreshnessLevel.fromSpoilage(spoilage);

        // reject STALE or worse (STALE, SPOILING, ROTTEN, INEDIBLE)
        if (level.ordinal() >= FreshnessLevel.STALE.ordinal()) {
            cir.setReturnValue(false);
        }
    }

}
