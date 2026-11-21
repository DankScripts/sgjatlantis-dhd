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
      at = {@At("TAIL")}
   )
   private static void sgjpatch$drainShieldPower(Level level, BlockPos pos, BlockState state, PegasusStargateEntity gate, CallbackInfo callback) {
      PegasusShieldPowerHelper.tick(gate);
   }
}
