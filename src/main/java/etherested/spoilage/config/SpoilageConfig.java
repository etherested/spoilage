package etherested.spoilage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import etherested.spoilage.platform.PlatformHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// JSON-based config system for spoilage;
// loads/saves config/spoilage.json using Gson;
// auto-creates defaults on first run;
// validates values on load
public class SpoilageConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilageConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "spoilage.json";

    private static SpoilageConfig INSTANCE;

    // general settings
    private boolean enabled = true;
    private double globalSpeedMultiplier = 1.0;
    private int checkIntervalTicks = 100;

    // tooltip settings
    private boolean showRemainingTime = true;
    private boolean showFreshnessWord = true;
    private boolean showFreshnessPercentage = false;

    // visual settings
    private boolean showTintOverlay = true;
    private boolean tintStyleRotten = true;
    private boolean useTextureBlending = true;
    private double blendStartThreshold = 0.2;
    private double blendFullThreshold = 1.0;

    // preservation settings
    private boolean yLevelPreservationEnabled = true;
    private boolean biomeTemperaturePreservationEnabled = true;
    private boolean coldSweatIntegrationEnabled = true;

    // Y-level preservation settings
    private int yLevelDeep = 0;
    private int yLevelUnderground = 50;
    private int yLevelSurface = 63;
    private double yLevelDeepMultiplier = 0.6;
    private double yLevelUndergroundMultiplier = 0.8;
    private double yLevelShallowMultiplier = 0.9;

    // biome preservation settings
    private double biomeColdThreshold = 0.3;
    private double biomeHotThreshold = 0.9;
    private double biomeColdMultiplier = 0.7;
    private double biomeHotMultiplier = 1.3;

    // gameplay settings
    private boolean offhandAutoCombineEnabled = true;
    private boolean villagersIgnoreSpoiled = true;
    private boolean animalsPoisonedByRotten = true;
    private boolean preventPlantingSpoiled = true;
    private boolean lootRandomizationEnabled = true;
    private boolean contaminationEnabled = true;
    private double contaminationMultiplierPerSlot = 0.15;
    private double contaminationMaxMultiplier = 3.0;
    private List<String> containerSpoilageRates = new ArrayList<>(List.of(
            "minecraft:shulker_box;0.85",
            "minecraft:barrel;0.9",
            "minecraft:chest;0.95"
    ));

    // crop lifecycle settings
    private int cropFreshPeriodTicks = 72000;
    private int cropRotPeriodTicks = 48000;
    private int cropMinimumHarvestStage = 1;
    private boolean bonemealResetsRot = true;
    private boolean bonemealBlockedOnRotten = true;
    private boolean staleSeedGrowthPenalty = true;
    private int staleSeedRecoveryTicks = 48000;

    private SpoilageConfig() {}

    // loads config from disk or creates defaults
    public static void load() {
        INSTANCE = new SpoilageConfig();
        Path configPath = PlatformHelper.getConfigDir().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    INSTANCE.deserialize(json);
                }
                LOGGER.info("loaded spoilage config from {}", configPath);
            } catch (IOException e) {
                LOGGER.error("failed to load spoilage config: {}", e.getMessage());
            }
        } else {
            LOGGER.info("no spoilage config found, creating defaults at {}", configPath);
        }

        INSTANCE.validate();
        save();
    }

    // saves current config to disk
    public static void save() {
        if (INSTANCE == null) return;
        Path configPath = PlatformHelper.getConfigDir().resolve(CONFIG_FILE);

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(INSTANCE.serialize(), writer);
            }
        } catch (IOException e) {
            LOGGER.error("failed to save spoilage config: {}", e.getMessage());
        }
    }

    // gets the config instance, loading if needed
    private static SpoilageConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    private void validate() {
        globalSpeedMultiplier = clamp(globalSpeedMultiplier, 0.01, 100.0);
        checkIntervalTicks = clamp(checkIntervalTicks, 1, 12000);

        yLevelDeep = clamp(yLevelDeep, -64, 320);
        yLevelUnderground = clamp(yLevelUnderground, -64, 320);
        yLevelSurface = clamp(yLevelSurface, -64, 320);
        yLevelDeepMultiplier = clamp(yLevelDeepMultiplier, 0.01, 1.0);
        yLevelUndergroundMultiplier = clamp(yLevelUndergroundMultiplier, 0.01, 1.0);
        yLevelShallowMultiplier = clamp(yLevelShallowMultiplier, 0.01, 1.0);

        biomeColdThreshold = clamp(biomeColdThreshold, -2.0, 2.0);
        biomeHotThreshold = clamp(biomeHotThreshold, -2.0, 2.0);
        biomeColdMultiplier = clamp(biomeColdMultiplier, 0.01, 2.0);
        biomeHotMultiplier = clamp(biomeHotMultiplier, 0.5, 3.0);

        contaminationMultiplierPerSlot = clamp(contaminationMultiplierPerSlot, 0.01, 1.0);
        contaminationMaxMultiplier = clamp(contaminationMaxMultiplier, 1.0, 10.0);

        cropFreshPeriodTicks = clamp(cropFreshPeriodTicks, 0, 1728000);
        cropRotPeriodTicks = clamp(cropRotPeriodTicks, 1, 1728000);
        cropMinimumHarvestStage = clamp(cropMinimumHarvestStage, 0, 7);
        staleSeedRecoveryTicks = clamp(staleSeedRecoveryTicks, 1, 1728000);

        blendStartThreshold = clamp(blendStartThreshold, 0.0, 1.0);
        blendFullThreshold = clamp(blendFullThreshold, 0.0, 1.0);
    }

    private JsonObject serialize() {
        JsonObject root = new JsonObject();

        // general
        JsonObject general = new JsonObject();
        general.addProperty("enabled", enabled);
        general.addProperty("globalSpeedMultiplier", globalSpeedMultiplier);
        general.addProperty("checkIntervalTicks", checkIntervalTicks);
        root.add("general", general);

        // preservation
        JsonObject preservation = new JsonObject();
        preservation.addProperty("yLevelPreservationEnabled", yLevelPreservationEnabled);
        preservation.addProperty("biomeTemperaturePreservationEnabled", biomeTemperaturePreservationEnabled);
        preservation.addProperty("coldSweatIntegrationEnabled", coldSweatIntegrationEnabled);

        JsonObject yLevel = new JsonObject();
        yLevel.addProperty("yLevelDeep", yLevelDeep);
        yLevel.addProperty("yLevelUnderground", yLevelUnderground);
        yLevel.addProperty("yLevelSurface", yLevelSurface);
        yLevel.addProperty("yLevelDeepMultiplier", yLevelDeepMultiplier);
        yLevel.addProperty("yLevelUndergroundMultiplier", yLevelUndergroundMultiplier);
        yLevel.addProperty("yLevelShallowMultiplier", yLevelShallowMultiplier);
        preservation.add("yLevel", yLevel);

        JsonObject biome = new JsonObject();
        biome.addProperty("biomeColdThreshold", biomeColdThreshold);
        biome.addProperty("biomeHotThreshold", biomeHotThreshold);
        biome.addProperty("biomeColdMultiplier", biomeColdMultiplier);
        biome.addProperty("biomeHotMultiplier", biomeHotMultiplier);
        preservation.add("biome", biome);

        root.add("preservation", preservation);

        // gameplay
        JsonObject gameplay = new JsonObject();
        gameplay.addProperty("offhandAutoCombineEnabled", offhandAutoCombineEnabled);
        gameplay.addProperty("villagersIgnoreSpoiled", villagersIgnoreSpoiled);
        gameplay.addProperty("animalsPoisonedByRotten", animalsPoisonedByRotten);
        gameplay.addProperty("preventPlantingSpoiled", preventPlantingSpoiled);
        gameplay.addProperty("lootRandomizationEnabled", lootRandomizationEnabled);
        gameplay.addProperty("contaminationEnabled", contaminationEnabled);
        gameplay.addProperty("contaminationMultiplierPerSlot", contaminationMultiplierPerSlot);
        gameplay.addProperty("contaminationMaxMultiplier", contaminationMaxMultiplier);

        JsonArray rates = new JsonArray();
        for (String rate : containerSpoilageRates) {
            rates.add(rate);
        }
        gameplay.add("containerSpoilageRates", rates);

        root.add("gameplay", gameplay);

        // crops
        JsonObject crops = new JsonObject();
        crops.addProperty("cropFreshPeriodTicks", cropFreshPeriodTicks);
        crops.addProperty("cropRotPeriodTicks", cropRotPeriodTicks);
        crops.addProperty("cropMinimumHarvestStage", cropMinimumHarvestStage);
        crops.addProperty("bonemealResetsRot", bonemealResetsRot);
        crops.addProperty("bonemealBlockedOnRotten", bonemealBlockedOnRotten);
        crops.addProperty("staleSeedGrowthPenalty", staleSeedGrowthPenalty);
        crops.addProperty("staleSeedRecoveryTicks", staleSeedRecoveryTicks);
        root.add("crops", crops);

        // tooltip
        JsonObject tooltip = new JsonObject();
        tooltip.addProperty("showRemainingTime", showRemainingTime);
        tooltip.addProperty("showFreshnessWord", showFreshnessWord);
        tooltip.addProperty("showFreshnessPercentage", showFreshnessPercentage);
        root.add("tooltip", tooltip);

        // visual
        JsonObject visual = new JsonObject();
        visual.addProperty("showTintOverlay", showTintOverlay);
        visual.addProperty("tintStyleRotten", tintStyleRotten);
        visual.addProperty("useTextureBlending", useTextureBlending);
        visual.addProperty("blendStartThreshold", blendStartThreshold);
        visual.addProperty("blendFullThreshold", blendFullThreshold);
        root.add("visual", visual);

        return root;
    }

    private void deserialize(JsonObject root) {
        // general
        JsonObject general = getObject(root, "general");
        if (general != null) {
            enabled = getBool(general, "enabled", enabled);
            globalSpeedMultiplier = getDouble(general, "globalSpeedMultiplier", globalSpeedMultiplier);
            checkIntervalTicks = getInt(general, "checkIntervalTicks", checkIntervalTicks);
        }

        // preservation
        JsonObject preservation = getObject(root, "preservation");
        if (preservation != null) {
            yLevelPreservationEnabled = getBool(preservation, "yLevelPreservationEnabled", yLevelPreservationEnabled);
            biomeTemperaturePreservationEnabled = getBool(preservation, "biomeTemperaturePreservationEnabled", biomeTemperaturePreservationEnabled);
            coldSweatIntegrationEnabled = getBool(preservation, "coldSweatIntegrationEnabled", coldSweatIntegrationEnabled);

            JsonObject yLevelObj = getObject(preservation, "yLevel");
            if (yLevelObj != null) {
                yLevelDeep = getInt(yLevelObj, "yLevelDeep", yLevelDeep);
                yLevelUnderground = getInt(yLevelObj, "yLevelUnderground", yLevelUnderground);
                yLevelSurface = getInt(yLevelObj, "yLevelSurface", yLevelSurface);
                yLevelDeepMultiplier = getDouble(yLevelObj, "yLevelDeepMultiplier", yLevelDeepMultiplier);
                yLevelUndergroundMultiplier = getDouble(yLevelObj, "yLevelUndergroundMultiplier", yLevelUndergroundMultiplier);
                yLevelShallowMultiplier = getDouble(yLevelObj, "yLevelShallowMultiplier", yLevelShallowMultiplier);
            }

            JsonObject biomeObj = getObject(preservation, "biome");
            if (biomeObj != null) {
                biomeColdThreshold = getDouble(biomeObj, "biomeColdThreshold", biomeColdThreshold);
                biomeHotThreshold = getDouble(biomeObj, "biomeHotThreshold", biomeHotThreshold);
                biomeColdMultiplier = getDouble(biomeObj, "biomeColdMultiplier", biomeColdMultiplier);
                biomeHotMultiplier = getDouble(biomeObj, "biomeHotMultiplier", biomeHotMultiplier);
            }
        }

        // gameplay
        JsonObject gameplay = getObject(root, "gameplay");
        if (gameplay != null) {
            offhandAutoCombineEnabled = getBool(gameplay, "offhandAutoCombineEnabled", offhandAutoCombineEnabled);
            villagersIgnoreSpoiled = getBool(gameplay, "villagersIgnoreSpoiled", villagersIgnoreSpoiled);
            animalsPoisonedByRotten = getBool(gameplay, "animalsPoisonedByRotten", animalsPoisonedByRotten);
            preventPlantingSpoiled = getBool(gameplay, "preventPlantingSpoiled", preventPlantingSpoiled);
            lootRandomizationEnabled = getBool(gameplay, "lootRandomizationEnabled", lootRandomizationEnabled);
            contaminationEnabled = getBool(gameplay, "contaminationEnabled", contaminationEnabled);
            contaminationMultiplierPerSlot = getDouble(gameplay, "contaminationMultiplierPerSlot", contaminationMultiplierPerSlot);
            contaminationMaxMultiplier = getDouble(gameplay, "contaminationMaxMultiplier", contaminationMaxMultiplier);

            if (gameplay.has("containerSpoilageRates") && gameplay.get("containerSpoilageRates").isJsonArray()) {
                containerSpoilageRates = new ArrayList<>();
                for (JsonElement element : gameplay.getAsJsonArray("containerSpoilageRates")) {
                    if (element.isJsonPrimitive()) {
                        containerSpoilageRates.add(element.getAsString());
                    }
                }
            }
        }

        // crops
        JsonObject crops = getObject(root, "crops");
        if (crops != null) {
            cropFreshPeriodTicks = getInt(crops, "cropFreshPeriodTicks", cropFreshPeriodTicks);
            cropRotPeriodTicks = getInt(crops, "cropRotPeriodTicks", cropRotPeriodTicks);
            cropMinimumHarvestStage = getInt(crops, "cropMinimumHarvestStage", cropMinimumHarvestStage);
            bonemealResetsRot = getBool(crops, "bonemealResetsRot", bonemealResetsRot);
            bonemealBlockedOnRotten = getBool(crops, "bonemealBlockedOnRotten", bonemealBlockedOnRotten);
            staleSeedGrowthPenalty = getBool(crops, "staleSeedGrowthPenalty", staleSeedGrowthPenalty);
            staleSeedRecoveryTicks = getInt(crops, "staleSeedRecoveryTicks", staleSeedRecoveryTicks);
        }

        // tooltip
        JsonObject tooltip = getObject(root, "tooltip");
        if (tooltip != null) {
            showRemainingTime = getBool(tooltip, "showRemainingTime", showRemainingTime);
            showFreshnessWord = getBool(tooltip, "showFreshnessWord", showFreshnessWord);
            showFreshnessPercentage = getBool(tooltip, "showFreshnessPercentage", showFreshnessPercentage);
        }

        // visual
        JsonObject visual = getObject(root, "visual");
        if (visual != null) {
            showTintOverlay = getBool(visual, "showTintOverlay", showTintOverlay);
            tintStyleRotten = getBool(visual, "tintStyleRotten", tintStyleRotten);
            useTextureBlending = getBool(visual, "useTextureBlending", useTextureBlending);
            blendStartThreshold = getDouble(visual, "blendStartThreshold", blendStartThreshold);
            blendFullThreshold = getDouble(visual, "blendFullThreshold", blendFullThreshold);
        }
    }

    // STATIC GETTERS (API surface stays identical)

    // general getters
    public static boolean isEnabled() { return get().enabled; }
    public static double getGlobalSpeedMultiplier() { return get().globalSpeedMultiplier; }
    public static int getCheckIntervalTicks() { return get().checkIntervalTicks; }

    // preservation getters
    public static boolean isYLevelPreservationEnabled() { return get().yLevelPreservationEnabled; }
    public static boolean isBiomeTemperaturePreservationEnabled() { return get().biomeTemperaturePreservationEnabled; }
    public static boolean isColdSweatIntegrationEnabled() { return get().coldSweatIntegrationEnabled; }

    // Y-level preservation getters
    public static int getYLevelDeep() { return get().yLevelDeep; }
    public static int getYLevelUnderground() { return get().yLevelUnderground; }
    public static int getYLevelSurface() { return get().yLevelSurface; }
    public static float getYLevelDeepMultiplier() { return (float) get().yLevelDeepMultiplier; }
    public static float getYLevelUndergroundMultiplier() { return (float) get().yLevelUndergroundMultiplier; }
    public static float getYLevelShallowMultiplier() { return (float) get().yLevelShallowMultiplier; }

    // biome preservation getters
    public static float getBiomeColdThreshold() { return (float) get().biomeColdThreshold; }
    public static float getBiomeHotThreshold() { return (float) get().biomeHotThreshold; }
    public static float getBiomeColdMultiplier() { return (float) get().biomeColdMultiplier; }
    public static float getBiomeHotMultiplier() { return (float) get().biomeHotMultiplier; }

    // gameplay getters
    public static boolean isOffhandAutoCombineEnabled() { return get().offhandAutoCombineEnabled; }
    public static boolean doVillagersIgnoreSpoiled() { return get().villagersIgnoreSpoiled; }
    public static boolean areAnimalsPoisonedByRotten() { return get().animalsPoisonedByRotten; }
    public static boolean isPreventPlantingSpoiledEnabled() { return get().preventPlantingSpoiled; }
    public static boolean isLootRandomizationEnabled() { return get().lootRandomizationEnabled; }
    public static boolean isContaminationEnabled() { return get().contaminationEnabled; }
    public static float getContaminationMultiplierPerSlot() { return (float) get().contaminationMultiplierPerSlot; }
    public static float getContaminationMaxMultiplier() { return (float) get().contaminationMaxMultiplier; }

    // container getters
    public static List<String> getContainerSpoilageRates() { return List.copyOf(get().containerSpoilageRates); }

    // tooltip getters
    public static boolean showRemainingTime() { return get().showRemainingTime; }
    public static boolean showFreshnessWord() { return get().showFreshnessWord; }
    public static boolean showFreshnessPercentage() { return get().showFreshnessPercentage; }

    // visual getters
    public static boolean showTintOverlay() { return get().showTintOverlay; }
    public static boolean tintStyleRotten() { return get().tintStyleRotten; }
    public static boolean useTextureBlending() { return get().useTextureBlending; }
    public static double getBlendStartThreshold() { return get().blendStartThreshold; }
    public static double getBlendFullThreshold() { return get().blendFullThreshold; }

    // crop lifecycle getters
    public static int getCropFreshPeriodTicks() { return get().cropFreshPeriodTicks; }
    public static int getCropRotPeriodTicks() { return get().cropRotPeriodTicks; }
    public static int getCropMinimumHarvestStage() { return get().cropMinimumHarvestStage; }
    public static boolean doesBonemealResetRot() { return get().bonemealResetsRot; }
    public static boolean isBonemealBlockedOnRotten() { return get().bonemealBlockedOnRotten; }
    public static boolean isStaleSeedGrowthPenaltyEnabled() { return get().staleSeedGrowthPenalty; }
    public static int getStaleSeedRecoveryTicks() { return get().staleSeedRecoveryTicks; }

    // MUTABLE INSTANCE ACCESS FOR CONFIG SCREEN

    // gets the mutable config instance for use by config screens
    public static SpoilageConfig getInstance() { return get(); }

    // setters for config screen
    public void setEnabled(boolean v) { enabled = v; }
    public void setGlobalSpeedMultiplier(double v) { globalSpeedMultiplier = v; }
    public void setCheckIntervalTicks(int v) { checkIntervalTicks = v; }
    public void setShowRemainingTime(boolean v) { showRemainingTime = v; }
    public void setShowFreshnessWord(boolean v) { showFreshnessWord = v; }
    public void setShowFreshnessPercentage(boolean v) { showFreshnessPercentage = v; }
    public void setShowTintOverlay(boolean v) { showTintOverlay = v; }
    public void setTintStyleRotten(boolean v) { tintStyleRotten = v; }
    public void setUseTextureBlending(boolean v) { useTextureBlending = v; }
    public void setBlendStartThreshold(double v) { blendStartThreshold = v; }
    public void setBlendFullThreshold(double v) { blendFullThreshold = v; }
    public void setYLevelPreservationEnabled(boolean v) { yLevelPreservationEnabled = v; }
    public void setBiomeTemperaturePreservationEnabled(boolean v) { biomeTemperaturePreservationEnabled = v; }
    public void setColdSweatIntegrationEnabled(boolean v) { coldSweatIntegrationEnabled = v; }
    public void setYLevelDeep(int v) { yLevelDeep = v; }
    public void setYLevelUnderground(int v) { yLevelUnderground = v; }
    public void setYLevelSurface(int v) { yLevelSurface = v; }
    public void setYLevelDeepMultiplier(double v) { yLevelDeepMultiplier = v; }
    public void setYLevelUndergroundMultiplier(double v) { yLevelUndergroundMultiplier = v; }
    public void setYLevelShallowMultiplier(double v) { yLevelShallowMultiplier = v; }
    public void setBiomeColdThreshold(double v) { biomeColdThreshold = v; }
    public void setBiomeHotThreshold(double v) { biomeHotThreshold = v; }
    public void setBiomeColdMultiplier(double v) { biomeColdMultiplier = v; }
    public void setBiomeHotMultiplier(double v) { biomeHotMultiplier = v; }
    public void setOffhandAutoCombineEnabled(boolean v) { offhandAutoCombineEnabled = v; }
    public void setVillagersIgnoreSpoiled(boolean v) { villagersIgnoreSpoiled = v; }
    public void setAnimalsPoisonedByRotten(boolean v) { animalsPoisonedByRotten = v; }
    public void setPreventPlantingSpoiled(boolean v) { preventPlantingSpoiled = v; }
    public void setLootRandomizationEnabled(boolean v) { lootRandomizationEnabled = v; }
    public void setContaminationEnabled(boolean v) { contaminationEnabled = v; }
    public void setContaminationMultiplierPerSlot(double v) { contaminationMultiplierPerSlot = v; }
    public void setContaminationMaxMultiplier(double v) { contaminationMaxMultiplier = v; }
    public void setCropFreshPeriodTicks(int v) { cropFreshPeriodTicks = v; }
    public void setCropRotPeriodTicks(int v) { cropRotPeriodTicks = v; }
    public void setCropMinimumHarvestStage(int v) { cropMinimumHarvestStage = v; }
    public void setBonemealResetsRot(boolean v) { bonemealResetsRot = v; }
    public void setBonemealBlockedOnRotten(boolean v) { bonemealBlockedOnRotten = v; }
    public void setStaleSeedGrowthPenalty(boolean v) { staleSeedGrowthPenalty = v; }
    public void setStaleSeedRecoveryTicks(int v) { staleSeedRecoveryTicks = v; }

    // JSON HELPERS

    private static JsonObject getObject(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    private static boolean getBool(JsonObject obj, String key, boolean def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsBoolean() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsDouble() : def;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
