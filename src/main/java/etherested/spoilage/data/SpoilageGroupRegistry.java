package etherested.spoilage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.LoggerFactory;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * registry for spoilage groups loaded from datapacks;
 * groups are loaded from: data/<namespace>/spoilage/groups/<name>.json
 */
public class SpoilageGroupRegistry extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilageGroupRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "spoilage/groups";

    private static final Map<ResourceLocation, SpoilageGroupData> GROUPS = new HashMap<>();

    public SpoilageGroupRegistry() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        GROUPS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                SpoilageGroupData data = SpoilageGroupData.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(error -> new IllegalStateException("Failed to parse spoilage group " + id + ": " + error));
                GROUPS.put(id, data);
                LOGGER.debug("Loaded spoilage group: {}", id);
            } catch (Exception e) {
                LOGGER.error("Failed to load spoilage group {}: {}", id, e.getMessage());
            }
        }

        LOGGER.info("Loaded {} spoilage groups", GROUPS.size());
    }

    @Nullable
    public static SpoilageGroupData getGroup(ResourceLocation id) {
        return GROUPS.get(id);
    }

    public static Map<ResourceLocation, SpoilageGroupData> getAllGroups() {
        return Map.copyOf(GROUPS);
    }

    public static boolean hasGroup(ResourceLocation id) {
        return GROUPS.containsKey(id);
    }
}
