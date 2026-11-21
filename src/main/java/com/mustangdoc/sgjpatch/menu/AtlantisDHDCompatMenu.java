package com.mustangdoc.sgjpatch.menu;

import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

public final class AtlantisDHDCompatMenu extends AbstractDHDMenu<AbstractDHDEntity<?>> {
    public AtlantisDHDCompatMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, resolveDhd(inventory, data));
    }

    public static AtlantisDHDCompatMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf data) {
        return new AtlantisDHDCompatMenu(containerId, inventory, data);
    }

    public AtlantisDHDCompatMenu(int containerId, Inventory inventory, AbstractDHDEntity<?> blockEntity) {
        super(SGJPatchMenus.ATLANTIS_DHD_COMPAT.get(), containerId, inventory, blockEntity);
    }

    private static AbstractDHDEntity<?> resolveDhd(Inventory inventory, FriendlyByteBuf data) {
        if (data == null) {
            throw new IllegalStateException("Missing Atlantis DHD menu data");
        }
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof AbstractDHDEntity<?> dhd) {
            return dhd;
        }
        throw new IllegalStateException("Expected an Atlantis DHD block entity");
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved()
                && player.level() == blockEntity.getLevel()
                && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
