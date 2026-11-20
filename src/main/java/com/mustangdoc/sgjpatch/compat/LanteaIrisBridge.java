package com.mustangdoc.sgjpatch.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Resolves a nearby Lantea gate when an Atlantis DHD has no SGJourney gate link. */
public final class LanteaIrisBridge {
    private static Level cachedLevel;
    private static BlockPos cachedControl;
    private static BlockEntity cachedGate;
    private static long cachedAt;
    private LanteaIrisBridge() { }

    public static boolean toggleNearest(ServerLevel level, BlockPos controlPos) {
        BlockEntity nearest = nearest(level, controlPos, true);
        if (!(nearest instanceof LanteaEnergyIrisState iris)) {
            notifyNearby(level, controlPos, Component.literal("No nearby Lantea gate has an Atlantis energy iris installed."));
            return false;
        }
        boolean closed = !iris.sgjpatch$irisClosed();
        iris.sgjpatch$setIris(true, closed);
        Component message = Component.literal(closed ? "Energy iris closed." : "Energy iris opened.");
        notifyNearby(level, controlPos, message);
        return true;
    }

    private static void notifyNearby(ServerLevel level, BlockPos controlPos, Component message) {
        for (var player : level.players())
            if (player.blockPosition().distSqr(controlPos) <= 64 * 64)
                player.displayClientMessage(message, true);
    }

    public static BlockEntity nearest(Level level, BlockPos controlPos, boolean installedOnly) {
        if (level.isClientSide() && !installedOnly && cachedLevel == level
                && controlPos.equals(cachedControl) && level.getGameTime() - cachedAt < 20)
            return cachedGate;
        BlockEntity nearest = null;
        double distance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) {
            cursor.set(controlPos.getX() + x, controlPos.getY(), controlPos.getZ() + z);
            if (!level.hasChunkAt(cursor)) continue;
            for (int y = -7; y <= 7; y++) {
                cursor.setY(controlPos.getY() + y);
                BlockEntity candidate = level.getBlockEntity(cursor);
                if (!(candidate instanceof LanteaEnergyIrisState iris)
                        || (installedOnly && !iris.sgjpatch$irisInstalled())) continue;
                double candidateDistance = cursor.distSqr(controlPos);
                if (candidateDistance < distance) {
                    distance = candidateDistance;
                    nearest = candidate;
                }
            }
        }
        if (level.isClientSide() && !installedOnly) {
            cachedLevel = level;
            cachedControl = controlPos.immutable();
            cachedGate = nearest;
            cachedAt = level.getGameTime();
        }
        return nearest;
    }
}
