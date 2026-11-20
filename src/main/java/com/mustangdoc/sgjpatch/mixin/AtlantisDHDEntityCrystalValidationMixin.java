package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.world.item.ItemStack;
import net.povstalec.sgjourney.common.items.CallForwardingDevice;
import net.povstalec.sgjourney.common.items.crystals.AbstractCrystalItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {AtlantisDHDEntity.class},
   remap = false
)
public abstract class AtlantisDHDEntityCrystalValidationMixin {
   @Inject(
      method = {"isValidCrystal"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sgjpatch$allowLargeCrystals(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getItem() instanceof CallForwardingDevice
               || stack.getItem() instanceof AbstractCrystalItem crystal && crystal.isLarge()) {
            cir.setReturnValue(true);
         }
      }
   }
}

