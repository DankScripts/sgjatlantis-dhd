package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;

/** FE buffer for the two independent jumper bay roof controls. */
public final class AtlantisHatchTableBlockEntity extends BlockEntity {
    public static final int OPERATION_COST = 50;
    public static final int MAX_ENERGY = 200;
    private final Buffer energy = new Buffer();
    private LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> energy);
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.restore(tag.getInt("Energy"));
    }

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> requested, @Nullable Direction side) {
        return requested == ForgeCapabilities.ENERGY ? capability.cast() : super.getCapability(requested, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capability = LazyOptional.of(() -> energy);
    }

    private final class Buffer extends EnergyStorage {
        private Buffer() { super(MAX_ENERGY, MAX_ENERGY, 0); }

        private boolean consume(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            return true;
        }

        private void restore(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received != 0) {
                setChanged();
                if (!loggedPowerDelivery) {
                    loggedPowerDelivery = true;
                    System.out.println("[Atlantis DHD] Door table " + worldPosition
                            + " received " + received + " FE; stored=" + getEnergyStored());
                }
            }
            return received;
        }
    }
}
