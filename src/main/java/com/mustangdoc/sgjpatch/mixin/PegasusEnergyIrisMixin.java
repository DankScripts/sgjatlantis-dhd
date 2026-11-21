package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.IrisMotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {IrisInfo.class},
   remap = false
)
public abstract class PegasusEnergyIrisMixin {
   @Shadow
   protected AbstractStargateEntity<?> stargate;

   @Inject(
      method = {"addIris"},
      at = {@At("RETURN")}
   )
   private void sgjpatch$installOpen(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
      if ((Boolean)callback.getReturnValue() && this.stargate instanceof PegasusStargateEntity gate && PegasusShieldPowerHelper.hasShield(gate.irisInfo())) {
         PegasusShieldPowerHelper.open(gate);
      }
   }

   @Inject(
      method = {"setIrisMotion"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sgjpatch$requirePower(IrisMotion motion, CallbackInfo callback) {
      if (motion.isClosing()
         && this.stargate instanceof PegasusStargateEntity gate
         && gate.getLevel() != null
         && !gate.getLevel().isClientSide()
         && !PegasusShieldPowerHelper.canEngage(gate)) {
         PegasusShieldPowerHelper.open(gate);
         callback.cancel();
      }
   }

   @Inject(
      method = {"decreaseIrisDurability"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sgjpatch$energyInsteadOfDurability(CallbackInfo callback) {
      if (this.stargate instanceof PegasusStargateEntity gate && PegasusShieldPowerHelper.hasShield(gate.irisInfo())) {
         callback.cancel();
      }
   }

   @Inject(
      method = {"playIrisThudSound"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sgjpatch$shieldImpactSound(CallbackInfo callback) {
      if (this.stargate instanceof PegasusStargateEntity gate && PegasusShieldPowerHelper.hasShield(gate.irisInfo()) && gate.getLevel() != null) {
         if (!gate.getLevel().isClientSide()) {
            gate.getLevel().playSound(null, gate.getBlockPos(), (SoundEvent)SGJPatchSounds.HIT.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
         }

         callback.cancel();
      }
   }
}
