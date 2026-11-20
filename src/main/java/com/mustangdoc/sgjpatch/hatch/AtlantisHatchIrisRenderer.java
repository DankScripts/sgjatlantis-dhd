package com.mustangdoc.sgjpatch.hatch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.client.models.block_entity.IrisModel;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

/** Reuses SGJourney's Milky Way iris blades, laid horizontally across the roof opening. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AtlantisHatchIrisRenderer implements BlockEntityRenderer<AtlantisHatchIrisBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("sgjourney", "textures/entity/stargate/iris/iron_iris.png");
    private static final ResourceLocation RIM_TEXTURE = new ResourceLocation("minecraft", "textures/block/iron_block.png");
    private final IrisModel iris = new IrisModel(false, AtlantisHatchIrisGeometry.OPEN_DEGREES);
    private final IrisInfo info = new IrisInfo(null) {
        @Override public Optional<ResourceLocation> getIrisTexture() { return Optional.of(TEXTURE); }
    };
    public AtlantisHatchIrisRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AtlantisHatchRegistry.IRIS_ENTITY, AtlantisHatchIrisRenderer::new);
    }
    @Override public boolean shouldRenderOffScreen(AtlantisHatchIrisBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 192; }
    @Override public void render(AtlantisHatchIrisBlockEntity be, float partial, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, .5, .5);
        // A stationary rim covers the blade pivots and seals the stepped opening corners.
        VertexConsumer rim = buffers.getBuffer(RenderType.entityCutoutNoCull(RIM_TEXTURE));
        for (int i = 0; i < 80; i++) {
            double a = i * Math.PI / 40, b = (i + 1) * Math.PI / 40;
            double oa = Math.min(6.5 / Math.max(Math.abs(Math.cos(a)), Math.abs(Math.sin(a))),
                    10.0 / (Math.abs(Math.cos(a)) + Math.abs(Math.sin(a))));
            double ob = Math.min(6.5 / Math.max(Math.abs(Math.cos(b)), Math.abs(Math.sin(b))),
                    10.0 / (Math.abs(Math.cos(b)) + Math.abs(Math.sin(b))));
            // Close the vertical edges as well as the top and underside of the rim.
            for (int edge = 0; edge < 2; edge++) {
                double ra = edge == 0 ? 6.25 : oa, rb = edge == 0 ? 6.25 : ob;
                float nx = (float)Math.cos((a+b)/2) * (edge == 0 ? -1 : 1);
                float nz = (float)Math.sin((a+b)/2) * (edge == 0 ? -1 : 1);
                edgeVertex(rim, pose, ra*Math.cos(a), -.09f, ra*Math.sin(a), 0, 0, light, overlay, nx, nz);
                edgeVertex(rim, pose, rb*Math.cos(b), -.09f, rb*Math.sin(b), 1, 0, light, overlay, nx, nz);
                edgeVertex(rim, pose, rb*Math.cos(b), .09f, rb*Math.sin(b), 1, 1, light, overlay, nx, nz);
                edgeVertex(rim, pose, ra*Math.cos(a), .09f, ra*Math.sin(a), 0, 1, light, overlay, nx, nz);
            }
            for (int side = 0; side < 2; side++) {
                float y = side == 0 ? .09f : -.09f;
                vertex(rim, pose, 6.25*Math.cos(a), y, 6.25*Math.sin(a), 0, 0, light, overlay, side);
                vertex(rim, pose, oa*Math.cos(a), y, oa*Math.sin(a), .05f, 0, light, overlay, side);
                vertex(rim, pose, ob*Math.cos(b), y, ob*Math.sin(b), .05f, .15f, light, overlay, side);
                vertex(rim, pose, 6.25*Math.cos(b), y, 6.25*Math.sin(b), 0, .15f, light, overlay, side);
            }
        }
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(AtlantisHatchIrisGeometry.SCALE, AtlantisHatchIrisGeometry.SCALE, 1);
        iris.renderIris(() -> info, pose, buffers, light, overlay, 58 * (1 - be.progress(partial)));
        pose.popPose();
    }
    private static void edgeVertex(VertexConsumer out, PoseStack pose, double x, float y, double z,
            float u, float v, int light, int overlay, float nx, float nz) {
        out.vertex(pose.last().pose(), (float)x, y, (float)z).color(165, 184, 190, 255)
                .uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(pose.last().normal(), nx, 0, nz).endVertex();
    }
    private static void vertex(VertexConsumer out, PoseStack pose, double x, float y, double z,
            float u, float v, int light, int overlay, int side) {
        out.vertex(pose.last().pose(), (float)x, y, (float)z).color(165, 184, 190, 255)
                .uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(pose.last().normal(), 0, side == 0 ? 1 : -1, 0).endVertex();
    }
}
