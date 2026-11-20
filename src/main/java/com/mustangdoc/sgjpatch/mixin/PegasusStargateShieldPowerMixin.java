package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PegasusStargateEntity.class},
   remap = false
)
public abstract class PegasusStargateShieldPowerMixin {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")},
      remap = false
   )
   private static void sgjpatch$drainShieldPower(Level var0, BlockPos var1, BlockState var2, PegasusStargateEntity var3, CallbackInfo var4) {
      PegasusShieldPowerHelper.tick(var3);
   }
}

