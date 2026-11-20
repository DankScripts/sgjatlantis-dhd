package com.mustangdoc.sgjpatch.mixin;

import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
   value = {AbstractDHDEntity.class},
   remap = false
)
public abstract class AbstractDHDEntityCenterFeedbackClearMixin {
}

