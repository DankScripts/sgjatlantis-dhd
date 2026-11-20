package com.mustangdoc.sgjpatch.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import com.mustangdoc.sgjpatch.client.sound.PegasusShieldHumController;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.povstalec.sgjourney.client.render.SGJourneyRenderTypes;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class PegasusShieldMeshRenderer {
   private static final Logger LOGGER = LogManager.getLogger("SGJPATCH");
   private static final ResourceLocation SHIELD_MODEL = ResourceLocation.fromNamespaceAndPath(
      "sgjadditions_capacity_patch", "models/entity/stargate/shield/pegasus_upgraded_shield.obj"
   );
   private static final ResourceLocation SHIELD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "sgjadditions_capacity_patch", "textures/entity/stargate/shield/pegasus_upgraded_shield.jpg"
   );
   private static final float CLOSED_PROGRESS = 58.0F;
   private static final float MAX_VISIBLE_ALPHA = 0.9F;
   private static final float TARGET_RADIUS = 2.5F;
   private static final float TARGET_OFFSET = 0.03125F;
   private static volatile PegasusShieldMeshRenderer.ShieldMesh mesh;
   private static volatile boolean meshLoadFailed;

   private PegasusShieldMeshRenderer() {
   }

   /** Lantea's controller pose is rooted at the base block, not the gate centre. */
   public static void renderLantea(BlockEntity gate, boolean closed, PoseStack stack, MultiBufferSource source,
                                   int combinedLight, Direction facing) {
      if (PegasusShieldTransitionHelper.reveal(gate, closed) <= 0.001F) return;
      ShieldMesh shieldMesh = getMesh();
      if (shieldMesh == null) return;
      stack.pushPose();
      stack.translate(0.5, 3.5, 0.5);
      stack.mulPose(Axis.YP.rotationDegrees(switch (facing) {
         case SOUTH -> 180.0F;
         case EAST -> -90.0F;
         case WEST -> 90.0F;
         default -> 0.0F;
      }));
      // The gate's front is local negative Z; this moves the OBJ ahead of
      // Lantea's event horizon instead of hiding it behind the wormhole.
      stack.scale(1.0F, 1.0F, -1.0F);

      Pose pose = stack.last();
      ShieldTextureFlow.beginFrame();
      VertexConsumer consumer = source.getBuffer(LanteaShieldRenderType.shield(SHIELD_TEXTURE));
      for (Face face : shieldMesh.faces) {
         emitTriangle(consumer, pose.pose(), pose.normal(), combinedLight, face, false, 0.9F, true);
         emitTriangle(consumer, pose.pose(), pose.normal(), combinedLight, face, true, 0.9F, true);
      }
      stack.popPose();
   }

   public static boolean renderPegasusShield(Interface irisStargate, PoseStack stack, MultiBufferSource source, int combinedLight, float progress) {
      if (irisStargate == null) {
         return false;
      } else {
         ItemStack irisStack = irisStargate.irisInfo().getIris();
         if (!irisStack.is((Item)SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get())) {
            if (irisStargate instanceof PegasusStargateEntity pegasusStargate) {
               PegasusShieldHumController.sync(pegasusStargate, false);
            }
            return false;
         } else {
            if (irisStargate instanceof PegasusStargateEntity pegasusStargate) {
               PegasusShieldHumController.sync(pegasusStargate, com.mustangdoc.sgjpatch.client.sound.IrisSoundState.active(irisStargate.irisInfo()));
            }
            float shieldAlpha = PegasusShieldTransitionHelper.alpha(irisStargate, progress);
            if (shieldAlpha <= 0.001F) {
               return true;
            } else {
               PegasusShieldMeshRenderer.ShieldMesh shieldMesh = getMesh();
               if (shieldMesh == null) {
                  return true;
               } else {
                  stack.pushPose();
                  Pose pose = stack.last();
                  Matrix4f matrix4f = pose.pose();
                  Matrix3f matrix3f = pose.normal();
                  ShieldTextureFlow.beginFrame();
      VertexConsumer consumer = source.getBuffer(SGJourneyRenderTypes.shield(SHIELD_TEXTURE));

                  for (PegasusShieldMeshRenderer.Face face : shieldMesh.faces) {
                     emitTriangle(consumer, matrix4f, matrix3f, combinedLight, face, false, shieldAlpha, true);
                     emitTriangle(consumer, matrix4f, matrix3f, combinedLight, face, true, shieldAlpha, true);
                  }

                  stack.popPose();
                  return true;
               }
            }
         }
      }
   }

   private static PegasusShieldMeshRenderer.ShieldMesh getMesh() {
      PegasusShieldMeshRenderer.ShieldMesh cachedMesh = mesh;
      if (cachedMesh != null) {
         return cachedMesh;
      } else if (meshLoadFailed) {
         return null;
      } else {
         synchronized (PegasusShieldMeshRenderer.class) {
            if (mesh != null) {
               return mesh;
            } else {
               try {
                  mesh = loadMesh();
                  LOGGER.info("[SGJPATCH] Loaded Pegasus upgraded shield mesh from {}", SHIELD_MODEL);
               } catch (Exception var4) {
                  meshLoadFailed = true;
                  LOGGER.error("[SGJPATCH] Failed to load Pegasus upgraded shield mesh from {}", SHIELD_MODEL, var4);
               }

               return mesh;
            }
         }
      }
   }

   private static PegasusShieldMeshRenderer.ShieldMesh loadMesh() throws IOException {
      List<float[]> vertices = new ArrayList<>();
      List<float[]> uvs = new ArrayList<>();
      List<float[]> normals = new ArrayList<>();
      List<PegasusShieldMeshRenderer.Face> faces = new ArrayList<>();
      vertices.add(new float[]{0.0F, 0.0F, 0.0F});
      uvs.add(new float[]{0.5F, 0.5F});
      normals.add(new float[]{0.0F, 0.0F, 1.0F});

      String line;
      try (
         InputStream inputStream = Minecraft.getInstance().getResourceManager().open(SHIELD_MODEL);
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      ) {
         while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
               if (trimmed.startsWith("v ")) {
                  String[] parts = trimmed.split("\\s+");
                  vertices.add(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
               } else if (trimmed.startsWith("vt ")) {
                  String[] parts = trimmed.split("\\s+");
                  uvs.add(new float[]{Float.parseFloat(parts[1]), 1.0F - Float.parseFloat(parts[2])});
               } else if (trimmed.startsWith("vn ")) {
                  String[] parts = trimmed.split("\\s+");
                  normals.add(normalize(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])}));
               } else if (trimmed.startsWith("f ")) {
                  String[] parts = trimmed.substring(2).trim().split("\\s+");
                  PegasusShieldMeshRenderer.VertexRef[] refs = new PegasusShieldMeshRenderer.VertexRef[parts.length];

                  for (int index = 0; index < parts.length; index++) {
                     refs[index] = parseVertexRef(parts[index]);
                  }

                  for (int index = 1; index < refs.length - 1; index++) {
                     faces.add(new PegasusShieldMeshRenderer.Face(refs[0], refs[index], refs[index + 1]));
                  }
               }
            }
         }
      }

      float maxRadius = 0.0F;
      float minZ = Float.MAX_VALUE;

      for (int index = 1; index < vertices.size(); index++) {
         float[] vertex = vertices.get(index);
         float radius = (float)Math.sqrt((double)(vertex[0] * vertex[0] + vertex[1] * vertex[1]));
         maxRadius = Math.max(maxRadius, radius);
         minZ = Math.min(minZ, vertex[2]);
      }

      float scale = maxRadius == 0.0F ? 1.0F : 2.5F / maxRadius;

      for (int index = 1; index < vertices.size(); index++) {
         float[] vertex = vertices.get(index);
         vertex[0] *= scale;
         vertex[1] *= scale;
         vertex[2] = (vertex[2] - minZ) * scale + 0.03125F;
      }

      return new PegasusShieldMeshRenderer.ShieldMesh(vertices, uvs, normals, faces);
   }

   private static void emitTriangle(
      VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, int combinedLight, PegasusShieldMeshRenderer.Face face, boolean reverse, float alpha, boolean animate
   ) {
      PegasusShieldMeshRenderer.VertexData first = face.resolveA(mesh);
      PegasusShieldMeshRenderer.VertexData second = face.resolveB(mesh);
      PegasusShieldMeshRenderer.VertexData third = face.resolveC(mesh);
      float[] normal = normalize(
         new float[]{
            first.normal[0] + second.normal[0] + third.normal[0],
            first.normal[1] + second.normal[1] + third.normal[1],
            first.normal[2] + second.normal[2] + third.normal[2]
         }
      );
      if (reverse) {
         normal[0] = -normal[0];
         normal[1] = -normal[1];
         normal[2] = -normal[2];
         emitVertex(consumer, matrix4f, matrix3f, combinedLight, first, normal, alpha, animate);
         emitVertex(consumer, matrix4f, matrix3f, combinedLight, third, normal, alpha, animate);
         emitVertex(consumer, matrix4f, matrix3f, combinedLight, second, normal, alpha, animate);
      } else {
         emitVertex(consumer, matrix4f, matrix3f, combinedLight, first, normal, alpha, animate);
         emitVertex(consumer, matrix4f, matrix3f, combinedLight, second, normal, alpha, animate);
         emitVertex(consumer, matrix4f, matrix3f, combinedLight, third, normal, alpha, animate);
      }
   }

   private static void emitVertex(
      VertexConsumer consumer,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      int combinedLight,
      PegasusShieldMeshRenderer.VertexData vertexData,
      float[] normal,
      float alpha, boolean animate
   ) {
      consumer.vertex(matrix4f, vertexData.vertex[0], vertexData.vertex[1], vertexData.vertex[2])
         .color(1.0F, 1.0F, 1.0F, animate ? PegasusShieldTransitionHelper.vertexAlpha(vertexData.vertex, alpha) : alpha)
         .uv(ShieldTextureFlow.u(vertexData.uv[0], vertexData.uv[1]), ShieldTextureFlow.v(vertexData.uv[0], vertexData.uv[1]))
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(combinedLight)
         .normal(matrix3f, normal[0], normal[1], normal[2])
         .endVertex();
   }

   private static PegasusShieldMeshRenderer.VertexRef parseVertexRef(String token) {
      String[] parts = token.split("/");
      int vertexIndex = Integer.parseInt(parts[0]);
      int uvIndex = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 0;
      int normalIndex = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : 0;
      return new PegasusShieldMeshRenderer.VertexRef(vertexIndex, uvIndex, normalIndex);
   }

   private static float[] normalize(float[] vector) {
      float length = (float)Math.sqrt((double)(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]));
      return length == 0.0F ? new float[]{0.0F, 0.0F, 1.0F} : new float[]{vector[0] / length, vector[1] / length, vector[2] / length};
   }

   private static record Face(PegasusShieldMeshRenderer.VertexRef a, PegasusShieldMeshRenderer.VertexRef b, PegasusShieldMeshRenderer.VertexRef c) {
      private PegasusShieldMeshRenderer.VertexData resolveA(PegasusShieldMeshRenderer.ShieldMesh mesh) {
         return this.a.resolve(mesh);
      }

      private PegasusShieldMeshRenderer.VertexData resolveB(PegasusShieldMeshRenderer.ShieldMesh mesh) {
         return this.b.resolve(mesh);
      }

      private PegasusShieldMeshRenderer.VertexData resolveC(PegasusShieldMeshRenderer.ShieldMesh mesh) {
         return this.c.resolve(mesh);
      }
   }

   private static record ShieldMesh(List<float[]> vertices, List<float[]> uvs, List<float[]> normals, List<PegasusShieldMeshRenderer.Face> faces) {
   }

   private static record VertexData(float[] vertex, float[] uv, float[] normal) {
   }

   private static record VertexRef(int vertexIndex, int uvIndex, int normalIndex) {
      private PegasusShieldMeshRenderer.VertexData resolve(PegasusShieldMeshRenderer.ShieldMesh mesh) {
         float[] vertex = mesh.vertices().get(this.vertexIndex);
         float[] uv = this.uvIndex > 0 && this.uvIndex < mesh.uvs().size() ? mesh.uvs().get(this.uvIndex) : new float[]{0.5F, 0.5F};
         float[] normal = this.normalIndex > 0 && this.normalIndex < mesh.normals().size()
            ? mesh.normals().get(this.normalIndex)
            : new float[]{0.0F, 0.0F, 1.0F};
         return new PegasusShieldMeshRenderer.VertexData(vertex, uv, normal);
      }
   }
}
