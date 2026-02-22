package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.ChunkSpoilageCapability;
import etherested.spoilage.data.ChunkSpoilageData;
import etherested.spoilage.network.BlockSpoilageNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
//?}

// handles bone meal usage on crops with spoilage;
// blocks bone meal on crops still recovering from seed spoilage;
// when BONEMEAL_RESETS_ROT is enabled and the crop hasn't fully rotted,
// bone meal resets the fullyGrownTime to restart the fresh period;
// when BONEMEAL_BLOCKED_ON_ROTTEN is enabled, bone meal has no effect
// on any crop that has started rotting (prevents vanilla stage growth)
//? if neoforge {
@EventBusSubscriber(modid = Spoilage.MODID)
//?}
public class CropBonemealHandler {

    //? if neoforge {
    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!SpoilageConfig.isEnabled()) return;

        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BonemealResult result = handleBonemeal(level, event.getPos(), event.getState(), event.getStack());
        if (result == BonemealResult.CONSUMED) {
            event.getStack().shrink(1);
            event.setSuccessful(true);
        } else if (result == BonemealResult.BLOCKED) {
            event.setSuccessful(false);
        }
    }
    //?} else {
    /*public static void registerFabricEvents() {
        // Fabric uses BoneMealItemMixin instead — no event registration needed
    }
    *///?}

    // result of bone meal application attempt
    public enum BonemealResult {
        PASS, CONSUMED, BLOCKED
    }

    // shared logic for handling bone meal on crops;
    // returns BonemealResult indicating what should happen
    public static BonemealResult handleBonemeal(Level level, BlockPos pos, BlockState state, ItemStack bonemealStack) {
        if (!SpoilageConfig.isEnabled()) return BonemealResult.PASS;

        if (!(state.getBlock() instanceof CropBlock)) return BonemealResult.PASS;

        ChunkSpoilageData.BlockSpoilageEntry entry = ChunkSpoilageCapability.getBlockSpoilage(level, pos);
        if (entry == null) return BonemealResult.PASS;

        // block bone meal on crops that are still recovering from seed spoilage
        if (!entry.isFullyGrown() && entry.initialSpoilage() > 0
                && SpoilageConfig.isStaleSeedGrowthPenaltyEnabled()) {
            long worldTime = level.getGameTime();
            long recoveryPeriod = SpoilageConfig.getStaleSeedRecoveryTicks();
            float recovering = entry.getRecoveringSpoilage(worldTime, recoveryPeriod);
            if (recovering > 0) {
                return BonemealResult.BLOCKED;
            }
        }

        // check if this crop is fully grown (for rot reset / bone meal blocking)
        if (!entry.isFullyGrown()) return BonemealResult.PASS;

        // check if the crop is actually rotting (past fresh period)
        long worldTime = level.getGameTime();
        long freshPeriod = SpoilageConfig.getCropFreshPeriodTicks();
        long rotPeriod = SpoilageConfig.getCropRotPeriodTicks();
        float rotProgress = entry.getRotProgress(worldTime, freshPeriod, rotPeriod);

        if (rotProgress <= 0) return BonemealResult.PASS;

        // crop is rotting — try to reset rot if enabled and not fully rotten
        if (rotProgress < 1.0f && SpoilageConfig.doesBonemealResetRot()) {
            ChunkSpoilageCapability.resetCropFullyGrownTime(level, pos);

            if (level instanceof ServerLevel serverLevel) {
                BlockSpoilageNetworkHandler.syncSingleBlock(serverLevel, pos, 0.0f);
            }
            return BonemealResult.CONSUMED;
        }

        // block bone meal on rotten crops to prevent vanilla stage growth
        if (SpoilageConfig.isBonemealBlockedOnRotten()) {
            return BonemealResult.BLOCKED;
        }

        return BonemealResult.PASS;
    }
}
