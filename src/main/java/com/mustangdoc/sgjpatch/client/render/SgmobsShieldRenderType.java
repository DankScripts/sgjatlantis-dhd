package com.mustangdoc.sgjpatch.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Translucent shield drawn over the wormhole without masking its depth. */
public final class SgmobsShieldRenderType extends RenderType {
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    private SgmobsShieldRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
            int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorted, setup, clear);
    }

    public static RenderType shield(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, key -> create("atlantis_sgmobs_energy_iris",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 256, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(true)));
    }
}
