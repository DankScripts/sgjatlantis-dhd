package com.mustangdoc.sgjpatch.mixin;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a virtual output from a Large Naquadah Cable to the nearby Atlantis
 * DHD. A free cable side is used so a real block entity above the cable (the
 * measured installation has a Mekanism Teleporter there) keeps its own power.
 */
@Mixin(targets = "net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity", remap = false)
public abstract class LargeNaquadahCableDHDPowerBridgeMixin {
    private static final int MAX_CONNECTED_CABLE_BLOCKS = 1024;
    private static final int CONCEALED_CABLE_DEPTH = 2;
    private static int sgjpatch$diagnosticsRemaining = 24;
    private static final String LARGE_CABLE_CLASS =
            "net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity$LargeNaquadahCable";
    private static final String STANDALONE_DHD_CLASS =
            "com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity";
    private static final String LEGACY_DHD_CLASS =
            "com.struxnet.sgjadditions.common.block_entities.AtlantisDHDEntity";

    @Inject(method = "getConnectedSides", at = @At(value = "RETURN", remap = false), remap = false)
    private void sgjpatch$addVirtualDhdOutput(
            CallbackInfoReturnable<List<Direction>> callback) {
        if (!sgjpatch$isLargeCable(this)) return;

        List<Direction> connectedSides = callback.getReturnValue();
        if (connectedSides == null) return;

        BlockEntity cable = (BlockEntity) (Object) this;
        Level level = cable.getLevel();
        BlockPos cablePos = cable.getBlockPos();
        if (level == null || cablePos == null
                || sgjpatch$hasDirectAtlantisDhd(level, cablePos)) return;

        BlockEntity receiver = sgjpatch$findNearbyAtlantisDhd(level, cablePos);
        if (receiver == null) return;

        Direction virtualSide = sgjpatch$findFreeSide(level, cablePos);
        if (virtualSide == null) {
            sgjpatch$log("No free virtual side on Large cable " + cablePos
                + " for Atlantis DHD " + receiver.getBlockPos());
            return;
        }

        if (!connectedSides.contains(virtualSide)) connectedSides.add(virtualSide);
        sgjpatch$log("Virtual side " + virtualSide + " on Large cable "
            + cablePos + " -> Atlantis DHD " + receiver.getBlockPos());
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
        if (cablePos == null || sgjpatch$hasDirectAtlantisDhd(level, cablePos)) {
            return null;
        }

        Direction virtualSide = sgjpatch$findFreeSide(level, cablePos);
        if (virtualSide == null
                || !cablePos.relative(virtualSide).equals(outputPos)) return null;

        BlockEntity receiver = sgjpatch$findNearbyAtlantisDhd(level, cablePos);
        if (receiver != null) {
            sgjpatch$log("Routed virtual side " + virtualSide + " from "
                    + cablePos + " to Atlantis DHD " + receiver.getBlockPos());
        }
        return receiver;
    }

    private static Direction sgjpatch$findFreeSide(Level level, BlockPos cablePos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(cablePos.relative(direction)) == null) {
                return direction;
            }
        }
        return null;
    }

    private static boolean sgjpatch$hasDirectAtlantisDhd(
            Level level, BlockPos cablePos) {
        for (Direction direction : Direction.values()) {
            if (sgjpatch$isAtlantisDhd(
                    level.getBlockEntity(cablePos.relative(direction)))) return true;
        }
        return false;
    }

    private static BlockEntity sgjpatch$findNearbyAtlantisDhd(
            Level level, BlockPos cablePos) {
        Block cableBlock = level.getBlockState(cablePos).getBlock();
        Queue<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(cablePos);

        while (!pending.isEmpty() && visited.size() < MAX_CONNECTED_CABLE_BLOCKS) {
            BlockPos currentPos = pending.remove();
            if (!visited.add(currentPos)
                    || level.getBlockState(currentPos).getBlock() != cableBlock) continue;

            BlockEntity receiver = sgjpatch$findAtlantisDhdAboveCable(
                    level, currentPos);
            if (receiver != null) return receiver;

            for (Direction direction : Direction.values()) {
                BlockPos connectedPos = currentPos.relative(direction);
                if (!visited.contains(connectedPos)) pending.add(connectedPos);
            }
        }
        return null;
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
        Class<?> type = candidate == null ? null : candidate.getClass();
        while (type != null) {
            String className = type.getName();
            if (STANDALONE_DHD_CLASS.equals(className)
                    || LEGACY_DHD_CLASS.equals(className)
                    || className.endsWith(".AtlantisDHDEntity")) return true;
            type = type.getSuperclass();
        }
        return false;
    }
}
