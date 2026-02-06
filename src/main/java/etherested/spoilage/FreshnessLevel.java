package etherested.spoilage;

/**
 * freshness levels with associated thresholds;
 * freshness = 1.0 - spoilage (so high freshness = low spoilage)
 */
public enum FreshnessLevel {
    FRESH(80, 100),      // 80-100% freshness (0-20% spoilage)
    GOOD(60, 79),        // 60-79% freshness (21-40% spoilage)
    STALE(40, 59),       // 40-59% freshness (41-60% spoilage)
    SPOILING(20, 39),    // 20-39% freshness (61-80% spoilage)
    ROTTEN(1, 19),       // 1-19% freshness (81-99% spoilage)
    INEDIBLE(0, 0);      // 0% freshness (100% spoilage)

    private final int minPercent;
    private final int maxPercent;

    FreshnessLevel(int minPercent, int maxPercent) {
        this.minPercent = minPercent;
        this.maxPercent = maxPercent;
    }

    public int getMinPercent() {
        return minPercent;
    }

    public int getMaxPercent() {
        return maxPercent;
    }

    public float getMinSpoilage() {
        return 1.0f - (maxPercent / 100f);
    }

    public float getMaxSpoilage() {
        return 1.0f - (minPercent / 100f);
    }

    public static FreshnessLevel fromFreshnessPercent(int percent) {
        if (percent >= 80) return FRESH;
        if (percent >= 60) return GOOD;
        if (percent >= 40) return STALE;
        if (percent >= 20) return SPOILING;
        if (percent > 0) return ROTTEN;
        return INEDIBLE;
    }

    public static FreshnessLevel fromSpoilage(float spoilage) {
        int freshPercent = Math.round((1.0f - spoilage) * 100);
        return fromFreshnessPercent(freshPercent);
    }

    public String getTranslationKey() {
        return "tooltip.spoilage.freshness." + name().toLowerCase();
    }
}
