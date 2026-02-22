package etherested.spoilage.mixin;

//? if fabric {
/*import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.event.CakePlacementHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fabric-only mixin to intercept block placement for spoilage transfer;
// replaces NeoForge's BlockEvent.EntityPlaceEvent
@Mixin(BlockItem.class)
public class BlockPlaceMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void spoilage$afterPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.FAIL) return;
        if (!SpoilageConfig.isEnabled()) return;

        Level level = context.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = context.getClickedPos();
        BlockState placedState = level.getBlockState(pos);
        Block placedBlock = placedState.getBlock();

        if (!SpoilageItemRegistry.isBlockSpoilable(placedBlock)) return;

        Player player = context.getPlayer();
        if (player == null) {
            CakePlacementHandler.handleNonPlayerPlacement(level, pos);
        } else {
            CakePlacementHandler.handlePlayerPlacement(level, pos, player, placedBlock);
        }
    }
}
*///?} else {
// NeoForge stub — BlockPlaceMixin is Fabric-only
public class BlockPlaceMixin {}
//?}
