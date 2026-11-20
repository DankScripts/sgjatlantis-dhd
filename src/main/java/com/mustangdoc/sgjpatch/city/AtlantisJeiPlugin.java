package com.mustangdoc.sgjpatch.city;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

/** Loaded by JEI only when JEI is present; never makes it a required mod. */
@JeiPlugin
public final class AtlantisJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation(AtlantisBuildingBlocks.NS, "unreleased_items"); }
}
