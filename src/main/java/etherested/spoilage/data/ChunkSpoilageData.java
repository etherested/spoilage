package etherested.spoilage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * stores spoilage data for blocks in a chunk,
 * used for cakes, placed food, and growing crops
 */
public class ChunkSpoilageData extends SavedData {

    private final Map<BlockPos, BlockSpoilageEntry> entries = new HashMap<>();

    public ChunkSpoilageData() {
    }

    public static ChunkSpoilageData load(CompoundTag tag, HolderLookup.Provider provider) {
        ChunkSpoilageData data = new ChunkSpoilageData();

        if (tag.contains("entries", Tag.TAG_LIST)) {
            ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                BlockPos pos = new BlockPos(
                        entryTag.getInt("x"),
                        entryTag.getInt("y"),
                        entryTag.getInt("z")
                );

                BlockSpoilageEntry entry = BlockSpoilageEntry.CODEC.parse(NbtOps.INSTANCE, entryTag)
                        .result()
                        .orElse(null);

                if (entry != null) {
                    data.entries.put(pos, entry);
                }
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();

        for (Map.Entry<BlockPos, BlockSpoilageEntry> entry : entries.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockSpoilageEntry spoilage = entry.getValue();

            Tag entryTag = BlockSpoilageEntry.CODEC.encodeStart(NbtOps.INSTANCE, spoilage)
                    .result()
                    .orElse(new CompoundTag());

            if (entryTag instanceof CompoundTag compound) {
                compound.putInt("x", pos.getX());
                compound.putInt("y", pos.getY());
                compound.putInt("z", pos.getZ());
                list.add(compound);
            }
        }

        tag.put("entries", list);
        return tag;
    }

    public void setEntry(BlockPos pos, BlockSpoilageEntry entry) {
        entries.put(pos, entry);
        setDirty();
    }

    public BlockSpoilageEntry getEntry(BlockPos pos) {
        return entries.get(pos);
    }

    public void removeEntry(BlockPos pos) {
        if (entries.remove(pos) != null) {
            setDirty();
        }
    }

    public boolean hasEntry(BlockPos pos) {
        return entries.containsKey(pos);
    }

    public Map<BlockPos, BlockSpoilageEntry> getAllEntries() {
        return entries;
    }

    /**
     * entry storing spoilage data for a single block position;
     * fullyGrownTime is -1 if not fully grown, else world time when reached max age
     */
    public record BlockSpoilageEntry(
            long creationTime,
            float initialSpoilage,
            boolean isPaused,
            BlockType type,
            long fullyGrownTime
    ) {
        public static final Codec<BlockSpoilageEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("creation_time").forGetter(BlockSpoilageEntry::creationTime),
                Codec.FLOAT.fieldOf("initial_spoilage").forGetter(BlockSpoilageEntry::initialSpoilage),
                Codec.BOOL.fieldOf("is_paused").forGetter(BlockSpoilageEntry::isPaused),
                Codec.STRING.fieldOf("type").xmap(BlockType::valueOf, BlockType::name).forGetter(BlockSpoilageEntry::type),
                Codec.LONG.optionalFieldOf("fully_grown_time", -1L).forGetter(BlockSpoilageEntry::fullyGrownTime)
        ).apply(instance, BlockSpoilageEntry::new));

        /** creates a new entry for a placed block (cake, etc.) */
        public static BlockSpoilageEntry create(long worldTime, BlockType type) {
            return new BlockSpoilageEntry(worldTime, 0.0f, false, type, -1L);
        }

        /**
         * creates a new entry with inherited spoilage that continues to spoil;
         * used for placed cakes and other blocks where spoilage should continue
         */
        public static BlockSpoilageEntry createWithSpoilage(long worldTime, float initialSpoilage, BlockType type) {
            return new BlockSpoilageEntry(worldTime, initialSpoilage, false, type, -1L);
        }

        /**
         * creates a new entry for a crop that just became fully grown;
         * the fresh period timer starts from this moment
         */
        public static BlockSpoilageEntry createMatureCrop(long worldTime) {
            return new BlockSpoilageEntry(worldTime, 0.0f, false, BlockType.MATURE_CROP, worldTime);
        }

        /**
         * creates a new entry for tracking a growing crop;
         * stores seed spoilage which is used if harvested before maturity;
         * when crop matures it becomes fresh and the stored spoilage is ignored
         */
        public static BlockSpoilageEntry createGrowingCrop(long worldTime, float seedSpoilage) {
            return new BlockSpoilageEntry(worldTime, seedSpoilage, false, BlockType.CROP, -1L);
        }

        /** calculates current spoilage percentage based on elapsed time */
        public float getSpoilage(long worldTime, long lifetime) {
            if (isPaused) {
                return initialSpoilage;
            }

            long elapsed = worldTime - creationTime;
            float spoilageFromTime = (float) elapsed / lifetime;
            return Math.min(1.0f, initialSpoilage + spoilageFromTime);
        }

        /** returns a new entry with paused state changed */
        public BlockSpoilageEntry withPaused(boolean paused) {
            return new BlockSpoilageEntry(creationTime, initialSpoilage, paused, type, fullyGrownTime);
        }

        /** returns a new entry reset to fresh (for harvested crops) */
        public BlockSpoilageEntry resetToFresh(long worldTime) {
            return new BlockSpoilageEntry(worldTime, 0.0f, false, type, -1L);
        }

        /** returns true if the crop has reached full maturity */
        public boolean isFullyGrown() {
            return fullyGrownTime >= 0;
        }

        /** returns a new entry marked as fully grown at the specified time */
        public BlockSpoilageEntry markFullyGrown(long worldTime) {
            return new BlockSpoilageEntry(creationTime, 0.0f, false, BlockType.MATURE_CROP, worldTime);
        }

        /**
         * returns a new entry with fullyGrownTime reset (for bone meal);
         * keeps the crop as MATURE_CROP but restarts the fresh timer
         */
        public BlockSpoilageEntry resetFullyGrownTime(long worldTime) {
            return new BlockSpoilageEntry(creationTime, 0.0f, false, BlockType.MATURE_CROP, worldTime);
        }

        /**
         * calculates the rot progress for a fully grown crop;
         * returns 0.0 during fresh period, then increases from 0.0 to 1.0 during rot period
         */
        public float getRotProgress(long worldTime, long freshPeriod, long rotPeriod) {
            if (!isFullyGrown()) {
                return 0.0f;
            }

            long elapsed = worldTime - fullyGrownTime;

            // still in fresh period
            if (elapsed <= freshPeriod) {
                return 0.0f;
            }

            // in rot period
            long rotElapsed = elapsed - freshPeriod;
            if (rotPeriod <= 0) {
                return rotElapsed > 0 ? 1.0f : 0.0f;
            }

            return Math.min(1.0f, (float) rotElapsed / rotPeriod);
        }
    }

    /** type of block being tracked for spoilage */
    public enum BlockType {
        CAKE,
        CROP,
        BLOCK,       // generic spoilable block (cakes, placed food, etc.)
        MATURE_CROP  // fully grown crop that can rot
    }

    public static Factory<ChunkSpoilageData> factory() {
        return new Factory<>(ChunkSpoilageData::new, ChunkSpoilageData::load);
    }
}
