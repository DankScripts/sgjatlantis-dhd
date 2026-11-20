package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.hatch.AtlantisHatchTableBlockEntity;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchCableBootstrapEvents;
import com.mustangdoc.sgjpatch.compat.ConcealedCableRouting;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives concealed Atlantis tables separate virtual outputs on real cable endpoints.
 * Intermediate SGJourney cable segments need not have block entities.
 */
@Mixin(targets = "net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity", remap = false)
public abstract class LargeNaquadahCableDHDPowerBridgeMixin {
    @Redirect(method="transferEnergy", at=@At(value="INVOKE",
            target="Lnet/povstalec/sgjourney/common/data/ConduitNetworks$ConduitNetwork;transferEnergy(Lnet/minecraft/world/level/Level;JZZ)J",
            remap=false),remap=false)
    private long sgjpatch$guardMixedTransfer(
            net.povstalec.sgjourney.common.data.ConduitNetworks.ConduitNetwork network,
            Level level,long amount,boolean simulate,boolean zeroPoint){
        return com.mustangdoc.sgjpatch.power.MixedPowerTransfer.run(network,amount,simulate,zeroPoint,
                ()->network.transferEnergy(level,amount,simulate,zeroPoint));
    }
    private static final int MAX_CONNECTED_CABLE_BLOCKS = 1024;
    private static final int CONCEALED_CABLE_DEPTH = 2;
    private static int sgjpatch$diagnosticsRemaining = 24;
    private static final String LARGE_CABLE_CLASS =
            "net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity$LargeNaquadahCable";

    @Unique private long sgjpatch$routeTick = Long.MIN_VALUE;
    @Unique private Map<Direction, BlockEntity> sgjpatch$routes = Map.of();

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void sgjpatch$invalidateRoutes(CallbackInfo callback) {
        sgjpatch$routeTick = Long.MIN_VALUE;
        sgjpatch$routes = Map.of();
    }

    @Inject(method = "getConnectedSides", at = @At(value = "RETURN", remap = false),
            cancellable = true, remap = false)
    private void sgjpatch$addVirtualDhdOutput(
            CallbackInfoReturnable<List<Direction>> callback) {
        if (!sgjpatch$isLargeCable(this)) return;

        if (callback.getReturnValue() == null) return;

        BlockEntity cable = (BlockEntity) (Object) this;
        Level level = cable.getLevel();
        BlockPos cablePos = cable.getBlockPos();
        if (level == null || cablePos == null) return;

        List<Direction> connectedSides = new ArrayList<>(callback.getReturnValue());
        for (Direction side : sgjpatch$outputRoutes(level, cablePos).keySet())
            if (!connectedSides.contains(side)) connectedSides.add(side);
        // Do not leave virtual sides behind in SGJourney's cached physical connections.
        callback.setReturnValue(connectedSides);
    }

    @Redirect(
            method = {"isOutput", "validOutputs", "outputEnergy"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_7702_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
                    remap = false),
            remap = false)
    private BlockEntity sgjpatch$routeVirtualOutput(Level level, BlockPos outputPos) {
        BlockEntity directReceiver = level.getBlockEntity(outputPos);
        if (!sgjpatch$isLargeCable(this) || directReceiver != null) {
            return directReceiver;
        }

        BlockEntity cable = (BlockEntity) (Object) this;
        BlockPos cablePos = cable.getBlockPos();
        if (cablePos == null) return null;

        for (var route : sgjpatch$outputRoutes(level, cablePos).entrySet()) {
            BlockEntity receiver = route.getValue();
            if (cablePos.relative(route.getKey()).equals(outputPos)
                    && !receiver.isRemoved()
                    && level.getBlockEntity(receiver.getBlockPos()) == receiver) return receiver;
        }
        return null;
    }

    @Unique
    private Map<Direction, BlockEntity> sgjpatch$outputRoutes(Level level, BlockPos cablePos) {
        // Topology is stable between block updates. Do not rediscover the entire
        // cable run for every endpoint on every server tick. update() still
        // invalidates immediately; the bounded refresh catches remote changes.
        long now = level.getGameTime();
        if (sgjpatch$routeTick != Long.MIN_VALUE && now >= sgjpatch$routeTick
                && now - sgjpatch$routeTick < 20L) return sgjpatch$routes;
        Block cableBlock = level.getBlockState(cablePos).getBlock();
        List<BlockEntity> receivers = ConcealedCableRouting.findReceivers(cablePos,
                MAX_CONNECTED_CABLE_BLOCKS,
                pos -> level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() == cableBlock,
                pos -> {
                    List<BlockPos> neighbors = new ArrayList<>(6);
                    for (Direction side : Direction.values()) neighbors.add(pos.relative(side));
                    return neighbors;
                }, pos -> {
                    List<BlockEntity> above = sgjpatch$findConcealedHatchTables(level, pos);
                    BlockEntity dhd = sgjpatch$findAtlantisDhdAboveCable(level, pos);
                    if (dhd != null) above.add(dhd);
                    return above;
                });
        // Keep the original DHD first, but give each hatch its own output as well.
        receivers.sort(Comparator.comparingInt(receiver -> sgjpatch$isAtlantisDhd(receiver) ? 0 : 1));
        List<Direction> freeSides = new ArrayList<>(6);
        for (Direction side : Direction.values()) {
            BlockEntity direct = level.getBlockEntity(cablePos.relative(side));
            if (direct == null) freeSides.add(side);
            else receivers.remove(direct);
        }
        sgjpatch$routes = ConcealedCableRouting.assign(freeSides, receivers);
        sgjpatch$routeTick = level.getGameTime();
        return sgjpatch$routes;
    }

    private static List<BlockEntity> sgjpatch$findConcealedHatchTables(Level level, BlockPos cablePos) {
        List<BlockEntity> tables = new ArrayList<>();
        for (int depth = AtlantisHatchCableBootstrapEvents.MIN_CABLE_DEPTH;
                depth <= AtlantisHatchCableBootstrapEvents.MAX_CABLE_DEPTH; depth++) {
            BlockPos above = cablePos.above(depth);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockEntity receiver = level.getBlockEntity(above.offset(dx, 0, dz));
                    if (receiver instanceof AtlantisHatchTableBlockEntity table
                            && AtlantisHatchCableBootstrapEvents.isBelowFootprint(table, cablePos))
                        tables.add(table);
                }
            }
        }
        return tables;
    }

    private static BlockEntity sgjpatch$findAtlantisDhdAboveCable(
            Level level, BlockPos cablePos) {
        BlockPos footprintPos = cablePos.above(CONCEALED_CABLE_DEPTH);
        BlockEntity receiver = level.getBlockEntity(footprintPos);
        if (sgjpatch$isAtlantisDhd(receiver)) return receiver;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            receiver = level.getBlockEntity(footprintPos.relative(direction));
            if (!sgjpatch$isAtlantisDhd(receiver)) continue;

            Direction facing = receiver.getBlockState().getValue(
                    BlockStateProperties.HORIZONTAL_FACING);
            if (receiver.getBlockPos().relative(facing.getClockWise())
                    .equals(footprintPos)) return receiver;
        }
        return null;
    }

    private static void sgjpatch$log(String message) {
        if (sgjpatch$diagnosticsRemaining <= 0) return;
        --sgjpatch$diagnosticsRemaining;
        System.out.println("[SGJPATCH-POWERBRIDGE] " + message);
    }

    private static boolean sgjpatch$isLargeCable(Object candidate) {
        Class<?> type = candidate == null ? null : candidate.getClass();
        while (type != null) {
            String className = type.getName();
            if (LARGE_CABLE_CLASS.equals(className)
                    || className.endsWith("$LargeNaquadahCable")) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean sgjpatch$isAtlantisDhd(BlockEntity candidate) {
        return candidate instanceof AtlantisDHDEntity;
    }
}
