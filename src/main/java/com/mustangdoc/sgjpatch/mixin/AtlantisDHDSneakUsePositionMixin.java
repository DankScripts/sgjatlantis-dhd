package com.mustangdoc.sgjpatch.mixin;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the model's proxy sneak-click send the DHD's real anchor position.
 * Sending the clicked support-block position creates different server/client
 * menu sizes (45 versus 36 slots) and breaks inventory synchronization.
 */
@Mixin(targets = "com.mustangdoc.sgjpatch.compat.AtlantisDHDSneakUseEvents", remap = false)
public abstract class AtlantisDHDSneakUsePositionMixin {
    @Redirect(
            method = "onRightClickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/network/NetworkHooks;openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Lnet/minecraft/core/BlockPos;)V",
                    remap = false),
            remap = false)
    private static void sgjpatch$openAtRealDhdPosition(
            ServerPlayer player, MenuProvider provider, BlockPos clickedPos) {
        BlockPos menuPos = clickedPos;
        try {
            Field entityField = provider.getClass().getDeclaredField("entity");
            entityField.setAccessible(true);
            Object entity = entityField.get(provider);
            if (entity instanceof BlockEntity) {
                BlockPos entityPos = ((BlockEntity) entity).getBlockPos();
                if (entityPos != null) menuPos = entityPos;
            }
        } catch (ReflectiveOperationException | SecurityException exception) {
            System.out.println("[SGJPATCH] Could not resolve proxy DHD position; "
                    + "using clicked position: " + exception.getClass().getSimpleName());
        }
        NetworkHooks.openScreen(player, provider, menuPos);
    }
}
