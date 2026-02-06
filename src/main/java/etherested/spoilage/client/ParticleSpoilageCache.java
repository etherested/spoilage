package etherested.spoilage.client;

import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/** cache for spoilage info to avoid repeated calculations when many particles spawn */
public class ParticleSpoilageCache {

    private static final Map<ItemStack, CachedInfo> cache = new WeakHashMap<>();

    public static CachedInfo get(ItemStack stack, long worldTime) {
        CachedInfo cached = cache.get(stack);
        // cache valid for 5 ticks (many particles spawn in one tick)
        if (cached != null && worldTime - cached.cacheTime < 5) {
            return cached;
        }

        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);
        BakedModel staleModel = SpoilageRottenTextureManager.getStaleModel(stack);
        BakedModel rottenModel = SpoilageRottenTextureManager.getRottenModel(stack);

        CachedInfo info = new CachedInfo(spoilage, worldTime, staleModel, rottenModel);
        cache.put(stack, info);
        return info;
    }

    public static class CachedInfo {
        public final float spoilage;
        public final long cacheTime;
        public final BakedModel staleModel;
        public final BakedModel rottenModel;

        public CachedInfo(float spoilage, long cacheTime, BakedModel staleModel, BakedModel rottenModel) {
            this.spoilage = spoilage;
            this.cacheTime = cacheTime;
            this.staleModel = staleModel;
            this.rottenModel = rottenModel;
        }
    }
}
