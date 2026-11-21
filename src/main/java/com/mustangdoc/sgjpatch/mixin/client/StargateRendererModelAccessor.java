package com.mustangdoc.sgjpatch.mixin.client;

import net.povstalec.sgjourney.client.models.block_entity.AbstractStargateModel;
import net.povstalec.sgjourney.client.render.block_entity.stargate.AbstractStargateRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractStargateRenderer.class, remap = false)
public interface StargateRendererModelAccessor {
    @Accessor("stargateModel")
    AbstractStargateModel<?, ?> sgjpatch$getStargateModel();
}
