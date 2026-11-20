package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AtlantisHatchPanelBlock extends Block implements EntityBlock {
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");
    public static final BooleanProperty IRIS = BooleanProperty.create("iris");
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    private static final VoxelShape[][] SHAPES = new VoxelShape[121][169];
    public AtlantisHatchPanelBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0F, 20.0F)
                .noOcclusion().dynamicShape().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(UPPER, false).setValue(IRIS, false).setValue(SEALED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPPER, IRIS, SEALED);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return state.getValue(IRIS) ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int door = state.getValue(UPPER) ? 1 : 0;
        return state.getValue(IRIS) && pos.equals(AtlantisHatchService.center(door)) ? new AtlantisHatchIrisBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == AtlantisHatchRegistry.IRIS_ENTITY ? (l, p, s, be) -> ((AtlantisHatchIrisBlockEntity) be).tick() : null;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state, level, pos, context);
    }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(IRIS) && state.getValue(SEALED) ? 15 : super.getLightBlock(state, level, pos);
    }
    @Override public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(IRIS)) return Shapes.block();
        int dx = pos.getX() - AtlantisHatchService.CENTER_X;
        int dz = pos.getZ() - AtlantisHatchService.CENTER_Z;
        if (Math.abs(dx) > 6 || Math.abs(dz) > 6) return Shapes.block();
        int door = state.getValue(UPPER) ? 1 : 0;
        var be = level.getBlockEntity(AtlantisHatchService.center(door));
        int step = be instanceof AtlantisHatchIrisBlockEntity iris ? Math.round(iris.progress() * 120) : 0;
        int index = (dz + 6) * 13 + dx + 6;
        if (SHAPES[step][index] != null) return SHAPES[step][index];
        double radius = AtlantisHatchIrisGeometry.apertureRadius(step / 120.0);
        VoxelShape shape = Shapes.empty();
        // Four strips conservatively follow the circular aperture, without full-block steps.
        for (int strip = 0; strip < 4; strip++) {
            double x0 = strip / 4.0, x1 = (strip + 1) / 4.0;
            double farX = Math.max(Math.abs(dx + x0 - .5), Math.abs(dx + x1 - .5));
            double half = Math.sqrt(Math.max(0, radius * radius - farX * farX));
            double low = Math.max(0, Math.min(1, .5 - dz - half));
            double high = Math.max(0, Math.min(1, .5 - dz + half));
            if (low > 0) shape = Shapes.or(shape, Shapes.box(x0, .40, 0, x1, .60, low));
            if (high < 1) shape = Shapes.or(shape, Shapes.box(x0, .40, high, x1, .60, 1));
        }
        return SHAPES[step][index] = shape.optimize();
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement,
            boolean moving) {
        if (!state.is(replacement.getBlock()) && state.getValue(IRIS)
                && level.getBlockState(pos.above()).is(AtlantisHatchRegistry.WEATHER_SEAL))
            level.removeBlock(pos.above(), false);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
