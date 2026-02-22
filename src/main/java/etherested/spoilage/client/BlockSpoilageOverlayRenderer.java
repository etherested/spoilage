package etherested.spoilage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.data.SpoilageTextureStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

//? if neoforge {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}

import java.util.List;
import java.util.Map;

// renders spoilage visual effects on placed blocks;
// rendering modes (matching item behavior):
//  1. custom textures available: render stale/rotten model overlays with alpha blending
//  2. no custom textures: render the block model with tint color applied
//? if neoforge {
@EventBusSubscriber(modid = Spoilage.MODID, value = Dist.CLIENT)
//?}
public class BlockSpoilageOverlayRenderer {

    //? if neoforge {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        renderSpoilageOverlays(event.getPoseStack(), event.getCamera().getPosition());
    }
    //?} else {
    /*public static void registerFabricEvents() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            renderSpoilageOverlays(context.matrixStack(), context.camera().getPosition());
        });
    }
    *///?}

    // shared rendering logic for both loaders
    private static void renderSpoilageOverlays(PoseStack poseStack, Vec3 cameraPos) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        // get cached spoilage data
        Map<BlockPos, Float> spoilageCache = BlockSpoilageClientCache.getAll();
        if (spoilageCache.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        RandomSource random = level.random;

        // render spoilage overlays for cached blocks
        for (Map.Entry<BlockPos, Float> entry : spoilageCache.entrySet()) {
            BlockPos pos = entry.getKey();
            float spoilage = entry.getValue();

            // skip blocks with no/minimal spoilage
            if (spoilage < 0.1f) {
                continue;
            }

            // check if block is in render distance
            double distSq = pos.distToCenterSqr(cameraPos);
            if (distSq > 64 * 64) { // 64 blocks max distance
                continue;
            }

            // get block state and verify it's still valid
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            // verify block is spoilable or is a crop (crops are rendered from MATURE_CROP sync data)
            Block block = state.getBlock();
            if (!SpoilageItemRegistry.isBlockSpoilable(block) && !(block instanceof CropBlock)) {
                continue;
            }

            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

            // get light level for the block
            int light = LevelRenderer.getLightColor(level, pos);

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - cameraPos.x,
                    pos.getY() - cameraPos.y,
                    pos.getZ() - cameraPos.z
            );

            // check if this block has spoilage textures
            boolean textureRendered = false;
            if (SpoilageConfig.useTextureBlending() && SpoilageRottenTextureManager.hasBlockSpoilageTextures(blockId)) {
                // try to render custom texture overlays
                // for multi-state blocks without state-aware models, this returns false to fall back to tint
                textureRendered = renderTextureOverlays(poseStack, bufferSource, state, blockId, spoilage, light, random);
            }

            // fall back to tint overlay if texture rendering didn't happen
            if (!textureRendered && SpoilageConfig.showTintOverlay()) {
                // render tinted overlay using the block's own model
                renderTintOverlay(poseStack, bufferSource, state, spoilage, light, random, mc);
            }

            poseStack.popPose();
        }

        // flush the buffer
        bufferSource.endBatch(SpoilageRenderTypes.SPOILAGE_BLOCK_OVERLAY);
    }

    // renders custom stale/rotten texture overlays
    // @return true if texture overlays were rendered, false if should fall back to tint
    private static boolean renderTextureOverlays(PoseStack poseStack, MultiBufferSource bufferSource,
                                               BlockState state, ResourceLocation blockId,
                                               float spoilage, int light, RandomSource random) {
        // Get texture stage data
        SpoilageTextureStage staleData = SpoilageRottenTextureManager.getBlockStaleTextureData(blockId);
        SpoilageTextureStage rottenData = SpoilageRottenTextureManager.getBlockRottenTextureData(blockId);

        // for multi-state blocks, check if we have state-aware models
        // if not, fall back to tint overlay for visual consistency
        Block block = state.getBlock();
        if (hasMultipleVisualStates(block)) {
            boolean hasStateAwareStale = staleData != null && staleData.hasStateAwareModels();
            boolean hasStateAwareRotten = rottenData != null && rottenData.hasStateAwareModels();
            if (!hasStateAwareStale && !hasStateAwareRotten) {
                // no state-aware models available, fall back to tint
                return false;
            }
        }

        // get models - use state-aware lookup
        BakedModel staleModel = staleData != null ? SpoilageRottenTextureManager.getModelForState(staleData, state) : null;
        BakedModel rottenModel = rottenData != null ? SpoilageRottenTextureManager.getModelForState(rottenData, state) : null;

        if (staleModel == null && rottenModel == null) {
            return false;
        }

        // calculate blend factors
        float staleBlend = staleData != null ? staleData.calculateBlendFactor(spoilage) : 0f;
        float rottenBlend = rottenData != null ? rottenData.calculateBlendFactor(spoilage) : 0f;

        // skip if no blend needed
        if (staleBlend <= 0.001f && rottenBlend <= 0.001f) {
            return false;
        }

        // render stale overlay
        if (staleModel != null && staleBlend > 0.001f) {
            renderBlockOverlay(poseStack, bufferSource, staleModel, staleBlend, light, random, state, 1f, 1f, 1f);
        }

        // render rotten overlay
        if (rottenModel != null && rottenBlend > 0.001f) {
            renderBlockOverlay(poseStack, bufferSource, rottenModel, rottenBlend, light, random, state, 1f, 1f, 1f);
        }

        return true;
    }

    // checks if a block has multiple visual states that affect its model;
    // for these blocks, texture overlays require state-aware models to look correct
    private static boolean hasMultipleVisualStates(Block block) {
        return block instanceof CakeBlock || block instanceof CropBlock;
    }

    // renders a tinted overlay using the block's own model;
    // this provides the fallback visual for blocks without custom textures
    private static void renderTintOverlay(PoseStack poseStack, MultiBufferSource bufferSource,
                                           BlockState state, float spoilage, int light,
                                           RandomSource random, Minecraft mc) {
        // get the block's model
        BakedModel blockModel = mc.getBlockRenderer().getBlockModel(state);
        if (blockModel == null) {
            return;
        }

        // calculate tint color and alpha
        int tintColor = BlockSpoilageTintHandler.calculateBlockTintColor(spoilage);
        float r = ((tintColor >> 16) & 0xFF) / 255f;
        float g = ((tintColor >> 8) & 0xFF) / 255f;
        float b = (tintColor & 0xFF) / 255f;

        // alpha based on spoilage (subtle at 10%, stronger at 100%)
        float alpha = Math.min(0.6f, (spoilage - 0.1f) / 0.9f * 0.6f);

        // render the block model with tint as an overlay
        renderBlockOverlay(poseStack, bufferSource, blockModel, alpha, light, random, state, r, g, b);
    }

    // renders a single block overlay with the given alpha blend and color
    private static void renderBlockOverlay(PoseStack poseStack, MultiBufferSource bufferSource,
                                            BakedModel model, float alpha, int light,
                                            RandomSource random, BlockState state,
                                            float red, float green, float blue) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(SpoilageRenderTypes.SPOILAGE_BLOCK_OVERLAY);

        // render all quads from the model
        List<BakedQuad> quads = model.getQuads(state, null, random);
        for (BakedQuad quad : quads) {
            //? if neoforge {
            vertexConsumer.putBulkData(poseStack.last(), quad, red, green, blue, alpha, light, 0, true);
            //?} else {
            /*vertexConsumer.putBulkData(poseStack.last(), quad, red, green, blue, alpha, light, 0);
            *///?}
        }

        // render sided quads
        for (Direction direction : Direction.values()) {
            List<BakedQuad> sidedQuads = model.getQuads(state, direction, random);
            for (BakedQuad quad : sidedQuads) {
                //? if neoforge {
                vertexConsumer.putBulkData(poseStack.last(), quad, red, green, blue, alpha, light, 0, true);
                //?} else {
                /*vertexConsumer.putBulkData(poseStack.last(), quad, red, green, blue, alpha, light, 0);
                *///?}
            }
        }
    }
}
