package com.mustangdoc.sgjpatch.mixin.client;

import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed$AtlantisTriangleSymbolButton", remap = false)
public abstract class AtlantisDHDConnectedSymbolLockMixin {
    @Shadow
    @Final
    private AbstractDHDMenu<?> menu;

    @Shadow
    @Final
    private int actionSymbol;

    @Inject(method = "sgjpatch$pressNormal", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjpatch$blockAdditionalSymbolsWhileConnected(CallbackInfo callbackInfo) {
        if (this.actionSymbol >= 0 && this.actionSymbol <= 38
                && this.menu != null && this.menu.isCenterButtonEngaged()) {
            callbackInfo.cancel();
        }
    }
}