package com.mustangdoc.sgjpatch.mixin.client;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"net.minecraft.client.renderer.block.model.BlockElement$Deserializer"},
   remap = false
)
public class AtlantisDHDModelAngleMixin {
   @Inject(
      method = {"getAngle"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void sgjpatch$allowAtlantisTriangleAngles(JsonObject var1, CallbackInfoReturnable<Float> var2) {
      if (var1 != null && var1.get("angle") != null) {
         float var3 = var1.get("angle").getAsFloat();
         if (sgjpatch$isAtlantisAngle(var3)) {
            var2.setReturnValue(var3);
         }
      }
   }

   private static boolean sgjpatch$isAtlantisAngle(float var0) {
      return sgjpatch$near(var0, -57.51492F)
         || sgjpatch$near(var0, 57.51492F)
         || sgjpatch$near(var0, -60.98254F)
         || sgjpatch$near(var0, 60.98254F)
         || sgjpatch$near(var0, -59.9135F)
         || sgjpatch$near(var0, 59.9135F)
         || sgjpatch$near(var0, -61.89373F)
         || sgjpatch$near(var0, 61.89373F);
   }

   private static boolean sgjpatch$near(float var0, float var1) {
      return Math.abs(var0 - var1) < 2.0E-4F;
   }
}

