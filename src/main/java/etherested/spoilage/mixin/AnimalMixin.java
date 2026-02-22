package etherested.spoilage.mixin;

import etherested.spoilage.FreshnessLevel;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mixin to poison animals when fed rotten food
@Mixin(Animal.class)
public abstract class AnimalMixin {

    // applies poison effect when animals eat rotten food;
    // injects after the interaction but checks if feeding occurred
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void spoilage$checkFoodQuality(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.areAnimalsPoisonedByRotten()) {
            return;
        }

        // only process if interaction was successful (feeding occurred)
        if (cir.getReturnValue() != InteractionResult.SUCCESS && cir.getReturnValue() != InteractionResult.CONSUME) {
            return;
        }

        Animal animal = (Animal) (Object) this;
        ItemStack food = player.getItemInHand(hand);

        // check if the item was food for this animal
        if (!animal.isFood(food)) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(food)) {
            return;
        }

        if (animal.level() == null) {
            return;
        }

        long worldTime = animal.level().getGameTime();
        float spoilage = SpoilageCalculator.getSpoilagePercent(food, worldTime);
        FreshnessLevel level = FreshnessLevel.fromSpoilage(spoilage);

        // apply poison for ROTTEN or INEDIBLE food (80%+ spoilage)
        if (level == FreshnessLevel.ROTTEN || level == FreshnessLevel.INEDIBLE) {
            // Poison I for 10 seconds (200 ticks)
            animal.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        }
    }
}
