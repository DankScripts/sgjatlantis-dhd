package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {IrisInfo.class},
   remap = false
)
public abstract class SGJourneyIrisInfoPegasusShieldSoundsMixin {
   private static final float PEGASUS_SHIELD_HIT_VOLUME = 16.0F;
   @Shadow
   protected AbstractStargateEntity stargate;

   @Shadow
   public abstract ItemStack getIris();

   @Inject(
      method = {"playIrisThudSound"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void sgjpatch$playPegasusShieldHitSound(CallbackInfo callbackInfo) {
      ItemStack irisStack = this.getIris();
      if (irisStack.is((Item)SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get()) && this.stargate != null) {
         Level level = this.stargate.getLevel();
         if (level != null) {
            level.playSound(null, this.stargate.getBlockPos(), (SoundEvent)SGJPatchSounds.PEGASUS_SHIELD_HIT.get(), SoundSource.BLOCKS, 16.0F, 1.0F);
            callbackInfo.cancel();
         }
      }
   }
}

