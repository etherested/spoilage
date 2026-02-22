package etherested.spoilage.logic.preservation;

import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.YLevelPreservation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// preservation provider based on Y-level;
// lower Y = better preservation (slower spoilage)
public class YLevelPreservationProvider implements PreservationProvider {

    public static final String ID = "y_level";

    @Override
    public float getMultiplier(Level level, BlockPos pos) {
        return YLevelPreservation.getPreservationMultiplier(pos.getY());
    }

    @Override
    public boolean isEnabled() {
        return SpoilageConfig.isYLevelPreservationEnabled();
    }

    @Override
    public String getId() {
        return ID;
    }
}
