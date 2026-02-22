package etherested.spoilage.util;

// thread-local helper for passing spoilage data to composting logic
public class SpoilageCompostHelper {
    private static final ThreadLocal<Float> CURRENT_SPOILAGE = ThreadLocal.withInitial(() -> 0f);

    public static void setCurrentSpoilage(float spoilage) {
        CURRENT_SPOILAGE.set(spoilage);
    }

    public static float getCurrentSpoilage() {
        return CURRENT_SPOILAGE.get();
    }

    public static void clear() {
        CURRENT_SPOILAGE.remove();
    }

    private static final float THRESHOLD_FULLY_ROTTEN = 1.0f;
    private static final float THRESHOLD_VERY_SPOILED = 0.8f;
    private static final float BONUS_FULLY_ROTTEN = 0.30f;
    private static final float BONUS_VERY_SPOILED = 0.15f;

    // calculates bonus compost chance for rotten items;
    // fully rotten items get +30% bonus chance
    public static float getBonusChance(float spoilage) {
        if (spoilage >= THRESHOLD_FULLY_ROTTEN) {
            return BONUS_FULLY_ROTTEN;
        } else if (spoilage >= THRESHOLD_VERY_SPOILED) {
            return BONUS_VERY_SPOILED;
        }
        return 0f;
    }
}
