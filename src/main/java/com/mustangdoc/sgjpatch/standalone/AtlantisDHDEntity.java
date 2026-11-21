package com.mustangdoc.sgjpatch.standalone;

import com.mustangdoc.sgjpatch.compat.AtlantisDHDConcealedCableBootstrapEvents;
import com.mustangdoc.sgjpatch.compat.AtlantisDhdCrystalRequirements;
import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.povstalec.sgjourney.StargateJourney;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.config.CommonDHDConfig;
import net.povstalec.sgjourney.common.config.CommonTechConfig;
import net.povstalec.sgjourney.common.init.ItemInit;
import net.povstalec.sgjourney.common.init.SoundInit;
import net.povstalec.sgjourney.common.items.CallForwardingDevice;
import net.povstalec.sgjourney.common.items.crystals.AbstractCrystalItem;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reconstructed from the Forge 2.0.5 baseline and its applied compatibility
 * mixins. The legacy inventory format is retained for existing worlds.
 */
public class AtlantisDHDEntity extends AbstractDHDEntity<AtlantisBlockEntityDHD> {
    protected int[] memoryCrystals = new int[0];
    protected int[] controlCrystals = new int[0];
    protected int[] energyCrystals = new int[0];
    protected int[] transferCrystals = new int[0];
    protected int[] communicationCrystals = new int[0];
    protected final ItemStackHandler itemHandler = createHandler();
    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);

    public AtlantisDHDEntity(BlockPos pos, BlockState state) {
        super(SGJPatchBlockEntities.ATLANTIS_DHD.get(), pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("Inventory"));
        addTransferCrystals(itemHandler);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt) {
        nbt.put("Inventory", itemHandler.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        recalculateCrystals();
        AtlantisDHDConcealedCableBootstrapEvents.register(this);
    }

    @Override
    public void invalidateCaps() {
        handler.invalidate();
        super.invalidateCaps();
        AtlantisDHDConcealedCableBootstrapEvents.unregister(this);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY && lazyEnergyHandler != null)
            return lazyEnergyHandler.cast();
        if (capability == ForgeCapabilities.ITEM_HANDLER)
            return handler.cast();
        return super.getCapability(capability, side);
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(9) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                recalculateCrystals();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return isValidCrystal(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            @Nonnull
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
            }
        };
    }

    protected boolean isValidCrystal(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() instanceof CallForwardingDevice
                    || stack.getItem() instanceof AbstractCrystalItem crystal
                        && (crystal.isLarge() || crystal.isAdvanced()));
    }

    public void recalculateCrystals() {
        networks.clear();
        boolean advancedProtocols = false;
        boolean callForwarding = false;
        boolean networkRestrictions = false;
        long target = 0L;
        long transfer = 0L;
        int distance = DEFAULT_CONNECTION_DISTANCE;
        int regularEnergy = 0, advancedEnergy = 0, regularTransfer = 0, advancedTransfer = 0;

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            var stack = itemHandler.getStackInSlot(slot);
            var item = stack.getItem();
            if (item instanceof EnergyCrystalItem crystal) {
                if (crystal.isAdvanced()) advancedEnergy++; else regularEnergy++;
                target += crystal.energyTargetIncrease();
            } else if (item instanceof TransferCrystalItem crystal) {
                if (crystal.isAdvanced()) advancedTransfer++; else regularTransfer++;
                transfer += crystal.getMaxTransfer();
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

        advancedProtocolsActive = advancedProtocols;
        doCallForwarding = callForwarding;
        hasNetworkRestrictions = networkRestrictions;
        energyTarget = regularEnergy >= 4 || advancedEnergy >= 3 ? -1L : target;
        maxEnergyTransfer = regularTransfer >= 4 || advancedTransfer >= 3 ? -1L : transfer;
        maxConnectionDistance = distance;

        if (level != null && !level.isClientSide()) {
            AtlantisBlockEntityDHD adapter = getDHD();
            adapter.receiverCache().markDirtyTwoWays();
            var stargate = adapter.getStargate();
            if (stargate != null) {
                setSymbolsFromStargate();
                stargate.updateFromDHD(adapter);
            }
            updateClient();
        }
    }

    public static void addTransferCrystals(ItemStackHandler itemHandler) {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.is(ItemInit.ADVANCED_ENERGY_CRYSTAL.get())
                    && stack.hasTag() && stack.getTag().getString("CrystalMode").equals("ENERGY_TRANSFER")) {
                itemHandler.setStackInSlot(slot, new ItemStack(ItemInit.TRANSFER_CRYSTAL.get()));
                StargateJourney.LOGGER.info("Replaced Transfer Crystal");
            }
        }
    }

    @Override
    protected AtlantisBlockEntityDHD fetchDHD() {
        return new AtlantisBlockEntityDHD(this);
    }

    @Override
    public void pressButton(int symbol) {
        if (symbol >= 0 && !AtlantisDhdCrystalRequirements.canDial(this))
            return;
        super.pressButton(symbol);
    }

    @Override
    public StargateInfo.FeedbackMessage engageStargate() {
        if (!AtlantisDhdCrystalRequirements.canDial(this))
            return StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
        return super.engageStargate();
    }

    @Override
    public StargateInfo.FeedbackMessage encodeSymbol(int symbol) {
        if (symbol >= 0 && !AtlantisDhdCrystalRequirements.canDial(this))
            return StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
        return super.encodeSymbol(symbol);
    }

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

    @Override
    protected long buttonPressEnergyCost() {
        return Math.max(1L, CommonDHDConfig.pegasus_dhd_button_press_energy_cost.get() / 100L);
    }

    public long maxEnergyDeplete() {
        return maxEnergyTransfer < 0L ? CommonDHDConfig.pegasus_dhd_max_energy_extract.get() : maxEnergyTransfer;
    }

    public boolean enableAdvancedProtocols() {
        return advancedProtocolsActive();
    }

    protected long getMaxDeplete() {
        return maxEnergyDeplete();
    }

    @Override
    protected SoundEvent getEnterSound() {
        return SoundInit.PEGASUS_DHD_ENTER.get();
    }

    @Override
    protected SoundEvent getPressSound() {
        return SoundInit.PEGASUS_DHD_PRESS.get();
    }

    @Override
    protected void generateEnergyCore() {
        int capacity = CommonTechConfig.fusion_core_fuel_capacity.get();
        energyItemHandler.setStackInSlot(0, FusionCoreItem.randomFusionCore(capacity / 2, capacity));
    }

    @Override
    public void onDialAttempt(StargateInfo.FeedbackMessage feedback, Address address) {
        CompoundTag entry = new StargateConnectionEntry("", level.getGameTime(),
                new StargateConnection.Result(address, feedback.feedback())).save();
        for (int slot = 0; slot < itemHandler.getSlots() && entry != null; slot++) {
            var stack = itemHandler.getStackInSlot(slot);
            if (stack.getItem() instanceof MemoryCrystalItem crystal)
                entry = crystal.saveCompound(stack, entry, true);
        }
    }

    public int getMaxDistance() {
        return (int) getMaxConnectionDistance();
    }

    @Override
    public long maxEnergyTransfer() {
        return maxEnergyDeplete();
    }

    @Override
    public long getEnergyCapacity() {
        return capacity();
    }

    @Override
    public long getMaxEnergyReceive() {
        return maxReceive();
    }

    @Override
    public long getMaxEnergyExtract() {
        return maxExtract();
    }
}
