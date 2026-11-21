package com.mustangdoc.sgjpatch.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A closed iris contains Lantea's opening plume at the gate plane. */
@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.client.render.KawooshRenderer", remap = false)
public abstract class LanteaKawooshIrisMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void sgjpatch$hideKawoosh(@Coerce Object gate, float tick, PoseStack pose,
                                             MultiBufferSource buffers, CallbackInfo ci) {
        if (gate instanceof LanteaEnergyIrisState iris && iris.sgjpatch$irisClosed()) ci.cancel();
    }
}
