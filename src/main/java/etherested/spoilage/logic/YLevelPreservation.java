package etherested.spoilage.logic;

import etherested.spoilage.config.SpoilageConfig;

/**
 * calculates preservation multiplier based on Y-level;
 * lower Y = better preservation
 */
public class YLevelPreservation {
    private static final float SURFACE_MULTIPLIER = 1.0f;

    public static float getPreservationMultiplier(int y) {
        int deepLevel = SpoilageConfig.getYLevelDeep();
        int undergroundLevel = SpoilageConfig.getYLevelUnderground();
        int surfaceLevel = SpoilageConfig.getYLevelSurface();

        float deepMultiplier = SpoilageConfig.getYLevelDeepMultiplier();
        float undergroundMultiplier = SpoilageConfig.getYLevelUndergroundMultiplier();
        float shallowMultiplier = SpoilageConfig.getYLevelShallowMultiplier();

        if (y <= deepLevel) {
            return deepMultiplier;
        } else if (y <= undergroundLevel) {
            float t = (float) (y - deepLevel) / (undergroundLevel - deepLevel);
            return deepMultiplier + t * (undergroundMultiplier - deepMultiplier);
        } else if (y <= surfaceLevel) {
            float t = (float) (y - undergroundLevel) / (surfaceLevel - undergroundLevel);
            return undergroundMultiplier + t * (shallowMultiplier - undergroundMultiplier);
        } else {
            return SURFACE_MULTIPLIER;
        }
    }
}
