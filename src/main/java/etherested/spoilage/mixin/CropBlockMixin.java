package etherested.spoilage.mixin;

import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.ChunkSpoilageData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * mixin to handle crop lifecycle with the spoilage system;
 *   - recovery phase: stale seeds freeze growth until freshness fully recovers
 *   - growing phase: after recovery (or if seed is fresh), crops grow normally
 *   - fresh period: fully grown crops stay 100% fresh for configurable duration
 *   - rotting phase: after fresh period, crops slowly rot and regress through growth stages
 *   - inedible: at minimum stage, crops become inedible when harvested
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {

    @Shadow
    public abstract boolean isMaxAge(BlockState state);

    @Shadow
    public abstract int getMaxAge();

    @Shadow
    public abstract int getAge(BlockState state);

    @Shadow
    public abstract BlockState getStateForAge(int age);

    /**
     * forces fully grown crops to keep receiving random ticks;
     * vanilla returns false for max-age crops, which prevents the rot regression logic from running
     */
    @Inject(method = "isRandomlyTicking", at = @At("RETURN"), cancellable = true)
    private void spoilage$keepTickingMatureCrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && isMaxAge(state) && spoilage$isSpoilageEnabled()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * safely checks if spoilage is enabled;
     * isRandomlyTicking can be called before config is loaded during block registration,
     * so we default to true when config is unavailable
     */
    @Unique
    private static boolean spoilage$isSpoilageEnabled() {
        try {
            return SpoilageConfig.isEnabled();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /**
     * freezes crop growth while the seed's spoilage is still recovering;
     * when recovery completes, clears initialSpoilage and lets the tick proceed
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void spoilage$freezeRecoveringCrop(BlockState state, ServerLevel level, BlockPos pos,
                                                RandomSource random, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.isStaleSeedGrowthPenaltyEnabled()) {
            return;
        }

        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        if (entry == null || entry.type() != ChunkSpoilageData.BlockType.CROP || entry.initialSpoilage() <= 0) {
            return;
        }

        long worldTime = level.getGameTime();
        long recoveryPeriod = SpoilageConfig.getStaleSeedRecoveryTicks();
        float recovering = entry.getRecoveringSpoilage(worldTime, recoveryPeriod);

        if (recovering > 0) {
            // still recovering — block vanilla growth
            ci.cancel();
        } else {
            // recovery complete — clear initialSpoilage so this check won't fire again
            ChunkSpoilageCapability.updateCropSpoilage(level, pos, 0.0f);
        }
    }

    /** handles crop growth and rotting lifecycle */
    @Inject(method = "randomTick", at = @At("RETURN"))
    private void spoilage$onCropRandomTick(BlockState state, ServerLevel level, BlockPos pos,
                                            RandomSource random, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        // important: get the current state from the world, not the parameter
        // the parameter is the state before vanilla's randomTick ran
        // if vanilla grew the crop, we need to check the new state
        BlockState currentState = level.getBlockState(pos);
        spoilage$handleCropLifecycle(currentState, level, pos);
    }

    /** crops stay fresh when fully grown, then rot and regress through growth stages */
    @Unique
    private void spoilage$handleCropLifecycle(BlockState state, ServerLevel level, BlockPos pos) {
        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        int currentAge = getAge(state);
        int maxAge = getMaxAge();
        long worldTime = level.getGameTime();

        // check if crop just became fully grown
        if (isMaxAge(state)) {
            // only mark fully grown if age equals max age
            if (currentAge >= maxAge) {
                if (entry == null || !entry.isFullyGrown()) {
                    // mark the crop as fully grown, fresh timer starts now
                    // spoilage is reset to 0% (100% fresh) at maturity
                    ChunkSpoilageCapability.markCropFullyGrown(level, pos);
                    return;
                }
            }

            // crop is fully grown, check if it should regress due to rot
            long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
            long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
            float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

            if (rotProgress > 0) {
                // calculate target age based on rot progress
                // at 0% rot = max age, at 100% rot = stage 0
                int targetAge = maxAge - (int) (rotProgress * maxAge);
                targetAge = Math.max(0, targetAge);

                if (targetAge < currentAge) {
                    // regress the crop to the target age
                    level.setBlock(pos, getStateForAge(targetAge), Block.UPDATE_ALL);
                }
            }
            return;
        }

        // crop is not at max age, check if it's a rotting crop that regressed
        if (entry != null && entry.isFullyGrown()) {
            // only regress if crop actually has rot progress
            // this prevents premature rot for crops that were incorrectly marked as fully grown
            long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
            long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
            float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

            // only regress if already rotting, fresh crops at non-max age should not process
            if (rotProgress > 0) {
                int targetAge = maxAge - (int) (rotProgress * maxAge);
                targetAge = Math.max(0, targetAge);

                if (targetAge < currentAge) {
                    level.setBlock(pos, getStateForAge(targetAge), Block.UPDATE_ALL);
                }
            }
        }
        // if entry is null, the crop is still growing naturally
        // entry will be created when planted via BlockItemMixin
    }
}
