package etherested.spoilage.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// data component storing spoilage state for items
// @param creationTime world time when first viewed (-1 = uninitialized)
// @param remainingLifetime ticks remaining when paused (-1 = not paused)
// @param isPaused spoilage paused (preservation/offline)
// @param preservationMultiplier current multiplier (1.0 = normal, lower = slower spoilage)
// @param yLevelSavedTicks cumulative ticks saved from Y-level preservation
// @param lastYLevelProcessTick world tick when Y-level processing last occurred
// @param currentContainerYMultiplier current preservation multiplier while in container (1.0 = not in container/no benefit)
// @param biomeMultiplier current biome-based multiplier (1.0 = normal, < 1.0 = cold, > 1.0 = hot)
public record SpoilageData(
        long creationTime,
        long remainingLifetime,
        boolean isPaused,
        float preservationMultiplier,
        long yLevelSavedTicks,
        long lastYLevelProcessTick,
        float currentContainerYMultiplier,
        float biomeMultiplier
) {
    public static final long UNINITIALIZED = -1L;
    public static final long NOT_PAUSED = -1L;

    public static final SpoilageData DEFAULT = new SpoilageData(UNINITIALIZED, NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);

    public static final Codec<SpoilageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("creation_time").forGetter(SpoilageData::creationTime),
            Codec.LONG.fieldOf("remaining_lifetime").forGetter(SpoilageData::remainingLifetime),
            Codec.BOOL.fieldOf("is_paused").forGetter(SpoilageData::isPaused),
            Codec.FLOAT.fieldOf("preservation_multiplier").forGetter(SpoilageData::preservationMultiplier),
            Codec.LONG.optionalFieldOf("y_level_saved_ticks", 0L).forGetter(SpoilageData::yLevelSavedTicks),
            Codec.LONG.optionalFieldOf("last_y_level_process_tick", 0L).forGetter(SpoilageData::lastYLevelProcessTick),
            Codec.FLOAT.optionalFieldOf("current_container_y_multiplier", 1.0f).forGetter(SpoilageData::currentContainerYMultiplier),
            Codec.FLOAT.optionalFieldOf("biome_multiplier", 1.0f).forGetter(SpoilageData::biomeMultiplier)
    ).apply(instance, SpoilageData::new));

    public static final StreamCodec<ByteBuf, SpoilageData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SpoilageData decode(ByteBuf buf) {
            long creationTime = ByteBufCodecs.VAR_LONG.decode(buf);
            long remainingLifetime = ByteBufCodecs.VAR_LONG.decode(buf);
            boolean isPaused = ByteBufCodecs.BOOL.decode(buf);
            float preservationMultiplier = ByteBufCodecs.FLOAT.decode(buf);
            long yLevelSavedTicks = ByteBufCodecs.VAR_LONG.decode(buf);
            long lastYLevelProcessTick = ByteBufCodecs.VAR_LONG.decode(buf);
            float currentContainerYMultiplier = ByteBufCodecs.FLOAT.decode(buf);
            float biomeMultiplier = ByteBufCodecs.FLOAT.decode(buf);
            return new SpoilageData(creationTime, remainingLifetime, isPaused, preservationMultiplier,
                    yLevelSavedTicks, lastYLevelProcessTick, currentContainerYMultiplier, biomeMultiplier);
        }

        @Override
        public void encode(ByteBuf buf, SpoilageData data) {
            ByteBufCodecs.VAR_LONG.encode(buf, data.creationTime());
            ByteBufCodecs.VAR_LONG.encode(buf, data.remainingLifetime());
            ByteBufCodecs.BOOL.encode(buf, data.isPaused());
            ByteBufCodecs.FLOAT.encode(buf, data.preservationMultiplier());
            ByteBufCodecs.VAR_LONG.encode(buf, data.yLevelSavedTicks());
            ByteBufCodecs.VAR_LONG.encode(buf, data.lastYLevelProcessTick());
            ByteBufCodecs.FLOAT.encode(buf, data.currentContainerYMultiplier());
            ByteBufCodecs.FLOAT.encode(buf, data.biomeMultiplier());
        }
    };

    public boolean isInitialized() {
        return creationTime != UNINITIALIZED;
    }

    public SpoilageData initialize(long worldTime) {
        return new SpoilageData(worldTime, NOT_PAUSED, false, 1.0f, 0L, 0L, 1.0f, 1.0f);
    }

    public SpoilageData pause(long currentRemaining) {
        return new SpoilageData(creationTime, currentRemaining, true, preservationMultiplier, yLevelSavedTicks, lastYLevelProcessTick, currentContainerYMultiplier, biomeMultiplier);
    }

    public SpoilageData resume(long newCreationTime) {
        return new SpoilageData(newCreationTime, NOT_PAUSED, false, preservationMultiplier, yLevelSavedTicks, lastYLevelProcessTick, currentContainerYMultiplier, biomeMultiplier);
    }

    public SpoilageData withPreservationMultiplier(float multiplier) {
        return new SpoilageData(creationTime, remainingLifetime, isPaused, multiplier, yLevelSavedTicks, lastYLevelProcessTick, currentContainerYMultiplier, biomeMultiplier);
    }

    public SpoilageData withRemainingLifetime(long remaining) {
        return new SpoilageData(creationTime, remaining, isPaused, preservationMultiplier, yLevelSavedTicks, lastYLevelProcessTick, currentContainerYMultiplier, biomeMultiplier);
    }

    public SpoilageData addYLevelSavings(long ticksSaved, long currentTick) {
        return new SpoilageData(creationTime, remainingLifetime, isPaused, preservationMultiplier,
                yLevelSavedTicks + ticksSaved, currentTick, currentContainerYMultiplier, biomeMultiplier);
    }

    public SpoilageData withContainerYMultiplier(float yMultiplier, long currentTick) {
        return new SpoilageData(creationTime, remainingLifetime, isPaused, preservationMultiplier,
                yLevelSavedTicks, currentTick, yMultiplier, biomeMultiplier);
    }

    public SpoilageData clearContainerYMultiplier() {
        return new SpoilageData(creationTime, remainingLifetime, isPaused, preservationMultiplier,
                yLevelSavedTicks, lastYLevelProcessTick, 1.0f, biomeMultiplier);
    }

    public SpoilageData withBiomeMultiplier(float biome) {
        return new SpoilageData(creationTime, remainingLifetime, isPaused, preservationMultiplier,
                yLevelSavedTicks, lastYLevelProcessTick, currentContainerYMultiplier, biome);
    }

    public SpoilageData withContainerPreservation(float containerMultiplier, float biome, long currentTick) {
        return new SpoilageData(creationTime, remainingLifetime, isPaused, preservationMultiplier,
                yLevelSavedTicks, currentTick, containerMultiplier, biome);
    }
}
