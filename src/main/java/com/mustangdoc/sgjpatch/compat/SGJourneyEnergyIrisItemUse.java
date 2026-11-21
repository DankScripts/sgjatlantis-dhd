package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

/** Allows the same direct iris installation and removal on SGJourney gate controllers. */
@EventBusSubscriber(modid = "sgjatlantis_dhd")
public final class SGJourneyEnergyIrisItemUse {
    private SGJourneyEnergyIrisItemUse() { }

    @SubscribeEvent public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        BlockEntity candidate = event.getLevel().getBlockEntity(event.getPos());
        if (!(candidate instanceof IrisInfo.Interface gate)) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get())) return;
        IrisInfo iris = gate.irisInfo();
        boolean removing = player.isShiftKeyDown() && iris.hasIris()
                && iris.getIris().is(SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get());
        if (!removing && iris.hasIris()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) return;
        if (removing) {
            ItemStack returned = iris.getIris();
            iris.removeIris();
            if (!player.getInventory().add(returned)) player.drop(returned, false);
            player.displayClientMessage(Component.literal("Atlantis energy iris removed."), true);
        } else if (iris.addIris(held.copyWithCount(1))) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.displayClientMessage(Component.literal("Atlantis energy iris installed."), true);
        }
    }
}
