package com.mustangdoc.sgjpatch.city;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Stepped octagonal column with a matching collision outline. */
public class AtlantisPillarBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 5, 3, 16, 11),
            Block.box(3, 0, 3, 5, 16, 13),
            Block.box(5, 0, 1, 11, 16, 15),
            Block.box(11, 0, 3, 13, 16, 13),
            Block.box(13, 0, 5, 15, 16, 11));

    public AtlantisPillarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
