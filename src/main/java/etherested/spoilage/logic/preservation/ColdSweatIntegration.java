package etherested.spoilage.logic.preservation;

import org.slf4j.LoggerFactory;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.lang.reflect.Method;

// soft integration with the Cold Sweat mod for temperature-based preservation;
// uses reflection to avoid hard dependency on Cold Sweat;
// when Cold Sweat is available and enabled:
//  - uses Cold Sweat's temperature API for more accurate temperature readings
//  - falls back to vanilla biome temperature if Cold Sweat is unavailable
public class ColdSweatIntegration implements PreservationProvider {

    public static final String ID = "cold_sweat";
    private static final Logger LOGGER = LoggerFactory.getLogger(ColdSweatIntegration.class);

    private static boolean initialized = false;
    private static boolean available = false;
    private static Method getTempMethod = null;

    // Fallback provider when Cold Sweat is unavailable
    private static final BiomeTemperatureProvider fallback = new BiomeTemperatureProvider();

    // initializes Cold Sweat integration;
    // should be called during mod initialization
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!PlatformHelper.isModLoaded("cold_sweat")) {
            LOGGER.info("Cold Sweat not detected, using vanilla biome temperatures for preservation");
            available = false;
            return;
        }

        try {
            // attempt to load Cold Sweat API via reflection
            // the exact class and method names may vary based on Cold Sweat version
            Class<?> tempHelper = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
            getTempMethod = tempHelper.getMethod("getTemperatureAt", BlockPos.class, Level.class);
            available = true;
            LOGGER.info("Cold Sweat integration initialized successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Cold Sweat detected but Temperature API class not found, falling back to vanilla biome temps");
            available = false;
        } catch (NoSuchMethodException e) {
            LOGGER.warn("Cold Sweat detected but getTemperatureAt method not found, falling back to vanilla biome temps");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Cold Sweat detected but API unavailable ({}), falling back to vanilla biome temps", e.getMessage());
            available = false;
        }
    }

    // checks if Cold Sweat integration is available and working
    public static boolean isAvailable() {
        if (!initialized) {
            init();
        }
        return available;
    }

    @Override
    public float getMultiplier(Level level, BlockPos pos) {
        if (!isAvailable() || !SpoilageConfig.isColdSweatIntegrationEnabled()) {
            // fall back to vanilla biome temperature
            return fallback.getMultiplier(level, pos);
        }

        try {
            // call Cold Sweat's temperature API
            Object result = getTempMethod.invoke(null, pos, level);
            if (result instanceof Number) {
                float temperature = ((Number) result).floatValue();
                // convert Cold Sweat temperature to multiplier
                // Cold Sweat uses different temperature units, so we normalize
                return convertColdSweatTemperature(temperature);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting Cold Sweat temperature, falling back to vanilla: {}", e.getMessage());
        }

        // fallback to vanilla biome temperature
        return fallback.getMultiplier(level, pos);
    }

    // converts Cold Sweat's temperature value to a spoilage multiplier;
    // Cold Sweat temperatures are typically in Minecraft units where:
    // - comfortable range is around 0.0
    // - negative is cold, positive is hot
    // @param coldSweatTemp the temperature from Cold Sweat
    // @return the spoilage multiplier
    private float convertColdSweatTemperature(float coldSweatTemp) {
        // Cold Sweat temperature ranges roughly from -1.0 (freezing) to 1.0 (burning)
        // we map this to our multiplier range

        if (coldSweatTemp < -0.3f) {
            // cold - slower spoilage
            // map -1.0 to 0.5, -0.3 to 0.8
            float t = Math.max(0, (coldSweatTemp + 1.0f) / 0.7f);
            return 0.5f + t * 0.3f;
        } else if (coldSweatTemp > 0.3f) {
            // hot - faster spoilage
            // map 0.3 to 1.0, 1.0 to 1.5
            float t = Math.min(1.0f, (coldSweatTemp - 0.3f) / 0.7f);
            return 1.0f + t * 0.5f;
        } else {
            // comfortable range - normal spoilage
            return 1.0f;
        }
    }

    @Override
    public boolean isEnabled() {
        // only enabled if Cold Sweat is available AND config allows it
        // if Cold Sweat is not available, we don't want this provider to be used at all
        // (the BiomeTemperatureProvider will handle temperature-based preservation instead)
        return isAvailable() && SpoilageConfig.isColdSweatIntegrationEnabled();
    }

    @Override
    public String getId() {
        return ID;
    }
}
