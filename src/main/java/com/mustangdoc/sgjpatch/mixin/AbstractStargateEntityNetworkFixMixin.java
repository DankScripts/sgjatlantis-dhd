package com.mustangdoc.sgjpatch.mixin;

import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
   value = {AbstractStargateEntity.class},
   remap = false
)
public abstract class AbstractStargateEntityNetworkFixMixin {
}

