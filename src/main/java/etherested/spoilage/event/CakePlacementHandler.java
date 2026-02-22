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

//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
//?}

// handles block placement to preserve item spoilage when placed;
// works with any block that has spoilage data defined (cakes, placed food, etc.)
//? if neoforge {
@EventBusSubscriber(modid = Spoilage.MODID)
//?}
public class CakePlacementHandler {

    //? if neoforge {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!SpoilageConfig.isEnabled()) return;

        BlockState placedState = event.getPlacedBlock();
        Block placedBlock = placedState.getBlock();

        if (!SpoilageItemRegistry.isBlockSpoilable(placedBlock)) return;

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();

        if (!(event.getEntity() instanceof Player player)) {
            handleNonPlayerPlacement(level, pos);
            return;
        }

        handlePlayerPlacement(level, pos, player, placedBlock);
    }
    //?} else {
    /*public static void registerFabricEvents() {
        // Fabric uses BlockPlaceMixin instead — no event registration needed
    }
    *///?}

    // handles block placement by a non-player entity
    public static void handleNonPlayerPlacement(Level level, BlockPos pos) {
        ChunkSpoilageCapability.setBlockPlaced(level, pos);
        if (level instanceof ServerLevel serverLevel) {
            BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, 0.0f);
        }
    }

    // handles block placement by a player, transferring item spoilage
    public static void handlePlayerPlacement(Level level, BlockPos pos, Player player, Block placedBlock) {
        ItemStack placedItem = findMatchingItem(player, placedBlock);
        if (placedItem == null || !SpoilageCalculator.isSpoilable(placedItem)) {
            handleNonPlayerPlacement(level, pos);
            return;
        }

        long worldTime = level.getGameTime();
        float itemSpoilage = SpoilageCalculator.getSpoilagePercent(placedItem, worldTime);

        ChunkSpoilageCapability.setBlockPlacedWithSpoilage(level, pos, itemSpoilage);

        if (level instanceof ServerLevel serverLevel) {
            BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, itemSpoilage);
        }
    }

    // finds an item in the player's hands that matches the placed block;
    // first checks if there's a linked item (same registry ID),
    // then falls back to checking if the block's item form matches
    private static ItemStack findMatchingItem(Player player, Block placedBlock) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(placedBlock);

        ResourceLocation linkedItemId = SpoilageItemRegistry.getLinkedItem(blockId);
        Item targetItem;

        if (linkedItemId != null) {
            targetItem = BuiltInRegistries.ITEM.get(linkedItemId);
        } else {
            targetItem = placedBlock.asItem();
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(targetItem)) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(targetItem)) {
            return offhand;
        }

        return null;
    }
}
