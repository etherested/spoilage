package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.ChunkSpoilageData;
import etherested.spoilage.network.BlockSpoilageNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

/**
 * handles bone meal usage on crops to reset rot timer;
 * when BONEMEAL_RESETS_ROT is enabled and bone meal is used on a rotting crop,
 * the fullyGrownTime is reset to the current time, restarting the fresh period;
 * this allows players to save crops that have started to rot
 */
@EventBusSubscriber(modid = Spoilage.MODID)
public class CropBonemealHandler {

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.doesBonemealResetRot()) {
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        // only handle crop blocks
        if (!(state.getBlock() instanceof CropBlock)) {
            return;
        }

        // check if this crop has spoilage data and is fully grown (rotting)
        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        if (entry == null || !entry.isFullyGrown()) {
            return;
        }

        // check if the crop is actually rotting (past fresh period)
        long worldTime = level.getGameTime();
        long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
        long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
        float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

        if (rotProgress > 0) {
            // reset the fully grown time to restart fresh period
            ChunkSpoilageCapability.resetCropFullyGrownTime(level, pos);

            // consume the bone meal item
            event.getStack().shrink(1);

            // mark as successful to trigger particles/sound
            event.setSuccessful(true);

            // sync the updated state to clients (rot tint should disappear)
            if (level instanceof ServerLevel serverLevel) {
                // after reset, rot progress is 0, so sync 0 spoilage
                BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, 0.0f);
            }
        }
    }
}
