package com.mustangdoc.sgjpatch.mixin;

import net.minecraft.world.item.ItemStack;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.blockstates.ShieldingState;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {IrisInfo.class},
   remap = false
)
public abstract class SGJourneyIrisInfoDefaultOpenMixin {
   @Shadow
   protected AbstractStargateEntity stargate;

   @Shadow
   protected abstract boolean setIris(ItemStack var1);

   @Shadow
   public abstract void setIrisProgress(short var1, short var2);

   @Inject(
      method = {"addIris"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void sgjpatch$defaultIrisToOpen(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (!this.setIris(stack)) {
         cir.setReturnValue(false);
      } else {
         short open = ShieldingState.OPEN.getProgress();
         this.setIrisProgress(open, open);
         if (this.stargate != null) {
            this.stargate.setStargateState(true, ShieldingState.OPEN);
            this.stargate.updateClient();
         }

         cir.setReturnValue(true);
      }
   }
}

