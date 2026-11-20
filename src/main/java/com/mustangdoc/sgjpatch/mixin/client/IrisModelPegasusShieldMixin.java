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
      cancellable = true,
      remap = false
   )
   private void sgjpatch$renderPegasusShield(
      Interface irisStargate, PoseStack stack, MultiBufferSource source, int combinedLight, int combinedOverlay, float progress, CallbackInfo callbackInfo
   ) {
      if (PegasusShieldMeshRenderer.renderPegasusShield(irisStargate, stack, source, combinedLight, progress)) {
         callbackInfo.cancel();
      }
   }
}
