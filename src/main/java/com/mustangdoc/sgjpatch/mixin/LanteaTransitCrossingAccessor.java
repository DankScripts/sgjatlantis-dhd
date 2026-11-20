package com.mustangdoc.sgjpatch.mixin;

import io.github.aqueoxx.lantea.server.gate.GateController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.server.transit.TransitService$Crossing", remap = false)
public interface LanteaTransitCrossingAccessor {
    @Accessor("gate") GateController sgjpatch$gate();
    @Accessor("entity") Entity sgjpatch$entity();
}
