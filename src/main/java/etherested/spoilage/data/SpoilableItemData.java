package etherested.spoilage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.OptionalLong;

// per-item/block spoilage definition loaded from datapacks;
// loaded from: data/<namespace>/spoilage/<name>.json;
// a single file can define spoilage for both an item and its block form;
// by default data applies to the item, set is_block: true to also apply to the block
public record SpoilableItemData(
        ResourceLocation spoilageGroup,
        OptionalLong lifetimeOverride,
        Optional<ResourceLocation> rottenReplacement,
        // whether this data applies to the block form
        boolean isBlock
) {
    public static final Codec<SpoilableItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("spoilage_group").forGetter(SpoilableItemData::spoilageGroup),
            Codec.LONG.optionalFieldOf("lifetime_override").xmap(
                    opt -> opt.map(OptionalLong::of).orElse(OptionalLong.empty()),
                    optLong -> optLong.isPresent() ? Optional.of(optLong.getAsLong()) : Optional.empty()
            ).forGetter(SpoilableItemData::lifetimeOverride),
            ResourceLocation.CODEC.optionalFieldOf("rotten_replacement").forGetter(SpoilableItemData::rottenReplacement),
            Codec.BOOL.optionalFieldOf("is_block", false).forGetter(SpoilableItemData::isBlock)
    ).apply(instance, SpoilableItemData::new));

    public SpoilableItemData(ResourceLocation spoilageGroup) {
        this(spoilageGroup, OptionalLong.empty(), Optional.empty(), false);
    }

    public SpoilableItemData(ResourceLocation spoilageGroup, OptionalLong lifetimeOverride) {
        this(spoilageGroup, lifetimeOverride, Optional.empty(), false);
    }

    public long getLifetime(SpoilageGroupData groupData) {
        return lifetimeOverride.orElse(groupData.baseLifetimeTicks());
    }
}
