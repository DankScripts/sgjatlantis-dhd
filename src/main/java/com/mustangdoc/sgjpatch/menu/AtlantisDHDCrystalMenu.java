package com.mustangdoc.sgjpatch.menu;

import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDEntityAccessor;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import java.lang.reflect.Method;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class AtlantisDHDCrystalMenu extends AbstractContainerMenu {
   private static final int CRYSTAL_SLOT_COUNT = 9;
   private static final int ENERGY_SLOT_COUNT = 2;
   private static final int DHD_SLOT_COUNT = 11;
   private final AtlantisDHDEntity blockEntity;
   private final Level level;
   private long clientEnergy;
   private long clientMaxEnergy;
   private long clientEnergyTarget;
   private long clientStargateEnergy;
   private long clientMaxEnergyDeplete;
   private int clientMaxDistance;
   private int clientStargateOpenTime;
   private int clientStargateTimeSinceLastTraveler;
   private boolean clientAdvancedProtocols;

   public static AtlantisDHDCrystalMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
      if (data == null) {
         return new AtlantisDHDCrystalMenu(containerId, playerInventory, null);
      } else {
         BlockEntity be = null;

         try {
            be = playerInventory.player.level().getBlockEntity(data.readBlockPos());
         } catch (Throwable var5) {
         }

         return new AtlantisDHDCrystalMenu(containerId, playerInventory, be);
      }
   }

   public AtlantisDHDCrystalMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
      super(SGJPatchMenus.ATLANTIS_DHD_CRYSTAL.get(), containerId);
      this.level = playerInventory.player.level();
      this.blockEntity = blockEntity instanceof AtlantisDHDEntity atlantis ? atlantis : null;
      this.sgjpatch$registerSyncedFields();
      if (this.blockEntity != null) {
         this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(this::bindCrystalSlots);
         if (this.blockEntity instanceof AbstractDHDEntityAccessor accessor) {
            this.bindEnergySlots(accessor.sgjpatch$getEnergyItemHandler());
         }
      }

      this.addPlayerInventory(playerInventory);
      this.addPlayerHotbar(playerInventory);
   }

   private void sgjpatch$registerSyncedFields() {
      this.sgjpatch$addLongSync(() -> this.sgjpatch$isClient() ? this.clientEnergy : this.sgjpatch$getEnergyStored(), v -> this.clientEnergy = v);
      this.sgjpatch$addLongSync(() -> this.sgjpatch$isClient() ? this.clientMaxEnergy : this.sgjpatch$getEnergyCapacity(), v -> this.clientMaxEnergy = v);
      this.sgjpatch$addLongSync(
         () -> this.sgjpatch$isClient() ? this.clientEnergyTarget : (this.blockEntity == null ? 0L : this.blockEntity.getEnergyTarget()),
         v -> this.clientEnergyTarget = v
      );
      this.sgjpatch$addLongSync(
         () -> this.sgjpatch$isClient() ? this.clientStargateEnergy : (this.blockEntity == null ? 0L : this.blockEntity.getStargateEnergy()),
         v -> this.clientStargateEnergy = v
      );
      this.sgjpatch$addLongSync(
         () -> this.sgjpatch$isClient() ? this.clientMaxEnergyDeplete : this.sgjpatch$getMaxEnergyDeplete(), v -> this.clientMaxEnergyDeplete = v
      );
      this.sgjpatch$addIntSync(() -> this.sgjpatch$isClient() ? this.clientMaxDistance : this.sgjpatch$getMaxDistanceSafe(), v -> this.clientMaxDistance = v);
      this.sgjpatch$addIntSync(
         () -> this.sgjpatch$isClient() ? this.clientStargateOpenTime : (this.blockEntity == null ? 0 : this.blockEntity.getStargateOpenTime()),
         v -> this.clientStargateOpenTime = v
      );
      this.sgjpatch$addIntSync(
         () -> this.sgjpatch$isClient()
               ? this.clientStargateTimeSinceLastTraveler
               : (this.blockEntity == null ? 0 : this.blockEntity.getStargateTimeSinceLastTraveler()),
         v -> this.clientStargateTimeSinceLastTraveler = v
      );
      this.sgjpatch$addIntSync(() -> {
         boolean value = this.sgjpatch$isClient() ? this.clientAdvancedProtocols : this.blockEntity != null && this.blockEntity.enableAdvancedProtocols();
         return value ? 1 : 0;
      }, v -> this.clientAdvancedProtocols = v != 0);
   }

   private boolean sgjpatch$isClient() {
      return this.level != null && this.level.isClientSide;
   }

   private long sgjpatch$getEnergyStored() {
      return PegasusShieldPowerHelper.getStoredEnergy(this.blockEntity);
   }

   private long sgjpatch$getEnergyCapacity() {
      return PegasusShieldPowerHelper.getEnergyCapacity(this.blockEntity);
   }

   private long sgjpatch$getMaxEnergyDeplete() {
      return sgjpatch$invokeLong(this.blockEntity, "maxEnergyDeplete", "getMaxDeplete", "getMaxExtract");
   }

   private int sgjpatch$getMaxDistanceSafe() {
      if (this.blockEntity == null) {
         return 0;
      } else {
         try {
            if (this.blockEntity.getClass().getMethod("getMaxDistance").invoke(this.blockEntity) instanceof Number number) {
               long asLong = number.longValue();
               if (asLong <= 0L) {
                  return 0;
               }

               return (int)Math.min(2147483647L, asLong);
            }
         } catch (Throwable var5) {
         }

         long fallback = sgjpatch$invokeLong(this.blockEntity, "getCommunicationRange", "communicationRange", "getCommRange");
         return fallback <= 0L ? 0 : (int)Math.min(2147483647L, fallback);
      }
   }

   private static long sgjpatch$invokeLong(Object target, String... methodNames) {
      if (target == null) {
         return 0L;
      } else {
         for (String methodName : methodNames) {
            try {
               Method method = target.getClass().getMethod(methodName);
               if (method.invoke(target) instanceof Number number) {
                  return number.longValue();
               }
            } catch (Throwable var9) {
            }
         }

         return 0L;
      }
   }

   private void sgjpatch$addIntSync(final IntSupplier getter, final IntConsumer setter) {
      this.addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return getter.getAsInt();
         }

         @Override
         public void set(int value) {
            setter.accept(value);
         }
      });
   }

   private void sgjpatch$addLongSync(LongSupplier getter, LongConsumer setter) {
      int[] parts = new int[2];
      this.sgjpatch$addIntSync(() -> (int)(getter.getAsLong() & 4294967295L), v -> {
         parts[0] = v;
         setter.accept((long)parts[1] << 32 | (long)parts[0] & 4294967295L);
      });
      this.sgjpatch$addIntSync(() -> (int)(getter.getAsLong() >>> 32 & 4294967295L), v -> {
         parts[1] = v;
         setter.accept((long)parts[1] << 32 | (long)parts[0] & 4294967295L);
      });
   }

   private void bindCrystalSlots(IItemHandler itemHandler) {
      if (itemHandler != null) {
         this.addSlot(new SlotItemHandler(itemHandler, 0, 80, 35));
         this.addSlot(new SlotItemHandler(itemHandler, 1, 80, 17));
         this.addSlot(new SlotItemHandler(itemHandler, 2, 98, 17));
         this.addSlot(new SlotItemHandler(itemHandler, 3, 98, 35));
         this.addSlot(new SlotItemHandler(itemHandler, 4, 98, 53));
         this.addSlot(new SlotItemHandler(itemHandler, 5, 80, 53));
         this.addSlot(new SlotItemHandler(itemHandler, 6, 62, 53));
         this.addSlot(new SlotItemHandler(itemHandler, 7, 62, 35));
         this.addSlot(new SlotItemHandler(itemHandler, 8, 62, 17));
      }
   }

   private void bindEnergySlots(IItemHandler energyItemHandler) {
      if (energyItemHandler != null) {
         this.addSlot(new SlotItemHandler(energyItemHandler, 0, 134, 27));
         this.addSlot(new SlotItemHandler(energyItemHandler, 1, 134, 53));
      }
   }

   private void addPlayerInventory(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            int slotIndex = col + row * 9 + 9;
            int x = 8 + col * 18;
            int y = 84 + row * 18;
            this.addSlot(new Slot(playerInventory, slotIndex, x, y));
         }
      }
   }

   private void addPlayerHotbar(Inventory playerInventory) {
      for (int col = 0; col < 9; col++) {
         int x = 8 + col * 18;
         int y = 142;
         this.addSlot(new Slot(playerInventory, col, x, y));
      }
   }

   @Override
   public boolean stillValid(Player player) {
      return this.blockEntity != null
         && !this.blockEntity.isRemoved()
         && player.distanceToSqr(
               (double)this.blockEntity.getBlockPos().getX() + 0.5,
               (double)this.blockEntity.getBlockPos().getY() + 0.5,
               (double)this.blockEntity.getBlockPos().getZ() + 0.5
            )
            <= 64.0;
   }

   public boolean hasItem(int slotIndex) {
      if (slotIndex >= 0 && slotIndex < this.slots.size()) {
         Slot slot = this.slots.get(slotIndex);
         return slot != null && slot.hasItem();
      } else {
         return false;
      }
   }

   public long getEnergy() {
      return this.sgjpatch$isClient() ? this.clientEnergy : this.sgjpatch$getEnergyStored();
   }

   public long getMaxEnergy() {
      return this.sgjpatch$isClient() ? this.clientMaxEnergy : this.sgjpatch$getEnergyCapacity();
   }

   public long getEnergyTarget() {
      return this.sgjpatch$isClient() ? this.clientEnergyTarget : (this.blockEntity == null ? 0L : this.blockEntity.getEnergyTarget());
   }

   public boolean enableAdvancedProtocols() {
      return this.sgjpatch$isClient() ? this.clientAdvancedProtocols : this.blockEntity != null && this.blockEntity.enableAdvancedProtocols();
   }

   public long maxEnergyDeplete() {
      return this.sgjpatch$isClient() ? this.clientMaxEnergyDeplete : this.sgjpatch$getMaxEnergyDeplete();
   }

   public int getMaxDistance() {
      return this.sgjpatch$isClient() ? this.clientMaxDistance : (this.blockEntity == null ? 0 : this.blockEntity.getMaxDistance());
   }

   public long getStargateEnergy() {
      return this.sgjpatch$isClient() ? this.clientStargateEnergy : (this.blockEntity == null ? 0L : this.blockEntity.getStargateEnergy());
   }

   public int getStargateOpenTime() {
      return this.sgjpatch$isClient() ? this.clientStargateOpenTime : (this.blockEntity == null ? 0 : this.blockEntity.getStargateOpenTime());
   }

   public int getStargateTimeSinceLastTraveler() {
      return this.sgjpatch$isClient()
         ? this.clientStargateTimeSinceLastTraveler
         : (this.blockEntity == null ? 0 : this.blockEntity.getStargateTimeSinceLastTraveler());
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      if (this.blockEntity == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack itemstack = ItemStack.EMPTY;
         Slot slot = this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            int teSlots = 11;
            if (index < teSlots) {
               if (!this.moveItemStackTo(stackInSlot, teSlots, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stackInSlot, 0, teSlots, false)) {
               return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
               slot.set(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            return itemstack;
         } else {
            return ItemStack.EMPTY;
         }
      }
   }
}
