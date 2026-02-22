package etherested.spoilage.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

// loot function that sets random spoilage on dropped items;
// use in loot tables to have spawned food start with some spoilage
public class SetSpoilageLootFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetSpoilageLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).and(instance.group(
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("min_spoilage", 0.0f).forGetter(f -> f.minSpoilage),
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("max_spoilage", 0.5f).forGetter(f -> f.maxSpoilage)
            )).apply(instance, SetSpoilageLootFunction::new));

    private final float minSpoilage;
    private final float maxSpoilage;

    protected SetSpoilageLootFunction(List<LootItemCondition> conditions, float minSpoilage, float maxSpoilage) {
        super(conditions);
        this.minSpoilage = Math.max(0.0f, Math.min(1.0f, minSpoilage));
        this.maxSpoilage = Math.max(this.minSpoilage, Math.min(1.0f, maxSpoilage));
    }

    @Override
    public LootItemFunctionType<SetSpoilageLootFunction> getType() {
        return ModLootFunctions.setSpoilage();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!SpoilageCalculator.isSpoilable(stack)) {
            return stack;
        }

        float spoilage;
        if (SpoilageConfig.isLootRandomizationEnabled()) {
            // random spoilage between min and max (per-item randomization)
            spoilage = minSpoilage + context.getRandom().nextFloat() * (maxSpoilage - minSpoilage);
        } else {
            // use the average of min and max when randomization is disabled
            spoilage = (minSpoilage + maxSpoilage) / 2.0f;
        }

        long worldTime = context.getLevel().getGameTime();
        SpoilageCalculator.initializeSpoilageWithPercent(stack, worldTime, spoilage);

        return stack;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<Builder> {
        private float minSpoilage = 0.0f;
        private float maxSpoilage = 0.5f;

        public Builder withRange(float min, float max) {
            this.minSpoilage = min;
            this.maxSpoilage = max;
            return this;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemConditionalFunction build() {
            return new SetSpoilageLootFunction(getConditions(), minSpoilage, maxSpoilage);
        }
    }
}
