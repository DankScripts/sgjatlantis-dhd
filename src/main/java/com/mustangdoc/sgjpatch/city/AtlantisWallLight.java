package com.mustangdoc.sgjpatch.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall fixture bounds follow the mesh instead of blocking an entire cube. */
public class AtlantisWallLight extends AtlantisBuildingBlocks.FacingBlock {
    private final VoxelShape[] shapes = new VoxelShape[4];
    public AtlantisWallLight(BlockBehaviour.Properties properties, boolean wide) {
        super(properties);
        double x1 = wide ? 2.5 : 6.3, x2 = wide ? 13.5 : 9.7;
        double y1 = wide ? .6 : 0, y2 = wide ? 15.4 : 16;
        double z1 = wide ? 12.15 : 12.84, z2 = 16;
        for (int turn = 0; turn < 4; turn++) {
            shapes[turn] = box(x1, y1, z1, x2, y2, z2);
            double oldX1 = x1, oldX2 = x2;
            x1 = 16 - z2; x2 = 16 - z1; z1 = oldX1; z2 = oldX2;
        }
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal()
                ? face : context.getHorizontalDirection().getOpposite());
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[(state.getValue(FACING).get2DDataValue() + 2) % 4];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
