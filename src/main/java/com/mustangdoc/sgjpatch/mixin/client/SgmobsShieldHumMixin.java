package com.mustangdoc.sgjpatch.mixin.client;

import com.mustangdoc.sgjpatch.client.sound.IrisSoundState;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keep SG Mobs' sound and its lifetime check in step with iris motion. */
@Pseudo
@Mixin(targets = "com.mike.wayfarer.client.AtlantisShieldHum", remap = false)
public abstract class SgmobsShieldHumMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(floatValue = 0.8F), remap = false)
    private float sgjpatch$matchShieldHumVolume(float original) {
        return 4.0F;
    }

    @Redirect(method = "sync", at = @At(value = "INVOKE", remap = false,
            target = "Lcom/mike/wayfarer/GateIris;isIrisClosed()Z"), remap = false)
    private static boolean sgjpatch$startOnClosing(@Coerce Object iris) {
        return IrisSoundState.active((IrisInfo) iris);
    }

    @Redirect(method = "valid", at = @At(value = "INVOKE", remap = false,
            target = "Lcom/mike/wayfarer/GateIris;isIrisClosed()Z"), remap = false)
    private boolean sgjpatch$continueWhileClosing(@Coerce Object iris) {
        return IrisSoundState.active((IrisInfo) iris);
    }
}
