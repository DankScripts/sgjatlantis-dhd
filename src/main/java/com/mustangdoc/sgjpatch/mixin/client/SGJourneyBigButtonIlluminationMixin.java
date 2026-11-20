package com.mustangdoc.sgjpatch.mixin.client;

import com.mustangdoc.sgjpatch.mixin.AbstractDHDEntityAccessor;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDMenuAccessor;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCompatMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.client.widgets.dhd.DHDBigButton;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.menu.dhd.IDHDMenu;
import net.povstalec.sgjourney.common.misc.CoordinateHelper.Relative;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {DHDBigButton.class},
   remap = false
)
public abstract class SGJourneyBigButtonIlluminationMixin {
   @Shadow
   public IDHDMenu menu;

   @Inject(
      method = {"getYImage"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void sgjpatch$useIrisIlluminationForAtlantis(boolean hovered, CallbackInfoReturnable<Integer> cir) {
      if (this.menu != null) {
         if (this.menu instanceof AtlantisDHDCompatMenu || this.menu instanceof AtlantisDHDMenu) {
            boolean irisClosed = false;

            try {
               if (this.menu instanceof AbstractDHDMenuAccessor accessor && accessor.sgjpatch$getBlockEntity() instanceof AbstractDHDEntity dhd) {
                  Level level = dhd.getLevel();
                  if (level != null) {
                     BlockPos dhdPos = dhd.getBlockPos();
                     Vec3i rel = ((AbstractDHDEntityAccessor)dhd).sgjpatch$getStargateRelativePos();
                     if (rel != null) {
                        BlockPos gatePos = Relative.getOffsetPos(dhd.getDirection(), dhdPos, rel);
                        if (gatePos != null && level.getBlockEntity(gatePos) instanceof AbstractStargateEntity gate && gate instanceof Interface irisHolder) {
                           IrisInfo iris = irisHolder.irisInfo();
                           irisClosed = iris != null && iris.hasIris() && iris.isIrisClosed();
                        }
                     }
                  }
               }
            } catch (Throwable var14) {
            }

            if (irisClosed) {
               cir.setReturnValue(hovered ? 3 : 2);
            } else {
               cir.setReturnValue(1);
            }
         }
      }
   }
}

