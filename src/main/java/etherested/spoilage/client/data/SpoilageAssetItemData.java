package etherested.spoilage.client.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import etherested.spoilage.data.SpoilageTextureStage;
import etherested.spoilage.data.TextureStage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * client-side asset data for spoilage textures;
 * loaded from: assets/<namespace>/spoilage/<item_path>.json;
 * separates visual/texture data from server-side gameplay data;
 * supports flexible texture stage definitions with any key name;
 * any key that contains an object with a "model" field will be parsed as a texture stage
 */
public record SpoilageAssetItemData(
        Optional<List<TextureStage>> textureStages,
        // all texture stages (flexible key names)
        Map<String, SpoilageTextureStage> allStages
) {
    // reserved keys that are NOT texture stages
    private static final Set<String> RESERVED_KEYS = Set.of("texture_stages");

    public static final Codec<SpoilageAssetItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextureStage.CODEC.listOf().optionalFieldOf("texture_stages").forGetter(SpoilageAssetItemData::textureStages),
            Codec.unboundedMap(Codec.STRING, SpoilageTextureStage.CODEC).optionalFieldOf("_stages", Map.of()).forGetter(d -> Map.of())
    ).apply(instance, (ts, ignored) -> new SpoilageAssetItemData(ts, Map.of())));

    /**
     * parses asset data from JSON with flexible key support;
     * any key containing an object with a "model" field is treated as a texture stage
     */
    public static SpoilageAssetItemData fromJson(JsonElement json) {
        if (!json.isJsonObject()) {
            return new SpoilageAssetItemData(Optional.empty(), Map.of());
        }

        JsonObject obj = json.getAsJsonObject();
        Map<String, SpoilageTextureStage> stages = new HashMap<>();

        // parse texture_stages if present
        Optional<List<TextureStage>> textureStages = Optional.empty();
        if (obj.has("texture_stages")) {
            textureStages = TextureStage.CODEC.listOf()
                    .parse(JsonOps.INSTANCE, obj.get("texture_stages"))
                    .result();
        }

        // parse all other keys as potential texture stages
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();

            // skip reserved keys
            if (RESERVED_KEYS.contains(key)) {
                continue;
            }

            JsonElement value = entry.getValue();

            // check if this looks like a texture stage (has "model" field)
            if (value.isJsonObject() && value.getAsJsonObject().has("model")) {
                SpoilageTextureStage.CODEC.parse(JsonOps.INSTANCE, value)
                        .result()
                        .ifPresent(stage -> stages.put(key, stage));
            }
        }

        return new SpoilageAssetItemData(textureStages, stages);
    }

    /** gets a texture stage by key name */
    public Optional<SpoilageTextureStage> getTextureStage(String key) {
        return Optional.ofNullable(allStages.get(key));
    }

    /** gets all texture stages */
    public Map<String, SpoilageTextureStage> getAllTextureStages() {
        return allStages;
    }

    public Optional<SpoilageTextureStage> staleItemTexture() {
        return getTextureStage("stale");
    }

    public Optional<SpoilageTextureStage> rottenItemTexture() {
        return getTextureStage("rotten");
    }

    public Optional<SpoilageTextureStage> staleBlockTexture() {
        return getTextureStage("stale_block");
    }

    public Optional<SpoilageTextureStage> rottenBlockTexture() {
        return getTextureStage("rotten_block");
    }

    /** returns true if this asset data has custom texture stages defined */
    public boolean hasTextureStages() {
        return textureStages.isPresent() && !textureStages.get().isEmpty();
    }

    /**
     * returns true if this asset data has a stale texture defined (item form);
     * uses pattern matching to find any key containing "stale" but not "block"
     */
    public boolean hasStaleTexture() {
        return allStages.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("stale") && !key.toLowerCase().contains("block"));
    }

    /**
     * returns true if this asset data has a rotten texture defined (item form);
     * uses pattern matching to find any key containing "rotten" but not "block"
     */
    public boolean hasRottenTexture() {
        return allStages.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("rotten") && !key.toLowerCase().contains("block"));
    }

    /**
     * returns true if this block has a stale texture defined;
     * uses pattern matching to find any key containing both "stale" and "block"
     */
    public boolean hasStaleBlockTexture() {
        return allStages.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("stale") && key.toLowerCase().contains("block"));
    }

    /**
     * returns true if this block has a rotten texture defined;
     * uses pattern matching to find any key containing both "rotten" and "block"
     */
    public boolean hasRottenBlockTexture() {
        return allStages.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("rotten") && key.toLowerCase().contains("block"));
    }

    /**
     * gets the stale texture for items using pattern matching;
     * finds any key containing "stale" but not "block"
     */
    public Optional<SpoilageTextureStage> getStaleItemTexture() {
        return allStages.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("stale") && !e.getKey().toLowerCase().contains("block"))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * gets the rotten texture for items using pattern matching;
     * finds any key containing "rotten" but not "block"
     */
    public Optional<SpoilageTextureStage> getRottenItemTexture() {
        return allStages.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("rotten") && !e.getKey().toLowerCase().contains("block"))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /** returns true if this asset data has any spoilage textures (item form) */
    public boolean hasSpoilageTextures() {
        return hasStaleTexture() || hasRottenTexture();
    }

    /** returns true if this block has any spoilage textures */
    public boolean hasBlockSpoilageTextures() {
        return hasStaleBlockTexture() || hasRottenBlockTexture();
    }
}
