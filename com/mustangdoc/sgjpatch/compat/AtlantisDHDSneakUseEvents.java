package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Routes sneak-use before vanilla suppresses block use for held items. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch")
public final class AtlantisDHDSneakUseEvents {
    private AtlantisDHDSneakUseEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity entity = findAtlantisDhd(level, pos);
        if (!(entity instanceof AtlantisDHDEntity)) {
            return;
        }

        NetworkHooks.openScreen(serverPlayer, new CrystalProvider(entity), pos);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    private static BlockEntity findAtlantisDhd(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof AtlantisDHDEntity) return entity;
        entity = level.getBlockEntity(pos.below());
        if (entity instanceof AtlantisDHDEntity) return entity;
        entity = level.getBlockEntity(pos.above());
        return entity instanceof AtlantisDHDEntity ? entity : null;
    }

    private record CrystalProvider(BlockEntity entity) implements MenuProvider {
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new AtlantisDHDCrystalMenu(containerId, inventory, entity);
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Crystal Controls");
        }
    }
}
