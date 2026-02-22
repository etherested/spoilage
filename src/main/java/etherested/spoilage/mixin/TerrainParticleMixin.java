package etherested.spoilage.mixin;

import etherested.spoilage.client.BlockSpoilageClientCache;
import etherested.spoilage.client.SpoilageRottenTextureManager;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageTextureStage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mixin to apply spoilage textures/tint to block break and crack particles;
// when breaking a spoiled food block, particles will show the appropriate spoilage texture,
// or fall back to a tint reflecting the spoilage level
@Mixin(TerrainParticle.class)
public abstract class TerrainParticleMixin extends TextureSheetParticle {

    @Shadow
    @Final
    private BlockPos pos;

    @Shadow
    @Final
    private float uo;

    @Shadow
    @Final
    private float vo;

    // replacement sprite for spoilage texture blending; null if not replaced
    @Unique
    private TextureAtlasSprite spoilage$replacementSprite;

    protected TerrainParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    // applies spoilage texture or tint to terrain particles after construction;
    // injected at the end of the constructor that both destroy() and crack() use
    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("RETURN"))
    private void spoilage$applySpoilageToParticle(ClientLevel level, double x, double y, double z,
                                                   double xSpeed, double ySpeed, double zSpeed,
                                                   BlockState state, BlockPos pos, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        if (this.pos == null || !BlockSpoilageClientCache.hasSpoilage(this.pos)) {
            return;
        }

        float spoilage = BlockSpoilageClientCache.getSpoilage(this.pos);
        if (spoilage < 0.2f) {
            return;
        }

        // try to use spoilage texture for particles (when texture blending is enabled)
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (SpoilageConfig.useTextureBlending() && SpoilageRottenTextureManager.hasBlockSpoilageTextures(blockId)) {
            SpoilageTextureStage rottenData = SpoilageRottenTextureManager.getBlockRottenTextureData(blockId);
            SpoilageTextureStage staleData = SpoilageRottenTextureManager.getBlockStaleTextureData(blockId);

            BakedModel textureModel = null;

            // check rotten first (higher priority when fully rotten)
            if (rottenData != null && rottenData.calculateBlendFactor(spoilage) >= 0.5f) {
                textureModel = SpoilageRottenTextureManager.getModelForState(rottenData, state);
            }
            // fall back to stale if not rotten enough
            else if (staleData != null && staleData.calculateBlendFactor(spoilage) >= 0.5f) {
                textureModel = SpoilageRottenTextureManager.getModelForState(staleData, state);
            }

            if (textureModel != null) {
                TextureAtlasSprite newSprite = textureModel.getParticleIcon();
                if (newSprite != null) {
                    // store replacement sprite for UV method overrides
                    this.spoilage$replacementSprite = newSprite;
                    return;
                }
            }
        }

        // fallback: apply tint for blocks without spoilage textures
        spoilage$applyBlockTint(spoilage);
    }

    // overrides U0 to use replacement sprite when spoilage texture is active;
    // this bypasses the inherited sprite field which setSprite may not reliably update
    @Inject(method = "getU0", at = @At("HEAD"), cancellable = true)
    private void spoilage$getU0(CallbackInfoReturnable<Float> cir) {
        if (this.spoilage$replacementSprite != null) {
            cir.setReturnValue(this.spoilage$replacementSprite.getU((this.uo + 1.0F) / 4.0F));
        }
    }

    // overrides U1 to use replacement sprite
    @Inject(method = "getU1", at = @At("HEAD"), cancellable = true)
    private void spoilage$getU1(CallbackInfoReturnable<Float> cir) {
        if (this.spoilage$replacementSprite != null) {
            cir.setReturnValue(this.spoilage$replacementSprite.getU(this.uo / 4.0F));
        }
    }

    // overrides V0 to use replacement sprite
    @Inject(method = "getV0", at = @At("HEAD"), cancellable = true)
    private void spoilage$getV0(CallbackInfoReturnable<Float> cir) {
        if (this.spoilage$replacementSprite != null) {
            cir.setReturnValue(this.spoilage$replacementSprite.getV(this.vo / 4.0F));
        }
    }

    // overrides V1 to use replacement sprite
    @Inject(method = "getV1", at = @At("HEAD"), cancellable = true)
    private void spoilage$getV1(CallbackInfoReturnable<Float> cir) {
        if (this.spoilage$replacementSprite != null) {
            cir.setReturnValue(this.spoilage$replacementSprite.getV((this.vo + 1.0F) / 4.0F));
        }
    }

    @Unique
    private void spoilage$applyBlockTint(float spoilage) {
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
