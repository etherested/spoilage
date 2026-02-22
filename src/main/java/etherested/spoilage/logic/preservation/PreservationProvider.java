package etherested.spoilage.logic.preservation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// interface for preservation multiplier providers;
// implementations calculate a spoilage rate multiplier based on various factors;
// multiplier values:
//  - 1.0 = normal spoilage rate
//  - < 1.0 = slower spoilage (preservation)
//  - > 1.0 = faster spoilage (accelerated decay)
public interface PreservationProvider {

    // gets the spoilage rate multiplier for a position in a level
    // @param level the world/level
    // @param pos the block position
    // @return the multiplier (lower = slower spoilage, 1.0 = normal)
    float getMultiplier(Level level, BlockPos pos);

    // checks if this provider is currently enabled
    // @return true if this provider should be used
    boolean isEnabled();

    // gets a unique identifier for this provider
    // @return the provider's ID
    String getId();
}
