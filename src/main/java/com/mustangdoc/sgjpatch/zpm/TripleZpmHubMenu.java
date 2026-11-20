package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubMenu extends AbstractContainerMenu {
   public final TripleZpmHubBlockEntity hub;

   public TripleZpmHubMenu(int id, Inventory player, FriendlyByteBuf data) {
      this(id, player, (TripleZpmHubBlockEntity)player.player.level().getBlockEntity(data.readBlockPos()));
   }

   public TripleZpmHubMenu(int id, Inventory player, TripleZpmHubBlockEntity hub) {
      super(TripleZpmHubRegistration.MENU, id);
      this.hub = hub;
      this.addSlot(new SlotItemHandler(hub.inventory, 0, 53, 45));
      this.addSlot(new SlotItemHandler(hub.inventory, 1, 107, 45));
      this.addSlot(new SlotItemHandler(hub.inventory, 2, 80, 20));

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(player, col + row * 9 + 9, 8 + 18 * col, 84 + 18 * row));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(player, col, 8 + 18 * col, 142));
      }
   }

   public boolean stillValid(Player player) {
      return player.level().getBlockEntity(this.hub.getBlockPos()) == this.hub
         && player.distanceToSqr(this.hub.getBlockPos().getCenter()) <= 64.0
         && this.hub.hasPermissions(player, false);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = slot.getItem();
         ItemStack original = stack.copy();
         boolean moved = index < 3 ? this.moveItemStackTo(stack, 3, 39, true) : this.moveItemStackTo(stack, 0, 3, false);
         if (!moved) {
            return ItemStack.EMPTY;
         } else {
            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            slot.onTake(player, stack);
            return original;
         }
      }
   }
}
