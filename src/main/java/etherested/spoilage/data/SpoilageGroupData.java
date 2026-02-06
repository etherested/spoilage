package etherested.spoilage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * defines a spoilage group with shared settings;
 * loaded from: data/<namespace>/spoilage/groups/<name>.json
 */
public record SpoilageGroupData(
        long baseLifetimeTicks,
        boolean showTooltip
) {
    public static final Codec<SpoilageGroupData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("base_lifetime_ticks").forGetter(SpoilageGroupData::baseLifetimeTicks),
            Codec.BOOL.optionalFieldOf("show_tooltip", true).forGetter(SpoilageGroupData::showTooltip)
    ).apply(instance, SpoilageGroupData::new));

    public static final long DEFAULT_LIFETIME = 48000L; // 40 minutes real time at 20 TPS

    /** constructor for backwards compatibility with existing code that only specifies lifetime */
    public SpoilageGroupData(long baseLifetimeTicks) {
        this(baseLifetimeTicks, true);
    }
}
