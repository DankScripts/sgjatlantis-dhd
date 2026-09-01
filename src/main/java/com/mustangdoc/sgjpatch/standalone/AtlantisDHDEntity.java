package com.mustangdoc.sgjpatch.standalone;

import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.povstalec.sgjourney.client.SyncedConfig;
import net.povstalec.sgjourney.common.block_entities.StructureGenEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.blockstates.ShieldingState;
import net.povstalec.sgjourney.common.block_entities.dhd.CrystalDHDEntity;
import net.povstalec.sgjourney.common.config.CommonDHDConfig;
import net.povstalec.sgjourney.common.config.CommonTechConfig;
import net.povstalec.sgjourney.common.init.ItemInit;
import net.povstalec.sgjourney.common.init.SoundInit;
import net.povstalec.sgjourney.common.items.energy_cores.FusionCoreItem;
import net.povstalec.sgjourney.common.sgjourney.PointOfOrigin;
import net.povstalec.sgjourney.common.sgjourney.Symbols;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

/**
 * Atlantis DHD backed by SGJourney's current Pegasus DHD behavior.
 * The physical model and UI remain supplied by this mod; energy/crystal/symbol behavior stays native to SGJourney.
 */
public class AtlantisDHDEntity extends CrystalDHDEntity {
    public AtlantisDHDEntity(BlockPos pos, BlockState state) {
        super(SGJPatchBlockEntities.ATLANTIS_DHD.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null || this.level.isClientSide())
            return;

        if (generationStep == Step.GENERATED) {
            if (stargateCache.isPresent())
                setSymbolsFromStargate();
            else
                setLocalSymbols();
        }
    }

    @Override
    protected long buttonPressEnergyCost() {
        return CommonDHDConfig.pegasus_dhd_button_press_energy_cost.getAsLong();
    }

    @Override
    public long getEnergyCapacity() {
        return level != null && level.isClientSide()
                ? SyncedConfig.pegasusDHDEnergyCapacity
                : CommonDHDConfig.pegasus_dhd_energy_buffer_capacity.getAsLong();
    }

    @Override
    public long getMaxEnergyReceive() {
        return CommonDHDConfig.pegasus_dhd_max_energy_receive.getAsLong();
    }

    @Override
    public long maxEnergyTransfer() {
        return this.maxEnergyTransfer < 0
                ? CommonDHDConfig.pegasus_dhd_max_energy_extract.getAsLong()
                : this.maxEnergyTransfer;
    }

    @Override
    protected SoundEvent getEnterSound() {
        return SoundInit.PEGASUS_DHD_ENTER.get();
    }

    @Override
    protected SoundEvent getPressSound() {
        return SoundInit.PEGASUS_DHD_PRESS.get();
    }

    /** Toggle the connected Pegasus Stargate iris/shield from the large blue DHD control. */
    public void toggleConnectedShield() {
        if (level == null || level.isClientSide() || !stargateCache.isPresent())
            return;

        AbstractStargateEntity<?> stargate = stargateCache.getCached();
        if (!(stargate instanceof IrisInfo.Interface irisGate))
            return;

        IrisInfo irisInfo = irisGate.irisInfo();
        if (!irisInfo.hasIris())
            return;

        boolean shouldClose = !irisInfo.isIrisClosed();
        irisInfo.setIrisMotion(IrisInfo.IrisMotion.IDLE);
        irisInfo.setIrisProgress(shouldClose ? ShieldingState.MAX_PROGRESS : ShieldingState.OPEN.getProgress());
        stargate.setStargateState(true, shouldClose ? ShieldingState.CLOSED : ShieldingState.OPEN);
        stargate.updateClient();
        stargate.setChanged();
    }

    public void clearSymbols() {
        symbolInfo().setPointOfOrigin(null);
        symbolInfo().setSymbols(null);
    }

    @Override
    protected void generateEnergyCore() {
        energyItemHandler.setStackInSlot(
                0,
                FusionCoreItem.randomFusionCore(
                        CommonTechConfig.fusion_core_fuel_capacity.getAsInt() / 2,
                        CommonTechConfig.fusion_core_fuel_capacity.getAsInt()
                )
        );
    }

    @Override
    public void generateAdditional(StructureGenEntity.Step generationStep) {
        if (generationStep == StructureGenEntity.Step.SETUP) {
            if (!PointOfOrigin.isValid(level.getServer(), symbolInfo().pointOfOrigin()))
                symbolInfo().setPointOfOrigin(null);
            if (!Symbols.isValid(level.getServer(), symbolInfo().symbols()))
                symbolInfo().setSymbols(null);
        } else if (stargateCache.isPresent()) {
            setSymbolsFromStargate();
        } else {
            setLocalSymbols();
        }

        crystalCache.recalculateCrystals();
    }

    @Override
    protected void generateCrystals() {
        crystalHandler.setStackInSlot(0, new ItemStack(ItemInit.LARGE_CONTROL_CRYSTAL.get()));
        crystalHandler.setStackInSlot(1, new ItemStack(ItemInit.ADVANCED_ENERGY_CRYSTAL.get()));
        crystalHandler.setStackInSlot(2, new ItemStack(ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get()));
        crystalHandler.setStackInSlot(3, new ItemStack(ItemInit.ADVANCED_ENERGY_CRYSTAL.get()));
        crystalHandler.setStackInSlot(6, new ItemStack(ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get()));
        crystalHandler.setStackInSlot(7, new ItemStack(ItemInit.ADVANCED_TRANSFER_CRYSTAL.get()));
    }
}
