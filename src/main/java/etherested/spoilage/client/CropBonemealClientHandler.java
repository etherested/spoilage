package etherested.spoilage.client;

//? if neoforge {
import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

// client-side handler for blocking bone meal on rotten crops;
// mirrors the server-side logic using the client spoilage cache
// to prevent hand swing and particles when bone meal is blocked
@EventBusSubscriber(modid = Spoilage.MODID, value = Dist.CLIENT)
public class CropBonemealClientHandler {

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!SpoilageConfig.isEnabled() || !event.getLevel().isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (!(state.getBlock() instanceof CropBlock)) {
            return;
        }

        float rotProgress = BlockSpoilageClientCache.getSpoilage(pos);
        if (rotProgress <= 0) {
            return;
        }

        // crop is rotting — allow bone meal if reset is enabled and not fully rotten
        if (rotProgress < 1.0f && SpoilageConfig.doesBonemealResetRot()) {
            return;
        }

        // block bone meal on rotten crops to prevent hand swing and particles
        if (SpoilageConfig.isBonemealBlockedOnRotten()) {
            event.setSuccessful(false);
        }
    }
}
//?} else {

/*// Fabric stub -- client bonemeal blocking is handled by BoneMealItemMixin
public class CropBonemealClientHandler {
    public static void registerFabricEvents() {
        // handled by BoneMealItemMixin on Fabric
    }
}
*///?}
