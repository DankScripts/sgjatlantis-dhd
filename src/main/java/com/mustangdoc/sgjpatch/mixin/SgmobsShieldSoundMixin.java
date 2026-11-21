package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.client.sound.IrisSoundState;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Start the transition sound with motion, not at the end of the animation. */
@Pseudo
@Mixin(targets = "com.mike.wayfarer.AtlantisShieldCompat", remap = false)
public abstract class SgmobsShieldSoundMixin {
    // The first check controls power consumption; only the second tracks sound.
    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/mike/wayfarer/GateIris;isIrisClosed()Z", remap = false), remap = false)
    private static boolean sgjpatch$soundOnMotion(@Coerce Object iris) {
        return IrisSoundState.active((IrisInfo) iris);
    }
}
