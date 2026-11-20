package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.AtlantisDhdCrystalRequirements;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block every server dialing entry point when the Atlantis table lacks crystals. */
@Mixin(AbstractDHDEntity.class)
public abstract class AtlantisDhdDialCrystalGuardMixin {
    @Inject(method = "engageStargate",
            at = @At("HEAD"), cancellable = true)
    private void sgjpatch$requireCrystals(CallbackInfoReturnable<StargateInfo.FeedbackMessage> callback) {
        if ((Object) this instanceof AtlantisDHDEntity dhd
                && !AtlantisDhdCrystalRequirements.canDial(dhd))
            callback.setReturnValue(StargateInfo.Feedback.UNKNOWN_ERROR.withInfo());
    }

    @Inject(method = "pressButton", at = @At("HEAD"), cancellable = true)
    private void sgjpatch$requireCrystalsForButton(int symbol, CallbackInfo callback) {
        if (symbol >= 0 && (Object) this instanceof AtlantisDHDEntity dhd
                && !AtlantisDhdCrystalRequirements.canDial(dhd))
            callback.cancel();
    }

    @Inject(method = "encodeSymbol", at = @At("HEAD"), cancellable = true)
    private void sgjpatch$requireCrystalsForSymbol(int symbol,
            CallbackInfoReturnable<StargateInfo.FeedbackMessage> callback) {
        if (symbol >= 0 && (Object) this instanceof AtlantisDHDEntity dhd
                && !AtlantisDhdCrystalRequirements.canDial(dhd))
            callback.setReturnValue(StargateInfo.Feedback.UNKNOWN_ERROR.withInfo());
    }
}
