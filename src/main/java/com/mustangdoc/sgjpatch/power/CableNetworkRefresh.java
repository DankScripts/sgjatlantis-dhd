package com.mustangdoc.sgjpatch.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.blocks.tech.CableBlock;
import net.povstalec.sgjourney.common.config.CommonCableConfig;
import net.povstalec.sgjourney.common.data.ConduitNetworks;

@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CableNetworkRefresh {
    private static final int FLOORS_PER_TICK = 256;
    private static final Map<ServerLevel, Work> WORK = new WeakHashMap<>();

    private CableNetworkRefresh() {}

    private static final class Work {
        final Set<BlockPos> floors = new LinkedHashSet<>();
        final Set<BlockPos> cables = new LinkedHashSet<>();
        final Map<Long, Set<BlockPos>> waitingFloors = new HashMap<>();
        final Map<Long, Set<BlockPos>> waitingCables = new HashMap<>();
    }

    public static void register(FloorConduitBlockEntity floor) {
        if (floor.getLevel() instanceof ServerLevel level)
            WORK.computeIfAbsent(level, ignored -> new Work()).floors.add(floor.getBlockPos().immutable());
    }

    private static void waitFor(Map<Long, Set<BlockPos>> waiting, BlockPos missing, BlockPos retry) {
        long key = ChunkPos.asLong(missing.getX() >> 4, missing.getZ() >> 4);
        waiting.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(retry.immutable());
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Work work = WORK.get(level);
        if (work == null) return;
        long key = event.getChunk().getPos().toLong();
        Set<BlockPos> floors = work.waitingFloors.remove(key);
        Set<BlockPos> cables = work.waitingCables.remove(key);
        if (floors != null) work.floors.addAll(floors);
        if (cables != null) work.cables.addAll(cables);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) WORK.remove(level);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Work work = WORK.get(level);
        if (work == null) return;
        boolean scannedFloor = false;
        for (int i = 0; i < FLOORS_PER_TICK && !work.floors.isEmpty(); i++) {
            BlockPos pos = removeFirst(work.floors);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof FloorConduitBlockEntity)) continue;
            scannedFloor = true;
            for (Direction side : Direction.values()) {
                BlockPos neighbor = pos.relative(side);
                if (!level.hasChunkAt(neighbor)) {
                    waitFor(work.waitingFloors, neighbor, pos);
                } else if (level.getBlockEntity(neighbor) instanceof CableBlockEntity) {
                    work.cables.add(neighbor);
                }
            }
        }
        if (scannedFloor) FloorConduitBlockEntity.refreshTopology(level);
        if (!work.cables.isEmpty()) refresh(level, work, removeFirst(work.cables));
    }

    private static BlockPos removeFirst(Set<BlockPos> positions) {
        var iterator = positions.iterator();
        BlockPos first = iterator.next();
        iterator.remove();
        return first;
    }

    enum ScanStatus { COMPLETE, UNLOADED, LIMIT }
    record Scan(ScanStatus status, Set<BlockPos> cables, BlockPos unloaded) {}

    // Match SGJourney's queue and work limit before calling its non-loaded-only BFS.
    static Scan scan(BlockPos start, int limit, Predicate<BlockPos> loaded, Predicate<BlockPos> matchingCable) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> cables = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        for (int i = 0; !queue.isEmpty() && i < limit; i++) {
            BlockPos pos = queue.remove();
            if (!loaded.test(pos)) return new Scan(ScanStatus.UNLOADED, cables, pos);
            visited.add(pos);
            if (matchingCable.test(pos)) {
                cables.add(pos);
                for (Direction side : Direction.values()) {
                    BlockPos next = pos.relative(side);
                    if (!visited.contains(next)) queue.add(next);
                }
            }
        }
        return new Scan(queue.isEmpty() ? ScanStatus.COMPLETE : ScanStatus.LIMIT, cables, null);
    }

    static boolean allNetworksMissing(Collection<Integer> ids, IntPredicate exists) {
        return !ids.isEmpty() && ids.stream().noneMatch(exists::test);
    }

    private static void refresh(ServerLevel level, Work work, BlockPos start) {
        if (!level.hasChunkAt(start)) {
            waitFor(work.waitingCables, start, start);
            return;
        }
        if (!(level.getBlockEntity(start) instanceof CableBlockEntity cable)) return;
        ConduitNetworks networks = ConduitNetworks.get(level);
        if (networks.getCableNetwork(cable.networkID()) != null) return;
        Block block = level.getBlockState(start).getBlock();
        if (!(block instanceof CableBlock)) return;
        Scan scan = scan(start, CommonCableConfig.max_cables_in_network.get(), level::hasChunkAt,
                pos -> level.getBlockState(pos).getBlock() == block);
        if (scan.status() == ScanStatus.UNLOADED) {
            waitFor(work.waitingCables, scan.unloaded(), start);
            return;
        }
        if (scan.status() == ScanStatus.LIMIT) {
            System.out.println("[SGJPATCH-POWERBRIDGE] Cannot safely refresh cable at " + start
                    + ": SGJourney's cable traversal limit was reached.");
            return;
        }
        List<CableBlockEntity> endpoints = new ArrayList<>();
        for (BlockPos pos : scan.cables())
            if (level.getBlockEntity(pos) instanceof CableBlockEntity part) endpoints.add(part);
        List<Integer> ids = endpoints.stream().map(CableBlockEntity::networkID).toList();
        if (!allNetworksMissing(ids, id -> networks.getCableNetwork(id) != null)) {
            System.out.println("[SGJPATCH-POWERBRIDGE] Cannot safely refresh cable at " + start
                    + ": its component also references an existing SGJourney network.");
            return;
        }
        endpoints.forEach(CableBlockEntity::update);
        if (endpoints.stream().noneMatch(CableBlockEntity::isOutput)) {
            System.out.println("[SGJPATCH-POWERBRIDGE] Cannot refresh cable at " + start
                    + ": SGJourney found no loaded energy output.");
            return;
        }
        networks.update(level, start);
        if (networks.getCableNetwork(cable.networkID()) == null) {
            System.out.println("[SGJPATCH-POWERBRIDGE] SGJourney did not register a rebuilt network at " + start);
            return;
        }
        System.out.println("[SGJPATCH-POWERBRIDGE] Rebuilt missing SGJourney cable network at " + start);
    }
}
