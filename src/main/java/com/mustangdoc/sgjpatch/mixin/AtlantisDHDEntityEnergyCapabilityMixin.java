package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {AtlantisDHDEntity.class},
   remap = false
)
public abstract class AtlantisDHDEntityEnergyCapabilityMixin {
   @Inject(
      method = {"getCapability"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <T> void sgjpatch$ensureEnergyCapability(Capability<T> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
      if (cap == ForgeCapabilities.ENERGY) {
         try {
            LazyOptional<?> lazy = ((EnergyBlockEntityAccessor)this).sgjpatch$getLazyEnergyHandler();
            if (lazy != null) {
               cir.setReturnValue(lazy.cast());
            }
         } catch (Throwable var5) {
         }
      }
   }
}

