package etherested.spoilage.logic.preservation;

import org.slf4j.LoggerFactory;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.ContainerSpoilageRates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * central manager for all preservation providers;
 * combines multiple preservation factors to calculate final spoilage multiplier
 */
public class PreservationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreservationManager.class);
    private static final List<PreservationProvider> providers = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * initializes the preservation system;
     * should be called during mod initialization
     */
    public static void init() {
        if (initialized) {
            return;
        }

        // register built-in providers
        providers.add(new YLevelPreservationProvider());
        providers.add(new BiomeTemperatureProvider());

        // initialize Cold Sweat integration (will do nothing if mod not present)
        ColdSweatIntegration.init();
        providers.add(new ColdSweatIntegration());

        initialized = true;
        LOGGER.info("Preservation manager initialized with {} providers", providers.size());
    }

    /**
     * registers a custom preservation provider;
     * can be used by other mods to add their own preservation factors
     * @param provider the provider to register
     */
    public static void registerProvider(PreservationProvider provider) {
        ensureInitialized();
        providers.add(provider);
        LOGGER.info("Registered custom preservation provider: {}", provider.getId());
    }

    /**
     * calculates the combined preservation multiplier for a position;
     * multiplies all enabled providers' multipliers together
     * @param level the world/level
     * @param pos the block position
     * @return the combined multiplier (lower = slower spoilage)
     */
    public static float getMultiplier(Level level, BlockPos pos) {
        ensureInitialized();

        float combinedMultiplier = 1.0f;

        for (PreservationProvider provider : providers) {
            if (provider.isEnabled()) {
                // special case: if Cold Sweat is enabled and available, skip BiomeTemperatureProvider
                // to avoid double-counting temperature effects
                if (provider.getId().equals(BiomeTemperatureProvider.ID)
                        && ColdSweatIntegration.isAvailable()
                        && SpoilageConfig.isColdSweatIntegrationEnabled()) {
                    continue;
                }

                float multiplier = provider.getMultiplier(level, pos);
                combinedMultiplier *= multiplier;
            }
        }

        return combinedMultiplier;
    }

    /**
     * calculates the combined preservation multiplier for a container;
     * includes both position-based and container-specific factors
     * @param level the world/level
     * @param pos the block position
     * @param blockEntity the container block entity (can be null)
     * @return the combined multiplier
     */
    public static float getContainerMultiplier(Level level, BlockPos pos, BlockEntity blockEntity) {
        float positionMultiplier = getMultiplier(level, pos);

        // add container-specific multiplier
        float containerMultiplier = ContainerSpoilageRates.getMultiplier(blockEntity);

        return positionMultiplier * containerMultiplier;
    }

    /**
     * gets detailed preservation info for display purposes
     * @param level the world/level
     * @param pos the block position
     * @return a PreservationInfo containing all individual multipliers
     */
    public static PreservationInfo getPreservationInfo(Level level, BlockPos pos) {
        ensureInitialized();

        float yLevel = 1.0f;
        float biome = 1.0f;
        float container = 1.0f;

        for (PreservationProvider provider : providers) {
            if (!provider.isEnabled()) {
                continue;
            }

            // skip biome temp if Cold Sweat handles it
            if (provider.getId().equals(BiomeTemperatureProvider.ID)
                    && ColdSweatIntegration.isAvailable()
                    && SpoilageConfig.isColdSweatIntegrationEnabled()) {
                continue;
            }

            float multiplier = provider.getMultiplier(level, pos);

            switch (provider.getId()) {
                case YLevelPreservationProvider.ID:
                    yLevel = multiplier;
                    break;
                case BiomeTemperatureProvider.ID:
                case ColdSweatIntegration.ID:
                    biome = multiplier;
                    break;
            }
        }

        return new PreservationInfo(yLevel, biome, container);
    }

    /**
     * gets detailed preservation info for a container
     * @param level the world/level
     * @param pos the block position
     * @param blockEntity the container block entity
     * @return a PreservationInfo containing all individual multipliers
     */
    public static PreservationInfo getContainerPreservationInfo(Level level, BlockPos pos, BlockEntity blockEntity) {
        PreservationInfo posInfo = getPreservationInfo(level, pos);
        float containerMultiplier = ContainerSpoilageRates.getMultiplier(blockEntity);
        return posInfo.withContainer(containerMultiplier);
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    /** record containing individual preservation multipliers for display */
    public record PreservationInfo(
            float yLevelMultiplier,
            float biomeMultiplier,
            float containerMultiplier
    ) {
        public float getCombinedMultiplier() {
            return yLevelMultiplier * biomeMultiplier * containerMultiplier;
        }

        public PreservationInfo withContainer(float container) {
            return new PreservationInfo(yLevelMultiplier, biomeMultiplier, container);
        }

        public boolean hasYLevelBonus() {
            return yLevelMultiplier < 1.0f;
        }

        public boolean hasBiomeBonus() {
            return biomeMultiplier < 1.0f;
        }

        public boolean hasBiomePenalty() {
            return biomeMultiplier > 1.0f;
        }

        public boolean hasContainerBonus() {
            return containerMultiplier < 1.0f;
        }

        public int getYLevelBonusPercent() {
            return Math.round((1.0f - yLevelMultiplier) * 100);
        }

        public int getBiomeBonusPercent() {
            if (biomeMultiplier < 1.0f) {
                return Math.round((1.0f - biomeMultiplier) * 100);
            } else {
                return -Math.round((biomeMultiplier - 1.0f) * 100);
            }
        }

        public int getContainerBonusPercent() {
            return Math.round((1.0f - containerMultiplier) * 100);
        }
    }
}
