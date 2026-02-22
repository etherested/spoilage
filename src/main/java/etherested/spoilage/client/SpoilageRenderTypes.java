package etherested.spoilage.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

// custom render types for spoilage texture rendering
public class SpoilageRenderTypes {

    // RenderType for spoilage texture blending;
    // used to render base and rotten textures with alpha blending for smooth transitions;
    //  - LEQUAL_DEPTH_TEST: normal depth test for proper occlusion
    //  - COLOR_DEPTH_WRITE: write both color and depth for proper rendering
    //  - TRANSLUCENT_TRANSPARENCY: enable alpha blending for smooth crossfade
    public static final RenderType SPOILAGE_TEXTURE_BLEND = RenderType.create(
            "spoilage_texture_blend",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true)
    );

    // RenderType for block spoilage overlays;
    // uses polygon offset to prevent z-fighting when rendering overlay on top of original block;
    //  - POLYGON_OFFSET_LAYERING: pushes overlay slightly forward to avoid z-fighting
    //  - LEQUAL_DEPTH_TEST: normal depth test for proper occlusion
    //  - TRANSLUCENT_TRANSPARENCY: enable alpha blending for smooth overlay
    public static final RenderType SPOILAGE_BLOCK_OVERLAY = RenderType.create(
            "spoilage_block_overlay",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .createCompositeState(true)
    );
}
