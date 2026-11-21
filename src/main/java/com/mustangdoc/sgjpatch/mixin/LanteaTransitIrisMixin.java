package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import io.github.aqueoxx.lantea.server.LanteaServerServices;
import io.github.aqueoxx.lantea.server.gate.GateController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A closed destination iris destroys travelers before Lantea transfers them. */
@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.server.transit.TransitService", remap = false)
public abstract class LanteaTransitIrisMixin {
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void sgjpatch$blockClosedIris(@Coerce Object candidate, long now, CallbackInfo ci) {
        if (!(candidate instanceof LanteaTransitCrossingAccessor crossing)) return;
        Entity traveler = crossing.sgjpatch$entity();
        GateController source = crossing.sgjpatch$gate();
        if (traveler == null || source == null || traveler.getServer() == null) return;
        LanteaServerServices.get(traveler.getServer()).flatMap(services ->
                services.connections().involving(source.gateId()).flatMap(connection ->
                        services.controller(connection.otherEnd(source.gateId()))))
                .ifPresent(destination -> {
                    if (destination.host() instanceof LanteaEnergyIrisState iris && iris.sgjpatch$irisClosed()) {
                        traveler.kill();
                        ci.cancel();
                    }
                });
    }
}
