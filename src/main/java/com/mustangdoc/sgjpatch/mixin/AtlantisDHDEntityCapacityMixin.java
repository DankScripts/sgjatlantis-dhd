package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.standalone.AtlantisBlockEntityDHD;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.items.ItemStackHandler;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.config.CommonDHDConfig;
import net.povstalec.sgjourney.common.config.CommonTechConfig;
import net.povstalec.sgjourney.common.init.SoundInit;
import net.povstalec.sgjourney.common.items.CallForwardingDevice;
import net.povstalec.sgjourney.common.items.crystals.CommunicationCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.ControlCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.EnergyCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.MemoryCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.TransferCrystalItem;
import net.povstalec.sgjourney.common.items.energy_cores.FusionCoreItem;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.memory_entry.StargateConnectionEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AtlantisDHDEntity.class, remap = false)
public abstract class AtlantisDHDEntityCapacityMixin {
    @Shadow @Final protected ItemStackHandler itemHandler;

    protected long capacity() {
        return CommonDHDConfig.pegasus_dhd_energy_buffer_capacity.get();
    }

    protected long getCapacity() {
        return capacity();
    }

    protected long maxReceive() {
        return CommonDHDConfig.pegasus_dhd_max_energy_receive.get();
    }

    protected long getMaxReceive() {
        return maxReceive();
    }

    protected long maxExtract() {
        return CommonDHDConfig.pegasus_dhd_max_energy_extract.get();
    }

    protected long getMaxExtract() {
        return maxExtract();
    }

    protected long buttonPressEnergyCost() {
        return Math.max(1L, CommonDHDConfig.pegasus_dhd_button_press_energy_cost.get() / 100L);
    }

    public long maxEnergyDeplete() {
        long maxTransfer = ((AbstractDHDEntityAccessor) this).sgjpatch$getMaxEnergyTransfer();
        return maxTransfer < 0L ? CommonDHDConfig.pegasus_dhd_max_energy_extract.get() : maxTransfer;
    }

    public boolean enableAdvancedProtocols() {
        return ((AbstractDHDEntity) (Object) this).advancedProtocolsActive();
    }

    protected long getMaxDeplete() {
        return maxEnergyDeplete();
    }

    protected SoundEvent getEnterSound() {
        return SoundInit.PEGASUS_DHD_ENTER.get();
    }

    protected SoundEvent getPressSound() {
        return SoundInit.PEGASUS_DHD_PRESS.get();
    }

    protected void generateEnergyCore() {
        int capacity = CommonTechConfig.fusion_core_fuel_capacity.get();
        ((AbstractDHDEntityAccessor) this).sgjpatch$getEnergyItemHandler()
                .setStackInSlot(0, FusionCoreItem.randomFusionCore(capacity / 2, capacity));
    }

    @Overwrite
    public void recalculateCrystals() {
        AtlantisDHDEntity dhd = (AtlantisDHDEntity) (Object) this;
        AbstractDHDEntityAccessor fields = (AbstractDHDEntityAccessor) this;
        var networks = dhd.getNetworks();
        networks.clear();

        boolean advancedProtocols = false;
        boolean callForwarding = false;
        boolean networkRestrictions = false;
        long energyTarget = 0L;
        long maxTransfer = 0L;
        int distance = AbstractDHDEntity.DEFAULT_CONNECTION_DISTANCE;
        int regularEnergy = 0, advancedEnergy = 0, regularTransfer = 0, advancedTransfer = 0;

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            var stack = itemHandler.getStackInSlot(slot);
            var item = stack.getItem();
            if (item instanceof EnergyCrystalItem crystal) {
                if (crystal.isAdvanced()) advancedEnergy++; else regularEnergy++;
                energyTarget += crystal.energyTargetIncrease();
            } else if (item instanceof TransferCrystalItem crystal) {
                if (crystal.isAdvanced()) advancedTransfer++; else regularTransfer++;
                maxTransfer += crystal.getMaxTransfer();
            } else if (item instanceof CommunicationCrystalItem crystal) {
                if (CommunicationCrystalItem.hasFrequency(stack))
                    networks.add(CommunicationCrystalItem.getFrequency(stack));
                else
                    distance += crystal.getRangeIncrease();
            } else if (item instanceof ControlCrystalItem crystal) {
                if (crystal.isLarge()) advancedProtocols = true;
                else networkRestrictions = true;
            } else if (item instanceof CallForwardingDevice) {
                callForwarding = true;
            }

        }

        fields.sgjpatch$setAdvancedProtocolsActive(advancedProtocols);
        fields.sgjpatch$setDoCallForwarding(callForwarding);
        fields.sgjpatch$setHasNetworkRestrictions(networkRestrictions);
        fields.sgjpatch$setEnergyTarget(regularEnergy >= 4 || advancedEnergy >= 3 ? -1L : energyTarget);
        fields.sgjpatch$setMaxEnergyTransfer(regularTransfer >= 4 || advancedTransfer >= 3 ? -1L : maxTransfer);
        fields.sgjpatch$setMaxConnectionDistance(distance);

        if (dhd.getLevel() != null && !dhd.getLevel().isClientSide()) {
            AtlantisBlockEntityDHD adapter = (AtlantisBlockEntityDHD) dhd.getDHD();
            adapter.receiverCache().markDirtyTwoWays();
            var stargate = adapter.getStargate();
            if (stargate != null) {
                dhd.setSymbolsFromStargate();
                stargate.updateFromDHD(adapter);
            }
            dhd.updateClient();
        }
    }

    @Overwrite
    public void onDialAttempt(StargateInfo.FeedbackMessage feedback, Address address) {
        AtlantisDHDEntity dhd = (AtlantisDHDEntity) (Object) this;
        CompoundTag entry = new StargateConnectionEntry("", dhd.getLevel().getGameTime(),
                new StargateConnection.Result(address, feedback.feedback())).save();
        for (int slot = 0; slot < itemHandler.getSlots() && entry != null; slot++) {
            var stack = itemHandler.getStackInSlot(slot);
            if (stack.getItem() instanceof MemoryCrystalItem crystal)
                entry = crystal.saveCompound(stack, entry, true);
        }
    }

    @Overwrite
    public int getMaxDistance() {
        return (int) ((AbstractDHDEntity) (Object) this).getMaxConnectionDistance();
    }

    @Overwrite
    public long maxEnergyTransfer() {
        return maxEnergyDeplete();
    }

    public long getEnergyCapacity() {
        return capacity();
    }

    public long getMaxEnergyReceive() {
        return maxReceive();
    }

    public long getMaxEnergyExtract() {
        return maxExtract();
    }
}
