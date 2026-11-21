package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.block.AtlantisDHDBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AtlantisDHDShapeHelper {
   private AtlantisDHDShapeHelper() {
   }

   public static VoxelShape shape(BlockState var0) {
      Direction var1 = var0.getValue(AtlantisDHDBlock.FACING);
      if (var1 == Direction.NORTH) {
         return Block.box(0.0, 0.0, 0.0, 32.0, 20.0, 16.0);
      } else if (var1 == Direction.EAST) {
         return Block.box(0.0, 0.0, 0.0, 16.0, 20.0, 32.0);
      } else {
         return var1 == Direction.WEST ? Block.box(0.0, 0.0, -16.0, 16.0, 20.0, 16.0) : Block.box(-16.0, 0.0, 0.0, 16.0, 20.0, 16.0);
      }
   }
}
