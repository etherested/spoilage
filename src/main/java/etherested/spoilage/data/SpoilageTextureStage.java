package etherested.spoilage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

/**
 * data structure for a spoilage texture stage that blends in over a threshold range;
 * used for both stale and rotten texture transitions
 * @param model resourceLocation of the model (e.g., "spoilage:item/potato_stale")
 * @param startThreshold spoilage percentage when blending begins
 * @param fullThreshold spoilage percentage when texture is fully visible
 * @param modelByState optional map of block state keys to models for state-aware rendering
 */
public record SpoilageTextureStage(
        ResourceLocation model,
        float startThreshold,
        float fullThreshold,
        Optional<Map<String, ResourceLocation>> modelByState
) {
    public SpoilageTextureStage(ResourceLocation model, float startThreshold, float fullThreshold) {
        this(model, startThreshold, fullThreshold, Optional.empty());
    }

    public static final Codec<SpoilageTextureStage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(SpoilageTextureStage::model),
            Codec.FLOAT.optionalFieldOf("start_threshold", 0.0f).forGetter(SpoilageTextureStage::startThreshold),
            Codec.FLOAT.optionalFieldOf("full_threshold", 1.0f).forGetter(SpoilageTextureStage::fullThreshold),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("model_by_state").forGetter(SpoilageTextureStage::modelByState)
    ).apply(instance, SpoilageTextureStage::new));

    /**
     * calculates the blend factor for this texture stage
     * @param spoilage Current spoilage percentage (0.0-1.0)
     * @return Blend factor (0.0 = not visible, 1.0 = fully visible)
     */
    public float calculateBlendFactor(float spoilage) {
        if (spoilage <= startThreshold) {
            return 0.0f;
        }
        if (spoilage >= fullThreshold) {
            return 1.0f;
        }
        return (spoilage - startThreshold) / (fullThreshold - startThreshold);
    }

    /**
     * gets the model location for a specific block state;
     * if modelByState is defined and contains a matching key, returns that model;
     * otherwise returns the default model
     * @param state The block state to get model for
     * @return The model location for this state
     */
    public ResourceLocation getModelForState(BlockState state) {
        if (modelByState.isEmpty()) {
            return model;
        }

        Map<String, ResourceLocation> stateModels = modelByState.get();

        // build state key strings and check for matches
        for (Property<?> property : state.getProperties()) {
            String key = property.getName() + "=" + getPropertyValueName(state, property);
            if (stateModels.containsKey(key)) {
                return stateModels.get(key);
            }
        }

        return model;
    }

    /** checks if this texture stage has state-aware models defined */
    public boolean hasStateAwareModels() {
        return modelByState.isPresent() && !modelByState.get().isEmpty();
    }

    private static <T extends Comparable<T>> String getPropertyValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
