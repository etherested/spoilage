package etherested.spoilage.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import etherested.spoilage.client.SpoilageRenderTypes;
import etherested.spoilage.client.SpoilageRottenTextureManager;
import etherested.spoilage.client.SpoilageTintHelper;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageTextureStage;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// mixin to apply spoilage visual effects during item rendering;
// rendering modes (in priority order):
// 1. texture blending: items with spoilage textures use layered rendering
//  - base texture (always full opacity)
//  - stale texture (blends in from stale_start to stale_full)
//  - rotten texture (blends in from rotten_start to rotten_full)
// 2. tint system: other spoilable items use color tinting
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Unique
    private static final ThreadLocal<ItemStack> spoilage$currentStack = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Boolean> spoilage$customRendering = ThreadLocal.withInitial(() -> false);

    @Inject(method = "renderQuadList", at = @At("HEAD"))
    private void spoilage$captureStack(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        List<BakedQuad> quads, ItemStack stack,
                                        int light, int overlay, CallbackInfo ci) {
        spoilage$currentStack.set(stack);
    }

    @Inject(method = "renderQuadList", at = @At("RETURN"))
    private void spoilage$clearStack(PoseStack poseStack, VertexConsumer vertexConsumer,
                                      List<BakedQuad> quads, ItemStack stack,
                                      int light, int overlay, CallbackInfo ci) {
        spoilage$currentStack.remove();
    }

    //? if neoforge {
    @Redirect(
            method = "renderQuadList",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"
            )
    )
    private void spoilage$redirectPutBulkData(VertexConsumer vertexConsumer, PoseStack.Pose pose,
                                               BakedQuad quad, float red, float green, float blue,
                                               float alpha, int light, int overlay, boolean readExistingColor) {
        float[] rgb = spoilage$applyTint(red, green, blue);
        vertexConsumer.putBulkData(pose, quad, rgb[0], rgb[1], rgb[2], alpha, light, overlay, readExistingColor);
    }
    //?} else {
    /*@Redirect(
            method = "renderQuadList",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"
            )
    )
    private void spoilage$redirectPutBulkData(VertexConsumer vertexConsumer, PoseStack.Pose pose,
                                               BakedQuad quad, float red, float green, float blue,
                                               float alpha, int light, int overlay) {
        float[] rgb = spoilage$applyTint(red, green, blue);
        vertexConsumer.putBulkData(pose, quad, rgb[0], rgb[1], rgb[2], alpha, light, overlay);
    }
    *///?}

    @Unique
    private float[] spoilage$applyTint(float red, float green, float blue) {
        ItemStack stack = spoilage$currentStack.get();
        if (!spoilage$customRendering.get() && stack != null && !stack.isEmpty()) {
            int tintColor = SpoilageTintHelper.getSpoilageTint(stack);
            if (tintColor != SpoilageTintHelper.NO_TINT) {
                red *= ((tintColor >> 16) & 0xFF) / 255f;
                green *= ((tintColor >> 8) & 0xFF) / 255f;
                blue *= (tintColor & 0xFF) / 255f;
            }
        }
        return new float[]{red, green, blue};
    }

    // intercept render method to handle multi-stage texture blending
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spoilage$handleTextureBlending(ItemStack stack, ItemDisplayContext displayContext,
                                                 boolean leftHand, PoseStack poseStack,
                                                 MultiBufferSource bufferSource, int light, int overlay,
                                                 BakedModel model, CallbackInfo ci) {
        if (spoilage$customRendering.get()) {
            return;
        }

        if (!SpoilageConfig.isEnabled() || !SpoilageConfig.useTextureBlending()) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        if (!SpoilageRottenTextureManager.hasSpoilageTextures(stack)) {
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
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);

        // get texture stage data
        SpoilageTextureStage staleData = SpoilageRottenTextureManager.getStaleTextureData(stack);
        SpoilageTextureStage rottenData = SpoilageRottenTextureManager.getRottenTextureData(stack);

        // get models
        BakedModel staleModel = SpoilageRottenTextureManager.getStaleModel(stack);
        BakedModel rottenModel = SpoilageRottenTextureManager.getRottenModel(stack);

        // need at least one spoilage model
        if (staleModel == null && rottenModel == null) {
            return;
        }

        ci.cancel();

        spoilage$customRendering.set(true);
        try {
            spoilage$renderLayeredTextures(poseStack, bufferSource, light, overlay,
                    model, staleModel, rottenModel, staleData, rottenData, spoilage, displayContext, leftHand);
        } finally {
            spoilage$customRendering.set(false);
        }
    }

    // renders base texture at full opacity, then stale and rotten textures layered on top;
    // each layer blends in based on its threshold range
    @Unique
    private void spoilage$renderLayeredTextures(PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int light, int overlay, BakedModel baseModel,
                                                 BakedModel staleModel, BakedModel rottenModel,
                                                 SpoilageTextureStage staleData, SpoilageTextureStage rottenData,
                                                 float spoilage, ItemDisplayContext displayContext, boolean leftHand) {
        Minecraft mc = Minecraft.getInstance();
        RandomSource random = mc.level != null ? mc.level.random : RandomSource.create();

        // calculate blend factors
        float staleBlend = staleData != null ? staleData.calculateBlendFactor(spoilage) : 0f;
        float rottenBlend = rottenData != null ? rottenData.calculateBlendFactor(spoilage) : 0f;

        // optimization: if fully rotten, only render rotten
        if (rottenBlend >= 0.999f && rottenModel != null) {
            spoilage$renderModelWithAlpha(poseStack, bufferSource, light, overlay, rottenModel,
                    displayContext, leftHand, 1.0f, random);
            return;
        }

        // optimization: if fully stale but no rotten yet, render stale only
        if (staleBlend >= 0.999f && rottenBlend <= 0.001f && staleModel != null) {
            spoilage$renderModelWithAlpha(poseStack, bufferSource, light, overlay, staleModel,
                    displayContext, leftHand, 1.0f, random);
            return;
        }

        // optimization: if no spoilage effect yet, render base only
        if (staleBlend <= 0.001f && rottenBlend <= 0.001f) {
            spoilage$renderModelWithAlpha(poseStack, bufferSource, light, overlay, baseModel,
                    displayContext, leftHand, 1.0f, random);
            return;
        }

        // layer 1: base texture at full opacity
        spoilage$renderModelWithAlpha(poseStack, bufferSource, light, overlay, baseModel,
                displayContext, leftHand, 1.0f, random);

        // layer 2: stale texture blending in
        if (staleModel != null && staleBlend > 0.001f) {
            spoilage$renderModelWithAlpha(poseStack, bufferSource, light, overlay, staleModel,
                    displayContext, leftHand, staleBlend, random);
        }

        // layer 3: rotten texture blending in on top
        if (rottenModel != null && rottenBlend > 0.001f) {
            spoilage$renderModelWithAlpha(poseStack, bufferSource, light, overlay, rottenModel,
                    displayContext, leftHand, rottenBlend, random);
        }
    }

    @Unique
    private void spoilage$renderModelWithAlpha(PoseStack poseStack, MultiBufferSource bufferSource,
                                                int light, int overlay, BakedModel model,
                                                ItemDisplayContext displayContext, boolean leftHand,
                                                float alpha, RandomSource random) {
        poseStack.pushPose();

        //? if neoforge {
        BakedModel transformedModel = model.applyTransform(displayContext, poseStack, leftHand);
        //?} else {
        /*model.getTransforms().getTransform(displayContext).apply(leftHand, poseStack);
        BakedModel transformedModel = model;
        *///?}
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(SpoilageRenderTypes.SPOILAGE_TEXTURE_BLEND);

        List<BakedQuad> quads = transformedModel.getQuads(null, null, random);
        for (BakedQuad quad : quads) {
            //? if neoforge {
            vertexConsumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, alpha, light, overlay, true);
            //?} else {
            /*vertexConsumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, alpha, light, overlay);
            *///?}
        }

        for (Direction direction : Direction.values()) {
            List<BakedQuad> sidedQuads = transformedModel.getQuads(null, direction, random);
            for (BakedQuad quad : sidedQuads) {
                //? if neoforge {
                vertexConsumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, alpha, light, overlay, true);
                //?} else {
                /*vertexConsumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, alpha, light, overlay);
                *///?}
            }
        }

        poseStack.popPose();
    }
}
