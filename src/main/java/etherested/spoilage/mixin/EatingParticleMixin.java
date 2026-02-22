package etherested.spoilage.mixin;

import etherested.spoilage.client.ParticleSpoilageCache;
import etherested.spoilage.client.SpoilageRottenTextureManager;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageTextureStage;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mixin to apply spoilage textures/tint to eating particles;
// when eating spoiled food, particles will show the appropriate spoilage texture,
// or fall back to a tint reflecting the spoilage level
@Mixin(BreakingItemParticle.class)
public abstract class EatingParticleMixin extends TextureSheetParticle {

    protected EatingParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    // applies spoilage texture or tint to the particle after it's constructed;
    // particles sync with the currently displayed texture based on spoilage level
    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDLnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"))
    private void spoilage$applyRottenTextureToParticle(ClientLevel level, double x, double y, double z,
                                                        ItemStack stack, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        if (SpoilageCalculator.getInitializedData(stack) == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        long worldTime = mc.level.getGameTime();
        ParticleSpoilageCache.CachedInfo cached = ParticleSpoilageCache.get(stack, worldTime);
        float spoilage = cached.spoilage;

        // try to use spoilage texture for particles (when texture blending is enabled)
        if (SpoilageConfig.useTextureBlending() && SpoilageRottenTextureManager.hasSpoilageTextures(stack)) {
            SpoilageTextureStage rottenData = SpoilageRottenTextureManager.getRottenTextureData(stack);
            SpoilageTextureStage staleData = SpoilageRottenTextureManager.getStaleTextureData(stack);

            BakedModel textureModel = null;

            // check rotten first (higher priority when fully rotten)
            if (rottenData != null && rottenData.calculateBlendFactor(spoilage) >= 0.5f) {
                textureModel = cached.rottenModel;
            }
            // fall back to stale if not rotten enough
            else if (staleData != null && staleData.calculateBlendFactor(spoilage) >= 0.5f) {
                textureModel = cached.staleModel;
            }

            if (textureModel != null) {
                TextureAtlasSprite newSprite = textureModel.getParticleIcon();
                if (newSprite != null) {
                    this.setSprite(newSprite);
                    return;  // skip tint when using custom texture
                }
            }
        }

        // fallback: apply tint for items without spoilage textures
        if (spoilage >= 0.2f) {
            spoilage$applyTint(spoilage);
        }
    }

    @Unique
    private void spoilage$applyTint(float spoilage) {
        int tintColor = spoilage$calculateParticleTint(spoilage);
        float tintR = ((tintColor >> 16) & 0xFF) / 255f;
        float tintG = ((tintColor >> 8) & 0xFF) / 255f;
        float tintB = (tintColor & 0xFF) / 255f;
        this.rCol *= tintR;
        this.gCol *= tintG;
        this.bCol *= tintB;
    }

    // calculates rotten-style tint for particles;
    // makes particles look decayed and moldy
    @Unique
    private int spoilage$calculateParticleTint(float spoilage) {
        // normalize spoilage from 0.2-1.0 range to 0-1 range
        float t = (spoilage - 0.2f) / 0.8f;
        t = Math.max(0f, Math.min(1f, t));

        int r, g, b;

        if (t < 0.5f) {
            // slight pale/sickly tint -> greenish
            float factor = t * 2f;
            r = (int) (255 - (40 * factor));
            g = (int) (255 - (20 * factor));
            b = (int) (255 - (60 * factor));
        } else {
            // greenish -> brown/moldy green
            float factor = (t - 0.5f) * 2f;
            r = (int) (215 - (70 * factor));
            g = (int) (235 - (50 * factor));
            b = (int) (195 - (100 * factor));
        }

        return (r << 16) | (g << 8) | b;
    }
}
