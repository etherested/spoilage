package etherested.spoilage.mixin;

import etherested.spoilage.client.BlockSpoilageClientCache;
import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * mixin to preserve spoilage when using pick block (middle mouse) on spoilable blocks;
 * targets the Block class since most blocks don't override getCloneItemStack
 */
@Mixin(Block.class)
public abstract class BlockPickMixin {

    // fallback lifetime if block not in registry (3 in-game days = 3 * 24000 = 72000 ticks)
    @Unique
    private static final long FALLBACK_LIFETIME = 72000L;

    /** preserves spoilage when using pick block (middle mouse) on spoilable blocks */
    @Inject(method = "getCloneItemStack", at = @At("RETURN"), cancellable = true)
    private void spoilage$preservePickBlockSpoilage(LevelReader level, BlockPos pos, BlockState state,
                                                     CallbackInfoReturnable<ItemStack> cir) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        // check if this block is spoilable
        Block block = state.getBlock();
        if (!SpoilageItemRegistry.isBlockSpoilable(block)) {
            return;
        }

        // need a Level (not just LevelReader) for world time
        if (!(level instanceof Level worldLevel)) {
            return;
        }

        // get spoilage - check client cache first (for creative pick block on client)
        // then fall back to server-side capability
        float spoilage = 0.0f;
        boolean hasSpoilage = false;

        if (worldLevel.isClientSide()) {
            // on client, use the client-side cache which receives synced data from server
            // use helper method to avoid direct class reference to client-only class
            float clientSpoilage = spoilage$getClientCacheSpoilage(pos);
            if (clientSpoilage >= 0.0f) {
                spoilage = clientSpoilage;
                hasSpoilage = true;
            }
        } else {
            // on server, use the capability
            if (ChunkSpoilageCapability.hasBlockSpoilage(worldLevel, pos)) {
                long lifetime = spoilage$getBlockLifetime(block);
                spoilage = ChunkSpoilageCapability.getBlockSpoilagePercent(worldLevel, pos, lifetime);
                hasSpoilage = true;
            }
        }

        if (!hasSpoilage || spoilage <= 0.0f) {
            return; // fresh or no data, no need to modify
        }

        ItemStack result = cir.getReturnValue();
        if (!result.isEmpty() && SpoilageCalculator.isSpoilable(result)) {
            long worldTime = worldLevel.getGameTime();

            if (spoilage >= 1.0f) {
                // inedible items: use creationTime = 0 so all inedible items stack
                // this matches the pattern in BlockSpoilageCleanupHandler
                SpoilageData inedibleData = new SpoilageData(0L, SpoilageData.NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);
                result.set(ModDataComponents.SPOILAGE_DATA.get(), inedibleData);
            } else {
                // normal partially spoiled: use existing method
                SpoilageCalculator.initializeSpoilageWithPercent(result, worldTime, spoilage);
            }

            cir.setReturnValue(result);
        }
    }

    @Unique
    private static long spoilage$getBlockLifetime(Block block) {
        // try to get the linked item for this block
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation linkedItemId = SpoilageItemRegistry.getLinkedItem(blockId);

        if (linkedItemId != null) {
            ItemStack itemStack = new ItemStack(BuiltInRegistries.ITEM.get(linkedItemId));
            long lifetime = SpoilageCalculator.getLifetime(itemStack);
            if (lifetime > 0) {
                return lifetime;
            }
        }

        // fallback: try cake item specifically
        ItemStack cakeStack = new ItemStack(Items.CAKE);
        long lifetime = SpoilageCalculator.getLifetime(cakeStack);
        return lifetime > 0 ? lifetime : FALLBACK_LIFETIME;
    }

    /**
     * gets spoilage from client cache. Returns -1 if not found;
     * uses a supplier to defer class loading of client-only BlockSpoilageClientCache
     */
    @Unique
    private static float spoilage$getClientCacheSpoilage(BlockPos pos) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return -1.0f;
        }
        // use inner class to defer loading of client-only BlockSpoilageClientCache
        return ClientCacheHelper.getSpoilage(pos);
    }

    /**
     * inner class to isolate client-only class references;
     * this class is only loaded when actually accessed, which only happens on client
     */
    private static class ClientCacheHelper {
        static float getSpoilage(BlockPos pos) {
            if (BlockSpoilageClientCache.hasSpoilage(pos)) {
                return BlockSpoilageClientCache.getSpoilage(pos);
            }
            return -1.0f;
        }
    }
}
