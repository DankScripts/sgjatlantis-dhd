package com.mustangdoc.sgjpatch.standalone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.misc.AutoCache;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.dhd.SGJourneyBlockEntityDHD;
import org.jetbrains.annotations.Nullable;

public final class AtlantisBlockEntityDHD implements SGJourneyBlockEntityDHD {
    private final AtlantisDHDEntity dhd;
    private final AutoCache.Receiver stargateCache = new AutoCache.Receiver(this);

    public AtlantisBlockEntityDHD(AtlantisDHDEntity dhd) {
        this.dhd = dhd;
        loadFromBlockEntity(dhd);
    }

    @Override
    public AutoCache.Receiver receiverCache() {
        return stargateCache;
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
    public @Nullable Vec3 getPosition() {
        return dhd.getBlockPos().getCenter();
    }

    @Override
    public double getMaxConnectionDistance() {
        return dhd.getMaxConnectionDistance();
    }

    @Override
    public Address getAddress() {
        return dhd.getAddress();
    }

    @Override
    public StargateInfo.FeedbackMessage engageStargate() {
        return AtlantisPendingDial.engage(this);
    }

    StargateInfo.FeedbackMessage engageNormally() {
        return SGJourneyBlockEntityDHD.super.engageStargate();
    }

    @Override
    public void updateFromStargate(DHDControlledStargate stargate, Address address,
                                   boolean shouldEngageCenterButton) {
        dhd.updateDHD(stargate, address, shouldEngageCenterButton);
    }

    @Override
    public boolean advancedProtocolsActive() {
        return dhd.advancedProtocolsActive();
    }

    @Override
    public int autocloseTicks() {
        return dhd.autocloseTicks();
    }

    @Override
    public boolean doCallForwarding() {
        return dhd.doCallForwarding();
    }

    @Override
    public boolean hasNetworkRestrictions() {
        return dhd.hasNetworkRestrictions();
    }

    @Override
    public java.util.Set<Integer> getNetworks() {
        return dhd.getNetworks();
    }

    @Override
    public void onDialAttempt(StargateInfo.FeedbackMessage feedback, Address address) {
        dhd.onDialAttempt(feedback, address);
    }

    @Override
    public void messageNearbyPlayers(Component message) {
        dhd.sendMessageToNearbyPlayers(message, net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity.DHD_INFO_DISTANCE);
    }

    @Override
    public void updateFromBlockEntity(BlockEntity blockEntity) {}
}
