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
 * handles bone meal usage on crops with spoilage;
 * blocks bone meal on crops still recovering from seed spoilage;
 * when BONEMEAL_RESETS_ROT is enabled and the crop hasn't fully rotted,
 * bone meal resets the fullyGrownTime to restart the fresh period;
 * when BONEMEAL_BLOCKED_ON_ROTTEN is enabled, bone meal has no effect
 * on any crop that has started rotting (prevents vanilla stage growth)
 */
@EventBusSubscriber(modid = Spoilage.MODID)
public class CropBonemealHandler {

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!SpoilageConfig.isEnabled()) {
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

        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        if (entry == null) {
            return;
        }

        // block bone meal on crops that are still recovering from seed spoilage
        if (!entry.isFullyGrown() && entry.initialSpoilage() > 0
                && SpoilageConfig.isStaleSeedGrowthPenaltyEnabled()) {
            long worldTime = level.getGameTime();
            long recoveryPeriod = SpoilageConfig.getStaleSeedRecoveryTicks();
            float recovering = entry.getRecoveringSpoilage(worldTime, recoveryPeriod);
            if (recovering > 0) {
                event.setSuccessful(false);
                return;
            }
        }

        // check if this crop is fully grown (for rot reset / bone meal blocking)
        if (!entry.isFullyGrown()) {
            return;
        }

        // check if the crop is actually rotting (past fresh period)
        long worldTime = level.getGameTime();
        long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
        long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
        float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

        if (rotProgress <= 0) {
            return;
        }

        // crop is rotting — try to reset rot if enabled and not fully rotten
        if (rotProgress < 1.0f && SpoilageConfig.doesBonemealResetRot()) {
            ChunkSpoilageCapability.resetCropFullyGrownTime(level, pos);

            event.getStack().shrink(1);
            event.setSuccessful(true);

            if (level instanceof ServerLevel serverLevel) {
                BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, 0.0f);
            }
            return;
        }

        // block bone meal on rotten crops to prevent vanilla stage growth
        if (SpoilageConfig.isBonemealBlockedOnRotten()) {
            event.setSuccessful(false);
        }
    }
}
