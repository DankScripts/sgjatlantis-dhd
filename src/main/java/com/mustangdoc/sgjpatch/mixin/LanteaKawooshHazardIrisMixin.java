package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import io.github.aqueoxx.lantea.server.gate.GateController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Closed irises also suppress Lantea's block and entity kawoosh hazard. */
@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.server.transit.KawooshHazard", remap = false)
public abstract class LanteaKawooshHazardIrisMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"), require = 1, remap = false)
    private Object sgjpatch$shieldedGates(Supplier<?> supplier) {
        Object value = supplier.get();
        if (!(value instanceof Collection<?> gates)) return value;
        var active = new ArrayList<Object>(gates.size());
        for (Object gate : gates)
            if (!(gate instanceof GateController controller)
                    || !(controller.host() instanceof LanteaEnergyIrisState iris)
                    || !iris.sgjpatch$irisClosed()) active.add(gate);
        return active;
    }
}
