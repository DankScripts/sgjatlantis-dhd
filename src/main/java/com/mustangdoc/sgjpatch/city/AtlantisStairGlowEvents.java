package com.mustangdoc.sgjpatch.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Player-proximity control for the original inscribed stair and slab lights. */
@EventBusSubscriber(modid = "sgjatlantis_dhd", bus = EventBusSubscriber.Bus.GAME)
public final class AtlantisStairGlowEvents {
    private static final int HORIZONTAL_RANGE = 8;
    private static final int VERTICAL_RANGE = 4;
    private static final Map<ServerLevel, Map<Long, Long>> LAST_NEARBY = new WeakHashMap<>();

    private AtlantisStairGlowEvents() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now % 20L != 0L) return;

        Map<Long, Long> tracked = LAST_NEARBY.computeIfAbsent(level, ignored -> new HashMap<>());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (var player : level.players()) {
            BlockPos center = player.blockPosition();
            for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
                for (int x = -HORIZONTAL_RANGE; x <= HORIZONTAL_RANGE; x++) {
                    for (int z = -HORIZONTAL_RANGE; z <= HORIZONTAL_RANGE; z++) {
                        cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        if (!level.hasChunkAt(cursor)) continue;
                        BlockState state = level.getBlockState(cursor);
                        if (!isProximityLight(state.getBlock())) continue;
                        long key = cursor.asLong();
                        tracked.put(key, now);
                        if (!state.getValue(BlockStateProperties.LIT))
                            level.setBlock(cursor, state.setValue(BlockStateProperties.LIT, true), 3);
                    }
                }
            }
        }

        long hold = AtlantisLightingConfig.holdTicks();
        Iterator<Map.Entry<Long, Long>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (now - entry.getValue() < hold) continue;
            BlockPos pos = BlockPos.of(entry.getKey());
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (isProximityLight(state.getBlock()) && state.getValue(BlockStateProperties.LIT))
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            iterator.remove();
        }
    }

    private static boolean isProximityLight(Block block) {
        return (block instanceof AtlantisGlowStairs || block instanceof AtlantisGlowSlab)
                && !(block instanceof PoweredLightBlocks.Powered);
    }
}
