package com.mustangdoc.sgjpatch.city;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Ordinary stairs: no inscription state, light emission, or proximity updates. */
public final class AtlantisPlainStairs extends StairBlock {
    public AtlantisPlainStairs(BlockState base, BlockBehaviour.Properties properties) {
        super(base, properties);
    }
}
