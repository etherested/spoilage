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

//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
//?}

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// handles cleanup of block spoilage data when blocks are removed;
// also transfers stored spoilage to dropped items
//? if neoforge {
@EventBusSubscriber(modid = Spoilage.MODID)
//?}
public class BlockSpoilageCleanupHandler {

    //? if neoforge {
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!SpoilageConfig.isEnabled()) return;

        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        if (block instanceof CropBlock cropBlock) {
            handleCropDrops(level, pos, cropBlock, state, event.getDrops());
            return;
        }

        if (SpoilageItemRegistry.isBlockSpoilable(block)) {
            handleSpoilableBlockDrops(level, pos, block, event.getDrops());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // this event fires before drops, use it for any pre-drop logic if needed
        // the main cleanup is now in onBlockDrops
    }
    //?} else {
    /*public static void registerFabricEvents() {
        // Fabric uses BlockDropMixin for drop handling — no event registration needed
    }
    *///?}

    // handles crop block drops;
    // applies rot progress as spoilage, marks inedible if below minimum stage;
    // shared between NeoForge event and Fabric mixin
    public static void handleCropDrops(Level level, BlockPos pos, CropBlock cropBlock,
                                        BlockState state, List<ItemEntity> drops) {
        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        long worldTime = level.getGameTime();
        float spoilageToApply = 0.0f;
        boolean hasTrackingData = (entry != null);

        int currentAge = cropBlock.getAge(state);
        int minHarvestStage = SpoilageConfig.getCropMinimumHarvestStage();

        if (entry != null && entry.isFullyGrown()) {
            long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
            long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
            float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

            if (currentAge <= minHarvestStage) {
                spoilageToApply = 1.0f;
            } else {
                spoilageToApply = rotProgress;
            }
        } else if (entry != null) {
            spoilageToApply = entry.initialSpoilage();
        }

        if (hasTrackingData || spoilageToApply >= 0) {
            Map<Item, SpoilageData> spoilageDataCache = new HashMap<>();
            boolean isInedible = spoilageToApply >= 1.0f;

            for (ItemEntity itemEntity : drops) {
                ItemStack stack = itemEntity.getItem();
                if (SpoilageCalculator.isSpoilable(stack)) {
                    Item itemType = stack.getItem();
                    SpoilageData cachedData = spoilageDataCache.get(itemType);
                    if (cachedData == null) {
                        long adjustedCreation;
                        if (isInedible) {
                            adjustedCreation = 0L;
                        } else {
                            long lifetime = SpoilageCalculator.getLifetime(stack);
                            long elapsed = (long) (lifetime * spoilageToApply);
                            adjustedCreation = worldTime - elapsed;
                        }
                        cachedData = new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);
                        spoilageDataCache.put(itemType, cachedData);
                    }
                    stack.set(ModDataComponents.spoilageData(), cachedData);
                }
            }
        }

        ChunkSpoilageCapability.removeBlockSpoilage(level, pos);
        BlockSpoilageNetworkHandler.syncSingleBlock((ServerLevel) level, pos, 0.0f);
    }

    // handles generic spoilable block drops, transfers stored spoilage to dropped items
    public static void handleSpoilableBlockDrops(Level level, BlockPos pos, Block block, List<ItemEntity> drops) {
        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);

        if (entry != null) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            SpoilableItemData blockData = SpoilageItemRegistry.getBlockData(blockId);

            if (blockData != null) {
                long worldTime = level.getGameTime();
                long lifetime = SpoilageCalculator.getLifetime(new ItemStack(block.asItem()));
                if (lifetime <= 0) {
                    lifetime = 24000L * 3;
                }
                float currentSpoilage = entry.getSpoilage(worldTime, lifetime);

                ResourceLocation linkedItemId = SpoilageItemRegistry.getLinkedItem(blockId);
                Item linkedItem = linkedItemId != null
                        ? BuiltInRegistries.ITEM.get(linkedItemId)
                        : block.asItem();

                boolean isInedible = currentSpoilage >= 1.0f;
                SpoilageData cachedData = null;

                for (ItemEntity itemEntity : drops) {
                    ItemStack stack = itemEntity.getItem();
                    if (stack.is(linkedItem) && SpoilageCalculator.isSpoilable(stack)) {
                        if (cachedData == null) {
                            long adjustedCreation;
                            if (isInedible) {
                                adjustedCreation = 0L;
                            } else {
                                long itemLifetime = SpoilageCalculator.getLifetime(stack);
                                long elapsed = (long) (itemLifetime * currentSpoilage);
                                adjustedCreation = worldTime - elapsed;
                            }
                            cachedData = new SpoilageData(adjustedCreation, SpoilageData.NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);
                        }
                        stack.set(ModDataComponents.spoilageData(), cachedData);
                    }
                }
            }
        }

        ChunkSpoilageCapability.removeBlockSpoilage(level, pos);
        BlockSpoilageNetworkHandler.syncSingleBlock((ServerLevel) level, pos, 0.0f);
    }
}
