package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.LanteaJumperConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Register the jumper setting during Forge's mod construction phase. */
@Mixin(targets = "com.mustangdoc.sgjpatch.SGJAdditionsCapacityPatch", remap = false)
public abstract class AtlantisJumperConfigBootstrapMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void sgjpatch$registerJumperConfig(CallbackInfo callback) {
        LanteaJumperConfig.register();
        com.mustangdoc.sgjpatch.city.AtlantisLightingConfig.register();
    }
}
