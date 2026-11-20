package com.mustangdoc.sgjpatch.city;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class AtlantisGlowStairs extends StairBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public AtlantisGlowStairs(BlockState base, BlockBehaviour.Properties properties) {
        super(base, properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
