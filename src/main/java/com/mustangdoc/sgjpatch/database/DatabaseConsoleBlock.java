package com.mustangdoc.sgjpatch.database;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class DatabaseConsoleBlock extends HorizontalDirectionalBlock implements EntityBlock {
    static final int WIDTH = DatabaseConsoleMesh.WIDTH, DEPTH = DatabaseConsoleMesh.DEPTH;
    static final int HEIGHT = DatabaseConsoleMesh.HEIGHT, COUNT = WIDTH * DEPTH * HEIGHT;
    static final int ROOT = WIDTH + WIDTH / 2;
    static final IntegerProperty PART = IntegerProperty.create("part", 0, COUNT - 1);
    private static final VoxelShape[][] SHAPES = makeShapes();
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

    private static final MapCodec<DatabaseConsoleBlock> CODEC = simpleCodec(properties -> new DatabaseConsoleBlock());

    @Override protected MapCodec<DatabaseConsoleBlock> codec() { return CODEC; }

    DatabaseConsoleBlock() {
        super(Properties.of().strength(2).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, ROOT));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    static int turns(Direction facing) {
        return (facing.get2DDataValue() + 2) % 4;
    }

    static BlockPos offset(Direction facing, int part) {
        return BlockPos.ZERO.relative(facing.getClockWise(), part % WIDTH - WIDTH / 2)
                .relative(facing.getOpposite(), part / WIDTH % DEPTH - DEPTH / 2)
                .above(part / (WIDTH * DEPTH));
    }

    static boolean occupied(int part) {
        return !SHAPES[0][part].isEmpty();
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ROOT ? new DatabaseConsoleEntity(pos, state) : null;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[turns(state.getValue(FACING))][state.getValue(PART)];
    }

    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(DatabaseConsoleRegistry.ITEM);
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) popResource(level, pos, getCloneItemStack(level, pos, state));
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (next.is(this) || level.isClientSide || REMOVING.get()) return;
        Direction facing = state.getValue(FACING);
        BlockPos root = pos.subtract(offset(facing, state.getValue(PART)));
        withoutPartCleanup(() -> {
            for (int part = 0; part < COUNT; part++) {
                // Older square plinths can have saved parts outside the new shape.
                BlockPos other = root.offset(offset(facing, part));
                BlockState candidate = level.getBlockState(other);
                if (candidate.is(this) && candidate.getValue(FACING) == facing && candidate.getValue(PART) == part) {
                    level.removeBlock(other, false);
                }
            }
        });
    }

    static void withoutPartCleanup(Runnable action) {
        REMOVING.set(true);
        try {
            action.run();
        } finally {
            REMOVING.remove();
        }
    }

    private static VoxelShape[][] makeShapes() {
        VoxelShape[][] result = new VoxelShape[4][COUNT];
        for (int part = 0; part < COUNT; part++) {
            int x = 16 * (part % WIDTH), y = 16 * (part / (WIDTH * DEPTH)), z = 16 * (part / WIDTH % DEPTH);
            VoxelShape shape = Shapes.empty();
            for (float[] bounds : DatabaseConsoleMesh.PLINTH_COLLISION) {
                shape = addClippedBox(shape, x, y, z, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
            }
            for (int row = DatabaseConsoleMesh.PLINTH_ROWS; row < DatabaseConsoleMesh.CUBES.length; row++) {
                float[] cube = DatabaseConsoleMesh.CUBES[row];
                float[] min = {16 * WIDTH, 16 * HEIGHT, 16 * DEPTH}, max = {0, 0, 0};
                for (int i = 0; i < 24; i++) {
                    int axis = i % 3;
                    min[axis] = Math.min(min[axis], cube[i]);
                    max[axis] = Math.max(max[axis], cube[i]);
                }
                shape = addClippedBox(shape, x, y, z, min[0], min[1], min[2], max[0], max[1], max[2]);
            }
            for (int turn = 0; turn < 4; turn++) {
                result[turn][part] = shape;
                VoxelShape[] rotated = {Shapes.empty()};
                shape.forAllBoxes((a,b,c,d,e,f) -> rotated[0] = Shapes.or(rotated[0], Shapes.box(1-f,b,a,1-c,e,d)));
                shape = rotated[0];
            }
        }
        return result;
    }

    private static VoxelShape addClippedBox(VoxelShape shape, int x, int y, int z,
                                           float x0, float y0, float z0, float x1, float y1, float z1) {
        double a = Math.max(0, x0 - x), b = Math.max(0, y0 - y), c = Math.max(0, z0 - z);
        double d = Math.min(16, x1 - x), e = Math.min(16, y1 - y), f = Math.min(16, z1 - z);
        return d-a > .001 && e-b > .001 && f-c > .001 ? Shapes.or(shape, box(a,b,c,d,e,f)) : shape;
    }
}
