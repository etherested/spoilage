package etherested.spoilage.mixin;

import etherested.spoilage.FreshnessLevel;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.logic.SpoilageCalculator;
import etherested.spoilage.logic.SpoilageEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mixin to add spoilage to placed cakes;
// tracks cake placement time and applies spoilage effects when eaten
@Mixin(CakeBlock.class)
public abstract class CakeBlockMixin {

    // fallback lifetime if cake not in registry (3 in-game days = 3 * 24000 = 72000 ticks)
    @Unique
    private static final long FALLBACK_LIFETIME = 72000L;

    @Unique
    private static long spoilage$getCakeLifetime() {
        // Get lifetime from the spoilage registry for cake item
        ItemStack cakeStack = new ItemStack(Items.CAKE);
        long lifetime = SpoilageCalculator.getLifetime(cakeStack);
        return lifetime > 0 ? lifetime : FALLBACK_LIFETIME;
    }

    // intercepts cake eating to apply spoilage effects;
    // uses RETURN injection to ensure effects only apply when player actually eats
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void spoilage$checkCakeSpoilage(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hitResult,
                                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!SpoilageConfig.isEnabled() || level.isClientSide()) {
            return;
        }

        // only apply effects if the player actually ate the cake
        InteractionResult result = cir.getReturnValue();
        if (result != InteractionResult.SUCCESS && !result.consumesAction()) {
            return;
        }

        // spoilage data is initialized by CakePlacementHandler when the cake is placed
        // if somehow missing (e.g., cake placed before mod installed), initialize now
        if (!ChunkSpoilageCapability.hasBlockSpoilage(level, pos)) {
            ChunkSpoilageCapability.setBlockPlaced(level, pos);
            return; // don't apply effects on first bite if just initialized
        }

        // get current spoilage
        float spoilage = ChunkSpoilageCapability.getBlockSpoilagePercent(level, pos, spoilage$getCakeLifetime());
        FreshnessLevel freshnessLevel = FreshnessLevel.fromSpoilage(spoilage);

        // apply unified spoilage effects (same as items)
        SpoilageEffects.applySpoilageEffects(player, spoilage);

        // show message for rotten/inedible cake
        if (freshnessLevel == FreshnessLevel.ROTTEN || freshnessLevel == FreshnessLevel.INEDIBLE) {
            player.displayClientMessage(
                    Component.translatable("message.spoilage.ate_rotten_cake"),
                    true
            );
        }
    }

    // note: cleanup on block removal is handled by BlockSpoilageCleanupHandler
    // note: pick block spoilage preservation is handled by BlockPickMixin
}
