package etherested.spoilage.client;

import etherested.spoilage.config.SpoilageConfig;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

// optional Cloth Config screen builder for spoilage settings;
// uses reflection to avoid hard dependency on Cloth Config
public class SpoilageConfigScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilageConfigScreen.class);
    private static boolean clothConfigAvailable;

    static {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            clothConfigAvailable = true;
        } catch (ClassNotFoundException e) {
            clothConfigAvailable = false;
        }
    }

    public static boolean isAvailable() {
        return clothConfigAvailable;
    }

    // creates the config screen, or returns null if Cloth Config is not available
    public static Screen create(Screen parent) {
        if (!clothConfigAvailable) {
            return null;
        }

        try {
            return SpoilageClothConfigBuilder.build(parent);
        } catch (Exception e) {
            LOGGER.error("failed to create Cloth Config screen: {}", e.getMessage());
            return null;
        }
    }
}
