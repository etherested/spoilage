package etherested.spoilage.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * global loot modifier that applies random spoilage to all food items in loot tables;
 * respects the lootRandomizationEnabled config option
 */
public class SpoilageLootModifier extends LootModifier {
    public static final MapCodec<SpoilageLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).and(instance.group(
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("min_spoilage", 0.0f).forGetter(m -> m.minSpoilage),
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("max_spoilage", 0.5f).forGetter(m -> m.maxSpoilage)
            )).apply(instance, SpoilageLootModifier::new));

    private final float minSpoilage;
    private final float maxSpoilage;

    public SpoilageLootModifier(LootItemCondition[] conditions, float minSpoilage, float maxSpoilage) {
        super(conditions);
        this.minSpoilage = Math.max(0.0f, Math.min(1.0f, minSpoilage));
        this.maxSpoilage = Math.max(this.minSpoilage, Math.min(1.0f, maxSpoilage));
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!SpoilageConfig.isEnabled()) {
            return generatedLoot;
        }

        long worldTime = context.getLevel().getGameTime();

        for (ItemStack stack : generatedLoot) {
            if (stack.isEmpty() || !SpoilageCalculator.isSpoilable(stack)) {
                continue;
            }

            // skip if spoilage was already initialized (e.g., by BlockSpoilageCleanupHandler for crops)
            if (SpoilageCalculator.getInitializedData(stack) != null) {
                continue;
            }

            float spoilage;
            if (SpoilageConfig.isLootRandomizationEnabled()) {
                // random spoilage between min and max (per-item randomization)
                spoilage = minSpoilage + context.getRandom().nextFloat() * (maxSpoilage - minSpoilage);
            } else {
                // use the average of min and max when randomization is disabled
                spoilage = (minSpoilage + maxSpoilage) / 2.0f;
            }

            SpoilageCalculator.initializeSpoilageWithPercent(stack, worldTime, spoilage);
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
