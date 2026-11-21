package com.mustangdoc.sgjpatch.city;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small Atlantis fixture mounted flat to the ceiling, emitting downward. */
public final class AtlantisCeilingLight extends Block {
    private static final VoxelShape SHAPE = box(6.3, 12.84, 0, 9.7, 16, 16);
    public AtlantisCeilingLight(BlockBehaviour.Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
