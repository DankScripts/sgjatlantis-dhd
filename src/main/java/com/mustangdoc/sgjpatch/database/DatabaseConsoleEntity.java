package com.mustangdoc.sgjpatch.database;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

final class DatabaseConsoleEntity extends BlockEntity {
    DatabaseConsoleEntity(BlockPos pos, BlockState state) {
        super(DatabaseConsoleRegistry.ENTITY, pos, state);
    }

    AABB consoleBounds() {
        int radius = DatabaseConsoleBlock.WIDTH / 2;
        return new AABB(worldPosition).inflate(radius, 0, radius)
                .expandTowards(0, DatabaseConsoleBlock.HEIGHT - 1, 0);
    }
}
