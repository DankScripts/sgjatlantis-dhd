package com.mustangdoc.sgjpatch.mixin;

import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractStargateEntity.class, remap = false)
public interface CityStargateCacheAccessor {
    @Accessor("stargateCacheRequiresUpdate")
    void sgjpatch$markStargateCacheDirty(boolean dirty);
}
