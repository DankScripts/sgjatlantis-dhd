package com.mustangdoc.sgjpatch.menu;

import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import com.struxnet.sgjadditions.common.block_entities.AtlantisDHDEntity;
import com.struxnet.sgjadditions.common.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

public final class AtlantisDHDCompatMenu extends AbstractDHDMenu<AbstractDHDEntity> {
	public AtlantisDHDCompatMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
		this(MenuInit.Atlantis_DHD.get(), containerId, playerInventory, resolveDhd(playerInventory, data));
	}

	public static AtlantisDHDCompatMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
		return new AtlantisDHDCompatMenu(
				SGJPatchMenus.ATLANTIS_DHD_COMPAT.get(), containerId, playerInventory, resolveDhd(playerInventory, data));
	}

	public AtlantisDHDCompatMenu(int containerId, Inventory playerInventory, AtlantisDHDEntity blockEntity) {
		this(SGJPatchMenus.ATLANTIS_DHD_COMPAT.get(), containerId, playerInventory, blockEntity);
	}

	public AtlantisDHDCompatMenu(int containerId, Inventory playerInventory,
			com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity blockEntity) {
		this(SGJPatchMenus.ATLANTIS_DHD_COMPAT.get(), containerId, playerInventory, blockEntity);
	}

	private AtlantisDHDCompatMenu(MenuType<?> menuType, int containerId, Inventory playerInventory,
			AbstractDHDEntity blockEntity) {
		super(menuType, containerId, playerInventory, blockEntity);
	}

	private static AbstractDHDEntity resolveDhd(Inventory playerInventory, FriendlyByteBuf data) {
		if (data == null) {
			throw new IllegalStateException("Missing menu data for AtlantisDHDCompatMenu");
		}

		BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
		if (blockEntity instanceof AbstractDHDEntity dhd) {
			return dhd;
		}
		throw new IllegalStateException("Expected AbstractDHDEntity for AtlantisDHDCompatMenu");
	}

	@Override
	public boolean stillValid(Player player) {
		return this.blockEntity != null;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}