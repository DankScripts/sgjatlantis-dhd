package com.mustangdoc.sgjpatch.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.capabilities.ZeroPointEnergy;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;
import net.povstalec.sgjourney.common.config.CommonZPMConfig;
import net.povstalec.sgjourney.common.init.ItemInit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes SG Mobs' three-slot hub use the same ZPM-to-cable transfer as our port. */
@Pseudo
@Mixin(targets = "com.mike.sgjourney_mobs.zpm.TripleZpmHubBlockEntity", remap = false)
public abstract class SgmobsTripleZpmHubPowerMixin {
    @Shadow @Final public ItemStackHandler inventory;
    @Shadow private int firstSlot;

    /** SG Mobs inherits an empty energy buffer, so cable consumers see no ZPMs. */
    public SGJourneyEnergy createEnergyStorage() {
        EnergyBlockEntity hub = (EnergyBlockEntity) (Object) this;
        return new SGJourneyEnergy(Long.MAX_VALUE, 0L, hub.getMaxEnergyExtract()) {
            @Override public long getTrueEnergyStored() {
                if (inventory == null) return 0L;
                long total = 0L;
                for (int slot = 0; slot < 3; slot++) {
                    ZeroPointEnergy zpm = sgjpatch$moduleEnergy(slot);
                    if (zpm == null) continue;
                    long amount = Math.max(0L, zpm.getTrueEnergyStored());
                    total = Long.MAX_VALUE - total < amount ? Long.MAX_VALUE : total + amount;
                }
                return total;
            }

            @Override public boolean hasEnergy() { return getTrueEnergyStored() > 0L; }
            @Override public boolean hasEnergy(long amount) { return getTrueEnergyStored() >= amount; }

            @Override public long depleteEnergy(long amount, boolean simulate) {
                if (inventory == null) return 0L;
                long requested = Math.max(0L, Math.min(amount, hub.getMaxEnergyExtract()));
                long left = requested;
                for (int step = 0; step < 3 && left > 0L; step++) {
                    ZeroPointEnergy zpm = sgjpatch$moduleEnergy((firstSlot + step) % 3);
                    if (zpm != null) left -= zpm.extractLongEnergy(left, simulate);
                }
                if (!simulate && requested != left) hub.setChanged();
                return requested - left;
            }
            @Override public void onEnergyChanged(long amount, boolean simulate) { }
        };
    }

    private ZeroPointEnergy sgjpatch$moduleEnergy(int slot) {
        if (inventory == null) return null;
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.is(ItemInit.ZPM.get())) return null;
        BlockEntity hub = (BlockEntity) (Object) this;
        return new ZeroPointEnergy(stack, CommonZPMConfig.zpm_hub_max_transfer.get()) {
            @Override public void onEnergyChanged(long amount, boolean simulate) {
                if (simulate) return;
                updateZPMItem(stack);
                hub.setChanged();
            }
        };
    }

    @Inject(method = "outputEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjpatch$outputInstalledZpms(Direction direction, CallbackInfo ci) {
        ci.cancel();
        BlockEntity hub = (BlockEntity) (Object) this;
        Level level = hub.getLevel();
        if (level == null || level.isClientSide() || inventory == null || direction != Direction.DOWN) return;

        BlockPos cablePos = hub.getBlockPos().below();
        IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                cablePos, Direction.UP);
        if (receiver == null) return;

        long transferLimit = CommonZPMConfig.zpm_hub_max_transfer.get();
        boolean changed = false;
        for (int step = 0; step < 3; step++) {
            ZeroPointEnergy zpm = sgjpatch$moduleEnergy((firstSlot + step) % 3);
            if (zpm == null) continue;
            long available = zpm.extractLongEnergy(transferLimit, true);
            if (available <= 0) continue;
            long accepted;
            if (receiver instanceof SGJourneyEnergy sgEnergy) {
                accepted = sgEnergy.receiveZeroPointEnergy(available, true);
                if (accepted > 0) {
                    long extracted = zpm.extractLongEnergy(accepted, false);
                    accepted = sgEnergy.receiveZeroPointEnergy(extracted, false);
                }
            } else if (CommonZPMConfig.other_mods_use_zero_point_energy.get()) {
                int offer = SGJourneyEnergy.regularEnergy(available);
                int canReceive = receiver.receiveEnergy(offer, true);
                long extracted = zpm.extractLongEnergy(canReceive, false);
                accepted = receiver.receiveEnergy((int) Math.min(Integer.MAX_VALUE, extracted), false);
            } else continue;
            changed |= accepted > 0;
        }
        firstSlot = (firstSlot + 1) % 3;
        if (changed) {
            hub.setChanged();
            level.sendBlockUpdated(hub.getBlockPos(), hub.getBlockState(), hub.getBlockState(), 2);
        }
    }
}
