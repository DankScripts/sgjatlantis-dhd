package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.LanteaJumperBridge;
import com.mustangdoc.sgjpatch.compat.LanteaNativeJumperBackend;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No linkage to Lantea classes when the optional mod is absent. */
@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.jumper.JumperDial", remap = false)
public abstract class LanteaJumperDialMixin {
    @Inject(method = "refuse", at = @At("HEAD"), require = 0, remap = false)
    private static void sgjpatch$captureNativeRefusal(ServerPlayer player, String key, Object[] args,
                                                      CallbackInfo callback) {
        LanteaNativeJumperBackend.captureFeedback(player, key, args);
    }
    @Inject(method = "open", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void sgjpatch$openJourneyDialer(ServerPlayer player, @Coerce Object jumper,
                                                 CallbackInfo callback) {
        if (LanteaJumperBridge.open(player, jumper)) callback.cancel();
    }
}
