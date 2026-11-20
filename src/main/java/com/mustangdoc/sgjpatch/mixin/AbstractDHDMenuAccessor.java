package com.mustangdoc.sgjpatch.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.menu.SGJourneyMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {SGJourneyMenu.class},
   remap = false
)
public interface AbstractDHDMenuAccessor {
   @Accessor("blockEntity")
   BlockEntity sgjpatch$getBlockEntity();
}

