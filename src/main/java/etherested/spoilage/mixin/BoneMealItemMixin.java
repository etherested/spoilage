package etherested.spoilage.mixin;

//? if fabric {
/*import etherested.spoilage.client.BlockSpoilageClientCache;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.event.CropBonemealHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fabric-only mixin to intercept bone meal usage on crops;
// replaces NeoForge's BonemealEvent for both client and server
@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void spoilage$onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!SpoilageConfig.isEnabled()) return;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CropBlock)) return;

        if (level.isClientSide()) {
            // client-side: check cache for visual blocking
            float rotProgress = BlockSpoilageClientCache.getSpoilage(pos);
            if (rotProgress <= 0) return;

            if (rotProgress < 1.0f && SpoilageConfig.doesBonemealResetRot()) return;

            if (SpoilageConfig.isBonemealBlockedOnRotten()) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        // server-side: use shared logic
        CropBonemealHandler.BonemealResult result = CropBonemealHandler.handleBonemeal(level, pos, state, context.getItemInHand());
        if (result == CropBonemealHandler.BonemealResult.CONSUMED) {
            context.getItemInHand().shrink(1);
            cir.setReturnValue(InteractionResult.sidedSuccess(false));
        } else if (result == CropBonemealHandler.BonemealResult.BLOCKED) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
*///?} else {
// NeoForge stub — BoneMealItemMixin is Fabric-only
public class BoneMealItemMixin {}
//?}
