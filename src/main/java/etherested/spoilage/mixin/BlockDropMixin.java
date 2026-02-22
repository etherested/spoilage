package etherested.spoilage.mixin;

//? if fabric {
/*import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.event.BlockSpoilageCleanupHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Fabric-only mixin to intercept block drops for spoilage transfer;
// replaces NeoForge's BlockDropsEvent
@Mixin(Block.class)
public class BlockDropMixin {

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL"))
    private static void spoilage$onDropResources(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled() || level.isClientSide()) return;

        Block block = state.getBlock();

        // collect dropped item entities near the block position
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(1.0));

        if (block instanceof CropBlock cropBlock) {
            BlockSpoilageCleanupHandler.handleCropDrops(level, pos, cropBlock, state, nearbyItems);
        } else if (SpoilageItemRegistry.isBlockSpoilable(block)) {
            BlockSpoilageCleanupHandler.handleSpoilableBlockDrops(level, pos, block, nearbyItems);
        }
    }
}
*///?} else {
// NeoForge stub — BlockDropMixin is Fabric-only
public class BlockDropMixin {}
//?}
