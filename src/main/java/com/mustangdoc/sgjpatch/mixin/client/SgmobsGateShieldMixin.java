package com.mustangdoc.sgjpatch.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.client.render.PegasusShieldMeshRenderer;
import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draw the SG Mobs energy iris after its wormhole, preserving both layers. */
@Pseudo
@Mixin(targets = "com.mike.wayfarer.client.GateRenderer", remap = false)
public abstract class SgmobsGateShieldMixin {
    private static Method sgjpatch$humSync;
    private static boolean sgjpatch$humChecked;

    @Redirect(method = "render(Lcom/mike/wayfarer/GateBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/mike/wayfarer/client/AtlantisShieldRenderer;render(Lcom/mike/wayfarer/GateBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            remap = false)
    private void sgjpatch$deferShield(@Coerce Object gate, PoseStack poses, MultiBufferSource buffers, int light) {
        // The shield mesh is drawn after the event horizon below.
    }

    private static void sgjpatch$syncHum(Object gate) {
        if (!sgjpatch$humChecked) {
            sgjpatch$humChecked = true;
            try {
                Class<?> hum = Class.forName("com.mike.wayfarer.client.AtlantisShieldHum");
                sgjpatch$humSync = hum.getMethod("sync", Class.forName("com.mike.wayfarer.GateBlockEntity"));
            } catch (ReflectiveOperationException ignored) { }
        }
        if (sgjpatch$humSync != null) {
            try { sgjpatch$humSync.invoke(null, gate); }
            catch (ReflectiveOperationException ignored) { }
        }
    }

    @Inject(method = "render(Lcom/mike/wayfarer/GateBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("RETURN"), remap = false)
    private void sgjpatch$drawShieldOverPool(@Coerce Object candidate, float partialTick,
            PoseStack poses, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (!(candidate instanceof BlockEntity gate) || !(candidate instanceof IrisInfo.Interface irisGate)
                || !PegasusShieldPowerHelper.hasShield(irisGate.irisInfo())) return;
        // Sync on every gate frame, including the closing animation. The native
        // renderer only reaches its sound call after the iris is fully closed.
        sgjpatch$syncHum(candidate);
        if (!gate.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = gate.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        PegasusShieldMeshRenderer.renderSgmobs(irisGate, poses, buffers, light, facing);
    }
}
