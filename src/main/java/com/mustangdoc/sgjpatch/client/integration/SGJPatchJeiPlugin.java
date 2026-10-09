package com.mustangdoc.sgjpatch.client.integration;

import com.mustangdoc.sgjpatch.SGJAdditionsCapacityPatch;
import com.mustangdoc.sgjpatch.config.SGJCompatibilityConfig;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@JeiPlugin
public final class SGJPatchJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(
            SGJAdditionsCapacityPatch.MOD_ID,
            "dhd_visibility");
    private static final ResourceLocation ORIGINAL_ATLANTIS_DHD = ResourceLocation.fromNamespaceAndPath(
            "sgjadditions",
            "atlantis_dhd");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        Item hiddenItem = SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()
                ? SGJPatchItems.ATLANTIS_DHD.get()
                : ForgeRegistries.ITEMS.getValue(ORIGINAL_ATLANTIS_DHD);
        if (hiddenItem != null) {
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(hiddenItem)));
        }
    }
}