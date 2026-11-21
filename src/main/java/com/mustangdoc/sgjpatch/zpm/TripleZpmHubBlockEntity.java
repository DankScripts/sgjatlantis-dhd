package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.povstalec.sgjourney.common.block_entities.ProtectedBlockEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.capabilities.ZeroPointEnergy;
import net.povstalec.sgjourney.common.config.CommonPermissionConfig;
import net.povstalec.sgjourney.common.config.CommonZPMConfig;
import net.povstalec.sgjourney.common.init.ItemInit;

/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubBlockEntity extends EnergyBlockEntity implements ProtectedBlockEntity {
   public static final int SLOT_COUNT = 3;
   private boolean protectedHub;
   private int firstSlot;
   public final ItemStackHandler inventory = new ItemStackHandler(3) {
      public boolean isItemValid(int slot, ItemStack stack) {
         return stack.is((Item)ItemInit.ZPM.get());
      }

      public int getSlotLimit(int slot) {
         return 1;
      }

      protected void onContentsChanged(int slot) {
         TripleZpmHubBlockEntity.this.setChanged();
         TripleZpmHubBlockEntity.this.syncInventory();
      }
   };
   private final IItemHandler protectedPowerInventory = new IItemHandler() {
      @Override public int getSlots() { return SLOT_COUNT; }
      @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
      @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
      @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
      @Override public int getSlotLimit(int slot) { return 1; }
      @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
   };

   public TripleZpmHubBlockEntity(BlockPos pos, BlockState state) {
      super(TripleZpmHubRegistration.BLOCK_ENTITY, pos, state);
   }

   private void syncInventory() {
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
   }

   public void loadAdditional(CompoundTag tag, Provider lookup) {
      super.loadAdditional(tag, lookup);
      CompoundTag items = tag.getCompound("inventory").copy();
      items.putInt("Size", 3);
      this.inventory.deserializeNBT(lookup, items);
      this.protectedHub = tag.getBoolean("protected");
   }

   protected void saveAdditional(CompoundTag tag, Provider lookup) {
      super.saveAdditional(tag, lookup);
      tag.put("inventory", this.inventory.serializeNBT(lookup));
      tag.putBoolean("protected", this.protectedHub);
   }

   public IItemHandler getItemHandler(Direction side) {
      return this.protectedHub && !CommonPermissionConfig.protected_inventory_access.get()
              ? this.protectedPowerInventory : this.inventory;
   }

   public void drops() {
      if (this.level != null && !this.level.isClientSide()) {
         for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            Containers.dropItemStack(
               this.level, (double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5, stack
            );
         }
      }
   }

   public boolean isCorrectEnergySide(Direction side) {
      return side == Direction.DOWN;
   }

   public long getEnergyCapacity() {
      return (Long)CommonZPMConfig.zpm_energy_per_entropy_level.get();
   }

   public long getMaxEnergyReceive() {
      return 0L;
   }

   public long getMaxEnergyExtract() {
      return (Long)CommonZPMConfig.zpm_hub_max_transfer.get();
   }

   public void outputEnergy(Direction direction) {
      if (this.level != null && !this.level.isClientSide()) {
         BlockPos target = this.worldPosition.relative(direction);
         IEnergyStorage receiver = (IEnergyStorage)this.level.getCapability(EnergyStorage.BLOCK, target, direction.getOpposite());
         if (receiver != null) {
            boolean changed = false;

            for (int step = 0; step < 3; step++) {
               ZeroPointEnergy zpm = moduleEnergy((this.firstSlot + step) % 3);
               if (zpm != null) {
                     long sent = 0L;
                     if (receiver instanceof SGJourneyEnergy) {
                        SGJourneyEnergy sgEnergy = (SGJourneyEnergy)receiver;
                        long available = zpm.extractLongEnergy(this.getMaxEnergyDeplete(), true);
                        long accepted = sgEnergy.receiveZeroPointEnergy(available, true);
                        if (accepted > 0L) {
                           sent = zpm.extractLongEnergy(accepted, false);
                           sgEnergy.receiveZeroPointEnergy(sent, false);
                        }
                     } else if ((Boolean)CommonZPMConfig.other_mods_use_zero_point_energy.get()) {
                        int available = zpm.extractEnergy(SGJourneyEnergy.regularEnergy(this.getMaxEnergyDeplete()), true);
                        int accepted = receiver.receiveEnergy(available, true);
                        if (accepted > 0) {
                           sent = zpm.extractLongEnergy((long)accepted, false);
                           receiver.receiveEnergy((int)sent, false);
                        }
                     }

                     changed |= sent > 0L;
               }
            }

            this.firstSlot = (this.firstSlot + 1) % 3;
            if (changed) {
               this.setChanged();
            }

            if (this.level.getGameTime() % 20L == 0L) {
               this.syncInventory();
            }
         }
      }
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

   public void setProtected(boolean value) {
      this.protectedHub = value;
      this.setChanged();
      this.syncInventory();
   }

   public boolean isProtected() {
      return this.protectedHub;
   }

   public boolean hasPermissions(Player player, boolean sendMessage) {
      if (this.protectedHub && !player.hasPermissions(CommonPermissionConfig.protected_zpm_hub_permissions.get())) {
         if (sendMessage) {
            player.displayClientMessage(Component.translatable("block.sgjourney.protected_permissions").withStyle(ChatFormatting.DARK_RED), true);
         }

         return false;
      } else {
         return true;
      }
   }
}


