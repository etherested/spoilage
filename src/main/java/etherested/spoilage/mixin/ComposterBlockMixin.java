package etherested.spoilage.mixin;

import etherested.spoilage.util.SpoilageCompostHelper;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * mixin to give bonus compost chance for rotten items;
 * fully rotten items are easier to compost
 */
@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {

    // use ThreadLocal to prevent cross-thread race conditions
    @Unique
    private static final ThreadLocal<Integer> spoilage$previousLevel = ThreadLocal.withInitial(() -> -1);

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void spoilage$storeRottenBonus(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!SpoilageConfig.isEnabled()) {
            SpoilageCompostHelper.clear();
            spoilage$previousLevel.set(-1);
            return;
        }
        if (!SpoilageCalculator.isSpoilable(stack)) {
            SpoilageCompostHelper.clear();
            spoilage$previousLevel.set(-1);
            return;
        }

        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, level.getGameTime());
        SpoilageCompostHelper.setCurrentSpoilage(spoilage);
        spoilage$previousLevel.set(state.getValue(ComposterBlock.LEVEL));
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void spoilage$applyRottenBonus(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        float spoilage = SpoilageCompostHelper.getCurrentSpoilage();
        SpoilageCompostHelper.clear();

        int previousLevel = spoilage$previousLevel.get();

        // only apply bonus if we have spoilage data and we're not already full
        if (spoilage < 0.8f || level.isClientSide()) {
            spoilage$previousLevel.remove();
            return;
        }

        BlockState currentState = level.getBlockState(pos);
        int currentLevel = currentState.getValue(ComposterBlock.LEVEL);

        // check if composting happened (level didn't increase = failed attempt)
        // and we had a valid previous level
        if (previousLevel >= 0 && currentLevel == previousLevel && currentLevel < 7) {
            // composting failed, apply bonus chance for rotten items
            float bonusChance = SpoilageCompostHelper.getBonusChance(spoilage);
            if (level.getRandom().nextFloat() < bonusChance) {
                // grant bonus level increase
                int newLevel = currentLevel + 1;
                level.setBlock(pos, currentState.setValue(ComposterBlock.LEVEL, newLevel), 3);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, currentState));
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }

        spoilage$previousLevel.remove();
    }
}
