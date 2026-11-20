package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.povstalec.sgjourney.common.block_entities.ProtectedBlockEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.capabilities.ZeroPointEnergy;
import net.povstalec.sgjourney.common.config.CommonPermissionConfig;
import net.povstalec.sgjourney.common.config.CommonZPMConfig;
import net.povstalec.sgjourney.common.init.ItemInit;

/** Three SGJourney ZPM sockets. Ported from SG Mobs with Mike's permission. */
public final class TripleZpmHubBlockEntity extends EnergyBlockEntity implements ProtectedBlockEntity {
    public static final int SLOT_COUNT = 3;
    private boolean protectedHub;
    private int firstSlot;
    public final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ItemInit.ZPM.get());
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            syncInventory();
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private final IItemHandler protectedPowerInventory = new IItemHandler() {
        @Override public int getSlots() { return SLOT_COUNT; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };
    private LazyOptional<IItemHandler> protectedPowerCapability = LazyOptional.of(() -> protectedPowerInventory);

    public TripleZpmHubBlockEntity(BlockPos pos, BlockState state) {
        super(TripleZpmHubRegistration.BLOCK_ENTITY, pos, state);
    }

    private void syncInventory() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag items = tag.getCompound("inventory").copy();
        items.putInt("Size", SLOT_COUNT);
        inventory.deserializeNBT(items);
        protectedHub = tag.getBoolean("protected");
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putBoolean("protected", protectedHub);
    }

    @Override public void invalidateCaps() {
        itemCapability.invalidate();
        protectedPowerCapability.invalidate();
        super.invalidateCaps();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> inventory);
        protectedPowerCapability = LazyOptional.of(() -> protectedPowerInventory);
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER)
            return protectedHub && !CommonPermissionConfig.protected_inventory_access.get()
                    ? protectedPowerCapability.cast() : itemCapability.cast();
        return super.getCapability(capability, side);
    }

    public void drops() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
        }
    }

    @Override public boolean isCorrectEnergySide(Direction side) { return side == Direction.DOWN; }
    @Override public long getEnergyCapacity() { return CommonZPMConfig.zpm_energy_per_entropy_level.get(); }
    @Override public long getMaxEnergyReceive() { return 0L; }
    @Override public long getMaxEnergyExtract() { return CommonZPMConfig.zpm_hub_max_transfer.get(); }

    @Override public void outputEnergy(Direction direction) {
        if (level == null || level.isClientSide) return;
        var target = level.getBlockEntity(worldPosition.relative(direction));
        if (target == null) return;
        IEnergyStorage receiver = target.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
        if (receiver == null) return;
        for (int step = 0; step < SLOT_COUNT; step++) {
            ZeroPointEnergy zpm = moduleEnergy((firstSlot + step) % SLOT_COUNT);
            if (zpm == null) continue;
            if (receiver instanceof SGJourneyEnergy sgEnergy)
                zpm.fillSGJourneyEnergyStorage(sgEnergy, getMaxEnergyDeplete());
            else if (CommonZPMConfig.other_mods_use_zero_point_energy.get())
                zpm.fillOtherEnergyStorage(receiver, getMaxEnergyDeplete());
        }
        firstSlot = (firstSlot + 1) % SLOT_COUNT;
        if (level.getGameTime() % 20 == 0) syncInventory();
        setChanged();
    }

    
    /** The exported capability is backed by the installed modules, not an empty buffer. */
    @Override public SGJourneyEnergy createEnergyStorage() {
        return new SGJourneyEnergy(Long.MAX_VALUE, 0L, getMaxEnergyExtract()) {
            @Override public long getTrueEnergyStored() {
                long total = 0;
                if (inventory == null) return 0;
                for (int slot = 0; slot < SLOT_COUNT; slot++) {
                    ZeroPointEnergy zpm = moduleEnergy(slot);
                    if (zpm == null) continue;
                    long available = Math.max(0L, zpm.getTrueEnergyStored());
                    total = Long.MAX_VALUE - total < available ? Long.MAX_VALUE : total + available;
                }
                return total;
            }
            @Override public boolean hasEnergy() { return getTrueEnergyStored() > 0L; }
            @Override public boolean hasEnergy(long amount) { return getTrueEnergyStored() >= amount; }
            @Override public int extractEnergy(int amount, boolean simulate) {
                return CommonZPMConfig.other_mods_use_zero_point_energy.get()
                        ? super.extractEnergy(amount, simulate) : 0;
            }
            @Override public long depleteEnergy(long amount, boolean simulate) {
                long left = Math.max(0L, Math.min(amount, getMaxEnergyExtract()));
                long requested = left;
                if (inventory == null) return 0;
                for (int step = 0; step < SLOT_COUNT && left > 0; step++) {
                    ZeroPointEnergy zpm = moduleEnergy((firstSlot + step) % SLOT_COUNT);
                    if (zpm != null) left -= zpm.extractLongEnergy(left, simulate);
                }
                if (!simulate && requested != left) { setChanged(); syncInventory(); }
                return requested - left;
            }
            @Override public void onEnergyChanged(long amount, boolean simulate) { }
        };
    }
    private ZeroPointEnergy moduleEnergy(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.is(ItemInit.ZPM.get())) return null;
        return new ZeroPointEnergy(stack, getMaxEnergyExtract()) {
            @Override public void onEnergyChanged(long amount, boolean simulate) {
                if (simulate) return;
                updateZPMItem(stack);
                setChanged();
                syncInventory();
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TripleZpmHubBlockEntity hub) {
        hub.outputEnergy(Direction.DOWN);
    }

    @Override public void setProtected(boolean value) {
        protectedHub = value;
        setChanged();
        syncInventory();
    }
    @Override public boolean isProtected() { return protectedHub; }
    @Override public boolean hasPermissions(Player player, boolean sendMessage) {
        if (protectedHub && !player.hasPermissions(CommonPermissionConfig.protected_zpm_hub_permissions.get())) {
            if (sendMessage) player.displayClientMessage(Component.translatable("block.sgjourney.protected_permissions")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return false;
        }
        return true;
    }
}


