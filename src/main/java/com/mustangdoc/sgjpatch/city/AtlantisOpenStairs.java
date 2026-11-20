package com.mustangdoc.sgjpatch.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Straight structural stairs: four quarter-rise treads, with no solid corner substitution. */
public class AtlantisOpenStairs extends AtlantisGlowStairs {
    private static final VoxelShape[][] SHAPES = new VoxelShape[2][4];
    static {
        for (int half = 0; half < 2; half++) for (int turn = 0; turn < 4; turn++) {
            VoxelShape shape = Shapes.empty();
            for (int step = 0; step < 4; step++) {
                double y = 4 + 4 * step;
                shape = Shapes.or(shape, transformed(0, y, -1.5 + 4 * step,
                        16, y + .5, 3.5 + 4 * step, half, turn));
                shape = Shapes.or(shape, transformed(6, y - 4, 4 * step,
                        10, y, 2 + 4 * step, half, turn));
            }
            SHAPES[half][turn] = shape.optimize();
        }
    }
    private static VoxelShape transformed(double x1, double y1, double z1,
            double x2, double y2, double z2, int half, int turn) {
        if (half == 1) {
            double oldY = y1, oldZ = z1;
            y1 = 16 - y2; y2 = 16 - oldY;
            z1 = 16 - z2; z2 = 16 - oldZ;
        }
        for (int i = 0; i < turn; i++) {
            double oldX1 = x1, oldX2 = x2;
            x1 = 16 - z2; x2 = 16 - z1;
            z1 = oldX1; z2 = oldX2;
        }
        return box(x1, y1, z1, x2, y2, z2);
    }
    public AtlantisOpenStairs(BlockState base, BlockBehaviour.Properties properties) { super(base, properties); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(SHAPE, StairsShape.STRAIGHT);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos)
                .setValue(SHAPE, StairsShape.STRAIGHT);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int half = state.getValue(HALF) == Half.TOP ? 1 : 0;
        int turn = (state.getValue(FACING).get2DDataValue() + half * 2) % 4;
        return SHAPES[half][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
}
