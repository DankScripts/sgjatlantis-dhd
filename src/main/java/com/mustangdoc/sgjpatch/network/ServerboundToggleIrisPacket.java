package com.mustangdoc.sgjpatch.network;

import com.mustangdoc.sgjpatch.compat.LanteaIrisBridge;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDEntityAccessor;
import com.mustangdoc.sgjpatch.util.DhdLookup;
import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.MilkyWayStargateEntity;
import net.povstalec.sgjourney.common.blockstates.ShieldingState;
import net.povstalec.sgjourney.common.misc.CoordinateHelper.Relative;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

/** Reconstructed from the Forge baseline and the Lantea iris compatibility mixin. */
public final class ServerboundToggleIrisPacket {
    private static final float PEGASUS_SHIELD_TOGGLE_VOLUME = 16.0F;
    private final BlockPos dhdPos;

    public ServerboundToggleIrisPacket(BlockPos dhdPos) {
        this.dhdPos = dhdPos;
    }

    public static void encode(ServerboundToggleIrisPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.dhdPos);
    }

    public static ServerboundToggleIrisPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundToggleIrisPacket(buffer.readBlockPos());
    }

    public static void handle(ServerboundToggleIrisPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.level() instanceof ServerLevel level
                && player.distanceToSqr(packet.dhdPos.getX() + 0.5, packet.dhdPos.getY() + 0.5,
                        packet.dhdPos.getZ() + 0.5) <= 4096
                && level.getBlockEntity(packet.dhdPos) instanceof AbstractDHDEntity<?> dhd) {
            AbstractStargateEntity<?> gate = DhdLookup.resolveStargate(dhd);
            if (gate instanceof MilkyWayStargateEntity milkyWay) {
                toggleMilkyWay(level, milkyWay);
            } else if (gate == null) {
                context.enqueueWork(() -> LanteaIrisBridge.toggleNearest(level, packet.dhdPos));
            } else {
                context.enqueueWork(() -> togglePegasus(player, packet.dhdPos));
            }
        }
        context.setPacketHandled(true);
    }

    private static void toggleMilkyWay(ServerLevel level, MilkyWayStargateEntity gate) {
        IrisInfo iris = gate.irisInfo();
        if (!iris.hasIris()) return;

        var motion = iris.getIrisMotion();
        boolean opening = motion.isMoving ? motion.isClosing() : iris.isIrisClosed();
        if (opening ? iris.aboveMinProgress() : iris.belowMaxProgress()) {
            iris.setIrisMotion(opening ? IrisInfo.IrisMotion.OPENING_COMPUTER : IrisInfo.IrisMotion.CLOSING_COMPUTER);
            gate.setChanged();
            level.playSound(null, gate.getBlockPos(),
                    opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (motion.isMoving) {
            iris.setIrisMotion(IrisInfo.IrisMotion.IDLE);
            gate.updateClient();
        }
    }

    private static void togglePegasus(ServerPlayer player, BlockPos dhdPos) {
        Level level = player.level();
        if (!(level.getBlockEntity(dhdPos) instanceof AbstractDHDEntity<?> dhd)
                || player.distanceToSqr(dhdPos.getX() + 0.5, dhdPos.getY() + 0.5, dhdPos.getZ() + 0.5) > 4096)
            return;

        AbstractStargateEntity<?> gate = DhdLookup.resolveStargate(dhd);
        if (gate == null) {
            Vec3i relative = ((AbstractDHDEntityAccessor) dhd).sgjpatch$getStargateRelativePos();
            if (relative != null) {
                BlockPos resolved = Relative.getOffsetPos(dhd.getDirection(), dhdPos, relative);
                if (resolved != null && level.getBlockEntity(resolved) instanceof AbstractStargateEntity<?> found)
                    gate = found;
            }
        }
        if (!(gate instanceof IrisInfo.Interface irisHolder)) return;
        IrisInfo iris = irisHolder.irisInfo();
        if (iris == null || !iris.hasIris()) return;

        ItemStack irisStack = iris.getIris();
        boolean targetClosed = !iris.isIrisClosed();
        if (targetClosed && !PegasusShieldPowerHelper.canEngage(gate)) {
            PegasusShieldPowerHelper.notifyInsufficient(player);
            return;
        }

        iris.setIrisProgress(targetClosed ? (short) 58 : ShieldingState.OPEN.getProgress());
        gate.setStargateState(true, targetClosed ? ShieldingState.CLOSED : ShieldingState.OPEN);
        if (irisStack.is(SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get())) {
            level.playSound(null, gate.getBlockPos(),
                    targetClosed ? SGJPatchSounds.PEGASUS_SHIELD_CLOSING.get() : SGJPatchSounds.PEGASUS_SHIELD_OPENING.get(),
                    SoundSource.BLOCKS, PEGASUS_SHIELD_TOGGLE_VOLUME, 1.0F);
        }
        gate.updateClient();
    }
}
