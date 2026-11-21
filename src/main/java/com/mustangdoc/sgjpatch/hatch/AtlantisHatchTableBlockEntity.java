package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.core.Direction;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;

/** FE buffer for the two independent jumper bay roof controls. */
public final class AtlantisHatchTableBlockEntity extends BlockEntity {
    public static final int OPERATION_COST = 50;
    public static final int MAX_ENERGY = 200;
    private final Buffer energy = new Buffer();
    private boolean loggedPowerDelivery;

    public AtlantisHatchTableBlockEntity(BlockPos pos, BlockState state) {
        super(AtlantisHatchRegistry.TABLE_ENTITY, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        AtlantisHatchCableBootstrapEvents.register(this);
    }

    @Override
    public void setRemoved() {
        AtlantisHatchCableBootstrapEvents.unregister(this);
        super.setRemoved();
    }

    public int storedEnergy() { return energy.getEnergyStored(); }

    public boolean consumeOperation() {
        if (!energy.consume(OPERATION_COST)) return false;
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.restore(tag.getInt("Energy"));
    }

    public IEnergyStorage getEnergyHandler(Direction side) { return energy; }

    private final class Buffer extends SGJourneyEnergy {
        private Buffer() { super(MAX_ENERGY, MAX_ENERGY, 0L); }

        private boolean consume(int amount) {
            if (energy < amount) return false;
            depleteEnergy(amount, false);
            return true;
        }

        private void restore(int amount) {
            setEnergyNoUpdate(Math.max(0, Math.min(capacity, amount)));
        }

        @Override
        public void onEnergyChanged(long amount, boolean simulate) {
            if (!simulate) {
                setChanged();
                if (amount > 0 && !loggedPowerDelivery) {
                    loggedPowerDelivery = true;
                    System.out.println("[Atlantis DHD] Door table " + worldPosition
                            + " received " + amount + " FE; stored=" + getEnergyStored());
                }
            }
        }
    }
}
