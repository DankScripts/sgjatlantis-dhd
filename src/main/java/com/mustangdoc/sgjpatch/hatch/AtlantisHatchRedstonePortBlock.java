package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** Optional direct redstone input. Place beside either roof opening at its height. */
public final class AtlantisHatchRedstonePortBlock extends Block {
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public AtlantisHatchRedstonePortBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5, 15)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(UPPER, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPPER, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        boolean upper = Math.abs(pos.getY() - AtlantisHatchService.HEIGHTS[1])
                < Math.abs(pos.getY() - AtlantisHatchService.HEIGHTS[0]);
        return defaultBlockState().setValue(UPPER, upper);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!old.is(this)) updateInput(state, level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean moved) {
        super.neighborChanged(state, level, pos, block, fromPos, moved);
        updateInput(state, level, pos);
    }

    private void updateInput(BlockState state, Level level, BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || !AtlantisHatchService.isAtlantis(level)) return;
        int door = state.getValue(UPPER) ? 1 : 0;
        if (pos.getY() != AtlantisHatchService.HEIGHTS[door]
                || Math.abs(pos.getX() - AtlantisHatchService.CENTER_X) > 7
                || Math.abs(pos.getZ() - AtlantisHatchService.CENTER_Z) > 7) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), 2);
        if (!AtlantisHatchService.setOpen(server, door, powered, null))
            level.setBlock(pos, state, 2);
    }
}
