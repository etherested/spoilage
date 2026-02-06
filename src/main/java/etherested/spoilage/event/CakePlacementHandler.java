package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.logic.SpoilageCalculator;
import etherested.spoilage.network.BlockSpoilageNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * handles block placement to preserve item spoilage when placed;
 * works with any block that has spoilage data defined (cakes, placed food, etc.)
 */
@EventBusSubscriber(modid = Spoilage.MODID)
public class CakePlacementHandler {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        BlockState placedState = event.getPlacedBlock();
        Block placedBlock = placedState.getBlock();

        // check if this block has spoilage data defined
        if (!SpoilageItemRegistry.isBlockSpoilable(placedBlock)) {
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();

        // get the item that was used to place the block
        if (!(event.getEntity() instanceof Player player)) {
            // if not placed by player, start fresh
            ChunkSpoilageCapability.setBlockPlaced(level, pos);
            if (level instanceof ServerLevel serverLevel) {
                BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, 0.0f);
            }
            return;
        }

        // find the matching item in player's hands
        ItemStack placedItem = findMatchingItem(player, placedBlock);
        if (placedItem == null || !SpoilageCalculator.isSpoilable(placedItem)) {
            // no spoilage data, start fresh
            ChunkSpoilageCapability.setBlockPlaced(level, pos);
            if (level instanceof ServerLevel serverLevel) {
                BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, 0.0f);
            }
            return;
        }

        // get the item's spoilage and transfer to the placed block
        long worldTime = level.getGameTime();
        float itemSpoilage = SpoilageCalculator.getSpoilagePercent(placedItem, worldTime);

        // store the block's spoilage (preserving from item)
        ChunkSpoilageCapability.setBlockPlacedWithSpoilage(level, pos, itemSpoilage);

        // immediately sync to clients for instant visual update
        if (level instanceof ServerLevel serverLevel) {
            BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, itemSpoilage);
        }
    }

    /**
     * finds an item in the player's hands that matches the placed block;
     * first checks if there's a linked item (same registry ID),
     * then falls back to checking if the block's item form matches
     */
    private static ItemStack findMatchingItem(Player player, Block placedBlock) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(placedBlock);

        // try to get the linked item (same registry ID as block)
        ResourceLocation linkedItemId = SpoilageItemRegistry.getLinkedItem(blockId);
        Item targetItem;

        if (linkedItemId != null) {
            targetItem = BuiltInRegistries.ITEM.get(linkedItemId);
        } else {
            // fallback: use the block's item form
            targetItem = placedBlock.asItem();
        }

        // check main hand
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(targetItem)) {
            return mainHand;
        }

        // check offhand
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(targetItem)) {
            return offhand;
        }

        return null;
    }
}
