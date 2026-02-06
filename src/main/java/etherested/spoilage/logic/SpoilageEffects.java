package etherested.spoilage.logic;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * unified spoilage effects for both items and blocks;
 * ensures consistent effect tiers across all food consumption methods
 */
public class SpoilageEffects {

    // tier thresholds
    private static final float THRESHOLD_STALE = 0.40f;
    private static final float THRESHOLD_SPOILING = 0.60f;
    private static final float THRESHOLD_ROTTEN = 0.80f;
    private static final float THRESHOLD_INEDIBLE = 1.0f;

    // inedible tier constants
    private static final int INEDIBLE_HUNGER_DURATION = 1200;     // 60 seconds
    private static final int INEDIBLE_HUNGER_AMPLIFIER = 2;       // Hunger III
    private static final int INEDIBLE_POISON_DURATION = 400;      // 20 seconds
    private static final int INEDIBLE_POISON_AMPLIFIER = 1;       // Poison II
    private static final int INEDIBLE_NAUSEA_DURATION = 400;      // 20 seconds
    private static final int INEDIBLE_WEAKNESS_DURATION = 300;    // 15 seconds
    private static final int INEDIBLE_WEAKNESS_AMPLIFIER = 1;     // Weakness II

    // rotten tier constants
    private static final int ROTTEN_HUNGER_DURATION = 600;    // 30 seconds
    private static final int ROTTEN_HUNGER_AMPLIFIER = 1;     // Hunger II
    private static final int ROTTEN_POISON_DURATION = 200;    // 10 seconds
    private static final int ROTTEN_POISON_AMPLIFIER = 0;     // Poison I
    private static final float ROTTEN_NAUSEA_CHANCE = 0.3f;
    private static final int ROTTEN_NAUSEA_DURATION = 300;    // 15 seconds

    // spoiling tier constants
    private static final float SPOILING_HUNGER_CHANCE = 0.5f;
    private static final int SPOILING_HUNGER_DURATION = 300;  // 15 seconds
    private static final int SPOILING_HUNGER_AMPLIFIER = 0;   // Hunger I
    private static final float SPOILING_POISON_CHANCE = 0.2f;
    private static final int SPOILING_POISON_DURATION = 100;  // 5 seconds
    private static final int SPOILING_POISON_AMPLIFIER = 0;   // Poison I
    private static final float SPOILING_NAUSEA_CHANCE = 0.1f;
    private static final int SPOILING_NAUSEA_DURATION = 150;  // 7.5 seconds

    // stale tier constants
    private static final float STALE_HUNGER_CHANCE = 0.2f;
    private static final int STALE_HUNGER_DURATION = 100;     // 5 seconds
    private static final int STALE_HUNGER_AMPLIFIER = 0;      // Hunger I
    private static final float STALE_WEAKNESS_CHANCE = 0.05f;
    private static final int STALE_WEAKNESS_DURATION = 200;   // 10 seconds
    private static final int STALE_WEAKNESS_AMPLIFIER = 0;    // Weakness I

    /**
     * applies graduated spoilage effects based on spoilage percentage;
     * used by both FoodDataMixin (items) and CakeBlockMixin (blocks);
     * effect tiers:
     *   - fresh/good (0-40%): no effects
     *   - stale (40-60%): 20% chance Hunger I (5s), 5% chance Weakness I (10s)
     *   - spoiling (60-80%): 50% Hunger I (15s), 20% Poison I (5s), 10% Nausea (7.5s)
     *   - rotten (80-99%): Hunger II (30s), Poison I (10s), 30% Nausea (15s)
     *   - inedible (100%): Hunger III (60s), Poison II (20s), Nausea (20s), Weakness II (15s) — all guaranteed
     */
    public static void applySpoilageEffects(Player player, float spoilage) {
        if (spoilage >= THRESHOLD_INEDIBLE) {
            // inedible tier (100%) — all effects guaranteed
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, INEDIBLE_HUNGER_DURATION, INEDIBLE_HUNGER_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, INEDIBLE_POISON_DURATION, INEDIBLE_POISON_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, INEDIBLE_NAUSEA_DURATION, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, INEDIBLE_WEAKNESS_DURATION, INEDIBLE_WEAKNESS_AMPLIFIER));
        } else if (spoilage >= THRESHOLD_ROTTEN) {
            // rotten tier (80-99%)
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ROTTEN_HUNGER_DURATION, ROTTEN_HUNGER_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, ROTTEN_POISON_DURATION, ROTTEN_POISON_AMPLIFIER));
            if (player.getRandom().nextFloat() < ROTTEN_NAUSEA_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ROTTEN_NAUSEA_DURATION, 0));
            }
        } else if (spoilage >= THRESHOLD_SPOILING) {
            // spoiling tier (60-80%)
            if (player.getRandom().nextFloat() < SPOILING_HUNGER_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, SPOILING_HUNGER_DURATION, SPOILING_HUNGER_AMPLIFIER));
            }
            if (player.getRandom().nextFloat() < SPOILING_POISON_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, SPOILING_POISON_DURATION, SPOILING_POISON_AMPLIFIER));
            }
            if (player.getRandom().nextFloat() < SPOILING_NAUSEA_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SPOILING_NAUSEA_DURATION, 0));
            }
        } else if (spoilage >= THRESHOLD_STALE) {
            // stale tier (40-60%)
            if (player.getRandom().nextFloat() < STALE_HUNGER_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, STALE_HUNGER_DURATION, STALE_HUNGER_AMPLIFIER));
            }
            if (player.getRandom().nextFloat() < STALE_WEAKNESS_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STALE_WEAKNESS_DURATION, STALE_WEAKNESS_AMPLIFIER));
            }
        }
        // fresh/good tier (0-40%): no effects
    }
}
