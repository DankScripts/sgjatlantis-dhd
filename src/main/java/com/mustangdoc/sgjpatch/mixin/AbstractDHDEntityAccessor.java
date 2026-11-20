package com.mustangdoc.sgjpatch.mixin;

import net.minecraft.core.Vec3i;
import net.minecraftforge.items.ItemStackHandler;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractDHDEntity.class, remap = false)
public interface AbstractDHDEntityAccessor {
    @Accessor("advancedProtocolsActive")
    void sgjpatch$setAdvancedProtocolsActive(boolean active);

    @Accessor("doCallForwarding")
    void sgjpatch$setDoCallForwarding(boolean active);

    @Accessor("hasNetworkRestrictions")
    void sgjpatch$setHasNetworkRestrictions(boolean active);

    @Accessor("energyTarget")
    void sgjpatch$setEnergyTarget(long energyTarget);

    @Accessor("maxEnergyTransfer")
    long sgjpatch$getMaxEnergyTransfer();

    @Accessor("maxEnergyTransfer")
    void sgjpatch$setMaxEnergyTransfer(long maxEnergyTransfer);

    @Accessor("maxConnectionDistance")
    void sgjpatch$setMaxConnectionDistance(int maxConnectionDistance);

    @Accessor("energyItemHandler")
    ItemStackHandler sgjpatch$getEnergyItemHandler();

    @Accessor("stargateRelativePos")
    Vec3i sgjpatch$getStargateRelativePos();
}
