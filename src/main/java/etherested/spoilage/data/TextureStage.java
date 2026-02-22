package etherested.spoilage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

// represents a texture stage for spoilage visualization;
// items can define multiple stages that activate at different spoilage thresholds
// @param threshold spoilage percentage (0.0-1.0) at which this texture activates
// @param texture ResourceLocation of the texture to use
public record TextureStage(
        float threshold,
        ResourceLocation texture
) {
    public static final Codec<TextureStage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("threshold").forGetter(TextureStage::threshold),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(TextureStage::texture)
    ).apply(instance, TextureStage::new));
}
