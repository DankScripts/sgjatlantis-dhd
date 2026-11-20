package com.mustangdoc.sgjpatch.database;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = DatabaseConsoleRegistry.NS, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DatabaseConsoleRenderer implements BlockEntityRenderer<DatabaseConsoleEntity> {
    private static final int[][] FACES = {
            {0,1,2,3}, {4,5,6,7}, {1,4,7,2}, {5,0,3,6}, {1,0,5,4}, {7,6,3,2}
    };
    private static final int GLASS = DatabaseConsoleMesh.TEXTURES.length - 1;
    private static final RenderType[] MATERIALS = new RenderType[DatabaseConsoleMesh.TEXTURES.length];
    static {
        for (int i = 0; i < MATERIALS.length; i++) {
            var texture = new ResourceLocation(DatabaseConsoleRegistry.NS,
                    "textures/block/database_console/" + DatabaseConsoleMesh.TEXTURES[i]);
            MATERIALS[i] = i == GLASS ? RenderType.entityTranslucentCull(texture) : RenderType.entityCutout(texture);
        }
    }

    public DatabaseConsoleRenderer(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DatabaseConsoleRegistry.ENTITY, DatabaseConsoleRenderer::new);
    }

    @Override public void render(DatabaseConsoleEntity console, float partialTick, PoseStack stack,
                                 MultiBufferSource buffers, int light, int overlay) {
        stack.pushPose();
        try {
            stack.translate(.5, 0, .5);
            int turns = DatabaseConsoleBlock.turns(console.getBlockState().getValue(HorizontalDirectionalBlock.FACING));
            stack.mulPose(Axis.YP.rotationDegrees(-90 * turns));
            stack.translate(-DatabaseConsoleBlock.WIDTH / 2.0, 0, -DatabaseConsoleBlock.DEPTH / 2.0);
            for (float[] cube : DatabaseConsoleMesh.CUBES) {
                for (int face = 0; face < FACES.length; face++) {
                    int material = (int) cube[24 + face];
                    if (material == -1) continue; // Internal plinth seam.
                    int[] indices = FACES[face];
                    VertexConsumer output = buffers.getBuffer(MATERIALS[material]);
                    Vector3f a = point(cube, indices[0]);
                    Vector3f normal = point(cube, indices[1]).sub(a)
                            .cross(point(cube, indices[2]).sub(a)).normalize();
                    int faceLight = material >= 3 && material != 4 ? LightTexture.FULL_BRIGHT : light;
                    float uMax = material == GLASS && face > 1 ? 1f / 128 : 1f;
                    float vMax = material == GLASS && face > 1 ? 1f / 48 : 1f;
                    for (int i = 0; i < 4; i++) {
                        Vector3f p = point(cube, indices[i]);
                        vertex(output, stack.last(), p, i == 0 || i == 3 ? 0 : uMax,
                                i < 2 ? vMax : 0, overlay, faceLight, normal);
                    }
                }
            }
        } finally {
            stack.popPose();
        }
    }

    private static Vector3f point(float[] cube, int index) {
        return new Vector3f(cube[3 * index] / 16, cube[3 * index + 1] / 16, cube[3 * index + 2] / 16);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vector3f p, float u, float v,
                               int overlay, int light, Vector3f normal) {
        out.vertex(pose.pose(), p.x, p.y, p.z).color(255,255,255,255).uv(u,v)
                .overlayCoords(overlay).uv2(light).normal(pose.normal(), normal.x, normal.y, normal.z).endVertex();
    }
}
