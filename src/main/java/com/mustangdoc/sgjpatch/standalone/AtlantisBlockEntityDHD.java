package com.mustangdoc.sgjpatch.standalone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.dhd.SGJourneyBlockEntityDHD;
import net.povstalec.sgjourney.common.sgjourney.dhd.pegasus.PegasusDHD;
import org.jetbrains.annotations.Nullable;

public final class AtlantisBlockEntityDHD extends PegasusDHD
        implements SGJourneyBlockEntityDHD<AtlantisDHDEntity> {
    private final AtlantisDHDEntity dhd;
    private PendingDial pending;

    public AtlantisBlockEntityDHD(AtlantisDHDEntity dhd) {
        this.dhd = dhd;
        loadFromBlockEntity(dhd);
    }

    @Override
    public BlockPos getBlockPos() {
        return dhd.getBlockPos();
    }

    @Override
    public AtlantisDHDEntity getDHDBlockEntity() {
        return dhd;
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        Level level = dhd.getLevel();
        return level == null ? null : level.getServer();
    }

    @Override
    public @Nullable ResourceKey<Level> getDimension() {
        Level level = dhd.getLevel();
        return level == null ? null : level.dimension();
    }

    @Override
    public int autocloseTicks() {
        return dhd.autocloseTicks();
    }

    @Override
    public void updateFromBlockEntity(AtlantisDHDEntity blockEntity) {}

    @Override
    public StargateInfo.FeedbackMessage engageStargate() {
        if (!(dhd.getLevel() instanceof ServerLevel))
            return StargateInfo.Feedback.NONE.withInfo();
        receiverCache().markDirtyTwoWays();
        DHDControlledStargate gate = getStargate();
        if (gate == null || gate.isConnected()) {
            pending = null;
            return SGJourneyBlockEntityDHD.super.engageStargate();
        }
        if (pending != null) return StargateInfo.Feedback.NONE.withInfo();

        Address address = getAddress();
        if (address.regularSymbolCount() < 6) {
            if (!address.isEmpty() || !gate.getAddress().isEmpty()) {
                var result = gate.resetStargate(StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo(
                        Component.translatable("message.sgjourney.stargate.error.incomplete_address.dialing_aborted")));
                gate.updateDHD(this);
                return result;
            }
            messageNearbyPlayers(Component.literal("Enter at least six address symbols before dialing."));
            return StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo();
        }
        if (!address.hasPointOfOriginOrMaxLength()) {
            var result = encodeSymbol(0);
            if (result.feedback() != StargateInfo.Feedback.SYMBOL_ENCODED) {
                if (result.feedback().isError()) receiveStargateFeedback(result);
                else messageNearbyPlayers(Component.literal("The point of origin could not be encoded."));
                return result;
            }
        }
        Address queued = getAddress();
        if (!queued.canBeDialed()) {
            messageNearbyPlayers(Component.literal("The dialed address is incomplete."));
            return StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo();
        }
        pending = new PendingDial(gate, queued.toArray().clone());
        messageNearbyPlayers(Component.literal("Waiting for the Stargate chevrons to lock."));
        return StargateInfo.Feedback.NONE.withInfo();
    }

    void tickPendingDial() {
        var dial = pending;
        if (dial == null) return;
        var gate = dial.gate();
        if (getStargate() != gate || !gate.isLoaded() || gate.isConnected()
                || !dhd.hasRequiredDialingCrystals()
                || !java.util.Arrays.equals(dial.address(), getAddress().toArray())) {
            pending = null;
            messageNearbyPlayers(Component.literal("Pending Atlantis dial cancelled because the DHD or gate changed."));
            return;
        }
        if (!java.util.Arrays.equals(dial.address(), gate.getAddress().toArray())) return;
        pending = null;
        receiveStargateFeedback(SGJourneyBlockEntityDHD.super.engageStargate());
    }

    private record PendingDial(DHDControlledStargate gate, int[] address) {}
}
