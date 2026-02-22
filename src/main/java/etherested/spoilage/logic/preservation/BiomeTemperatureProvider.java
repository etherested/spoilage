package etherested.spoilage.logic.preservation;

import etherested.spoilage.config.SpoilageConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

// preservation provider based on vanilla biome temperature;
// biome base temperatures typically range from 0.0 to 2.0:
//  - cold biomes (< threshold): slower spoilage (configurable multiplier)
//  - temperate biomes (between thresholds): normal spoilage (1.0x)
//  - hot biomes (> threshold): faster spoilage (configurable multiplier)
public class BiomeTemperatureProvider implements PreservationProvider {

    public static final String ID = "biome_temperature";

    private static final float NORMAL_MULTIPLIER = 1.0f;

    @Override
    public float getMultiplier(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();

        // get the biome's base temperature
        float temperature = biome.getBaseTemperature();

        return getMultiplierForTemperature(temperature);
    }

    // calculates the spoilage multiplier for a given temperature value;
    // can be used by other systems (like Cold Sweat integration) for consistent behavior;
    // @param temperature the temperature value
    // @return the spoilage multiplier
    public static float getMultiplierForTemperature(float temperature) {
        float coldThreshold = SpoilageConfig.getBiomeColdThreshold();
        float hotThreshold = SpoilageConfig.getBiomeHotThreshold();
        float coldMultiplier = SpoilageConfig.getBiomeColdMultiplier();
        float hotMultiplier = SpoilageConfig.getBiomeHotMultiplier();

        if (temperature < coldThreshold) {
            // cold biome - slower spoilage
            // interpolate from coldMultiplier at temp=0 to NORMAL_MULTIPLIER at temp=coldThreshold
            float t = temperature / coldThreshold;
            return coldMultiplier + t * (NORMAL_MULTIPLIER - coldMultiplier);
        } else if (temperature > hotThreshold) {
            // hot biome - faster spoilage
            // interpolate from NORMAL_MULTIPLIER at temp=hotThreshold to hotMultiplier at temp=2.0
            float t = Math.min(1.0f, (temperature - hotThreshold) / (2.0f - hotThreshold));
            return NORMAL_MULTIPLIER + t * (hotMultiplier - NORMAL_MULTIPLIER);
        } else {
            // temperate biome - normal spoilage
            return NORMAL_MULTIPLIER;
        }
    }

    @Override
    public boolean isEnabled() {
        return SpoilageConfig.isBiomeTemperaturePreservationEnabled();
    }

    @Override
    public String getId() {
        return ID;
    }
}
