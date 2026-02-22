package etherested.spoilage.mixin;

import etherested.spoilage.advancement.ModTriggers;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import etherested.spoilage.logic.SpoilageEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// mixin to reduce food nutrition based on spoilage percentage;
// targets Player.eat() which calls FoodData.eat(FoodProperties)
@Mixin(Player.class)
public abstract class FoodDataMixin {

    @Redirect(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V")
    )
    private void spoilage$modifyFoodValues(FoodData foodData, FoodProperties original, Level level, ItemStack stack, FoodProperties props) {
        if (!SpoilageConfig.isEnabled() || !SpoilageCalculator.isSpoilable(stack)) {
            foodData.eat(original);
            return;
        }

        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, level.getGameTime());

        // reduce nutrition linearly with spoilage
        // at 0% spoilage: 100% nutrition
        // at 100% spoilage: 25% nutrition (still gives some food value)
        float freshness = 1.0f - spoilage;
        float nutritionMultiplier = 0.25f + (freshness * 0.75f);
        int newNutrition = Math.max(1, Math.round(original.nutrition() * nutritionMultiplier));

        // reduce saturation more aggressively than nutrition
        // at 0% spoilage: 100% saturation
        // at 100% spoilage: 10% saturation
        float saturationMultiplier = 0.1f + (freshness * 0.9f);
        float newSaturation = original.saturation() * saturationMultiplier;

        // create new FoodProperties with modified values
        FoodProperties modified = new FoodProperties(
                newNutrition,
                newSaturation,
                original.canAlwaysEat(),
                original.eatSeconds(),
                original.usingConvertsTo(),
                original.effects()
        );

        foodData.eat(modified);

        // apply graduated negative effects based on spoilage level
        Player player = (Player)(Object)this;
        SpoilageEffects.applySpoilageEffects(player, spoilage);

        // trigger achievement for eating fully spoiled food
        if (spoilage >= 1.0f && player instanceof ServerPlayer serverPlayer) {
            ModTriggers.eatInedible().trigger(serverPlayer);
        }
    }

}
