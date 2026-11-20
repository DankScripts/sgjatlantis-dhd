package com.mustangdoc.sgjpatch.mixin;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {EnergyBlockEntity.class},
   remap = false
)
public interface EnergyBlockEntityAccessor {
   @Accessor("lazyEnergyHandler")
   LazyOptional<IEnergyStorage> sgjpatch$getLazyEnergyHandler();
}

