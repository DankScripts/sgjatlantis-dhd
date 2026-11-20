package com.mustangdoc.sgjpatch.standalone;

import com.mustangdoc.sgjpatch.compat.AtlantisDhdCrystalRequirements;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AtlantisPendingDial {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Atlantis DHD/Dial");
    private static final Map<AtlantisBlockEntityDHD, Pending> PENDING = new IdentityHashMap<>();

    private AtlantisPendingDial() {}

    static StargateInfo.FeedbackMessage engage(AtlantisBlockEntityDHD dhd) {
        var entity = dhd.getDHDBlockEntity();
        if (!(entity.getLevel() instanceof ServerLevel)) {
            throw new IllegalStateException("Atlantis dialing must run on the server");
        }
        dhd.receiverCache().markDirtyTwoWays();
        DHDControlledStargate gate = dhd.getStargate();
        if (gate == null || gate.isConnected()) {
            PENDING.remove(dhd);
            return dhd.engageNormally();
        }
        if (PENDING.containsKey(dhd)) {
            return StargateInfo.Feedback.NONE.withInfo();
        }
        Address address = dhd.getAddress();
        var action = AtlantisDialProgress.centerAction(address.regularSymbolCount(), !address.isEmpty(),
                !gate.getAddress().isEmpty());
        if (action == AtlantisDialProgress.CenterAction.CLEAR) {
            var result = gate.resetStargate(StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo(
                    Component.translatable("message.sgjourney.stargate.error.incomplete_address.dialing_aborted")));
            gate.updateDHD(dhd);
            return result;
        }
        if (action == AtlantisDialProgress.CenterAction.EMPTY) {
            dhd.messageNearbyPlayers(Component.literal("Enter at least six address symbols before dialing."));
            return StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo();
        }
        if (!address.hasPointOfOriginOrMaxLength()) {
            StargateInfo.FeedbackMessage result = dhd.encodeSymbol(0);
            if (result.feedback() != StargateInfo.Feedback.SYMBOL_ENCODED) {
                dhd.receiveStargateFeedback(result);
                if (!result.feedback().isError()) {
                    LOGGER.warn("Atlantis DHD at {} could not encode its point of origin: {}", entity.getBlockPos(), result);
                    dhd.messageNearbyPlayers(Component.literal("The point of origin could not be encoded."));
                }
                return result;
            }
        }
        Address queuedAddress = dhd.getAddress();
        if (!queuedAddress.canBeDialed()) {
            LOGGER.warn("Atlantis DHD at {} has an invalid buffered address after encoding the point of origin", entity.getBlockPos());
            dhd.messageNearbyPlayers(Component.literal("The dialed address is incomplete."));
            return StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo();
        }
        PENDING.put(dhd, new Pending(gate, new AtlantisDialProgress(queuedAddress.toArray())));
        dhd.messageNearbyPlayers(Component.literal("Waiting for the Stargate chevrons to lock."));
        return StargateInfo.Feedback.NONE.withInfo();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        Iterator<Map.Entry<AtlantisBlockEntityDHD, Pending>> entries = PENDING.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            AtlantisBlockEntityDHD dhd = entry.getKey();
            var entity = dhd.getDHDBlockEntity();
            Pending pending = entry.getValue();
            if (entity.isRemoved() || !(entity.getLevel() instanceof ServerLevel level)
                    || !level.hasChunkAt(entity.getBlockPos()) || level.getBlockEntity(entity.getBlockPos()) != entity) {
                entries.remove();
                LOGGER.warn("Cancelled pending Atlantis dial at {} because its DHD was unloaded or removed", entity.getBlockPos());
                continue;
            }
            AtlantisDialProgress.Decision decision = pending.progress().decide(
                    dhd.getStargate() == pending.gate(), pending.gate().isLoaded(), pending.gate().isConnected(),
                    AtlantisDhdCrystalRequirements.canDial(entity), dhd.getAddress().toArray(),
                    pending.gate().getAddress().toArray());
            if (decision == AtlantisDialProgress.Decision.CANCEL) {
                entries.remove();
                LOGGER.warn("Cancelled pending Atlantis dial at {} because its gate, address, connection or crystals changed", entity.getBlockPos());
                dhd.messageNearbyPlayers(Component.literal("Pending Atlantis dial cancelled because the DHD or gate changed."));
                continue;
            }
            if (decision == AtlantisDialProgress.Decision.WAIT) continue;
            entries.remove();
            StargateInfo.FeedbackMessage result = dhd.engageNormally();
            dhd.receiveStargateFeedback(result);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Iterator<Map.Entry<AtlantisBlockEntityDHD, Pending>> entries = PENDING.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (entry.getKey().getServer() == event.getServer()) {
                LOGGER.warn("Cancelled pending Atlantis dial at {} because the server is stopping",
                        entry.getKey().getBlockPos());
                entries.remove();
            }
        }
    }

    private record Pending(DHDControlledStargate gate, AtlantisDialProgress progress) {}
}
