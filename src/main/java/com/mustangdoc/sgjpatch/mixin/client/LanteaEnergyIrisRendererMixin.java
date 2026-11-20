package com.mustangdoc.sgjpatch.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.client.render.PegasusShieldMeshRenderer;
import com.mustangdoc.sgjpatch.client.sound.LanteaIrisHum;
import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.client.render.StargateControllerRenderer", remap = false)
public abstract class LanteaEnergyIrisRendererMixin {
    @Inject(method = "render(Lio/github/aqueoxx/lantea/blockentity/StargateControllerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("TAIL"), require = 1, remap = false)
    private void sgjpatch$renderLanteaIris(@Coerce Object candidate, float partialTick, PoseStack poses,
                                           MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (!(candidate instanceof BlockEntity gate) || !(candidate instanceof LanteaEnergyIrisState iris)) return;
        LanteaIrisHum.sync(gate, iris.sgjpatch$irisClosed());
        if (!iris.sgjpatch$irisClosed() || !gate.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = gate.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        PegasusShieldMeshRenderer.renderLantea(poses, buffers, light, facing);
    }
}
