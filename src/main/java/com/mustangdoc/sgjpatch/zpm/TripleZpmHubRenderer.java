package com.mustangdoc.sgjpatch.zpm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.povstalec.sgjourney.common.init.ItemInit;
import net.povstalec.sgjourney.common.items.ZeroPointModule;

/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubRenderer implements BlockEntityRenderer<TripleZpmHubBlockEntity> {
   private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
   private static final float[][] SOCKETS = new float[][]{{4.0F, 4.0F}, {12.0F, 4.0F}, {8.0F, 12.0F}};
   private static final float MODULE_SCALE = 0.4F;
   private static final double MODULE_CENTER_Y = 0.8775;
   private final ItemRenderer itemRenderer;

   public TripleZpmHubRenderer(Context context) {
      this.itemRenderer = context.getItemRenderer();
   }

   public AABB getRenderBoundingBox(TripleZpmHubBlockEntity hub) {
      return new AABB(hub.getBlockPos()).expandTowards(0.0, 0.03, 0.0);
   }

   private static void quad(
      PoseStack poses, VertexConsumer b, int light, int overlay, int color, float[] a, float[] c, float[] d, float[] e, float nx, float ny, float nz
   ) {
      float[][] points = new float[][]{a, c, d, e};

      for (int i = 0; i < 4; i++) {
         float[] p = points[i];
         b.vertex(poses.last().pose(), p[0] / 16.0F, p[1] / 16.0F, p[2] / 16.0F)
            .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255)
            .uv(i != 1 && i != 2 ? 0.0F : 1.0F, i >= 2 ? 1.0F : 0.0F)
            .overlayCoords(overlay)
            .uv2(light)
            .normal(poses.last().normal(), nx, ny, nz).endVertex();
      }
   }

   public void render(TripleZpmHubBlockEntity hub, float partial, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
      poses.pushPose();
      poses.translate(0.5, 0.0, 0.5);

      float rotation = switch ((Direction)hub.getBlockState().getValue(TripleZpmHubBlock.FACING)) {
         case EAST -> -90.0F;
         case SOUTH -> 180.0F;
         case WEST -> 90.0F;
         default -> 0.0F;
      };
      poses.mulPose(Axis.YP.rotationDegrees(rotation));
      poses.translate(-0.5, 0.0, -0.5);
      VertexConsumer amber = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
      float[][] bars = new float[][]{{8.0F, 2.7F, 0.0F}, {3.8F, 8.5F, -45.0F}, {12.2F, 8.5F, 45.0F}};

      for (float[] bar : bars) {
         poses.pushPose();
         poses.translate((double)bar[0] / 16.0, 0.99625, (double)bar[1] / 16.0);
         poses.mulPose(Axis.YP.rotationDegrees(bar[2]));
         quad(
            poses,
            amber,
            15728880,
            overlay,
            -3014657,
            new float[]{-0.72F, 0.0F, -0.065F},
            new float[]{-0.72F, 0.0F, 0.065F},
            new float[]{0.72F, 0.0F, 0.065F},
            new float[]{0.72F, 0.0F, -0.065F},
            0.0F,
            1.0F,
            0.0F
         );
         poses.popPose();
      }

      for (int slot = 0; slot < 3; slot++) {
         ItemStack item = hub.inventory.getStackInSlot(slot);
         if (item.is((Item)ItemInit.ZPM.get())) {
            poses.pushPose();
            poses.translate((double)SOCKETS[slot][0] / 16.0, 0.8775, (double)SOCKETS[slot][1] / 16.0);
            poses.scale(0.4F, 0.4F, 0.4F);
            BakedModel model = this.itemRenderer.getModel(item, hub.getLevel(), null, 0);
            this.itemRenderer
               .render(
                  item, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, poses, buffers, ZeroPointModule.hasEnergy(item) ? 15728880 : light, overlay, model
               );
            poses.popPose();
         }
      }

      poses.popPose();
   }
}
