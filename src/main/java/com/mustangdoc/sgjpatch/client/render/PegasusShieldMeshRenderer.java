package com.mustangdoc.sgjpatch.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import com.mustangdoc.sgjpatch.client.sound.PegasusShieldHumController;
import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.povstalec.sgjourney.client.render.SGJourneyRenderTypes;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface;
import org.apache.logging.log4j.LogManager;

public final class PegasusShieldMeshRenderer {
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
      "sgjatlantis_dhd", "models/entity/stargate/shield/pegasus_upgraded_shield.obj"
   );
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "sgjatlantis_dhd", "textures/entity/stargate/shield/pegasus_upgraded_shield.jpg"
   );
   private static PegasusShieldMeshRenderer.Mesh mesh;
   private static boolean loadFailed;

   private PegasusShieldMeshRenderer() {
   }

   /** Lantea's controller pose is rooted at the base block, not the gate centre. */
   public static void renderLantea(PoseStack poses, MultiBufferSource buffers, int light, Direction facing) {
      if (mesh == null && !loadFailed) {
         try { mesh = loadMesh(); }
         catch (RuntimeException | IOException error) {
            loadFailed = true;
            LogManager.getLogger("SGJPATCH").error("Failed to load energy iris mesh", error);
         }
      }
      if (mesh == null) return;
      poses.pushPose();
      poses.translate(0.5, 3.5, 0.5);
      poses.mulPose(Axis.YP.rotationDegrees(switch (facing) {
         case SOUTH -> 180.0F;
         case EAST -> -90.0F;
         case WEST -> 90.0F;
         default -> 0.0F;
      }));
      // The gate's front points along local negative Z. The OBJ was authored
      // on positive Z, behind Lantea's opaque event horizon.
      poses.scale(1.0F, 1.0F, -1.0F);

      ShieldTextureFlow.beginFrame();
         VertexConsumer consumer = buffers.getBuffer(LanteaShieldRenderType.shield(TEXTURE));
      Pose pose = poses.last();
      for (List<Vertex> triangle : mesh.triangles()) {
         emitFixed(consumer, pose, triangle.get(0), light, false);
         emitFixed(consumer, pose, triangle.get(1), light, false);
         emitFixed(consumer, pose, triangle.get(2), light, false);
         emitFixed(consumer, pose, triangle.get(0), light, true);
         emitFixed(consumer, pose, triangle.get(2), light, true);
         emitFixed(consumer, pose, triangle.get(1), light, true);
      }
      poses.popPose();
   }

   /** SG Mobs draws its shield before the wormhole and writes depth, obscuring the pool. */
   public static void renderSgmobs(Interface gate, PoseStack poses, MultiBufferSource buffers,
                                   int light, Direction facing) {
      if (gate == null || !PegasusShieldPowerHelper.hasShield(gate.irisInfo())) return;
      PegasusShieldTransitionHelper.Transition transition = PegasusShieldTransitionHelper.update(gate);
      if (transition.reveal() <= 0.001F) return;
      if (mesh == null && !loadFailed) {
         try { mesh = loadMesh(); }
         catch (RuntimeException | IOException error) {
            loadFailed = true;
            LogManager.getLogger("SGJPATCH").error("Failed to load SG Mobs energy iris mesh", error);
         }
      }
      if (mesh == null) return;
      poses.pushPose();
      poses.translate(0.5, 3.625, 0.5);
      poses.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
      // Match the native energy-shield transform: its OBJ faces the opposite
      // direction from the ring. Without this turn it sits behind the pool.
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
      poses.scale(1.0625F, 1.0625F, 1.0F);
      ShieldTextureFlow.beginFrame();
         VertexConsumer consumer = buffers.getBuffer(SgmobsShieldRenderType.shield(TEXTURE));
      Pose pose = poses.last();
      for (List<Vertex> triangle : mesh.triangles()) {
         emit(consumer, pose, triangle.get(0), light, transition, false, 1.0F);
         emit(consumer, pose, triangle.get(1), light, transition, false, 1.0F);
         emit(consumer, pose, triangle.get(2), light, transition, false, 1.0F);
         emit(consumer, pose, triangle.get(0), light, transition, true, 1.0F);
         emit(consumer, pose, triangle.get(2), light, transition, true, 1.0F);
         emit(consumer, pose, triangle.get(1), light, transition, true, 1.0F);
      }
      poses.popPose();
   }

   private static void emitFixed(VertexConsumer consumer, Pose pose, Vertex vertex, int light, boolean reverse) {
      float direction = reverse ? -1.0F : 1.0F;
      consumer.addVertex(pose.pose(), vertex.position()[0], vertex.position()[1], vertex.position()[2])
         .setColor(1.0F, 1.0F, 1.0F, 0.9F)
         .setUv(ShieldTextureFlow.u(vertex.uv()[0], vertex.uv()[1]), ShieldTextureFlow.v(vertex.uv()[0], vertex.uv()[1]))
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, direction * vertex.normal()[0], direction * vertex.normal()[1], direction * vertex.normal()[2]);
   }

   public static boolean render(Interface irisStargate, PoseStack poses, MultiBufferSource buffers, int light) {
      if (irisStargate != null && PegasusShieldPowerHelper.hasShield(irisStargate.irisInfo())) {
         if (irisStargate instanceof PegasusStargateEntity gate) {
            PegasusShieldHumController.sync(gate);
         }
         PegasusShieldTransitionHelper.Transition transition = PegasusShieldTransitionHelper.update(irisStargate);
         if (transition.reveal() <= 0.001F) {
            return true;
         }

         if (mesh == null && !loadFailed) {
            try {
               mesh = loadMesh();
            } catch (RuntimeException | IOException var10) {
               loadFailed = true;
               LogManager.getLogger("SGJPATCH").error("Failed to load Pegasus shield mesh", var10);
            }
         }

         if (mesh == null) {
            return true;
         }

         ShieldTextureFlow.beginFrame();
         VertexConsumer consumer = buffers.getBuffer(SGJourneyRenderTypes.shield(TEXTURE));
         Pose pose = poses.last();

         for (List<PegasusShieldMeshRenderer.Vertex> triangle : mesh.triangles()) {
            emit(consumer, pose, triangle.get(0), light, transition, false, 1.0F);
            emit(consumer, pose, triangle.get(1), light, transition, false, 1.0F);
            emit(consumer, pose, triangle.get(2), light, transition, false, 1.0F);
            emit(consumer, pose, triangle.get(0), light, transition, true, 1.0F);
            emit(consumer, pose, triangle.get(2), light, transition, true, 1.0F);
            emit(consumer, pose, triangle.get(1), light, transition, true, 1.0F);
         }

         return true;
      }

      return false;
   }

   private static void emit(
      VertexConsumer consumer,
      Pose pose,
      PegasusShieldMeshRenderer.Vertex vertex,
      int light,
      PegasusShieldTransitionHelper.Transition transition,
      boolean reverse,
      float alphaScale
   ) {
      float direction = reverse ? -1.0F : 1.0F;
      consumer.addVertex(pose.pose(), vertex.position()[0], vertex.position()[1], vertex.position()[2])
         .setColor(1.0F, 1.0F, 1.0F, alphaScale * transition.vertexAlpha(vertex.position()))
         .setUv(ShieldTextureFlow.u(vertex.uv()[0], vertex.uv()[1]), ShieldTextureFlow.v(vertex.uv()[0], vertex.uv()[1]))
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, direction * vertex.normal()[0], direction * vertex.normal()[1], direction * vertex.normal()[2]);
   }

   private static PegasusShieldMeshRenderer.Mesh loadMesh() throws IOException {
      List<float[]> positions = new ArrayList<>();
      List<float[]> coordinates = new ArrayList<>();
      List<float[]> normals = new ArrayList<>();
      List<List<PegasusShieldMeshRenderer.VertexRef>> faces = new ArrayList<>();
      positions.add(new float[]{0.0F, 0.0F, 0.0F});
      coordinates.add(new float[]{0.5F, 0.5F});
      normals.add(new float[]{0.0F, 0.0F, 1.0F});
      BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().open(MODEL), StandardCharsets.UTF_8));

      String line;
      try {
         while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            String triangles = parts[0];
            switch (triangles) {
               case "v":
                  positions.add(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                  break;
               case "vt":
                  coordinates.add(new float[]{Float.parseFloat(parts[1]), 1.0F - Float.parseFloat(parts[2])});
                  break;
               case "vn":
                  normals.add(normalize(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])}));
                  break;
               case "f":
                  List<PegasusShieldMeshRenderer.VertexRef> face = new ArrayList<>();

                  for (int index = 1; index < parts.length; index++) {
                     String[] reference = parts[index].split("/", -1);
                     face.add(
                        new PegasusShieldMeshRenderer.VertexRef(
                           Integer.parseInt(reference[0]),
                           reference.length > 1 && !reference[1].isEmpty() ? Integer.parseInt(reference[1]) : 0,
                           reference.length > 2 && !reference[2].isEmpty() ? Integer.parseInt(reference[2]) : 0
                        )
                     );
                  }

                  faces.add(face);
            }
         }
      } catch (Throwable var13) {
         try {
            reader.close();
         } catch (Throwable var12) {
            var13.addSuppressed(var12);
         }

         throw var13;
      }

      reader.close();
      if (positions.size() >= 2 && !faces.isEmpty()) {
         float radius = 0.0F;
         float minDepth = Float.MAX_VALUE;

         for (int index = 1; index < positions.size(); index++) {
            float[] position = positions.get(index);
            radius = Math.max(radius, (float)Math.hypot((double)position[0], (double)position[1]));
            minDepth = Math.min(minDepth, position[2]);
         }

         float scale = radius == 0.0F ? 1.0F : 2.5F / radius;

         for (int index = 1; index < positions.size(); index++) {
            float[] position = positions.get(index);
            position[0] *= scale;
            position[1] *= scale;
            position[2] = (position[2] - minDepth) * scale + 0.03125F;
         }

         List<List<PegasusShieldMeshRenderer.Vertex>> triangles = new ArrayList<>();

         for (List<PegasusShieldMeshRenderer.VertexRef> face : faces) {
            for (int index = 1; index < face.size() - 1; index++) {
               triangles.add(
                  List.of(
                     resolve(face.get(0), positions, coordinates, normals),
                     resolve(face.get(index), positions, coordinates, normals),
                     resolve(face.get(index + 1), positions, coordinates, normals)
                  )
               );
            }
         }

         return new PegasusShieldMeshRenderer.Mesh(triangles);
      } else {
         throw new IOException("Empty shield mesh");
      }
   }

   private static PegasusShieldMeshRenderer.Vertex resolve(
      PegasusShieldMeshRenderer.VertexRef reference, List<float[]> positions, List<float[]> coordinates, List<float[]> normals
   ) {
      return new PegasusShieldMeshRenderer.Vertex(positions.get(reference.position()), coordinates.get(reference.uv()), normals.get(reference.normal()));
   }

   private static float[] normalize(float[] normal) {
      float length = (float)Math.sqrt((double)(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]));
      return length == 0.0F ? new float[]{0.0F, 0.0F, 1.0F} : new float[]{normal[0] / length, normal[1] / length, normal[2] / length};
   }

   private static record Mesh(List<List<PegasusShieldMeshRenderer.Vertex>> triangles) {
   }

   private static record Vertex(float[] position, float[] uv, float[] normal) {
   }

   private static record VertexRef(int position, int uv, int normal) {
   }
}
