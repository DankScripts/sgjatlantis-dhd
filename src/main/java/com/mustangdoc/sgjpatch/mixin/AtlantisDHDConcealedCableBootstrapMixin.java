package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.AtlantisDHDConcealedCableBootstrapEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity", remap = false)
public abstract class AtlantisDHDConcealedCableBootstrapMixin {
    @Inject(method = "onLoad", at = @At("TAIL"), remap = false)
    private void sgjpatch$registerConcealedCableBootstrap(CallbackInfo callback) {
        AtlantisDHDConcealedCableBootstrapEvents.register((BlockEntity) (Object) this);
    }

    @Inject(method = "invalidateCaps", at = @At("TAIL"), remap = false)
    private void sgjpatch$unregisterConcealedCableBootstrap(CallbackInfo callback) {
        AtlantisDHDConcealedCableBootstrapEvents.unregister((BlockEntity) (Object) this);
    }
}
