package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

public final class AtlantisHatchTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(-8, 13, 0, 24, 20, 16),
            box(-5, 0, 1, -2, 14, 16),
            box(18, 0, 1, 21, 14, 16));
    private static final VoxelShape EAST_SHAPE = rotate(NORTH_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = rotate(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = rotate(SOUTH_SHAPE);

    public AtlantisHatchTableBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0F, 15.0F)
                .sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtlantisHatchTableBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.create(getCollisionShape(state, level, pos, context).bounds());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return tableShape(state.getValue(FACING));
    }

    public static VoxelShape tableShape(Direction facing) {
        return switch (facing) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private static VoxelShape rotate(VoxelShape source) {
        final VoxelShape[] result = {Shapes.empty()};
        source.forAllBoxes((minX,minY,minZ,maxX,maxY,maxZ) ->
                result[0] = Shapes.or(result[0],
                        Shapes.box(1-maxZ,minY,minX,1-minZ,maxY,maxX)));
        return result[0];
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AtlantisHatchTableBlockEntity table
                    && table.storedEnergy() < AtlantisHatchTableBlockEntity.OPERATION_COST)
                AtlantisHatchCableBootstrapEvents.register(table);
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, ignored) ->
                            new AtlantisHatchMenu(id, inventory, level, pos),
                            Component.literal("Atlantis Jumper Bay Doors")), pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && AtlantisHatchService.isAtlantis(level)
                && Math.abs(pos.getX() - AtlantisHatchService.CENTER_X) < 128
                && Math.abs(pos.getZ() - AtlantisHatchService.CENTER_Z) < 128) {
            ServerPlayer operator = placer instanceof ServerPlayer p ? p : null;
            for (int door = 0; door < 2; door++)
                if (AtlantisHatchService.status(level, door) == 1)
                    AtlantisHatchService.setOpen(serverLevel, door, false, operator);
        }
    }
}
