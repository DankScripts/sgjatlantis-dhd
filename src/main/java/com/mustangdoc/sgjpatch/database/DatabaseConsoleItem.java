package com.mustangdoc.sgjpatch.database;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

final class DatabaseConsoleItem extends Item {
    DatabaseConsoleItem() { super(new Properties()); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos().above();
        Direction facing = context.getHorizontalDirection().getOpposite();
        var positions = new ArrayList<BlockPos>();
        var previousStates = new ArrayList<BlockState>();
        var consoleStates = new ArrayList<BlockState>();
        for (int part = 0; part < DatabaseConsoleBlock.COUNT; part++) {
            if (!DatabaseConsoleBlock.occupied(part)) continue;
            BlockPos pos = root.offset(DatabaseConsoleBlock.offset(facing, part));
            if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
                    || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand())
                    || !level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()
                    || level.getBlockEntity(pos) != null
                    || !level.getEntities((Entity) null, new AABB(pos), entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) {
                return fail(level, player);
            }
            positions.add(pos);
            previousStates.add(level.getBlockState(pos));
            consoleStates.add(DatabaseConsoleRegistry.BLOCK.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing).setValue(DatabaseConsoleBlock.PART, part));
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var snapshots = positions.stream().map(pos -> BlockSnapshot.create(level.dimension(), level, pos)).toList();
        if (ForgeEventFactory.onMultiBlockPlace(player, snapshots, Direction.UP)) return fail(level, player);
        int placed = 0;
        for (; placed < positions.size(); placed++) {
            if (!level.setBlock(positions.get(placed), consoleStates.get(placed), Block.UPDATE_CLIENTS)) break;
        }
        if (placed != positions.size()) {
            int count = placed;
            DatabaseConsoleBlock.withoutPartCleanup(() -> {
                for (int i = count - 1; i >= 0; i--) {
                    level.setBlock(positions.get(i), previousStates.get(i), Block.UPDATE_CLIENTS);
                }
            });
            return fail(level, player);
        }
        for (BlockPos pos : positions) level.updateNeighborsAt(pos, DatabaseConsoleRegistry.BLOCK);
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult fail(Level level, Player player) {
        if (!level.isClientSide) player.displayClientMessage(
                Component.translatable("message.sgjadditions_capacity_patch.database_console.clear_space"), true);
        return InteractionResult.FAIL;
    }
}
