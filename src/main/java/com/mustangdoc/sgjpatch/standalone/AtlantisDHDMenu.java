package com.mustangdoc.sgjpatch.standalone;

import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

public class AtlantisDHDMenu extends AbstractDHDMenu<AtlantisDHDEntity> {
    public AtlantisDHDMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, getDhd(inventory, extraData));
    }

    public AtlantisDHDMenu(int containerId, Inventory inventory, AtlantisDHDEntity dhd) {
        super(SGJPatchMenus.ATLANTIS_DHD_COMPAT.get(), containerId, inventory, dhd);
    }

    private static AtlantisDHDEntity getDhd(Inventory inventory, FriendlyByteBuf extraData) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof AtlantisDHDEntity dhd) {
            return dhd;
        }
        throw new IllegalStateException("Expected standalone AtlantisDHDEntity");
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
                player,
                SGJPatchBlocks.ATLANTIS_DHD.get());
    }
}