package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.ChunkSpoilageData;
import etherested.spoilage.data.SpoilableItemData;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.logic.SpoilageCalculator;
import etherested.spoilage.network.BlockSpoilageNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * handles cleanup of block spoilage data when blocks are removed;
 * also transfers stored spoilage to dropped items
 */
@EventBusSubscriber(modid = Spoilage.MODID)
public class BlockSpoilageCleanupHandler {

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        // handle crop drops, transfer stored seed spoilage to dropped items
        if (block instanceof CropBlock cropBlock) {
            handleCropDrops(level, pos, cropBlock, state, event);
            return;
        }

        // handle generic spoilable block drops
        if (SpoilageItemRegistry.isBlockSpoilable(block)) {
            handleSpoilableBlockDrops(level, pos, block, event);
        }
    }

    /**
     * handles crop block drops;
     * applies rot progress as spoilage, marks inedible if below minimum stage
     */
    private static void handleCropDrops(Level level, BlockPos pos, CropBlock cropBlock,
                                        BlockState state, BlockDropsEvent event) {
        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        long worldTime = level.getGameTime();
        float spoilageToApply = 0.0f;
        boolean hasTrackingData = (entry != null);

        // get current crop age
        int currentAge = cropBlock.getAge(state);
        int minHarvestStage = SpoilageConfig.getCropMinimumHarvestStage();

        if (entry != null && entry.isFullyGrown()) {
            // crop was fully grown at some point, calculate rot progress
            long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
            long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
            float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

            // below minimum stage = inedible (100% spoilage)
            if (currentAge <= minHarvestStage) {
                spoilageToApply = 1.0f;
            } else {
                spoilageToApply = rotProgress;
            }
        } else if (entry != null) {
            // crop was never fully grown (harvested early), use the stored seed spoilage
            spoilageToApply = entry.initialSpoilage();
        } else {
            // no tracking data, apply 0% (fresh)
            spoilageToApply = 0.0f;
        }

        // always apply spoilage for tracked crops (even 0%) to prevent other systems
        // from applying random spoilage to the drops
        if (hasTrackingData || spoilageToApply >= 0) {
            // cache SpoilageData per item type to ensure identical creation_time for stacking
            Map<Item, SpoilageData> spoilageDataCache = new HashMap<>();

            // for inedible items (100% spoilage), use a unified creation_time of 0
            // this ensures all inedible items of the same type stack regardless of when planted
            boolean isInedible = spoilageToApply >= 1.0f;

            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack stack = itemEntity.getItem();
                if (SpoilageCalculator.isSpoilable(stack)) {
                    Item itemType = stack.getItem();

                    // get or create cached SpoilageData for this item type
                    SpoilageData cachedData = spoilageDataCache.get(itemType);
                    if (cachedData == null) {
                        long adjustedCreation;
                        if (isInedible) {
                            // use creation_time of 0 for all inedible items so they stack
                            adjustedCreation = 0L;
                        } else {
                            // calculate normally for partially spoiled items
                            long lifetime = SpoilageCalculator.getLifetime(stack);
                            long elapsed = (long) (lifetime * spoilageToApply);
                            adjustedCreation = worldTime - elapsed;
                        }
                        cachedData = new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);
                        spoilageDataCache.put(itemType, cachedData);
                    }

                    // apply the exact same SpoilageData to ensure stacking
                    stack.set(ModDataComponents.SPOILAGE_DATA.get(), cachedData);
                }
            }
        }

        // clean up the stored data and notify clients to clear the cache
        ChunkSpoilageCapability.removeBlockSpoilage(level, pos);
        BlockSpoilageNetworkHandler.syncSingleBlock((ServerLevel) level, pos, 0.0f);
    }

    /** handles generic spoilable block drops, transfers stored spoilage to dropped items */
    private static void handleSpoilableBlockDrops(Level level, BlockPos pos, Block block, BlockDropsEvent event) {
        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);

        if (entry != null) {
            // get the block's spoilage data to determine lifetime
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            SpoilableItemData blockData = SpoilageItemRegistry.getBlockData(blockId);

            if (blockData != null) {
                // calculate current spoilage percentage
                long worldTime = level.getGameTime();
                long lifetime = SpoilageCalculator.getLifetime(new ItemStack(block.asItem()));
                if (lifetime <= 0) {
                    // fallback to a default lifetime if not determinable from item
                    lifetime = 24000L * 3; // 3 Minecraft days default
                }
                float currentSpoilage = entry.getSpoilage(worldTime, lifetime);

                // find the linked item to transfer spoilage to
                ResourceLocation linkedItemId = SpoilageItemRegistry.getLinkedItem(blockId);
                Item linkedItem = linkedItemId != null
                        ? BuiltInRegistries.ITEM.get(linkedItemId)
                        : block.asItem();

                // for inedible items (100% spoilage), use unified creation_time so they stack
                boolean isInedible = currentSpoilage >= 1.0f;
                SpoilageData cachedData = null;

                // apply spoilage to matching dropped items
                for (ItemEntity itemEntity : event.getDrops()) {
                    ItemStack stack = itemEntity.getItem();
                    if (stack.is(linkedItem) && SpoilageCalculator.isSpoilable(stack)) {
                        if (cachedData == null) {
                            long adjustedCreation;
                            if (isInedible) {
                                // use creation_time of 0 for all inedible items so they stack
                                adjustedCreation = 0L;
                            } else {
                                // calculate normally for partially spoiled items
                                long itemLifetime = SpoilageCalculator.getLifetime(stack);
                                long elapsed = (long) (itemLifetime * currentSpoilage);
                                adjustedCreation = worldTime - elapsed;
                            }
                            cachedData = new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);
                        }
                        stack.set(ModDataComponents.SPOILAGE_DATA.get(), cachedData);
                    }
                }
            }
        }

        // clean up the stored data and notify clients to clear the cache
        ChunkSpoilageCapability.removeBlockSpoilage(level, pos);
        BlockSpoilageNetworkHandler.syncSingleBlock((ServerLevel) level, pos, 0.0f);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // this event fires before drops, use it for any pre-drop logic if needed
        // the main cleanup is now in onBlockDrops
    }
}
