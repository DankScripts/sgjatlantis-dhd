package com.mustangdoc.sgjpatch.hatch;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.blocks.tech.CableBlock;
import net.povstalec.sgjourney.common.data.ConduitNetworks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rebuilds the SGJourney network when a table loads above a concealed cable. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AtlantisHatchCableBootstrapEvents {
    public static final int MIN_CABLE_DEPTH = 2;
    public static final int MAX_CABLE_DEPTH = 3;

    private static final Set<AtlantisHatchTableBlockEntity> PENDING =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<AtlantisHatchTableBlockEntity> DIAGNOSED =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final java.util.Map<AtlantisHatchTableBlockEntity,Long> NEXT_ATTEMPT=new WeakHashMap<>();

    private AtlantisHatchCableBootstrapEvents() {}

    public static void register(AtlantisHatchTableBlockEntity table) {
        if (table.getLevel() instanceof ServerLevel) {
            com.mustangdoc.sgjpatch.compat.ConcealedPowerBootstrapChanges.watch(table);
            PENDING.add(table);
        }
    }

    public static void unregister(AtlantisHatchTableBlockEntity table) {
        PENDING.remove(table);
        DIAGNOSED.remove(table);
        NEXT_ATTEMPT.remove(table);
    }

    /** The tabletop and its two legs span three columns across its facing. */
    public static boolean isBelowFootprint(AtlantisHatchTableBlockEntity table, BlockPos cablePos) {
        BlockPos anchor = table.getBlockPos();
        int depth = anchor.getY() - cablePos.getY();
        if (depth < MIN_CABLE_DEPTH || depth > MAX_CABLE_DEPTH) return false;
        Direction facing = table.getBlockState().getValue(AtlantisHatchTableBlock.FACING);
        // The bundled feed may sit below the floor immediately in front of the table.
        return facing.getAxis() == Direction.Axis.Z
                ? Math.abs(cablePos.getZ() - anchor.getZ()) <= 1 && Math.abs(cablePos.getX() - anchor.getX()) <= 1
                : Math.abs(cablePos.getX() - anchor.getX()) <= 1 && Math.abs(cablePos.getZ() - anchor.getZ()) <= 1;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 20L != 0L) return;

        for (AtlantisHatchTableBlockEntity table : PENDING.toArray(AtlantisHatchTableBlockEntity[]::new)) {
            if (table.isRemoved()) {
                unregister(table);
                continue;
            }
            if (table.getLevel() != level) continue;
            if(level.getGameTime()<NEXT_ATTEMPT.getOrDefault(table,0L))continue;
            NEXT_ATTEMPT.put(table,level.getGameTime()+200L);
            boolean connected = false;
            boolean cableFound = false;
            Direction across = table.getBlockState().getValue(AtlantisHatchTableBlock.FACING)
                    .getCounterClockWise();
            Direction along = table.getBlockState().getValue(AtlantisHatchTableBlock.FACING);
            search: for (int depth = MIN_CABLE_DEPTH; depth <= MAX_CABLE_DEPTH; depth++) {
                for (int offset = -1; offset <= 1; offset++) {
                  for (int forward = -1; forward <= 1; forward++) {
                    BlockPos cablePos = table.getBlockPos().relative(across, offset)
                            .relative(along, forward).below(depth);
                    if (!level.hasChunkAt(cablePos)
                            || !(level.getBlockState(cablePos).getBlock() instanceof CableBlock.LargeNaquadahCable)) continue;

                    cableFound = true;
                    // A saved floor can load before its cable endpoint. Recalculate the
                    // cable's upper connection as if that floor had just been replaced.
                    BlockState cableState = level.getBlockState(cablePos);
                    BlockPos abovePos = cablePos.above();
                    BlockState aboveState = level.getBlockState(abovePos);
                    BlockState refreshedState = cableState.updateShape(Direction.UP,
                            aboveState, level, cablePos, abovePos);
                    if (refreshedState != cableState) level.setBlock(cablePos, refreshedState, 3);
                    level.updateNeighborsAt(abovePos, aboveState.getBlock());
                    // SGJourney deliberately omits block entities on intermediate cable segments.
                    // Rebuild from the cable block, then update its actual network endpoints.
                    var endpoints = ConduitNetworks.findConnectedCables(level, cablePos);
                    for (CableBlockEntity part : endpoints) {
                        part.update();
                    }
                    ConduitNetworks networks = ConduitNetworks.get(level);
                    networks.update(level, cablePos);
                    boolean accepted = endpoints.stream()
                            .anyMatch(part -> networks.getCableNetwork(part.networkID()) != null);
                    if (accepted) {
                        connected = true;
                        System.out.println("[Atlantis DHD] Rebuilt concealed cable network at "
                                + cablePos + " for jumper bay door table " + table.getBlockPos()
                                + " (depth " + depth + ", endpoints " + endpoints.size() + ")");
                        break search;
                    }
                    break search;
                  }
                }
            }
            if (connected || !cableFound) unregister(table);
            else if (DIAGNOSED.add(table))
                System.out.println("[Atlantis DHD] Door table " + table.getBlockPos()
                        + (cableFound ? " found its concealed cable, but no usable network yet; retrying."
                                      : " has no concealed cable; waiting for a nearby block change."));
            if(cableFound)break; // Spread expensive cable rebuilds across ticks.
        }
    }
}
