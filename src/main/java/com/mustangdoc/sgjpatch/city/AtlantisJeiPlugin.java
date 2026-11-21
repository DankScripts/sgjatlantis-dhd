package com.mustangdoc.sgjpatch.city;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

/** Loaded by JEI only when JEI is present; never makes it a required mod. */
@JeiPlugin
public final class AtlantisJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(AtlantisBuildingBlocks.NS, "unreleased_items"); }

    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        if (!net.neoforged.fml.ModList.get().isLoaded("sgjourney_mobs")) return;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(AtlantisBuildingBlocks.NS, "triple_zpm_hub"));
        if (item != net.minecraft.world.item.Items.AIR)
            runtime.getIngredientManager().removeIngredientsAtRuntime(
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                    java.util.List.of(new net.minecraft.world.item.ItemStack(item)));
    }
}
