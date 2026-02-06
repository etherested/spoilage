package etherested.spoilage.mixin;

import etherested.spoilage.FreshnessLevel;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * mixin to prevent planting spoiled seeds and crops,
 * and to track seed spoilage when planting
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Shadow
    public abstract Block getBlock();

    /**
     * ThreadLocal to store seed spoilage between HEAD and RETURN injections;
     * needed because the stack becomes empty after placement,
     * so we can't read spoilage in the RETURN injection
     */
    @Unique
    private static final ThreadLocal<Float> spoilage$capturedSeedSpoilage = ThreadLocal.withInitial(() -> -1.0f);

    /** prevents planting spoiled items and captures seed spoilage for later use */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void spoilage$preventSpoiledPlanting(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // reset captured spoilage at the start of each useOn call
        spoilage$capturedSeedSpoilage.set(-1.0f);

        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        // check if this is a plantable block (crop, stem, sapling)
        Block block = getBlock();
        if (!(block instanceof CropBlock) && !(block instanceof StemBlock) && !(block instanceof SaplingBlock)) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        Level level = context.getLevel();
        if (level == null) {
            return;
        }

        long worldTime = level.getGameTime();
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);

        // capture the spoilage for use in RETURN injection (before stack becomes empty)
        if (block instanceof CropBlock) {
            spoilage$capturedSeedSpoilage.set(spoilage);
        }

        // check if planting would actually succeed before showing error message
        if (SpoilageConfig.isPreventPlantingSpoiledEnabled()) {
            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            BlockPos placePos = placeContext.getClickedPos();
            BlockState stateToPlace = block.getStateForPlacement(placeContext);

            // if placement wouldn't work anyway, don't show our message
            if (stateToPlace == null || !stateToPlace.canSurvive(level, placePos)) {
                return;
            }

            FreshnessLevel freshnessLevel = FreshnessLevel.fromSpoilage(spoilage);

            // prevent planting SPOILING or worse (60%+ spoilage)
            if (freshnessLevel.ordinal() >= FreshnessLevel.SPOILING.ordinal()) {
                // display message to player
                if (context.getPlayer() != null && !level.isClientSide()) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("message.spoilage.cannot_plant_spoiled"),
                            true
                    );
                }
                spoilage$capturedSeedSpoilage.set(-1.0f);
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }

    /**
     * stores seed spoilage when planting;
     * seed spoilage is used for immature harvest, reset to fresh at maturity
     */
    @Inject(method = "useOn", at = @At("RETURN"))
    private void spoilage$storeSeedSpoilage(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        // only process successful placements
        if (cir.getReturnValue() != InteractionResult.SUCCESS && cir.getReturnValue() != InteractionResult.CONSUME) {
            spoilage$capturedSeedSpoilage.set(-1.0f);
            return;
        }

        // check if this is a crop block
        Block block = getBlock();
        if (!(block instanceof CropBlock)) {
            spoilage$capturedSeedSpoilage.set(-1.0f);
            return;
        }

        // get the captured spoilage from HEAD injection
        float seedSpoilage = spoilage$capturedSeedSpoilage.get();
        spoilage$capturedSeedSpoilage.set(-1.0f);

        // if no spoilage was captured (item wasn't spoilable), skip
        if (seedSpoilage < 0) {
            return;
        }

        Level level = context.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        // get the position where the crop was placed
        BlockPos placedPos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(placedPos).is(block)) {
            // try clicked position if relative didn't work
            placedPos = context.getClickedPos();
            if (!level.getBlockState(placedPos).is(block)) {
                return;
            }
        }

        // store seed spoilage for immature harvest
        // when crop matures it becomes 100% fresh and starts rot cycle
        ChunkSpoilageCapability.setCropPlanted(level, placedPos, seedSpoilage);
    }
}
