package com.mustangdoc.sgjpatch.compat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.blocks.tech.CableBlock;
import net.povstalec.sgjourney.common.data.ConduitNetworks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = "sgjatlantis_dhd",
        bus = EventBusSubscriber.Bus.GAME)
public final class AtlantisDHDConcealedCableBootstrapEvents {
    private static final int CONCEALED_CABLE_DEPTH = 2;
    private static final Set<BlockEntity> PENDING_DHDS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<BlockEntity, Boolean> DIAGNOSED_DHDS = new WeakHashMap<>();
    private static final Map<BlockEntity, Long> NEXT_ATTEMPT = new WeakHashMap<>();

    private AtlantisDHDConcealedCableBootstrapEvents() {}

    public static void register(BlockEntity dhd) {
        Level level = dhd.getLevel();
        if (level == null || level.isClientSide()) return;

        ConcealedPowerBootstrapChanges.watch(dhd);
        PENDING_DHDS.add(dhd);
    }

    public static void unregister(BlockEntity dhd) {
        PENDING_DHDS.remove(dhd);
        DIAGNOSED_DHDS.remove(dhd);
        NEXT_ATTEMPT.remove(dhd);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()
                || event.getLevel().getGameTime() % 20L != 0L) return;

        for (BlockEntity dhd : PENDING_DHDS.toArray(BlockEntity[]::new)) {
            if (dhd.isRemoved()) {unregister(dhd);continue;}
            if (dhd.getLevel() != event.getLevel()) continue;
            if(event.getLevel().getGameTime()<NEXT_ATTEMPT.getOrDefault(dhd,0L))continue;
            NEXT_ATTEMPT.put(dhd,event.getLevel().getGameTime()+200L);

            BlockPos dhdPos = dhd.getBlockPos();
            BlockPos cablePos = findConcealedLargeCable(dhd);
            if (cablePos == null) {
                unregister(dhd);
                continue;
            }

            ConduitNetworks networks = ConduitNetworks.get(event.getLevel());
            var endpoints = ConduitNetworks.findConnectedCables(event.getLevel(), cablePos);
            for (CableBlockEntity cable : endpoints) {
                cable.update();
            }
            networks.update(event.getLevel(), cablePos);
            boolean networkAccepted = endpoints.stream().anyMatch(cable ->
                            networks.getCableNetwork(cable.networkID()) != null);
            if (!networkAccepted) {
                if (!Boolean.TRUE.equals(DIAGNOSED_DHDS.put(dhd, Boolean.TRUE))) {
                    System.out.println("[SGJPATCH-POWERBRIDGE] Found Large cable "
                            + cablePos + " for Atlantis DHD " + dhdPos
                            + " but SGJourney rejected the rebuilt output network");
                }
                break;
            }

            PENDING_DHDS.remove(dhd);
            DIAGNOSED_DHDS.remove(dhd);
            System.out.println("[SGJPATCH-POWERBRIDGE] Bootstrapped concealed Large cable "
                    + cablePos + " for Atlantis DHD " + dhdPos);
            break; // At most one expensive cable rebuild per level per second.
        }
    }

    private static BlockPos findConcealedLargeCable(BlockEntity dhd) {
        Level level = dhd.getLevel();
        BlockPos dhdPos = dhd.getBlockPos();
        Direction facing = dhd.getBlockState().getValue(
                BlockStateProperties.HORIZONTAL_FACING);
        BlockPos[] footprint = {
                dhdPos,
                dhdPos.relative(facing.getClockWise())
        };

        for (BlockPos footprintPos : footprint) {
            BlockPos cablePos = footprintPos.below(CONCEALED_CABLE_DEPTH);
            if (level.hasChunkAt(cablePos) && level.getBlockState(cablePos).getBlock()
                    instanceof CableBlock.LargeNaquadahCable) {
                return cablePos;
            }
        }
        return null;
    }

    private static void diagnoseMissingCable(BlockEntity dhd) {
        if (Boolean.TRUE.equals(DIAGNOSED_DHDS.put(dhd, Boolean.TRUE))) return;

        Level level = dhd.getLevel();
        BlockPos dhdPos = dhd.getBlockPos();
        Direction facing = dhd.getBlockState().getValue(
            BlockStateProperties.HORIZONTAL_FACING);
        BlockPos anchorCablePos = dhdPos.below(CONCEALED_CABLE_DEPTH);
        BlockPos outerCablePos = dhdPos.relative(facing.getClockWise())
            .below(CONCEALED_CABLE_DEPTH);
        System.out.println("[SGJPATCH-POWERBRIDGE] Atlantis DHD level event active at "
            + dhdPos + " but no Large cable block was found beneath its footprint at "
            + anchorCablePos + " or " + outerCablePos);
    }

}

