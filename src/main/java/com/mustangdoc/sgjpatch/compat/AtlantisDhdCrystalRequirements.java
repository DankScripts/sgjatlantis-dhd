package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.povstalec.sgjourney.common.init.ItemInit;

/** The Pegasus DHD's minimum dialing crystals, independent of its energy source. */
public final class AtlantisDhdCrystalRequirements {
    private AtlantisDhdCrystalRequirements() {}

    public static boolean canDial(AtlantisDHDEntity dhd) {
        IItemHandler crystals = dhd.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElse(null);
        if (crystals == null) return false;

        int controls = 0;
        int energy = 0;
        int transfer = 0;
        for (int slot = 0; slot < crystals.getSlots(); ++slot) {
            var item = crystals.getStackInSlot(slot).getItem();
            if (item == ItemInit.LARGE_CONTROL_CRYSTAL.get()) ++controls;
            else if (item == ItemInit.ADVANCED_ENERGY_CRYSTAL.get()) ++energy;
            else if (item == ItemInit.ADVANCED_TRANSFER_CRYSTAL.get()) ++transfer;
        }
        return controls >= 1 && energy >= 2 && transfer >= 1;
    }

    /** Fill the city template's DHD without changing player-placed tables. */
    public static boolean stockBundledDhd(AtlantisDHDEntity dhd) {
        IItemHandler crystals = dhd.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (crystals == null || crystals.getSlots() <= 7) return false;
        Item control = ItemInit.LARGE_CONTROL_CRYSTAL.get();
        insertIfEmpty(crystals, 1, ItemInit.ADVANCED_ENERGY_CRYSTAL.get());
        insertIfEmpty(crystals, 2, ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get());
        insertIfEmpty(crystals, 3, ItemInit.ADVANCED_ENERGY_CRYSTAL.get());
        insertIfEmpty(crystals, 6, ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get());
        insertIfEmpty(crystals, 7, ItemInit.ADVANCED_TRANSFER_CRYSTAL.get());
        if (!contains(crystals, control)) {
            for (int slot = 0; slot < crystals.getSlots(); slot++) {
                if (crystals.getStackInSlot(slot).isEmpty()) {
                    insertIfEmpty(crystals, slot, control);
                    if (contains(crystals, control)) break;
                }
            }
        }
        if (!contains(crystals, control) || !canDial(dhd)) return false;
        dhd.setChanged();
        return true;
    }

    private static void insertIfEmpty(IItemHandler handler, int slot, Item item) {
        if (handler.getStackInSlot(slot).isEmpty())
            handler.insertItem(slot, new ItemStack(item), false);
    }

    private static boolean contains(IItemHandler handler, Item item) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).getItem() == item) return true;
        }
        return false;
    }
}
