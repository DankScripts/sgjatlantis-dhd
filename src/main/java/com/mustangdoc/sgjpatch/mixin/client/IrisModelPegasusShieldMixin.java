package com.mustangdoc.sgjpatch.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.client.render.PegasusShieldMeshRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.povstalec.sgjourney.client.models.block_entity.IrisModel;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {IrisModel.class},
   remap = false
)
public abstract class IrisModelPegasusShieldMixin {
   @Inject(
      method = {"renderIris"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sgjpatch$renderEnergyShield(
      Interface gate, PoseStack poses, MultiBufferSource buffers, int light, int overlay, float progress, CallbackInfo callback
   ) {
      if (PegasusShieldMeshRenderer.render(gate, poses, buffers, light)) {
         callback.cancel();
      }
   }
}
