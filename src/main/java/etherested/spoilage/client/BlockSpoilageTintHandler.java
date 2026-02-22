package etherested.spoilage.client;

import etherested.spoilage.config.SpoilageConfig;

// helper class for calculating block spoilage tint colors;
// used by BlockSpoilageOverlayRenderer for blocks without custom textures;
// unlike items, blocks don't use Minecraft's BlockColor system for spoilage
// because that requires models to have tint indices defined,
// instead, the BlockSpoilageOverlayRenderer renders a tinted overlay on top of the block
public class BlockSpoilageTintHandler {

    // calculates the tint color for a block based on spoilage percentage;
    // uses the same color calculation as SpoilageTintHelper for consistency
    // @param spoilage spoilage percentage (0.0 to 1.0)
    // @return RGB color value
    public static int calculateBlockTintColor(float spoilage) {
        // normalize spoilage from 0.1-1.0 range to 0-1 range
        float t = (spoilage - 0.1f) / 0.9f;
        t = Math.max(0f, Math.min(1f, t));

        int r, g, b;

        if (SpoilageConfig.tintStyleRotten()) {
            // rotten style tint: pale -> greenish -> brown/dark green
            if (t < 0.5f) {
                float factor = t * 2f;
                r = (int) (255 - (40 * factor));
                g = (int) (255 - (20 * factor));
                b = (int) (255 - (60 * factor));
            } else {
                float factor = (t - 0.5f) * 2f;
                r = (int) (215 - (70 * factor));
                g = (int) (235 - (50 * factor));
                b = (int) (195 - (100 * factor));
            }
        } else {
            // warning style tint: yellow -> orange -> red
            if (t < 0.5f) {
                float factor = t * 2f;
                r = 255;
                g = (int) (255 - (60 * factor));
                b = (int) (255 - (100 * factor));
            } else {
                float factor = (t - 0.5f) * 2f;
                r = 255;
                g = (int) (195 - (100 * factor));
                b = (int) (155 - (100 * factor));
            }
        }

        return (r << 16) | (g << 8) | b;
    }
}
