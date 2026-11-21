package com.mustangdoc.sgjpatch.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import java.util.Arrays;
import java.util.List;

/** Optional SG Mobs gate controls. No SG Mobs classes are linked when the mod is absent. */
public final class SgmobsGateBridge {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String GATE_CLASS = "com.mike.wayfarer.GateBlockEntity";
    private static Level cachedLevel;
    private static BlockPos cachedControl;
    private static BlockEntity cachedGate;
    private static long cachedAt;

    private SgmobsGateBridge() { }

    public static BlockEntity nearest(Level level, BlockPos controlPos) {
        if (level == null || !net.neoforged.fml.ModList.get().isLoaded("sgjourney_mobs")) return null;
        if (cachedLevel == level && controlPos.equals(cachedControl)
                && level.getGameTime() - cachedAt < 10 && (cachedGate == null || !cachedGate.isRemoved()))
            return cachedGate;
        BlockEntity nearest = null;
        double distance = Double.MAX_VALUE;
        // The controller is registered with SG Mobs at the gate base, which
        // may be beyond the DHD's small block-entity search volume.
        if (level instanceof ServerLevel serverLevel) {
            try {
                Class<?> network = Class.forName("com.mike.wayfarer.GateNetwork");
                for (Object candidate : (List<?>) network.getMethod("loadedGates", ServerLevel.class)
                        .invoke(null, serverLevel)) {
                    if (!(candidate instanceof BlockEntity gate) || gate.isRemoved()) continue;
                    double candidateDistance = gate.getBlockPos().distSqr(controlPos);
                    if (candidateDistance <= 32 * 32 && candidateDistance < distance) {
                        nearest = gate;
                        distance = candidateDistance;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }
        if (nearest == null) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) {
            cursor.set(controlPos.getX() + x, controlPos.getY(), controlPos.getZ() + z);
            if (!level.hasChunkAt(cursor)) continue;
            for (int y = -7; y <= 7; y++) {
                cursor.setY(controlPos.getY() + y);
                BlockEntity candidate = level.getBlockEntity(cursor);
                if (candidate == null || !GATE_CLASS.equals(candidate.getClass().getName())) continue;
                double candidateDistance = cursor.distSqr(controlPos);
                if (candidateDistance < distance) {
                    distance = candidateDistance;
                    nearest = candidate;
                }
            }
        }
        }
        cachedLevel = level;
        cachedControl = controlPos.immutable();
        cachedGate = nearest;
        cachedAt = level.getGameTime();
        return nearest;
    }

    public static int dialStatus(BlockEntity gate, Player player, int[] address) {
        try {
            return (int) gate.getClass().getMethod("dialFromDhd", Player.class, int[].class)
                    .invoke(gate, player, address);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not dial SG Mobs gate at {}", gate.getBlockPos(), e);
            return -1;
        }
    }

    public static Boolean busy(BlockEntity gate) {
        try {
            return (boolean) gate.getClass().getMethod("dhdBusy").invoke(gate);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not read SG Mobs gate state at {}", gate.getBlockPos(), e);
            return null;
        }
    }

    public static boolean close(BlockEntity gate, Player player) {
        try {
            gate.getClass().getMethod("closeFromDhd", Player.class).invoke(gate, player);
            return true;
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not close SG Mobs gate at {}", gate.getBlockPos(), e);
            return false;
        }
    }

    public static boolean toggleIris(ServerLevel level, BlockPos controlPos, Player player) {
        BlockEntity gate = nearest(level, controlPos);
        if (!(gate instanceof IrisInfo.Interface irisGate) || !irisGate.irisInfo().hasIris()) {
            player.displayClientMessage(Component.literal("No nearby SG Mobs gate has an iris installed."), true);
            return false;
        }
        IrisInfo iris = irisGate.irisInfo();
        IrisInfo.IrisMotion motion = iris.isIrisClosed()
                ? IrisInfo.IrisMotion.OPENING_COMPUTER : IrisInfo.IrisMotion.CLOSING_COMPUTER;
        try {
            boolean accepted = (boolean) iris.getClass().getMethod("playerControl", Player.class,
                    IrisInfo.IrisMotion.class).invoke(iris, player, motion);
            if (accepted) player.displayClientMessage(Component.literal(
                    motion.isClosing() ? "Energy iris closing." : "Energy iris opening."), true);
            return accepted;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static int litMask(BlockEntity gate) {
        try {
            return (int) gate.getClass().getMethod("litMask", float.class).invoke(gate, 0F);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    /** Finds a loaded SG Mobs destination so the recent-address list can name its world. */
    public static BlockEntity destination(ServerLevel level, int[] address) {
        if (!net.neoforged.fml.ModList.get().isLoaded("sgjourney_mobs")) return null;
        try {
            Class<?> network = Class.forName("com.mike.wayfarer.GateNetwork");
            var loadedGates = network.getMethod("loadedGates", ServerLevel.class);
            for (ServerLevel world : level.getServer().getAllLevels()) {
                for (Object candidate : (List<?>) loadedGates.invoke(null, world)) {
                    if (!(candidate instanceof BlockEntity gate)) continue;
                    Object gateAddress = gate.getClass().getMethod("dialAddress").invoke(gate);
                    int[] symbols = (int[]) gateAddress.getClass().getMethod("symbols").invoke(gateAddress);
                    if (Arrays.equals(symbols, address)
                            || (symbols.length == address.length + 1 && symbols[symbols.length - 1] == 0
                                && Arrays.equals(Arrays.copyOf(symbols, address.length), address))) return gate;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return null;
    }
}
