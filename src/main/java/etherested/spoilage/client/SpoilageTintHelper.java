package etherested.spoilage.client;

import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

// helper class for calculating spoilage tint colors;
// used by ItemRendererMixin to apply tints during rendering
public class SpoilageTintHelper {

    // no tint (white = multiply by 1.0)
    public static final int NO_TINT = 0xFFFFFF;

    // gets the spoilage tint color for an item stack;
    // returns 0xFFFFFF (white) if no tint should be applied;
    // items with custom texture stages skip tinting entirely
    public static int getSpoilageTint(ItemStack stack) {
        if (!SpoilageConfig.showTintOverlay()) {
            return NO_TINT;
        }

        if (!SpoilageConfig.isEnabled()) {
            return NO_TINT;
        }

        if (!SpoilageCalculator.isSpoilable(stack)) {
            return NO_TINT;
        }

        // skip tinting for items with spoilage textures (texture blending handles visualization)
        if (SpoilageRottenTextureManager.hasSpoilageTextures(stack)) {
            return NO_TINT;
        }

        if (SpoilageCalculator.getInitializedData(stack) == null) {
            return NO_TINT;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return NO_TINT;
        }

        long worldTime = mc.level.getGameTime();
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);

        // only show tint when item starts spoiling (>10% spoiled)
        if (spoilage < 0.1f) {
            return NO_TINT;
        }

        // get tint color based on style
        return SpoilageConfig.tintStyleRotten()
                ? calculateRottenTintColor(spoilage)
                : calculateWarningTintColor(spoilage);
    }

    // warning style tint: yellow -> orange -> red;
    // used to indicate danger/warning as item spoils
    private static int calculateWarningTintColor(float spoilage) {
        // normalize spoilage from 0.1-1.0 range to 0-1 range
        float t = (spoilage - 0.1f) / 0.9f;
        t = Math.max(0f, Math.min(1f, t));

        // for multiply blend, we use colors close to white for subtle effect
        // white (255,255,255) = no change, darker = more tint
        int r, g, b;

        if (t < 0.5f) {
            // subtle yellow tint -> orange tint
            float factor = t * 2f;
            r = 255;
            g = (int) (255 - (60 * factor));  // 255 -> 195
            b = (int) (255 - (100 * factor)); // 255 -> 155
        } else {
            // orange tint -> red tint
            float factor = (t - 0.5f) * 2f;
            r = 255;
            g = (int) (195 - (100 * factor)); // 195 -> 95
            b = (int) (155 - (100 * factor)); // 155 -> 55
        }

        return (r << 16) | (g << 8) | b;
    }

    // rotten style tint: pale -> greenish -> brown/dark green;
    // makes items look decayed and moldy
    private static int calculateRottenTintColor(float spoilage) {
        // normalize spoilage from 0.1-1.0 range to 0-1 range
        float t = (spoilage - 0.1f) / 0.9f;
        t = Math.max(0f, Math.min(1f, t));

        int r, g, b;

        if (t < 0.5f) {
            // slight pale/sickly tint -> greenish
            float factor = t * 2f;
            r = (int) (255 - (40 * factor));  // 255 -> 215
            g = (int) (255 - (20 * factor));  // 255 -> 235 (keep green high)
            b = (int) (255 - (60 * factor));  // 255 -> 195
        } else {
            // greenish -> brown/moldy green
            float factor = (t - 0.5f) * 2f;
            r = (int) (215 - (70 * factor));  // 215 -> 145
            g = (int) (235 - (50 * factor));  // 235 -> 185
            b = (int) (195 - (100 * factor)); // 195 -> 95
        }

        return (r << 16) | (g << 8) | b;
    }
}
