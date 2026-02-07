package etherested.spoilage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class SpoilageConfig {
    public static final ModConfigSpec SPEC;

    // general settings
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue GLOBAL_SPEED_MULTIPLIER;
    public static final ModConfigSpec.IntValue CHECK_INTERVAL_TICKS;

    // tooltip settings
    public static final ModConfigSpec.BooleanValue SHOW_REMAINING_TIME;
    public static final ModConfigSpec.BooleanValue SHOW_FRESHNESS_WORD;
    public static final ModConfigSpec.BooleanValue SHOW_FRESHNESS_PERCENTAGE;

    // visual settings
    public static final ModConfigSpec.BooleanValue SHOW_TINT_OVERLAY;
    public static final ModConfigSpec.BooleanValue TINT_STYLE_ROTTEN;

    // texture blending settings
    public static final ModConfigSpec.BooleanValue USE_TEXTURE_BLENDING;
    public static final ModConfigSpec.DoubleValue BLEND_START_THRESHOLD;
    public static final ModConfigSpec.DoubleValue BLEND_FULL_THRESHOLD;

    // preservation settings
    public static final ModConfigSpec.BooleanValue Y_LEVEL_PRESERVATION_ENABLED;
    public static final ModConfigSpec.BooleanValue BIOME_TEMPERATURE_PRESERVATION_ENABLED;
    public static final ModConfigSpec.BooleanValue COLD_SWEAT_INTEGRATION_ENABLED;

    // Y-level preservation settings
    public static final ModConfigSpec.IntValue Y_LEVEL_DEEP;
    public static final ModConfigSpec.IntValue Y_LEVEL_UNDERGROUND;
    public static final ModConfigSpec.IntValue Y_LEVEL_SURFACE;
    public static final ModConfigSpec.DoubleValue Y_LEVEL_DEEP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue Y_LEVEL_UNDERGROUND_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue Y_LEVEL_SHALLOW_MULTIPLIER;

    // biome preservation settings
    public static final ModConfigSpec.DoubleValue BIOME_COLD_THRESHOLD;
    public static final ModConfigSpec.DoubleValue BIOME_HOT_THRESHOLD;
    public static final ModConfigSpec.DoubleValue BIOME_COLD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BIOME_HOT_MULTIPLIER;

    // gameplay settings
    public static final ModConfigSpec.BooleanValue OFFHAND_AUTO_COMBINE_ENABLED;
    public static final ModConfigSpec.BooleanValue VILLAGERS_IGNORE_SPOILED;
    public static final ModConfigSpec.BooleanValue ANIMALS_POISONED_BY_ROTTEN;
    public static final ModConfigSpec.BooleanValue PREVENT_PLANTING_SPOILED;
    public static final ModConfigSpec.BooleanValue LOOT_RANDOMIZATION_ENABLED;

    // food contamination settings
    public static final ModConfigSpec.BooleanValue CONTAMINATION_ENABLED;
    public static final ModConfigSpec.DoubleValue CONTAMINATION_MULTIPLIER_PER_SLOT;
    public static final ModConfigSpec.DoubleValue CONTAMINATION_MAX_MULTIPLIER;

    // container settings
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTAINER_SPOILAGE_RATES;

    // crop lifecycle settings
    public static final ModConfigSpec.IntValue CROP_FRESH_PERIOD_TICKS;
    public static final ModConfigSpec.IntValue CROP_ROT_PERIOD_TICKS;
    public static final ModConfigSpec.IntValue CROP_MINIMUM_HARVEST_STAGE;
    public static final ModConfigSpec.BooleanValue BONEMEAL_RESETS_ROT;
    public static final ModConfigSpec.BooleanValue BONEMEAL_BLOCKED_ON_ROTTEN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Spoilage System Configuration");
        builder.push("general");

        ENABLED = builder
                .comment("Enable or disable the spoilage system entirely")
                .define("enabled", true);

        GLOBAL_SPEED_MULTIPLIER = builder
                .comment("Global spoilage speed multiplier (higher = faster spoilage)")
                .defineInRange("globalSpeedMultiplier", 1.0, 0.01, 100.0);

        CHECK_INTERVAL_TICKS = builder
                .comment("How often to check and update spoilage (in ticks, 20 ticks = 1 second)")
                .defineInRange("checkIntervalTicks", 100, 1, 12000);

        builder.pop();

        // preservation settings
        builder.comment("Preservation system settings").push("preservation");

        Y_LEVEL_PRESERVATION_ENABLED = builder
                .comment("Enable Y-level based preservation (deeper = slower spoilage)")
                .define("yLevelPreservationEnabled", true);

        BIOME_TEMPERATURE_PRESERVATION_ENABLED = builder
                .comment("Enable biome temperature based preservation (cold biomes = slower spoilage)")
                .define("biomeTemperaturePreservationEnabled", true);

        COLD_SWEAT_INTEGRATION_ENABLED = builder
                .comment("Enable Cold Sweat mod integration for temperature-based preservation (requires Cold Sweat mod)")
                .define("coldSweatIntegrationEnabled", true);

        builder.comment("Y-Level preservation thresholds and multipliers").push("yLevel");

        Y_LEVEL_DEEP = builder
                .comment("Y coordinate considered 'deep underground' (maximum preservation)")
                .defineInRange("yLevelDeep", 0, -64, 320);

        Y_LEVEL_UNDERGROUND = builder
                .comment("Y coordinate considered 'underground'")
                .defineInRange("yLevelUnderground", 50, -64, 320);

        Y_LEVEL_SURFACE = builder
                .comment("Y coordinate at which preservation stops")
                .defineInRange("yLevelSurface", 63, -64, 320);

        Y_LEVEL_DEEP_MULTIPLIER = builder
                .comment("Spoilage speed at deep level (0.6 = 40% slower)")
                .defineInRange("yLevelDeepMultiplier", 0.6, 0.01, 1.0);

        Y_LEVEL_UNDERGROUND_MULTIPLIER = builder
                .comment("Spoilage speed at underground level (0.8 = 20% slower)")
                .defineInRange("yLevelUndergroundMultiplier", 0.8, 0.01, 1.0);

        Y_LEVEL_SHALLOW_MULTIPLIER = builder
                .comment("Spoilage speed at shallow underground (0.9 = 10% slower)")
                .defineInRange("yLevelShallowMultiplier", 0.9, 0.01, 1.0);

        builder.pop();

        builder.comment("Biome temperature preservation thresholds and multipliers").push("biome");

        BIOME_COLD_THRESHOLD = builder
                .comment("Biome temperatures below this get cold preservation bonus")
                .defineInRange("biomeColdThreshold", 0.3, -2.0, 2.0);

        BIOME_HOT_THRESHOLD = builder
                .comment("Biome temperatures above this get hot spoilage penalty")
                .defineInRange("biomeHotThreshold", 0.9, -2.0, 2.0);

        BIOME_COLD_MULTIPLIER = builder
                .comment("Spoilage speed in coldest biomes (0.7 = 30% slower)")
                .defineInRange("biomeColdMultiplier", 0.7, 0.01, 2.0);

        BIOME_HOT_MULTIPLIER = builder
                .comment("Spoilage speed in hottest biomes (1.3 = 30% faster)")
                .defineInRange("biomeHotMultiplier", 1.3, 0.5, 3.0);

        builder.pop();

        builder.pop();

        // gameplay settings
        builder.comment("Gameplay feature settings").push("gameplay");

        OFFHAND_AUTO_COMBINE_ENABLED = builder
                .comment("Automatically combine picked up food with same food in offhand (weighted spoilage average)")
                .define("offhandAutoCombineEnabled", true);

        VILLAGERS_IGNORE_SPOILED = builder
                .comment("Villagers will not pick up or plant spoiled food")
                .define("villagersIgnoreSpoiled", true);

        ANIMALS_POISONED_BY_ROTTEN = builder
                .comment("Animals get poisoned when fed rotten food")
                .define("animalsPoisonedByRotten", true);

        PREVENT_PLANTING_SPOILED = builder
                .comment("Prevent planting spoiled seeds and crops")
                .define("preventPlantingSpoiled", true);

        LOOT_RANDOMIZATION_ENABLED = builder
                .comment("Enable per-item freshness randomization in loot tables")
                .define("lootRandomizationEnabled", true);

        CONTAMINATION_ENABLED = builder
                .comment("Rotten food accelerates spoilage of nearby fresh food in the same inventory")
                .define("contaminationEnabled", true);

        CONTAMINATION_MULTIPLIER_PER_SLOT = builder
                .comment("Spoilage speed increase per rotten slot (0.15 = +15% faster per slot)")
                .defineInRange("contaminationMultiplierPerSlot", 0.15, 0.01, 1.0);

        CONTAMINATION_MAX_MULTIPLIER = builder
                .comment("Maximum spoilage speed multiplier from food contamination")
                .defineInRange("contaminationMaxMultiplier", 3.0, 1.0, 10.0);

        CONTAINER_SPOILAGE_RATES = builder
                .comment("Container spoilage rate multipliers. Format: \"namespace:container;multiplier\"",
                        "Example: \"minecraft:shulker_box;0.85\" means items in shulker boxes spoil at 85% speed",
                        "Lower values = slower spoilage. 1.0 = normal rate, 0.5 = half speed, 0.0 = no spoilage",
                        "Containers not listed here will use normal spoilage rate (1.0)")
                .defineListAllowEmpty(
                        "containerSpoilageRates",
                        List.of("minecraft:shulker_box;0.85", "minecraft:barrel;0.9", "minecraft:chest;0.95"),
                        () -> "minecraft:chest;1.0",
                        obj -> obj instanceof String && ((String) obj).contains(";")
                );

        builder.pop();

        // crop lifecycle settings
        builder.comment("Crop lifecycle settings - controls how crops grow, ripen, and rot").push("crops");

        CROP_FRESH_PERIOD_TICKS = builder
                .comment("How long fully grown crops stay 100% fresh (in ticks)",
                        "Default: 72000 (1 hour real time = 3 Minecraft days)")
                .defineInRange("cropFreshPeriodTicks", 72000, 0, 1728000);

        CROP_ROT_PERIOD_TICKS = builder
                .comment("How long it takes for a crop to fully rot after the fresh period ends (in ticks)",
                        "During this time, crops will regress through growth stages",
                        "Default: 48000 (40 minutes real time = 2 Minecraft days)")
                .defineInRange("cropRotPeriodTicks", 48000, 1, 1728000);

        CROP_MINIMUM_HARVEST_STAGE = builder
                .comment("Crops harvested at or below this growth stage are considered inedible (100% spoiled)",
                        "Default: 1 (stage 0-1 = inedible)")
                .defineInRange("cropMinimumHarvestStage", 1, 0, 7);

        BONEMEAL_RESETS_ROT = builder
                .comment("When bonemeal is used on a rotting crop, reset the fresh timer",
                        "This allows players to save crops that have started to rot")
                .define("bonemealResetsRot", true);

        BONEMEAL_BLOCKED_ON_ROTTEN = builder
                .comment("Block bone meal from having any effect on rotten crops",
                        "When enabled, bone meal cannot grow or reset crops that are currently rotting",
                        "When disabled, vanilla bone meal can still grow stages on rotten crops")
                .define("bonemealBlockedOnRotten", true);

        builder.pop();

        // tooltip settings
        builder.comment("Tooltip display settings").push("tooltip");

        SHOW_REMAINING_TIME = builder
                .comment("Show remaining time until fully spoiled in tooltip")
                .define("showRemainingTime", true);

        SHOW_FRESHNESS_WORD = builder
                .comment("Show freshness label (Fresh, Stale, Spoiled, etc) in tooltip")
                .define("showFreshnessWord", true);

        SHOW_FRESHNESS_PERCENTAGE = builder
                .comment("Show freshness as percentage in tooltip")
                .define("showFreshnessPercentage", false);

        builder.pop();

        // visual settings
        builder.comment("Visual feedback settings").push("visual");

        SHOW_TINT_OVERLAY = builder
                .comment("Show color tint overlay on spoiling items (fallback when no rotten texture available)")
                .define("showTintOverlay", true);

        TINT_STYLE_ROTTEN = builder
                .comment("Tint style: false = warning colors (yellow -> red), true = rotten colors (green/brown decay)")
                .define("tintStyleRotten", true);

        USE_TEXTURE_BLENDING = builder
                .comment("Enable texture blending system for items with rotten textures")
                .define("useTextureBlending", true);

        BLEND_START_THRESHOLD = builder
                .comment("Default spoilage percentage when texture blending begins (0.0-1.0)")
                .defineInRange("blendStartThreshold", 0.2, 0.0, 1.0);

        BLEND_FULL_THRESHOLD = builder
                .comment("Default spoilage percentage when item is fully rotten (0.0-1.0)")
                .defineInRange("blendFullThreshold", 1.0, 0.0, 1.0);

        builder.pop();

        SPEC = builder.build();
    }

    // general getters
    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static double getGlobalSpeedMultiplier() {
        return GLOBAL_SPEED_MULTIPLIER.get();
    }

    public static int getCheckIntervalTicks() {
        return CHECK_INTERVAL_TICKS.get();
    }

    // preservation getters
    public static boolean isYLevelPreservationEnabled() {
        return Y_LEVEL_PRESERVATION_ENABLED.get();
    }

    public static boolean isBiomeTemperaturePreservationEnabled() {
        return BIOME_TEMPERATURE_PRESERVATION_ENABLED.get();
    }

    public static boolean isColdSweatIntegrationEnabled() {
        return COLD_SWEAT_INTEGRATION_ENABLED.get();
    }

    // Y-level preservation getters
    public static int getYLevelDeep() {
        return Y_LEVEL_DEEP.get();
    }

    public static int getYLevelUnderground() {
        return Y_LEVEL_UNDERGROUND.get();
    }

    public static int getYLevelSurface() {
        return Y_LEVEL_SURFACE.get();
    }

    public static float getYLevelDeepMultiplier() {
        return Y_LEVEL_DEEP_MULTIPLIER.get().floatValue();
    }

    public static float getYLevelUndergroundMultiplier() {
        return Y_LEVEL_UNDERGROUND_MULTIPLIER.get().floatValue();
    }

    public static float getYLevelShallowMultiplier() {
        return Y_LEVEL_SHALLOW_MULTIPLIER.get().floatValue();
    }

    // biome preservation getters
    public static float getBiomeColdThreshold() {
        return BIOME_COLD_THRESHOLD.get().floatValue();
    }

    public static float getBiomeHotThreshold() {
        return BIOME_HOT_THRESHOLD.get().floatValue();
    }

    public static float getBiomeColdMultiplier() {
        return BIOME_COLD_MULTIPLIER.get().floatValue();
    }

    public static float getBiomeHotMultiplier() {
        return BIOME_HOT_MULTIPLIER.get().floatValue();
    }

    // gameplay getters
    public static boolean isOffhandAutoCombineEnabled() {
        return OFFHAND_AUTO_COMBINE_ENABLED.get();
    }

    public static boolean doVillagersIgnoreSpoiled() {
        return VILLAGERS_IGNORE_SPOILED.get();
    }

    public static boolean areAnimalsPoisonedByRotten() {
        return ANIMALS_POISONED_BY_ROTTEN.get();
    }

    public static boolean isPreventPlantingSpoiledEnabled() {
        return PREVENT_PLANTING_SPOILED.get();
    }

    public static boolean isLootRandomizationEnabled() {
        return LOOT_RANDOMIZATION_ENABLED.get();
    }

    public static boolean isContaminationEnabled() {
        return CONTAMINATION_ENABLED.get();
    }

    public static float getContaminationMultiplierPerSlot() {
        return CONTAMINATION_MULTIPLIER_PER_SLOT.get().floatValue();
    }

    public static float getContaminationMaxMultiplier() {
        return CONTAMINATION_MAX_MULTIPLIER.get().floatValue();
    }

    // container getters
    public static List<? extends String> getContainerSpoilageRates() {
        return CONTAINER_SPOILAGE_RATES.get();
    }

    // tooltip getters
    public static boolean showRemainingTime() {
        return SHOW_REMAINING_TIME.get();
    }

    public static boolean showFreshnessWord() {
        return SHOW_FRESHNESS_WORD.get();
    }

    public static boolean showFreshnessPercentage() {
        return SHOW_FRESHNESS_PERCENTAGE.get();
    }

    // visual getters
    public static boolean showTintOverlay() {
        return SHOW_TINT_OVERLAY.get();
    }

    public static boolean tintStyleRotten() {
        return TINT_STYLE_ROTTEN.get();
    }

    public static boolean useTextureBlending() {
        return USE_TEXTURE_BLENDING.get();
    }

    public static double getBlendStartThreshold() {
        return BLEND_START_THRESHOLD.get();
    }

    public static double getBlendFullThreshold() {
        return BLEND_FULL_THRESHOLD.get();
    }

    // crop lifecycle getters
    public static int getCropFreshPeriodTicks() {
        return CROP_FRESH_PERIOD_TICKS.get();
    }

    public static int getCropRotPeriodTicks() {
        return CROP_ROT_PERIOD_TICKS.get();
    }

    public static int getCropMinimumHarvestStage() {
        return CROP_MINIMUM_HARVEST_STAGE.get();
    }

    public static boolean doesBonemealResetRot() {
        return BONEMEAL_RESETS_ROT.get();
    }

    public static boolean isBonemealBlockedOnRotten() {
        return BONEMEAL_BLOCKED_ON_ROTTEN.get();
    }
}
