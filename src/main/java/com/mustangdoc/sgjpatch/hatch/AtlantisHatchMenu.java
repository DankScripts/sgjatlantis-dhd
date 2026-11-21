package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public final class AtlantisHatchMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos tablePos;
    private final ContainerData status;

    public AtlantisHatchMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, inventory.player.level(), buffer.readBlockPos(), new SimpleContainerData(3));
    }

    public AtlantisHatchMenu(int id, Inventory inventory, Level level, BlockPos tablePos) {
        this(id, inventory, level, tablePos, new ContainerData() {
            @Override public int get(int index) {
                if (index < 2) return AtlantisHatchService.status(level, index);
                return level.getBlockEntity(tablePos) instanceof AtlantisHatchTableBlockEntity table
                        ? table.storedEnergy() : 0;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 3; }
        });
    }

    private AtlantisHatchMenu(int id, Inventory inventory, Level level, BlockPos pos, ContainerData data) {
        super(AtlantisHatchRegistry.MENU, id);
        this.level = level;
        this.tablePos = pos;
        this.status = data;
        addDataSlots(data);
    }

    public int doorStatus(int index) { return status.get(index); }
    public int storedEnergy() { return status.get(2); }

    @Override
    public boolean stillValid(Player player) {
        return level.getBlockState(tablePos).is(AtlantisHatchRegistry.TABLE)
                && player.distanceToSqr(tablePos.getX() + .5, tablePos.getY() + .5,
                        tablePos.getZ() + .5) <= 64.0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > 1 || !stillValid(player)) return false;
        if (player instanceof ServerPlayer operator && level instanceof ServerLevel serverLevel) {
            if (!(level.getBlockEntity(tablePos) instanceof AtlantisHatchTableBlockEntity table)) return false;
            if (table.storedEnergy() < AtlantisHatchTableBlockEntity.OPERATION_COST) {
                operator.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Jumper bay door table needs 50 FE per operation."), true);
                return false;
            }
            if (!AtlantisHatchService.toggle(serverLevel, id, operator)) return false;
            return table.consumeOperation();
        }
        return true;
    }

    @Override public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
