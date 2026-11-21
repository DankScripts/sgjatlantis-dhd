package com.mustangdoc.sgjpatch.util;

import com.mustangdoc.sgjpatch.mixin.AbstractDHDEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.misc.CoordinateHelper.Relative;

public final class DhdLookup {
   private DhdLookup() {
   }

   public static AbstractStargateEntity<?> resolveStargate(AbstractDHDEntity dhd) {
      if (dhd instanceof AbstractDHDEntityAccessor accessor) {
         Level level = dhd.getLevel();
         Vec3i relativePos = accessor.sgjpatch$getStargateRelativePos();
         if (level != null && relativePos != null) {
            BlockPos gatePos = Relative.getOffsetPos(dhd.getDirection(), dhd.getBlockPos(), relativePos);
            if (gatePos == null) {
               return null;
            } else {
               return level.getBlockEntity(gatePos) instanceof AbstractStargateEntity<?> gate ? gate : null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
