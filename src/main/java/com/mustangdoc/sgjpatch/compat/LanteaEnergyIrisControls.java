package com.mustangdoc.sgjpatch.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LanteaEnergyIrisControls {
    private static final String IRIS_ITEM = "stargate_atlantis:pegasus_upgraded_shield";

    private LanteaEnergyIrisControls() { }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        BlockEntity gate = controllerAt(event.getLevel(), event.getPos());
        if (!(gate instanceof LanteaEnergyIrisState iris)) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        boolean holdingIris = !held.isEmpty() && IRIS_ITEM.equals(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
        if (!holdingIris || (iris.sgjpatch$irisInstalled() && !player.isShiftKeyDown())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) return;

        if (holdingIris && !iris.sgjpatch$irisInstalled()) {
            iris.sgjpatch$setIris(true, false);
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.displayClientMessage(Component.literal("Lantea energy iris installed. Use an Atlantis DHD to control it."), true);
        } else if (holdingIris && player.isShiftKeyDown()) {
            iris.sgjpatch$setIris(false, false);
            if (!player.getAbilities().instabuild) {
                ItemStack returned = new ItemStack(held.getItem());
                if (!player.getInventory().add(returned)) player.drop(returned, false);
            }
            player.displayClientMessage(Component.literal("Lantea energy iris removed."), true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof LanteaEnergyIrisState iris)
                || !iris.sgjpatch$irisInstalled() || event.getPlayer().getAbilities().instabuild) return;
        Block.popResource(level, event.getPos(), new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(IRIS_ITEM))));
    }

    private static BlockEntity controllerAt(LevelAccessor level, BlockPos pos) {
        BlockEntity candidate = level.getBlockEntity(pos);
        if (candidate instanceof LanteaEnergyIrisState) return candidate;
        if (candidate == null || !candidate.getClass().getName()
                .equals("io.github.aqueoxx.lantea.blockentity.StargatePartBlockEntity")) return null;
        try {
            Object controller = candidate.getClass().getMethod("controllerPos").invoke(candidate);
            return controller instanceof BlockPos controllerPos ? level.getBlockEntity(controllerPos) : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
